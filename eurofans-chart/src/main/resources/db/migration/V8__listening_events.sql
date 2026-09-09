CREATE TABLE listening_events (
    id                          BIGSERIAL PRIMARY KEY,
    user_id                     BIGINT NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    connected_account_id        BIGINT NOT NULL REFERENCES connected_accounts (id) ON DELETE CASCADE,
    provider                    VARCHAR(32) NOT NULL,
    provider_track_id           VARCHAR(500),
    provider_artist_id          VARCHAR(500),
    raw_artist_name             VARCHAR(1000) NOT NULL,
    raw_track_name              VARCHAR(1000) NOT NULL,
    raw_album_name               VARCHAR(1000),
    played_at_utc                TIMESTAMPTZ,
    is_estimated_timestamp       BOOLEAN NOT NULL DEFAULT FALSE,
    canonical_artist_id          BIGINT REFERENCES eurovision_artists (id),
    resolution_method            VARCHAR(64),
    resolution_status            VARCHAR(32) NOT NULL DEFAULT 'UNKNOWN',
    resolution_confidence        VARCHAR(32),
    fingerprint                  CHAR(64) NOT NULL,
    created_at                   TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_listening_events_fingerprint UNIQUE (fingerprint),
    CONSTRAINT chk_listening_events_provider CHECK (provider IN ('SPOTIFY', 'APPLE_MUSIC', 'SOUNDCLOUD', 'LASTFM')),
    CONSTRAINT chk_listening_events_resolution_status
        CHECK (resolution_status IN ('CONFIRMED', 'PROBABLE', 'UNKNOWN', 'CONFLICT', 'REJECTED'))
);

CREATE INDEX idx_listening_events_user_id ON listening_events (user_id);
CREATE INDEX idx_listening_events_canonical_artist_id ON listening_events (canonical_artist_id);
CREATE INDEX idx_listening_events_played_at_utc ON listening_events (played_at_utc);
CREATE INDEX idx_listening_events_resolution_status ON listening_events (resolution_status);
