# Requirements — Expenses Module

Backend module for user expenses, built on the approved, already-applied V2
schema (`expenses`). No schema change. Follows the established Auth/User,
Categories, Payment Methods, Loans, and Credit Cards module patterns.

## Context (from V2 schema)

`expenses` columns: `id UUID`, `user_id UUID NOT NULL` → `users` (ON DELETE
RESTRICT), `category_id UUID NULL` → `categories` (ON DELETE SET NULL),
`payment_method_id UUID NULL` → `payment_methods` (ON DELETE SET NULL),
`amount NUMERIC(15,2) NOT NULL`, `expense_date DATE NOT NULL`,
`description TEXT NULL`, `notes TEXT NULL`, `created_at`, `updated_at`.

DB guardrails on `expenses`:
- `chk_expenses_amount_positive` — amount > 0.
- FKs on `category_id` and `payment_method_id` enforce existence only (not
  ownership), and are ON DELETE SET NULL (deleting a category/payment method
  nulls the reference on the expense rather than blocking or cascading).

## Endpoints (all under `/api/v1/expenses`)

- `GET /` — list the authenticated user's expenses (newest expense date first).
  Optional filters: `from`, `to` (ISO dates, inclusive), `categoryId`.
- `GET /{id}` — get an owned expense.
- `POST /` — create an expense (201).
- `PUT /{id}` — update an owned expense.
- `DELETE /{id}` — delete an owned expense (204).

## R1 — Ownership (JWT subject is the only source)
The acting user id comes exclusively from the JWT subject. All reads and
mutations are scoped to `expenses.user_id = <jwt subject>`. An expense that
exists but is not owned by the caller is **404 EXPENSE_NOT_FOUND**, never 403.

## R2 — Referenced category / payment method must belong to the user
A client may supply an optional `categoryId` and/or `paymentMethodId`. Because
the DB FKs enforce existence only, the service validates ownership/visibility
before persisting so a user cannot attach another user's category or payment
method:
- `categoryId`: must reference a category **visible** to the user — either a
  system category (`user_id IS NULL`) or one owned by the user. Otherwise
  **404 CATEGORY_NOT_FOUND**.
- `paymentMethodId`: must reference a payment method **owned** by the user.
  Otherwise **404 PAYMENT_METHOD_NOT_FOUND**.
- Both are optional; when null, the expense simply has no category / payment
  method (consistent with the nullable columns).

## R3 — Input validation
- `amount` required, > 0, scale ≤ 2.
- `expenseDate` required (ISO date).
- `description` optional, max 500 chars.
- `notes` optional, max 2000 chars.
- `categoryId`, `paymentMethodId` optional (UUID).

## R4 — Consistent errors (ApiResponse envelope)
- Validation failure → `400 VALIDATION_ERROR`.
- Missing/non-owned expense → `404 EXPENSE_NOT_FOUND`.
- Referenced category not visible → `404 CATEGORY_NOT_FOUND`.
- Referenced payment method not owned → `404 PAYMENT_METHOD_NOT_FOUND`.
- Unauthenticated → `401 UNAUTHORIZED`. Constraint names never exposed.

## R5 — No schema change / architecture
- The V2 schema, FKs, and CHECK are unchanged; they remain the structural
  backstop.
- Thin controller → service (business rules + `@Transactional` + ownership +
  reference validation) → repository; DTO/mapper boundary; `ApiResponse`
  envelope; module isolated in `com.moneypilot.expense`.
- Deletion: `expenses.user_id` is ON DELETE RESTRICT; deleting an expense is a
  plain row delete (expenses have no children). Deleting a category/payment
  method nulls the reference (SET NULL) — not this module's concern.
