package com.eurovision.analytics.nowplaying;

import com.eurovision.analytics.connectedaccount.ConnectedAccount;
import com.eurovision.analytics.connectedaccount.Provider;

import java.util.Optional;

/**
 * One implementation per provider, consulted in strict priority order by
 * {@code NowPlayingResolver}. Returning {@link Optional#empty()} means "no
 * live signal from this provider right now" and the resolver moves on to the
 * next provider in the chain; it does NOT mean an error occurred.
 */
public interface NowPlayingProviderClient {

    Provider provider();

    Optional<NowPlayingSignal> fetchNowPlaying(ConnectedAccount account);
}
