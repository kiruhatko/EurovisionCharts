package com.eurovision.analytics.telegram.command.user;

import com.eurovision.analytics.connectedaccount.ConnectedAccountService;
import com.eurovision.analytics.connectedaccount.Provider;
import com.eurovision.analytics.oauth.ProviderAvailabilityService;
import com.eurovision.analytics.oauth.lastfm.LastFmConnectionService;
import com.eurovision.analytics.telegram.Command;
import com.eurovision.analytics.telegram.CommandContext;
import com.eurovision.analytics.telegram.MessageSender;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class LastFmCommand implements Command {

    private final ProviderAvailabilityService availabilityService;
    private final LastFmConnectionService connectionService;
    private final ConnectedAccountService connectedAccountService;
    private final MessageSender messageSender;

    public LastFmCommand(ProviderAvailabilityService availabilityService, LastFmConnectionService connectionService,
                          ConnectedAccountService connectedAccountService, MessageSender messageSender) {
        this.availabilityService = availabilityService;
        this.connectionService = connectionService;
        this.connectedAccountService = connectedAccountService;
        this.messageSender = messageSender;
    }

    @Override
    public String name() {
        return "lastfm";
    }

    @Override
    public void handle(CommandContext ctx) {
        if (!availabilityService.isConfigured(Provider.LASTFM)) {
            messageSender.send(ctx.chatId(), ProviderAvailabilityService.NOT_CONFIGURED_MESSAGE);
            return;
        }
        String[] args = ctx.args();
        if (args.length == 0) {
            messageSender.send(ctx.chatId(), "Використання: /lastfm &lt;username&gt;");
            return;
        }
        Optional<String> verified = connectionService.verifyUsername(args[0]);
        if (verified.isEmpty()) {
            messageSender.send(ctx.chatId(), "Користувача Last.fm з таким іменем не знайдено.");
            return;
        }
        connectedAccountService.upsertConnection(ctx.user(), Provider.LASTFM, verified.get(), verified.get(),
                null, null, null, null);
        messageSender.send(ctx.chatId(), "Last.fm підключено: " + verified.get());
    }
}
