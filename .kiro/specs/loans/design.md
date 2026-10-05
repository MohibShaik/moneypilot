# Design — Loans + Loan EMIs Module

Implementation design for the Loans module. No schema change; mirrors the
Categories / Payment Methods / Auth module structure, extended for a parent
(loan) + child (EMI schedule) aggregate.

## Package layout (`com.moneypilot.loan`)

```
loan/
├── Loan.java                   (@Entity -> loans)
├── LoanEmi.java                (@Entity -> loan_emis)
├── LoanType.java               (enum: HOME/CAR/PERSONAL/EDUCATION/GOLD/OTHER)
├── LoanStatus.java             (enum: ACTIVE/CLOSED/FORECLOSED)
├── EmiStatus.java              (enum: UPCOMING/DUE/PAID/OVERDUE/WAIVED)
├── LoanRepository.java         (findByUserIdOrderByCreatedAtDesc, findByIdAndUserId)
├── LoanEmiRepository.java      (findByLoanIdOrderByInstallmentNoAsc, deleteByLoanId)
├── LoanScheduleGenerator.java  (reducing-balance amortization, exact 2dp rows)
├── LoanService.java            (CRUD + ownership + schedule generation)
├── LoanController.java         (/api/v1/loans)
├── LoanMapper.java
├── LoanNotFoundException.java  (-> 404)
└── dto/
    ├── CreateLoanRequest.java
    ├── UpdateLoanRequest.java
    ├── LoanResponse.java        (loan summary + optional emis list)
    └── LoanEmiResponse.java
```

## Entities

Same conventions as the other entities: `id`, `created_at`, `updated_at` are
DB-generated (`@Generated(INSERT)` / `(INSERT,UPDATE)` + `insertable=false`);
enums `@Enumerated(STRING)`; monetary fields `BigDecimal`; dates `LocalDate`;
timestamps `OffsetDateTime`. JPA no-arg ctor `protected`.

The loan ↔ EMI relationship is modeled **by id**, not with a JPA
`@OneToMany`/`@ManyToOne` object graph, to keep the entities flat and the write
path explicit (mirrors how `userId`/`creditCardId` are plain UUID columns
elsewhere). The service loads EMIs via `LoanEmiRepository` when needed.

```
@Entity @Table(name="loans")
class Loan {
  @Id @Generated(INSERT) @Column(insertable=false,updatable=false) UUID id;
  @Column(name="user_id",nullable=false) UUID userId;
  String name;
  String lender;                       // nullable
  @Enumerated(STRING) LoanType loanType;
  BigDecimal principalAmount;
  BigDecimal outstandingPrincipal;
  BigDecimal emiAmount;
  BigDecimal interestRate;
  LocalDate startDate;
  LocalDate endDate;                   // nullable in schema; always set here
  Integer tenureMonths;
  @Enumerated(STRING) LoanStatus status;
  @Generated(INSERT) createdAt; @Generated(INSERT,UPDATE) updatedAt;
  // ctor builds an ACTIVE loan with outstanding = principal
}

@Entity @Table(name="loan_emis")
class LoanEmi {
  @Id @Generated(INSERT) @Column(insertable=false,updatable=false) UUID id;
  @Column(name="loan_id",nullable=false) UUID loanId;
  Integer installmentNo;
  BigDecimal principalComponent;
  BigDecimal interestComponent;
  BigDecimal emiAmount;
  LocalDate dueDate;
  LocalDate paidDate;                  // nullable
  @Enumerated(STRING) EmiStatus status;  // UPCOMING at creation
  @Generated(INSERT) createdAt; @Generated(INSERT,UPDATE) updatedAt;
}
```

## Repositories

```
interface LoanRepository extends JpaRepository<Loan, UUID> {
  List<Loan> findByUserIdOrderByCreatedAtDesc(UUID userId);
  Optional<Loan> findByIdAndUserId(UUID id, UUID userId);
}

interface LoanEmiRepository extends JpaRepository<LoanEmi, UUID> {
  List<LoanEmi> findByLoanIdOrderByInstallmentNoAsc(UUID loanId);
  void deleteByLoanId(UUID loanId);   // used before deleting the parent loan
}
```

## DTOs

- `CreateLoanRequest { @NotBlank @Size(1..150) name; @Size(max=150) lender;
  @NotNull loanType; @NotNull @Positive @Digits(12,2) principalAmount;
  @NotNull @PositiveOrZero @Digits(3,3) interestRate; @NotNull startDate;
  @NotNull @Min(1) @Max(600) tenureMonths; @Positive @Digits(12,2) emiAmount? }`.
- `UpdateLoanRequest { @NotBlank @Size(1..150) name; @Size(max=150) lender;
  @NotNull LoanStatus status }`.
