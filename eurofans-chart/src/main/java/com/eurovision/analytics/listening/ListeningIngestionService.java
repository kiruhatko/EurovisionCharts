package com.eurovision.analytics.listening;

import com.eurovision.analytics.connectedaccount.ConnectedAccount;
import com.eurovision.analytics.eurovision.identity.EurovisionIdentityResolver;
import com.eurovision.analytics.eurovision.identity.IdentityResolution;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Turns one provider-reported {@link RecentPlay} into a persisted
 * {@link ListeningEvent}, applying fingerprint-based duplicate protection
 * (spec 7) before ever touching identity resolution. Never called for a live
 * now-playing signal -- only for a provider's completed-history endpoint.
 */
@Service
public class ListeningIngestionService {

    private final ListeningEventRepository listeningEventRepository;
    private final ListeningEventAttributionRepository attributionRepository;
    private final EurovisionIdentityResolver identityResolver;

    public ListeningIngestionService(ListeningEventRepository listeningEventRepository,
                                      ListeningEventAttributionRepository attributionRepository,
                                      EurovisionIdentityResolver identityResolver) {
        this.listeningEventRepository = listeningEventRepository;
        this.attributionRepository = attributionRepository;
        this.identityResolver = identityResolver;
    }

    /** @return true if a new listening event was persisted, false if it was a duplicate. */
    @Transactional
    public boolean ingest(ConnectedAccount account, RecentPlay play) {
        String timestampKey = play.playedAtUtc() != null ? play.playedAtUtc().toString() : "";
        String fingerprint = ListeningEventFingerprint.compute(
                account.getUser().getId(), account.getProvider(), timestampKey,
                play.rawArtistName(), play.rawTrackName(), play.rawAlbumName(), play.providerTrackId());

        if (listeningEventRepository.existsByFingerprint(fingerprint)) {
            return false;
        }

        IdentityResolution resolution = identityResolver.resolveAny(account.getProvider(), play.providerArtistId(),
                play.rawArtistName(), play.rawTrackName(), play.rawAlbumName(), play.additionalArtists());

        ListeningEvent event = new ListeningEvent();
        event.setUser(account.getUser());
        event.setConnectedAccount(account);
        event.setProvider(account.getProvider());
        event.setProviderTrackId(play.providerTrackId());
        event.setProviderArtistId(play.providerArtistId());
        event.setRawArtistName(play.rawArtistName());
        event.setRawTrackName(play.rawTrackName());
        event.setRawAlbumName(play.rawAlbumName());
        event.setPlayedAtUtc(play.playedAtUtc());
        event.setEstimatedTimestamp(play.estimatedTimestamp());
        event.setCanonicalArtist(resolution.canonicalArtist());
        event.setResolutionMethod(resolution.method());
        event.setResolutionStatus(resolution.status());
        event.setResolutionConfidence(resolution.confidence());
        event.setFingerprint(fingerprint);
        listeningEventRepository.save(event);

        if (resolution.status() == ResolutionStatus.CONFIRMED && resolution.canonicalArtist() != null) {
            attributionRepository.save(new ListeningEventAttribution(
                    event, resolution.canonicalArtist(), AttributionType.PRIMARY, resolution.confidence()));
        }

        return true;
    }
}
