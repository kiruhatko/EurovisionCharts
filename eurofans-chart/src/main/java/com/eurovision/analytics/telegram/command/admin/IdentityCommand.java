package com.eurovision.analytics.telegram.command.admin;

import com.eurovision.analytics.eurovision.identity.UnresolvedArtistEntity;
import com.eurovision.analytics.eurovision.identity.UnresolvedArtistEntityRepository;
import com.eurovision.analytics.eurovision.identity.UnresolvedStatus;
import com.eurovision.analytics.telegram.Command;
import com.eurovision.analytics.telegram.CommandContext;
import com.eurovision.analytics.telegram.MessageSender;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class IdentityCommand implements Command {

    private final UnresolvedArtistEntityRepository repository;
    private final MessageSender messageSender;

    public IdentityCommand(UnresolvedArtistEntityRepository repository, MessageSender messageSender) {
        this.repository = repository;
        this.messageSender = messageSender;
    }

    @Override
    public String name() {
        return "identity";
    }

    @Override
    public boolean requiresAdmin() {
        return true;
    }

    @Override
    public void handle(CommandContext ctx) {
        List<UnresolvedArtistEntity> entries = repository.findByStatusOrderByOccurrencesDesc(
                UnresolvedStatus.PENDING_REVIEW, PageRequest.of(0, 20));
        if (entries.isEmpty()) {
            messageSender.send(ctx.chatId(), "Черга нерозпізнаних артистів порожня.");
            return;
        }
        StringBuilder sb = new StringBuilder("<b>Unresolved artists (top 20 by occurrences)</b>\n");
        for (UnresolvedArtistEntity entry : entries) {
            sb.append("#").append(entry.getId()).append(" [").append(entry.getProvider()).append("] ")
                    .append(entry.getRawName()).append(" — ").append(entry.getOccurrences()).append("×\n");
        }
        messageSender.send(ctx.chatId(), sb.toString());
    }
}
