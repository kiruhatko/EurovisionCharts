package com.eurovision.analytics.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

@ConfigurationProperties(prefix = "providers")
public record ProviderProperties(
        @NestedConfigurationProperty Spotify spotify,
        @NestedConfigurationProperty AppleMusic appleMusic,
        @NestedConfigurationProperty SoundCloud soundcloud,
        @NestedConfigurationProperty LastFm lastfm
) {

    public record Spotify(String clientId, String clientSecret, String redirectUri) {
        public boolean isConfigured() {
            return notBlank(clientId) && notBlank(clientSecret) && notBlank(redirectUri);
        }
    }

    public record AppleMusic(String teamId, String keyId, String privateKey, String redirectOrigin) {
        public boolean isConfigured() {
            return notBlank(teamId) && notBlank(keyId) && notBlank(privateKey) && notBlank(redirectOrigin);
        }
    }

    public record SoundCloud(String clientId, String clientSecret, String redirectUri) {
        public boolean isConfigured() {
            return notBlank(clientId) && notBlank(clientSecret) && notBlank(redirectUri);
        }
    }

    public record LastFm(String apiKey) {
        public boolean isConfigured() {
            return notBlank(apiKey);
        }
    }

    private static boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }
}
