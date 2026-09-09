package com.eurovision.analytics.oauth;

public record OAuthStateData(long telegramUserId, String codeVerifier) {
}
