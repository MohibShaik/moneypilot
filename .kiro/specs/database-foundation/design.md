# Design — Database Foundation (Spec 002)

Documentation-only schema design. No SQL or migrations are created in this spec. The
canonical diagram lives in `docs/ERD.md`; this file records the schema shape and the
business rules the design must enforce.

## Entity relationship diagram

See `docs/ERD.md` for the Mermaid ER diagram. It now includes the
`CREDIT_CARDS ||--o{ PAYMENT_METHODS` relationship introduced by this correction.

## Schema notes by entity

### USERS
Root tenant. Owns loans, credit cards, expenses, payment methods, payments, audit logs,
and user-owned categories.

### CATEGORIES
- `id UUID PK`
- `user_id UUID NULL` — NULL = system/default category; NOT NULL = user-owned.
- `user_id` is intentionally optional.

### PAYMENT_METHODS (corrected)
- `id UUID PK`
- `user_id UUID NOT NULL` — owner.
- `type` — enumerated; includes `CREDIT_CARD`.
- `credit_card_id UUID NULL` → `CREDIT_CARDS.id`.
  - Populated when `type = CREDIT_CARD`; NULL otherwise.

### CREDIT_CARDS
- `id UUID PK`
- `user_id UUID NOT NULL` — owner.
- Generates many `CARD_STATEMENTS`.

### CARD_STATEMENTS
- One per billing cycle; captures statement amount and due date.

### LOANS / LOAN_EMIS
- A loan contains a schedule of EMIs; each EMI has a due amount and due date.

### EXPENSES
- References `CATEGORIES` (categorization) and `PAYMENT_METHODS` (how it was paid).
- Resolves the exact credit card via `PAYMENT_METHODS.credit_card_id` — not stored on
  `EXPENSES`.

### PAYMENTS (corrected)
- `id UUID PK`
- `user_id UUID NOT NULL` — payer.
- `payment_type` — `LOAN_EMI | CREDIT_CARD | OTHER`.
- `loan_emi_id UUID NULL` → `LOAN_EMIS.id`.
- `card_statement_id UUID NULL` → `CARD_STATEMENTS.id`.
- `payment_method_id UUID` → `PAYMENT_METHODS.id`.
- Neither `loan_emi_id` nor `card_statement_id` is unique (partial payments allowed).

### AUDIT_LOGS
- Owned by a user; append-only activity record.

## Business rules

1. **Credit-card payment method linkage** — if `PAYMENT_METHODS.type = CREDIT_CARD`,
   then `credit_card_id` must be populated; for all other types it must be NULL.
2. **Category ownership** — `categories.user_id IS NULL` denotes a system/default
   category; `IS NOT NULL` denotes a user-owned category. `user_id` stays optional.
3. **Partial payments (loan EMI)** — multiple payments may apply to one EMI. The EMI
   becomes `PAID` when the total of valid payments equals the EMI amount
   (e.g. ₹10,000 + ₹15,000 = ₹25,000).
4. **Partial payments (card statement)** — multiple payments may apply to one
   statement. The statement becomes `PAID` when the total of valid payments equals the
   statement amount (e.g. ₹20,000 + ₹30,000 = ₹50,000).
5. **Payment type consistency** —
   - `LOAN_EMI`: `loan_emi_id` populated, `card_statement_id` NULL.
   - `CREDIT_CARD`: `card_statement_id` populated, `loan_emi_id` NULL.
   - `OTHER`: both NULL.
6. **Explicit foreign keys over polymorphic references** — `PAYMENTS` uses
   `loan_emi_id` and `card_statement_id` rather than `reference_type`/`reference_id`,
   preserving referential integrity, query clarity, type safety, and schema evolution.

## Rationale: explicit FKs vs. polymorphic references
Explicit, type-specific foreign keys allow database-enforced referential integrity,
direct indexable joins, strongly typed ORM mappings, and independent evolution of each
payable target — none of which a polymorphic `reference_type`/`reference_id` pair can
provide.

## Index strategy

Every foreign key gets a supporting index (PostgreSQL does not auto-index FK columns),
plus indexes on columns used for range/filter queries:

- `users`: unique `email`.
- `categories`: `idx_categories_user_id`; partial unique `uq_categories_user_name`
  (user-owned) and `uq_categories_system_name` (system defaults).
- `credit_cards`: `idx_credit_cards_user_id`.
- `payment_methods`: `idx_payment_methods_user_id`, `idx_payment_methods_credit_card_id`.
- `loans`: `idx_loans_user_id`.
- `loan_emis`: `idx_loan_emis_loan_id`, `idx_loan_emis_due_date`.
- `card_statements`: `idx_card_statements_credit_card_id`, `idx_card_statements_due_date`.
- `expenses`: `idx_expenses_user_id`, `idx_expenses_category_id`,
  `idx_expenses_payment_method_id`, `idx_expenses_expense_date`.
- `payments`: `idx_payments_user_id`, `idx_payments_loan_emi_id`,
  `idx_payments_card_statement_id`, `idx_payments_payment_method_id`
  (all non-unique — multiple payments per EMI/statement).
- `audit_logs`: `idx_audit_logs_user_id`, `idx_audit_logs_created_at`,
  `idx_audit_logs_entity (entity_type, entity_id)`.

## Constraint strategy

