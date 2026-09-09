package com.eurovision.analytics.eurovision;

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

@Entity
@Table(name = "eurovision_artist_external_ids")
@Getter
@Setter
@NoArgsConstructor
public class EurovisionArtistExternalId {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // EAGER: EurovisionIdentityResolver reads .getArtist() outside a transaction.
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "artist_id", nullable = false)
    private EurovisionArtist artist;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false, length = 32)
    private ExternalIdProvider provider;

    @Column(name = "external_id", nullable = false)
    private String externalId;

    @Column(name = "external_url")
    private String externalUrl;

    @Column(name = "verified", nullable = false)
    private boolean verified = false;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public EurovisionArtistExternalId(EurovisionArtist artist, ExternalIdProvider provider,
                                       String externalId, String externalUrl, boolean verified) {
        this.artist = artist;
        this.provider = provider;
        this.externalId = externalId;
        this.externalUrl = externalUrl;
        this.verified = verified;
    }
}
