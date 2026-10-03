package com.eurovision.analytics.chartstudio;

import com.eurovision.analytics.artwork.ArtworkCache;
import com.eurovision.analytics.artwork.ArtworkCacheRepository;
import com.eurovision.analytics.chart.ChartEntry;
import com.eurovision.analytics.chart.ChartPeriod;
import com.eurovision.analytics.chart.ChartService;
import com.eurovision.analytics.eurovision.EurovisionArtist;
import com.eurovision.analytics.listening.ListeningEvent;
import com.eurovision.analytics.listening.ListeningEventRepository;
import com.eurovision.analytics.listening.ResolutionStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Turns the artist-level chart engine into what the chart-studio page needs
 * for one specific calendar week: rank, week-over-week movement, a streak of
 * how many consecutive weeks the artist has charted, a 7-day play trend, and
 * a cover pulled from the artist's most recently played track (never just an
 * artist photo -- spec: the cover must be the song's own artwork).
 */
@Service
public class ChartStudioService {

    private static final int TOP_N = 10;
    private static final int MAX_STREAK_LOOKBACK_WEEKS = 260;
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd.MM.yyyy", new Locale("uk"));

    private final ChartService chartService;
    private final ListeningEventRepository listeningEventRepository;
    private final ArtworkCacheRepository artworkCacheRepository;
    private final FlagCatalog flagCatalog;

    public ChartStudioService(ChartService chartService,
                               ListeningEventRepository listeningEventRepository,
                               ArtworkCacheRepository artworkCacheRepository,
                               FlagCatalog flagCatalog) {
        this.chartService = chartService;
        this.listeningEventRepository = listeningEventRepository;
        this.artworkCacheRepository = artworkCacheRepository;
        this.flagCatalog = flagCatalog;
    }

    @Transactional(readOnly = true)
    public ChartStudioDtos.Response chartForWeek(int weekOffset) {
        Instant now = Instant.now();
        LocalDate weekStartDate = mondayOf(now, weekOffset);
        Instant weekStart = weekStartDate.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant weekEnd = weekStartDate.plusWeeks(1).atStartOfDay(ZoneOffset.UTC).toInstant();

        LocalDate previousWeekStartDate = weekStartDate.minusWeeks(1);
        Instant previousWeekStart = previousWeekStartDate.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant previousWeekEnd = weekStartDate.atStartOfDay(ZoneOffset.UTC).toInstant();

        List<ChartEntry> current = chartService.computeChartForWindow(
                weekStart, weekEnd, previousWeekStart, previousWeekEnd, true, ChartPeriod.WEEK)
                .stream().limit(TOP_N).toList();

        Map<Long, Integer> previousRankByArtist = new HashMap<>();
        for (ChartEntry e : chartService.computeChartForWindow(
                previousWeekStart, previousWeekEnd, previousWeekStart, previousWeekStart, false, ChartPeriod.WEEK)) {
            previousRankByArtist.put(e.artist().getId(), e.rank());
        }

        List<ChartStudioDtos.Entry> entries = current.stream()
                .map(e -> toEntry(e, previousRankByArtist.get(e.artist().getId()), weekStart, weekEnd))
                .toList();

        String label = "Тиждень (" + weekStartDate.format(DATE_FMT) + " – " + weekStartDate.plusDays(6).format(DATE_FMT) + ")";
        return new ChartStudioDtos.Response(label, weekOffset, entries, availableWeeks(now));
    }

    private ChartStudioDtos.Entry toEntry(ChartEntry entry, Integer previousRank, Instant weekStart, Instant weekEnd) {
        EurovisionArtist artist = entry.artist();
        Object move = previousRank == null ? "new" : (previousRank - entry.rank());

        return new ChartStudioDtos.Entry(
                entry.rank(),
                artist.getCanonicalName(),
                artist.getCountry() == null ? null : artist.getCountry().getName(),
                flagCatalog.urlFor(artist.getCountry()),
                resolveCoverUrl(artist.getId()),
                move,
                weeksInChartStreak(artist.getId(), weekStart),
                entry.listenCount(),
                dailyTrend(artist.getId(), weekStart, weekEnd)
        );
    }

    private String resolveCoverUrl(long artistId) {
        return listeningEventRepository
                .findFirstByCanonicalArtist_IdAndResolutionStatusInOrderByPlayedAtUtcDesc(
                        artistId, java.util.List.of(ResolutionStatus.CONFIRMED, ResolutionStatus.PROBABLE))
                .flatMap(this::coverFromListeningEvent)
                .orElse(null);
    }

    private java.util.Optional<String> coverFromListeningEvent(ListeningEvent event) {
        String lookupKey = event.getProvider().name() + ":" + event.getRawArtistName() + ":" + event.getRawTrackName();
        return artworkCacheRepository.findByLookupKey(lookupKey).map(ArtworkCache::getOriginalUrl);
    }

    /** Consecutive weeks (including the requested one) this artist has had at least one confirmed play. */
    private int weeksInChartStreak(long artistId, Instant currentWeekStart) {
        int streak = 0;
        Instant windowStart = currentWeekStart;
        for (int i = 0; i < MAX_STREAK_LOOKBACK_WEEKS; i++) {
            Instant windowEnd = windowStart.plus(7, ChronoUnit.DAYS);
            long count = listeningEventRepository.countForArtistInWindow(artistId, windowStart, windowEnd);
            if (count <= 0) {
                break;
            }
            streak++;
            windowStart = windowStart.minus(7, ChronoUnit.DAYS);
        }
        return streak;
    }

    private long[] dailyTrend(long artistId, Instant weekStart, Instant weekEnd) {
        long[] days = new long[7];
        LocalDate weekStartDate = weekStart.atZone(ZoneOffset.UTC).toLocalDate();
        for (ListeningEventRepository.DailyCountRow row : listeningEventRepository.dailyCounts(artistId, weekStart, weekEnd)) {
            int index = (int) ChronoUnit.DAYS.between(weekStartDate, row.getDay());
            if (index >= 0 && index < 7) {
                days[index] = row.getCnt();
            }
        }
        return days;
    }

    private List<ChartStudioDtos.WeekOption> availableWeeks(Instant now) {
        List<ChartStudioDtos.WeekOption> options = new java.util.ArrayList<>();
        for (int offset = 0; offset < 12; offset++) {
            LocalDate start = mondayOf(now, offset);
            options.add(new ChartStudioDtos.WeekOption(
                    offset,
                    "Тиждень (" + start.format(DATE_FMT) + " – " + start.plusDays(6).format(DATE_FMT) + ")",
                    start.toString(),
                    start.plusDays(6).toString()));
        }
        return options;
    }

    private static LocalDate mondayOf(Instant now, int weeksAgo) {
        LocalDate today = now.atZone(ZoneOffset.UTC).toLocalDate();
        LocalDate thisMonday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        return thisMonday.minusWeeks(weeksAgo);
    }
}
