package com.eurovision.analytics.eurovision;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EurovisionArtistRepository extends JpaRepository<EurovisionArtist, Long> {

    List<EurovisionArtist> findByNormalizedName(String normalizedName);

    List<EurovisionArtist> findByStatusAndActiveTrue(ArtistStatus status);
}
