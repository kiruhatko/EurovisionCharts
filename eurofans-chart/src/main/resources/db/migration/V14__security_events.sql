CREATE TABLE security_events (
    id              BIGSERIAL PRIMARY KEY,
    event_type      VARCHAR(64) NOT NULL,
    telegram_user_id BIGINT,
    chat_id         BIGINT,
    detail          TEXT,
    occurred_at     TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_security_events_type_occurred_at ON security_events (event_type, occurred_at);
