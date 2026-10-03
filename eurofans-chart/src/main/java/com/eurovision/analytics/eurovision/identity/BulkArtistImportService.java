package com.eurovision.analytics.eurovision.identity;

import com.eurovision.analytics.admin.AdminAuditService;
import com.eurovision.analytics.eurovision.ArtistStatus;
import com.eurovision.analytics.eurovision.EurovisionArtist;
import com.eurovision.analytics.eurovision.EurovisionArtistExternalId;
import com.eurovision.analytics.eurovision.EurovisionArtistExternalIdRepository;
import com.eurovision.analytics.eurovision.EurovisionArtistRepository;
import com.eurovision.analytics.eurovision.EurovisionCountryRepository;
import com.eurovision.analytics.eurovision.EurovisionEditionRepository;
import com.eurovision.analytics.eurovision.ExternalIdProvider;
import com.eurovision.analytics.util.NameNormalizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * {@code /reloadartists}: a JSON array of admin-curated entries, each
 * possibly carrying multiple provider URLs for the SAME real-world artist
 * (spec 6.1.9). Unlike the interactive {@code /addartist} flow, this is a
 * trusted bulk action -- entries go straight to VERIFIED, no MusicBrainz
 * confidence gating or confirm/cancel step, since the admin already curated
 * the file offline. One bad entry or URL never aborts the rest of the batch.
 */
@Service
public class BulkArtistImportService {

    private static final Logger log = LoggerFactory.getLogger(BulkArtistImportService.class);

    private final ArtistUrlCanonicalizer canonicalizer;
    private final Map<ExternalIdProvider, ProviderArtistResolver> resolvers;
    private final EurovisionArtistRepository artistRepository;
    private final EurovisionArtistExternalIdRepository externalIdRepository;
    private final EurovisionCountryRepository countryRepository;
    private final EurovisionEditionRepository editionRepository;
    private final AdminAuditService adminAuditService;
    private final SpotifyProviderArtistResolver spotifyProviderArtistResolver;
    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    public BulkArtistImportService(ArtistUrlCanonicalizer canonicalizer,
                                    List<ProviderArtistResolver> resolvers,
                                    EurovisionArtistRepository artistRepository,
                                    EurovisionArtistExternalIdRepository externalIdRepository,
                                    EurovisionCountryRepository countryRepository,
                                    EurovisionEditionRepository editionRepository,
                                    AdminAuditService adminAuditService,
                                    SpotifyProviderArtistResolver spotifyProviderArtistResolver) {
        this.canonicalizer = canonicalizer;
        this.resolvers = resolvers.stream().collect(Collectors.toMap(ProviderArtistResolver::provider, Function.identity()));
        this.artistRepository = artistRepository;
        this.externalIdRepository = externalIdRepository;
        this.countryRepository = countryRepository;
        this.editionRepository = editionRepository;
        this.adminAuditService = adminAuditService;
        this.spotifyProviderArtistResolver = spotifyProviderArtistResolver;
    }

    public record BulkImportSummary(int artistsCreated, int artistsUpdated, int urlsLinked,
                                     int spotifyAutoLinked, int spotifyAmbiguous, List<String> errors) {
    }

