package com.eurovision.analytics.chart;

import com.eurovision.analytics.eurovision.EurovisionArtist;

import java.math.BigDecimal;

public record ChartEntry(int rank, EurovisionArtist artist, long listenCount, long uniqueListeners,
                          BigDecimal growthPercent) {
}
