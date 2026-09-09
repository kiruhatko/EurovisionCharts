CREATE TABLE chart_snapshots (
    id                  BIGSERIAL PRIMARY KEY,
    period              VARCHAR(16) NOT NULL,
    period_start        TIMESTAMPTZ NOT NULL,
    period_end          TIMESTAMPTZ NOT NULL,
    artist_id           BIGINT NOT NULL REFERENCES eurovision_artists (id),
    rank                INTEGER NOT NULL,
    listen_count        BIGINT NOT NULL,
    unique_listeners    BIGINT NOT NULL,
    growth_percent      NUMERIC(10, 2),
    generated_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_chart_snapshots_period CHECK (period IN ('WEEK', 'MONTH', 'YEAR', 'ALL_TIME'))
);

CREATE INDEX idx_chart_snapshots_period_window ON chart_snapshots (period, period_start, period_end);
CREATE INDEX idx_chart_snapshots_artist_id ON chart_snapshots (artist_id);
