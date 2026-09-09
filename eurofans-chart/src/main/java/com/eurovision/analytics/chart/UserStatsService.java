package com.eurovision.analytics.chart;

import com.eurovision.analytics.listening.ListeningEventRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Backs {@code /me} and {@code /stats}: a user's own rank and percentile
 * among active, chart-participating users (spec 10). Only users with
 * {@code active=true} and {@code chartParticipationEnabled=true} are counted
 * on either side of the computation.
 *
 * <p>Percentile formula (a standard "percentile rank"): among the N
 * participating users, if a given user is ranked R (1 = most plays), their
 * percentile is {@code 100 * (N - R) / (N - 1)} -- i.e. the share of other
 * participants they outperform. A lone participant (N = 1) is defined as the
 * 100th percentile.
 */
@Service
public class UserStatsService {

    private final ListeningEventRepository listeningEventRepository;

    public UserStatsService(ListeningEventRepository listeningEventRepository) {
        this.listeningEventRepository = listeningEventRepository;
    }

    public Optional<UserChartStanding> standingFor(long userId, ChartPeriod period) {
        Instant now = Instant.now();
        Instant windowStart = period.windowStart(now);
        // ALL_TIME's unbounded start must still be a concrete Instant bind value, never null
        // (see the comment on ListeningEventRepository's aggregate queries for why).
        Instant from = windowStart == null ? Instant.EPOCH : windowStart;

        List<ListeningEventRepository.UserListenCountRow> rows = listeningEventRepository
                .aggregateUserListenCounts(from, now);

        List<ListeningEventRepository.UserListenCountRow> ranked = rows.stream()
                .sorted(Comparator.comparingLong(ListeningEventRepository.UserListenCountRow::getListenCount).reversed())
                .toList();

        int totalParticipants = ranked.size();
        for (int i = 0; i < ranked.size(); i++) {
            if (ranked.get(i).getUserId() == userId) {
                int rank = i + 1;
                long listenCount = ranked.get(i).getListenCount();
                double percentile = totalParticipants <= 1
                        ? 100.0
                        : 100.0 * (totalParticipants - rank) / (totalParticipants - 1);
                return Optional.of(new UserChartStanding(listenCount, rank, totalParticipants, percentile));
            }
        }
        return Optional.empty();
    }
}
