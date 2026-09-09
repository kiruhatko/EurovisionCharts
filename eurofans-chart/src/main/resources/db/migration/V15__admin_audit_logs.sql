CREATE TABLE admin_audit_logs (
    id                  BIGSERIAL PRIMARY KEY,
    admin_telegram_id   BIGINT NOT NULL,
    action              VARCHAR(64) NOT NULL,
    target_type         VARCHAR(64),
    target_id           VARCHAR(255),
    detail              TEXT,
    occurred_at         TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_admin_audit_logs_admin_id ON admin_audit_logs (admin_telegram_id);
CREATE INDEX idx_admin_audit_logs_occurred_at ON admin_audit_logs (occurred_at);