    @Transactional
    public BulkImportSummary importFromStream(InputStream json, long adminTelegramId) {
        List<BulkArtistEntry> entries = jsonMapper.readValue(json, jsonMapper.getTypeFactory()
                .constructCollectionType(List.class, BulkArtistEntry.class));

        int created = 0;
        int updated = 0;
        int urlsLinked = 0;
        int spotifyAutoLinked = 0;
        int spotifyAmbiguous = 0;
        List<String> errors = new java.util.ArrayList<>();

        for (BulkArtistEntry entry : entries) {
            try {
                String normalized = NameNormalizer.normalize(entry.canonicalName());
                List<EurovisionArtist> existing = artistRepository.findByNormalizedName(normalized);
                EurovisionArtist artist = existing.isEmpty() ? new EurovisionArtist() : existing.get(0);
                boolean isNew = existing.isEmpty();

                artist.setCanonicalName(entry.canonicalName());
                artist.setNormalizedName(normalized);
                artist.setStatus(ArtistStatus.VERIFIED);
                artist.setActive(true);
                if (entry.countryIso() != null) {
                    countryRepository.findByIsoCode(entry.countryIso()).ifPresent(artist::setCountry);
                }
                if (entry.editionYear() != null) {
                    editionRepository.findByYear(entry.editionYear()).ifPresent(artist::setEdition);
                }
                artist = artistRepository.save(artist);

                if (isNew) {
                    created++;
                } else {
                    updated++;
                }

                for (String rawUrl : entry.urls() == null ? List.<String>of() : entry.urls()) {
                    try {
                        urlsLinked += linkUrl(artist, rawUrl, adminTelegramId) ? 1 : 0;
                    } catch (Exception e) {
                        errors.add(rawUrl + ": " + e.getMessage());
                    }
                }

                SpotifyAutoLinkOutcome outcome = autoLinkSpotifyByName(artist, entry.canonicalName());
                if (outcome == SpotifyAutoLinkOutcome.LINKED) {
                    spotifyAutoLinked++;
                    urlsLinked++;
                } else if (outcome == SpotifyAutoLinkOutcome.AMBIGUOUS) {
                    spotifyAmbiguous++;
                }
            } catch (Exception e) {
                errors.add(entry.canonicalName() + ": " + e.getMessage());
            }
        }

        adminAuditService.log(adminTelegramId, "RELOADARTISTS", "bulk_import", null,
                "created=" + created + " updated=" + updated + " urlsLinked=" + urlsLinked
                        + " spotifyAutoLinked=" + spotifyAutoLinked + " spotifyAmbiguous=" + spotifyAmbiguous
                        + " errors=" + errors.size());

        return new BulkImportSummary(created, updated, urlsLinked, spotifyAutoLinked, spotifyAmbiguous, errors);
    }

    private enum SpotifyAutoLinkOutcome { LINKED, AMBIGUOUS, SKIPPED }

    /**
     * For an entry that didn't already carry (or resolve) a Spotify URL, try to find the SAME
     * artist on Spotify by an exact normalized-name search match, using the app's own
     * client-credentials token (spec: never fabricate an ID -- only link an unambiguous match).
     */
    private SpotifyAutoLinkOutcome autoLinkSpotifyByName(EurovisionArtist artist, String canonicalName) {
        boolean alreadyLinked = externalIdRepository.findByArtistId(artist.getId()).stream()
                .anyMatch(e -> e.getProvider() == ExternalIdProvider.SPOTIFY);
        if (alreadyLinked) {
            return SpotifyAutoLinkOutcome.SKIPPED;
        }

        List<ResolvedProviderArtist> candidates = spotifyProviderArtistResolver.searchByExactName(canonicalName);
        if (candidates.isEmpty()) {
            return SpotifyAutoLinkOutcome.SKIPPED;
        }
        if (candidates.size() > 1) {
            return SpotifyAutoLinkOutcome.AMBIGUOUS;
        }

        ResolvedProviderArtist match = candidates.get(0);
        if (externalIdRepository.findByProviderAndExternalId(match.provider(), match.externalId()).isPresent()) {
            return SpotifyAutoLinkOutcome.SKIPPED;
        }
        externalIdRepository.save(new EurovisionArtistExternalId(
                artist, match.provider(), match.externalId(), match.canonicalUrl(), true));
        return SpotifyAutoLinkOutcome.LINKED;
    }

    private boolean linkUrl(EurovisionArtist artist, String rawUrl, long adminTelegramId) {
        ArtistUrlCanonicalizer.Canonicalized canonicalized = canonicalizer.canonicalize(rawUrl, adminTelegramId);
        ProviderArtistResolver resolver = resolvers.get(canonicalized.provider());
        if (resolver == null) {
            return false;
        }
        Optional<ResolvedProviderArtist> resolved = resolver.resolve(canonicalized.canonicalUri());
        if (resolved.isEmpty()) {
            return false;
        }
        ResolvedProviderArtist providerArtist = resolved.get();
        if (externalIdRepository.findByProviderAndExternalId(providerArtist.provider(), providerArtist.externalId()).isPresent()) {
            return false;
        }
        externalIdRepository.save(new EurovisionArtistExternalId(
                artist, providerArtist.provider(), providerArtist.externalId(), providerArtist.canonicalUrl(), true));
        return true;
    }
}
