package com.eurovision.analytics.eurovision.identity;

import com.eurovision.analytics.connectedaccount.Provider;
import com.eurovision.analytics.eurovision.ArtistStatus;
import com.eurovision.analytics.eurovision.EurovisionArtist;
import com.eurovision.analytics.eurovision.EurovisionArtistAlias;
import com.eurovision.analytics.eurovision.EurovisionArtistAliasRepository;
import com.eurovision.analytics.eurovision.EurovisionArtistExternalId;
import com.eurovision.analytics.eurovision.EurovisionArtistExternalIdRepository;
import com.eurovision.analytics.eurovision.ExternalIdProvider;
import com.eurovision.analytics.listening.ResolutionStatus;
import com.eurovision.analytics.util.NameNormalizer;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Resolves a raw listening event against admin-supplied links only: an exact provider id
 * (Spotify/Apple Music/SoundCloud id, or a Last.fm MBID), the exact Last.fm artist page that was
 * linked, or an admin-curated alias. There is deliberately no fallback that matches a play by
 * guessing from the artist's name -- an artist without a link for that provider simply doesn't
 * resolve there. Any ambiguity or miss goes to {@link UnresolvedArtistQueueService}.
 */
@Service
public class EurovisionIdentityResolver {

    private final EurovisionArtistExternalIdRepository externalIdRepository;
    private final EurovisionArtistAliasRepository aliasRepository;
    private final UnresolvedArtistQueueService unresolvedArtistQueueService;

    public EurovisionIdentityResolver(EurovisionArtistExternalIdRepository externalIdRepository,
                                       EurovisionArtistAliasRepository aliasRepository,
                                       UnresolvedArtistQueueService unresolvedArtistQueueService) {
        this.externalIdRepository = externalIdRepository;
        this.aliasRepository = aliasRepository;
        this.unresolvedArtistQueueService = unresolvedArtistQueueService;
    }

    /**
     * Same as {@link #resolve} but also checks every other artist credited on the
     * track (feat./collab) for an exact provider-ID match first -- so a collab track
     * resolves as long as ANY credited artist is registered, never by guessing which one.
     */
    public IdentityResolution resolveAny(Provider provider, String primaryProviderArtistId, String primaryRawArtistName,
                                          String rawTrackName, String rawAlbumName, List<ArtistCredit> additionalArtists) {
        ExternalIdProvider externalIdProvider = toExternalIdProvider(provider);
        for (ArtistCredit credit : additionalArtists) {
            if (credit.providerArtistId() == null || credit.providerArtistId().isBlank()) {
                continue;
            }
            Optional<EurovisionArtist> exact = externalIdRepository
                    .findByProviderAndExternalId(externalIdProvider, credit.providerArtistId())
                    .map(EurovisionArtistExternalId::getArtist)
                    .filter(this::isChartEligible);
            if (exact.isPresent()) {
                return new IdentityResolution(exact.get(), "EXACT_PROVIDER_ID_FEATURED", ResolutionStatus.CONFIRMED, "HIGH");
            }
        }
        return resolve(provider, primaryProviderArtistId, primaryRawArtistName, rawTrackName, rawAlbumName);
    }

    public IdentityResolution resolve(Provider provider, String providerArtistId, String rawArtistName) {
        return resolve(provider, providerArtistId, rawArtistName, null, null);
    }

