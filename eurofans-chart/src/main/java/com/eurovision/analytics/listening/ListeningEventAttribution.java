package com.eurovision.analytics.listening;

import com.eurovision.analytics.eurovision.EurovisionArtist;
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

/**
 * One raw listening event may attribute to several Eurovision artists
 * (feat./collaboration) without multiplying the raw play count itself.
 */
@Entity
@Table(name = "listening_event_attributions")
@Getter
@Setter
@NoArgsConstructor
public class ListeningEventAttribution {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "listening_event_id", nullable = false)
    private ListeningEvent listeningEvent;

    // EAGER for consistency with the other Artist relations (see ListeningEvent.canonicalArtist).
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "eurovision_artist_id", nullable = false)
    private EurovisionArtist eurovisionArtist;

    @Enumerated(EnumType.STRING)
    @Column(name = "attribution_type", nullable = false, length = 32)
    private AttributionType attributionType;

    @Column(name = "confidence", nullable = false, length = 32)
    private String confidence;

    public ListeningEventAttribution(ListeningEvent listeningEvent, EurovisionArtist eurovisionArtist,
                                      AttributionType attributionType, String confidence) {
        this.listeningEvent = listeningEvent;
        this.eurovisionArtist = eurovisionArtist;
        this.attributionType = attributionType;
        this.confidence = confidence;
    }
}
