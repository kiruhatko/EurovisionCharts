package com.eurovision.analytics.listening;

import com.eurovision.analytics.connectedaccount.ConnectedAccount;
import com.eurovision.analytics.connectedaccount.ConnectedAccountRepository;
import com.eurovision.analytics.connectedaccount.ConnectedAccountService;
import com.eurovision.analytics.connectedaccount.ConnectedAccountStatus;
import com.eurovision.analytics.connectedaccount.Provider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.List;

/**
 * Shared sync-loop skeleton for the four provider-specific schedulers (spec
 * 7): each provider gets its own {@code @Scheduled} entry point and its own
 * {@link RecentPlayProviderClient}, but all four share the same isolation
 * rule -- one user's failure never stops the others in the same run -- and
 * the same simple circuit breaker: an account with 5+ consecutive failures is
 * skipped until the user reconnects, rather than hammering a dead token
 * forever.
 */
public abstract class AbstractProviderSyncScheduler {

    private static final int CIRCUIT_BREAKER_THRESHOLD = 5;

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final ConnectedAccountRepository connectedAccountRepository;
    private final ConnectedAccountService connectedAccountService;
    private final SyncStateRepository syncStateRepository;
    private final ListeningIngestionService ingestionService;
    private final RecentPlayProviderClient client;

    protected AbstractProviderSyncScheduler(ConnectedAccountRepository connectedAccountRepository,
                                             ConnectedAccountService connectedAccountService,
                                             SyncStateRepository syncStateRepository,
                                             ListeningIngestionService ingestionService,
                                             RecentPlayProviderClient client) {
        this.connectedAccountRepository = connectedAccountRepository;
        this.connectedAccountService = connectedAccountService;
        this.syncStateRepository = syncStateRepository;
        this.ingestionService = ingestionService;
        this.client = client;
    }

    protected void syncAll() {
        Provider provider = client.provider();
        List<ConnectedAccount> accounts = connectedAccountRepository.findByProviderAndStatus(provider, ConnectedAccountStatus.ACTIVE);
        for (ConnectedAccount account : accounts) {
            if (!account.getUser().isTrackingEnabled()) {
                continue;
            }
            if (account.getConsecutiveFailures() >= CIRCUIT_BREAKER_THRESHOLD) {
                continue;
            }
            syncOne(account);
        }
    }

    private void syncOne(ConnectedAccount account) {
        try {
            List<RecentPlay> plays = client.fetchRecentPlays(account);
            int ingested = 0;
            for (RecentPlay play : plays) {
                if (ingestionService.ingest(account, play)) {
                    ingested++;
                }
            }
            connectedAccountService.markSyncResult(account, true, "OK: " + ingested + " new events");
            updateSyncState(account, true);
        } catch (Exception e) {
            log.warn("Sync failed for connectedAccountId={} provider={}: {}",
                    account.getId(), account.getProvider(), e.getMessage());
            connectedAccountService.markSyncResult(account, false, "ERROR: " + e.getMessage());
            updateSyncState(account, false);
        }
    }

    private void updateSyncState(ConnectedAccount account, boolean success) {
        SyncState state = syncStateRepository.findByUserIdAndProvider(account.getUser().getId(), account.getProvider())
                .orElseGet(() -> new SyncState(account.getUser().getId(), account.getProvider()));
        state.setLastSyncedAt(Instant.now());
        state.setLastStatus(success ? "OK" : "ERROR");
        state.setConsecutiveFailures(success ? 0 : state.getConsecutiveFailures() + 1);
        state.setUpdatedAt(Instant.now());
        syncStateRepository.save(state);
    }
}
