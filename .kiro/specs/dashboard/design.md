# Design — Dashboard Module

Implementation design for the read-only dashboard summary. No schema change, no
writes. Aggregates across the existing modules via a few additive repository
queries.

## Package layout (`com.moneypilot.dashboard`)

```
dashboard/
├── DashboardService.java       (ownership-scoped aggregation, read-only)
├── DashboardController.java    (GET /api/v1/dashboard)
└── dto/
    ├── DashboardResponse.java
    └── UpcomingDueItem.java
```

No entity, repository, or exception of its own — it composes existing
repositories.

## Additive repository queries

All are user-scoped and additive (existing methods unchanged).

`LoanRepository`:
```
@Query("SELECT COUNT(l) FROM Loan l WHERE l.userId=:u AND l.status='ACTIVE'")
long countActiveByUserId(u);
@Query("SELECT COALESCE(SUM(l.outstandingPrincipal),0) FROM Loan l
        WHERE l.userId=:u AND l.status='ACTIVE'")
BigDecimal sumOutstandingPrincipalActive(u);
@Query("SELECT COALESCE(SUM(l.emiAmount),0) FROM Loan l
        WHERE l.userId=:u AND l.status='ACTIVE'")
BigDecimal sumMonthlyEmiActive(u);
```

`CreditCardRepository`:
```
@Query("SELECT COUNT(c) FROM CreditCard c WHERE c.userId=:u AND c.status='ACTIVE'")
long countActiveByUserId(u);
@Query("SELECT COALESCE(SUM(c.creditLimit),0) FROM CreditCard c
        WHERE c.userId=:u AND c.status='ACTIVE'")
BigDecimal sumCreditLimitActive(u);
```

`CardStatementRepository`:
```
// Outstanding balance across the user's non-PAID statements.
@Query("SELECT COALESCE(SUM(s.statementAmount - s.paidAmount),0)
        FROM CardStatement s JOIN CreditCard c ON c.id = s.creditCardId
        WHERE c.userId=:u AND s.status <> 'PAID'")
BigDecimal sumOutstandingForUser(u);

// Unsettled statements for the upcoming-dues list, soonest first.
@Query("SELECT s FROM CardStatement s JOIN CreditCard c ON c.id = s.creditCardId
        WHERE c.userId=:u AND s.status <> 'PAID' ORDER BY s.dueDate ASC")
List<CardStatement> findUnsettledForUser(u);
```

(To build statement labels, the service also needs the owning card name; either
join-fetch or look up cards by id. V1: the service loads the user's cards once
into a `Map<UUID,String>` id→cardName for labeling — simple and avoids N+1.)

`ExpenseRepository`:
```
@Query("SELECT COALESCE(SUM(e.amount),0) FROM Expense e
        WHERE e.userId=:u AND e.expenseDate BETWEEN :from AND :to")
BigDecimal sumForUserBetween(u, from, to);
@Query("SELECT COUNT(e) FROM Expense e
        WHERE e.userId=:u AND e.expenseDate BETWEEN :from AND :to")
long countForUserBetween(u, from, to);
```

`LoanEmiRepository`:
```
// Unpaid EMIs for the user's loans, soonest first (for upcoming dues + labels).
@Query("SELECT e FROM LoanEmi e JOIN Loan l ON l.id = e.loanId
        WHERE l.userId=:u AND e.status IN ('UPCOMING','DUE','OVERDUE')
        ORDER BY e.dueDate ASC")
List<LoanEmi> findUnpaidForUser(u);
```

For EMI labels (loan name + installment no), the service loads the user's loans
once into a `Map<UUID,String>` loanId→loanName.

## DTOs

```
record DashboardResponse(
  long activeLoanCount, BigDecimal totalOutstandingPrincipal,
  BigDecimal totalMonthlyEmi,
  long activeCardCount, BigDecimal totalCreditLimit,
  BigDecimal totalOutstandingStatementBalance,
  BigDecimal currentMonthExpenseTotal, long currentMonthExpenseCount,
  List<UpcomingDueItem> upcomingDues) {}

record UpcomingDueItem(
  PaymentType type,         // reuse com.moneypilot.payment.PaymentType (LOAN_EMI/CREDIT_CARD)
  UUID referenceId, String label, LocalDate dueDate, BigDecimal amountDue) {}
```

Reusing `PaymentType` keeps the "pay this" affordance aligned with the Payments
API (`type` + `referenceId` map directly onto a `CreatePaymentRequest`).

## Service

`@Transactional(readOnly = true) getSummary(UUID userId)`:
1. Loan figures: `countActiveByUserId`, `sumOutstandingPrincipalActive`,
   `sumMonthlyEmiActive`.
2. Card figures: `countActiveByUserId`, `sumCreditLimitActive`,
   `cardStatementRepository.sumOutstandingForUser`.
3. Expense figures: compute `[from,to]` = current month from `LocalDate.now()`
   (`withDayOfMonth(1)` … `with(TemporalAdjusters.lastDayOfMonth())`);
   `sumForUserBetween`, `countForUserBetween`.
4. Upcoming dues:
   - `emis = loanEmiRepository.findUnpaidForUser(userId)`; map to items with
     label `"<loanName> — EMI #<installmentNo>"`, amountDue = `emi.emiAmount`.
   - `stmts = cardStatementRepository.findUnsettledForUser(userId)`; map with
     label `<cardName>`, amountDue = `statementAmount − paidAmount`.
   - merge, sort by `dueDate` ascending (nulls last defensively), take first 10.
   - loan/card name maps built from `loanRepository.findByUserIdOrderByCreatedAtDesc`
     and `creditCardRepository.findByUserIdOrderByCreatedAtDesc`.
5. assemble `DashboardResponse` (all sums already COALESCE'd to 0).

## Controller

```
@RestController @RequestMapping("/api/v1/dashboard")
class DashboardController {
  @GetMapping ApiResponse<DashboardResponse> get(Authentication auth) {
    return ApiResponse.ok(service.getSummary(UUID.fromString(auth.getName())));
  }
}
```

## Tests
Skipped per current instruction (write later).
