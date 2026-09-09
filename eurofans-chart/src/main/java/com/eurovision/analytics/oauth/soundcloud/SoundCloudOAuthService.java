package com.eurovision.analytics.oauth.soundcloud;

import com.eurovision.analytics.config.ProviderProperties;
import com.eurovision.analytics.connectedaccount.Provider;
import com.eurovision.analytics.oauth.OAuthAuthorizationRequest;
import com.eurovision.analytics.oauth.OAuthStateStore;
import com.eurovision.analytics.security.SecurityEventService;
import com.eurovision.analytics.security.SecurityEventType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.nio.charset.StandardCharsets;

/** OAuth 2.1 Authorization Code + mandatory PKCE (spec 4.4). */
@Service
public class SoundCloudOAuthService {

    private final ProviderProperties.SoundCloud config;
    private final OAuthStateStore stateStore;
    private final WebClient soundcloudAuthWebClient;
    private final SecurityEventService securityEventService;

    public SoundCloudOAuthService(ProviderProperties properties,
                                   OAuthStateStore stateStore,
                                   WebClient soundcloudAuthWebClient,
                                   SecurityEventService securityEventService) {
        this.config = properties.soundcloud();
        this.stateStore = stateStore;
        this.soundcloudAuthWebClient = soundcloudAuthWebClient;
        this.securityEventService = securityEventService;
    }

    public String buildAuthorizationUrl(long telegramUserId) {
        OAuthAuthorizationRequest request = stateStore.issueWithPkce(Provider.SOUNDCLOUD, telegramUserId);
        return UriComponentsBuilder.fromUriString("https://secure.soundcloud.com/authorize")
                .queryParam("client_id", config.clientId())
                .queryParam("redirect_uri", config.redirectUri())
                .queryParam("response_type", "code")
                .queryParam("code_challenge_method", "S256")
                .queryParam("code_challenge", request.codeChallenge())
                .queryParam("state", request.state())
                .encode(StandardCharsets.UTF_8)
                .build()
                .toUriString();
    }

    public SoundCloudDtos.TokenResponse exchangeCode(String code, String codeVerifier) {
        try {
            return soundcloudAuthWebClient.post()
                    .uri("/oauth/token")
                    .body(BodyInserters.fromFormData("grant_type", "authorization_code")
                            .with("client_id", config.clientId())
                            .with("client_secret", config.clientSecret())
                            .with("redirect_uri", config.redirectUri())
                            .with("code", code)
                            .with("code_verifier", codeVerifier))
                    .retrieve()
                    .bodyToMono(SoundCloudDtos.TokenResponse.class)
                    .block();
        } catch (Exception e) {
            securityEventService.record(SecurityEventType.OAUTH_TOKEN_EXCHANGE_FAILED, null, null,
                    "SoundCloud token exchange failed: " + e.getMessage());
            throw e;
        }
    }

    public SoundCloudDtos.TokenResponse refreshToken(String refreshToken) {
        return soundcloudAuthWebClient.post()
                .uri("/oauth/token")
                .body(BodyInserters.fromFormData("grant_type", "refresh_token")
                        .with("client_id", config.clientId())
                        .with("client_secret", config.clientSecret())
                        .with("refresh_token", refreshToken))
                .retrieve()
                .bodyToMono(SoundCloudDtos.TokenResponse.class)
                .block();
    }
}
