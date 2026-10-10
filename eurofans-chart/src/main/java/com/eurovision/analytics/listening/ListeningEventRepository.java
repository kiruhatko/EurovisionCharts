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

    Optional<ListeningEvent> findFirstByUserIdAndProviderAndCanonicalArtistIdIsNotNullAndResolutionStatusInOrderByPlayedAtUtcDesc(
            long userId, Provider provider, List<ResolutionStatus> resolutionStatuses);

    // Chart-studio cover art: the most recent confirmed-or-probable play carries the raw
    // provider/artist/track that ArtworkResolutionService's cache is keyed by.
    Optional<ListeningEvent> findFirstByCanonicalArtist_IdAndResolutionStatusInOrderByPlayedAtUtcDesc(
            long artistId, List<ResolutionStatus> resolutionStatuses);

    // Chart-studio 7-day trend bars: one bar per calendar day in the selected window.
    // PROBABLE (an admin-curated alias match) counts alongside CONFIRMED here and in every
    // aggregate query below. CONFLICT and UNKNOWN are never counted.
    @Query(value = """
            select cast(date_trunc('day', played_at_utc) as date) as day, count(*) as cnt
            from listening_events
            where canonical_artist_id = :artistId
              and resolution_status in ('CONFIRMED', 'PROBABLE')
              and played_at_utc >= :from
              and played_at_utc < :to
            group by 1
            """, nativeQuery = true)
    List<DailyCountRow> dailyCounts(@Param("artistId") long artistId, @Param("from") Instant from, @Param("to") Instant to);

    interface DailyCountRow {
        java.time.LocalDate getDay();
        Long getCnt();
    }

    // `from`/`to` are always concrete Instants (never null) at the call site -- ALL_TIME's
    // unbounded start is passed as Instant.EPOCH -- so every comparison below is a plain
    // bind-parameter comparison. A `(:from is null or ...)` form was tried first but Postgres'
    // JDBC driver cannot infer a bind parameter's type when the only usage is inside such an
    // "is null or" branch, and fails the query outright with "could not determine data type of
    // parameter $1". One consequence: a listening_event with no playedAtUtc at all (Apple
    // Music's recent-tracks endpoint never reports one, spec 4.3.8) never counts toward any
    // chart period, ALL_TIME included, since NULL >= any bound is NULL/false in SQL -- an
    // accepted extension of that provider's already-documented timestamp limitation.
    @Query("""
            select le.canonicalArtist.id as artistId, count(le) as listenCount, count(distinct le.user.id) as uniqueListeners,
                   max(le.playedAtUtc) as mostRecentPlayAt
            from ListeningEvent le
            where le.resolutionStatus in (com.eurovision.analytics.listening.ResolutionStatus.CONFIRMED,
                                           com.eurovision.analytics.listening.ResolutionStatus.PROBABLE)
              and le.canonicalArtist is not null
              and le.canonicalArtist.status = com.eurovision.analytics.eurovision.ArtistStatus.VERIFIED
              and le.canonicalArtist.active = true
              and le.playedAtUtc >= :from
              and le.playedAtUtc < :to
              and le.user.chartParticipationEnabled = true
            group by le.canonicalArtist.id
            """)
    List<ChartAggregationRow> aggregateForPeriod(@Param("from") Instant from, @Param("to") Instant to);

    @Query("""
            select count(le)
            from ListeningEvent le
            where le.canonicalArtist.id = :artistId
              and le.resolutionStatus in (com.eurovision.analytics.listening.ResolutionStatus.CONFIRMED,
                                           com.eurovision.analytics.listening.ResolutionStatus.PROBABLE)
              and le.playedAtUtc >= :from
              and le.playedAtUtc < :to
              and le.user.chartParticipationEnabled = true
            """)
    long countForArtistInWindow(@Param("artistId") long artistId, @Param("from") Instant from, @Param("to") Instant to);

    @Query("""
            select le.user.id as userId, count(le) as listenCount
            from ListeningEvent le
            where le.resolutionStatus in (com.eurovision.analytics.listening.ResolutionStatus.CONFIRMED,
                                           com.eurovision.analytics.listening.ResolutionStatus.PROBABLE)
              and le.playedAtUtc >= :from
              and le.playedAtUtc < :to
              and le.user.active = true
              and le.user.chartParticipationEnabled = true
            group by le.user.id
            """)
    List<UserListenCountRow> aggregateUserListenCounts(@Param("from") Instant from, @Param("to") Instant to);

    interface UserListenCountRow {
        Long getUserId();
        Long getListenCount();
    }

    interface ChartAggregationRow {
        Long getArtistId();
        Long getListenCount();
        Long getUniqueListeners();
        Instant getMostRecentPlayAt();
    }
}
