-- V2__create_core_financial_schema.sql
-- MoneyPilot core financial schema.
--
-- Targets PostgreSQL 16. Forward-only migration building the domain tables on
-- top of the V1 no-op baseline.
--
-- Conventions enforced throughout:
--   * UUID primary keys, defaulted with gen_random_uuid() (pgcrypto).
--   * NUMERIC(15,2) for all monetary amounts (never floating point).
--   * TIMESTAMPTZ for event/audit timestamps (stored in UTC).
--   * DATE for date-only business dates (due dates, statement/billing dates).
--   * Real foreign keys for every relationship (no polymorphic
--     reference_type/reference_id columns anywhere).
--   * CHECK constraints to enforce domain invariants.
--   * UNIQUE constraints and indexes as described in the design docs.
--   * No triggers (settlement/PAID status is computed in the service layer).
--
-- Financial-record deletion policy:
--   Financial tables (loans, loan_emis, credit_cards, card_statements,
--   payments, expenses) use ON DELETE RESTRICT so history is never silently
--   cascade-deleted. Only user-scoped lookup data uses CASCADE where noted.
--   audit_logs.user_id is nullable with ON DELETE SET NULL so audit history
--   survives user deletion.

-- gen_random_uuid() lives in the pgcrypto extension on PostgreSQL.
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- ---------------------------------------------------------------------------
-- USERS
-- ---------------------------------------------------------------------------
CREATE TABLE users (
    id            UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    email         TEXT         NOT NULL,
    display_name  TEXT         NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    currency      CHAR(3)      NOT NULL DEFAULT 'INR',
    timezone      VARCHAR(50)  NOT NULL DEFAULT 'Asia/Kolkata',
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_users_email UNIQUE (email)
);

-- ---------------------------------------------------------------------------
-- CATEGORIES
--   user_id NULL      => system/default category (shared across all users)
--   user_id NOT NULL  => user-owned category
-- ---------------------------------------------------------------------------
CREATE TABLE categories (
    id             UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id        UUID        NULL REFERENCES users (id) ON DELETE CASCADE,
    name           TEXT        NOT NULL,
    category_type  VARCHAR(20) NOT NULL,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_categories_type CHECK (category_type IN ('EXPENSE', 'INCOME'))
);

-- A user cannot have two categories with the same name. System/default
-- categories (user_id IS NULL) are kept unique by name via a partial index.
CREATE UNIQUE INDEX uq_categories_user_name
    ON categories (user_id, name)
    WHERE user_id IS NOT NULL;
CREATE UNIQUE INDEX uq_categories_system_name
    ON categories (name)
    WHERE user_id IS NULL;
CREATE INDEX idx_categories_user_id ON categories (user_id);

-- ---------------------------------------------------------------------------
-- CREDIT_CARDS
-- ---------------------------------------------------------------------------
CREATE TABLE credit_cards (
    id               UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id          UUID          NOT NULL REFERENCES users (id) ON DELETE RESTRICT,
    card_name        TEXT          NOT NULL,
    bank             VARCHAR(100)  NOT NULL,
    last_four        CHAR(4)       NOT NULL,
    credit_limit     NUMERIC(15,2) NOT NULL,
    statement_day    SMALLINT      NOT NULL,
    payment_due_day  SMALLINT      NOT NULL,
    status           VARCHAR(20)   NOT NULL,
    created_at       TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT chk_credit_cards_limit_positive CHECK (credit_limit > 0),
    CONSTRAINT chk_credit_cards_last_four_digits CHECK (last_four ~ '^[0-9]{4}$'),
    CONSTRAINT chk_credit_cards_statement_day CHECK (statement_day BETWEEN 1 AND 31),
    CONSTRAINT chk_credit_cards_payment_due_day CHECK (payment_due_day BETWEEN 1 AND 31),
    CONSTRAINT chk_credit_cards_status CHECK (status IN ('ACTIVE', 'BLOCKED', 'CLOSED'))
);
CREATE INDEX idx_credit_cards_user_id ON credit_cards (user_id);

-- ---------------------------------------------------------------------------
-- PAYMENT_METHODS
--   type = CREDIT_CARD  => credit_card_id IS NOT NULL
--   type <> CREDIT_CARD => credit_card_id IS NULL
-- ---------------------------------------------------------------------------
CREATE TABLE payment_methods (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID        NOT NULL REFERENCES users (id) ON DELETE RESTRICT,
    type            TEXT        NOT NULL,
    label           TEXT        NOT NULL,
    credit_card_id  UUID        NULL REFERENCES credit_cards (id) ON DELETE RESTRICT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_payment_methods_type
        CHECK (type IN ('CASH', 'UPI', 'CREDIT_CARD', 'DEBIT_CARD', 'BANK_TRANSFER', 'OTHER')),
    -- Credit-card linkage invariant:
    CONSTRAINT chk_payment_methods_credit_card_link CHECK (
        (type = 'CREDIT_CARD' AND credit_card_id IS NOT NULL)
        OR
        (type <> 'CREDIT_CARD' AND credit_card_id IS NULL)
    )
);
CREATE INDEX idx_payment_methods_user_id ON payment_methods (user_id);
CREATE INDEX idx_payment_methods_credit_card_id ON payment_methods (credit_card_id);

