package com.eurovision.analytics.eurovision.identity;

import com.eurovision.analytics.eurovision.ExternalIdProvider;
import com.eurovision.analytics.security.SsrfUrlValidator;
import org.springframework.stereotype.Component;

import java.net.URI;

/**
 * Canonicalizes an admin-supplied artist URL: fixed host per provider,
 * re-encoded path, query and fragment always discarded (spec 6.1 step 2).
 * Delegates the actual HTTPS/allowlist enforcement to {@link SsrfUrlValidator}.
 */
@Component
public class ArtistUrlCanonicalizer {

    private final SsrfUrlValidator ssrfUrlValidator;

    public ArtistUrlCanonicalizer(SsrfUrlValidator ssrfUrlValidator) {
        this.ssrfUrlValidator = ssrfUrlValidator;
    }

    public record Canonicalized(ExternalIdProvider provider, URI canonicalUri) {
    }

    public Canonicalized canonicalize(String rawUrl, Long adminTelegramId) {
        URI validated = ssrfUrlValidator.validate(rawUrl, adminTelegramId);
        String host = validated.getHost().toLowerCase();
        ExternalIdProvider provider = switch (host) {
            case "open.spotify.com" -> ExternalIdProvider.SPOTIFY;
            case "music.apple.com" -> ExternalIdProvider.APPLE_MUSIC;
            case "soundcloud.com", "www.soundcloud.com" -> ExternalIdProvider.SOUNDCLOUD;
            case "last.fm", "www.last.fm" -> ExternalIdProvider.LASTFM;
            default -> throw new IllegalArgumentException("Unsupported artist URL host: " + host);
        };

        String fixedHost = switch (provider) {
            case SPOTIFY -> "open.spotify.com";
            case APPLE_MUSIC -> "music.apple.com";
            case SOUNDCLOUD -> "soundcloud.com";
            case LASTFM -> "www.last.fm";
            case MUSICBRAINZ -> throw new IllegalStateException("MusicBrainz is never an import source URL");
        };

        String normalizedPath = normalizePath(validated.getRawPath());
        URI canonical = URI.create("https://" + fixedHost + normalizedPath);
        return new Canonicalized(provider, canonical);
    }

    private String normalizePath(String rawPath) {
        if (rawPath == null || rawPath.isBlank()) {
            return "/";
        }
        // Collapse any duplicate slashes and drop a single trailing slash for a stable key.
        String collapsed = rawPath.replaceAll("/{2,}", "/");
        if (collapsed.length() > 1 && collapsed.endsWith("/")) {
            collapsed = collapsed.substring(0, collapsed.length() - 1);
        }
        return collapsed;
    }
}
