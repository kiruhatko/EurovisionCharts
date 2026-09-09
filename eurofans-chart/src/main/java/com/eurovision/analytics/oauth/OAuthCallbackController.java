package com.eurovision.analytics.oauth;

import com.eurovision.analytics.connectedaccount.ConnectedAccountService;
import com.eurovision.analytics.connectedaccount.Provider;
import com.eurovision.analytics.oauth.applemusic.AppleMusicConnectionService;
import com.eurovision.analytics.oauth.applemusic.AppleMusicDeveloperTokenService;
import com.eurovision.analytics.oauth.soundcloud.SoundCloudApiClient;
import com.eurovision.analytics.oauth.soundcloud.SoundCloudDtos;
import com.eurovision.analytics.oauth.soundcloud.SoundCloudOAuthService;
import com.eurovision.analytics.oauth.spotify.SpotifyApiClient;
import com.eurovision.analytics.oauth.spotify.SpotifyDtos;
import com.eurovision.analytics.oauth.spotify.SpotifyOAuthService;
import com.eurovision.analytics.security.TokenEncryptionService;
import com.eurovision.analytics.user.User;
import com.eurovision.analytics.user.UserRepository;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Optional;

/**
 * The application's only unauthenticated external HTTP entry points (spec
 * 4.6). Kept deliberately minimal: strict DTO parsing of every provider
 * response, no direct JSON-to-entity mapping, and every failure path is a
 * plain-text page rather than a stack trace.
 */
@RestController
public class OAuthCallbackController {

    private final SpotifyOAuthService spotifyOAuthService;
    private final SpotifyApiClient spotifyApiClient;
    private final SoundCloudOAuthService soundCloudOAuthService;
    private final SoundCloudApiClient soundCloudApiClient;
    private final AppleMusicConnectionService appleMusicConnectionService;
    private final AppleMusicDeveloperTokenService appleMusicDeveloperTokenService;
    private final ConnectedAccountService connectedAccountService;
    private final TokenEncryptionService tokenEncryptionService;
    private final UserRepository userRepository;
    private final OAuthStateStore oauthStateStore;

    public OAuthCallbackController(SpotifyOAuthService spotifyOAuthService,
                                    SpotifyApiClient spotifyApiClient,
                                    SoundCloudOAuthService soundCloudOAuthService,
                                    SoundCloudApiClient soundCloudApiClient,
                                    AppleMusicConnectionService appleMusicConnectionService,
                                    AppleMusicDeveloperTokenService appleMusicDeveloperTokenService,
                                    ConnectedAccountService connectedAccountService,
                                    TokenEncryptionService tokenEncryptionService,
                                    UserRepository userRepository,
                                    OAuthStateStore oauthStateStore) {
        this.spotifyOAuthService = spotifyOAuthService;
        this.spotifyApiClient = spotifyApiClient;
        this.soundCloudOAuthService = soundCloudOAuthService;
        this.soundCloudApiClient = soundCloudApiClient;
        this.appleMusicConnectionService = appleMusicConnectionService;
        this.appleMusicDeveloperTokenService = appleMusicDeveloperTokenService;
        this.connectedAccountService = connectedAccountService;
        this.tokenEncryptionService = tokenEncryptionService;
        this.userRepository = userRepository;
        this.oauthStateStore = oauthStateStore;
    }

