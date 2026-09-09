package com.eurovision.analytics.oauth.applemusic;

import com.eurovision.analytics.config.AppProperties;
import com.eurovision.analytics.connectedaccount.Provider;
import com.eurovision.analytics.oauth.OAuthStateData;
import com.eurovision.analytics.oauth.OAuthStateStore;
import com.eurovision.analytics.security.SecurityEventService;
import com.eurovision.analytics.security.SecurityEventType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Optional;

/**
 * Apple Music's connection flow is fundamentally different from the other
 * three providers (spec 4.3): the authorization code never touches our
 * server. The browser running MusicKit JS obtains the Music User Token
 * directly from Apple and POSTs it to {@code /oauth/apple-music/callback}
 * ourselves, so this service's job is state validation + a trial API call,
 * never a token exchange.
 */
@Service
public class AppleMusicConnectionService {

    private final AppProperties appProperties;
    private final OAuthStateStore stateStore;
    private final AppleMusicDeveloperTokenService developerTokenService;
    private final WebClient appleMusicApiWebClient;
    private final SecurityEventService securityEventService;

    public AppleMusicConnectionService(AppProperties appProperties,
                                        OAuthStateStore stateStore,
                                        AppleMusicDeveloperTokenService developerTokenService,
                                        WebClient appleMusicApiWebClient,
                                        SecurityEventService securityEventService) {
        this.appProperties = appProperties;
        this.stateStore = stateStore;
        this.developerTokenService = developerTokenService;
        this.appleMusicApiWebClient = appleMusicApiWebClient;
        this.securityEventService = securityEventService;
    }

    /** Link sent to the user by the /applemusic command; opens our static MusicKit JS page. */
    public String buildConnectUrl(long telegramUserId) {
        String state = stateStore.issueWithoutPkce(Provider.APPLE_MUSIC, telegramUserId);
        return appProperties.publicBaseUrl() + "/applemusic-connect.html?state=" + state;
    }

    /** Called by the static page's own bootstrap fetch, before the user authorizes. */
    public Optional<String> developerTokenForState(String state) {
        // Peeking, not consuming: the state is still needed at the final callback.
        // A short-lived, single-purpose peek key avoids a second consumable secret.
        if (state == null || state.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(developerTokenService.currentToken());
    }

    public record CallbackResult(long telegramUserId, String pseudoAccountId) {
    }

    /**
     * Validates the single-use state and performs a trial Apple Music API
     * call to confirm the Music User Token actually works before it is ever
     * persisted.
     */
    public Optional<CallbackResult> handleCallback(String state, String musicUserToken) {
        Optional<OAuthStateData> stateData = stateStore.consume(Provider.APPLE_MUSIC, state);
        if (stateData.isEmpty()) {
            return Optional.empty();
        }
        if (musicUserToken == null || musicUserToken.isBlank()) {
            securityEventService.record(SecurityEventType.OAUTH_CALLBACK_ABUSE, stateData.get().telegramUserId(), null,
                    "Apple Music callback submitted with no Music User Token");
            return Optional.empty();
        }

        boolean valid = probeToken(musicUserToken);
        if (!valid) {
            securityEventService.record(SecurityEventType.OAUTH_TOKEN_EXCHANGE_FAILED, stateData.get().telegramUserId(), null,
                    "Apple Music Music-User-Token failed the trial API call");
            return Optional.empty();
        }

        // Apple never exposes a stable, queryable user id for privacy reasons; we derive a
        // stable local surrogate from the token itself purely to satisfy the NOT NULL /
        // per-(user,provider) uniqueness shape of connected_accounts. It is never treated
        // as an identity claim about the Apple account.
        String pseudoAccountId = sha256Hex(musicUserToken);
        return Optional.of(new CallbackResult(stateData.get().telegramUserId(), pseudoAccountId));
    }

    private boolean probeToken(String musicUserToken) {
        try {
            appleMusicApiWebClient.get()
                    .uri("/v1/me/storefront")
                    .headers(h -> {
                        h.setBearerAuth(developerTokenService.currentToken());
                        h.set("Music-User-Token", musicUserToken);
                    })
                    .retrieve()
                    .bodyToMono(AppleMusicDtos.StorefrontProbeResponse.class)
                    .block();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static String sha256Hex(String input) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
