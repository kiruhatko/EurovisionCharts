package com.eurovision.analytics.artwork;

import com.eurovision.analytics.connectedaccount.Provider;
import com.eurovision.analytics.musicbrainz.MusicBrainzClient;
import com.eurovision.analytics.oauth.spotify.SpotifyApiClient;
import com.eurovision.analytics.oauth.spotify.SpotifyDtos;
import com.eurovision.analytics.oauth.spotify.SpotifyOAuthService;
import com.eurovision.analytics.util.NameNormalizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;

/**
 * Artwork source priority (spec 9): a confirmed MusicBrainz release match via
 * Cover Art Archive first, otherwise the artwork the provider itself
 * returned with the listening event, applying that provider's own
 * highest-quality rule (already resolved into {@code nativeArtworkUrl} by
 * each provider client -- see {@code SpotifyApiClient}, {@code
 * AppleMusicApiClient}, {@code SoundCloudApiClient}, {@code LastFmApiClient}).
 *
 * <p>Unlike admin-supplied artist-import URLs, these are CDN URLs returned by
 * APIs we already trust (Spotify/Apple/SoundCloud/Last.fm/MusicBrainz), whose
 * exact CDN hostnames are not stable enough to hardcode into an allowlist;
 * the mitigation applied here instead is HTTPS-only, a byte-size cap, and
 * strict content-type + decodability validation before anything is cached or
 * served onward -- never an AI upscale, never a guessed larger size.
 */
@Service
public class ArtworkResolutionService {

    private static final Logger log = LoggerFactory.getLogger(ArtworkResolutionService.class);
    private static final int MAX_BYTES = 15 * 1024 * 1024;

    private final MusicBrainzClient musicBrainzClient;
    private final CoverArtArchiveClient coverArtArchiveClient;
    private final ArtworkCacheRepository artworkCacheRepository;
    private final WebClient unrestrictedWebClient;
    private final SpotifyOAuthService spotifyOAuthService;
    private final SpotifyApiClient spotifyApiClient;

    public ArtworkResolutionService(MusicBrainzClient musicBrainzClient,
                                     CoverArtArchiveClient coverArtArchiveClient,
                                     ArtworkCacheRepository artworkCacheRepository,
                                     WebClient unrestrictedWebClient,
                                     SpotifyOAuthService spotifyOAuthService,
                                     SpotifyApiClient spotifyApiClient) {
        this.musicBrainzClient = musicBrainzClient;
        this.coverArtArchiveClient = coverArtArchiveClient;
        this.artworkCacheRepository = artworkCacheRepository;
        this.unrestrictedWebClient = unrestrictedWebClient;
        this.spotifyOAuthService = spotifyOAuthService;
        this.spotifyApiClient = spotifyApiClient;
    }

    @Transactional
    public Optional<ArtworkCache> resolve(Provider provider, String rawArtistName, String rawTrackName,
                                           String nativeArtworkUrl) {
        String lookupKey = provider.name() + ":" + rawArtistName + ":" + rawTrackName;
        Optional<ArtworkCache> cached = artworkCacheRepository.findByLookupKey(lookupKey);
        if (cached.isPresent()) {
            return cached;
        }

        Optional<String> coverArtUrl = musicBrainzClient.findReleaseForRecording(rawArtistName, rawTrackName)
                .flatMap(coverArtArchiveClient::fetchFrontImageUrl);

        if (coverArtUrl.isPresent()) {
            Optional<ArtworkCache> saved = downloadAndCache(lookupKey, provider, ArtworkSource.COVER_ART_ARCHIVE, coverArtUrl.get());
            if (saved.isPresent()) {
                return saved;
            }
        }

        // Last.fm (and, to a lesser extent, SoundCloud) routinely returns a tiny or generic
        // placeholder image for "image" -- Last.fm disabled new image uploads years ago, so most
        // of its artwork today is a low-res leftover or a static grey note icon. Spotify's own
        // catalog is a much higher-quality fallback and needs no user to have connected Spotify
        // themselves -- it's the same app-level client-credentials token used for artist search.
        // Skip this when the play is already FROM Spotify: nativeArtworkUrl is already its best image.
        if (provider != Provider.SPOTIFY) {
            Optional<String> spotifyUrl = trySpotifyArtwork(rawArtistName, rawTrackName);
            if (spotifyUrl.isPresent()) {
                Optional<ArtworkCache> saved = downloadAndCache(lookupKey, provider, ArtworkSource.PROVIDER_NATIVE, spotifyUrl.get());
                if (saved.isPresent()) {
                    return saved;
                }
            }
        }

        if (nativeArtworkUrl == null || nativeArtworkUrl.isBlank()) {
            return Optional.empty();
        }
        return downloadAndCache(lookupKey, provider, ArtworkSource.PROVIDER_NATIVE, nativeArtworkUrl);
    }

    /**
     * Best-effort, never-guessed Spotify cover lookup: only trusted when the top search result's
     * own primary artist name is a normalized-exact match for the play's artist, so a same-named
     * but different track/artist never silently supplies the wrong cover.
     */
    private Optional<String> trySpotifyArtwork(String rawArtistName, String rawTrackName) {
        if (rawArtistName == null || rawArtistName.isBlank() || rawTrackName == null || rawTrackName.isBlank()) {
            return Optional.empty();
        }
        try {
            String appToken = spotifyOAuthService.getAppAccessToken();
            List<SpotifyDtos.Track> candidates = spotifyApiClient.searchTracks(appToken, rawArtistName, rawTrackName, 5);
            String normalizedArtist = NameNormalizer.normalize(rawArtistName);
            for (SpotifyDtos.Track candidate : candidates) {
                if (candidate.artists() == null || candidate.artists().isEmpty()) {
                    continue;
                }
                String candidateArtist = NameNormalizer.normalize(candidate.artists().get(0).name());
                if (candidateArtist.equals(normalizedArtist)) {
                    return spotifyApiClient.largestImage(candidate).map(SpotifyDtos.Image::url);
                }
            }
        } catch (Exception e) {
            log.warn("Spotify artwork lookup failed for \"{}\" - \"{}\": {}", rawArtistName, rawTrackName, e.getMessage());
        }
        return Optional.empty();
    }

    private Optional<ArtworkCache> downloadAndCache(String lookupKey, Provider provider, ArtworkSource source, String url) {
        if (url == null || !url.toLowerCase().startsWith("https://")) {
            return Optional.empty();
        }
        try {
            byte[] bytes = unrestrictedWebClient.get()
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
            String contentType = detectContentType(bytes);
            String sha256 = sha256Hex(bytes);

            ArtworkCache entity = new ArtworkCache(lookupKey, provider, source, url, contentType,
                    image.getWidth(), image.getHeight(), bytes.length, sha256);
            return Optional.of(artworkCacheRepository.save(entity));
        } catch (Exception e) {
            log.warn("Artwork download failed for url={}: {}", url, e.getMessage());
            return Optional.empty();
        }
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
