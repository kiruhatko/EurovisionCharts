package com.eurovision.analytics.artwork;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Optional;

/** The highest-quality artwork source when a confirmed MusicBrainz release match exists (spec 9). */
@Component
public class CoverArtArchiveClient {

    private static final Logger log = LoggerFactory.getLogger(CoverArtArchiveClient.class);

    private final WebClient coverArtArchiveWebClient;

    public CoverArtArchiveClient(WebClient coverArtArchiveWebClient) {
        this.coverArtArchiveWebClient = coverArtArchiveWebClient;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Image(
            @JsonProperty("image") String image,
            @JsonProperty("front") boolean front
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ReleaseArtwork(
            @JsonProperty("images") List<Image> images
    ) {
    }

    public Optional<String> fetchFrontImageUrl(String releaseMbid) {
        try {
            ReleaseArtwork artwork = coverArtArchiveWebClient.get()
                    .uri("/release/{mbid}", releaseMbid)
                    .retrieve()
                    .bodyToMono(ReleaseArtwork.class)
                    .block();
            if (artwork == null || artwork.images() == null) {
                return Optional.empty();
            }
            return artwork.images().stream()
                    .filter(Image::front)
                    .map(Image::image)
                    .findFirst()
                    .or(() -> artwork.images().stream().map(Image::image).findFirst());
        } catch (Exception e) {
            log.debug("No Cover Art Archive artwork for release={}: {}", releaseMbid, e.getMessage());
            return Optional.empty();
        }
    }
}
