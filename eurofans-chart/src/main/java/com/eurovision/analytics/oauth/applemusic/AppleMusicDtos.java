package com.eurovision.analytics.oauth.applemusic;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public final class AppleMusicDtos {

    private AppleMusicDtos() {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Artwork(
            @JsonProperty("url") String urlTemplate,
            @JsonProperty("width") Integer width,
            @JsonProperty("height") Integer height
    ) {
        /** Apple's {@code {w}x{h}} template resolved to the EXACT size the API reported for this file, never larger. */
        public String resolvedUrl() {
            if (urlTemplate == null || width == null || height == null) {
                return urlTemplate;
            }
            return urlTemplate.replace("{w}", String.valueOf(width)).replace("{h}", String.valueOf(height));
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record SongAttributes(
            @JsonProperty("name") String name,
            @JsonProperty("artistName") String artistName,
            @JsonProperty("albumName") String albumName,
            @JsonProperty("artwork") Artwork artwork
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record SongResource(
            @JsonProperty("id") String id,
            @JsonProperty("type") String type,
            @JsonProperty("attributes") SongAttributes attributes
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record SongsResponse(
            @JsonProperty("data") List<SongResource> data
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record StorefrontProbeResponse(
            @JsonProperty("data") List<Object> data
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ArtistAttributes(
            @JsonProperty("name") String name
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ArtistResource(
            @JsonProperty("id") String id,
            @JsonProperty("attributes") ArtistAttributes attributes
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ArtistsResponse(
            @JsonProperty("data") List<ArtistResource> data
    ) {
    }
}
