package com.eurovision.analytics.connectedaccount;

import java.util.List;

/**
 * The four supported listening-history providers. This is the single
 * authoritative ordering used by {@code NowPlayingResolver} for the
 * Spotify -> Apple Music -> SoundCloud -> Last.fm priority chain (section 5
 * of the spec) as well as by {@code TokenRefreshScheduler} and every
 * per-provider sync scheduler.
 */
public enum Provider {
    SPOTIFY,
    APPLE_MUSIC,
    SOUNDCLOUD,
    LASTFM;

    /** Fixed now-playing / /last resolution priority order. Never reorder. */
    public static final List<Provider> PRIORITY_ORDER = List.of(SPOTIFY, APPLE_MUSIC, SOUNDCLOUD, LASTFM);
}
