-- Monthly account statements produced by the scheduler.
-- Each row is a point-in-time summary for one account over a calendar month.
-- Kept separate from the live ledger so regenerating or inspecting statements never
-- rewrites financial history.

CREATE TABLE account_statements (
    id                BIGSERIAL PRIMARY KEY,
    public_id         UUID          NOT NULL DEFAULT gen_random_uuid(),
    account_id        BIGINT        NOT NULL REFERENCES accounts (id),
    period_year       INTEGER       NOT NULL,
    period_month      INTEGER       NOT NULL CHECK (period_month BETWEEN 1 AND 12),
    opening_balance   NUMERIC(19,2) NOT NULL,
    closing_balance   NUMERIC(19,2) NOT NULL,
    total_credits     NUMERIC(19,2) NOT NULL DEFAULT 0,
    total_debits      NUMERIC(19,2) NOT NULL DEFAULT 0,
    transaction_count INTEGER       NOT NULL DEFAULT 0,
    generated_at      TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT ux_statement_account_period UNIQUE (account_id, period_year, period_month)
);

CREATE INDEX ix_account_statements_account ON account_statements (account_id);