-- ---------------------------------------------------------------------------
-- LOANS
-- ---------------------------------------------------------------------------
CREATE TABLE loans (
    id                      UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id                 UUID          NOT NULL REFERENCES users (id) ON DELETE RESTRICT,
    name                    TEXT          NOT NULL,
    lender                  VARCHAR(150)  NULL,
    loan_type               VARCHAR(30)   NOT NULL,
    principal_amount        NUMERIC(15,2) NOT NULL,
    outstanding_principal   NUMERIC(15,2) NOT NULL,
    emi_amount              NUMERIC(15,2) NOT NULL,
    interest_rate           NUMERIC(6,3)  NOT NULL,
    start_date              DATE          NOT NULL,
    end_date                DATE          NULL,
    tenure_months           INTEGER       NOT NULL,
    status                  VARCHAR(20)   NOT NULL,
    created_at              TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at              TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT chk_loans_principal_positive CHECK (principal_amount > 0),
    CONSTRAINT chk_loans_outstanding_nonneg CHECK (outstanding_principal >= 0),
    CONSTRAINT chk_loans_emi_positive       CHECK (emi_amount > 0),
    CONSTRAINT chk_loans_interest_nonneg    CHECK (interest_rate >= 0),
    CONSTRAINT chk_loans_tenure_positive    CHECK (tenure_months > 0),
    CONSTRAINT chk_loans_type
        CHECK (loan_type IN ('HOME', 'CAR', 'PERSONAL', 'EDUCATION', 'GOLD', 'OTHER')),
    CONSTRAINT chk_loans_status
        CHECK (status IN ('ACTIVE', 'CLOSED', 'FORECLOSED'))
);
CREATE INDEX idx_loans_user_id ON loans (user_id);

-- ---------------------------------------------------------------------------
-- LOAN_EMIS
-- ---------------------------------------------------------------------------
CREATE TABLE loan_emis (
    id                   UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    loan_id              UUID          NOT NULL REFERENCES loans (id) ON DELETE RESTRICT,
    installment_no       INTEGER       NOT NULL,
    principal_component  NUMERIC(15,2) NOT NULL,
    interest_component   NUMERIC(15,2) NOT NULL,
    emi_amount           NUMERIC(15,2) NOT NULL,
    due_date             DATE          NOT NULL,
    paid_date            DATE          NULL,
    status               VARCHAR(20)   NOT NULL DEFAULT 'UPCOMING',
    created_at           TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at           TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT chk_loan_emis_principal_nonneg  CHECK (principal_component >= 0),
    CONSTRAINT chk_loan_emis_interest_nonneg   CHECK (interest_component >= 0),
    CONSTRAINT chk_loan_emis_emi_positive      CHECK (emi_amount > 0),
    CONSTRAINT chk_loan_emis_installment_positive CHECK (installment_no > 0),
    CONSTRAINT chk_loan_emis_status
        CHECK (status IN ('UPCOMING', 'DUE', 'PAID', 'OVERDUE', 'WAIVED')),
    CONSTRAINT chk_loan_emis_component_sum
        CHECK (principal_component + interest_component = emi_amount),
    CONSTRAINT uq_loan_emis_loan_installment UNIQUE (loan_id, installment_no)
);
CREATE INDEX idx_loan_emis_loan_id ON loan_emis (loan_id);
CREATE INDEX idx_loan_emis_due_date ON loan_emis (due_date);

-- ---------------------------------------------------------------------------
-- CARD_STATEMENTS
-- ---------------------------------------------------------------------------
CREATE TABLE card_statements (
    id                  UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    credit_card_id      UUID          NOT NULL REFERENCES credit_cards (id) ON DELETE RESTRICT,
    statement_date      DATE          NOT NULL,
    due_date            DATE          NOT NULL,
    statement_amount    NUMERIC(15,2) NOT NULL,
    minimum_due         NUMERIC(15,2) NOT NULL,
    paid_amount         NUMERIC(15,2) NOT NULL DEFAULT 0,
    status              VARCHAR(20)   NOT NULL DEFAULT 'GENERATED',
    created_at          TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT chk_card_statements_amount_nonneg CHECK (statement_amount >= 0),
    CONSTRAINT chk_card_statements_min_due_range CHECK (minimum_due >= 0 AND minimum_due <= statement_amount),
    CONSTRAINT chk_card_statements_paid_range CHECK (paid_amount >= 0 AND paid_amount <= statement_amount),
    CONSTRAINT chk_card_statements_status
        CHECK (status IN ('GENERATED', 'PARTIALLY_PAID', 'PAID', 'OVERDUE')),
    CONSTRAINT chk_card_statements_due_date CHECK (due_date >= statement_date),
    CONSTRAINT uq_card_statements_card_date UNIQUE (credit_card_id, statement_date)
);
CREATE INDEX idx_card_statements_credit_card_id ON card_statements (credit_card_id);
CREATE INDEX idx_card_statements_due_date ON card_statements (due_date);

