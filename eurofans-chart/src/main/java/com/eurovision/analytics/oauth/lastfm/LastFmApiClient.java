package com.eurovision.analytics.oauth.lastfm;

import com.eurovision.analytics.config.ProviderProperties;
import com.eurovision.analytics.connectedaccount.ConnectedAccount;
import com.eurovision.analytics.connectedaccount.Provider;
import com.eurovision.analytics.listening.RecentPlay;
import com.eurovision.analytics.listening.RecentPlayProviderClient;
import com.eurovision.analytics.nowplaying.NowPlayingProviderClient;
import com.eurovision.analytics.nowplaying.NowPlayingSignal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
public class LastFmApiClient implements NowPlayingProviderClient, RecentPlayProviderClient {

    private static final Logger log = LoggerFactory.getLogger(LastFmApiClient.class);

    private final WebClient lastfmApiWebClient;
    private final ProviderProperties.LastFm config;

    public LastFmApiClient(WebClient lastfmApiWebClient, ProviderProperties properties) {
        this.lastfmApiWebClient = lastfmApiWebClient;
        this.config = properties.lastfm();
    }

    @Override
    public Provider provider() {
        return Provider.LASTFM;
    }

    private List<LastFmDtos.TrackEntry> fetchRecentTracks(String username, int limit) {
        try {
            LastFmDtos.RecentTracksResponse response = lastfmApiWebClient.get()
                    .uri(uriBuilder -> uriBuilder.path("/2.0/")
                            .queryParam("method", "user.getrecenttracks")
                            .queryParam("user", username)
                            .queryParam("api_key", config.apiKey())
                            .queryParam("format", "json")
                            .queryParam("limit", limit)
                            .build())
                    .retrieve()
                    .bodyToMono(LastFmDtos.RecentTracksResponse.class)
                    .block();
            if (response == null || response.recenttracks() == null || response.recenttracks().track() == null) {
                return List.of();
            }
            return response.recenttracks().track();
        } catch (Exception e) {
            log.warn("Last.fm recent-tracks fetch failed for username={}: {}", username, e.getMessage());
            return List.of();
        }
    }

    @Override
    public Optional<NowPlayingSignal> fetchNowPlaying(ConnectedAccount account) {
        List<LastFmDtos.TrackEntry> tracks = fetchRecentTracks(account.getProviderAccountId(), 1);
        if (tracks.isEmpty() || !tracks.get(0).isNowPlaying()) {
            return Optional.empty();
        }
        LastFmDtos.TrackEntry track = tracks.get(0);
        return Optional.of(new NowPlayingSignal(
                track.artist() == null ? null : track.artist().text(),
                track.name(),
                track.album() == null ? null : track.album().text(),
                null,
                track.artist() == null ? null : track.artist().mbid(),
                track.largestImageUrl(),
                List.of()));
    }

    @Override
    public List<RecentPlay> fetchRecentPlays(ConnectedAccount account) {
        List<RecentPlay> plays = new ArrayList<>();
        for (LastFmDtos.TrackEntry track : fetchRecentTracks(account.getProviderAccountId(), 50)) {
            if (track.isNowPlaying()) {
                // the currently-playing entry has no timestamp and is never a completed play
                continue;
            }
            Instant playedAt = null;
            if (track.date() != null && track.date().epochSeconds() != null) {
                try {
                    playedAt = Instant.ofEpochSecond(Long.parseLong(track.date().epochSeconds()));
                } catch (NumberFormatException ignored) {
                    // leave null rather than guessing
                }
            }
            plays.add(new RecentPlay(
                    track.artist() == null ? null : track.artist().text(),
                    track.name(),
                    track.album() == null ? null : track.album().text(),
                    null,
                    track.artist() == null ? null : track.artist().mbid(),
                    playedAt,
                    false,
                    track.largestImageUrl(),
                    List.of()));
        }
        return plays;
    }
}
