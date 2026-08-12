-- Hibernate Envers audit schema.
-- Envers records a full versioned history of audited entities: every insert/update/delete produces
-- a row in the corresponding *_aud table, tied to a revision in revinfo. These tables are written
-- by the app at runtime, so they must exist up front (Hibernate runs in validate mode and never
-- creates them). History rows are append-only and never modified, giving an immutable audit trail.

-- Revision metadata. One row per transaction that touched an audited entity.
CREATE SEQUENCE revinfo_seq INCREMENT BY 1 START WITH 1;

CREATE TABLE revinfo (
    rev       INTEGER PRIMARY KEY,
    revtstmp  BIGINT,
    username  VARCHAR(50)   -- captured from the security context, or 'system' for background jobs
);

-- Versioned history of accounts. revtype: 0=ADD, 1=MOD, 2=DEL.
CREATE TABLE accounts_aud (
    id              BIGINT       NOT NULL,
    rev             INTEGER      NOT NULL REFERENCES revinfo (rev),
    revtype         SMALLINT,
    public_id       UUID,
    owner_id        BIGINT,
    account_number  VARCHAR(20),
    account_type    VARCHAR(20),
    currency        VARCHAR(3),
    balance         NUMERIC(19,2),
    status          VARCHAR(20),
    pan_encrypted   VARCHAR(512),
    created_at      TIMESTAMPTZ,
    updated_at      TIMESTAMPTZ,
    version         BIGINT,
    PRIMARY KEY (id, rev)
);

-- Versioned history of transactions (the ledger itself is already append-only; this captures the
-- revision/actor metadata for completeness and cross-entity audit queries).
CREATE TABLE transactions_aud (
    id                  BIGINT       NOT NULL,
    rev                 INTEGER      NOT NULL REFERENCES revinfo (rev),
    revtype             SMALLINT,
    public_id           UUID,
    reference           VARCHAR(40),
    type                VARCHAR(20),
    status              VARCHAR(20),
    source_account_id   BIGINT,
    target_account_id   BIGINT,
    amount              NUMERIC(19,2),
    currency            VARCHAR(3),
    description         VARCHAR(255),
    created_at          TIMESTAMPTZ,
    PRIMARY KEY (id, rev)
);