    @GetMapping(value = "/oauth/spotify/callback", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> spotifyCallback(@RequestParam(required = false) String code,
                                                    @RequestParam(required = false) String state,
                                                    @RequestParam(required = false) String error) {
        if (error != null) {
            return ResponseEntity.badRequest().body("Spotify authorization was not completed: " + error);
        }
        Optional<OAuthStateData> stateData = spotifyStateStoreConsume(state);
        if (stateData.isEmpty()) {
            return ResponseEntity.badRequest().body("This authorization link is invalid, expired, or already used.");
        }
        try {
            SpotifyDtos.TokenResponse token = spotifyOAuthService.exchangeCode(code, stateData.get().codeVerifier());
            SpotifyDtos.UserProfile profile = spotifyApiClient.fetchProfile(token.accessToken());
            User user = requireUser(stateData.get().telegramUserId());

            connectedAccountService.upsertConnection(
                    user, Provider.SPOTIFY, profile.id(), profile.displayName(),
                    tokenEncryptionService.encrypt(token.accessToken()),
                    tokenEncryptionService.encrypt(token.refreshToken()),
                    Instant.now().plusSeconds(token.expiresInSeconds()),
                    token.scope());

            return ResponseEntity.ok("Spotify connected successfully! You can close this window and return to Telegram.");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Could not complete Spotify connection. Please try /spotify again.");
        }
    }

    @GetMapping(value = "/oauth/soundcloud/callback", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> soundCloudCallback(@RequestParam(required = false) String code,
                                                       @RequestParam(required = false) String state,
                                                       @RequestParam(required = false) String error) {
        if (error != null) {
            return ResponseEntity.badRequest().body("SoundCloud authorization was not completed: " + error);
        }
        Optional<OAuthStateData> stateData = soundCloudStateStoreConsume(state);
        if (stateData.isEmpty()) {
            return ResponseEntity.badRequest().body("This authorization link is invalid, expired, or already used.");
        }
        try {
            SoundCloudDtos.TokenResponse token = soundCloudOAuthService.exchangeCode(code, stateData.get().codeVerifier());
            SoundCloudDtos.UserProfile profile = soundCloudApiClient.fetchProfile(token.accessToken());
            User user = requireUser(stateData.get().telegramUserId());

            connectedAccountService.upsertConnection(
                    user, Provider.SOUNDCLOUD, String.valueOf(profile.id()), profile.username(),
                    tokenEncryptionService.encrypt(token.accessToken()),
                    tokenEncryptionService.encrypt(token.refreshToken()),
                    Instant.now().plusSeconds(token.expiresInSeconds()),
                    token.scope());

            return ResponseEntity.ok("SoundCloud connected successfully! You can close this window and return to Telegram.");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Could not complete SoundCloud connection. Please try /soundcloud again.");
        }
    }

    @GetMapping(value = "/oauth/apple-music/developer-token", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<DeveloperTokenResponse> appleMusicDeveloperToken(@RequestParam String state) {
        if (!appleMusicDeveloperTokenService.isConfigured()) {
            return ResponseEntity.status(503).build();
        }
        Optional<String> token = appleMusicConnectionService.developerTokenForState(state);
        return token.map(t -> ResponseEntity.ok(new DeveloperTokenResponse(t)))
                .orElseGet(() -> ResponseEntity.badRequest().build());
    }

    // Same-origin only: applemusic-connect.html is served from this same application under
    // app.public-base-url, so its fetch() calls here are ordinary same-origin requests and need no CORS relaxation.
    @PostMapping(value = "/oauth/apple-music/callback",
            consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> appleMusicCallback(@RequestBody AppleMusicCallbackRequest request) {
        Optional<AppleMusicConnectionService.CallbackResult> result =
                appleMusicConnectionService.handleCallback(request.state(), request.musicUserToken());
        if (result.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        User user = requireUser(result.get().telegramUserId());
        connectedAccountService.upsertConnection(
                user, Provider.APPLE_MUSIC, result.get().pseudoAccountId(), "Apple Music",
                tokenEncryptionService.encrypt(request.musicUserToken()),
                null, null, null);
        return ResponseEntity.ok().build();
    }

    private Optional<OAuthStateData> spotifyStateStoreConsume(String state) {
        return consumeOrLogAbuse(Provider.SPOTIFY, state);
    }

    private Optional<OAuthStateData> soundCloudStateStoreConsume(String state) {
        return consumeOrLogAbuse(Provider.SOUNDCLOUD, state);
    }

    private Optional<OAuthStateData> consumeOrLogAbuse(Provider provider, String state) {
        return oauthStateStore.consume(provider, state);
    }

    private User requireUser(long telegramUserId) {
        return userRepository.findByTelegramId(telegramUserId)
                .orElseThrow(() -> new IllegalStateException("No Telegram user found for id " + telegramUserId));
    }

    public record DeveloperTokenResponse(String developerToken) {
    }

    public record AppleMusicCallbackRequest(String state, String musicUserToken) {
    }
}
