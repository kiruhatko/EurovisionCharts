CREATE TABLE connected_accounts (
    id                          BIGSERIAL PRIMARY KEY,
    user_id                     BIGINT NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    provider                    VARCHAR(32) NOT NULL,
    provider_account_id         VARCHAR(255) NOT NULL,
    provider_display_name       VARCHAR(255),
    access_token_encrypted      BYTEA,
    refresh_token_encrypted     BYTEA,
    token_expires_at            TIMESTAMPTZ,
    scopes                      TEXT,
    status                      VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    connected_at                TIMESTAMPTZ NOT NULL DEFAULT now(),
    last_token_refresh_at       TIMESTAMPTZ,
    last_sync_at                TIMESTAMPTZ,
    last_sync_status            VARCHAR(32),
    consecutive_failures        INTEGER NOT NULL DEFAULT 0,
    disconnected_at             TIMESTAMPTZ,
    CONSTRAINT uq_connected_accounts_user_provider UNIQUE (user_id, provider),
    CONSTRAINT chk_connected_accounts_provider CHECK (provider IN ('SPOTIFY', 'APPLE_MUSIC', 'SOUNDCLOUD', 'LASTFM')),
    CONSTRAINT chk_connected_accounts_status CHECK (status IN ('ACTIVE', 'EXPIRED', 'REVOKED', 'ERROR', 'NOT_CONFIGURED'))
);

CREATE INDEX idx_connected_accounts_user_id ON connected_accounts (user_id);
CREATE INDEX idx_connected_accounts_provider_status ON connected_accounts (provider, status);
