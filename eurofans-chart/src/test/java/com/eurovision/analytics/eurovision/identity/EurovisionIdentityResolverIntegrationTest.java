package com.eurovision.analytics.eurovision.identity;

import com.eurovision.analytics.connectedaccount.Provider;
import com.eurovision.analytics.eurovision.ArtistStatus;
import com.eurovision.analytics.eurovision.EurovisionArtist;
import com.eurovision.analytics.eurovision.EurovisionArtistExternalId;
import com.eurovision.analytics.eurovision.EurovisionArtistExternalIdRepository;
import com.eurovision.analytics.eurovision.EurovisionArtistRepository;
import com.eurovision.analytics.eurovision.ExternalIdProvider;
import com.eurovision.analytics.listening.ResolutionStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Reproduces the exact real-world failure a live /track command hit: every
 * repository call below runs in its own short-lived transaction (like
 * production), and the resolver result is inspected only AFTER that. Before
 * the *_artist relations were switched to EAGER (see the FetchType.EAGER
 * comments on ConnectedAccount.user, EurovisionArtistAlias.artist,
 * EurovisionArtistExternalId.artist, ListeningEvent.canonicalArtist and
 * ListeningEventAttribution.eurovisionArtist), {@code getCanonicalName()}
 * below threw {@code LazyInitializationException} because the Hibernate
 * session backing the repository call had already closed by the time
 * {@code NowPlayingResolver}/{@code TrackCommand} touched the result.
 */
@Testcontainers
@SpringBootTest(properties = {
        "deployment.binding-enabled=false",
        "security.token-encryption-key=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY="
})
class EurovisionIdentityResolverIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine");

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private EurovisionArtistRepository artistRepository;
    @Autowired
    private EurovisionArtistExternalIdRepository externalIdRepository;
    @Autowired
    private EurovisionIdentityResolver identityResolver;

    @Test
    void resolvedArtistSurvivesAccessOutsideTheOriginatingTransaction() {
        EurovisionArtist artist = new EurovisionArtist();
        artist.setCanonicalName("Jamala");
        artist.setNormalizedName("jamala");
        artist.setStatus(ArtistStatus.VERIFIED);
        artist.setActive(true);
        artist = artistRepository.save(artist);

        externalIdRepository.save(new EurovisionArtistExternalId(
                artist, ExternalIdProvider.SPOTIFY, "4EXfia20rgVLyubQiYtOIC", null, true));

        // This call, and everything inside EurovisionIdentityResolver.resolve(), has already
        // committed and closed its own transaction by the time we get the result back --
        // exactly like NowPlayingResolver calling it from a Telegram command handler.
        IdentityResolution result = identityResolver.resolve(Provider.SPOTIFY, "4EXfia20rgVLyubQiYtOIC", "Jamala");

        assertThat(result.status()).isEqualTo(ResolutionStatus.CONFIRMED);
        assertThat(result.canonicalArtist()).isNotNull();

        // The actual regression: this used to throw LazyInitializationException.
        assertThatCode(() -> result.canonicalArtist().getCanonicalName()).doesNotThrowAnyException();
        assertThat(result.canonicalArtist().getCanonicalName()).isEqualTo("Jamala");
    }
}
