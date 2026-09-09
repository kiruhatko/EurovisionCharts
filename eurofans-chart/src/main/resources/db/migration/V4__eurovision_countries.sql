CREATE TABLE eurovision_countries (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(255) NOT NULL,
    iso_code    VARCHAR(8) NOT NULL,
    CONSTRAINT uq_eurovision_countries_iso_code UNIQUE (iso_code)
);