    public IdentityResolution resolve(Provider provider, String providerArtistId, String rawArtistName,
                                      String rawTrackName, String rawAlbumName) {
        ExternalIdProvider externalIdProvider = toExternalIdProvider(provider);

        if (providerArtistId != null && !providerArtistId.isBlank()) {
            Optional<EurovisionArtist> exact = externalIdRepository
                    .findByProviderAndExternalId(externalIdProvider, providerArtistId)
                    .map(EurovisionArtistExternalId::getArtist)
                    .filter(this::isChartEligible);
            if (exact.isPresent()) {
                return new IdentityResolution(exact.get(), "EXACT_PROVIDER_ID", ResolutionStatus.CONFIRMED, "HIGH");
            }
        }

        // SoundCloud has no artist name to fall back on: the uploader id IS the identity,
        // and text-matching a track title/uploader display name is explicitly disallowed (spec 6.2).
        if (provider == Provider.SOUNDCLOUD) {
            unresolvedArtistQueueService.recordOccurrence(provider, providerArtistId, rawArtistName);
            return IdentityResolution.unknown();
        }

        if (rawArtistName == null || rawArtistName.isBlank()) {
            unresolvedArtistQueueService.recordOccurrence(provider, providerArtistId, rawArtistName);
            return IdentityResolution.unknown();
        }

        // A scrobble's artist name IS the Last.fm page it belongs to, so these are exact matches
        // against the linked song or page (Last.fm usually omits the MBID, so the id check above
        // rarely hits). A song link is more specific than a page link, so it wins.
        if (provider == Provider.LASTFM) {
            Optional<EurovisionArtist> song = findLastFmSong(rawArtistName, rawTrackName)
                    .or(() -> findLastFmSong(rawArtistName, rawAlbumName));
            if (song.isPresent()) {
                return new IdentityResolution(song.get(), "EXACT_LASTFM_SONG", ResolutionStatus.CONFIRMED, "HIGH");
            }
            Optional<EurovisionArtist> page = findLastFm(LastFmProviderArtistResolver.pageKey(rawArtistName));
            if (page.isPresent()) {
                return new IdentityResolution(page.get(), "EXACT_LASTFM_PAGE", ResolutionStatus.CONFIRMED, "HIGH");
            }
        }

        String normalized = NameNormalizer.normalize(rawArtistName);

        List<EurovisionArtist> aliasCandidates = aliasRepository.findByNormalizedAliasAndActiveTrue(normalized).stream()
                .map(EurovisionArtistAlias::getArtist)
                .filter(this::isChartEligible)
                .distinct()
                .toList();
        if (aliasCandidates.size() == 1) {
            return new IdentityResolution(aliasCandidates.get(0), "ALIAS_MATCH", ResolutionStatus.PROBABLE, "MEDIUM");
        }
        if (aliasCandidates.size() > 1) {
            unresolvedArtistQueueService.recordOccurrence(provider, providerArtistId, rawArtistName);
            return IdentityResolution.conflict();
        }

        unresolvedArtistQueueService.recordOccurrence(provider, providerArtistId, rawArtistName);
        return IdentityResolution.unknown();
    }

    private Optional<EurovisionArtist> findLastFm(String key) {
        return externalIdRepository.findByProviderAndExternalId(ExternalIdProvider.LASTFM, key)
                .map(EurovisionArtistExternalId::getArtist)
                .filter(this::isChartEligible);
    }

    /**
     * Matches a song-scoped Last.fm link. The scrobbled title only has to START with the linked
     * title on a word boundary, so "Wasted Love - Eurovision 2025" still matches "Wasted Love"
     * while "Milano" never matches "Mila".
     */
    private Optional<EurovisionArtist> findLastFmSong(String rawArtistName, String rawTitle) {
        if (rawTitle == null || rawTitle.isBlank()) {
            return Optional.empty();
        }
        String normalizedTitle = NameNormalizer.normalize(rawTitle);
        if (normalizedTitle.isEmpty()) {
            return Optional.empty();
        }
        String[] words = normalizedTitle.split(" ");
        for (int length = words.length; length > 0; length--) {
            String prefix = String.join(" ", java.util.Arrays.copyOf(words, length));
            Optional<EurovisionArtist> match = findLastFm(LastFmProviderArtistResolver.songKey(rawArtistName, prefix));
            if (match.isPresent()) {
                return match;
            }
        }
        return Optional.empty();
    }

    private boolean isChartEligible(EurovisionArtist artist) {
        return artist.getStatus() == ArtistStatus.VERIFIED && artist.isActive();
    }

    private ExternalIdProvider toExternalIdProvider(Provider provider) {
        return switch (provider) {
            case SPOTIFY -> ExternalIdProvider.SPOTIFY;
            case APPLE_MUSIC -> ExternalIdProvider.APPLE_MUSIC;
            case SOUNDCLOUD -> ExternalIdProvider.SOUNDCLOUD;
            case LASTFM -> ExternalIdProvider.LASTFM;
        };
    }
}
