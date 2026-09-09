package com.eurovision.analytics.nowplaying;

import com.eurovision.analytics.listening.ListeningEvent;

public sealed interface LastTrackResult {

    record Found(ListeningEvent event) implements LastTrackResult {
    }

    record NothingFound() implements LastTrackResult {
    }
}
