package com.eurovision.analytics.chart;

import java.time.Duration;
import java.time.Instant;

public enum ChartPeriod {
    WEEK(Duration.ofDays(7)),
    MONTH(Duration.ofDays(30)),
    YEAR(Duration.ofDays(365)),
    ALL_TIME(null);

    private final Duration windowLength;

    ChartPeriod(Duration windowLength) {
        this.windowLength = windowLength;
    }

    /** Start of the current window, or {@code null} for ALL_TIME (unbounded). */
    public Instant windowStart(Instant now) {
        return windowLength == null ? null : now.minus(windowLength);
    }

    /** Start of the immediately preceding, equal-length window (for growth %). */
    public Instant previousWindowStart(Instant now) {
        return windowLength == null ? null : now.minus(windowLength.multipliedBy(2));
    }
}
