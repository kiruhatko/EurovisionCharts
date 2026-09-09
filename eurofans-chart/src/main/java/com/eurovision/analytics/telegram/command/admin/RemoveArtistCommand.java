package com.eurovision.analytics.telegram.command.admin;

import com.eurovision.analytics.admin.AdminAuditService;
import com.eurovision.analytics.eurovision.ArtistStatus;
import com.eurovision.analytics.eurovision.EurovisionArtist;
import com.eurovision.analytics.eurovision.EurovisionArtistRepository;
import com.eurovision.analytics.telegram.Command;
import com.eurovision.analytics.telegram.CommandContext;
import com.eurovision.analytics.telegram.MessageSender;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Component
public class RemoveArtistCommand implements Command {

    private final EurovisionArtistRepository artistRepository;
    private final AdminAuditService adminAuditService;
    private final MessageSender messageSender;

    public RemoveArtistCommand(EurovisionArtistRepository artistRepository, AdminAuditService adminAuditService,
                                MessageSender messageSender) {
        this.artistRepository = artistRepository;
        this.adminAuditService = adminAuditService;
        this.messageSender = messageSender;
    }

    @Override
    public String name() {
        return "removeartist";
    }

    @Override
    public boolean requiresAdmin() {
        return true;
    }

    @Override
    @Transactional
    public void handle(CommandContext ctx) {
        String[] args = ctx.args();
        if (args.length == 0) {
            messageSender.send(ctx.chatId(), "Використання: /removeartist &lt;artistId&gt;");
            return;
        }
        long artistId;
        try {
            artistId = Long.parseLong(args[0]);
        } catch (NumberFormatException e) {
            messageSender.send(ctx.chatId(), "artistId має бути числом.");
            return;
        }
        Optional<EurovisionArtist> maybeArtist = artistRepository.findById(artistId);
        if (maybeArtist.isEmpty()) {
            messageSender.send(ctx.chatId(), "Артиста #" + artistId + " не знайдено.");
            return;
        }
        EurovisionArtist artist = maybeArtist.get();
        artist.setStatus(ArtistStatus.DISABLED);
        artist.setActive(false);
        artistRepository.save(artist);
        adminAuditService.log(ctx.telegramUserId(), "REMOVEARTIST", "eurovision_artist",
                String.valueOf(artistId), artist.getCanonicalName());
        messageSender.send(ctx.chatId(), "Артиста #" + artistId + " (" + artist.getCanonicalName() + ") вимкнено.");
    }
}
