# Implementation Plan — Database Foundation (Spec 002)

Design is approved. This is the forward-only implementation plan for the core
financial schema. The V2 migration SQL has been authored and is ready for review; it is
**not executed yet**, and no JPA entities, controllers, or services are created or
modified at this stage.

## Platform baseline
- PostgreSQL 16.
- UUID primary keys defaulted via `gen_random_uuid()` (pgcrypto extension).
- `NUMERIC(15,2)` for all monetary values; never floating point.
- `TIMESTAMPTZ` for event/audit timestamps (UTC); `DATE` for date-only business dates.
- Real foreign keys for every relationship; no polymorphic `reference_type`/`reference_id`.
- CHECK constraints for domain invariants; UNIQUE constraints and indexes per strategy below.
- No database triggers — settlement/`PAID` status is computed in the service layer.

## Tasks

- [ ] 1. Create V2 migration `V2__create_core_financial_schema.sql`
  (authored; pending review and execution).
  - All ten entities: users, categories, credit_cards, payment_methods, loans,
    loan_emis, card_statements, expenses, payments, audit_logs.
  - _Requirements: R1–R10_

- [ ] 2. PAYMENT_METHODS credit-card linkage CHECK.
  - `type = CREDIT_CARD` ⇒ `credit_card_id IS NOT NULL`;
    `type <> CREDIT_CARD` ⇒ `credit_card_id IS NULL`.
  - _Requirements: R4_

- [ ] 3. CATEGORIES nullable `user_id` with partial unique indexes.
  - NULL = system/default, NOT NULL = user-owned. Not mandatory.
  - _Requirements: R5_

- [ ] 4. PAYMENTS payment-type/FK consistency CHECK.
  - `LOAN_EMI`/`CREDIT_CARD`/`OTHER` with the exact NULL/NOT-NULL rules.
  - _Requirements: R9_

- [ ] 5. Partial-payment support.
  - `loan_emi_id` and `card_statement_id` non-unique; non-unique indexes on both.
  - _Requirements: R8_

- [ ] 6. Index + constraint strategy applied (see design.md).
  - _Requirements: R6–R9_

- [ ] 7. (Deferred) Review migration, then run Flyway. Do not execute yet.

- [ ] 8. (Deferred) JPA entities, repositories, services, controllers — later specs.
