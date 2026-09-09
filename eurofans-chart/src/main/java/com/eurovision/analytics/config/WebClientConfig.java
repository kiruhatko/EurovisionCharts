package com.eurovision.analytics.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * One dedicated WebClient per external API, each pinned to its own base URL so
 * that no admin-supplied or provider-returned URL can be substituted into it.
 */
@Configuration
public class WebClientConfig {

    @Bean
    public WebClient spotifyAccountsWebClient() {
        return WebClient.builder().baseUrl("https://accounts.spotify.com").build();
    }

    @Bean
    public WebClient spotifyApiWebClient() {
        return WebClient.builder().baseUrl("https://api.spotify.com").build();
    }

    @Bean
    public WebClient appleMusicApiWebClient() {
        return WebClient.builder().baseUrl("https://api.music.apple.com").build();
    }

    @Bean
    public WebClient soundcloudApiWebClient() {
        return WebClient.builder().baseUrl("https://api.soundcloud.com").build();
    }

    @Bean
    public WebClient soundcloudAuthWebClient() {
        return WebClient.builder().baseUrl("https://secure.soundcloud.com").build();
    }

    @Bean
    public WebClient lastfmApiWebClient() {
        return WebClient.builder().baseUrl("https://ws.audioscrobbler.com").build();
    }

    @Bean
    public WebClient musicBrainzApiWebClient() {
        return WebClient.builder()
                .baseUrl("https://musicbrainz.org")
                .defaultHeader("User-Agent", "EurofansUaChart/1.0 (+https://github.com/kiruhatko/eurofans-ua-chart)")
                .build();
    }

    @Bean
    public WebClient coverArtArchiveWebClient() {
        return WebClient.builder().baseUrl("https://coverartarchive.org").build();
    }

    /** Unbound client used only for admin-supplied artist URLs, always guarded by SsrfUrlValidator first. */
    @Bean
    public WebClient unrestrictedWebClient() {
        return WebClient.builder().build();
    }
}
