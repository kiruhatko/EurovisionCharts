package com.eurovision.analytics.eurovision.identity;

import com.eurovision.analytics.eurovision.ExternalIdProvider;
import com.eurovision.analytics.oauth.spotify.SpotifyApiClient;
import com.eurovision.analytics.oauth.spotify.SpotifyDtos;
import com.eurovision.analytics.oauth.spotify.SpotifyOAuthService;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class SpotifyProviderArtistResolver implements ProviderArtistResolver {

    private static final Pattern ARTIST_ID_PATTERN = Pattern.compile("^/artist/([A-Za-z0-9]+)$");

    private final SpotifyOAuthService oAuthService;
    private final SpotifyApiClient apiClient;

    public SpotifyProviderArtistResolver(SpotifyOAuthService oAuthService, SpotifyApiClient apiClient) {
        this.oAuthService = oAuthService;
        this.apiClient = apiClient;
    }

    @Override
    public ExternalIdProvider provider() {
        return ExternalIdProvider.SPOTIFY;
    }

    @Override
    public Optional<ResolvedProviderArtist> resolve(URI canonicalUri) {
        Matcher matcher = ARTIST_ID_PATTERN.matcher(canonicalUri.getPath());
        if (!matcher.matches()) {
            return Optional.empty();
        }
        String artistId = matcher.group(1);
        String appToken = oAuthService.getAppAccessToken();
        Optional<SpotifyDtos.ArtistDetail> detail = apiClient.fetchArtist(appToken, artistId);
        return detail.map(d -> new ResolvedProviderArtist(
                ExternalIdProvider.SPOTIFY, d.id(), d.name(), canonicalUri.toString()));
    }
}
