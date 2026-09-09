-- Composite index supporting chart-period aggregation: COUNT(*) GROUP BY canonical_artist_id
-- filtered by a played_at_utc window and resolution_status = 'CONFIRMED'.
CREATE INDEX idx_listening_events_chart_aggregation
    ON listening_events (canonical_artist_id, played_at_utc)
    WHERE resolution_status = 'CONFIRMED';

CREATE INDEX idx_connected_accounts_provider_account_id
    ON connected_accounts (provider, provider_account_id);

CREATE INDEX idx_eurovision_artists_country_edition
    ON eurovision_artists (country_id, edition_id);
