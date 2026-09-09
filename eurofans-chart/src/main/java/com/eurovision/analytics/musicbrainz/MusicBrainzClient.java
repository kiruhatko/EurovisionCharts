package com.eurovision.analytics.musicbrainz;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * MusicBrainz artist entities frequently carry relationship URLs pointing at
 * an artist's Spotify / Apple Music / SoundCloud / Last.fm pages. This client
 * asks MusicBrainz "which artist(s), if any, relate to this exact external
 * URL" -- the crosscheck required before {@code /addartist} ever creates a
 * VERIFIED artist (spec 6.1).
 */
@Component
public class MusicBrainzClient {

    private static final Logger log = LoggerFactory.getLogger(MusicBrainzClient.class);

    private final WebClient musicBrainzApiWebClient;

    public MusicBrainzClient(WebClient musicBrainzApiWebClient) {
        this.musicBrainzApiWebClient = musicBrainzApiWebClient;
    }

    public MusicBrainzCrosscheckResult crosscheckByUrl(URI exactUrl) {
        try {
            MusicBrainzDtos.UrlLookupResponse response = musicBrainzApiWebClient.get()
                    .uri(uriBuilder -> uriBuilder.path("/ws/2/url")
                            .queryParam("resource", exactUrl.toString())
                            .queryParam("inc", "artist-rels")
                            .queryParam("fmt", "json")
                            .build())
                    .retrieve()
                    .bodyToMono(MusicBrainzDtos.UrlLookupResponse.class)
                    .block();

            if (response == null || response.relations() == null || response.relations().isEmpty()) {
                return MusicBrainzCrosscheckResult.of(MusicBrainzCrosscheckResult.Outcome.NO_MATCH, List.of());
            }

            Map<String, MusicBrainzDtos.ArtistRef> distinctArtists = response.relations().stream()
                    .map(MusicBrainzDtos.Relation::artist)
                    .filter(a -> a != null && a.id() != null)
                    .collect(Collectors.toMap(MusicBrainzDtos.ArtistRef::id, Function.identity(), (a, b) -> a));

            if (distinctArtists.isEmpty()) {
                return MusicBrainzCrosscheckResult.of(MusicBrainzCrosscheckResult.Outcome.NO_MATCH, List.of());
            }
            if (distinctArtists.size() == 1) {
                return MusicBrainzCrosscheckResult.of(MusicBrainzCrosscheckResult.Outcome.SINGLE_MATCH,
                        List.copyOf(distinctArtists.values()));
            }
            return MusicBrainzCrosscheckResult.of(MusicBrainzCrosscheckResult.Outcome.AMBIGUOUS,
                    List.copyOf(distinctArtists.values()));
        } catch (Exception e) {
            log.warn("MusicBrainz crosscheck failed for url={}: {}", exactUrl, e.getMessage());
            return MusicBrainzCrosscheckResult.of(MusicBrainzCrosscheckResult.Outcome.LOOKUP_FAILED, List.of());
        }
    }

    /**
     * Best-effort recording search used only for artwork resolution (spec 9):
     * find the highest-scoring recording matching this exact artist + track
     * name, and return the MBID of its first associated release, if any.
     */
    public Optional<String> findReleaseForRecording(String artistName, String trackName) {
        try {
            String query = "artist:\"" + escapeLucene(artistName) + "\" AND recording:\"" + escapeLucene(trackName) + "\"";
            MusicBrainzDtos.RecordingSearchResponse response = musicBrainzApiWebClient.get()
                    .uri(uriBuilder -> uriBuilder.path("/ws/2/recording")
                            .queryParam("query", query)
                            .queryParam("fmt", "json")
                            .queryParam("limit", 1)
                            .build())
                    .retrieve()
                    .bodyToMono(MusicBrainzDtos.RecordingSearchResponse.class)
                    .block();
            if (response == null || response.recordings() == null || response.recordings().isEmpty()) {
                return Optional.empty();
            }
            MusicBrainzDtos.Recording best = response.recordings().get(0);
            if (best.releases() == null || best.releases().isEmpty()) {
                return Optional.empty();
            }
            return Optional.ofNullable(best.releases().get(0).id());
        } catch (Exception e) {
            log.warn("MusicBrainz recording search failed for artist={} track={}: {}", artistName, trackName, e.getMessage());
            return Optional.empty();
        }
    }

    private static String escapeLucene(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
