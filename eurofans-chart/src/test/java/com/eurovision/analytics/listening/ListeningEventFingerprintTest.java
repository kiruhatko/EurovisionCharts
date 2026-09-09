package com.eurovision.analytics.listening;

import com.eurovision.analytics.connectedaccount.Provider;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ListeningEventFingerprintTest {

    @Test
    void identicalPlayOnDifferentProvidersIsNotADuplicate() {
        String spotifyFp = ListeningEventFingerprint.compute(1L, Provider.SPOTIFY, "2024-01-01T00:00:00Z",
                "Jamala", "1944", "1944", "track123");
        String appleFp = ListeningEventFingerprint.compute(1L, Provider.APPLE_MUSIC, "2024-01-01T00:00:00Z",
                "Jamala", "1944", "1944", "track123");

        assertThat(spotifyFp).isNotEqualTo(appleFp);
    }

    @Test
    void sameEventOnSameProviderProducesTheSameFingerprint() {
        String fp1 = ListeningEventFingerprint.compute(1L, Provider.SPOTIFY, "2024-01-01T00:00:00Z",
                "Jamala", "1944", "1944", "track123");
        String fp2 = ListeningEventFingerprint.compute(1L, Provider.SPOTIFY, "2024-01-01T00:00:00Z",
                "Jamala", "1944", "1944", "track123");

        assertThat(fp1).isEqualTo(fp2);
    }

    @Test
    void differentUsersProduceDifferentFingerprintsForTheSamePlay() {
        String fp1 = ListeningEventFingerprint.compute(1L, Provider.SPOTIFY, "2024-01-01T00:00:00Z",
                "Jamala", "1944", "1944", "track123");
        String fp2 = ListeningEventFingerprint.compute(2L, Provider.SPOTIFY, "2024-01-01T00:00:00Z",
                "Jamala", "1944", "1944", "track123");

        assertThat(fp1).isNotEqualTo(fp2);
    }

    @Test
    void differentTimestampsProduceDifferentFingerprints() {
        String fp1 = ListeningEventFingerprint.compute(1L, Provider.SPOTIFY, "2024-01-01T00:00:00Z",
                "Jamala", "1944", "1944", "track123");
        String fp2 = ListeningEventFingerprint.compute(1L, Provider.SPOTIFY, "2024-01-01T00:05:00Z",
                "Jamala", "1944", "1944", "track123");

        assertThat(fp1).isNotEqualTo(fp2);
    }
}
