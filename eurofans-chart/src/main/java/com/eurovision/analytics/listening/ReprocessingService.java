package com.eurovision.analytics.listening;

import com.eurovision.analytics.eurovision.identity.EurovisionIdentityResolver;
import com.eurovision.analytics.eurovision.identity.IdentityResolution;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@code /reprocess}: after an admin adds an artist, alias, or external-id
 * mapping, re-run identity resolution over every previously unresolved raw
 * event. Never rewrites the raw provider fields (spec 6.3) -- only
 * {@code canonicalArtist}/{@code resolutionStatus}/{@code resolutionMethod}
 * /{@code resolutionConfidence} and the derived attribution row change.
 */
@Service
public class ReprocessingService {

    private static final int PAGE_SIZE = 500;

    private final ListeningEventRepository listeningEventRepository;
    private final ListeningEventAttributionRepository attributionRepository;
    private final EurovisionIdentityResolver identityResolver;

    public ReprocessingService(ListeningEventRepository listeningEventRepository,
                                ListeningEventAttributionRepository attributionRepository,
                                EurovisionIdentityResolver identityResolver) {
        this.listeningEventRepository = listeningEventRepository;
        this.attributionRepository = attributionRepository;
        this.identityResolver = identityResolver;
    }

    /** @return how many events changed resolution status as a result. */
    @Transactional
    public int reprocessUnresolved() {
        // Simple full re-scan, paged, since this table is not expected to be enormous
        // relative to a single admin-triggered maintenance operation.
        int changed = 0;
        int pageNumber = 0;
        while (true) {
            var page = listeningEventRepository.findAll(PageRequest.of(pageNumber, PAGE_SIZE));
            for (ListeningEvent event : page) {
                if (event.getResolutionStatus() != ResolutionStatus.UNKNOWN
                        && event.getResolutionStatus() != ResolutionStatus.CONFLICT) {
                    continue;
                }
                IdentityResolution resolution = identityResolver.resolve(
                        event.getProvider(), event.getProviderArtistId(), event.getRawArtistName());
                if (resolution.status() != event.getResolutionStatus()) {
                    event.setCanonicalArtist(resolution.canonicalArtist());
                    event.setResolutionMethod(resolution.method());
                    event.setResolutionStatus(resolution.status());
                    event.setResolutionConfidence(resolution.confidence());
                    listeningEventRepository.save(event);
                    if (resolution.status() == ResolutionStatus.CONFIRMED && resolution.canonicalArtist() != null) {
                        attributionRepository.save(new ListeningEventAttribution(
                                event, resolution.canonicalArtist(), AttributionType.PRIMARY, resolution.confidence()));
                    }
                    changed++;
                }
            }
            if (!page.hasNext()) {
                break;
            }
            pageNumber++;
        }
        return changed;
    }
}
