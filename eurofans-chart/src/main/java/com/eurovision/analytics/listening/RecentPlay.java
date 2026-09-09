package com.eurovision.analytics.listening;

import java.time.Instant;

/** One completed play as reported by a provider's history/recently-played endpoint. */
public record RecentPlay(
        String rawArtistName,
        String rawTrackName,
        String rawAlbumName,
        String providerTrackId,
        String providerArtistId,
        Instant playedAtUtc,
        boolean estimatedTimestamp,
        String nativeArtworkUrl
) {
}
