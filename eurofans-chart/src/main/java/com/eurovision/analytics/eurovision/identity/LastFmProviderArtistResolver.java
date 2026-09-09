package com.eurovision.analytics.eurovision.identity;

import com.eurovision.analytics.eurovision.ExternalIdProvider;
import com.eurovision.analytics.oauth.lastfm.LastFmApiClient;
import com.eurovision.analytics.oauth.lastfm.LastFmDtos;
import com.eurovision.analytics.util.NameNormalizer;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Last.fm is the one provider with no stable numeric artist id at all (spec
 * 4.5). When {@code artist.getinfo} returns an MBID we anchor on that (as
 * strong as any other provider); otherwise the external id falls back to the
 * normalized artist-page name, which is weaker and can still collide across
 * two different real-world artists sharing a name -- a limitation this
 * application documents rather than hides.
 */
@Component
public class LastFmProviderArtistResolver implements ProviderArtistResolver {

    private static final Pattern ARTIST_PATH_PATTERN = Pattern.compile("^/music/([^/]+)$");

    private final LastFmApiClient apiClient;

    public LastFmProviderArtistResolver(LastFmApiClient apiClient) {
        this.apiClient = apiClient;
    }

    @Override
    public ExternalIdProvider provider() {
        return ExternalIdProvider.LASTFM;
    }

    @Override
    public Optional<ResolvedProviderArtist> resolve(URI canonicalUri) {
        Matcher matcher = ARTIST_PATH_PATTERN.matcher(canonicalUri.getPath());
        if (!matcher.matches()) {
            return Optional.empty();
        }
        String decodedName = URLDecoder.decode(matcher.group(1), StandardCharsets.UTF_8).replace('+', ' ');
        Optional<LastFmDtos.ArtistInfo> info = apiClient.fetchArtistInfo(decodedName);
        if (info.isEmpty()) {
            return Optional.empty();
        }
        String externalId = (info.get().mbid() != null && !info.get().mbid().isBlank())
                ? info.get().mbid()
                : "name:" + NameNormalizer.normalize(info.get().name());
        return Optional.of(new ResolvedProviderArtist(
                ExternalIdProvider.LASTFM, externalId, info.get().name(), canonicalUri.toString()));
    }
}
