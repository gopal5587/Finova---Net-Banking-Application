-- Immutable application-level audit trail.
-- Rows are append-only: no UPDATE/DELETE from the application; compliance queries
-- rely on created_at + actor + action being trustworthy.

CREATE TABLE audit_log (
    id              BIGSERIAL PRIMARY KEY,
    public_id       UUID          NOT NULL DEFAULT gen_random_uuid(),
    actor           VARCHAR(100),
    action          VARCHAR(80)   NOT NULL,
    resource_type   VARCHAR(80),
    resource_id     VARCHAR(100),
    outcome         VARCHAR(20)   NOT NULL,
    details         TEXT,
    ip_address      VARCHAR(64),
    created_at      TIMESTAMPTZ   NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX ux_audit_log_public_id ON audit_log (public_id);
CREATE INDEX ix_audit_log_actor ON audit_log (actor);
CREATE INDEX ix_audit_log_action ON audit_log (action);
CREATE INDEX ix_audit_log_created_at ON audit_log (created_at);
