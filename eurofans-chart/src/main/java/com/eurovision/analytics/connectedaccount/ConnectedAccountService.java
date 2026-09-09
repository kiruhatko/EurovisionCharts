package com.eurovision.analytics.connectedaccount;

import com.eurovision.analytics.user.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ConnectedAccountService {

    private static final Logger log = LoggerFactory.getLogger(ConnectedAccountService.class);

    private final ConnectedAccountRepository repository;
    private final Map<Provider, TokenRevocationHandler> revocationHandlers;

    public ConnectedAccountService(ConnectedAccountRepository repository,
                                    List<TokenRevocationHandler> handlers) {
        this.repository = repository;
        this.revocationHandlers = handlers.stream()
                .collect(Collectors.toMap(TokenRevocationHandler::provider, Function.identity()));
    }

    public List<ConnectedAccount> findAllForUser(long userId) {
        return repository.findByUserId(userId);
    }

    public Optional<ConnectedAccount> findActive(long userId, Provider provider) {
        return repository.findByUserIdAndProvider(userId, provider)
                .filter(ConnectedAccount::isActive);
    }

    @Transactional
    public ConnectedAccount upsertConnection(User user, Provider provider, String providerAccountId,
                                              String providerDisplayName, byte[] accessTokenEncrypted,
                                              byte[] refreshTokenEncrypted, Instant tokenExpiresAt, String scopes) {
        ConnectedAccount account = repository.findByUserIdAndProvider(user.getId(), provider)
                .orElseGet(() -> new ConnectedAccount(user, provider, providerAccountId, providerDisplayName));

        account.setProviderAccountId(providerAccountId);
        account.setProviderDisplayName(providerDisplayName);
        account.setAccessTokenEncrypted(accessTokenEncrypted);
        account.setRefreshTokenEncrypted(refreshTokenEncrypted);
        account.setTokenExpiresAt(tokenExpiresAt);
        account.setScopes(scopes);
        account.setStatus(ConnectedAccountStatus.ACTIVE);
        account.setConsecutiveFailures(0);
        account.setDisconnectedAt(null);
        if (account.getConnectedAt() == null) {
            account.setConnectedAt(Instant.now());
        }
        return repository.save(account);
    }

    @Transactional
    public boolean disconnect(long userId, Provider provider) {
        Optional<ConnectedAccount> maybeAccount = repository.findByUserIdAndProvider(userId, provider);
        if (maybeAccount.isEmpty() || !maybeAccount.get().isActive()) {
            return false;
        }
        ConnectedAccount account = maybeAccount.get();

        TokenRevocationHandler handler = revocationHandlers.get(provider);
        if (handler != null) {
            try {
                handler.revoke(account);
            } catch (Exception e) {
                log.warn("Token revocation failed for provider={}, userId={}: {}", provider, userId, e.getMessage());
            }
        }

        account.setStatus(ConnectedAccountStatus.REVOKED);
        account.setAccessTokenEncrypted(null);
        account.setRefreshTokenEncrypted(null);
        account.setDisconnectedAt(Instant.now());
        repository.save(account);
        return true;
    }

    @Transactional
    public void markSyncResult(ConnectedAccount account, boolean success, String statusDetail) {
        account.setLastSyncAt(Instant.now());
        account.setLastSyncStatus(statusDetail);
        if (success) {
            account.setConsecutiveFailures(0);
        } else {
            account.setConsecutiveFailures(account.getConsecutiveFailures() + 1);
        }
        repository.save(account);
    }
}