- `LoanResponse { id, name, lender, loanType, principalAmount,
  outstandingPrincipal, emiAmount, interestRate, startDate, endDate,
  tenureMonths, status, createdAt, List<LoanEmiResponse> emis }` — `emis` is
  null/omitted in the list endpoint and populated in the detail endpoint.
- `LoanEmiResponse { id, installmentNo, principalComponent, interestComponent,
  emiAmount, dueDate, paidDate, status }`.

## Schedule generation (`LoanScheduleGenerator`)

Standard reducing-balance (amortizing) schedule, computed in `BigDecimal` and
rounded so every persisted row satisfies the DB CHECK
`principal_component + interest_component = emi_amount` at scale 2.

Algorithm for principal `P`, monthly rate `r = interestRate/100/12`, tenure `n`,
and EMI `E`:

- If the client did not supply `emiAmount`:
  - `r = 0` → `E = round2(P / n)`.
  - `r > 0` → `E = round2( P * r * (1+r)^n / ((1+r)^n - 1) )`.
- Iterate month `k = 1..n` over a running `balance` (starts at `P`):
  - `interest_k = round2(balance * r)`.
  - `principal_k = E - interest_k`.
  - For the **final** installment, set `principal_k = balance` (absorb residual)
    and `E_k = principal_k + interest_k`; otherwise `E_k = E`.
  - Guard: `principal_k` is clamped to not exceed the remaining balance and not
    go negative; `balance -= principal_k`.
  - `due_date_k = startDate.plusMonths(k)`.
- The last row's `emi_amount` may differ slightly from the nominal `E` so the
  components sum exactly and the principal components total `P`. All rows
  therefore satisfy both `component_sum` and `emi_positive` CHECKs.
- `endDate = startDate.plusMonths(n)`.

All monetary rounding uses `RoundingMode.HALF_UP` at scale 2. Interest rate
scale is 3 (matches `NUMERIC(6,3)`).

## Service

- `list(userId)` → `findByUserIdOrderByCreatedAtDesc` → map to summaries (no
  emis).
- `get(userId, id)` → `findByIdAndUserId` or `LoanNotFoundException`; load EMIs
  via `findByLoanIdOrderByInstallmentNoAsc`; map to a detail response.
- `create(userId, req)`:
  - resolve `emiAmount` (supplied or computed by `LoanScheduleGenerator`).
  - build `Loan` (outstanding = principal, status ACTIVE, endDate computed),
    `saveAndFlush` to get the generated id.
  - generate the schedule rows, set `loanId`, `saveAll` + flush.
  - return the detail response (loan + emis).
- `update(userId, id, req)`: load owned or 404; set `name`, `lender`, `status`;
  `saveAndFlush`; return detail (emis reloaded).
- `delete(userId, id)`: load owned or 404; `emiRepository.deleteByLoanId(id)`
  (RESTRICT FK → children first), then `loanRepository.delete(loan)`.

All mutations `@Transactional`; identity only from the passed `userId`. A
generic `DataIntegrityViolationException` is left to the global handler
(409 DATA_INTEGRITY_VIOLATION).

## Controller

Thin; `userId = UUID.fromString(authentication.getName())`; `@Valid` bodies;
`GET /` list, `GET /{id}` detail, `POST /` 201, `PUT /{id}`, `DELETE /{id}`
204. `ApiResponse` envelope.

## Error mapping (GlobalExceptionHandler)

| Exception                 | HTTP | code             |
| ------------------------- | ---- | ---------------- |
| `LoanNotFoundException`   | 404  | `LOAN_NOT_FOUND` |

(Validation / unauthorized / data-integrity handled by existing handlers.)

## Tests

**Unit (`LoanScheduleGeneratorTest`):** interest-bearing schedule — every row
`principal+interest==emi` at scale 2, principal components sum to principal,
`n` rows, due dates monthly; zero-interest schedule — interest components all 0,
principals sum to principal; client-supplied emiAmount is honored.

**Unit (`LoanServiceTest`, mocked repos):** create persists loan then schedule;
get/update/delete of non-owned → `LoanNotFoundException` (never touches
children for update/save); delete removes EMIs before the loan; update changes
only name/lender/status.

**Integration (`LoanIntegrationTest`, local PostgreSQL, full HTTP + security):**
auth required (401); create returns 201 with a generated schedule whose rows
satisfy the sum constraint (persisted — proves the DB CHECK is satisfied);
get detail returns the EMI list; list omits emis; update changes
name/lender/status; delete returns 204 and removes the loan + EMIs; user B
cannot get/update/delete user A's loan (404); validation (bad principal/tenure)
→ 400. Cleanup: delete EMIs then loans then users (all RESTRICT), handled by
deleting via the API or repositories in `@AfterEach`.
