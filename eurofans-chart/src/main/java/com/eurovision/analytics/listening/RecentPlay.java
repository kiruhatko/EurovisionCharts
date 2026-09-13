package com.eurovision.analytics.listening;

import com.eurovision.analytics.eurovision.identity.ArtistCredit;

import java.time.Instant;
import java.util.List;

/** One completed play as reported by a provider's history/recently-played endpoint. */
public record RecentPlay(
        String rawArtistName,
        String rawTrackName,
        String rawAlbumName,
        String providerTrackId,
        String providerArtistId,
        Instant playedAtUtc,
        boolean estimatedTimestamp,
        String nativeArtworkUrl,
        // Every OTHER artist credited on the track (feat./collab), beyond the primary
        // one above. Empty for providers that only ever report a single artist.
        List<ArtistCredit> additionalArtists
) {
}
