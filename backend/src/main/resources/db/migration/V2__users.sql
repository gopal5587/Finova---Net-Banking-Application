-- User accounts for authentication and authorization.
-- Passwords are stored only as BCrypt hashes; plaintext never touches the DB.

CREATE TABLE users (
    id              BIGSERIAL PRIMARY KEY,
    public_id       UUID        NOT NULL DEFAULT gen_random_uuid(),
    username        VARCHAR(50) NOT NULL,
    email           VARCHAR(255) NOT NULL,
    password_hash   VARCHAR(100) NOT NULL,
    full_name       VARCHAR(150) NOT NULL,
    role            VARCHAR(20) NOT NULL DEFAULT 'USER',
    enabled         BOOLEAN     NOT NULL DEFAULT TRUE,
    -- 2FA columns are provisioned now (Phase 9 populates them) to avoid a later migration on a live table.
    mfa_enabled     BOOLEAN     NOT NULL DEFAULT FALSE,
    mfa_secret      VARCHAR(255),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    version         BIGINT      NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX ux_users_username ON users (LOWER(username));
CREATE UNIQUE INDEX ux_users_email    ON users (LOWER(email));
CREATE UNIQUE INDEX ux_users_public_id ON users (public_id);
