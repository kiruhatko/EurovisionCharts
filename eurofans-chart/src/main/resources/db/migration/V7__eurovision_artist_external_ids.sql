CREATE TABLE eurovision_artist_external_ids (
    id              BIGSERIAL PRIMARY KEY,
    artist_id       BIGINT NOT NULL REFERENCES eurovision_artists (id) ON DELETE CASCADE,
    provider        VARCHAR(32) NOT NULL,
    external_id     VARCHAR(500) NOT NULL,
    external_url    VARCHAR(1000),
    verified        BOOLEAN NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_eurovision_artist_external_ids_provider_external_id UNIQUE (provider, external_id),
    CONSTRAINT chk_eurovision_artist_external_ids_provider
        CHECK (provider IN ('SPOTIFY', 'APPLE_MUSIC', 'SOUNDCLOUD', 'LASTFM', 'MUSICBRAINZ'))
);

CREATE INDEX idx_eurovision_artist_external_ids_artist_id ON eurovision_artist_external_ids (artist_id);
