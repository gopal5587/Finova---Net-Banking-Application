-- Finova baseline migration.
-- Establishes Flyway version control for the schema. Domain tables (users, accounts,
-- transactions, audit) are introduced in later, feature-specific migrations so each
-- change is reviewable and reversible in isolation.

-- pgcrypto provides gen_random_uuid() and cryptographic helpers used by later features.
CREATE EXTENSION IF NOT EXISTS pgcrypto;
