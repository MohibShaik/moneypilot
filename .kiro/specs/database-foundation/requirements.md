# Requirements — Database Foundation (Spec 002)

This document captures the data-model requirements for MoneyPilot's financial domain.
It is design/documentation only — no SQL, schema objects, or migrations are produced by
this spec yet.

## Entities in scope

USERS, CATEGORIES, PAYMENT_METHODS, LOANS, LOAN_EMIS, CREDIT_CARDS, CARD_STATEMENTS,
EXPENSES, PAYMENTS, AUDIT_LOGS.

## R1 — User ownership
Every primary resource (loans, credit cards, expenses, payment methods, payments, audit
logs, and user-owned categories) is owned by a `USERS` row through a one-to-many
relationship. Ownership scopes data per user and backs authorization checks.

## R2 — Loan and EMI structure
A loan is broken into a schedule of EMIs. One `LOANS` row contains many `LOAN_EMIS`
rows; each EMI captures a due amount and due date.

## R3 — Credit card and statements
One `CREDIT_CARDS` row generates many `CARD_STATEMENTS` rows, one per billing cycle.

## R4 — Credit-card payment methods (corrected)
`PAYMENT_METHODS` must identify the specific credit card when its type is `CREDIT_CARD`.

- `PAYMENT_METHODS.credit_card_id UUID NULL` references `CREDIT_CARDS.id`.
- If `type = CREDIT_CARD`, `credit_card_id` must be populated.
- For non-credit-card types, `credit_card_id` must be `NULL`.

This lets an expense resolve the exact credit card used via its payment method, without
duplicating `credit_card_id` on `EXPENSES`.

## R5 — Categories ownership (corrected)
`categories.user_id` is nullable and encodes ownership:

- `user_id IS NULL` → system/default category (shared).
- `user_id IS NOT NULL` → user-owned category.

`user_id` must remain optional (not mandatory).

## R6 — Expense categorization
One `CATEGORIES` row categorizes many `EXPENSES` rows. Each expense references the
`PAYMENT_METHODS` used.

## R7 — Payments with explicit foreign keys
`PAYMENTS` references its obligation through explicit nullable foreign keys rather than a
polymorphic `reference_type` / `reference_id` pair:

- `PAYMENTS.loan_emi_id` → `LOAN_EMIS.id`
- `PAYMENTS.card_statement_id` → `CARD_STATEMENTS.id`

## R8 — Partial payments (corrected)
Multiple payments may be applied to the same loan EMI or the same card statement.
Neither `loan_emi_id` nor `card_statement_id` is unique in `PAYMENTS`. An obligation is
`PAID` when the sum of its valid payments equals its amount.

## R9 — Payment types (corrected)
`PAYMENTS.payment_type` supports `LOAN_EMI`, `CREDIT_CARD`, and `OTHER`.

- `LOAN_EMI` → `loan_emi_id` populated, `card_statement_id` NULL.
- `CREDIT_CARD` → `card_statement_id` populated, `loan_emi_id` NULL.
- `OTHER` → both `loan_emi_id` and `card_statement_id` NULL.

## R10 — Audit logging
One `USERS` row generates many `AUDIT_LOGS` rows.
