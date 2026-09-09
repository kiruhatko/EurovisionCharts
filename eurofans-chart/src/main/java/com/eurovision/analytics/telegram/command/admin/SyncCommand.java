package com.eurovision.analytics.telegram.command.admin;

import com.eurovision.analytics.listening.applemusic.AppleMusicSyncScheduler;
import com.eurovision.analytics.listening.lastfm.LastFmSyncScheduler;
import com.eurovision.analytics.listening.soundcloud.SoundCloudSyncScheduler;
import com.eurovision.analytics.listening.spotify.SpotifySyncScheduler;
import com.eurovision.analytics.telegram.Command;
import com.eurovision.analytics.telegram.CommandContext;
import com.eurovision.analytics.telegram.MessageSender;
import org.springframework.stereotype.Component;

@Component
public class SyncCommand implements Command {

    private final SpotifySyncScheduler spotifySyncScheduler;
    private final AppleMusicSyncScheduler appleMusicSyncScheduler;
    private final SoundCloudSyncScheduler soundCloudSyncScheduler;
    private final LastFmSyncScheduler lastFmSyncScheduler;
    private final MessageSender messageSender;

    public SyncCommand(SpotifySyncScheduler spotifySyncScheduler, AppleMusicSyncScheduler appleMusicSyncScheduler,
                        SoundCloudSyncScheduler soundCloudSyncScheduler, LastFmSyncScheduler lastFmSyncScheduler,
                        MessageSender messageSender) {
        this.spotifySyncScheduler = spotifySyncScheduler;
        this.appleMusicSyncScheduler = appleMusicSyncScheduler;
        this.soundCloudSyncScheduler = soundCloudSyncScheduler;
        this.lastFmSyncScheduler = lastFmSyncScheduler;
        this.messageSender = messageSender;
    }

    @Override
    public String name() {
        return "sync";
    }

    @Override
    public boolean requiresAdmin() {
        return true;
    }

    @Override
    public void handle(CommandContext ctx) {
        messageSender.send(ctx.chatId(), "Ручна синхронізація запущена для всіх провайдерів...");
        spotifySyncScheduler.run();
        appleMusicSyncScheduler.run();
        soundCloudSyncScheduler.run();
        lastFmSyncScheduler.run();
        messageSender.send(ctx.chatId(), "Синхронізацію завершено.");
    }
}
