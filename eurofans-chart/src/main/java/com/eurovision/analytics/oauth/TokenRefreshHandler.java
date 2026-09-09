package com.eurovision.analytics.oauth;

import com.eurovision.analytics.connectedaccount.ConnectedAccount;
import com.eurovision.analytics.connectedaccount.Provider;

/**
 * Implemented only by providers with a refresh token (Spotify, SoundCloud).
 * Apple Music has no refresh mechanism and Last.fm has no token at all, so
 * neither implements this; {@code TokenRefreshScheduler} simply never
 * queries accounts of those two providers.
 */
public interface TokenRefreshHandler {

    Provider provider();

    RefreshedTokens refresh(String refreshToken);

    record RefreshedTokens(String accessToken, String refreshToken, long expiresInSeconds) {
    }
}
