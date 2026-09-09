package com.eurovision.analytics.oauth.soundcloud;

import com.eurovision.analytics.config.ProviderProperties;
import com.eurovision.analytics.connectedaccount.ConnectedAccount;
import com.eurovision.analytics.connectedaccount.Provider;
import com.eurovision.analytics.listening.RecentPlay;
import com.eurovision.analytics.listening.RecentPlayProviderClient;
import com.eurovision.analytics.nowplaying.NowPlayingProviderClient;
import com.eurovision.analytics.nowplaying.NowPlayingSignal;
import com.eurovision.analytics.security.TokenEncryptionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * SoundCloud has neither an "artist" entity nor a genuine live now-playing
 * endpoint for third-party observers (spec 4.4, 6.2). {@link #fetchNowPlaying}
 * is therefore a documented best-effort heuristic: the most recent history
 * entry is treated as "currently playing" only if it was played within the
 * last two minutes. This is explicitly not a real-time signal.
 */
@Component
public class SoundCloudApiClient implements NowPlayingProviderClient, RecentPlayProviderClient {

    private static final Logger log = LoggerFactory.getLogger(SoundCloudApiClient.class);
    private static final Duration NOW_PLAYING_HEURISTIC_WINDOW = Duration.ofMinutes(2);

    private final WebClient soundcloudApiWebClient;
    private final TokenEncryptionService tokenEncryptionService;
    private final ProviderProperties.SoundCloud config;

    public SoundCloudApiClient(WebClient soundcloudApiWebClient, TokenEncryptionService tokenEncryptionService,
                                ProviderProperties properties) {
        this.soundcloudApiWebClient = soundcloudApiWebClient;
        this.tokenEncryptionService = tokenEncryptionService;
        this.config = properties.soundcloud();
    }

    /**
     * Resolves a public SoundCloud profile permalink URL to its numeric
     * uploader id -- the identity anchor used everywhere else in this
     * application (spec 6.2). Uses the app's own {@code client_id} rather
     * than any user's token, since admin artist import must not depend on an
     * admin having connected their own SoundCloud account.
     */
    public Optional<SoundCloudDtos.UserProfile> resolveByUrl(String permalinkUrl) {
        try {
            SoundCloudDtos.UserProfile profile = soundcloudApiWebClient.get()
                    .uri(uriBuilder -> uriBuilder.path("/resolve")
                            .queryParam("url", permalinkUrl)
                            .queryParam("client_id", config.clientId())
                            .build())
                    .retrieve()
                    .bodyToMono(SoundCloudDtos.UserProfile.class)
                    .block();
            return Optional.ofNullable(profile);
        } catch (Exception e) {
            log.warn("SoundCloud /resolve failed for url={}: {}", permalinkUrl, e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public Provider provider() {
        return Provider.SOUNDCLOUD;
    }

    public SoundCloudDtos.UserProfile fetchProfile(String accessToken) {
        return soundcloudApiWebClient.get()
                .uri("/me")
                .headers(h -> h.setBearerAuth(accessToken))
                .retrieve()
                .bodyToMono(SoundCloudDtos.UserProfile.class)
                .block();
    }

    public Optional<SoundCloudDtos.UserProfile> resolveUploaderById(long userId) {
        try {
            SoundCloudDtos.UserProfile profile = soundcloudApiWebClient.get()
                    .uri("/users/{id}", userId)
                    .retrieve()
                    .bodyToMono(SoundCloudDtos.UserProfile.class)
                    .block();
            return Optional.ofNullable(profile);
        } catch (Exception e) {
            log.warn("SoundCloud uploader lookup failed for userId={}: {}", userId, e.getMessage());
            return Optional.empty();
        }
    }

    private List<SoundCloudDtos.PlayHistoryEntry> fetchHistory(String accessToken) {
        try {
            SoundCloudDtos.PlayHistoryResponse response = soundcloudApiWebClient.get()
                    .uri("/me/play-history/tracks?limit=50")
                    .headers(h -> h.setBearerAuth(accessToken))
                    .retrieve()
                    .bodyToMono(SoundCloudDtos.PlayHistoryResponse.class)
                    .block();
            return response == null || response.collection() == null ? List.of() : response.collection();
        } catch (Exception e) {
            log.warn("SoundCloud play-history fetch failed (this endpoint requires SoundCloud-granted "
                    + "access per spec 4.4): {}", e.getMessage());
            return List.of();
        }
    }

    @Override
    public Optional<NowPlayingSignal> fetchNowPlaying(ConnectedAccount account) {
        String accessToken = tokenEncryptionService.decrypt(account.getAccessTokenEncrypted());
        List<SoundCloudDtos.PlayHistoryEntry> history = fetchHistory(accessToken);
        if (history.isEmpty()) {
            return Optional.empty();
        }
        SoundCloudDtos.PlayHistoryEntry mostRecent = history.get(0);
        if (mostRecent.playedAtEpochMillis() == null) {
            return Optional.empty();
        }
        Instant playedAt = Instant.ofEpochMilli(mostRecent.playedAtEpochMillis());
        if (playedAt.isBefore(Instant.now().minus(NOW_PLAYING_HEURISTIC_WINDOW))) {
            return Optional.empty();
        }
        return Optional.ofNullable(toSignal(mostRecent.track()));
    }

    @Override
    public List<RecentPlay> fetchRecentPlays(ConnectedAccount account) {
        String accessToken = tokenEncryptionService.decrypt(account.getAccessTokenEncrypted());
        List<RecentPlay> plays = new ArrayList<>();
        for (SoundCloudDtos.PlayHistoryEntry entry : fetchHistory(accessToken)) {
            if (entry.track() == null) {
                continue;
            }
            Instant playedAt = entry.playedAtEpochMillis() == null ? null : Instant.ofEpochMilli(entry.playedAtEpochMillis());
            plays.add(new RecentPlay(
                    entry.track().user() == null ? null : entry.track().user().username(),
                    entry.track().title(),
                    null,
                    Long.toString(entry.track().id()),
                    entry.track().user() == null ? null : Long.toString(entry.track().user().id()),
                    playedAt,
                    false,
                    originalArtworkUrl(entry.track())));
        }
        return plays;
    }

    private NowPlayingSignal toSignal(SoundCloudDtos.Track track) {
        if (track == null) {
            return null;
        }
        return new NowPlayingSignal(
                track.user() == null ? null : track.user().username(),
                track.title(),
                null,
                Long.toString(track.id()),
                track.user() == null ? null : Long.toString(track.user().id()),
                originalArtworkUrl(track));
    }

    /** Swap the "-large" suffix for "-original": the un-resized file the uploader actually uploaded (spec 9). */
    private String originalArtworkUrl(SoundCloudDtos.Track track) {
        if (track == null || track.artworkUrl() == null) {
            return null;
        }
        return track.artworkUrl().replace("-large", "-original");
    }
}
