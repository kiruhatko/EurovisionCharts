package com.eurovision.analytics.oauth;

import com.eurovision.analytics.connectedaccount.Provider;
import com.eurovision.analytics.security.SecurityEventService;
import com.eurovision.analytics.security.SecurityEventType;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

/**
 * Redis-backed, single-use, TTL-bound OAuth {@code state} storage (spec 4.6).
 * A state token is scoped to exactly one {@code (provider, telegramUserId)}
 * pair and is deleted atomically on first read: any replay attempt gets a
 * miss, is treated as CSRF, and is logged as
 * {@link SecurityEventType#OAUTH_STATE_INVALID}.
 */
@Component
public class OAuthStateStore {

    private static final Duration TTL = Duration.ofMinutes(10);
    private static final String DELIMITER = "|";

    private final StringRedisTemplate redisTemplate;
    private final SecurityEventService securityEventService;

    public OAuthStateStore(StringRedisTemplate redisTemplate, SecurityEventService securityEventService) {
        this.redisTemplate = redisTemplate;
        this.securityEventService = securityEventService;
    }

    public OAuthAuthorizationRequest issueWithPkce(Provider provider, long telegramUserId) {
        String state = PkceUtil.generateRandomToken();
        String codeVerifier = PkceUtil.generateCodeVerifier();
        String codeChallenge = PkceUtil.deriveCodeChallenge(codeVerifier);
        store(provider, state, telegramUserId, codeVerifier);
        return new OAuthAuthorizationRequest(state, codeVerifier, codeChallenge);
    }

    public String issueWithoutPkce(Provider provider, long telegramUserId) {
        String state = PkceUtil.generateRandomToken();
        store(provider, state, telegramUserId, "");
        return state;
    }

    private void store(Provider provider, String state, long telegramUserId, String codeVerifier) {
        String value = telegramUserId + DELIMITER + codeVerifier;
        redisTemplate.opsForValue().set(key(provider, state), value, TTL);
    }

    /** Single-use: the state is deleted whether or not it is found. */
    public Optional<OAuthStateData> consume(Provider provider, String state) {
        if (state == null || state.isBlank()) {
            securityEventService.record(SecurityEventType.OAUTH_STATE_INVALID, null, null,
                    "Empty OAuth state for provider " + provider);
            return Optional.empty();
        }
        String value = redisTemplate.opsForValue().getAndDelete(key(provider, state));
        if (value == null) {
            securityEventService.record(SecurityEventType.OAUTH_STATE_INVALID, null, null,
                    "Unknown, expired, or replayed OAuth state for provider " + provider);
            return Optional.empty();
        }
        int idx = value.indexOf(DELIMITER);
        long telegramUserId = Long.parseLong(value.substring(0, idx));
        String codeVerifier = value.substring(idx + 1);
        return Optional.of(new OAuthStateData(telegramUserId, codeVerifier));
    }

    private String key(Provider provider, String state) {
        return "oauth:state:" + provider.name() + ":" + state;
    }
}
