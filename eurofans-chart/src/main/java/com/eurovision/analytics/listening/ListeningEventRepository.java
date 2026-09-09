package com.eurovision.analytics.listening;

import com.eurovision.analytics.connectedaccount.Provider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface ListeningEventRepository extends JpaRepository<ListeningEvent, Long> {

    boolean existsByFingerprint(String fingerprint);

    Optional<ListeningEvent> findFirstByUserIdAndProviderAndCanonicalArtistIdIsNotNullAndResolutionStatusOrderByPlayedAtUtcDesc(
            long userId, Provider provider, ResolutionStatus resolutionStatus);

    @Query("""
            select le.canonicalArtist.id as artistId, count(le) as listenCount, count(distinct le.user.id) as uniqueListeners
            from ListeningEvent le
            where le.resolutionStatus = com.eurovision.analytics.listening.ResolutionStatus.CONFIRMED
              and le.canonicalArtist is not null
              and le.canonicalArtist.status = com.eurovision.analytics.eurovision.ArtistStatus.VERIFIED
              and le.canonicalArtist.active = true
              and (:from is null or le.playedAtUtc >= :from)
              and (:to is null or le.playedAtUtc < :to)
              and le.user.chartParticipationEnabled = true
            group by le.canonicalArtist.id
            """)
    List<ChartAggregationRow> aggregateForPeriod(@Param("from") Instant from, @Param("to") Instant to);

    interface ChartAggregationRow {
        Long getArtistId();
        Long getListenCount();
        Long getUniqueListeners();
    }
}
