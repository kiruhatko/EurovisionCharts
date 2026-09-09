package com.eurovision.analytics.telegram.command.user;

import com.eurovision.analytics.connectedaccount.Provider;
import com.eurovision.analytics.oauth.ProviderAvailabilityService;
import com.eurovision.analytics.oauth.soundcloud.SoundCloudOAuthService;
import com.eurovision.analytics.telegram.Command;
import com.eurovision.analytics.telegram.CommandContext;
import com.eurovision.analytics.telegram.MessageSender;
import org.springframework.stereotype.Component;

@Component
public class SoundCloudCommand implements Command {

    private final ProviderAvailabilityService availabilityService;
    private final SoundCloudOAuthService oAuthService;
    private final MessageSender messageSender;

    public SoundCloudCommand(ProviderAvailabilityService availabilityService, SoundCloudOAuthService oAuthService,
                              MessageSender messageSender) {
        this.availabilityService = availabilityService;
        this.oAuthService = oAuthService;
        this.messageSender = messageSender;
    }

    @Override
    public String name() {
        return "soundcloud";
    }

    @Override
    public void handle(CommandContext ctx) {
        if (!availabilityService.isConfigured(Provider.SOUNDCLOUD)) {
            messageSender.send(ctx.chatId(), ProviderAvailabilityService.NOT_CONFIGURED_MESSAGE);
            return;
        }
        String url = oAuthService.buildAuthorizationUrl(ctx.telegramUserId());
        messageSender.send(ctx.chatId(), "Підключіть SoundCloud за посиланням (одноразове, дійсне 10 хвилин):\n" + url);
    }
}
