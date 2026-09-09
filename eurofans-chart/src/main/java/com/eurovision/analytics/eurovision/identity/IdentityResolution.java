package com.eurovision.analytics.eurovision.identity;

import com.eurovision.analytics.eurovision.EurovisionArtist;
import com.eurovision.analytics.listening.ResolutionStatus;

public record IdentityResolution(EurovisionArtist canonicalArtist, String method, ResolutionStatus status,
                                  String confidence) {

    public static IdentityResolution unknown() {
        return new IdentityResolution(null, null, ResolutionStatus.UNKNOWN, null);
    }

    public static IdentityResolution conflict() {
        return new IdentityResolution(null, "AMBIGUOUS_CANDIDATES", ResolutionStatus.CONFLICT, null);
    }
}
