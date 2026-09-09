CREATE TABLE eurovision_artist_aliases (
    id                  BIGSERIAL PRIMARY KEY,
    artist_id           BIGINT NOT NULL REFERENCES eurovision_artists (id) ON DELETE CASCADE,
    alias               VARCHAR(500) NOT NULL,
    normalized_alias    VARCHAR(500) NOT NULL,
    active              BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT uq_eurovision_artist_aliases_artist_alias UNIQUE (artist_id, normalized_alias)
);

CREATE INDEX idx_eurovision_artist_aliases_normalized_alias ON eurovision_artist_aliases (normalized_alias);
