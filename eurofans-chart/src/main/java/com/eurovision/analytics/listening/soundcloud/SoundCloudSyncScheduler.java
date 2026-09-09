package com.eurovision.analytics.listening.soundcloud;

import com.eurovision.analytics.connectedaccount.ConnectedAccountRepository;
import com.eurovision.analytics.connectedaccount.ConnectedAccountService;
import com.eurovision.analytics.listening.AbstractProviderSyncScheduler;
import com.eurovision.analytics.listening.ListeningIngestionService;
import com.eurovision.analytics.listening.SyncStateRepository;
import com.eurovision.analytics.oauth.soundcloud.SoundCloudApiClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class SoundCloudSyncScheduler extends AbstractProviderSyncScheduler {

    public SoundCloudSyncScheduler(ConnectedAccountRepository connectedAccountRepository,
                                    ConnectedAccountService connectedAccountService,
                                    SyncStateRepository syncStateRepository,
                                    ListeningIngestionService ingestionService,
                                    SoundCloudApiClient soundCloudApiClient) {
        super(connectedAccountRepository, connectedAccountService, syncStateRepository, ingestionService, soundCloudApiClient);
    }

    @Scheduled(fixedRate = 10, timeUnit = TimeUnit.MINUTES)
    public void run() {
        syncAll();
    }
}
