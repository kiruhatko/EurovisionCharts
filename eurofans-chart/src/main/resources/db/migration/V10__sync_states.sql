CREATE TABLE sync_states (
    id                  BIGSERIAL PRIMARY KEY,
    user_id             BIGINT NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    provider            VARCHAR(32) NOT NULL,
    last_synced_at      TIMESTAMPTZ,
    last_cursor         VARCHAR(500),
    last_status         VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    consecutive_failures INTEGER NOT NULL DEFAULT 0,
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_sync_states_user_provider UNIQUE (user_id, provider),
    CONSTRAINT chk_sync_states_provider CHECK (provider IN ('SPOTIFY', 'APPLE_MUSIC', 'SOUNDCLOUD', 'LASTFM'))
);
