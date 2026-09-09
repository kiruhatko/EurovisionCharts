package com.eurovision.analytics.oauth.soundcloud;

import com.eurovision.analytics.connectedaccount.Provider;
import com.eurovision.analytics.oauth.TokenRefreshHandler;
import org.springframework.stereotype.Component;

@Component
public class SoundCloudTokenRefreshHandler implements TokenRefreshHandler {

    private final SoundCloudOAuthService oAuthService;

    public SoundCloudTokenRefreshHandler(SoundCloudOAuthService oAuthService) {
        this.oAuthService = oAuthService;
    }

    @Override
    public Provider provider() {
        return Provider.SOUNDCLOUD;
    }

    @Override
    public RefreshedTokens refresh(String refreshToken) {
        SoundCloudDtos.TokenResponse response = oAuthService.refreshToken(refreshToken);
        return new RefreshedTokens(response.accessToken(), response.refreshToken(), response.expiresInSeconds());
    }
}
