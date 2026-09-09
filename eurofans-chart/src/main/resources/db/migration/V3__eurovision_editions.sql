CREATE TABLE eurovision_editions (
    id          BIGSERIAL PRIMARY KEY,
    year        INTEGER NOT NULL,
    name        VARCHAR(255) NOT NULL,
    active      BOOLEAN NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_eurovision_editions_year UNIQUE (year)
);
