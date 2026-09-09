package com.eurovision.analytics.telegram.command.user;

import com.eurovision.analytics.connectedaccount.ConnectedAccountService;
import com.eurovision.analytics.connectedaccount.Provider;
import com.eurovision.analytics.telegram.Command;
import com.eurovision.analytics.telegram.CommandContext;
import com.eurovision.analytics.telegram.MessageSender;
import org.springframework.stereotype.Component;

@Component
public class DisconnectCommand implements Command {

    private final ConnectedAccountService connectedAccountService;
    private final MessageSender messageSender;

    public DisconnectCommand(ConnectedAccountService connectedAccountService, MessageSender messageSender) {
        this.connectedAccountService = connectedAccountService;
        this.messageSender = messageSender;
    }

    @Override
    public String name() {
        return "disconnect";
    }

    @Override
    public void handle(CommandContext ctx) {
        String[] args = ctx.args();
        if (args.length == 0) {
            messageSender.send(ctx.chatId(), "Використання: /disconnect &lt;spotify|applemusic|soundcloud|lastfm&gt;");
            return;
        }
        Provider provider;
        try {
            provider = parseProvider(args[0]);
        } catch (IllegalArgumentException e) {
            messageSender.send(ctx.chatId(), "Невідомий провайдер: " + args[0]);
            return;
        }
        boolean disconnected = connectedAccountService.disconnect(ctx.user().getId(), provider);
        messageSender.send(ctx.chatId(), disconnected ? provider + " відключено." : provider + " не було підключено.");
    }

    private Provider parseProvider(String raw) {
        return switch (raw.toLowerCase()) {
            case "spotify" -> Provider.SPOTIFY;
            case "applemusic", "apple_music", "apple" -> Provider.APPLE_MUSIC;
            case "soundcloud" -> Provider.SOUNDCLOUD;
            case "lastfm" -> Provider.LASTFM;
            default -> throw new IllegalArgumentException(raw);
        };
    }
}
