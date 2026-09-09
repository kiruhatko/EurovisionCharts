package com.eurovision.analytics.telegram;

import com.eurovision.analytics.config.AdminProperties;
import com.eurovision.analytics.deployment.DeploymentBindingVerifier;
import com.eurovision.analytics.eurovision.identity.EurovisionArtistImportService;
import com.eurovision.analytics.security.RateLimiter;
import com.eurovision.analytics.security.SecurityEventService;
import com.eurovision.analytics.security.SecurityEventType;
import com.eurovision.analytics.telegram.keyboard.AddArtistKeyboards;
import com.eurovision.analytics.user.User;
import com.eurovision.analytics.user.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.methods.groupadministration.LeaveChat;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * The single Telegram update entry point. Enforces, in order, before any
 * command ever runs: (1) group deployment-binding authorization for
 * group/supergroup chats -- private chats are never subject to this lock,
 * (2) admin-only gating for admin commands, (3) per-command rate limiting.
 */
@Component
public class UpdateDispatcher implements LongPollingSingleThreadUpdateConsumer {

    private static final Logger log = LoggerFactory.getLogger(UpdateDispatcher.class);
    private static final Duration UNAUTHORIZED_WARNING_COOLDOWN = Duration.ofMinutes(5);
    private static final List<String> RATE_LIMITED_COMMANDS = List.of("track", "last", "stats", "chart");

    private final Map<String, Command> commandsByName;
    private final DeploymentBindingVerifier deploymentBindingVerifier;
    private final AdminProperties adminProperties;
    private final UserService userService;
    private final SecurityEventService securityEventService;
    private final RateLimiter rateLimiter;
    private final MessageSender messageSender;
    private final TelegramClient telegramClient;
    private final EurovisionArtistImportService importService;

    private final Map<Long, Instant> lastUnauthorizedWarningByChat = new ConcurrentHashMap<>();

    public UpdateDispatcher(List<Command> commands,
                             DeploymentBindingVerifier deploymentBindingVerifier,
                             AdminProperties adminProperties,
                             UserService userService,
                             SecurityEventService securityEventService,
                             RateLimiter rateLimiter,
                             MessageSender messageSender,
                             TelegramClient telegramClient,
                             EurovisionArtistImportService importService) {
        this.commandsByName = commands.stream().collect(Collectors.toMap(Command::name, Function.identity()));
        this.deploymentBindingVerifier = deploymentBindingVerifier;
        this.adminProperties = adminProperties;
        this.userService = userService;
        this.securityEventService = securityEventService;
        this.rateLimiter = rateLimiter;
        this.messageSender = messageSender;
        this.telegramClient = telegramClient;
        this.importService = importService;
    }

    @Override
    public void consume(Update update) {
        try {
            if (update.hasCallbackQuery()) {
                handleCallbackQuery(update.getCallbackQuery());
                return;
            }
            if (update.hasMessage() && update.getMessage().hasText()) {
                handleMessage(update.getMessage());
            }
        } catch (Exception e) {
            log.error("Unhandled error processing update {}: {}", update.getUpdateId(), e.getMessage(), e);
        }
    }

    private void handleMessage(Message message) {
        long chatId = message.getChatId();
        String chatType = message.getChat().getType();
        boolean isGroupChat = "group".equals(chatType) || "supergroup".equals(chatType);

        if (isGroupChat && !isGroupAuthorized(chatId)) {
            rejectUnauthorizedGroup(chatId);
            return;
        }

        String text = message.getText();
        if (!text.startsWith("/")) {
            return;
        }
        String[] firstTokenSplit = text.substring(1).split("\\s+", 2);
        String commandName = firstTokenSplit[0].split("@", 2)[0].toLowerCase();
        String argsRaw = firstTokenSplit.length > 1 ? firstTokenSplit[1] : "";

        Command command = commandsByName.get(commandName);
        if (command == null) {
            return;
        }

        long telegramUserId = message.getFrom().getId();
        boolean isAdmin = adminProperties.isAdmin(telegramUserId);

        if (command.requiresAdmin() && !isAdmin) {
            securityEventService.record(SecurityEventType.ADMIN_ACTION_DENIED, telegramUserId, chatId,
                    "Non-admin attempted admin command /" + commandName);
            return;
        }
        // Admin status never authorizes a group on its own: an admin command inside an
        // unauthorized group was already rejected above, before we ever reach here.

        if (RATE_LIMITED_COMMANDS.contains(commandName)) {
            String rateLimitKey = commandName + ":" + telegramUserId;
            if (!rateLimiter.tryAcquire(rateLimitKey, 20, Duration.ofMinutes(1))) {
                messageSender.send(chatId, "Забагато запитів. Спробуйте трохи пізніше.");
                return;
            }
        }

        User user = userService.findOrCreate(telegramUserId, message.getFrom().getUserName(), message.getFrom().getFirstName());
        CommandContext ctx = new CommandContext(message, chatId, telegramUserId,
                message.getFrom().getUserName(), message.getFrom().getFirstName(), argsRaw, user);

        try {
            command.handle(ctx);
        } catch (Exception e) {
            log.error("Command /{} failed for telegramUserId={}: {}", commandName, telegramUserId, e.getMessage(), e);
            messageSender.send(chatId, "Сталася помилка під час виконання команди.");
        }
    }

    private void handleCallbackQuery(CallbackQuery callbackQuery) {
        String data = callbackQuery.getData();
        if (data == null) {
            return;
        }
        long telegramUserId = callbackQuery.getFrom().getId();
        if (!adminProperties.isAdmin(telegramUserId)) {
            messageSender.answerCallback(callbackQuery.getId(), "Недостатньо прав.");
            return;
        }

        if (data.startsWith(AddArtistKeyboards.CONFIRM_PREFIX)) {
            long artistId = Long.parseLong(data.substring(AddArtistKeyboards.CONFIRM_PREFIX.length()));
            boolean confirmed = importService.confirm(artistId, telegramUserId).isPresent();
            messageSender.answerCallback(callbackQuery.getId(), confirmed ? "Підтверджено." : "Вже оброблено.");
        } else if (data.startsWith(AddArtistKeyboards.CANCEL_PREFIX)) {
            long artistId = Long.parseLong(data.substring(AddArtistKeyboards.CANCEL_PREFIX.length()));
            boolean cancelled = importService.cancel(artistId, telegramUserId);
            messageSender.answerCallback(callbackQuery.getId(), cancelled ? "Скасовано." : "Вже оброблено.");
        }
    }

    private boolean isGroupAuthorized(long chatId) {
        return deploymentBindingVerifier.requireVerified().isChatAuthorized(chatId);
    }

    private void rejectUnauthorizedGroup(long chatId) {
        securityEventService.record(SecurityEventType.UNAUTHORIZED_GROUP, null, chatId,
                "Update received from unauthorized group chatId=" + chatId);

        Instant lastWarned = lastUnauthorizedWarningByChat.get(chatId);
        Instant now = Instant.now();
        if (lastWarned == null || now.isAfter(lastWarned.plus(UNAUTHORIZED_WARNING_COOLDOWN))) {
            lastUnauthorizedWarningByChat.put(chatId, now);
            messageSender.send(chatId, "Цей бот не авторизований для роботи в цій групі.");
        }

        try {
            telegramClient.execute(LeaveChat.builder().chatId(String.valueOf(chatId)).build());
        } catch (TelegramApiException e) {
            log.warn("Failed to leave unauthorized chatId={}: {}", chatId, e.getMessage());
        }
    }
}
