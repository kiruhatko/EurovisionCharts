package com.eurovision.analytics.eurovision.identity;

import com.eurovision.analytics.eurovision.ExternalIdProvider;

/** The result of resolving an admin-supplied provider URL to that provider's own native artist identity. */
public record ResolvedProviderArtist(
        ExternalIdProvider provider,
        String externalId,
        String displayName,
        String canonicalUrl
) {
}
