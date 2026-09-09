package com.eurovision.analytics.telegram.command.admin;

import com.eurovision.analytics.eurovision.identity.ArtistImportResult;
import com.eurovision.analytics.eurovision.identity.EurovisionArtistImportService;
import com.eurovision.analytics.security.SsrfUrlValidator;
import com.eurovision.analytics.telegram.Command;
import com.eurovision.analytics.telegram.CommandContext;
import com.eurovision.analytics.telegram.MessageSender;
import com.eurovision.analytics.telegram.keyboard.AddArtistKeyboards;
import org.springframework.stereotype.Component;

@Component
public class AddArtistCommand implements Command {

    private final EurovisionArtistImportService importService;
    private final MessageSender messageSender;

    public AddArtistCommand(EurovisionArtistImportService importService, MessageSender messageSender) {
        this.importService = importService;
        this.messageSender = messageSender;
    }

    @Override
    public String name() {
        return "addartist";
    }

    @Override
    public boolean requiresAdmin() {
        return true;
    }

    @Override
    public void handle(CommandContext ctx) {
        String url = ctx.argsRaw().trim();
        if (url.isEmpty()) {
            messageSender.send(ctx.chatId(), "Використання: /addartist &lt;URL&gt;");
            return;
        }
        report(ctx, url, importService, messageSender);
    }

    static void report(CommandContext ctx, String url, EurovisionArtistImportService importService,
                        MessageSender messageSender) {
        try {
            ArtistImportResult result = importService.importFromUrl(url, ctx.telegramUserId());
            switch (result) {
                case ArtistImportResult.Failed failed ->
                        messageSender.send(ctx.chatId(), "❌ " + url + "\n" + failed.reason());
                case ArtistImportResult.AlreadyLinked already -> messageSender.send(ctx.chatId(),
                        "ℹ️ Вже пов'язано з артистом #" + already.artist().getId() + " (" + already.artist().getCanonicalName() + ")");
                case ArtistImportResult.PendingConfirmation pending -> {
                    String candidates = pending.musicBrainzCandidateNames().isEmpty()
                            ? "немає збігів у MusicBrainz"
                            : String.join(", ", pending.musicBrainzCandidateNames());
                    String text = "🔎 <b>" + pending.artist().getCanonicalName() + "</b>\n"
                            + "Confidence: " + pending.confidence() + "\n"
                            + "MusicBrainz: " + candidates + "\n\n"
                            + "Підтвердити створення VERIFIED-артиста?";
                    messageSender.send(ctx.chatId(), text, AddArtistKeyboards.confirmCancel(pending.artist().getId()));
                }
            }
        } catch (SsrfUrlValidator.SsrfBlockedException e) {
            messageSender.send(ctx.chatId(), "❌ " + url + "\nЗаборонений URL: " + e.getMessage());
        } catch (Exception e) {
            messageSender.send(ctx.chatId(), "❌ " + url + "\nПомилка: " + e.getMessage());
        }
    }
}
