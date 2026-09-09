package com.eurovision.analytics.oauth.lastfm;

import com.eurovision.analytics.config.ProviderProperties;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Optional;

/** No OAuth: Last.fm listening history is public per-username (spec 4.5). */
@Service
public class LastFmConnectionService {

    private final WebClient lastfmApiWebClient;
    private final ProviderProperties.LastFm config;

    public LastFmConnectionService(WebClient lastfmApiWebClient, ProviderProperties properties) {
        this.lastfmApiWebClient = lastfmApiWebClient;
        this.config = properties.lastfm();
    }

    /** Verifies the username exists via {@code user.getinfo} before connecting it. */
    public Optional<String> verifyUsername(String username) {
        try {
            LastFmDtos.UserInfoResponse response = lastfmApiWebClient.get()
                    .uri(uriBuilder -> uriBuilder.path("/2.0/")
                            .queryParam("method", "user.getinfo")
                            .queryParam("user", username)
                            .queryParam("api_key", config.apiKey())
                            .queryParam("format", "json")
                            .build())
                    .retrieve()
                    .bodyToMono(LastFmDtos.UserInfoResponse.class)
                    .block();
            if (response == null || response.user() == null || response.user().name() == null) {
                return Optional.empty();
            }
            return Optional.of(response.user().name());
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}
