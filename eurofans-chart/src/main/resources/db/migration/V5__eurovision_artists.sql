CREATE TABLE eurovision_artists (
    id                  BIGSERIAL PRIMARY KEY,
    canonical_name      VARCHAR(500) NOT NULL,
    normalized_name     VARCHAR(500) NOT NULL,
    country_id          BIGINT REFERENCES eurovision_countries (id),
    edition_id          BIGINT REFERENCES eurovision_editions (id),
    status              VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    active              BOOLEAN NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_eurovision_artists_status CHECK (status IN ('PENDING', 'VERIFIED', 'DISABLED', 'CONFLICT', 'UNRESOLVED'))
);

CREATE INDEX idx_eurovision_artists_normalized_name ON eurovision_artists (normalized_name);
CREATE INDEX idx_eurovision_artists_status_active ON eurovision_artists (status, active);
