CREATE TABLE artwork_cache (
    id              BIGSERIAL PRIMARY KEY,
    lookup_key      VARCHAR(700) NOT NULL,
    provider        VARCHAR(32) NOT NULL,
    source          VARCHAR(32) NOT NULL,
    original_url    VARCHAR(1000) NOT NULL,
    content_type    VARCHAR(100) NOT NULL,
    width           INTEGER,
    height          INTEGER,
    byte_size       INTEGER NOT NULL,
    sha256          VARCHAR(64) NOT NULL,
    cached_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_artwork_cache_lookup_key UNIQUE (lookup_key),
    CONSTRAINT chk_artwork_cache_source CHECK (source IN ('COVER_ART_ARCHIVE', 'PROVIDER_NATIVE'))
);
