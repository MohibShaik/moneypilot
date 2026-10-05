# Requirements — Loans + Loan EMIs Module

Backend module for user loans and their EMI (installment) schedules, built on
the approved, already-applied V2 schema (`loans`, `loan_emis`). No schema
change. Follows the established Auth/User, Categories, and Payment Methods
module patterns.

## Context (from V2 schema)

`loans` columns: `id UUID`, `user_id UUID NOT NULL` → `users` (ON DELETE
RESTRICT), `name TEXT NOT NULL`, `lender VARCHAR(150) NULL`,
`loan_type VARCHAR(30)` (CHECK: HOME/CAR/PERSONAL/EDUCATION/GOLD/OTHER),
`principal_amount NUMERIC(15,2)`, `outstanding_principal NUMERIC(15,2)`,
`emi_amount NUMERIC(15,2)`, `interest_rate NUMERIC(6,3)`, `start_date DATE`,
`end_date DATE NULL`, `tenure_months INTEGER`,
`status VARCHAR(20)` (CHECK: ACTIVE/CLOSED/FORECLOSED), `created_at`,
`updated_at`.

DB guardrails on `loans`:
- `chk_loans_principal_positive` — principal_amount > 0.
- `chk_loans_outstanding_nonneg` — outstanding_principal >= 0.
- `chk_loans_emi_positive` — emi_amount > 0.
- `chk_loans_interest_nonneg` — interest_rate >= 0.
- `chk_loans_tenure_positive` — tenure_months > 0.
- `chk_loans_type`, `chk_loans_status` — the enum value sets.

`loan_emis` columns: `id UUID`, `loan_id UUID NOT NULL` → `loans` (ON DELETE
RESTRICT), `installment_no INTEGER`, `principal_component NUMERIC(15,2)`,
`interest_component NUMERIC(15,2)`, `emi_amount NUMERIC(15,2)`,
`due_date DATE`, `paid_date DATE NULL`,
`status VARCHAR(20) DEFAULT 'UPCOMING'` (CHECK: UPCOMING/DUE/PAID/OVERDUE/WAIVED),
`created_at`, `updated_at`.

DB guardrails on `loan_emis`:
- `chk_loan_emis_principal_nonneg`, `chk_loan_emis_interest_nonneg`,
  `chk_loan_emis_emi_positive`, `chk_loan_emis_installment_positive`.
- `chk_loan_emis_component_sum` — `principal_component + interest_component =
  emi_amount` **exactly** at NUMERIC(15,2). The schedule generator must respect
  this at 2-decimal precision for every row.
- `uq_loan_emis_loan_installment` — UNIQUE `(loan_id, installment_no)`.

## Endpoints (all under `/api/v1/loans`)

- `GET /` — list the authenticated user's loans (summary; no EMI rows).
- `GET /{id}` — get an owned loan with its full EMI schedule.
- `POST /` — create a loan; the EMI schedule is generated server-side (201).
- `PUT /{id}` — update mutable fields of an owned loan.
- `DELETE /{id}` — delete an owned loan and its EMIs (204).

## R1 — Ownership (JWT subject is the only source)
The acting user id comes exclusively from the JWT subject. All reads and
mutations are scoped to `loans.user_id = <jwt subject>`. A loan that exists but
is not owned by the caller is reported as **404 LOAN_NOT_FOUND**, never 403, so
another user's id existence is not leaked (same rule as Categories / Payment
Methods). EMIs are reached only through their owned parent loan.

## R2 — Loan creation and EMI schedule generation
On `POST`, the client supplies the loan terms (name, optional lender, type,
principal, interest rate, start date, tenure in months, optional emiAmount). The
server:

- Computes `emiAmount` with the standard reducing-balance formula when the
  client does not supply one; if the client supplies `emiAmount` it is used
  as-is (must be > 0).
- Sets `outstandingPrincipal = principalAmount` at creation.
- Sets `status = ACTIVE`.
- Computes `endDate = startDate + tenureMonths` (first due date is one month
  after start; last due date equals endDate).
- Generates `tenureMonths` EMI rows (installment_no 1..N), each with
  `principal_component + interest_component = emi_amount` **exactly**. Rounding
  residuals are absorbed into the final installment so every row satisfies the
  DB sum constraint and the principal components sum to the loan principal.
- All EMI rows start `status = UPCOMING`, `paid_date = NULL`.

Zero-interest loans are supported (interest_rate = 0 → every interest_component
is 0 and emi = principal/tenure with residual on the last row).

## R3 — Loan update
`PUT` updates only descriptive/mutable fields: `name`, `lender`, `status`.
Financial terms (principal, interest, tenure, start date) and the generated EMI
schedule are **immutable** in V1 — changing them would require regenerating the
schedule and reconciling payments, which is out of scope here. Supplying new
financial terms is ignored (not part of the update contract). `status` may be
set to ACTIVE/CLOSED/FORECLOSED.

## R4 — Input validation
- `name` required, 1–150 chars.
- `lender` optional, max 150 chars.
- `loanType` required; one of the six enum values.
- `principalAmount` required, > 0, scale ≤ 2.
- `interestRate` required, >= 0, scale ≤ 3.
- `startDate` required (ISO date).
- `tenureMonths` required, 1..600.
- `emiAmount` optional, > 0 when present.

## R5 — Consistent errors (ApiResponse envelope)
- Validation failure → `400 VALIDATION_ERROR`.
- Missing/non-owned loan → `404 LOAN_NOT_FOUND`.
- Unauthenticated → `401 UNAUTHORIZED` (existing security chain).
- Other data-integrity → `409 DATA_INTEGRITY_VIOLATION`. Constraint names are
  never exposed.

## R6 — No schema change / architecture
- The V2 schema, FKs, and CHECKs are unchanged; they remain the structural
  backstop (the component-sum CHECK in particular validates the generator).
- Thin controller → service (business rules + `@Transactional` + ownership +
  schedule generation) → repositories; DTO/mapper boundary; `ApiResponse`
  envelope; module isolated in `com.moneypilot.loan`.
- Deletion: `loan_emis.loan_id` is ON DELETE RESTRICT, so deleting a loan
  deletes its EMIs first (service-managed), then the loan.
