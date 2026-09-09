package com.eurovision.analytics.oauth;

import com.eurovision.analytics.config.ProviderProperties;
import com.eurovision.analytics.connectedaccount.Provider;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Checked at startup and on every connect-command invocation (spec rule #11).
 * A provider with empty credentials is never a crash; every connect command
 * politely reports "not configured by the admin yet" instead.
 */
@Service
public class ProviderAvailabilityService {

    private static final Logger log = LoggerFactory.getLogger(ProviderAvailabilityService.class);

    private final ProviderProperties properties;

    public ProviderAvailabilityService(ProviderProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    void logAvailabilityOnStartup() {
        for (Provider provider : Provider.values()) {
            log.info("Provider {} configured: {}", provider, isConfigured(provider));
        }
    }

    public boolean isConfigured(Provider provider) {
        return switch (provider) {
            case SPOTIFY -> properties.spotify() != null && properties.spotify().isConfigured();
            case APPLE_MUSIC -> properties.appleMusic() != null && properties.appleMusic().isConfigured();
            case SOUNDCLOUD -> properties.soundcloud() != null && properties.soundcloud().isConfigured();
            case LASTFM -> properties.lastfm() != null && properties.lastfm().isConfigured();
        };
    }

    public static final String NOT_CONFIGURED_MESSAGE =
            "Ця інтеграція ще не налаштована адміністратором.";
}
