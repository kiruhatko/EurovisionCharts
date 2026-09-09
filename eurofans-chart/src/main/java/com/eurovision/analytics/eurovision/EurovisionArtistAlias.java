package com.eurovision.analytics.eurovision;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

@Entity
@Table(name = "eurovision_artist_aliases")
@Getter
@Setter
@NoArgsConstructor
public class EurovisionArtistAlias {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // EAGER: EurovisionIdentityResolver reads .getArtist() outside a transaction.
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "artist_id", nullable = false)
    private EurovisionArtist artist;

    @Column(name = "alias", nullable = false)
    private String alias;

    @Column(name = "normalized_alias", nullable = false)
    private String normalizedAlias;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    public EurovisionArtistAlias(EurovisionArtist artist, String alias, String normalizedAlias) {
        this.artist = artist;
        this.alias = alias;
        this.normalizedAlias = normalizedAlias;
    }
}
