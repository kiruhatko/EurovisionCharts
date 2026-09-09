package com.eurovision.analytics.oauth.soundcloud;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public final class SoundCloudDtos {

    private SoundCloudDtos() {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TokenResponse(
            @JsonProperty("access_token") String accessToken,
            @JsonProperty("refresh_token") String refreshToken,
            @JsonProperty("expires_in") long expiresInSeconds,
            @JsonProperty("scope") String scope,
            @JsonProperty("token_type") String tokenType
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record UserProfile(
            @JsonProperty("id") long id,
            @JsonProperty("username") String username,
            @JsonProperty("permalink_url") String permalinkUrl
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record UserRef(
            @JsonProperty("id") long id,
            @JsonProperty("username") String username
    ) {
    }

    /**
     * SoundCloud has no "artist" entity; the uploader ({@code user}) IS the
     * identity anchor for matching a track to a Eurovision artist (spec 6.2).
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Track(
            @JsonProperty("id") long id,
            @JsonProperty("title") String title,
            @JsonProperty("user") UserRef user,
            @JsonProperty("artwork_url") String artworkUrl
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PlayHistoryEntry(
            @JsonProperty("track") Track track,
            @JsonProperty("played_at") Long playedAtEpochMillis
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PlayHistoryResponse(
            @JsonProperty("collection") List<PlayHistoryEntry> collection
    ) {
    }
}
