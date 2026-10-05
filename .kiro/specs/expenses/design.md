# Design — Expenses Module

Implementation design. No schema change; mirrors the Categories / Payment
Methods / Loans module structure. A flat entity (no child aggregate) with two
optional, ownership-validated references.

## Package layout (`com.moneypilot.expense`)

```
expense/
├── Expense.java                (@Entity -> expenses)
├── ExpenseRepository.java      (ownership list + date/category filter, findByIdAndUserId)
├── ExpenseService.java         (CRUD + ownership + reference validation)
├── ExpenseController.java      (/api/v1/expenses)
├── ExpenseMapper.java
├── ExpenseNotFoundException.java  (-> 404)
└── dto/
    ├── CreateExpenseRequest.java
    ├── UpdateExpenseRequest.java
    └── ExpenseResponse.java
```

Reuses existing exceptions for reference failures:
`com.moneypilot.category.CategoryNotFoundException` (404) and
`com.moneypilot.paymentmethod.PaymentMethodNotFoundException` (404), already
mapped in `GlobalExceptionHandler`.

## Entity

Same conventions: `id`, `created_at`, `updated_at` DB-generated
(`@Generated(INSERT)` / `(INSERT,UPDATE)` + `insertable=false`); money
`BigDecimal`; `expenseDate` `LocalDate`; `categoryId`/`paymentMethodId` plain
nullable UUID columns (modeled by id, not object graph, like the other modules).

```
@Entity @Table(name="expenses")
class Expense {
  @Id @Generated(INSERT) UUID id;
  @Column(name="user_id") UUID userId;
  @Column(name="category_id") UUID categoryId;             // nullable
  @Column(name="payment_method_id") UUID paymentMethodId;  // nullable
  BigDecimal amount;
  LocalDate expenseDate;
  String description;   // nullable
  String notes;         // nullable
  createdAt; updatedAt;
  // ctor(userId, categoryId, paymentMethodId, amount, expenseDate, description, notes)
  // setters for the mutable fields
}
```

## Repository

```
interface ExpenseRepository extends JpaRepository<Expense, UUID> {
  Optional<Expense> findByIdAndUserId(UUID id, UUID userId);

  // Filtered list: user-scoped, optional [from,to] inclusive and optional
  // categoryId; null filters are ignored. Newest expense date first.
  @Query("""
      SELECT e FROM Expense e
      WHERE e.userId = :userId
        AND (:from IS NULL OR e.expenseDate >= :from)
        AND (:to IS NULL OR e.expenseDate <= :to)
        AND (:categoryId IS NULL OR e.categoryId = :categoryId)
      ORDER BY e.expenseDate DESC, e.createdAt DESC
      """)
  List<Expense> findForUserFiltered(userId, from, to, categoryId);
}
```

`CategoryRepository` gains a visibility-by-id lookup used for reference
validation:

```
@Query("SELECT c FROM Category c WHERE c.id = :id
        AND (c.userId IS NULL OR c.userId = :userId)")
Optional<Category> findVisibleByIdForUser(id, userId);
```

(Additive; existing Category methods unchanged.)

## DTOs

- `CreateExpenseRequest { @NotNull @Positive @Digits(13,2) amount; @NotNull
  expenseDate; @Size(max=500) description; @Size(max=2000) notes; UUID
  categoryId; UUID paymentMethodId }`.
- `UpdateExpenseRequest { same fields }` (full replace of mutable fields).
- `ExpenseResponse { id, amount, expenseDate, description, notes, categoryId,
  paymentMethodId, createdAt }`.

## Service

Constructor-injects `ExpenseRepository`, `CategoryRepository`,
`PaymentMethodRepository`, `ExpenseMapper`.

- `list(userId, from, to, categoryId)` → `findForUserFiltered` → map.
- `get(userId, id)` → `findByIdAndUserId` or `ExpenseNotFoundException`.
- `create(userId, req)`:
  - `validateReferences(userId, req.categoryId, req.paymentMethodId)`.
  - build `Expense`, `saveAndFlush`, map.
- `update(userId, id, req)`: load owned or 404; validate references; set
  amount/expenseDate/description/notes/categoryId/paymentMethodId;
  `saveAndFlush`; map.
- `delete(userId, id)`: load owned or 404; delete.

`validateReferences`:
- if `categoryId != null` → `categoryRepository.findVisibleByIdForUser(categoryId,
  userId)` or throw `CategoryNotFoundException`.
- if `paymentMethodId != null` → `paymentMethodRepository
  .findByIdAndUserId(paymentMethodId, userId)` or throw
  `PaymentMethodNotFoundException`.

All mutations `@Transactional`; identity only from the passed `userId`.

## Controller

Thin; `userId = UUID.fromString(authentication.getName())`; `@Valid` bodies.
`GET /` accepts optional `@RequestParam from, to` (`@DateTimeFormat ISO date`)
and `categoryId`. `POST` 201, `PUT /{id}`, `DELETE /{id}` 204. `ApiResponse`.

## Error mapping (GlobalExceptionHandler)

| Exception                   | HTTP | code               |
| --------------------------- | ---- | ------------------ |
| `ExpenseNotFoundException`  | 404  | `EXPENSE_NOT_FOUND`|

(Category/PaymentMethod not-found + validation/unauthorized handled by existing
handlers.)

## Tests (written, not run per instruction)

**Unit (`ExpenseServiceTest`, mocked repos):** create with no references
persists; create validates category visibility (non-visible → CategoryNotFound,
never saves) and payment-method ownership (non-owned → PaymentMethodNotFound);
get/update/delete non-owned → ExpenseNotFound; update replaces mutable fields;
delete removes.

**Integration (`ExpenseIntegrationTest`, local PostgreSQL):** auth 401; create
with amount+date only; create referencing an owned category + owned payment
method persists the references; create referencing a non-owned category → 404
CATEGORY_NOT_FOUND; non-owned payment method → 404 PAYMENT_METHOD_NOT_FOUND;
list with date-range and category filters; validation (amount <= 0) → 400;
update; delete 204; user B cannot get/update/delete user A's expense (404).
Cleanup (RESTRICT on user): delete the user's expenses, payment methods,
categories, then the user.
