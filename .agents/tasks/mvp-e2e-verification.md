# MVP E2E — Verification Note (payments + profile integration)

This records exactly what was run and the outcomes so the reviewer does not need
to re-run anything. Tests were explicitly out of scope; verification is the
TypeScript type checker plus static (grep) checks.

## What changed

### Task 1 — Payments integration
- Added `mobile/src/services/api/paymentApi.ts` — thin client mirroring the
  backend `PaymentResponse` / `CreatePaymentRequest` DTOs for
  `/api/v1/payments` (list with optional `type`/`from`/`to`, get, create,
  delete). Returns `res.data` directly (shared client unwraps the envelope).
- Rewrote `mobile/src/features/payments/usePayments.ts` to use the real client
  (TanStack Query) instead of `./mock/mockPayments`. Hook signatures and
  return shapes are unchanged (`usePayments`, `usePayment`, `usePayableTarget`
  and their `*Result` types); `groupByDate` grouping is preserved.

Mapping decisions (documented in code comments too):
- `paidAt`: backend returns a full ISO timestamp; reduced to `YYYY-MM-DD` to
  match the mobile `Payment` view contract.
- `label`: backend has no human-readable title, so the payment is labeled by
  its type via the existing `paymentTypeLabel`.
- `paymentMethodLabel`: resolved from the existing payment-methods lookup
  (`paymentMethodsQuery` in `features/expenses/useLookups`).
- `currency`: taken from the user's profile (`GET /users/me` via `useMe`), the
  same source the expenses and dashboard screens use.
- `usePayableTarget`: the backend has no payable-target endpoint and no
  fetch-by-id for an EMI or a statement. The two payment screens forward only a
  bare `targetId`, so the hook resolves it by scanning the user's loans (EMI
  schedules from `GET /loans/{id}`) and cards' statements
  (`GET /credit-cards/{cardId}/statements`) for a matching id. All monetary
  values come from backend fields:
  - EMI: `emiAmount` is the total; an EMI is binary (PAID vs not), so
    `alreadyPaid`/`remaining` are `emiAmount` vs `0` by its PAID status.
  - Statement: `statementAmount` (total) and `paidAmount` (paid);
    `remaining = statementAmount - paidAmount` (presentation-level subtraction
    of two backend-authoritative fields; no client-invented authoritative
    amount — HLD §4.1).
  No backend change was required, so no `warning` pause was raised.

### Task 2 — Profile integration
- `mobile/src/hooks/useMe.ts` already calls `GET /users/me` (via
  `authApi.getMe`), so it was reused rather than duplicated. No new user client
  created.
- Rewrote `mobile/src/features/profile/useProfile.ts` to delegate to `useMe`
  instead of `./mock/mockProfile`. `UseProfileResult` return shape unchanged;
  `ProfileScreen` untouched. Backend `UserResponse` matches `UserProfile`
  exactly, so no field adaptation is needed.

### Task 3 — Remove dead mock code
- Deleted `mobile/src/features/payments/mock/` (mockPayments.ts).
- Deleted `mobile/src/features/profile/mock/` (mockProfile.ts).
- Deleted `mobile/src/features/dashboard/mock/` (mockDashboard.ts) — confirmed
  nothing imported it (useDashboard was already migrated; only a stale comment
  mentioned it).

No screen components were edited; hook return-shape contracts are preserved.

## Commands run and outcomes

1. `npx tsc --noEmit` (from `mobile/`, TypeScript 6.0.3)
   - Result: PASS — zero type errors. Output captured to a temp file which was
     empty on completion.
   - Sanity check: a deliberate type error in a throwaway `src/__tsc_probe.ts`
     made tsc report `TS2322`, confirming the empty output genuinely means "no
     errors". The probe file was then removed and the clean run repeated (still
     empty). Temp files were deleted.
   - Note: the cmd shell reported a non-zero wrapper exit code regardless of
     outcome, so the redirected output file (empty vs. error lines) was used as
     the authoritative signal.

2. Lint: `package.json` has no `typecheck` or `lint` script, so no lint step was
   run (none was invented).

3. Static grep checks (all pass):
   - No feature hook imports from a `./mock/...` path anymore
     (`from './mock` / `/mock/` → no matches).
   - No remaining references to `mockPayments` / `mockProfile` / `mockDashboard`
     / `findMock*` → no matches.
   - `paymentApi.ts` exists and is imported by `usePayments.ts`.
   - `useProfile.ts` fetches `users/me` via `useMe`.

4. Expo dev server / watch processes: NOT started (one-shot commands only).
