# MoneyPilot Database Architecture

## 1. Database

PostgreSQL 16.

Schema migrations are managed using Flyway.

## 2. Domain Model

```text
USERS
 |
 +-- LOANS
 |     |
 |     +-- LOAN_EMIS
 |            |
 |            +-- PAYMENTS
 |
 +-- CREDIT_CARDS
 |     |
 |     +-- CARD_STATEMENTS
 |            |
 |            +-- PAYMENTS
 |
 +-- EXPENSES
 |     |
 |     +-- CATEGORIES
 |
 +-- PAYMENT_METHODS
 |
 +-- AUDIT_LOGS
```

## 3. Tables

Current approved tables:

1. users
2. categories
3. payment_methods
4. loans
5. loan_emis
6. credit_cards
7. card_statements
8. expenses
9. payments
10. audit_logs

## 4. Ownership

Every financial record belongs to a user directly or indirectly.

Authorization must verify ownership before reading or modifying data.

Example:

```text
User
 ↓
Loan
 ↓
Loan EMI
 ↓
Payment
```

A user must not be able to access another user's financial records by changing a UUID in an API request.

## 5. Deletion Policy

Financial records use restrictive deletion.

```text
Loan → Loan EMI             RESTRICT
Credit Card → Statement     RESTRICT
EMI → Payment               RESTRICT
Statement → Payment         RESTRICT
```

Soft references:

```text
Expense → Category          SET NULL
Expense → PaymentMethod     SET NULL
AuditLog → User             SET NULL
```

Only:

```text
categories.user_id
```

uses CASCADE according to the approved schema.

## 6. Payment Model

Payments use explicit nullable foreign keys.

```text
payment_type = LOAN_EMI
    → loan_emi_id required
    → card_statement_id null

payment_type = CREDIT_CARD
    → card_statement_id required
    → loan_emi_id null

payment_type = OTHER
    → both null
```

This is enforced by database CHECK constraints.

Do not replace this with polymorphic:

```text
reference_type
reference_id
```

## 7. Partial Payments

Partial payments are supported.

Example:

```text
EMI = ₹25,000

Payment 1 = ₹10,000
Payment 2 = ₹15,000
```

Similarly:

```text
Statement = ₹50,000

Payment 1 = ₹20,000
Payment 2 = ₹30,000
```

Payment records remain independently auditable.

## 8. Monetary Types

All monetary values:

```sql
NUMERIC(15,2)
```

Do not use:

```text
FLOAT
REAL
DOUBLE
```

for financial amounts.

## 9. Sensitive Card Data

The system stores only:

```text
last_four
```

The system must never store:

- full card number
- CVV
- PIN

## 10. Migration Rules

Flyway migrations are immutable after being applied.

Example:

```text
V1__baseline.sql
V2__financial_schema.sql
V3__...
```

Do not modify an already-applied migration.

Create a new migration for schema changes.

## 11. Schema Ownership

The database schema is part of the approved architecture.

Application code must conform to the schema.

Changing the schema requires:

1. requirement
2. design update
3. migration
4. testing
