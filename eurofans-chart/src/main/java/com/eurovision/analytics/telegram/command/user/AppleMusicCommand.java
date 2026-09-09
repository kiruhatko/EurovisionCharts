package com.eurovision.analytics.telegram.command.user;

import com.eurovision.analytics.connectedaccount.Provider;
import com.eurovision.analytics.oauth.ProviderAvailabilityService;
import com.eurovision.analytics.oauth.applemusic.AppleMusicConnectionService;
import com.eurovision.analytics.telegram.Command;
import com.eurovision.analytics.telegram.CommandContext;
import com.eurovision.analytics.telegram.MessageSender;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;

import java.util.List;

@Component
public class AppleMusicCommand implements Command {

    private final ProviderAvailabilityService availabilityService;
    private final AppleMusicConnectionService connectionService;
    private final MessageSender messageSender;

    public AppleMusicCommand(ProviderAvailabilityService availabilityService,
                              AppleMusicConnectionService connectionService,
                              MessageSender messageSender) {
        this.availabilityService = availabilityService;
        this.connectionService = connectionService;
        this.messageSender = messageSender;
    }

    @Override
    public String name() {
        return "applemusic";
    }

    @Override
    public void handle(CommandContext ctx) {
        if (!availabilityService.isConfigured(Provider.APPLE_MUSIC)) {
            messageSender.send(ctx.chatId(), ProviderAvailabilityService.NOT_CONFIGURED_MESSAGE);
            return;
        }
        String url = connectionService.buildConnectUrl(ctx.telegramUserId());
        InlineKeyboardButton button = InlineKeyboardButton.builder().text("🍎 Connect Apple Music").url(url).build();
        InlineKeyboardMarkup keyboard = InlineKeyboardMarkup.builder().keyboard(List.of(new InlineKeyboardRow(button))).build();
        messageSender.send(ctx.chatId(), "Натисніть кнопку нижче, щоб підключити Apple Music у браузері.", keyboard);
    }
}
