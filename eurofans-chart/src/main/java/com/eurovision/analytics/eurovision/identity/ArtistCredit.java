package com.eurovision.analytics.eurovision.identity;

/**
 * One artist credited on a track, beyond the primary artist a provider
 * reports. Used so a collab/feat. track resolves as long as ANY credited
 * artist has an exact registered provider ID -- never by name-guessing a
 * featured artist (spec: no guessing).
 */
public record ArtistCredit(String providerArtistId, String rawArtistName) {
}
