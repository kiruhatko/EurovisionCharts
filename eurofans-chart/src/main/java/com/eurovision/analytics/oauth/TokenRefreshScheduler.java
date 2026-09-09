package com.eurovision.analytics.oauth;

import com.eurovision.analytics.connectedaccount.ConnectedAccount;
import com.eurovision.analytics.connectedaccount.ConnectedAccountRepository;
import com.eurovision.analytics.connectedaccount.ConnectedAccountStatus;
import com.eurovision.analytics.connectedaccount.Provider;
import com.eurovision.analytics.security.SecurityEventService;
import com.eurovision.analytics.security.SecurityEventType;
import com.eurovision.analytics.security.TokenEncryptionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Proactively refreshes Spotify/SoundCloud access tokens ~5 minutes before
 * expiry (spec 4.6). Apple Music and Last.fm are never queried here: neither
 * has a refresh mechanism.
 */
@Component
public class TokenRefreshScheduler {

    private static final Logger log = LoggerFactory.getLogger(TokenRefreshScheduler.class);
    private static final long REFRESH_LOOKAHEAD_MINUTES = 5;

    private final ConnectedAccountRepository connectedAccountRepository;
    private final TokenEncryptionService tokenEncryptionService;
    private final SecurityEventService securityEventService;
    private final Map<Provider, TokenRefreshHandler> handlers;

    public TokenRefreshScheduler(ConnectedAccountRepository connectedAccountRepository,
                                  TokenEncryptionService tokenEncryptionService,
                                  SecurityEventService securityEventService,
                                  List<TokenRefreshHandler> refreshHandlers) {
        this.connectedAccountRepository = connectedAccountRepository;
        this.tokenEncryptionService = tokenEncryptionService;
        this.securityEventService = securityEventService;
        this.handlers = refreshHandlers.stream()
                .collect(Collectors.toMap(TokenRefreshHandler::provider, Function.identity()));
    }

    @Scheduled(fixedRate = 1, timeUnit = TimeUnit.MINUTES)
    public void refreshExpiringTokens() {
        if (!tokenEncryptionService.isConfigured()) {
            return;
        }
        Instant threshold = Instant.now().plusSeconds(REFRESH_LOOKAHEAD_MINUTES * 60);
        for (Map.Entry<Provider, TokenRefreshHandler> entry : handlers.entrySet()) {
            List<ConnectedAccount> expiring = connectedAccountRepository
                    .findByStatusAndTokenExpiresAtBefore(ConnectedAccountStatus.ACTIVE, threshold)
                    .stream()
                    .filter(a -> a.getProvider() == entry.getKey())
                    .filter(a -> a.getRefreshTokenEncrypted() != null)
                    .toList();
            for (ConnectedAccount account : expiring) {
                refreshOne(account, entry.getValue());
            }
        }
    }

    @Transactional
    void refreshOne(ConnectedAccount account, TokenRefreshHandler handler) {
        try {
            String refreshToken = tokenEncryptionService.decrypt(account.getRefreshTokenEncrypted());
            TokenRefreshHandler.RefreshedTokens refreshed = handler.refresh(refreshToken);

            account.setAccessTokenEncrypted(tokenEncryptionService.encrypt(refreshed.accessToken()));
            if (refreshed.refreshToken() != null) {
                account.setRefreshTokenEncrypted(tokenEncryptionService.encrypt(refreshed.refreshToken()));
            }
            account.setTokenExpiresAt(Instant.now().plusSeconds(refreshed.expiresInSeconds()));
            account.setLastTokenRefreshAt(Instant.now());
            account.setConsecutiveFailures(0);
            connectedAccountRepository.save(account);
        } catch (Exception e) {
            log.warn("Token refresh failed for connectedAccountId={} provider={}: {}",
                    account.getId(), account.getProvider(), e.getMessage());
            account.setConsecutiveFailures(account.getConsecutiveFailures() + 1);
            if (account.getConsecutiveFailures() >= 3) {
                account.setStatus(ConnectedAccountStatus.EXPIRED);
                securityEventService.record(SecurityEventType.OAUTH_TOKEN_EXCHANGE_FAILED, null, null,
                        "Repeated refresh failures for connectedAccountId=" + account.getId()
                                + "; marked EXPIRED, user must reconnect");
            }
            connectedAccountRepository.save(account);
        }
    }
}
