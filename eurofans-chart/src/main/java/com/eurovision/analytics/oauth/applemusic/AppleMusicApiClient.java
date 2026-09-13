package com.eurovision.analytics.oauth.applemusic;

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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Apple Music has no public API for observing what a user is playing right
 * now (spec 4.3.8): {@link #fetchNowPlaying} therefore always reports no
 * signal, honestly, rather than approximating one. Recently-played tracks are
 * available but come back with no play timestamp at all, so every
 * {@link RecentPlay} from this client is marked {@code estimatedTimestamp}
 * with a {@code null} {@code playedAtUtc} -- never a guessed time.
 */
@Component
public class AppleMusicApiClient implements NowPlayingProviderClient, RecentPlayProviderClient {

    private static final Logger log = LoggerFactory.getLogger(AppleMusicApiClient.class);

    private final WebClient appleMusicApiWebClient;
    private final AppleMusicDeveloperTokenService developerTokenService;
    private final TokenEncryptionService tokenEncryptionService;

    public AppleMusicApiClient(WebClient appleMusicApiWebClient,
                                AppleMusicDeveloperTokenService developerTokenService,
                                TokenEncryptionService tokenEncryptionService) {
        this.appleMusicApiWebClient = appleMusicApiWebClient;
        this.developerTokenService = developerTokenService;
        this.tokenEncryptionService = tokenEncryptionService;
    }

    @Override
    public Provider provider() {
        return Provider.APPLE_MUSIC;
    }

    /**
     * Catalog artist lookup by storefront + numeric id, used only for admin
     * {@code /addartist} import. Requires only the developer token (no
     * end-user Music-User-Token), so it never depends on an admin having
     * connected their own Apple Music account.
     */
    public Optional<AppleMusicDtos.ArtistResource> lookupCatalogArtist(String storefront, String artistId) {
        try {
            AppleMusicDtos.ArtistsResponse response = appleMusicApiWebClient.get()
                    .uri("/v1/catalog/{storefront}/artists/{id}", storefront, artistId)
                    .headers(h -> h.setBearerAuth(developerTokenService.currentToken()))
                    .retrieve()
                    .bodyToMono(AppleMusicDtos.ArtistsResponse.class)
                    .block();
            if (response == null || response.data() == null || response.data().isEmpty()) {
                return Optional.empty();
            }
            return Optional.of(response.data().get(0));
        } catch (Exception e) {
            log.warn("Apple Music catalog artist lookup failed for storefront={} id={}: {}", storefront, artistId, e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public Optional<NowPlayingSignal> fetchNowPlaying(ConnectedAccount account) {
        // No third-party "now playing" endpoint exists for Apple Music; never guess.
        return Optional.empty();
    }

    @Override
    public List<RecentPlay> fetchRecentPlays(ConnectedAccount account) {
        String musicUserToken = tokenEncryptionService.decrypt(account.getAccessTokenEncrypted());
        List<RecentPlay> plays = new ArrayList<>();
        try {
            AppleMusicDtos.SongsResponse response = appleMusicApiWebClient.get()
                    .uri("/v1/me/recent/played/tracks?limit=30")
                    .headers(h -> {
                        h.setBearerAuth(developerTokenService.currentToken());
                        h.set("Music-User-Token", musicUserToken);
                    })
                    .retrieve()
                    .bodyToMono(AppleMusicDtos.SongsResponse.class)
                    .block();
            if (response == null || response.data() == null) {
                return plays;
            }
            for (AppleMusicDtos.SongResource song : response.data()) {
                if (song.attributes() == null) {
                    continue;
                }
                plays.add(new RecentPlay(
                        song.attributes().artistName(),
                        song.attributes().name(),
                        song.attributes().albumName(),
                        song.id(),
                        null,
                        null,
                        true,
                        song.attributes().artwork() == null ? null : song.attributes().artwork().resolvedUrl(),
                        List.of()));
            }
        } catch (Exception e) {
            log.warn("Apple Music recent-tracks fetch failed for connectedAccountId={}: {} "
                            + "(requires the user to have enabled 'Use Listening History' in system settings, spec 4.3.8)",
                    account.getId(), e.getMessage());
        }
        return plays;
    }
}
