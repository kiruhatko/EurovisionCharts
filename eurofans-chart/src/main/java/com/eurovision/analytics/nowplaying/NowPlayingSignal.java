package com.eurovision.analytics.nowplaying;

import com.eurovision.analytics.eurovision.identity.ArtistCredit;

import java.util.List;

/** A provider reporting "something is playing right now" for a connected account. */
public record NowPlayingSignal(
        String rawArtistName,
        String rawTrackName,
        String rawAlbumName,
        String providerTrackId,
        String providerArtistId,
        String nativeArtworkUrl,
        // Every OTHER artist credited on the track (feat./collab), beyond the primary
        // one above. Empty for providers that only ever report a single artist.
        List<ArtistCredit> additionalArtists
) {
}
