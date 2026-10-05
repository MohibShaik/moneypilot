# Design — Payment Methods Module

Implementation design for the Payment Methods module. No schema change; mirrors
the Categories/Auth module structure.

## Package layout (`com.moneypilot.paymentmethod`)

```
paymentmethod/
├── PaymentMethod.java              (@Entity -> payment_methods)
├── PaymentMethodType.java          (enum: CASH/UPI/CREDIT_CARD/DEBIT_CARD/BANK_TRANSFER/OTHER)
├── PaymentMethodRepository.java    (findByUserIdOrderByLabelAsc, findByIdAndUserId)
├── PaymentMethodService.java       (CRUD + ownership + V1 capability boundary)
├── PaymentMethodController.java    (/api/v1/payment-methods)
├── PaymentMethodMapper.java
├── PaymentMethodNotFoundException.java        (-> 404)
├── CreditCardLinkUnsupportedException.java     (-> 400, temporary)
└── dto/
    ├── CreatePaymentMethodRequest.java
    ├── UpdatePaymentMethodRequest.java
    └── PaymentMethodResponse.java
```

## Entity

Maps `payment_methods` with the same conventions as `Category`/`User`:
`id`, `created_at`, `updated_at` are DB-generated (`@Generated(INSERT)` +
`insertable=false`); `type` is `@Enumerated(STRING)`; `user_id` NOT NULL;
`credit_card_id` nullable (unused in V1, always persisted NULL).

```
@Entity @Table(name="payment_methods")
class PaymentMethod {
  @Id @Generated(INSERT) @Column(insertable=false, updatable=false) UUID id;
  @Column(name="user_id", nullable=false) UUID userId;
  @Enumerated(STRING) @Column(nullable=false) PaymentMethodType type;
  @Column(nullable=false) String label;
  @Column(name="credit_card_id") UUID creditCardId;   // NULL in V1
  @Generated(INSERT) createdAt; @Generated(INSERT,UPDATE) updatedAt;
  // ctor(userId, type, label) — creditCardId stays null in V1
}
```

## Repository

```
interface PaymentMethodRepository extends JpaRepository<PaymentMethod, UUID> {
  List<PaymentMethod> findByUserIdOrderByLabelAsc(UUID userId);
  Optional<PaymentMethod> findByIdAndUserId(UUID id, UUID userId);
}
```

## DTOs

- `CreatePaymentMethodRequest { @NotNull type, @NotBlank @Size(1..100) label }`.
- `UpdatePaymentMethodRequest { @NotNull type, @NotBlank @Size(1..100) label }`.
- `PaymentMethodResponse { id, type, label, creditCardId, createdAt }`
  (`creditCardId` is always null in V1 but kept in the contract for forward
  compatibility).

A request `creditCardId` field is intentionally omitted in V1 — supplying a card
only makes sense with `type=CREDIT_CARD`, which is rejected; adding the field
later is backward-compatible.

## Service

- `list(userId)` → `findByUserIdOrderByLabelAsc` → map.
- `create(userId, req)`:
  - if `req.type == CREDIT_CARD` → throw `CreditCardLinkUnsupportedException`.
  - else build `PaymentMethod(userId, type, label)` (creditCardId null),
    `saveAndFlush`, map.
- `update(userId, id, req)`:
  - load via `findByIdAndUserId` or throw `PaymentMethodNotFoundException`.
  - if `req.type == CREDIT_CARD` → throw `CreditCardLinkUnsupportedException`.
  - set type + label (creditCardId stays null), `saveAndFlush`, map.
- `delete(userId, id)`: load via `findByIdAndUserId` or 404; delete.

All mutations `@Transactional`; identity only from the passed `userId`.

## Controller

Thin; `userId = UUID.fromString(authentication.getName())`; `@Valid` bodies;
`GET` list, `POST` 201, `PUT /{id}`, `DELETE /{id}` 204. `ApiResponse` envelope.

## Error mapping (GlobalExceptionHandler)

| Exception                            | HTTP | code                          |
| ------------------------------------ | ---- | ----------------------------- |
| `PaymentMethodNotFoundException`     | 404  | `PAYMENT_METHOD_NOT_FOUND`    |
| `CreditCardLinkUnsupportedException` | 400  | `CREDIT_CARD_LINK_UNSUPPORTED`|

(Validation/unauthorized handled by existing handlers.)

## Tests

**Unit (`PaymentMethodServiceTest`, mocked repo):** create non-CC type persists
with null card; create/update with `CREDIT_CARD` throws
`CreditCardLinkUnsupportedException` and never saves; update/delete of non-owned
→ `PaymentMethodNotFoundException`; delete of owned calls repo.delete.

**Integration (`PaymentMethodIntegrationTest`, local PostgreSQL, full HTTP +
security):** auth required (401); create/list/update/delete owned (UPI/CASH
etc.); `type=CREDIT_CARD` → 400 `CREDIT_CARD_LINK_UNSUPPORTED`; validation →
400; user B cannot update/delete or see user A's method (404 / absent in list).
Cleanup deletes created users (payment_methods.user_id is ON DELETE RESTRICT, so
delete the user's payment methods first, then the user).

## Follow-up (when Credit Cards module exists)
Lift the capability boundary: inject `CreditCardRepository`, validate
`credit_card_id` with `findByIdAndUserId(cardId, userId)`, allow `CREDIT_CARD`,
persist the (owned) card id. The DB CHECK/FK remain the backstop.
