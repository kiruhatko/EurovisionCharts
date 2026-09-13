package com.eurovision.analytics.nowplaying;

import com.eurovision.analytics.connectedaccount.ConnectedAccount;
import com.eurovision.analytics.connectedaccount.ConnectedAccountService;
import com.eurovision.analytics.connectedaccount.Provider;
import com.eurovision.analytics.eurovision.ArtistStatus;
import com.eurovision.analytics.eurovision.EurovisionArtist;
import com.eurovision.analytics.eurovision.identity.EurovisionIdentityResolver;
import com.eurovision.analytics.eurovision.identity.IdentityResolution;
import com.eurovision.analytics.listening.ListeningEventRepository;
import com.eurovision.analytics.listening.ResolutionStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Verifies the strict Spotify -> Apple Music -> SoundCloud -> Last.fm chain
 * from spec 5.2: the first provider with ANY signal is final, whether or not
 * it turns out to be Eurovision-relevant.
 */
class NowPlayingResolverTest {

    private ConnectedAccountService connectedAccountService;
    private EurovisionIdentityResolver identityResolver;
    private ListeningEventRepository listeningEventRepository;
    private NowPlayingProviderClient spotifyClient;
    private NowPlayingProviderClient appleMusicClient;
    private NowPlayingProviderClient soundCloudClient;
    private NowPlayingProviderClient lastFmClient;
    private static final long USER_ID = 1L;

    @BeforeEach
    void setUp() {
        connectedAccountService = mock(ConnectedAccountService.class);
        identityResolver = mock(EurovisionIdentityResolver.class);
        listeningEventRepository = mock(ListeningEventRepository.class);
        spotifyClient = mockClient(Provider.SPOTIFY);
        appleMusicClient = mockClient(Provider.APPLE_MUSIC);
        soundCloudClient = mockClient(Provider.SOUNDCLOUD);
        lastFmClient = mockClient(Provider.LASTFM);
    }

    private NowPlayingProviderClient mockClient(Provider provider) {
        NowPlayingProviderClient client = mock(NowPlayingProviderClient.class);
        when(client.provider()).thenReturn(provider);
        return client;
    }

    private void connected(Provider... providers) {
        for (Provider provider : providers) {
            ConnectedAccount account = mock(ConnectedAccount.class);
            when(connectedAccountService.findActive(USER_ID, provider)).thenReturn(Optional.of(account));
        }
    }

    private NowPlayingResolver resolver() {
        return new NowPlayingResolver(connectedAccountService,
                List.of(spotifyClient, appleMusicClient, soundCloudClient, lastFmClient),
                identityResolver, listeningEventRepository);
    }

    @Test
    void spotifyActiveTakesPriorityAndNeverConsultsOthers() {
        connected(Provider.SPOTIFY, Provider.APPLE_MUSIC, Provider.SOUNDCLOUD, Provider.LASTFM);
        NowPlayingSignal signal = new NowPlayingSignal("Jamala", "1944", "1944", "trackid", "artistid", null, List.of());
        when(spotifyClient.fetchNowPlaying(any())).thenReturn(Optional.of(signal));

        EurovisionArtist artist = new EurovisionArtist();
        artist.setCanonicalName("Jamala");
        artist.setStatus(ArtistStatus.VERIFIED);
        artist.setActive(true);
        when(identityResolver.resolveAny(Provider.SPOTIFY, "artistid", "Jamala", List.of()))
                .thenReturn(new IdentityResolution(artist, "EXACT_PROVIDER_ID", ResolutionStatus.CONFIRMED, "HIGH"));

        NowPlayingResult result = resolver().resolveNowPlaying(USER_ID);

        assertThat(result).isInstanceOf(NowPlayingResult.EurovisionTrack.class);
        assertThat(((NowPlayingResult.EurovisionTrack) result).provider()).isEqualTo(Provider.SPOTIFY);
        verifyNoNonSpotifySignalFetches();
    }

    private void verifyNoNonSpotifySignalFetches() {
        org.mockito.Mockito.verify(appleMusicClient, org.mockito.Mockito.never()).fetchNowPlaying(any());
        org.mockito.Mockito.verify(soundCloudClient, org.mockito.Mockito.never()).fetchNowPlaying(any());
        org.mockito.Mockito.verify(lastFmClient, org.mockito.Mockito.never()).fetchNowPlaying(any());
    }

    @Test
    void spotifyPlayingNonEurovisionStopsChainWithoutCheckingSoundCloud() {
        connected(Provider.SPOTIFY, Provider.SOUNDCLOUD);
        NowPlayingSignal signal = new NowPlayingSignal("Some Random Band", "Some Song", null, "t1", "a1", null, List.of());
        when(spotifyClient.fetchNowPlaying(any())).thenReturn(Optional.of(signal));
        when(identityResolver.resolveAny(Provider.SPOTIFY, "a1", "Some Random Band", List.of()))
                .thenReturn(IdentityResolution.unknown());

        NowPlayingResult result = resolver().resolveNowPlaying(USER_ID);

        assertThat(result).isInstanceOf(NowPlayingResult.NonEurovision.class);
        org.mockito.Mockito.verify(soundCloudClient, org.mockito.Mockito.never()).fetchNowPlaying(any());
    }

    @Test
    void fallsThroughToAppleMusicWhenSpotifySilent() {
        connected(Provider.SPOTIFY, Provider.APPLE_MUSIC);
        when(spotifyClient.fetchNowPlaying(any())).thenReturn(Optional.empty());
        NowPlayingSignal signal = new NowPlayingSignal("Kalush Orchestra", "Stefania", null, "t2", "a2", null, List.of());
        when(appleMusicClient.fetchNowPlaying(any())).thenReturn(Optional.of(signal));

        EurovisionArtist artist = new EurovisionArtist();
        artist.setCanonicalName("Kalush Orchestra");
        when(identityResolver.resolveAny(Provider.APPLE_MUSIC, "a2", "Kalush Orchestra", List.of()))
                .thenReturn(new IdentityResolution(artist, "EXACT_PROVIDER_ID", ResolutionStatus.CONFIRMED, "HIGH"));

        NowPlayingResult result = resolver().resolveNowPlaying(USER_ID);

        assertThat(result).isInstanceOf(NowPlayingResult.EurovisionTrack.class);
        assertThat(((NowPlayingResult.EurovisionTrack) result).provider()).isEqualTo(Provider.APPLE_MUSIC);
    }

    @Test
    void nothingPlayingWhenAllConnectedProvidersAreSilent() {
        connected(Provider.SPOTIFY, Provider.LASTFM);
        when(spotifyClient.fetchNowPlaying(any())).thenReturn(Optional.empty());
        when(lastFmClient.fetchNowPlaying(any())).thenReturn(Optional.empty());

        NowPlayingResult result = resolver().resolveNowPlaying(USER_ID);

        assertThat(result).isInstanceOf(NowPlayingResult.NothingPlaying.class);
    }

    @Test
    void noConnectedProvidersMeansNothingPlaying() {
        NowPlayingResult result = resolver().resolveNowPlaying(USER_ID);
        assertThat(result).isInstanceOf(NowPlayingResult.NothingPlaying.class);
    }
}
