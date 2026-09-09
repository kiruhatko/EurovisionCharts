package com.eurovision.analytics.eurovision.identity;

import com.eurovision.analytics.connectedaccount.Provider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UnresolvedArtistQueueService {

    private final UnresolvedArtistEntityRepository repository;

    public UnresolvedArtistQueueService(UnresolvedArtistEntityRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void recordOccurrence(Provider provider, String rawExternalId, String rawName) {
        repository.findByProviderAndRawExternalIdAndStatus(provider, rawExternalId, UnresolvedStatus.PENDING_REVIEW)
                .ifPresentOrElse(
                        UnresolvedArtistEntity::recordAnotherOccurrence,
                        () -> repository.save(new UnresolvedArtistEntity(rawName, rawExternalId, provider)));
    }
}
