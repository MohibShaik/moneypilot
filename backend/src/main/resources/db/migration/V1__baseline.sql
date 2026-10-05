-- V1__baseline.sql
-- MoneyPilot schema baseline.
--
-- This is the initial Flyway migration establishing the migration history for
-- the MoneyPilot database. The foundation intentionally creates NO business or
-- domain tables yet (no loans, EMIs, credit cards, expenses, payments,
-- categories, users, notifications, etc.). All future schema changes will be
-- introduced as additional, forward-only Flyway migrations (V2__..., V3__...).
--
-- Monetary columns in future migrations MUST use NUMERIC(15,2); never a
-- floating-point type.
--
-- No-op baseline: this statement does not alter the schema. It only gives
-- Flyway a first versioned migration to record so the migration history starts
-- cleanly on an empty database.
DO $$
BEGIN
    RAISE NOTICE 'MoneyPilot Flyway baseline (V1) applied. No domain tables created.';
END $$;
