package com.eurovision.analytics.oauth.lastfm;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public final class LastFmDtos {

    private LastFmDtos() {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TextRef(
            @JsonProperty("#text") String text,
            @JsonProperty("mbid") String mbid
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record DateRef(
            @JsonProperty("uts") String epochSeconds
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record NowPlayingAttr(
            @JsonProperty("nowplaying") String nowPlaying
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TrackEntry(
            @JsonProperty("artist") TextRef artist,
            @JsonProperty("name") String name,
            @JsonProperty("album") TextRef album,
            @JsonProperty("date") DateRef date,
            @JsonProperty("@attr") NowPlayingAttr attr
    ) {
        public boolean isNowPlaying() {
            return attr != null && "true".equalsIgnoreCase(attr.nowPlaying());
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record RecentTracks(
            @JsonProperty("track") List<TrackEntry> track
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record RecentTracksResponse(
            @JsonProperty("recenttracks") RecentTracks recenttracks
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record UserInfo(
            @JsonProperty("name") String name
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record UserInfoResponse(
            @JsonProperty("user") UserInfo user
    ) {
    }
}
