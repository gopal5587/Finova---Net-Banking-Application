-- Fraud detection flags.
-- Each row is a suspicion raised by a detection rule against a transaction/account. Flags are
-- advisory (they never block a transfer automatically) and are reviewed by admins, who can resolve
-- them. Kept separate from the immutable ledger so investigations don't touch financial records.

CREATE TABLE fraud_flags (
    id              BIGSERIAL PRIMARY KEY,
    public_id       UUID          NOT NULL DEFAULT gen_random_uuid(),
    account_id      BIGINT        NOT NULL REFERENCES accounts (id),
    transaction_id  BIGINT        REFERENCES transactions (id),
    reason          VARCHAR(30)   NOT NULL,
    severity        VARCHAR(10)   NOT NULL DEFAULT 'MEDIUM',
    details         VARCHAR(500),
    resolved        BOOLEAN       NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMPTZ   NOT NULL DEFAULT now()
);

CREATE INDEX ix_fraud_flags_account ON fraud_flags (account_id);
CREATE INDEX ix_fraud_flags_resolved ON fraud_flags (resolved);
CREATE INDEX ix_fraud_flags_created_at ON fraud_flags (created_at);
