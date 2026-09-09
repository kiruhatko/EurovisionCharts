package com.eurovision.analytics.eurovision.identity;

import com.eurovision.analytics.connectedaccount.Provider;
import com.eurovision.analytics.eurovision.ArtistStatus;
import com.eurovision.analytics.eurovision.EurovisionArtist;
import com.eurovision.analytics.eurovision.EurovisionArtistAliasRepository;
import com.eurovision.analytics.eurovision.EurovisionArtistExternalId;
import com.eurovision.analytics.eurovision.EurovisionArtistExternalIdRepository;
import com.eurovision.analytics.eurovision.EurovisionArtistRepository;
import com.eurovision.analytics.eurovision.ExternalIdProvider;
import com.eurovision.analytics.listening.ResolutionStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EurovisionIdentityResolverTest {

    private EurovisionArtistExternalIdRepository externalIdRepository;
    private EurovisionArtistAliasRepository aliasRepository;
    private EurovisionArtistRepository artistRepository;
    private UnresolvedArtistQueueService unresolvedArtistQueueService;
    private EurovisionIdentityResolver resolver;

    @BeforeEach
    void setUp() {
        externalIdRepository = mock(EurovisionArtistExternalIdRepository.class);
        aliasRepository = mock(EurovisionArtistAliasRepository.class);
        artistRepository = mock(EurovisionArtistRepository.class);
        unresolvedArtistQueueService = mock(UnresolvedArtistQueueService.class);
        resolver = new EurovisionIdentityResolver(externalIdRepository, aliasRepository, artistRepository,
                unresolvedArtistQueueService);
    }

    private EurovisionArtist verifiedArtist(long id, String name) {
        EurovisionArtist artist = new EurovisionArtist();
        artist.setId(id);
        artist.setCanonicalName(name);
        artist.setStatus(ArtistStatus.VERIFIED);
        artist.setActive(true);
        return artist;
    }

    @Test
    void soundCloudMatchesOnlyByExactUploaderId() {
        EurovisionArtist artist = verifiedArtist(1L, "Go_A");
        EurovisionArtistExternalId link = new EurovisionArtistExternalId(artist, ExternalIdProvider.SOUNDCLOUD, "999", null, true);
        when(externalIdRepository.findByProviderAndExternalId(ExternalIdProvider.SOUNDCLOUD, "999"))
                .thenReturn(Optional.of(link));

        IdentityResolution result = resolver.resolve(Provider.SOUNDCLOUD, "999", "Go_A (official upload)");

        assertThat(result.status()).isEqualTo(ResolutionStatus.CONFIRMED);
        assertThat(result.canonicalArtist()).isSameAs(artist);
    }

    @Test
    void soundCloudWrongUploaderNeverFallsBackToNameMatching() {
        // Even if a raw name looks exactly like a known Eurovision artist, SoundCloud
        // must never resolve via text matching (spec 6.2) -- only the uploader id counts.
        when(externalIdRepository.findByProviderAndExternalId(ExternalIdProvider.SOUNDCLOUD, "111"))
                .thenReturn(Optional.empty());

        IdentityResolution result = resolver.resolve(Provider.SOUNDCLOUD, "111", "Go_A");

        assertThat(result.status()).isEqualTo(ResolutionStatus.UNKNOWN);
        assertThat(result.canonicalArtist()).isNull();
        verify(aliasRepository, never()).findByNormalizedAliasAndActiveTrue(org.mockito.ArgumentMatchers.anyString());
        verify(unresolvedArtistQueueService).recordOccurrence(Provider.SOUNDCLOUD, "111", "Go_A");
    }

    @Test
    void spotifyExactProviderIdIsConfirmed() {
        EurovisionArtist artist = verifiedArtist(2L, "Kalush Orchestra");
        EurovisionArtistExternalId link = new EurovisionArtistExternalId(artist, ExternalIdProvider.SPOTIFY, "spotify-id", null, true);
        when(externalIdRepository.findByProviderAndExternalId(ExternalIdProvider.SPOTIFY, "spotify-id"))
                .thenReturn(Optional.of(link));

        IdentityResolution result = resolver.resolve(Provider.SPOTIFY, "spotify-id", "Kalush Orchestra");

        assertThat(result.status()).isEqualTo(ResolutionStatus.CONFIRMED);
        assertThat(result.method()).isEqualTo("EXACT_PROVIDER_ID");
    }

    @Test
    void pendingArtistIsNotChartEligibleEvenWithExactIdMatch() {
        EurovisionArtist pending = new EurovisionArtist();
        pending.setId(3L);
        pending.setStatus(ArtistStatus.PENDING);
        pending.setActive(true);
        EurovisionArtistExternalId link = new EurovisionArtistExternalId(pending, ExternalIdProvider.SPOTIFY, "pid", null, false);
        when(externalIdRepository.findByProviderAndExternalId(ExternalIdProvider.SPOTIFY, "pid"))
                .thenReturn(Optional.of(link));
        when(aliasRepository.findByNormalizedAliasAndActiveTrue(org.mockito.ArgumentMatchers.anyString()))
                .thenReturn(List.of());
        when(artistRepository.findByNormalizedName(org.mockito.ArgumentMatchers.anyString()))
                .thenReturn(List.of());

        IdentityResolution result = resolver.resolve(Provider.SPOTIFY, "pid", "Some Artist");

        assertThat(result.status()).isEqualTo(ResolutionStatus.UNKNOWN);
    }

    @Test
    void ambiguousAliasMatchesAreNeverGuessed() {
        when(externalIdRepository.findByProviderAndExternalId(ExternalIdProvider.LASTFM, null)).thenReturn(Optional.empty());
        EurovisionArtist a = verifiedArtist(4L, "Artist A");
        EurovisionArtist b = verifiedArtist(5L, "Artist B");
        com.eurovision.analytics.eurovision.EurovisionArtistAlias aliasA =
                new com.eurovision.analytics.eurovision.EurovisionArtistAlias(a, "Same Name", "same name");
        com.eurovision.analytics.eurovision.EurovisionArtistAlias aliasB =
                new com.eurovision.analytics.eurovision.EurovisionArtistAlias(b, "Same Name", "same name");
        when(aliasRepository.findByNormalizedAliasAndActiveTrue("same name")).thenReturn(List.of(aliasA, aliasB));

        IdentityResolution result = resolver.resolve(Provider.LASTFM, null, "Same Name");

        assertThat(result.status()).isEqualTo(ResolutionStatus.CONFLICT);
        assertThat(result.canonicalArtist()).isNull();
    }
}