-- ---------------------------------------------------------------------------
-- EXPENSES
-- ---------------------------------------------------------------------------
CREATE TABLE expenses (
    id                  UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id             UUID          NOT NULL REFERENCES users (id) ON DELETE RESTRICT,
    category_id         UUID          NULL REFERENCES categories (id) ON DELETE SET NULL,
    payment_method_id   UUID          NULL REFERENCES payment_methods (id) ON DELETE SET NULL,
    amount              NUMERIC(15,2) NOT NULL,
    expense_date        DATE          NOT NULL,
    description         TEXT          NULL,
    notes               TEXT          NULL,
    created_at          TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT chk_expenses_amount_positive CHECK (amount > 0)
);
CREATE INDEX idx_expenses_user_id ON expenses (user_id);
CREATE INDEX idx_expenses_category_id ON expenses (category_id);
CREATE INDEX idx_expenses_payment_method_id ON expenses (payment_method_id);
CREATE INDEX idx_expenses_expense_date ON expenses (expense_date);

-- ---------------------------------------------------------------------------
-- PAYMENTS
--   payment_type = LOAN_EMI    => loan_emi_id NOT NULL,  card_statement_id NULL
--   payment_type = CREDIT_CARD => loan_emi_id NULL,      card_statement_id NOT NULL
--   payment_type = OTHER       => loan_emi_id NULL,      card_statement_id NULL
--
--   loan_emi_id / card_statement_id are intentionally NOT UNIQUE so multiple
--   (partial) payments can target the same EMI or statement.
-- ---------------------------------------------------------------------------
CREATE TABLE payments (
    id                  UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id             UUID          NOT NULL REFERENCES users (id) ON DELETE RESTRICT,
    payment_type        TEXT          NOT NULL,
    loan_emi_id         UUID          NULL REFERENCES loan_emis (id) ON DELETE RESTRICT,
    card_statement_id   UUID          NULL REFERENCES card_statements (id) ON DELETE RESTRICT,
    payment_method_id   UUID          NULL REFERENCES payment_methods (id) ON DELETE SET NULL,
    amount              NUMERIC(15,2) NOT NULL,
    paid_at             TIMESTAMPTZ   NOT NULL DEFAULT now(),
    created_at          TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT chk_payments_amount_positive CHECK (amount > 0),
    CONSTRAINT chk_payments_type CHECK (payment_type IN ('LOAN_EMI', 'CREDIT_CARD', 'OTHER')),
    -- Payment-type / foreign-key consistency invariant:
    CONSTRAINT chk_payments_target CHECK (
        (payment_type = 'LOAN_EMI'
            AND loan_emi_id IS NOT NULL AND card_statement_id IS NULL)
        OR
        (payment_type = 'CREDIT_CARD'
            AND loan_emi_id IS NULL AND card_statement_id IS NOT NULL)
        OR
        (payment_type = 'OTHER'
            AND loan_emi_id IS NULL AND card_statement_id IS NULL)
    )
);
-- Non-unique indexes support partial-payment lookups and settlement sums.
CREATE INDEX idx_payments_user_id ON payments (user_id);
CREATE INDEX idx_payments_loan_emi_id ON payments (loan_emi_id);
CREATE INDEX idx_payments_card_statement_id ON payments (card_statement_id);
CREATE INDEX idx_payments_payment_method_id ON payments (payment_method_id);

-- ---------------------------------------------------------------------------
-- AUDIT_LOGS
--   user_id is nullable with ON DELETE SET NULL so audit history is preserved
--   even when the originating user is removed. Never cascade-delete audit rows.
-- ---------------------------------------------------------------------------
CREATE TABLE audit_logs (
    id            UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id       UUID        NULL REFERENCES users (id) ON DELETE SET NULL,
    action        TEXT        NOT NULL,
    entity_type   TEXT        NOT NULL,
    entity_id     UUID        NULL,
    details       JSONB       NULL,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_audit_logs_user_id ON audit_logs (user_id);
CREATE INDEX idx_audit_logs_created_at ON audit_logs (created_at);
CREATE INDEX idx_audit_logs_entity ON audit_logs (entity_type, entity_id);
