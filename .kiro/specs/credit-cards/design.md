# Design — Credit Cards + Card Statements Module

Implementation design. No schema change; mirrors the Loans / Payment Methods /
Categories module structure, extended for a parent (card) + child (statement)
aggregate, and lifts the Payment Methods CREDIT_CARD boundary.

## Package layout (`com.moneypilot.creditcard`)

```
creditcard/
├── CreditCard.java              (@Entity -> credit_cards)
├── CardStatement.java           (@Entity -> card_statements)
├── CardStatus.java              (enum: ACTIVE/BLOCKED/CLOSED)
├── StatementStatus.java         (enum: GENERATED/PARTIALLY_PAID/PAID/OVERDUE)
├── CreditCardRepository.java    (findByUserIdOrderByCreatedAtDesc, findByIdAndUserId)
├── CardStatementRepository.java (findByCreditCardIdOrderByStatementDateDesc,
│                                 findByIdAndCreditCardId, deleteByCreditCardId)
├── CreditCardService.java       (card CRUD + ownership)
├── CardStatementService.java    (statement CRUD + ownership via parent card)
├── CreditCardController.java     (/api/v1/credit-cards)
├── CardStatementController.java  (/api/v1/credit-cards/{cardId}/statements)
├── CreditCardMapper.java
├── CardStatementMapper.java
├── CreditCardNotFoundException.java      (-> 404)
├── CardStatementNotFoundException.java   (-> 404)
├── DuplicateCardStatementException.java  (-> 409)
└── dto/
    ├── CreateCreditCardRequest.java
    ├── UpdateCreditCardRequest.java
    ├── CreditCardResponse.java
    ├── CreateCardStatementRequest.java
    ├── UpdateCardStatementRequest.java
    └── CardStatementResponse.java
```

## Entities

Same conventions as the other entities: `id`, `created_at`, `updated_at`
DB-generated (`@Generated(INSERT)` / `(INSERT,UPDATE)` + `insertable=false`);
enums `@Enumerated(STRING)`; money `BigDecimal`; dates `LocalDate`; `last_four`
and day fields plain columns. The card ↔ statement link is modeled by id
(`CardStatement.creditCardId`), consistent with Loans.

```
@Entity @Table(name="credit_cards")
class CreditCard {
  @Id @Generated(INSERT) UUID id;
  @Column(name="user_id") UUID userId;
  String cardName; String bank; String lastFour;
  BigDecimal creditLimit;
  Short statementDay; Short paymentDueDay;
  @Enumerated(STRING) CardStatus status;   // ACTIVE at creation
  createdAt; updatedAt;
}

@Entity @Table(name="card_statements")
class CardStatement {
  @Id @Generated(INSERT) UUID id;
  @Column(name="credit_card_id") UUID creditCardId;
  LocalDate statementDate; LocalDate dueDate;
  BigDecimal statementAmount; BigDecimal minimumDue; BigDecimal paidAmount;
  @Enumerated(STRING) StatementStatus status;
  createdAt; updatedAt;
}
```

`statement_day`/`payment_due_day` are SMALLINT → mapped as `Short`.

## Repositories

```
interface CreditCardRepository extends JpaRepository<CreditCard, UUID> {
  List<CreditCard> findByUserIdOrderByCreatedAtDesc(UUID userId);
  Optional<CreditCard> findByIdAndUserId(UUID id, UUID userId);
}

interface CardStatementRepository extends JpaRepository<CardStatement, UUID> {
  List<CardStatement> findByCreditCardIdOrderByStatementDateDesc(UUID cardId);
  Optional<CardStatement> findByIdAndCreditCardId(UUID id, UUID cardId);
  @Modifying void deleteByCreditCardId(UUID cardId);
}
```

`CreditCardRepository.findByIdAndUserId` is the hook the Payment Methods module
uses to validate card ownership.

## DTOs

- `CreateCreditCardRequest { @NotBlank @Size(1..120) cardName; @NotBlank
  @Size(1..100) bank; @Pattern("^[0-9]{4}$") lastFour; @NotNull @Positive
  @Digits(13,2) creditLimit; @NotNull @Min(1)@Max(31) statementDay; @NotNull
  @Min(1)@Max(31) paymentDueDay }`.
- `UpdateCreditCardRequest { cardName, bank, creditLimit, statementDay,
  paymentDueDay, @NotNull CardStatus status }` (same validation; no lastFour).
- `CreditCardResponse { id, cardName, bank, lastFour, creditLimit, statementDay,
  paymentDueDay, status, createdAt }`.
- `CreateCardStatementRequest { @NotNull statementDate; @NotNull dueDate;
  @NotNull @PositiveOrZero @Digits(13,2) statementAmount; @NotNull
  @PositiveOrZero @Digits(13,2) minimumDue; @PositiveOrZero @Digits(13,2)
  paidAmount? ; StatementStatus status? }`.
