package com.eurovision.analytics.listening;

import com.eurovision.analytics.connectedaccount.Provider;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SyncStateRepository extends JpaRepository<SyncState, Long> {

    Optional<SyncState> findByUserIdAndProvider(long userId, Provider provider);
}
