package com.eurovision.analytics.chart;

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

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "chart_snapshots")
@Getter
@Setter
@NoArgsConstructor
public class ChartSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "period", nullable = false, length = 16)
    private ChartPeriod period;

    @Column(name = "period_start", nullable = false)
    private Instant periodStart;

    @Column(name = "period_end", nullable = false)
    private Instant periodEnd;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "artist_id", nullable = false)
    private EurovisionArtist artist;

    @Column(name = "rank", nullable = false)
    private int rank;

    @Column(name = "listen_count", nullable = false)
    private long listenCount;

    @Column(name = "unique_listeners", nullable = false)
    private long uniqueListeners;

    @Column(name = "growth_percent")
    private BigDecimal growthPercent;

    @Column(name = "generated_at", nullable = false)
    private Instant generatedAt = Instant.now();
}
