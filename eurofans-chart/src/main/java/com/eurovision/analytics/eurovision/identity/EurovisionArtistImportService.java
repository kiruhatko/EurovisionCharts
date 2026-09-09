package com.eurovision.analytics.eurovision.identity;

import com.eurovision.analytics.admin.AdminAuditService;
import com.eurovision.analytics.eurovision.ArtistStatus;
import com.eurovision.analytics.eurovision.EurovisionArtist;
import com.eurovision.analytics.eurovision.EurovisionArtistExternalId;
import com.eurovision.analytics.eurovision.EurovisionArtistExternalIdRepository;
import com.eurovision.analytics.eurovision.EurovisionArtistRepository;
import com.eurovision.analytics.eurovision.ExternalIdProvider;
import com.eurovision.analytics.musicbrainz.MusicBrainzClient;
import com.eurovision.analytics.musicbrainz.MusicBrainzCrosscheckResult;
import com.eurovision.analytics.musicbrainz.MusicBrainzDtos;
import com.eurovision.analytics.util.NameNormalizer;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * The full {@code /addartist <URL>} pipeline (spec 6.1): canonicalize the
 * admin-supplied URL, resolve it against the provider's own API, crosscheck
 * against MusicBrainz, and create a PENDING artist that only becomes
 * VERIFIED (and therefore chart-eligible) once an admin explicitly confirms
 * it via the inline [Confirm]/[Cancel] keyboard.
 */
@Service
public class EurovisionArtistImportService {

    private final ArtistUrlCanonicalizer canonicalizer;
    private final Map<ExternalIdProvider, ProviderArtistResolver> resolvers;
    private final MusicBrainzClient musicBrainzClient;
    private final EurovisionArtistRepository artistRepository;
    private final EurovisionArtistExternalIdRepository externalIdRepository;
    private final AdminAuditService adminAuditService;

    public EurovisionArtistImportService(ArtistUrlCanonicalizer canonicalizer,
                                          List<ProviderArtistResolver> resolvers,
                                          MusicBrainzClient musicBrainzClient,
                                          EurovisionArtistRepository artistRepository,
                                          EurovisionArtistExternalIdRepository externalIdRepository,
                                          AdminAuditService adminAuditService) {
        this.canonicalizer = canonicalizer;
        this.resolvers = resolvers.stream().collect(Collectors.toMap(ProviderArtistResolver::provider, Function.identity()));
        this.musicBrainzClient = musicBrainzClient;
        this.artistRepository = artistRepository;
        this.externalIdRepository = externalIdRepository;
        this.adminAuditService = adminAuditService;
    }

    @Transactional
    public ArtistImportResult importFromUrl(String rawUrl, long adminTelegramId) {
        ArtistUrlCanonicalizer.Canonicalized canonicalized = canonicalizer.canonicalize(rawUrl, adminTelegramId);
        ProviderArtistResolver resolver = resolvers.get(canonicalized.provider());
        if (resolver == null) {
            return new ArtistImportResult.Failed("No resolver registered for provider " + canonicalized.provider());
        }

        Optional<ResolvedProviderArtist> resolved = resolver.resolve(canonicalized.canonicalUri());
        if (resolved.isEmpty()) {
            return new ArtistImportResult.Failed(
                    "Could not resolve an artist from this URL via the " + canonicalized.provider() + " API.");
        }
        ResolvedProviderArtist providerArtist = resolved.get();

        Optional<EurovisionArtistExternalId> existing = externalIdRepository
                .findByProviderAndExternalId(providerArtist.provider(), providerArtist.externalId());
        if (existing.isPresent()) {
            return new ArtistImportResult.AlreadyLinked(existing.get().getArtist());
        }

        MusicBrainzCrosscheckResult mbResult = musicBrainzClient.crosscheckByUrl(canonicalized.canonicalUri());
        ImportConfidence confidence = switch (mbResult.outcome()) {
            case SINGLE_MATCH -> ImportConfidence.CONFIRMED;
            case AMBIGUOUS -> ImportConfidence.CONFLICT;
            case NO_MATCH, LOOKUP_FAILED -> ImportConfidence.PROBABLE;
        };

        String canonicalName = (confidence == ImportConfidence.CONFIRMED)
                ? mbResult.candidates().get(0).name()
                : providerArtist.displayName();
        if (canonicalName == null || canonicalName.isBlank()) {
            canonicalName = providerArtist.displayName() != null ? providerArtist.displayName() : "Unknown artist";
        }

        EurovisionArtist artist = new EurovisionArtist();
        artist.setCanonicalName(canonicalName);
        artist.setNormalizedName(NameNormalizer.normalize(canonicalName));
        artist.setStatus(ArtistStatus.PENDING);
        artist.setActive(true);
        artist = artistRepository.save(artist);

        EurovisionArtistExternalId providerLink = new EurovisionArtistExternalId(
                artist, providerArtist.provider(), providerArtist.externalId(), providerArtist.canonicalUrl(), false);
        externalIdRepository.save(providerLink);

        if (confidence == ImportConfidence.CONFIRMED) {
            MusicBrainzDtos.ArtistRef mbArtist = mbResult.candidates().get(0);
            externalIdRepository.save(new EurovisionArtistExternalId(
                    artist, ExternalIdProvider.MUSICBRAINZ, mbArtist.id(), null, true));
        }

        adminAuditService.log(adminTelegramId, "ADDARTIST_PENDING", "eurovision_artist",
                String.valueOf(artist.getId()),
                "provider=" + providerArtist.provider() + " externalId=" + providerArtist.externalId()
                        + " confidence=" + confidence);

        List<String> mbCandidateNames = mbResult.candidates().stream().map(MusicBrainzDtos.ArtistRef::name).toList();
        return new ArtistImportResult.PendingConfirmation(artist, confidence, mbCandidateNames);
    }

    @Transactional
    public Optional<EurovisionArtist> confirm(long artistId, long adminTelegramId) {
        Optional<EurovisionArtist> maybeArtist = artistRepository.findById(artistId)
                .filter(a -> a.getStatus() == ArtistStatus.PENDING);
        maybeArtist.ifPresent(artist -> {
            artist.setStatus(ArtistStatus.VERIFIED);
            artist.setUpdatedAt(Instant.now());
            artistRepository.save(artist);
            for (EurovisionArtistExternalId externalId : externalIdRepository.findByArtistId(artist.getId())) {
                externalId.setVerified(true);
                externalId.setUpdatedAt(Instant.now());
                externalIdRepository.save(externalId);
            }
            adminAuditService.log(adminTelegramId, "ADDARTIST_CONFIRM", "eurovision_artist",
                    String.valueOf(artist.getId()), artist.getCanonicalName());
        });
        return maybeArtist;
    }

    @Transactional
    public boolean cancel(long artistId, long adminTelegramId) {
        Optional<EurovisionArtist> maybeArtist = artistRepository.findById(artistId)
                .filter(a -> a.getStatus() == ArtistStatus.PENDING);
        maybeArtist.ifPresent(artist -> {
            adminAuditService.log(adminTelegramId, "ADDARTIST_CANCEL", "eurovision_artist",
                    String.valueOf(artist.getId()), artist.getCanonicalName());
            artistRepository.delete(artist);
        });
        return maybeArtist.isPresent();
    }
}
