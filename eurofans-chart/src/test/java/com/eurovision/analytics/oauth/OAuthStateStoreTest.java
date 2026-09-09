package com.eurovision.analytics.oauth;

import com.eurovision.analytics.connectedaccount.Provider;
import com.eurovision.analytics.security.SecurityEventService;
import com.eurovision.analytics.security.SecurityEventType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Spec 4.6: OAuth state must be single-use and TTL-bound; a replay is CSRF, not a valid callback. */
class OAuthStateStoreTest {

    private final Map<String, String> fakeRedis = new HashMap<>();
    private StringRedisTemplate redisTemplate;
    private SecurityEventService securityEventService;
    private OAuthStateStore store;

    @SuppressWarnings("unchecked")
    @BeforeEach
    void setUp() {
        redisTemplate = mock(StringRedisTemplate.class);
        ValueOperations<String, String> valueOps = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);

        when(valueOps.getAndDelete(anyString())).thenAnswer(inv -> fakeRedis.remove(inv.getArgument(0, String.class)));
        org.mockito.Mockito.doAnswer(inv -> {
            fakeRedis.put(inv.getArgument(0, String.class), inv.getArgument(1, String.class));
            return null;
        }).when(valueOps).set(anyString(), anyString(), any(Duration.class));

        securityEventService = mock(SecurityEventService.class);
        store = new OAuthStateStore(redisTemplate, securityEventService);
    }

    @Test
    void issuedStateCanBeConsumedExactlyOnce() {
        OAuthAuthorizationRequest request = store.issueWithPkce(Provider.SPOTIFY, 42L);

        Optional<OAuthStateData> first = store.consume(Provider.SPOTIFY, request.state());
        assertThat(first).isPresent();
        assertThat(first.get().telegramUserId()).isEqualTo(42L);
        assertThat(first.get().codeVerifier()).isEqualTo(request.codeVerifier());

        Optional<OAuthStateData> replay = store.consume(Provider.SPOTIFY, request.state());
        assertThat(replay).isEmpty();
        verify(securityEventService).record(eq(SecurityEventType.OAUTH_STATE_INVALID), any(), any(), anyString());
    }

    @Test
    void unknownStateIsRejectedAsCsrf() {
        Optional<OAuthStateData> result = store.consume(Provider.SPOTIFY, "never-issued-state");
        assertThat(result).isEmpty();
        verify(securityEventService).record(eq(SecurityEventType.OAUTH_STATE_INVALID), any(), any(), anyString());
    }

    @Test
    void stateIssuedForOneProviderIsRejectedForAnother() {
        OAuthAuthorizationRequest request = store.issueWithPkce(Provider.SPOTIFY, 1L);
        Optional<OAuthStateData> crossProvider = store.consume(Provider.SOUNDCLOUD, request.state());
        assertThat(crossProvider).isEmpty();
    }

    @Test
    void emptyStateIsRejected() {
        Optional<OAuthStateData> result = store.consume(Provider.LASTFM, "");
        assertThat(result).isEmpty();
    }
}
