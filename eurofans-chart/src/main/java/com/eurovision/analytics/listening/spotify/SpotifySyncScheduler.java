package com.eurovision.analytics.listening.spotify;

import com.eurovision.analytics.connectedaccount.ConnectedAccountRepository;
import com.eurovision.analytics.connectedaccount.ConnectedAccountService;
import com.eurovision.analytics.listening.AbstractProviderSyncScheduler;
import com.eurovision.analytics.listening.ListeningIngestionService;
import com.eurovision.analytics.listening.SyncStateRepository;
import com.eurovision.analytics.oauth.spotify.SpotifyApiClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class SpotifySyncScheduler extends AbstractProviderSyncScheduler {

    public SpotifySyncScheduler(ConnectedAccountRepository connectedAccountRepository,
                                 ConnectedAccountService connectedAccountService,
                                 SyncStateRepository syncStateRepository,
                                 ListeningIngestionService ingestionService,
                                 SpotifyApiClient spotifyApiClient) {
        super(connectedAccountRepository, connectedAccountService, syncStateRepository, ingestionService, spotifyApiClient);
    }

    @Scheduled(fixedRate = 5, timeUnit = TimeUnit.MINUTES)
    public void run() {
        syncAll();
    }
}
