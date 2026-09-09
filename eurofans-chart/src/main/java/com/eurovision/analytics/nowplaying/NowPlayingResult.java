package com.eurovision.analytics.nowplaying;

import com.eurovision.analytics.connectedaccount.Provider;
import com.eurovision.analytics.eurovision.EurovisionArtist;

public sealed interface NowPlayingResult {

    /** A Eurovision-relevant track is playing right now: full card, per spec 8's /track format. */
    record EurovisionTrack(Provider provider, NowPlayingSignal signal, EurovisionArtist artist) implements NowPlayingResult {
    }

    /** Something is playing, but it is not Eurovision-relevant: never reveal what it actually is. */
    record NonEurovision(Provider provider) implements NowPlayingResult {
    }

    /** No connected provider reported an active playback signal. */
    record NothingPlaying() implements NowPlayingResult {
    }
}
