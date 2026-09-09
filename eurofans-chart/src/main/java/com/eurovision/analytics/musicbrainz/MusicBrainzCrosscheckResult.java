package com.eurovision.analytics.musicbrainz;

import java.util.List;

public record MusicBrainzCrosscheckResult(Outcome outcome, List<MusicBrainzDtos.ArtistRef> candidates) {

    public enum Outcome {
        /** Exactly one distinct MusicBrainz artist relates to this exact URL. */
        SINGLE_MATCH,
        /** No MusicBrainz "url" entity relates to this exact URL at all. */
        NO_MATCH,
        /** More than one distinct MusicBrainz artist relates to this URL: never auto-picked. */
        AMBIGUOUS,
        /** The MusicBrainz API call itself failed (network, rate limit, etc). */
        LOOKUP_FAILED
    }

    public static MusicBrainzCrosscheckResult of(Outcome outcome, List<MusicBrainzDtos.ArtistRef> candidates) {
        return new MusicBrainzCrosscheckResult(outcome, candidates);
    }
}
