package com.eurovision.analytics.musicbrainz;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public final class MusicBrainzDtos {

    private MusicBrainzDtos() {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Area(
            @JsonProperty("name") String name
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ArtistRef(
            @JsonProperty("id") String id,
            @JsonProperty("name") String name,
            @JsonProperty("disambiguation") String disambiguation,
            @JsonProperty("area") Area area
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Relation(
            @JsonProperty("type") String type,
            @JsonProperty("target-type") String targetType,
            @JsonProperty("artist") ArtistRef artist
    ) {
    }

    /** Response of {@code GET /ws/2/url?resource=<url>&inc=artist-rels&fmt=json}. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record UrlLookupResponse(
            @JsonProperty("id") String id,
            @JsonProperty("resource") String resource,
            @JsonProperty("relations") List<Relation> relations
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ReleaseRef(
            @JsonProperty("id") String id,
            @JsonProperty("title") String title
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Recording(
            @JsonProperty("id") String id,
            @JsonProperty("score") Integer score,
            @JsonProperty("releases") List<ReleaseRef> releases
    ) {
    }

    /** Response of {@code GET /ws/2/recording?query=...&fmt=json}. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record RecordingSearchResponse(
            @JsonProperty("recordings") List<Recording> recordings
    ) {
    }
}
