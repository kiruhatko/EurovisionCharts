package com.eurovision.analytics.chart;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface ChartSnapshotRepository extends JpaRepository<ChartSnapshot, Long> {

    List<ChartSnapshot> findByPeriodAndPeriodStartAndPeriodEndOrderByRankAsc(
            ChartPeriod period, Instant periodStart, Instant periodEnd);
}
