-- Application-level audit log.
-- Complements Envers (which tracks entity state changes) by recording business ACTIONS - who did
-- what, when, and whether it succeeded - including attempts that fail. Rows are insert-only; the
-- application never updates or deletes them, giving a tamper-evident compliance trail.

CREATE TABLE audit_log (
    id           BIGSERIAL PRIMARY KEY,
    public_id    UUID          NOT NULL DEFAULT gen_random_uuid(),
    event_time   TIMESTAMPTZ   NOT NULL DEFAULT now(),
    username     VARCHAR(50)   NOT NULL,
    action       VARCHAR(60)   NOT NULL,
    target_type  VARCHAR(60),
    outcome      VARCHAR(10)   NOT NULL,
    detail       VARCHAR(1000)
);

CREATE INDEX ix_audit_log_username ON audit_log (username);
CREATE INDEX ix_audit_log_action ON audit_log (action);
CREATE INDEX ix_audit_log_event_time ON audit_log (event_time);
