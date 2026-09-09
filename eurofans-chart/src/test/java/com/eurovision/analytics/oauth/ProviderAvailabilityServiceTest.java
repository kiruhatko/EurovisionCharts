package com.eurovision.analytics.oauth;

import com.eurovision.analytics.config.ProviderProperties;
import com.eurovision.analytics.connectedaccount.Provider;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/** Spec rule #11: empty credentials must never crash, only report "not configured". */
class ProviderAvailabilityServiceTest {

    @Test
    void allProvidersReportNotConfiguredWhenEverythingIsBlank() {
        ProviderProperties properties = new ProviderProperties(
                new ProviderProperties.Spotify("", "", ""),
                new ProviderProperties.AppleMusic("", "", "", ""),
                new ProviderProperties.SoundCloud("", "", ""),
                new ProviderProperties.LastFm(""));
        ProviderAvailabilityService service = new ProviderAvailabilityService(properties);

        assertThatCode(service::logAvailabilityOnStartup).doesNotThrowAnyException();
        for (Provider provider : Provider.values()) {
            assertThat(service.isConfigured(provider)).isFalse();
        }
    }

    @Test
    void nullNestedPropertiesDoNotCrash() {
        ProviderProperties properties = new ProviderProperties(null, null, null, null);
        ProviderAvailabilityService service = new ProviderAvailabilityService(properties);

        assertThatCode(() -> {
            for (Provider provider : Provider.values()) {
                service.isConfigured(provider);
            }
        }).doesNotThrowAnyException();
    }

    @Test
    void aProviderWithAllFieldsSetIsConfigured() {
        ProviderProperties properties = new ProviderProperties(
                new ProviderProperties.Spotify("client-id", "client-secret", "https://example.com/callback"),
                new ProviderProperties.AppleMusic("", "", "", ""),
                new ProviderProperties.SoundCloud("", "", ""),
                new ProviderProperties.LastFm(""));
        ProviderAvailabilityService service = new ProviderAvailabilityService(properties);

        assertThat(service.isConfigured(Provider.SPOTIFY)).isTrue();
        assertThat(service.isConfigured(Provider.APPLE_MUSIC)).isFalse();
    }
}
