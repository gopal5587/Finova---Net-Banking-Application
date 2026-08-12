-- Bank accounts owned by users.
-- Balance uses NUMERIC(19,2): exact decimal arithmetic, no floating-point rounding drift.
-- The PAN (tax id) is a sensitive KYC field and is stored encrypted (AES-256-GCM) via the
-- application layer, hence a wide VARCHAR to hold Base64 ciphertext rather than the raw value.

CREATE TABLE accounts (
    id              BIGSERIAL PRIMARY KEY,
    public_id       UUID          NOT NULL DEFAULT gen_random_uuid(),
    owner_id        BIGINT        NOT NULL REFERENCES users (id),
    account_number  VARCHAR(20)   NOT NULL,
    account_type    VARCHAR(20)   NOT NULL DEFAULT 'SAVINGS',
    currency        VARCHAR(3)    NOT NULL DEFAULT 'INR',
    balance         NUMERIC(19,2) NOT NULL DEFAULT 0 CHECK (balance >= 0),
    status          VARCHAR(20)   NOT NULL DEFAULT 'ACTIVE',
    pan_encrypted   VARCHAR(512),
    created_at      TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ   NOT NULL DEFAULT now(),
    version         BIGINT        NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX ux_accounts_account_number ON accounts (account_number);
CREATE UNIQUE INDEX ux_accounts_public_id ON accounts (public_id);
CREATE INDEX ix_accounts_owner ON accounts (owner_id);
