package com.eurovision.analytics.oauth.spotify;

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
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
public class SpotifyApiClient implements NowPlayingProviderClient, RecentPlayProviderClient {

    private static final Logger log = LoggerFactory.getLogger(SpotifyApiClient.class);

    private final WebClient spotifyApiWebClient;
    private final TokenEncryptionService tokenEncryptionService;

    public SpotifyApiClient(WebClient spotifyApiWebClient, TokenEncryptionService tokenEncryptionService) {
        this.spotifyApiWebClient = spotifyApiWebClient;
        this.tokenEncryptionService = tokenEncryptionService;
    }

    @Override
    public Provider provider() {
        return Provider.SPOTIFY;
    }

    public SpotifyDtos.UserProfile fetchProfile(String accessToken) {
        return spotifyApiWebClient.get()
                .uri("/v1/me")
                .headers(h -> h.setBearerAuth(accessToken))
                .retrieve()
                .bodyToMono(SpotifyDtos.UserProfile.class)
                .block();
    }

    public Optional<SpotifyDtos.ArtistDetail> fetchArtist(String accessToken, String artistId) {
        try {
            SpotifyDtos.ArtistDetail detail = spotifyApiWebClient.get()
                    .uri("/v1/artists/{id}", artistId)
                    .headers(h -> h.setBearerAuth(accessToken))
                    .retrieve()
                    .bodyToMono(SpotifyDtos.ArtistDetail.class)
                    .block();
            return Optional.ofNullable(detail);
        } catch (WebClientResponseException e) {
            log.warn("Spotify artist lookup failed for {}: {}", artistId, e.getStatusCode());
            return Optional.empty();
        }
    }

    @Override
    public Optional<NowPlayingSignal> fetchNowPlaying(ConnectedAccount account) {
        String accessToken = tokenEncryptionService.decrypt(account.getAccessTokenEncrypted());
        try {
            SpotifyDtos.CurrentlyPlaying response = spotifyApiWebClient.get()
                    .uri("/v1/me/player/currently-playing")
                    .headers(h -> h.setBearerAuth(accessToken))
                    .retrieve()
                    .bodyToMono(SpotifyDtos.CurrentlyPlaying.class)
                    .blockOptional()
                    .orElse(null);

            if (response == null || !response.isPlaying() || response.item() == null) {
                return Optional.empty();
            }
            return Optional.of(toSignal(response.item()));
        } catch (WebClientResponseException.Unauthorized e) {
            log.warn("Spotify access token invalid/expired for connectedAccountId={}", account.getId());
            return Optional.empty();
        } catch (Exception e) {
            log.warn("Spotify now-playing lookup failed for connectedAccountId={}: {}", account.getId(), e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public List<RecentPlay> fetchRecentPlays(ConnectedAccount account) {
        String accessToken = tokenEncryptionService.decrypt(account.getAccessTokenEncrypted());
        List<RecentPlay> plays = new ArrayList<>();
        try {
            SpotifyDtos.RecentlyPlayedResponse response = spotifyApiWebClient.get()
                    .uri(uriBuilder -> uriBuilder.path("/v1/me/player/recently-played").queryParam("limit", 50).build())
                    .headers(h -> h.setBearerAuth(accessToken))
                    .retrieve()
                    .bodyToMono(SpotifyDtos.RecentlyPlayedResponse.class)
                    .block();
            if (response == null || response.items() == null) {
                return plays;
            }
            for (SpotifyDtos.PlayHistoryItem item : response.items()) {
                if (item.track() == null) {
                    continue;
                }
                Instant playedAt = null;
                try {
                    playedAt = Instant.parse(item.playedAt());
                } catch (Exception ignored) {
                    // malformed timestamp from Spotify: leave null rather than guessing
                }
                plays.add(toRecentPlay(item.track(), playedAt));
            }
        } catch (Exception e) {
            log.warn("Spotify recently-played sync failed for connectedAccountId={}: {}", account.getId(), e.getMessage());
        }
        return plays;
    }

    private NowPlayingSignal toSignal(SpotifyDtos.Track track) {
        SpotifyDtos.ArtistRef primaryArtist = track.artists() == null || track.artists().isEmpty() ? null : track.artists().get(0);
        return new NowPlayingSignal(
                primaryArtist == null ? null : primaryArtist.name(),
                track.name(),
                track.album() == null ? null : track.album().name(),
                track.id(),
                primaryArtist == null ? null : primaryArtist.id());
    }

    private RecentPlay toRecentPlay(SpotifyDtos.Track track, Instant playedAt) {
        SpotifyDtos.ArtistRef primaryArtist = track.artists() == null || track.artists().isEmpty() ? null : track.artists().get(0);
        return new RecentPlay(
                primaryArtist == null ? null : primaryArtist.name(),
                track.name(),
                track.album() == null ? null : track.album().name(),
                track.id(),
                primaryArtist == null ? null : primaryArtist.id(),
                playedAt,
                false);
    }

    /** Largest available image, i.e. the highest {@code width} Spotify returned for this track's album art. */
    public Optional<SpotifyDtos.Image> largestImage(SpotifyDtos.Track track) {
        if (track.album() == null || track.album().images() == null || track.album().images().isEmpty()) {
            return Optional.empty();
        }
        return track.album().images().stream()
                .max((a, b) -> Integer.compare(
                        a.width() == null ? 0 : a.width(),
                        b.width() == null ? 0 : b.width()));
    }
}
