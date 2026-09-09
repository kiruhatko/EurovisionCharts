package com.eurovision.analytics.oauth.spotify;

import com.eurovision.analytics.connectedaccount.Provider;
import com.eurovision.analytics.oauth.TokenRefreshHandler;
import org.springframework.stereotype.Component;

@Component
public class SpotifyTokenRefreshHandler implements TokenRefreshHandler {

    private final SpotifyOAuthService oAuthService;

    public SpotifyTokenRefreshHandler(SpotifyOAuthService oAuthService) {
        this.oAuthService = oAuthService;
    }

    @Override
    public Provider provider() {
        return Provider.SPOTIFY;
    }

    @Override
    public RefreshedTokens refresh(String refreshToken) {
        SpotifyDtos.TokenResponse response = oAuthService.refreshToken(refreshToken);
        // Spotify does not always return a new refresh_token; the caller keeps the old one when null.
        return new RefreshedTokens(response.accessToken(), response.refreshToken(), response.expiresInSeconds());
    }
}
