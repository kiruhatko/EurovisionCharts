package com.eurovision.analytics.eurovision.identity;

import com.eurovision.analytics.eurovision.ExternalIdProvider;

import java.net.URI;
import java.util.Optional;

/** One implementation per provider, resolving a canonicalized artist URL to that provider's own native identity. */
public interface ProviderArtistResolver {

    ExternalIdProvider provider();

    Optional<ResolvedProviderArtist> resolve(URI canonicalUri);

    /**
     * For admin-curated bulk links: resolve without calling the provider when the URL itself
     * already carries the id, so a /reloadartists of ~1000 links doesn't spend the API quota.
     */
    default Optional<ResolvedProviderArtist> resolveLinkOnly(URI canonicalUri) {
        return resolve(canonicalUri);
    }
}
