package com.eurovision.analytics.telegram.command.user;

import com.eurovision.analytics.connectedaccount.Provider;
import com.eurovision.analytics.oauth.ProviderAvailabilityService;
import com.eurovision.analytics.oauth.spotify.SpotifyOAuthService;
import com.eurovision.analytics.telegram.Command;
import com.eurovision.analytics.telegram.CommandContext;
import com.eurovision.analytics.telegram.MessageSender;
import org.springframework.stereotype.Component;

@Component
public class SpotifyCommand implements Command {

    private final ProviderAvailabilityService availabilityService;
    private final SpotifyOAuthService oAuthService;
    private final MessageSender messageSender;

    public SpotifyCommand(ProviderAvailabilityService availabilityService, SpotifyOAuthService oAuthService,
                           MessageSender messageSender) {
        this.availabilityService = availabilityService;
        this.oAuthService = oAuthService;
        this.messageSender = messageSender;
    }

    @Override
    public String name() {
        return "spotify";
    }

    @Override
    public void handle(CommandContext ctx) {
        if (!availabilityService.isConfigured(Provider.SPOTIFY)) {
            messageSender.send(ctx.chatId(), ProviderAvailabilityService.NOT_CONFIGURED_MESSAGE);
            return;
        }
        String url = oAuthService.buildAuthorizationUrl(ctx.telegramUserId());
        messageSender.send(ctx.chatId(), "Підключіть Spotify за посиланням (одноразове, дійсне 10 хвилин):\n" + url);
    }
}
