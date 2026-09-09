package com.eurovision.analytics.chart;

import com.eurovision.analytics.eurovision.EurovisionArtist;
import com.eurovision.analytics.eurovision.EurovisionArtistRepository;
import com.eurovision.analytics.listening.ListeningEventRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Chart ranking is a straight {@code COUNT(confirmed listening events) DESC},
 * never an opaque weighted score (spec 10). Every provider a user has
 * connected contributes together here -- unlike {@code NowPlayingResolver},
 * there is no provider priority in the chart itself.
 */
@Service
public class ChartService {

    private final ListeningEventRepository listeningEventRepository;
    private final EurovisionArtistRepository artistRepository;
    private final ChartSnapshotRepository chartSnapshotRepository;

    public ChartService(ListeningEventRepository listeningEventRepository,
                         EurovisionArtistRepository artistRepository,
                         ChartSnapshotRepository chartSnapshotRepository) {
        this.listeningEventRepository = listeningEventRepository;
        this.artistRepository = artistRepository;
        this.chartSnapshotRepository = chartSnapshotRepository;
    }

    public List<ChartEntry> computeChart(ChartPeriod period) {
        Instant now = Instant.now();
        Instant from = orEpoch(period.windowStart(now));
        Instant previousFrom = orEpoch(period.previousWindowStart(now));

        List<ListeningEventRepository.ChartAggregationRow> rows = listeningEventRepository.aggregateForPeriod(from, now);
        if (rows.isEmpty()) {
            return List.of();
        }

        Map<Long, EurovisionArtist> artistsById = artistRepository
                .findAllById(rows.stream().map(ListeningEventRepository.ChartAggregationRow::getArtistId).toList())
                .stream()
                .collect(Collectors.toMap(EurovisionArtist::getId, a -> a));

        record Unranked(EurovisionArtist artist, long listenCount, long uniqueListeners,
                         Instant mostRecentPlayAt, BigDecimal growthPercent) {
        }

        List<Unranked> unranked = rows.stream()
                .map(row -> {
                    EurovisionArtist artist = artistsById.get(row.getArtistId());
                    if (artist == null) {
                        return null;
                    }
                    long previousCount = period.previousWindowStart(now) == null
                            ? 0L
                            : listeningEventRepository.countForArtistInWindow(artist.getId(), previousFrom, from);
                    BigDecimal growth = computeGrowthPercent(row.getListenCount(), previousCount, period);
                    return new Unranked(artist, row.getListenCount(), row.getUniqueListeners(),
                            row.getMostRecentPlayAt(), growth);
                })
                .filter(java.util.Objects::nonNull)
                .sorted(Comparator
                        .comparingLong(Unranked::listenCount).reversed()
                        .thenComparing(u -> u.mostRecentPlayAt() == null ? Instant.EPOCH : u.mostRecentPlayAt(),
                                Comparator.reverseOrder())
                        .thenComparingLong(Unranked::uniqueListeners).reversed()
                        .thenComparing(u -> u.artist().getCanonicalName(), Comparator.naturalOrder()))
                .toList();

        return java.util.stream.IntStream.range(0, unranked.size())
                .mapToObj(i -> {
                    Unranked u = unranked.get(i);
                    return new ChartEntry(i + 1, u.artist(), u.listenCount(), u.uniqueListeners(), u.growthPercent());
                })
                .toList();
    }

    /** ALL_TIME's unbounded window start, expressed as a concrete Instant for the JPQL bind parameter. */
    private static Instant orEpoch(Instant instant) {
        return instant == null ? Instant.EPOCH : instant;
    }

    /** Explicit, documented division-by-zero handling: never Infinity/NaN (spec 10). */
    private BigDecimal computeGrowthPercent(long current, long previous, ChartPeriod period) {
        if (period == ChartPeriod.ALL_TIME) {
            return null; // growth is meaningless without a bounded comparison window
        }
        if (previous == 0 && current == 0) {
            return BigDecimal.ZERO;
        }
        if (previous == 0) {
            return null; // undefined ("infinite") growth from a zero baseline: reported as unavailable, not Infinity
        }
        return BigDecimal.valueOf(current - previous)
                .divide(BigDecimal.valueOf(previous), 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP);
    }

    /** Daily snapshot of every period's current standings, preserved forever (spec 10). */
    @Scheduled(fixedRate = 24, timeUnit = TimeUnit.HOURS)
    @Transactional
    public void snapshotAllPeriods() {
        Instant now = Instant.now();
        for (ChartPeriod period : ChartPeriod.values()) {
            List<ChartEntry> chart = computeChart(period);
            Instant from = period.windowStart(now);
            for (ChartEntry entry : chart) {
                ChartSnapshot snapshot = new ChartSnapshot();
                snapshot.setPeriod(period);
                snapshot.setPeriodStart(from == null ? Instant.EPOCH : from);
                snapshot.setPeriodEnd(now);
                snapshot.setArtist(entry.artist());
                snapshot.setRank(entry.rank());
                snapshot.setListenCount(entry.listenCount());
                snapshot.setUniqueListeners(entry.uniqueListeners());
                snapshot.setGrowthPercent(entry.growthPercent());
                chartSnapshotRepository.save(snapshot);
            }
        }
    }
}
