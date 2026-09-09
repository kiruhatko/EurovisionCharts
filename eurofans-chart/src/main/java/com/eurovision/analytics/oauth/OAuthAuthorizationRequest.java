package com.eurovision.analytics.oauth;

/** Everything a provider-specific service needs to build its authorization URL. */
public record OAuthAuthorizationRequest(String state, String codeVerifier, String codeChallenge) {
}
