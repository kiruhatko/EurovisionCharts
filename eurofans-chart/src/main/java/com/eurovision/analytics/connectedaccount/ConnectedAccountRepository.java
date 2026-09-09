package com.eurovision.analytics.connectedaccount;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ConnectedAccountRepository extends JpaRepository<ConnectedAccount, Long> {

    Optional<ConnectedAccount> findByUserIdAndProvider(long userId, Provider provider);

    List<ConnectedAccount> findByUserId(long userId);

    List<ConnectedAccount> findByProviderAndStatus(Provider provider, ConnectedAccountStatus status);

    List<ConnectedAccount> findByStatusAndTokenExpiresAtBefore(ConnectedAccountStatus status, java.time.Instant threshold);
}
