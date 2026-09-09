CREATE TABLE listening_event_attributions (
    id                      BIGSERIAL PRIMARY KEY,
    listening_event_id      BIGINT NOT NULL REFERENCES listening_events (id) ON DELETE CASCADE,
    eurovision_artist_id    BIGINT NOT NULL REFERENCES eurovision_artists (id) ON DELETE CASCADE,
    attribution_type        VARCHAR(32) NOT NULL,
    confidence              VARCHAR(32) NOT NULL,
    CONSTRAINT uq_listening_event_attributions UNIQUE (listening_event_id, eurovision_artist_id),
    CONSTRAINT chk_listening_event_attributions_type
        CHECK (attribution_type IN ('PRIMARY', 'FEATURED', 'COLLABORATION'))
);

CREATE INDEX idx_listening_event_attributions_artist_id ON listening_event_attributions (eurovision_artist_id);
