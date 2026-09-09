package com.eurovision.analytics.nowplaying;

/** A provider reporting "something is playing right now" for a connected account. */
public record NowPlayingSignal(
        String rawArtistName,
        String rawTrackName,
        String rawAlbumName,
        String providerTrackId,
        String providerArtistId,
        String nativeArtworkUrl
) {
}
