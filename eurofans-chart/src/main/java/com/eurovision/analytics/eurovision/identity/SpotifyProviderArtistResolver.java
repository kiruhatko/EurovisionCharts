package com.eurovision.analytics.eurovision.identity;

import com.eurovision.analytics.eurovision.ExternalIdProvider;
import com.eurovision.analytics.oauth.spotify.SpotifyApiClient;
import com.eurovision.analytics.oauth.spotify.SpotifyDtos;
import com.eurovision.analytics.oauth.spotify.SpotifyOAuthService;
import com.eurovision.analytics.util.NameNormalizer;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.List;
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

    /**
     * Best-effort Spotify match by name, for bulk-import entries that don't carry an explicit
     * Spotify URL. Never guesses: only candidates whose Spotify name is normalized-identical to
     * the queried name count as a match, and an ambiguous (multiple identical-name) result is
     * returned as-is for the caller to treat as "do not auto-link" rather than picking one.
     */
    public List<ResolvedProviderArtist> searchByExactName(String name) {
        String appToken = oAuthService.getAppAccessToken();
        String normalizedTarget = NameNormalizer.normalize(name);
        return apiClient.searchArtists(appToken, name, 10).stream()
                .filter(candidate -> NameNormalizer.normalize(candidate.name()).equals(normalizedTarget))
                .map(candidate -> new ResolvedProviderArtist(ExternalIdProvider.SPOTIFY, candidate.id(),
                        candidate.name(), "https://open.spotify.com/artist/" + candidate.id()))
                .toList();
    }
}
