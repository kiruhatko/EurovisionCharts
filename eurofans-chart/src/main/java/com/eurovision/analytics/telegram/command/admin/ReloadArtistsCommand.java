package com.eurovision.analytics.telegram.command.admin;

import com.eurovision.analytics.eurovision.identity.BulkArtistImportService;
import com.eurovision.analytics.telegram.Command;
import com.eurovision.analytics.telegram.CommandContext;
import com.eurovision.analytics.telegram.MessageSender;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import java.io.InputStream;

/** Reloads {@code eurovision-artists.json} (classpath by default, or a path given as an argument). */
@Component
public class ReloadArtistsCommand implements Command {

    private static final String DEFAULT_PATH = "classpath:eurovision-artists.json";

    private final BulkArtistImportService bulkArtistImportService;
    private final ResourceLoader resourceLoader;
    private final MessageSender messageSender;

    public ReloadArtistsCommand(BulkArtistImportService bulkArtistImportService, ResourceLoader resourceLoader,
                                 MessageSender messageSender) {
        this.bulkArtistImportService = bulkArtistImportService;
        this.resourceLoader = resourceLoader;
        this.messageSender = messageSender;
    }

    @Override
    public String name() {
        return "reloadartists";
    }

    @Override
    public boolean requiresAdmin() {
        return true;
    }

    @Override
    public void handle(CommandContext ctx) {
        String path = ctx.argsRaw().isBlank() ? DEFAULT_PATH : ctx.argsRaw().trim();
        Resource resource = resourceLoader.getResource(path);
        if (!resource.exists()) {
            messageSender.send(ctx.chatId(), "Файл не знайдено: " + path);
            return;
        }
        try (InputStream in = resource.getInputStream()) {
            BulkArtistImportService.BulkImportSummary summary = bulkArtistImportService.importFromStream(in, ctx.telegramUserId());
            StringBuilder sb = new StringBuilder("Імпорт завершено.\n")
                    .append("Створено артистів: ").append(summary.artistsCreated()).append('\n')
                    .append("Оновлено артистів: ").append(summary.artistsUpdated()).append('\n')
                    .append("Прив'язано URL: ").append(summary.urlsLinked()).append('\n');
            if (!summary.errors().isEmpty()) {
                sb.append("Помилки (").append(summary.errors().size()).append("):\n");
                summary.errors().stream().limit(10).forEach(e -> sb.append("• ").append(e).append('\n'));
            }
            messageSender.send(ctx.chatId(), sb.toString());
        } catch (Exception e) {
            messageSender.send(ctx.chatId(), "Помилка імпорту: " + e.getMessage());
        }
    }
}
