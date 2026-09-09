package com.eurovision.analytics.eurovision.identity;

import com.eurovision.analytics.connectedaccount.Provider;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "unresolved_artist_entities")
@Getter
@Setter
@NoArgsConstructor
public class UnresolvedArtistEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "raw_name", nullable = false)
    private String rawName;

    @Column(name = "raw_external_id")
    private String rawExternalId;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false, length = 32)
    private Provider provider;

    @Column(name = "first_seen_at", nullable = false)
    private Instant firstSeenAt = Instant.now();

    @Column(name = "last_seen_at", nullable = false)
    private Instant lastSeenAt = Instant.now();

    @Column(name = "occurrences", nullable = false)
    private int occurrences = 1;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private UnresolvedStatus status = UnresolvedStatus.PENDING_REVIEW;

    @Column(name = "possible_matches", columnDefinition = "text")
    private String possibleMatches;

    @Column(name = "notes", columnDefinition = "text")
    private String notes;

    public UnresolvedArtistEntity(String rawName, String rawExternalId, Provider provider) {
        this.rawName = rawName;
        this.rawExternalId = rawExternalId;
        this.provider = provider;
    }

    public void recordAnotherOccurrence() {
        this.occurrences++;
        this.lastSeenAt = Instant.now();
    }
}
