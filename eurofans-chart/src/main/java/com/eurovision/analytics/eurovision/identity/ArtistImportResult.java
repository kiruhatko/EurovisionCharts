package com.eurovision.analytics.eurovision.identity;

import com.eurovision.analytics.eurovision.EurovisionArtist;

import java.util.List;

public sealed interface ArtistImportResult {

    record PendingConfirmation(EurovisionArtist artist, ImportConfidence confidence,
                                List<String> musicBrainzCandidateNames) implements ArtistImportResult {
    }

    record AlreadyLinked(EurovisionArtist artist) implements ArtistImportResult {
    }

    record Failed(String reason) implements ArtistImportResult {
    }
}
