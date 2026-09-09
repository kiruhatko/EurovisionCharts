package com.eurovision.analytics.telegram.command.user;

import com.eurovision.analytics.telegram.Command;
import com.eurovision.analytics.telegram.CommandContext;
import com.eurovision.analytics.telegram.MessageSender;
import org.springframework.stereotype.Component;

@Component
public class HelpCommand implements Command {

    private final MessageSender messageSender;

    public HelpCommand(MessageSender messageSender) {
        this.messageSender = messageSender;
    }

    @Override
    public String name() {
        return "help";
    }

    @Override
    public void handle(CommandContext ctx) {
        messageSender.send(ctx.chatId(), """
                <b>Підключення</b>
                /spotify — підключити Spotify
                /applemusic — підключити Apple Music
                /soundcloud — підключити SoundCloud
                /lastfm &lt;username&gt; — підключити Last.fm
                /connections — статус усіх підключень
                /disconnect &lt;provider&gt; — відключити один сервіс
                /logout — вимкнути трекінг і участь у чарті всюди

                <b>Прослуховування</b>
                /track — що зараз грає
                /last — останній Eurovision-трек
                /me — ваша особиста статистика
                /stats — детальна статистика
                /chart [week|month|year|alltime] — чарт Eurovision-артистів""");
    }
}
