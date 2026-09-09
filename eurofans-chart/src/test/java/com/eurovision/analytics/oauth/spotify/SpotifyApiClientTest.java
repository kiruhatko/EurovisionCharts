package com.eurovision.analytics.oauth.spotify;

import com.eurovision.analytics.connectedaccount.ConnectedAccount;
import com.eurovision.analytics.connectedaccount.Provider;
import com.eurovision.analytics.listening.RecentPlay;
import com.eurovision.analytics.nowplaying.NowPlayingSignal;
import com.eurovision.analytics.security.TokenEncryptionService;
import com.eurovision.analytics.user.User;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Exercises {@link SpotifyApiClient} against a real embedded HTTP server
 * (spec 14: "кожен provider sync scheduler ізольовано тестується з mocked
 * API"): success, 204 no-content ("nothing playing"), and malformed JSON.
 */
class SpotifyApiClientTest {

    private MockWebServer server;
    private SpotifyApiClient client;
    private ConnectedAccount account;

    @BeforeEach
    void setUp() throws IOException {
        server = new MockWebServer();
        server.start();
        WebClient webClient = WebClient.builder().baseUrl(server.url("/").toString()).build();
        TokenEncryptionService tokenEncryptionService = mock(TokenEncryptionService.class);
        when(tokenEncryptionService.decrypt(org.mockito.ArgumentMatchers.any())).thenReturn("fake-access-token");
        client = new SpotifyApiClient(webClient, tokenEncryptionService);

        account = new ConnectedAccount(new User(1L, "u", "U"), Provider.SPOTIFY, "acct", "Display");
        account.setId(10L);
        account.setAccessTokenEncrypted(new byte[]{1, 2, 3});
    }

    @AfterEach
    void tearDown() throws IOException {
        server.shutdown();
    }

    @Test
    void currentlyPlayingTrackIsParsedIntoASignal() {
        String body = """
                {
                  "is_playing": true,
                  "item": {
                    "id": "track1",
                    "name": "1944",
                    "artists": [{"id": "artist1", "name": "Jamala"}],
                    "album": {"name": "1944", "images": [{"url": "https://img/small.jpg", "width": 64, "height": 64},
                                                          {"url": "https://img/large.jpg", "width": 640, "height": 640}]}
                  }
                }
                """;
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(body));

        Optional<NowPlayingSignal> signal = client.fetchNowPlaying(account);

        assertThat(signal).isPresent();
        assertThat(signal.get().rawArtistName()).isEqualTo("Jamala");
        assertThat(signal.get().rawTrackName()).isEqualTo("1944");
        assertThat(signal.get().nativeArtworkUrl()).isEqualTo("https://img/large.jpg");
    }

    @Test
    void noContentResponseMeansNothingPlaying() {
        server.enqueue(new MockResponse().setResponseCode(204));

        Optional<NowPlayingSignal> signal = client.fetchNowPlaying(account);

        assertThat(signal).isEmpty();
    }

    @Test
    void malformedJsonIsHandledWithoutThrowing() {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("{ not valid json !!"));

        Optional<NowPlayingSignal> signal = client.fetchNowPlaying(account);

        assertThat(signal).isEmpty();
    }

    @Test
    void unauthorizedResponseIsHandledWithoutThrowing() {
        server.enqueue(new MockResponse().setResponseCode(401).setBody("{\"error\":\"invalid token\"}"));

        Optional<NowPlayingSignal> signal = client.fetchNowPlaying(account);

        assertThat(signal).isEmpty();
    }

    @Test
    void rateLimitedResponseIsHandledWithoutThrowing() {
        server.enqueue(new MockResponse().setResponseCode(429).setHeader("Retry-After", "1"));

        Optional<NowPlayingSignal> signal = client.fetchNowPlaying(account);

        assertThat(signal).isEmpty();
    }

    @Test
    void recentlyPlayedParsesMultipleTracksWithTimestamps() {
        String body = """
                {
                  "items": [
                    {"track": {"id": "t1", "name": "Song A", "artists": [{"id": "a1", "name": "Artist A"}], "album": {"name": "Alb", "images": []}},
                     "played_at": "2024-05-01T10:00:00Z"},
                    {"track": {"id": "t2", "name": "Song B", "artists": [{"id": "a2", "name": "Artist B"}], "album": {"name": "Alb2", "images": []}},
                     "played_at": "2024-05-01T11:00:00Z"}
                  ]
                }
                """;
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(body));

        List<RecentPlay> plays = client.fetchRecentPlays(account);

        assertThat(plays).hasSize(2);
        assertThat(plays.get(0).rawArtistName()).isEqualTo("Artist A");
        assertThat(plays.get(1).rawArtistName()).isEqualTo("Artist B");
        assertThat(plays.get(0).playedAtUtc()).isNotNull();
        assertThat(plays.get(0).estimatedTimestamp()).isFalse();
    }
}