- **Primary keys** — UUID on every table.
- **Foreign keys** — real FKs with explicit `ON DELETE` behavior:
  `CASCADE` for ownership chains (user → resources, loan → EMIs, card → statements),
  `RESTRICT` for references that must not be silently orphaned (`payments.loan_emi_id`,
  `payments.card_statement_id`, `payment_methods.credit_card_id`), and `SET NULL` for
  soft references on expenses/payments (`category_id`, `payment_method_id`).
- **UNIQUE** — `users.email`; `(loan_id, installment_no)` on `loan_emis`;
  `(credit_card_id, statement_date)` on `card_statements`; partial unique category names.
  `payments.loan_emi_id` and `payments.card_statement_id` are deliberately **not** unique.
- **CHECK** — positive/non-negative amounts; enumerated `type`/`status`/`payment_type`
  values; `minimum_due <= statement_amount`; the credit-card linkage invariant on
  `payment_methods`; and the payment-type/FK consistency invariant on `payments`.
- **No triggers** — PAID/PARTIAL settlement is derived in the service layer from the sum
  of valid payments; the DB stores status but does not compute it via triggers.

## Ownership validation notes (application/service layer)

UUIDs are unguessable but are **not** an authorization mechanism. The service layer must
verify that every referenced financial resource belongs to the authenticated user before
reading or writing. Walk the ownership chain on each operation:

- Payment → EMI → Loan → User: a `LOAN_EMI` payment must confirm
  `loan_emis → loans.user_id` equals the caller.
- Payment → Card Statement → Credit Card → User: a `CREDIT_CARD` payment must confirm
  `card_statements → credit_cards.user_id` equals the caller.
- Payment Method → Credit Card → User: when a method has `type = CREDIT_CARD`, confirm
  `credit_cards.user_id` equals the method owner (and the caller).
- Expense → Category / Payment Method: confirm referenced category (if user-owned) and
  payment method belong to the caller; system/default categories (`user_id IS NULL`) are
  shared and readable by all.

Never authorize solely because a UUID was supplied. The database enforces integrity and
shape; the service layer enforces tenancy.

## V2 corrections (approved field set)

The following corrections restore approved fields that an earlier draft simplified.

### USERS
- `password_hash VARCHAR(255) NOT NULL`
- `currency CHAR(3) NOT NULL DEFAULT 'INR'`
- `timezone VARCHAR(50) NOT NULL DEFAULT 'Asia/Kolkata'`

### CATEGORIES
- `category_type VARCHAR(20) NOT NULL` with CHECK `IN ('EXPENSE','INCOME')`.

### CREDIT_CARDS
- `bank VARCHAR(100) NOT NULL`
- `statement_day SMALLINT NOT NULL` (CHECK 1–31)
- `payment_due_day SMALLINT NOT NULL` (CHECK 1–31)
- `status VARCHAR(20) NOT NULL` with CHECK `IN ('ACTIVE','BLOCKED','CLOSED')`.

### LOANS
- `lender VARCHAR(150)` (nullable)
- `loan_type VARCHAR(30) NOT NULL` with CHECK `IN ('HOME','CAR','PERSONAL','EDUCATION','GOLD','OTHER')`
- `emi_amount NUMERIC(15,2) NOT NULL`
- `end_date DATE` (nullable)
- `outstanding_principal NUMERIC(15,2) NOT NULL`
- `status VARCHAR(20) NOT NULL` with CHECK `IN ('ACTIVE','CLOSED','FORECLOSED')`.

### LOAN_EMIS
- Replaced the single `amount` with `principal_component`, `interest_component`, and
  `emi_amount` (all `NUMERIC(15,2) NOT NULL`), plus `paid_date DATE` (nullable).
- Status CHECK: `IN ('UPCOMING','DUE','PAID','OVERDUE','WAIVED')`.
- Unique `(loan_id, installment_no)` preserved.

### CARD_STATEMENTS
- `paid_amount NUMERIC(15,2) NOT NULL DEFAULT 0` with CHECK
  `paid_amount >= 0 AND paid_amount <= statement_amount`.
- Status CHECK: `IN ('GENERATED','PARTIALLY_PAID','PAID','OVERDUE')`.

### EXPENSES
- `notes TEXT` (nullable).

### AUDIT_LOGS
- `user_id` is now nullable with `ON DELETE SET NULL`; audit history is never
  cascade-deleted when a user is removed.

## Financial-record deletion policy

Financial records must not be silently cascade-deleted. The following tables use
`ON DELETE RESTRICT` on their inbound ownership FKs (including `user_id`):
`loans`, `loan_emis`, `credit_cards`, `card_statements`, `payments`, `expenses`.
`payment_methods.user_id` is also `RESTRICT`. Only non-financial lookup data
(`categories.user_id`) retains `CASCADE`. Soft references (`expenses.category_id`,
`expenses.payment_method_id`, `payments.payment_method_id`) use `SET NULL`.
`audit_logs.user_id` uses `SET NULL` to preserve history.

## Ownership validation (service layer)

Database constraints enforce shape and integrity; the application/service layer enforces
tenancy. On every operation it must verify:

- **payment user == EMI/loan owner** — a `LOAN_EMI` payment's user matches
  `loan_emis → loans.user_id`.
- **payment user == card/statement owner** — a `CREDIT_CARD` payment's user matches
  `card_statements → credit_cards.user_id`.
- **payment method user == credit card owner** — a `CREDIT_CARD`-type payment method's
  user matches `credit_cards.user_id`.

Never authorize solely because a UUID was supplied.
