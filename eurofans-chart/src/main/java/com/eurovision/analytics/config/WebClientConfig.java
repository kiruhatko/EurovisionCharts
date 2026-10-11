package com.eurovision.analytics.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

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

    // coverartarchive.org answers every request with a redirect to archive.org.
    @Bean
    public WebClient coverArtArchiveWebClient() {
        return WebClient.builder()
                .baseUrl("https://coverartarchive.org")
                .clientConnector(new ReactorClientHttpConnector(HttpClient.create().followRedirect(true)))
                .build();
    }

    /**
     * Downloads cover images whose URLs come from Spotify, Cover Art Archive or Last.fm. Follows
     * redirects (Cover Art Archive images live behind one) and lifts the default 256 KB buffer
     * limit, which otherwise silently rejects every real high-resolution cover.
     */
    @Bean
    public WebClient artworkWebClient() {
        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(HttpClient.create().followRedirect(true)))
                .codecs(c -> c.defaultCodecs().maxInMemorySize(16 * 1024 * 1024))
                .build();
    }

    /** Unbound client used only for admin-supplied artist URLs, always guarded by SsrfUrlValidator first. */
    @Bean
    public WebClient unrestrictedWebClient() {
        return WebClient.builder().build();
    }
}