- `UpdateCardStatementRequest { statementAmount, minimumDue, paidAmount,
  dueDate, StatementStatus status? ; (statementDate immutable — it is the
  unique key with the card) }`.
- `CardStatementResponse { id, creditCardId, statementDate, dueDate,
  statementAmount, minimumDue, paidAmount, status, createdAt }`.

## Status derivation (statements)

`deriveStatus(statementAmount, paidAmount)`:
- `paidAmount <= 0` → GENERATED.
- `paidAmount >= statementAmount` → PAID.
- otherwise → PARTIALLY_PAID.

Used on create and on update when the caller does not supply an explicit
`status`; an explicit `status` (e.g. OVERDUE) is honored.

## Cross-field validation (service, before persist)

- `dueDate >= statementDate` else `BadRequestException("dueDate must not be
  before statementDate")`.
- `0 <= minimumDue <= statementAmount` else BadRequest.
- `0 <= paidAmount <= statementAmount` else BadRequest.

(`BadRequestException` already maps to 400 BAD_REQUEST via the existing handler
path, or add a small mapping if not present.)

## Services

`CreditCardService`:
- `list/get/create/update` straightforward with ownership via
  `findByIdAndUserId`.
- `delete(userId, id)`: load owned or 404; `statementRepository
  .deleteByCreditCardId(id)` then `cardRepository.delete(card)` (RESTRICT FK).

`CardStatementService` (ownership always through the parent card):
- helper `requireOwnedCard(userId, cardId)` → `creditCardRepository
  .findByIdAndUserId` or `CreditCardNotFoundException`.
- `list(userId, cardId)`: requireOwnedCard; return statements.
- `create(userId, cardId, req)`: requireOwnedCard; validate cross-field; derive
  or honor status; saveAndFlush; translate `uq_card_statements_card_date` →
  `DuplicateCardStatementException`.
- `update(userId, cardId, stmtId, req)`: requireOwnedCard; load via
  `findByIdAndCreditCardId` or `CardStatementNotFoundException`; apply; derive
  status if not supplied; saveAndFlush (same duplicate translation not needed —
  statementDate immutable).
- `delete(userId, cardId, stmtId)`: requireOwnedCard; load or 404; delete.

All mutations `@Transactional`; identity only from the passed `userId`.

## Controllers

Thin; `userId = UUID.fromString(authentication.getName())`; `@Valid` bodies.
`CardStatementController` is mapped under
`/api/v1/credit-cards/{cardId}/statements` and passes `cardId` to the service.

## Error mapping (GlobalExceptionHandler)

| Exception                          | HTTP | code                        |
| ---------------------------------- | ---- | --------------------------- |
| `CreditCardNotFoundException`      | 404  | `CREDIT_CARD_NOT_FOUND`     |
| `CardStatementNotFoundException`   | 404  | `CARD_STATEMENT_NOT_FOUND`  |
| `DuplicateCardStatementException`  | 409  | `DUPLICATE_CARD_STATEMENT`  |
| `BadRequestException` (cross-field)| 400  | `BAD_REQUEST`               |

## Payment Methods boundary lift (R7)

- Add `CreditCardRepository` to `PaymentMethodService`.
- `CreatePaymentMethodRequest`/`UpdatePaymentMethodRequest` gain an optional
  `UUID creditCardId`.
- Service logic:
  - `type == CREDIT_CARD`: require non-null `creditCardId`; validate via
    `creditCardRepository.findByIdAndUserId(cardId, userId)` or throw
    `CreditCardNotFoundException` (404); persist the owned card id.
  - `type != CREDIT_CARD`: `creditCardId` must be null (ignore/reject a stray
    one); persist null.
- Remove `CreditCardLinkUnsupportedException`, its handler, and the
  `rejectCreditCardForNow` guard. `PaymentMethodResponse.creditCardId` now
  carries a real value for credit-card methods.
- Update the Payment Methods tests that asserted the 400 boundary to the new
  behavior (owned card → created; missing/non-owned card → 404).

## Tests (written, not run per instruction)

**Unit (`CreditCardServiceTest`, `CardStatementServiceTest`, mocked repos):**
card CRUD + ownership 404; statement create derives status, honors explicit
status, cross-field validation throws BadRequest, duplicate → Duplicate; delete
removes statements before card; statement ops require owned parent card.

**Integration (`CreditCardIntegrationTest`, local PostgreSQL):** auth 401; card
CRUD; statement create/list/update/delete; derived statuses persist (proves DB
CHECKs); duplicate statement date → 409; cross-field 400; ownership isolation
(user B → 404 for card and statements). Payment Methods: CREDIT_CARD with an
owned card → 201 with creditCardId; with a non-owned/missing card → 404/400.
Cleanup respects RESTRICT order: statements → cards → payment methods → user.
