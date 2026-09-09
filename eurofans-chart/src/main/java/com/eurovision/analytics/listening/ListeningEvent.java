package com.eurovision.analytics.listening;

import com.eurovision.analytics.connectedaccount.ConnectedAccount;
import com.eurovision.analytics.connectedaccount.Provider;
import com.eurovision.analytics.eurovision.EurovisionArtist;
import com.eurovision.analytics.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * The provider-agnostic replacement for a Last.fm-style "scrobble": one raw
 * playback event from exactly one provider. Never persisted for a live
 * now-playing signal — only for a completed, provider-confirmed play.
 */
@Entity
@Table(name = "listening_events")
@Getter
@Setter
@NoArgsConstructor
public class ListeningEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "connected_account_id", nullable = false)
    private ConnectedAccount connectedAccount;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false, length = 32)
    private Provider provider;

    @Column(name = "provider_track_id")
    private String providerTrackId;

    @Column(name = "provider_artist_id")
    private String providerArtistId;

    @Column(name = "raw_artist_name", nullable = false)
    private String rawArtistName;

    @Column(name = "raw_track_name", nullable = false)
    private String rawTrackName;

    @Column(name = "raw_album_name")
    private String rawAlbumName;

    @Column(name = "played_at_utc")
    private Instant playedAtUtc;

    @Column(name = "is_estimated_timestamp", nullable = false)
    private boolean estimatedTimestamp = false;

    // EAGER: LastCommand reads .getCanonicalArtist().getCanonicalName() outside a transaction.
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "canonical_artist_id")
    private EurovisionArtist canonicalArtist;

    @Column(name = "resolution_method", length = 64)
    private String resolutionMethod;

    @Enumerated(EnumType.STRING)
    @Column(name = "resolution_status", nullable = false, length = 32)
    private ResolutionStatus resolutionStatus = ResolutionStatus.UNKNOWN;

    @Column(name = "resolution_confidence", length = 32)
    private String resolutionConfidence;

    @Column(name = "fingerprint", nullable = false, unique = true, length = 64)
    private String fingerprint;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();
}
