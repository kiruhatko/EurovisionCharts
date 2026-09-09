package com.eurovision.analytics.telegram.command.user;

import com.eurovision.analytics.listening.ListeningEvent;
import com.eurovision.analytics.nowplaying.LastTrackResult;
import com.eurovision.analytics.nowplaying.NowPlayingResolver;
import com.eurovision.analytics.telegram.Command;
import com.eurovision.analytics.telegram.CommandContext;
import com.eurovision.analytics.telegram.MessageSender;
import org.springframework.stereotype.Component;

@Component
public class LastCommand implements Command {

    private final NowPlayingResolver nowPlayingResolver;
    private final MessageSender messageSender;

    public LastCommand(NowPlayingResolver nowPlayingResolver, MessageSender messageSender) {
        this.nowPlayingResolver = nowPlayingResolver;
        this.messageSender = messageSender;
    }

    @Override
    public String name() {
        return "last";
    }

    @Override
    public void handle(CommandContext ctx) {
        LastTrackResult result = nowPlayingResolver.resolveLastTrack(ctx.user().getId());
        switch (result) {
            case LastTrackResult.NothingFound ignored ->
                    messageSender.send(ctx.chatId(), "Ще немає жодного зафіксованого Eurovision-треку.");
            case LastTrackResult.Found found -> {
                ListeningEvent event = found.event();
                String when = event.getPlayedAtUtc() == null
                        ? "час невідомий"
                        : event.getPlayedAtUtc().toString();
                messageSender.send(ctx.chatId(), "🇪🇺 <b>" + event.getCanonicalArtist().getCanonicalName() + "</b>\n"
                        + event.getRawTrackName() + "\n"
                        + "Джерело: " + event.getProvider() + " • " + when);
            }
        }
    }
}
