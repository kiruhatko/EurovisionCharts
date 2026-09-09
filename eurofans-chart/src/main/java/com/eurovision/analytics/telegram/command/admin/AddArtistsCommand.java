package com.eurovision.analytics.telegram.command.admin;

import com.eurovision.analytics.eurovision.identity.EurovisionArtistImportService;
import com.eurovision.analytics.telegram.Command;
import com.eurovision.analytics.telegram.CommandContext;
import com.eurovision.analytics.telegram.MessageSender;
import org.springframework.stereotype.Component;

/** Batch variant of {@code /addartist}: one URL per line/whitespace token, each resolved independently. */
@Component
public class AddArtistsCommand implements Command {

    private final EurovisionArtistImportService importService;
    private final MessageSender messageSender;

    public AddArtistsCommand(EurovisionArtistImportService importService, MessageSender messageSender) {
        this.importService = importService;
        this.messageSender = messageSender;
    }

    @Override
    public String name() {
        return "addartists";
    }

    @Override
    public boolean requiresAdmin() {
        return true;
    }

    @Override
    public void handle(CommandContext ctx) {
        String[] urls = ctx.argsRaw().trim().split("\\s+");
        if (urls.length == 0 || urls[0].isBlank()) {
            messageSender.send(ctx.chatId(), "Використання: /addartists &lt;URL1&gt; &lt;URL2&gt; ...");
            return;
        }
        for (String url : urls) {
            AddArtistCommand.report(ctx, url, importService, messageSender);
        }
    }
}
