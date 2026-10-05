# Requirements — Dashboard Module

Read-only aggregation module providing a single summary payload for the app's
home screen. Built on the already-implemented Loans, Credit Cards, Expenses, and
Payments modules and the V2 schema. No schema change; no writes.

## Endpoint

- `GET /api/v1/dashboard` — returns the authenticated user's financial summary.

## R1 — Ownership (JWT subject is the only source)
Every figure is computed strictly over data owned by the JWT-subject user. No
cross-user aggregation is possible; the user id is never taken from the request.

## R2 — Summary content

The response (`DashboardResponse`) contains:

### Loans
- `activeLoanCount` — count of loans with `status = ACTIVE`.
- `totalOutstandingPrincipal` — Σ `outstanding_principal` over ACTIVE loans.
- `totalMonthlyEmi` — Σ `emi_amount` over ACTIVE loans (the nominal monthly
  commitment).

### Credit cards
- `activeCardCount` — count of cards with `status = ACTIVE`.
- `totalCreditLimit` — Σ `credit_limit` over ACTIVE cards.
- `totalOutstandingStatementBalance` — Σ `(statement_amount − paid_amount)` over
  the user's non-PAID statements (GENERATED / PARTIALLY_PAID / OVERDUE),
  i.e. the amount still owed across statements.

### Expenses
- `currentMonthExpenseTotal` — Σ `amount` of expenses whose `expense_date`
  falls in the current calendar month (server clock).
- `currentMonthExpenseCount` — count of those expenses.

### Upcoming dues (merged, soonest first, capped)
A single list `upcomingDues` of the nearest unsettled obligations, each item:
- `type` — `LOAN_EMI` or `CREDIT_CARD`.
- `referenceId` — the EMI id or statement id.
- `label` — human-readable (loan name + installment no, or card name).
- `dueDate` — the EMI due date or statement due date.
- `amountDue` — EMI amount, or statement `(statement_amount − paid_amount)`.

Selection:
- Loan EMIs with `status` in {UPCOMING, DUE, OVERDUE} (unpaid, not WAIVED/PAID).
- Card statements with `status` in {GENERATED, PARTIALLY_PAID, OVERDUE}
  (not PAID).
- Merged, sorted by `dueDate` ascending, limited to the next 10.

## R3 — Determinism and empty state
- All monetary sums default to `0.00` (never null) when there is no data.
- Counts default to `0`; `upcomingDues` defaults to an empty list.
- "Current month" is `[firstDayOfMonth, lastDayOfMonth]` of the server's current
  date (consistent with the rest of the app's server-clock behavior).

## R4 — Errors (ApiResponse envelope)
- Unauthenticated → `401 UNAUTHORIZED` (existing security chain).
- The endpoint never 404s: an empty dataset returns zeros/empty, not an error.

## R5 — Architecture
- Thin controller → `DashboardService` (ownership-scoped reads + aggregation,
  `@Transactional(readOnly = true)`) → existing repositories (extended with a
  few aggregate/read queries). DTO boundary; `ApiResponse` envelope; module
  isolated in `com.moneypilot.dashboard`.
- No new tables, no writes, no settlement — this module only reads.
