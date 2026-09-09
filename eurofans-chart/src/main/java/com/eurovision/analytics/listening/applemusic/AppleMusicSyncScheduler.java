package com.eurovision.analytics.listening.applemusic;

import com.eurovision.analytics.connectedaccount.ConnectedAccountRepository;
import com.eurovision.analytics.connectedaccount.ConnectedAccountService;
import com.eurovision.analytics.listening.AbstractProviderSyncScheduler;
import com.eurovision.analytics.listening.ListeningIngestionService;
import com.eurovision.analytics.listening.SyncStateRepository;
import com.eurovision.analytics.oauth.applemusic.AppleMusicApiClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class AppleMusicSyncScheduler extends AbstractProviderSyncScheduler {

    public AppleMusicSyncScheduler(ConnectedAccountRepository connectedAccountRepository,
                                    ConnectedAccountService connectedAccountService,
                                    SyncStateRepository syncStateRepository,
                                    ListeningIngestionService ingestionService,
                                    AppleMusicApiClient appleMusicApiClient) {
        super(connectedAccountRepository, connectedAccountService, syncStateRepository, ingestionService, appleMusicApiClient);
    }

    @Scheduled(fixedRate = 10, timeUnit = TimeUnit.MINUTES)
    public void run() {
        syncAll();
    }
}
