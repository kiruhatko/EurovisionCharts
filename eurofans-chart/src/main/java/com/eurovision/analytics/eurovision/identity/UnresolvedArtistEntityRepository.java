package com.eurovision.analytics.eurovision.identity;

import com.eurovision.analytics.connectedaccount.Provider;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UnresolvedArtistEntityRepository extends JpaRepository<UnresolvedArtistEntity, Long> {

    Optional<UnresolvedArtistEntity> findByProviderAndRawExternalIdAndStatus(
            Provider provider, String rawExternalId, UnresolvedStatus status);
}
