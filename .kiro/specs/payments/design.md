# Design — Payments Module

Implementation design. No schema change; mirrors the established module
structure and integrates with Loans + Credit Cards for settlement.

## Package layout (`com.moneypilot.payment`)

```
payment/
├── Payment.java                (@Entity -> payments)
├── PaymentType.java            (enum: LOAN_EMI/CREDIT_CARD/OTHER)
├── PaymentRepository.java      (ownership list/filter, sum-by-target)
├── PaymentService.java         (CRUD + ownership + target validation + settlement)
├── PaymentController.java      (/api/v1/payments)
├── PaymentMapper.java
├── PaymentNotFoundException.java  (-> 404)
└── dto/
    ├── CreatePaymentRequest.java
    └── PaymentResponse.java
```

Reuses existing exceptions for reference failures:
`loan.LoanNotFoundException` (404), `creditcard.CreditCardNotFoundException`
(404), `paymentmethod.PaymentMethodNotFoundException` (404),
`common.BadRequestException` (400) — all already mapped.

## Entity

Same conventions: `id`, `created_at`, `updated_at` DB-generated; `amount`
`BigDecimal`; `paidAt` `OffsetDateTime` (DB default now(), but we set it so the
value is explicit); `payment_type` `@Enumerated(STRING)`; target ids plain
nullable UUID columns.

```
@Entity @Table(name="payments")
class Payment {
  @Id @Generated(INSERT) UUID id;
  @Column(name="user_id") UUID userId;
  @Enumerated(STRING) @Column(name="payment_type") PaymentType paymentType;
  @Column(name="loan_emi_id") UUID loanEmiId;              // nullable
  @Column(name="card_statement_id") UUID cardStatementId;  // nullable
  @Column(name="payment_method_id") UUID paymentMethodId;  // nullable
  BigDecimal amount;
  OffsetDateTime paidAt;
  createdAt; updatedAt;
  // ctor(userId, paymentType, loanEmiId, cardStatementId, paymentMethodId, amount, paidAt)
}
```

`paidAt` is marked insertable (unlike the generated audit columns): the service
provides it (request value or now()).

## Repository

```
interface PaymentRepository extends JpaRepository<Payment, UUID> {
  Optional<Payment> findByIdAndUserId(UUID id, UUID userId);

  @Query(user-scoped, optional type/from/to on paid_at, newest first)
  List<Payment> findForUserFiltered(userId, type, from, to);

  @Query("SELECT COALESCE(SUM(p.amount),0) FROM Payment p WHERE p.loanEmiId = :emiId")
  BigDecimal sumByLoanEmiId(UUID emiId);

  @Query("SELECT COALESCE(SUM(p.amount),0) FROM Payment p WHERE p.cardStatementId = :stmtId")
  BigDecimal sumByCardStatementId(UUID stmtId);
}
```

## Cross-module ownership lookups (added, additive)

- `LoanEmiRepository.findOwnedByIdAndUserId(emiId, userId)` —
  `SELECT e FROM LoanEmi e JOIN Loan l ON l.id = e.loanId
   WHERE e.id = :emiId AND l.userId = :userId`.
- `CardStatementRepository.findOwnedByIdAndUserId(stmtId, userId)` —
  `SELECT s FROM CardStatement s JOIN CreditCard c ON c.id = s.creditCardId
   WHERE s.id = :stmtId AND c.userId = :userId`.

These let the Payment service validate target ownership and load the target for
settlement in one step.

## DTOs

- `CreatePaymentRequest { @NotNull PaymentType type; @NotNull @Positive
  @Digits(13,2) amount; OffsetDateTime paidAt?; UUID loanEmiId?; UUID
  cardStatementId?; UUID paymentMethodId? }`.
- `PaymentResponse { id, type, amount, paidAt, loanEmiId, cardStatementId,
  paymentMethodId, createdAt }`.

(No update DTO — payments are immutable; correct via delete + re-create.)

## Service

Constructor-injects `PaymentRepository`, `LoanEmiRepository`,
`CardStatementRepository`, `PaymentMethodRepository`, `PaymentMapper`.

- `list(userId, type, from, to)` → `findForUserFiltered` → map.
- `get(userId, id)` → `findByIdAndUserId` or `PaymentNotFoundException`.
- `create(userId, req)`:
  - validate `paymentMethodId` ownership if present (→ PaymentMethodNotFound).
  - branch on `type`:
    - LOAN_EMI: require `loanEmiId`, `cardStatementId == null` (else BadRequest);
      load owned EMI (→ LoanNotFound); build + save payment; settle EMI.
    - CREDIT_CARD: require `cardStatementId`, `loanEmiId == null`; load owned
      statement (→ CreditCardNotFound); save; settle statement.
    - OTHER: both ids null (else BadRequest); save; no settlement.
  - `paidAt` = req.paidAt or now().
  - return mapped payment.
- `delete(userId, id)`: load owned or 404; capture target ref; delete payment;
  re-settle the affected EMI/statement from the remaining sum.

Settlement helpers (run after the payment row is flushed so the sum includes/
excludes it):

- `settleEmi(emi)`: `total = paymentRepository.sumByLoanEmiId(emi.id)`;
  if `total >= emi.emiAmount` → status PAID, paidDate = today (or the latest
  payment's date — V1 uses `LocalDate.now()` of the settling call for
  simplicity); else status UPCOMING, paidDate null. saveAndFlush emi.
- `settleStatement(stmt)`: `total = paymentRepository.sumByCardStatementId(stmt.id)`;
  `paid = min(total, stmt.statementAmount)`; `stmt.setPaidAmount(paid)`;
  `stmt.setStatus(CardStatementService.deriveStatus(stmt.statementAmount, paid))`;
  saveAndFlush. (Reuses the public static `deriveStatus`.)

All mutations `@Transactional`; identity only from the passed `userId`.

## Controller

Thin; `userId = UUID.fromString(authentication.getName())`; `@Valid` body.
`GET /` with optional `@RequestParam type` (PaymentType) and `from`/`to`
(`@DateTimeFormat ISO date-time`); `GET /{id}`; `POST /` 201; `DELETE /{id}`
204. `ApiResponse` envelope.

## Error mapping (GlobalExceptionHandler)

| Exception                  | HTTP | code              |
| -------------------------- | ---- | ----------------- |
| `PaymentNotFoundException` | 404  | `PAYMENT_NOT_FOUND`|

(Loan/CreditCard/PaymentMethod not-found, BadRequest, validation, unauthorized
handled by existing handlers.)

## Settlement edge cases

- Overpayment on an EMI: EMI becomes PAID; the extra is retained as payment
  history (no negative balance tracking in V1).
- Overpayment on a statement: `paid_amount` capped at `statement_amount` to
  satisfy `chk_card_statements_paid_range`; status PAID.
- Deleting the last payment for a target reverts it (EMI → UPCOMING/paidDate
  null; statement → paid 0 / GENERATED).
- Settlement never changes a user-set OVERDUE on a statement back unless a
  payment arrives; V1 keeps the derive rule (OVERDUE is only set explicitly via
  the statement update endpoint and will be recomputed to GENERATED/PARTIALLY_
  PAID/PAID on the next payment settle — acceptable for V1).

## Tests

Skipped per current instruction (write later).
