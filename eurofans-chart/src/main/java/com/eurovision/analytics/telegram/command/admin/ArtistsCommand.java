package com.eurovision.analytics.telegram.command.admin;

import com.eurovision.analytics.eurovision.ArtistStatus;
import com.eurovision.analytics.eurovision.EurovisionArtist;
import com.eurovision.analytics.eurovision.EurovisionArtistRepository;
import com.eurovision.analytics.telegram.Command;
import com.eurovision.analytics.telegram.CommandContext;
import com.eurovision.analytics.telegram.MessageSender;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ArtistsCommand implements Command {

    private final EurovisionArtistRepository artistRepository;
    private final MessageSender messageSender;

    public ArtistsCommand(EurovisionArtistRepository artistRepository, MessageSender messageSender) {
        this.artistRepository = artistRepository;
        this.messageSender = messageSender;
    }

    @Override
    public String name() {
        return "artists";
    }

    @Override
    public boolean requiresAdmin() {
        return true;
    }

    @Override
    public void handle(CommandContext ctx) {
        List<EurovisionArtist> verified = artistRepository.findByStatusAndActiveTrue(ArtistStatus.VERIFIED);
        if (verified.isEmpty()) {
            messageSender.send(ctx.chatId(), "Ще немає жодного VERIFIED артиста.");
            return;
        }
        StringBuilder sb = new StringBuilder("<b>Verified artists (" + verified.size() + ")</b>\n");
        for (EurovisionArtist artist : verified) {
            sb.append("#").append(artist.getId()).append(" — ").append(artist.getCanonicalName()).append('\n');
        }
        messageSender.send(ctx.chatId(), sb.toString());
    }
}
