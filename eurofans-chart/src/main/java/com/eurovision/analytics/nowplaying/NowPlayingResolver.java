package com.eurovision.analytics.nowplaying;

import com.eurovision.analytics.connectedaccount.ConnectedAccount;
import com.eurovision.analytics.connectedaccount.ConnectedAccountService;
import com.eurovision.analytics.connectedaccount.Provider;
import com.eurovision.analytics.eurovision.identity.EurovisionIdentityResolver;
import com.eurovision.analytics.eurovision.identity.IdentityResolution;
import com.eurovision.analytics.listening.ListeningEventRepository;
import com.eurovision.analytics.listening.ResolutionStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Implements the strict Spotify -> Apple Music -> SoundCloud -> Last.fm
 * fallback chain from spec section 5.2. This is deliberately a sequential
 * "first provider with any signal wins" chain, never a "pick the best match
 * across all providers" search: predictability is the point.
 */
@Service
public class NowPlayingResolver {

    private static final Logger log = LoggerFactory.getLogger(NowPlayingResolver.class);

    private final ConnectedAccountService connectedAccountService;
    private final Map<Provider, NowPlayingProviderClient> nowPlayingClients;
    private final EurovisionIdentityResolver identityResolver;
    private final ListeningEventRepository listeningEventRepository;

    public NowPlayingResolver(ConnectedAccountService connectedAccountService,
                               List<NowPlayingProviderClient> nowPlayingClients,
                               EurovisionIdentityResolver identityResolver,
                               ListeningEventRepository listeningEventRepository) {
        this.connectedAccountService = connectedAccountService;
        this.nowPlayingClients = nowPlayingClients.stream()
                .collect(Collectors.toMap(NowPlayingProviderClient::provider, Function.identity()));
        this.identityResolver = identityResolver;
        this.listeningEventRepository = listeningEventRepository;
    }

    public NowPlayingResult resolveNowPlaying(long userId) {
        for (Provider provider : Provider.PRIORITY_ORDER) {
            Optional<ConnectedAccount> account = connectedAccountService.findActive(userId, provider);
            if (account.isEmpty()) {
                continue;
            }
            NowPlayingProviderClient client = nowPlayingClients.get(provider);
            if (client == null) {
                continue;
            }

            Optional<NowPlayingSignal> signal;
            try {
                signal = client.fetchNowPlaying(account.get());
            } catch (Exception e) {
                log.warn("Now-playing lookup errored for provider={}, userId={}: {}", provider, userId, e.getMessage());
                signal = Optional.empty();
            }

            if (signal.isEmpty()) {
                // No live signal from this provider: move on to the next one in priority order.
                continue;
            }

            // A signal was found: this IS the final answer, whether or not it turns out
            // to be Eurovision-relevant. We never look further down the chain from here.
            IdentityResolution resolution = identityResolver.resolve(
                    provider, signal.get().providerArtistId(), signal.get().rawArtistName());
            if (resolution.status() == ResolutionStatus.CONFIRMED) {
                return new NowPlayingResult.EurovisionTrack(provider, signal.get(), resolution.canonicalArtist());
            }
            return new NowPlayingResult.NonEurovision(provider);
        }
        return new NowPlayingResult.NothingPlaying();
    }

    public LastTrackResult resolveLastTrack(long userId) {
        for (Provider provider : Provider.PRIORITY_ORDER) {
            Optional<ConnectedAccount> account = connectedAccountService.findActive(userId, provider);
            if (account.isEmpty()) {
                continue;
            }
            var last = listeningEventRepository
                    .findFirstByUserIdAndProviderAndCanonicalArtistIdIsNotNullAndResolutionStatusOrderByPlayedAtUtcDesc(
                            userId, provider, ResolutionStatus.CONFIRMED);
            if (last.isPresent()) {
                return new LastTrackResult.Found(last.get());
            }
        }
        return new LastTrackResult.NothingFound();
    }
}
