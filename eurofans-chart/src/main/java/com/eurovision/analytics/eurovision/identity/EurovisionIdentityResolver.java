package com.eurovision.analytics.eurovision.identity;

import com.eurovision.analytics.connectedaccount.Provider;
import com.eurovision.analytics.eurovision.ArtistStatus;
import com.eurovision.analytics.eurovision.EurovisionArtist;
import com.eurovision.analytics.eurovision.EurovisionArtistAlias;
import com.eurovision.analytics.eurovision.EurovisionArtistAliasRepository;
import com.eurovision.analytics.eurovision.EurovisionArtistExternalId;
import com.eurovision.analytics.eurovision.EurovisionArtistExternalIdRepository;
import com.eurovision.analytics.eurovision.EurovisionArtistRepository;
import com.eurovision.analytics.eurovision.ExternalIdProvider;
import com.eurovision.analytics.listening.ResolutionStatus;
import com.eurovision.analytics.util.NameNormalizer;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * The resolution order from spec 6.3, applied to every raw listening event
 * during ingestion. Never guesses: any ambiguity or miss goes to
 * {@link UnresolvedArtistQueueService} instead of picking a "best" candidate.
 */
@Service
public class EurovisionIdentityResolver {

    private final EurovisionArtistExternalIdRepository externalIdRepository;
    private final EurovisionArtistAliasRepository aliasRepository;
    private final EurovisionArtistRepository artistRepository;
    private final UnresolvedArtistQueueService unresolvedArtistQueueService;

    public EurovisionIdentityResolver(EurovisionArtistExternalIdRepository externalIdRepository,
                                       EurovisionArtistAliasRepository aliasRepository,
                                       EurovisionArtistRepository artistRepository,
                                       UnresolvedArtistQueueService unresolvedArtistQueueService) {
        this.externalIdRepository = externalIdRepository;
        this.aliasRepository = aliasRepository;
        this.artistRepository = artistRepository;
        this.unresolvedArtistQueueService = unresolvedArtistQueueService;
    }

    /**
     * Same as {@link #resolve} but also checks every other artist credited on the
     * track (feat./collab) for an exact provider-ID match before falling back to
     * the primary artist's name-based matching -- so a collab track resolves as
     * long as ANY credited artist is registered, never by guessing which one.
     */
    public IdentityResolution resolveAny(Provider provider, String primaryProviderArtistId, String primaryRawArtistName,
                                          List<ArtistCredit> additionalArtists) {
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
        return resolve(provider, primaryProviderArtistId, primaryRawArtistName);
    }

    public IdentityResolution resolve(Provider provider, String providerArtistId, String rawArtistName) {
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

        List<EurovisionArtist> nameCandidates = artistRepository.findByNormalizedName(normalized).stream()
                .filter(this::isChartEligible)
                .distinct()
                .toList();
        if (nameCandidates.size() == 1) {
            return new IdentityResolution(nameCandidates.get(0), "NORMALIZED_NAME_MATCH", ResolutionStatus.PROBABLE, "LOW");
        }
        if (nameCandidates.size() > 1) {
            unresolvedArtistQueueService.recordOccurrence(provider, providerArtistId, rawArtistName);
            return IdentityResolution.conflict();
        }

        unresolvedArtistQueueService.recordOccurrence(provider, providerArtistId, rawArtistName);
        return IdentityResolution.unknown();
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
