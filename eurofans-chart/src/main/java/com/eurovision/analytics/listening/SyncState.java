package com.eurovision.analytics.listening;

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
@Table(name = "sync_states")
@Getter
@Setter
@NoArgsConstructor
public class SyncState {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false, length = 32)
    private Provider provider;

    @Column(name = "last_synced_at")
    private Instant lastSyncedAt;

    @Column(name = "last_cursor")
    private String lastCursor;

    @Column(name = "last_status", nullable = false, length = 32)
    private String lastStatus = "PENDING";

    @Column(name = "consecutive_failures", nullable = false)
    private int consecutiveFailures = 0;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public SyncState(Long userId, Provider provider) {
        this.userId = userId;
        this.provider = provider;
    }
}
