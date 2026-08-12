-- Financial transactions ledger.
-- Every money movement (deposit, withdrawal, transfer) is recorded as one immutable row.
-- source_account_id is null for deposits; target_account_id is null for withdrawals.
-- Amount is NUMERIC(19,2) to preserve exact monetary precision.

CREATE TABLE transactions (
    id                  BIGSERIAL PRIMARY KEY,
    public_id           UUID          NOT NULL DEFAULT gen_random_uuid(),
    reference           VARCHAR(40)   NOT NULL,
    type                VARCHAR(20)   NOT NULL,
    status              VARCHAR(20)   NOT NULL DEFAULT 'COMPLETED',
    source_account_id   BIGINT        REFERENCES accounts (id),
    target_account_id   BIGINT        REFERENCES accounts (id),
    amount              NUMERIC(19,2) NOT NULL CHECK (amount > 0),
    currency            VARCHAR(3)    NOT NULL DEFAULT 'INR',
    description         VARCHAR(255),
    created_at          TIMESTAMPTZ   NOT NULL DEFAULT now(),
    -- At least one side must be present; enforced so orphan ledger rows cannot exist.
    CONSTRAINT chk_tx_has_account CHECK (source_account_id IS NOT NULL OR target_account_id IS NOT NULL)
);

CREATE UNIQUE INDEX ux_transactions_reference ON transactions (reference);
CREATE UNIQUE INDEX ux_transactions_public_id ON transactions (public_id);
CREATE INDEX ix_transactions_source ON transactions (source_account_id);
CREATE INDEX ix_transactions_target ON transactions (target_account_id);
CREATE INDEX ix_transactions_created_at ON transactions (created_at);
