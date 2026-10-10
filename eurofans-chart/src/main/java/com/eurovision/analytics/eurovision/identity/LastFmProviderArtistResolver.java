package com.eurovision.analytics.eurovision.identity;

import com.eurovision.analytics.eurovision.ExternalIdProvider;
import com.eurovision.analytics.util.NameNormalizer;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Last.fm has no artist id: an artist IS its page, {@code last.fm/music/{name}}, and every
 * scrobble carries exactly that page name. So the admin-supplied link is taken as the identity
 * as-is -- no API lookup, no search -- and a scrobble matches it when its artist name maps to the
 * same key (see {@link EurovisionIdentityResolver}).
 *
 * <p>A generic name's page is shared by unrelated artists, so a link may instead point at one
 * song ({@code /music/{artist}/_/{track}}) or single ({@code /music/{artist}/{release}}): only
 * scrobbles of that song/release under that page then count.
 */
@Component
public class LastFmProviderArtistResolver implements ProviderArtistResolver {

    private static final Pattern ARTIST_PATH = Pattern.compile("^/music/([^/]+)$");
    private static final Pattern TRACK_PATH = Pattern.compile("^/music/([^/]+)/_/([^/]+)$");
    private static final Pattern RELEASE_PATH = Pattern.compile("^/music/([^/]+)/([^_+/][^/]*)$");

    public static String pageKey(String artistName) {
        return "name:" + NameNormalizer.normalize(artistName);
    }

    public static String songKey(String artistName, String normalizedTitle) {
        return "song:" + NameNormalizer.normalize(artistName) + "|" + normalizedTitle;
    }

    @Override
    public ExternalIdProvider provider() {
        return ExternalIdProvider.LASTFM;
    }

    @Override
    public Optional<ResolvedProviderArtist> resolve(URI canonicalUri) {
        String path = canonicalUri.getRawPath();
        Matcher artist = ARTIST_PATH.matcher(path);
        if (artist.matches()) {
            String pageName = decode(artist.group(1));
            return NameNormalizer.normalize(pageName).isEmpty() ? Optional.empty() : Optional.of(
                    new ResolvedProviderArtist(ExternalIdProvider.LASTFM, pageKey(pageName), pageName, canonicalUri.toString()));
        }
        Matcher song = TRACK_PATH.matcher(path);
        if (!song.matches()) {
            song = RELEASE_PATH.matcher(path);
            if (!song.matches()) {
                return Optional.empty();
            }
        }
        String pageName = decode(song.group(1));
        String title = NameNormalizer.normalize(decode(song.group(2)));
        if (NameNormalizer.normalize(pageName).isEmpty() || title.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new ResolvedProviderArtist(
                ExternalIdProvider.LASTFM, songKey(pageName, title), pageName, canonicalUri.toString()));
    }

    private static String decode(String rawSegment) {
        return URLDecoder.decode(rawSegment, StandardCharsets.UTF_8);
    }
}
