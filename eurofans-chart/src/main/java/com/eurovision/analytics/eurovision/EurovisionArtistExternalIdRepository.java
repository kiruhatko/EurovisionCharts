package com.eurovision.analytics.eurovision;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EurovisionArtistExternalIdRepository extends JpaRepository<EurovisionArtistExternalId, Long> {

    Optional<EurovisionArtistExternalId> findByProviderAndExternalId(ExternalIdProvider provider, String externalId);

    List<EurovisionArtistExternalId> findByArtistId(long artistId);
}
