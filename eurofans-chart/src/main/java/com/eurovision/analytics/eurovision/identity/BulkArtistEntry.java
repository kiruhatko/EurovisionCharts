package com.eurovision.analytics.eurovision.identity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/** One row of the {@code eurovision-artists.json} bulk-import file (spec 6.1.9). */
@JsonIgnoreProperties(ignoreUnknown = true)
public record BulkArtistEntry(
        @JsonProperty("canonicalName") String canonicalName,
        @JsonProperty("countryIso") String countryIso,
        @JsonProperty("editionYear") Integer editionYear,
        @JsonProperty("urls") List<String> urls
) {
}
