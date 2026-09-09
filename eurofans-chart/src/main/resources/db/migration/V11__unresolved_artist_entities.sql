CREATE TABLE unresolved_artist_entities (
    id                  BIGSERIAL PRIMARY KEY,
    raw_name            VARCHAR(1000) NOT NULL,
    raw_external_id     VARCHAR(500),
    provider            VARCHAR(32) NOT NULL,
    first_seen_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    last_seen_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    occurrences         INTEGER NOT NULL DEFAULT 1,
    status              VARCHAR(32) NOT NULL DEFAULT 'PENDING_REVIEW',
    possible_matches    TEXT,
    notes               TEXT,
    CONSTRAINT chk_unresolved_artist_entities_provider CHECK (provider IN ('SPOTIFY', 'APPLE_MUSIC', 'SOUNDCLOUD', 'LASTFM')),
    CONSTRAINT chk_unresolved_artist_entities_status
        CHECK (status IN ('PENDING_REVIEW', 'RESOLVED', 'IGNORED'))
);

CREATE INDEX idx_unresolved_artist_entities_provider_raw
    ON unresolved_artist_entities (provider, raw_external_id);
