package com.eurovision.analytics.eurovision.identity;

import com.eurovision.analytics.eurovision.ExternalIdProvider;
import com.eurovision.analytics.oauth.applemusic.AppleMusicApiClient;
import com.eurovision.analytics.oauth.applemusic.AppleMusicDtos;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class AppleMusicProviderArtistResolver implements ProviderArtistResolver {

    private static final Pattern ARTIST_PATH_PATTERN = Pattern.compile("^/([a-z]{2})/artist/[^/]+/(\\d+)$");

    private final AppleMusicApiClient apiClient;

    public AppleMusicProviderArtistResolver(AppleMusicApiClient apiClient) {
        this.apiClient = apiClient;
    }

    @Override
    public ExternalIdProvider provider() {
        return ExternalIdProvider.APPLE_MUSIC;
    }

    @Override
    public Optional<ResolvedProviderArtist> resolve(URI canonicalUri) {
        Matcher matcher = ARTIST_PATH_PATTERN.matcher(canonicalUri.getPath());
        if (!matcher.matches()) {
            return Optional.empty();
        }
        String storefront = matcher.group(1);
        String artistId = matcher.group(2);
        Optional<AppleMusicDtos.ArtistResource> resource = apiClient.lookupCatalogArtist(storefront, artistId);
        return resource.map(r -> new ResolvedProviderArtist(
                ExternalIdProvider.APPLE_MUSIC, r.id(),
                r.attributes() == null ? null : r.attributes().name(),
                canonicalUri.toString()));
    }
}
