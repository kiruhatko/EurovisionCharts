package com.eurovision.analytics.telegram.command.user;

import com.eurovision.analytics.telegram.Command;
import com.eurovision.analytics.telegram.CommandContext;
import com.eurovision.analytics.telegram.MessageSender;
import org.springframework.stereotype.Component;

@Component
public class StartCommand implements Command {

    private final MessageSender messageSender;

    public StartCommand(MessageSender messageSender) {
        this.messageSender = messageSender;
    }

    @Override
    public String name() {
        return "start";
    }

    @Override
    public void handle(CommandContext ctx) {
        messageSender.send(ctx.chatId(), """
                🇪🇺 <b>Eurofans UA Chart</b>

                Слідкуйте, скільки ви (і вся спільнота) слухаєте Eurovision-артистів на Spotify, Apple Music, SoundCloud та Last.fm.

                Підключіть хоча б один сервіс:
                /spotify /applemusic /soundcloud /lastfm

                Потім спробуйте /track, /last, /me, /stats, /chart.
                Повний список команд: /help""");
    }
}
