package com.eurovision.analytics.artwork;

import com.eurovision.analytics.connectedaccount.Provider;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "artwork_cache")
@Getter
@Setter
@NoArgsConstructor
public class ArtworkCache {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "lookup_key", nullable = false, unique = true, length = 700)
    private String lookupKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false, length = 32)
    private Provider provider;

    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false, length = 32)
    private ArtworkSource source;

    @Column(name = "original_url", nullable = false, length = 1000)
    private String originalUrl;

    @Column(name = "content_type", nullable = false, length = 100)
    private String contentType;

    @Column(name = "width")
    private Integer width;

    @Column(name = "height")
    private Integer height;

    @Column(name = "byte_size", nullable = false)
    private int byteSize;

    @Column(name = "sha256", nullable = false, length = 64)
    private String sha256;

    @Column(name = "cached_at", nullable = false)
    private Instant cachedAt = Instant.now();

    public ArtworkCache(String lookupKey, Provider provider, ArtworkSource source, String originalUrl,
                         String contentType, Integer width, Integer height, int byteSize, String sha256) {
        this.lookupKey = lookupKey;
        this.provider = provider;
        this.source = source;
        this.originalUrl = originalUrl;
        this.contentType = contentType;
        this.width = width;
        this.height = height;
        this.byteSize = byteSize;
        this.sha256 = sha256;
    }
}
