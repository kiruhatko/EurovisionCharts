package com.eurovision.analytics.eurovision.identity;

public enum ImportConfidence {
    /** Exactly one MusicBrainz artist relates to the exact submitted URL. */
    CONFIRMED,
    /** No MusicBrainz crosscheck match was found; provider identity alone. */
    PROBABLE,
    /** More than one distinct MusicBrainz artist relates to this URL: never auto-picked. */
    CONFLICT
}
