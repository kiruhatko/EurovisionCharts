package com.eurovision.analytics.eurovision.identity;

import com.eurovision.analytics.eurovision.ExternalIdProvider;
import com.eurovision.analytics.oauth.soundcloud.SoundCloudApiClient;
import com.eurovision.analytics.oauth.soundcloud.SoundCloudDtos;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.Optional;
import java.util.regex.Pattern;

@Component
public class SoundCloudProviderArtistResolver implements ProviderArtistResolver {

    private static final Pattern PROFILE_PATH_PATTERN = Pattern.compile("^/[A-Za-z0-9_-]+$");

    private final SoundCloudApiClient apiClient;

    public SoundCloudProviderArtistResolver(SoundCloudApiClient apiClient) {
        this.apiClient = apiClient;
    }

    @Override
    public ExternalIdProvider provider() {
        return ExternalIdProvider.SOUNDCLOUD;
    }

    @Override
    public Optional<ResolvedProviderArtist> resolve(URI canonicalUri) {
        if (!PROFILE_PATH_PATTERN.matcher(canonicalUri.getPath()).matches()) {
            return Optional.empty();
        }
        Optional<SoundCloudDtos.UserProfile> profile = apiClient.resolveByUrl(canonicalUri.toString());
        return profile.map(p -> new ResolvedProviderArtist(
                ExternalIdProvider.SOUNDCLOUD, String.valueOf(p.id()), p.username(), canonicalUri.toString()));
    }
}
