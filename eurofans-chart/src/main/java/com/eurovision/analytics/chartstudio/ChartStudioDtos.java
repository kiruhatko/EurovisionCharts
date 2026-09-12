package com.eurovision.analytics.chartstudio;

import java.util.List;

public final class ChartStudioDtos {

    private ChartStudioDtos() {
    }

    public record Entry(
            int rank,
            String artistName,
            String countryName,
            String flagUrl,
            String coverUrl,
            Object move,          // Integer delta, 0, or the string "new"
            int weeksInChart,
            long plays,
            long[] dailyTrend     // 7 entries, Monday..Sunday
    ) {
    }

    public record WeekOption(int weekOffset, String label, String periodStart, String periodEnd) {
    }

    public record Response(
            String periodLabel,
            int weekOffset,
            List<Entry> entries,
            List<WeekOption> availableWeeks
    ) {
    }
}
