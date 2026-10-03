package com.eurovision.analytics.oauth.spotify;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Every external JSON response from Spotify is parsed strictly into one of
 * these records (never mapped directly onto a JPA entity), per the project's
 * standing DTO-parsing discipline for untrusted external input.
 */
public final class SpotifyDtos {

    private SpotifyDtos() {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TokenResponse(
            @JsonProperty("access_token") String accessToken,
            @JsonProperty("token_type") String tokenType,
            @JsonProperty("scope") String scope,
            @JsonProperty("expires_in") long expiresInSeconds,
            @JsonProperty("refresh_token") String refreshToken
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record UserProfile(
            @JsonProperty("id") String id,
            @JsonProperty("display_name") String displayName
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Image(
            @JsonProperty("url") String url,
            @JsonProperty("width") Integer width,
            @JsonProperty("height") Integer height
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Album(
            @JsonProperty("name") String name,
            @JsonProperty("images") List<Image> images
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ArtistRef(
            @JsonProperty("id") String id,
            @JsonProperty("name") String name
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Track(
            @JsonProperty("id") String id,
            @JsonProperty("name") String name,
            @JsonProperty("artists") List<ArtistRef> artists,
            @JsonProperty("album") Album album
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CurrentlyPlaying(
            @JsonProperty("is_playing") boolean isPlaying,
            @JsonProperty("item") Track item
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PlayHistoryItem(
            @JsonProperty("track") Track track,
            @JsonProperty("played_at") String playedAt
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record RecentlyPlayedResponse(
            @JsonProperty("items") List<PlayHistoryItem> items
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ArtistDetail(
            @JsonProperty("id") String id,
            @JsonProperty("name") String name
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ArtistsPage(
            @JsonProperty("items") List<ArtistDetail> items
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ArtistSearchResponse(
            @JsonProperty("artists") ArtistsPage artists
    ) {
    }
}
