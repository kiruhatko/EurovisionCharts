package com.eurovision.analytics.eurovision;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EurovisionArtistAliasRepository extends JpaRepository<EurovisionArtistAlias, Long> {

    List<EurovisionArtistAlias> findByNormalizedAliasAndActiveTrue(String normalizedAlias);
}
