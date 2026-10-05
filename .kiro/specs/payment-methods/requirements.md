# Requirements — Payment Methods Module

Backend module for user payment methods, built on the approved, already-applied
V2 schema (`payment_methods`). No schema change. Follows the established
Auth/User and Categories module patterns.

## Context (from V2 schema)

`payment_methods` columns: `id UUID`, `user_id UUID NOT NULL` → `users`,
`type TEXT` (CHECK: CASH/UPI/CREDIT_CARD/DEBIT_CARD/BANK_TRANSFER/OTHER),
`label TEXT NOT NULL`, `credit_card_id UUID NULL` → `credit_cards` (ON DELETE
RESTRICT), `created_at`, `updated_at`.

DB guardrails already present:
- `chk_payment_methods_type` — the six allowed type values.
- `chk_payment_methods_credit_card_link` — `type=CREDIT_CARD ⇔ credit_card_id
  NOT NULL`; all other types ⇔ `credit_card_id NULL`.
- FK `credit_card_id → credit_cards(id)` (existence only — NOT ownership).

## Endpoints (all under `/api/v1/payment-methods`)

- `GET /` — list the authenticated user's payment methods.
- `POST /` — create a payment method (201).
- `PUT /{id}` — update an owned payment method.
- `DELETE /{id}` — delete an owned payment method (204).

## R1 — Ownership (JWT subject is the only source)
The acting user id comes exclusively from the JWT subject. All reads and
mutations are scoped to `payment_methods.user_id = <jwt subject>`. A payment
method that exists but is not owned by the caller is reported as **404**, never
403, so another user's id existence is not leaked (same rule as Categories).

## R2 — V1 capability boundary (temporary)
Types fully supported in V1: `CASH`, `UPI`, `DEBIT_CARD`, `BANK_TRANSFER`,
`OTHER` — all with `credit_card_id = NULL`.

`CREDIT_CARD` is a **temporary capability boundary**, not a permanent business
rule. The DB FK/CHECK can enforce the link's shape and existence but cannot
establish credit-card ownership, and the Credit Cards module (repository/service)
does not exist yet. Allowing the association now would create an application-level
authorization gap (a user could link another user's card). Therefore:

- `POST` or `PUT` with `type = CREDIT_CARD` is rejected with **400
  CREDIT_CARD_LINK_UNSUPPORTED**.
- When the Credit Cards module is implemented, this restriction is lifted in a
  small follow-up: `PaymentMethodService` will validate `credit_card_id` via
  `CreditCardRepository.findByIdAndUserId(cardId, jwtUserId)` and then allow
  `CREDIT_CARD`.

## R3 — Input validation
- `type` required; one of the six enum values.
- `label` required, 1–100 chars.
- `creditCardId` optional in the request; for V1 it is effectively unused
  because `CREDIT_CARD` is rejected and non-credit-card types must not carry a
  card id. The service never persists a non-null `credit_card_id` in V1.

## R4 — Consistent errors (ApiResponse envelope)
- Validation failure → `400 VALIDATION_ERROR`.
- `type = CREDIT_CARD` in V1 → `400 CREDIT_CARD_LINK_UNSUPPORTED`.
- Missing/non-owned payment method → `404 PAYMENT_METHOD_NOT_FOUND`.
- Unauthenticated → `401 UNAUTHORIZED` (existing security chain).
- Constraint names are never exposed.

## R5 — No schema change / architecture
- The V2 schema, FK, and CHECK are unchanged; they remain the structural
  backstop.
- Thin controller → service (business rules + `@Transactional` + ownership) →
  repository; DTO/mapper boundary; `ApiResponse` envelope; module isolated in
  `com.moneypilot.paymentmethod`.
