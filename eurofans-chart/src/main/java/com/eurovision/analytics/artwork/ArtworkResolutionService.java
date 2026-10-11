package com.eurovision.analytics.artwork;

import com.eurovision.analytics.connectedaccount.Provider;
import com.eurovision.analytics.eurovision.EurovisionArtistExternalId;
import com.eurovision.analytics.eurovision.EurovisionArtistExternalIdRepository;
import com.eurovision.analytics.eurovision.ExternalIdProvider;
import com.eurovision.analytics.musicbrainz.MusicBrainzClient;
import com.eurovision.analytics.oauth.spotify.SpotifyApiClient;
import com.eurovision.analytics.oauth.spotify.SpotifyDtos;
import com.eurovision.analytics.oauth.spotify.SpotifyOAuthService;
import com.eurovision.analytics.util.NameNormalizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Picks the sharpest real cover for a played track, trying in order: the play's own Spotify album
 * art, the same track in Spotify's catalog (verified against the artist's linked Spotify profile),
 * Cover Art Archive, and only then the provider's own image -- which for Last.fm is usually a
 * 300px leftover or its grey placeholder. The first image at least {@value #GOOD_WIDTH}px wide
 * wins; never an upscale or a guessed larger size.
 *
 * <p>A cached image below that width is looked at again at most once a day, so covers improve on
 * their own once a better source (e.g. a newly added Spotify link) exists.
 *
 * <p>These are CDN URLs returned by APIs we already trust, so the safeguards are HTTPS-only, a
 * size cap, and strict decodability validation before anything is cached or served onward.
 */
@Service
public class ArtworkResolutionService {

    private static final Logger log = LoggerFactory.getLogger(ArtworkResolutionService.class);
    private static final int MAX_BYTES = 15 * 1024 * 1024;
    private static final int GOOD_WIDTH = 500;
    private static final int MIN_WIDTH = 300;
    private static final Duration RECHECK_AFTER = Duration.ofDays(1);
    private static final String LASTFM_PLACEHOLDER = "2a96cbd8b46e442fc41c2b86b821562f";

    private final MusicBrainzClient musicBrainzClient;
    private final CoverArtArchiveClient coverArtArchiveClient;
    private final ArtworkCacheRepository artworkCacheRepository;
    private final EurovisionArtistExternalIdRepository externalIdRepository;
    private final WebClient artworkWebClient;
    private final SpotifyOAuthService spotifyOAuthService;
    private final SpotifyApiClient spotifyApiClient;
    // Tracks with no usable cover anywhere: without this, chart-studio's 20s refresh would search
    // Spotify and MusicBrainz again for each of them on every poll and burn the Spotify API quota.
    private final Map<String, Instant> recentMisses = new ConcurrentHashMap<>();

    public ArtworkResolutionService(MusicBrainzClient musicBrainzClient,
                                     CoverArtArchiveClient coverArtArchiveClient,
                                     ArtworkCacheRepository artworkCacheRepository,
                                     EurovisionArtistExternalIdRepository externalIdRepository,
                                     WebClient artworkWebClient,
                                     SpotifyOAuthService spotifyOAuthService,
                                     SpotifyApiClient spotifyApiClient) {
        this.musicBrainzClient = musicBrainzClient;
        this.coverArtArchiveClient = coverArtArchiveClient;
        this.artworkCacheRepository = artworkCacheRepository;
        this.externalIdRepository = externalIdRepository;
        this.artworkWebClient = artworkWebClient;
        this.spotifyOAuthService = spotifyOAuthService;
        this.spotifyApiClient = spotifyApiClient;
    }

    private record Downloaded(ArtworkSource source, String url, String contentType, int width, int height,
                              int byteSize, String sha256) {
    }

    /**
     * @param canonicalArtistId the Eurovision artist the play resolved to, used to verify Spotify
     *                          catalog matches against that artist's linked profile(s); may be null
     */
    // Its own transaction: chart-studio calls this from inside a read-only one, and the cache write must still commit.
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Optional<ArtworkCache> resolve(Provider provider, String rawArtistName, String rawTrackName,
                                           String nativeArtworkUrl, Long canonicalArtistId) {
        String lookupKey = provider.name() + ":" + rawArtistName + ":" + rawTrackName;
        Optional<ArtworkCache> cached = artworkCacheRepository.findByLookupKey(lookupKey);
        Instant missedAt = recentMisses.get(lookupKey);
        if (cached.isEmpty() && missedAt != null && missedAt.isAfter(Instant.now().minus(RECHECK_AFTER))) {
            return Optional.empty();
        }
        if (cached.isPresent() && (width(cached.get()) >= GOOD_WIDTH
                || cached.get().getCachedAt().isAfter(Instant.now().minus(RECHECK_AFTER)))) {
            return cached;
        }

        Optional<Downloaded> best = findBest(provider, rawArtistName, rawTrackName, nativeArtworkUrl, canonicalArtistId);
        if (best.isEmpty() && cached.isEmpty()) {
            if (recentMisses.size() > 50_000) {
                recentMisses.clear();
            }
            recentMisses.put(lookupKey, Instant.now());
            return Optional.empty();
        }
        if (best.isEmpty() || (cached.isPresent() && best.get().width() <= width(cached.get()))) {
            cached.ifPresent(c -> {
                c.setCachedAt(Instant.now());
                artworkCacheRepository.save(c);
            });
            return cached;
        }

        Downloaded d = best.get();
        ArtworkCache entity = cached.orElseGet(ArtworkCache::new);
        entity.setLookupKey(lookupKey);
        entity.setProvider(provider);
        entity.setSource(d.source());
        entity.setOriginalUrl(d.url());
        entity.setContentType(d.contentType());
        entity.setWidth(d.width());
        entity.setHeight(d.height());
        entity.setByteSize(d.byteSize());
        entity.setSha256(d.sha256());
        entity.setCachedAt(Instant.now());
        return Optional.of(artworkCacheRepository.save(entity));
    }

    private Optional<Downloaded> findBest(Provider provider, String rawArtistName, String rawTrackName,
                                          String nativeArtworkUrl, Long canonicalArtistId) {
        List<Supplier<Optional<Downloaded>>> sources = List.of(
                () -> provider == Provider.SPOTIFY
                        ? download(ArtworkSource.PROVIDER_NATIVE, nativeArtworkUrl) : Optional.empty(),
                () -> spotifyCatalogCover(rawArtistName, rawTrackName, canonicalArtistId)
                        .flatMap(url -> download(ArtworkSource.SPOTIFY_CATALOG, url)),
                () -> musicBrainzClient.findReleaseForRecording(rawArtistName, rawTrackName)
                        .flatMap(coverArtArchiveClient::fetchFrontImageUrl)
                        .flatMap(url -> download(ArtworkSource.COVER_ART_ARCHIVE, url)),
                () -> provider != Provider.SPOTIFY
                        ? download(ArtworkSource.PROVIDER_NATIVE, nativeArtworkUrl) : Optional.empty());

        Downloaded best = null;
        for (Supplier<Optional<Downloaded>> source : sources) {
            Optional<Downloaded> image = source.get();
            if (image.isEmpty()) {
                continue;
            }
            if (image.get().width() >= GOOD_WIDTH) {
                return image;
            }
            if (best == null || image.get().width() > best.width()) {
                best = image.get();
            }
        }
        return Optional.ofNullable(best);
    }

    /**
     * The same track in Spotify's catalog. With linked Spotify profiles, a result only counts if
     * one of those profiles is credited on it (so the title alone is enough to search by); without
     * any, the result's primary artist must carry exactly the played artist's name.
     */
    private Optional<String> spotifyCatalogCover(String rawArtistName, String rawTrackName, Long canonicalArtistId) {
        if (rawTrackName == null || rawTrackName.isBlank() || rawArtistName == null || rawArtistName.isBlank()) {
            return Optional.empty();
        }
        try {
            Set<String> linked = linkedSpotifyIds(canonicalArtistId);
            String token = spotifyOAuthService.getAppAccessToken();
            Optional<String> byArtistAndTitle = firstVerified(
                    spotifyApiClient.searchTracks(token, rawArtistName, rawTrackName, 10), linked, rawArtistName);
            if (byArtistAndTitle.isPresent() || linked.isEmpty()) {
                return byArtistAndTitle;
            }
            return firstVerified(spotifyApiClient.searchTracks(token, null, rawTrackName, 10), linked, rawArtistName);
        } catch (Exception e) {
            log.warn("Spotify artwork lookup failed for \"{}\" - \"{}\": {}", rawArtistName, rawTrackName, e.getMessage());
            return Optional.empty();
        }
    }

    private Optional<String> firstVerified(List<SpotifyDtos.Track> candidates, Set<String> linked, String rawArtistName) {
        String normalizedArtist = NameNormalizer.normalize(rawArtistName);
        for (SpotifyDtos.Track candidate : candidates) {
            if (candidate.artists() == null || candidate.artists().isEmpty()) {
                continue;
            }
            boolean verified = linked.isEmpty()
                    ? NameNormalizer.normalize(candidate.artists().get(0).name()).equals(normalizedArtist)
                    : candidate.artists().stream().anyMatch(a -> linked.contains(a.id()));
            if (verified) {
                return spotifyApiClient.largestImage(candidate).map(SpotifyDtos.Image::url);
            }
        }
        return Optional.empty();
    }

    private Set<String> linkedSpotifyIds(Long canonicalArtistId) {
        if (canonicalArtistId == null) {
            return Set.of();
        }
        return externalIdRepository.findByArtistId(canonicalArtistId).stream()
                .filter(e -> e.getProvider() == ExternalIdProvider.SPOTIFY)
                .map(EurovisionArtistExternalId::getExternalId)
                .collect(Collectors.toSet());
    }

    private Optional<Downloaded> download(ArtworkSource source, String url) {
        if (url == null || !url.toLowerCase().startsWith("https://") || url.contains(LASTFM_PLACEHOLDER)) {
            return Optional.empty();
        }
        try {
            byte[] bytes = artworkWebClient.get()
                    .uri(url)
                    .retrieve()
                    .bodyToMono(byte[].class)
                    .block();
            if (bytes == null || bytes.length == 0 || bytes.length > MAX_BYTES) {
                return Optional.empty();
            }
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(bytes));
            if (image == null) {
                log.warn("Artwork at {} could not be decoded as an image; rejecting", url);
                return Optional.empty();
            }
            if (image.getWidth() < MIN_WIDTH) {
                return Optional.empty();
            }
            return Optional.of(new Downloaded(source, url, detectContentType(bytes), image.getWidth(), image.getHeight(),
                    bytes.length, sha256Hex(bytes)));
        } catch (Exception e) {
            log.warn("Artwork download failed for url={}: {}", url, e.getMessage());
            return Optional.empty();
        }
    }

    private static int width(ArtworkCache cache) {
        return cache.getWidth() == null ? 0 : cache.getWidth();
    }

    private String detectContentType(byte[] bytes) {
        if (bytes.length >= 8 && (bytes[0] & 0xFF) == 0x89 && bytes[1] == 'P' && bytes[2] == 'N' && bytes[3] == 'G') {
            return "image/png";
        }
        if (bytes.length >= 3 && (bytes[0] & 0xFF) == 0xFF && (bytes[1] & 0xFF) == 0xD8) {
            return "image/jpeg";
        }
        if (bytes.length >= 6 && bytes[0] == 'G' && bytes[1] == 'I' && bytes[2] == 'F') {
            return "image/gif";
        }
        return "application/octet-stream";
    }

    private static String sha256Hex(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
