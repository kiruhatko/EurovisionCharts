package com.eurovision.analytics.artwork;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ArtworkCacheRepository extends JpaRepository<ArtworkCache, Long> {

    Optional<ArtworkCache> findByLookupKey(String lookupKey);
}
