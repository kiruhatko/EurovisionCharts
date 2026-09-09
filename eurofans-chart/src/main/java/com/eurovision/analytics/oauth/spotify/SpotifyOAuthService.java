package com.eurovision.analytics.oauth.spotify;

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
import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class SpotifyOAuthService {

    private static final String SCOPES = "user-read-recently-played user-read-currently-playing user-read-private";

    private final ProviderProperties.Spotify config;
    private final OAuthStateStore stateStore;
    private final WebClient spotifyAccountsWebClient;
    private final SecurityEventService securityEventService;
    private final AtomicReference<AppToken> cachedAppToken = new AtomicReference<>();

    private record AppToken(String accessToken, Instant expiresAt) {
    }

    public SpotifyOAuthService(ProviderProperties properties,
                                OAuthStateStore stateStore,
                                WebClient spotifyAccountsWebClient,
                                SecurityEventService securityEventService) {
        this.config = properties.spotify();
        this.stateStore = stateStore;
        this.spotifyAccountsWebClient = spotifyAccountsWebClient;
        this.securityEventService = securityEventService;
    }

    public String buildAuthorizationUrl(long telegramUserId) {
        OAuthAuthorizationRequest request = stateStore.issueWithPkce(Provider.SPOTIFY, telegramUserId);
        return UriComponentsBuilder.fromUriString("https://accounts.spotify.com/authorize")
                .queryParam("client_id", config.clientId())
                .queryParam("response_type", "code")
                .queryParam("redirect_uri", config.redirectUri())
                .queryParam("scope", SCOPES)
                .queryParam("state", request.state())
                .queryParam("code_challenge_method", "S256")
                .queryParam("code_challenge", request.codeChallenge())
                .encode(StandardCharsets.UTF_8)
                .build()
                .toUriString();
    }

    public SpotifyDtos.TokenResponse exchangeCode(String code, String codeVerifier) {
        try {
            return spotifyAccountsWebClient.post()
                    .uri("/api/token")
                    .headers(h -> h.setBasicAuth(config.clientId(), config.clientSecret()))
                    .body(BodyInserters.fromFormData("grant_type", "authorization_code")
                            .with("code", code)
                            .with("redirect_uri", config.redirectUri())
                            .with("code_verifier", codeVerifier))
                    .retrieve()
                    .bodyToMono(SpotifyDtos.TokenResponse.class)
                    .block();
        } catch (Exception e) {
            securityEventService.record(SecurityEventType.OAUTH_TOKEN_EXCHANGE_FAILED, null, null,
                    "Spotify token exchange failed: " + e.getMessage());
            throw e;
        }
    }

    public SpotifyDtos.TokenResponse refreshToken(String refreshToken) {
        return spotifyAccountsWebClient.post()
                .uri("/api/token")
                .headers(h -> h.setBasicAuth(config.clientId(), config.clientSecret()))
                .body(BodyInserters.fromFormData("grant_type", "refresh_token")
                        .with("refresh_token", refreshToken))
                .retrieve()
                .bodyToMono(SpotifyDtos.TokenResponse.class)
                .block();
    }

    /**
     * App-only Client Credentials token for catalog lookups (e.g. admin
     * {@code /addartist} imports) that must not require any end user to have
     * connected their own Spotify account.
     */
    public synchronized String getAppAccessToken() {
        AppToken current = cachedAppToken.get();
        if (current != null && Instant.now().isBefore(current.expiresAt())) {
            return current.accessToken();
        }
        SpotifyDtos.TokenResponse response = spotifyAccountsWebClient.post()
                .uri("/api/token")
                .headers(h -> h.setBasicAuth(config.clientId(), config.clientSecret()))
                .body(BodyInserters.fromFormData("grant_type", "client_credentials"))
                .retrieve()
                .bodyToMono(SpotifyDtos.TokenResponse.class)
                .block();
        AppToken token = new AppToken(response.accessToken(), Instant.now().plusSeconds(response.expiresInSeconds() - 30));
        cachedAppToken.set(token);
        return token.accessToken();
    }
}
