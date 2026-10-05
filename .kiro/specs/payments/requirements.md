# Requirements — Payments Module

Backend module for user payments against loan EMIs, credit-card statements, or
untargeted "other" payments, built on the approved, already-applied V2 schema
(`payments`). No schema change. Follows the established module patterns and
integrates with the Loans and Credit Cards modules for settlement.

## Context (from V2 schema)

`payments` columns: `id UUID`, `user_id UUID NOT NULL` → `users` (ON DELETE
RESTRICT), `payment_type TEXT` (CHECK: LOAN_EMI/CREDIT_CARD/OTHER),
`loan_emi_id UUID NULL` → `loan_emis` (ON DELETE RESTRICT),
`card_statement_id UUID NULL` → `card_statements` (ON DELETE RESTRICT),
`payment_method_id UUID NULL` → `payment_methods` (ON DELETE SET NULL),
`amount NUMERIC(15,2)`, `paid_at TIMESTAMPTZ DEFAULT now()`, `created_at`,
`updated_at`.

DB guardrails on `payments`:
- `chk_payments_amount_positive` — amount > 0.
- `chk_payments_type` — LOAN_EMI/CREDIT_CARD/OTHER.
- `chk_payments_target` — the type/target invariant:
  - `LOAN_EMI`  ⇒ `loan_emi_id` NOT NULL AND `card_statement_id` NULL.
  - `CREDIT_CARD` ⇒ `card_statement_id` NOT NULL AND `loan_emi_id` NULL.
  - `OTHER`     ⇒ both NULL.
- `loan_emi_id` / `card_statement_id` are intentionally NON-unique: multiple
  (partial) payments may target the same EMI or statement.

## Endpoints (all under `/api/v1/payments`)

- `GET /` — list the authenticated user's payments (newest `paid_at` first).
  Optional filters: `type`, `from`, `to` (ISO date-time or date on paid_at).
- `GET /{id}` — get an owned payment.
- `POST /` — record a payment (201); settles the target where applicable.
- `DELETE /{id}` — delete an owned payment (204); re-settles the target.

Payments are immutable once recorded except for deletion (no PUT): correcting a
payment is delete + re-create, which keeps settlement sums consistent.

## R1 — Ownership (JWT subject is the only source)
The acting user id comes exclusively from the JWT subject. All reads and
mutations are scoped to `payments.user_id = <jwt subject>`. A payment that
exists but is not owned by the caller is **404 PAYMENT_NOT_FOUND**, never 403.

## R2 — Target shape + ownership validation
The service enforces the `chk_payments_target` shape at the application layer
(clear 400) before persisting, and additionally validates ownership of the
referenced target (which the DB FK cannot do):
- `LOAN_EMI`: `loanEmiId` required (else 400); must reference an EMI whose loan
  is owned by the user, else **404 LOAN_NOT_FOUND**. `cardStatementId` must be
  null (else 400).
- `CREDIT_CARD`: `cardStatementId` required (else 400); must reference a
  statement whose card is owned by the user, else **404
  CREDIT_CARD_NOT_FOUND**. `loanEmiId` must be null (else 400).
- `OTHER`: both `loanEmiId` and `cardStatementId` must be null (else 400).
- `paymentMethodId` optional for any type; when supplied must be owned by the
  user, else **404 PAYMENT_METHOD_NOT_FOUND**.

## R3 — Settlement (partial payments supported)
Recording or deleting a payment recomputes the target's settled state from the
sum of all payments against it:
- LOAN_EMI target: `totalPaid = Σ amount` of payments for that EMI.
  - `totalPaid >= emi.emiAmount` → EMI `status = PAID`, `paidDate = payment date`
    (latest paid_at date); else EMI reverts to `UPCOMING` with `paidDate = null`
    (OVERDUE/DUE are time-based and not auto-set here).
- CREDIT_CARD target: `statement.paidAmount = min(Σ amount, statementAmount)`
  (DB CHECK caps paid_amount at statement_amount); status derived:
  `paidAmount <= 0` → GENERATED, `>= statementAmount` → PAID, else
  PARTIALLY_PAID (reuses the Credit Cards derivation rule).
- OTHER: no settlement side effect.
- A payment that would push an EMI/statement total strictly over the owed
  amount is allowed (overpayment), but the statement's persisted `paid_amount`
  is capped at `statement_amount` to satisfy the DB CHECK; EMI simply becomes
  PAID.

## R4 — Input validation
- `type` required; one of the three enum values.
- `amount` required, > 0, scale ≤ 2.
- `paidAt` optional; defaults to now when omitted.
- `loanEmiId`, `cardStatementId`, `paymentMethodId` optional UUIDs (shape
  enforced by R2).

## R5 — Consistent errors (ApiResponse envelope)
- Validation failure → `400 VALIDATION_ERROR`; shape violation → `400
  BAD_REQUEST`.
- Missing/non-owned payment → `404 PAYMENT_NOT_FOUND`.
- Non-owned EMI target → `404 LOAN_NOT_FOUND`; non-owned statement target →
  `404 CREDIT_CARD_NOT_FOUND`; non-owned payment method → `404
  PAYMENT_METHOD_NOT_FOUND`.
- Unauthenticated → `401 UNAUTHORIZED`. Constraint names never exposed.

## R6 — No schema change / architecture
- The V2 schema, FKs, and CHECKs are unchanged; they remain the structural
  backstop (the target CHECK validates the shape; amount CHECK the sign).
- Thin controller → service (business rules + `@Transactional` + ownership +
  target validation + settlement) → repositories; DTO/mapper boundary;
  `ApiResponse` envelope; module isolated in `com.moneypilot.payment`.
- Settlement updates `loan_emis` / `card_statements` within the same
  transaction as the payment write so the target state stays consistent.
