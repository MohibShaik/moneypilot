# Requirements — Credit Cards + Card Statements Module

Backend module for user credit cards and their statements, built on the
approved, already-applied V2 schema (`credit_cards`, `card_statements`). No
schema change. Follows the established Auth/User, Categories, Payment Methods,
and Loans module patterns.

## Context (from V2 schema)

`credit_cards` columns: `id UUID`, `user_id UUID NOT NULL` → `users` (ON DELETE
RESTRICT), `card_name TEXT NOT NULL`, `bank VARCHAR(100) NOT NULL`,
`last_four CHAR(4) NOT NULL`, `credit_limit NUMERIC(15,2) NOT NULL`,
`statement_day SMALLINT NOT NULL`, `payment_due_day SMALLINT NOT NULL`,
`status VARCHAR(20) NOT NULL`, `created_at`, `updated_at`.

DB guardrails on `credit_cards`:
- `chk_credit_cards_limit_positive` — credit_limit > 0.
- `chk_credit_cards_last_four_digits` — last_four matches `^[0-9]{4}$`.
- `chk_credit_cards_statement_day` — statement_day in 1..31.
- `chk_credit_cards_payment_due_day` — payment_due_day in 1..31.
- `chk_credit_cards_status` — ACTIVE/BLOCKED/CLOSED.

`card_statements` columns: `id UUID`, `credit_card_id UUID NOT NULL` →
`credit_cards` (ON DELETE RESTRICT), `statement_date DATE`, `due_date DATE`,
`statement_amount NUMERIC(15,2)`, `minimum_due NUMERIC(15,2)`,
`paid_amount NUMERIC(15,2) DEFAULT 0`,
`status VARCHAR(20) DEFAULT 'GENERATED'`, `created_at`, `updated_at`.

DB guardrails on `card_statements`:
- `chk_card_statements_amount_nonneg` — statement_amount >= 0.
- `chk_card_statements_min_due_range` — 0 <= minimum_due <= statement_amount.
- `chk_card_statements_paid_range` — 0 <= paid_amount <= statement_amount.
- `chk_card_statements_status` — GENERATED/PARTIALLY_PAID/PAID/OVERDUE.
- `chk_card_statements_due_date` — due_date >= statement_date.
- `uq_card_statements_card_date` — UNIQUE (credit_card_id, statement_date).

## Endpoints

Cards (under `/api/v1/credit-cards`):
- `GET /` — list the authenticated user's cards.
- `GET /{id}` — get an owned card.
- `POST /` — create a card (201).
- `PUT /{id}` — update an owned card.
- `DELETE /{id}` — delete an owned card and its statements (204).

Statements (nested under a card):
- `GET /{cardId}/statements` — list an owned card's statements (newest first).
- `POST /{cardId}/statements` — create a statement for an owned card (201).
- `PUT /{cardId}/statements/{statementId}` — update an owned card's statement.
- `DELETE /{cardId}/statements/{statementId}` — delete a statement (204).

## R1 — Ownership (JWT subject is the only source)
The acting user id comes exclusively from the JWT subject. All card reads and
mutations are scoped to `credit_cards.user_id = <jwt subject>`. A card that
exists but is not owned by the caller is reported as **404
CREDIT_CARD_NOT_FOUND**, never 403. Statements are reached only through their
owned parent card: a statement whose parent card is not owned (or does not
exist) is **404 CARD_STATEMENT_NOT_FOUND**. No ownership leakage.

## R2 — Card creation / update
- `POST` supplies cardName, bank, lastFour, creditLimit, statementDay,
  paymentDueDay. Status defaults to ACTIVE.
- `PUT` updates cardName, bank, creditLimit, statementDay, paymentDueDay,
  status. `lastFour` is immutable after creation (identity of the card).

## R3 — Statement management
- `POST` supplies statementDate, dueDate, statementAmount, minimumDue, and
  optional paidAmount (defaults 0). Status is derived from paidAmount vs
  statementAmount: paidAmount == 0 → GENERATED; 0 < paidAmount <
  statementAmount → PARTIALLY_PAID; paidAmount == statementAmount → PAID.
  (OVERDUE is a time-based status not auto-assigned here; it may be set via
  update.)
- `PUT` updates statementAmount, minimumDue, paidAmount, dueDate, status.
- The service recomputes the derived status on create and on update when the
  caller does not explicitly override it; an explicitly supplied status is
  honored (so OVERDUE can be set).
- Duplicate (card, statementDate) → **409 DUPLICATE_CARD_STATEMENT** (DB
  `uq_card_statements_card_date`).

## R4 — Input validation (mirrors DB CHECKs)
- `cardName` required, 1–120 chars; `bank` required, 1–100 chars.
- `lastFour` required, exactly 4 digits (`^[0-9]{4}$`).
- `creditLimit` required, > 0, scale ≤ 2.
- `statementDay`, `paymentDueDay` required, 1..31.
- Statement: `statementDate`, `dueDate` required with `dueDate >=
  statementDate`; `statementAmount` >= 0; `0 <= minimumDue <= statementAmount`;
  `0 <= paidAmount <= statementAmount`. Cross-field rules the service validates
  before persisting (returning 400) so the user gets a clear message rather
  than a generic integrity error, with the DB CHECKs as the backstop.

## R5 — Consistent errors (ApiResponse envelope)
- Validation failure → `400 VALIDATION_ERROR` (field) or `400 BAD_REQUEST`
  (cross-field, e.g. dueDate before statementDate, minimumDue > statementAmount).
- Missing/non-owned card → `404 CREDIT_CARD_NOT_FOUND`.
- Missing/non-owned statement → `404 CARD_STATEMENT_NOT_FOUND`.
- Duplicate statement date on a card → `409 DUPLICATE_CARD_STATEMENT`.
- Unauthenticated → `401 UNAUTHORIZED`. Constraint names never exposed.

## R6 — No schema change / architecture
- The V2 schema, FKs, and CHECKs are unchanged; they remain the structural
  backstop.
- Thin controllers → services (business rules + `@Transactional` + ownership)
  → repositories; DTO/mapper boundary; `ApiResponse` envelope; module isolated
  in `com.moneypilot.creditcard`.
- Deletion: `card_statements.credit_card_id` is ON DELETE RESTRICT, so deleting
  a card deletes its statements first (service-managed), then the card.

## R7 — Unblock Payment Methods CREDIT_CARD (follow-up in this module)
With `CreditCardRepository.findByIdAndUserId(cardId, userId)` now available, the
Payment Methods V1 capability boundary is lifted:
- `PaymentMethodService` gains the card repository and allows `type =
  CREDIT_CARD` when a valid, owned `creditCardId` is supplied.
- `type = CREDIT_CARD` with a missing/non-owned `creditCardId` → **400
  BAD_REQUEST** (or 404 for a non-owned card, consistent with ownership rules).
- Non-CREDIT_CARD types must not carry a `creditCardId` (DB CHECK
  `chk_payment_methods_credit_card_link` remains the backstop).
- `CreditCardLinkUnsupportedException` and its 400 handler are removed.
