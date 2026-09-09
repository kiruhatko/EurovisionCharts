CREATE TABLE deployment_binding_metadata (
    id                      BIGSERIAL PRIMARY KEY,
    deployment_id           VARCHAR(255) NOT NULL,
    authorized_chat_id      BIGINT,
    environment             VARCHAR(64) NOT NULL,
    application_identifier  VARCHAR(255) NOT NULL,
    binding_version         VARCHAR(32) NOT NULL,
    verified_at             TIMESTAMPTZ NOT NULL DEFAULT now(),
    casual_mode             BOOLEAN NOT NULL DEFAULT FALSE
);
