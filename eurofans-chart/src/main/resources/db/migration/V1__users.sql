CREATE TABLE users (
    id                          BIGSERIAL PRIMARY KEY,
    telegram_id                 BIGINT NOT NULL,
    telegram_username           VARCHAR(255),
    first_name                  VARCHAR(255),
    tracking_enabled            BOOLEAN NOT NULL DEFAULT TRUE,
    chart_participation_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    active                       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at                  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at                  TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_users_telegram_id UNIQUE (telegram_id)
);
