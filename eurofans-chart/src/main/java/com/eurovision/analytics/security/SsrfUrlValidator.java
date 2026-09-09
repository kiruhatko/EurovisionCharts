package com.eurovision.analytics.security;

import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Set;

/**
 * Exact-hostname allowlist for every admin-supplied or provider-returned URL
 * that this application will ever fetch. HTTPS-only, exact host match (never
 * suffix/subdomain matching, so {@code evil.com/open.spotify.com} or
 * {@code open.spotify.com.evil.com} are both rejected).
 */
@Component
public class SsrfUrlValidator {

    private static final Set<String> ALLOWED_HOSTS = Set.of(
            "open.spotify.com",
            "music.apple.com",
            "soundcloud.com",
            "www.soundcloud.com",
            "www.last.fm",
            "last.fm",
            "musicbrainz.org",
            "www.musicbrainz.org"
    );

    private final SecurityEventService securityEventService;

    public SsrfUrlValidator(SecurityEventService securityEventService) {
        this.securityEventService = securityEventService;
    }

    /**
     * @throws SsrfBlockedException if the URL is not HTTPS, not parseable, or
     *                               its host is not on the exact allowlist.
     */
    public URI validate(String rawUrl, Long adminTelegramId) {
        URI uri;
        try {
            uri = new URI(rawUrl);
        } catch (URISyntaxException e) {
            block(rawUrl, adminTelegramId, "malformed URL");
            throw new SsrfBlockedException("Malformed URL");
        }

        if (!"https".equalsIgnoreCase(uri.getScheme())) {
            block(rawUrl, adminTelegramId, "non-HTTPS scheme");
            throw new SsrfBlockedException("Only HTTPS URLs are accepted");
        }

        String host = uri.getHost();
        if (host == null || !ALLOWED_HOSTS.contains(host.toLowerCase())) {
            block(rawUrl, adminTelegramId, "host not on allowlist: " + host);
            throw new SsrfBlockedException("Host is not an allowed provider domain: " + host);
        }

        if (uri.getUserInfo() != null) {
            block(rawUrl, adminTelegramId, "URL userinfo component present");
            throw new SsrfBlockedException("URLs with embedded credentials are rejected");
        }

        return uri;
    }

    private void block(String rawUrl, Long adminTelegramId, String reason) {
        securityEventService.record(SecurityEventType.SSRF_BLOCKED, adminTelegramId, null,
                "Blocked URL '" + rawUrl + "': " + reason);
    }

    public static class SsrfBlockedException extends RuntimeException {
        public SsrfBlockedException(String message) {
            super(message);
        }
    }
}
