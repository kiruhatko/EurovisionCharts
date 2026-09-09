package com.eurovision.analytics.telegram.command.admin;

import com.eurovision.analytics.admin.AdminAuditService;
import com.eurovision.analytics.eurovision.EurovisionArtist;
import com.eurovision.analytics.eurovision.EurovisionArtistAlias;
import com.eurovision.analytics.eurovision.EurovisionArtistAliasRepository;
import com.eurovision.analytics.eurovision.EurovisionArtistRepository;
import com.eurovision.analytics.telegram.Command;
import com.eurovision.analytics.telegram.CommandContext;
import com.eurovision.analytics.telegram.MessageSender;
import com.eurovision.analytics.util.NameNormalizer;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Component
public class AddAliasCommand implements Command {

    private final EurovisionArtistRepository artistRepository;
    private final EurovisionArtistAliasRepository aliasRepository;
    private final AdminAuditService adminAuditService;
    private final MessageSender messageSender;

    public AddAliasCommand(EurovisionArtistRepository artistRepository, EurovisionArtistAliasRepository aliasRepository,
                            AdminAuditService adminAuditService, MessageSender messageSender) {
        this.artistRepository = artistRepository;
        this.aliasRepository = aliasRepository;
        this.adminAuditService = adminAuditService;
        this.messageSender = messageSender;
    }

    @Override
    public String name() {
        return "addalias";
    }

    @Override
    public boolean requiresAdmin() {
        return true;
    }

    @Override
    @Transactional
    public void handle(CommandContext ctx) {
        String[] args = ctx.argsRaw().trim().split("\\s+", 2);
        if (args.length < 2 || args[0].isBlank()) {
            messageSender.send(ctx.chatId(), "Використання: /addalias &lt;artistId&gt; &lt;alias&gt;");
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
        String alias = args[1].trim();
        aliasRepository.save(new EurovisionArtistAlias(maybeArtist.get(), alias, NameNormalizer.normalize(alias)));
        adminAuditService.log(ctx.telegramUserId(), "ADDALIAS", "eurovision_artist", String.valueOf(artistId), alias);
        messageSender.send(ctx.chatId(), "Alias \"" + alias + "\" додано для #" + artistId + ".");
    }
}
