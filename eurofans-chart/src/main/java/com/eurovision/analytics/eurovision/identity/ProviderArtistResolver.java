package com.eurovision.analytics.eurovision.identity;

import com.eurovision.analytics.eurovision.ExternalIdProvider;

import java.net.URI;
import java.util.Optional;

/** One implementation per provider, resolving a canonicalized artist URL to that provider's own native identity. */
public interface ProviderArtistResolver {

    ExternalIdProvider provider();

    Optional<ResolvedProviderArtist> resolve(URI canonicalUri);
}
