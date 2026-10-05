# MoneyPilot Mobile UI — HLD & LLD

**Status:** Proposed / Ready for Kiro implementation  
**Client:** React Native + TypeScript  
**Backend:** Spring Boot Modular Monolith  
**API:** REST / JSON / `/api/v1`

---

## 1. Purpose

This document defines the architecture and implementation design for the MoneyPilot mobile UI.

The UI will be developed before the remaining Loan, Credit Card, Expense, and Payment backend modules so that navigation, information architecture, and user experience can be validated early.

The UI-first phase covers:

- navigation
- screen structure
- design system
- reusable components
- forms
- validation
- loading/error/empty states
- mock presentation data
- client-side state boundaries
- API integration boundaries

The mobile client must never become the authoritative source of financial state.

---

## 2. UI-First Strategy

The UI may use mock data during development.

Mock data is for presentation and interaction only. It must be isolated so it can later be replaced by real API responses without redesigning screens.

Final architecture:

```text
React Native
     |
     | REST / JSON
     v
Spring Boot
     |
     v
PostgreSQL
```

MoneyPilot is **not offline-first**.

Do not introduce SQLite, offline synchronization, or a local authoritative financial database.

---

## 3. Approved Technology Stack

### Core
- React Native
- TypeScript

### Navigation
- React Navigation

### UI
- React Native Paper

### Client State
- Zustand

### Server State
- TanStack Query

### Forms
- React Hook Form

### Validation
- Zod

---

## 4. Architecture Principles

### 4.1 Backend is authoritative

The backend determines authoritative:

- loan balances
- EMI status
- credit-card outstanding
- statement amounts
- payment status
- financial totals

The mobile application must not independently calculate authoritative financial state.

### 4.2 No offline-first

TanStack Query caching is allowed.

Do not introduce:

- SQLite financial storage
- synchronization engines
- conflict-resolution logic
- offline financial transactions

### 4.3 No fake business logic

Mock data is acceptable:

```ts
const mockLoan = {
  name: "Home Loan",
  outstandingPrincipal: 3250000,
  emiAmount: 28500,
};
```

The mobile app must not implement an independent authoritative EMI or financial calculation engine.

### 4.4 Reusable UI

Common patterns should become reusable components:

- AppButton
- AppInput
- AmountText
- FinancialCard
- StatusBadge
- EmptyState
- LoadingState
- ErrorState
- SectionHeader
- ConfirmDialog

---

# 5. Mobile Project Structure

```text
mobile/
└── src/
    ├── navigation/
    │   ├── RootNavigator.tsx
    │   ├── AuthNavigator.tsx
    │   └── MainNavigator.tsx
    │
    ├── theme/
    │   ├── theme.ts
    │   ├── colors.ts
    │   ├── typography.ts
    │   ├── spacing.ts
    │   └── dimensions.ts
    │
    ├── components/
    │   ├── AppButton/
    │   ├── AppInput/
    │   ├── AmountText/
    │   ├── FinancialCard/
    │   ├── StatusBadge/
    │   ├── SectionHeader/
    │   ├── EmptyState/
    │   ├── LoadingState/
    │   ├── ErrorState/
    │   └── ConfirmDialog/
    │
    ├── features/
    │   ├── auth/
    │   ├── dashboard/
    │   ├── loans/
    │   ├── creditCards/
    │   ├── expenses/
    │   ├── payments/
    │   └── profile/
    │
    ├── services/
    │   └── api/
    │       ├── client.ts
    │       ├── authApi.ts
    │       ├── loanApi.ts
    │       ├── creditCardApi.ts
    │       ├── expenseApi.ts
    │       └── paymentApi.ts
    │
    ├── store/
    ├── hooks/
    ├── types/
    └── utils/
```

Feature-specific code should remain inside its feature. Shared UI belongs under `components`.

---

# 6. Navigation Architecture

```text
RootNavigator
    |
    +----------------------+
    |                      |
    v                      v
AuthNavigator         MainNavigator
    |                      |
    +-- Login              +-- Main Tabs
    +-- Register
```

## Authentication Navigation

```text
AuthNavigator
├── LoginScreen
└── RegisterScreen
```

## Main Navigation

Recommended bottom navigation:

```text
Home
Loans
Cards
Expenses
More
```

Conceptually:

```text
MainNavigator
├── Dashboard
├── Loans
│   ├── LoanList
│   ├── LoanDetails
│   └── Add/Edit Loan
├── Credit Cards
│   ├── CardList
│   ├── CardDetails
│   └── Add/Edit Card
├── Expenses
│   ├── ExpenseList
│   └── Add/Edit Expense
└── More
    ├── Payments
    ├── Profile
    └── Settings
```

---

# 7. Screen Inventory

## Authentication
1. Login
2. Register

## Dashboard
3. Dashboard/Home

## Loans
4. Loan List
5. Loan Details
6. Add Loan
7. Edit Loan
8. EMI Schedule

## Credit Cards
9. Credit Card List
10. Credit Card Details
11. Add Credit Card
12. Edit Credit Card
13. Statement Details

## Expenses
14. Expense List
15. Add Expense
16. Edit Expense
17. Expense Details
18. Category Selection

## Payments
19. Payment History
20. Payment Details
21. Make Loan EMI Payment
22. Make Credit Card Payment

## Profile / Settings
23. Profile
24. Settings

---

# 8. Login Screen

```text
MoneyPilot

Email
[________________________]

Password
[____________________ 👁]

[         Login         ]

Create an account
```

Requirements:

- validate email
- validate password
- show field errors
- disable submit while processing
- display authentication errors
- navigate to Dashboard after success

During UI-only development, login may use temporary/mock authentication state.

Do not implement fake JWT handling.

---

# 9. Registration Screen

Fields:

- Display name
- Email
- Password
- Confirm password
- Currency
- Timezone

Validation must align with backend requirements:

```text
Email       max 254
Password    8-128
Name        1-100
Currency    3 uppercase characters
Timezone    valid IANA timezone
```

Confirm-password validation is client-side.

Backend remains authoritative.

---

# 10. Dashboard

The dashboard is the primary MoneyPilot screen.

Recommended sections:

```text
Greeting
    ↓
Financial Overview
    ↓
Upcoming Payments
    ↓
Monthly Spending
    ↓
Loan Summary
    ↓
Credit Card Summary
```

Example:

```text
Good morning, User 👋

Financial Overview
------------------
Outstanding
₹39,30,000

Upcoming Payments
-----------------
Home Loan       ₹28,500
Due Oct 10

HDFC Card       ₹12,500
Due Oct 15

This Month
----------
Expenses        ₹32,500
Loan Payments   ₹28,500
Card Payments   ₹15,000
```

Initially these can be mock values.

Eventually all authoritative values come from backend APIs.

---

# 11. Loan UI

## Loan List

Each loan card:

- loan name
- lender
- loan type
- outstanding principal
- EMI
- next due date
- status

Example:

```text
Home Loan
SBI

Outstanding
₹32,50,000

EMI
₹28,500

Next EMI
Oct 10

ACTIVE
```

## Loan Details

Sections:

```text
Loan Summary
Principal
Outstanding
EMI
Interest Rate
Start Date
End Date
Tenure
Status

EMI Schedule
```

## EMI Schedule

Display:

- installment number
- due date
- EMI amount
- principal component
- interest component
- paid amount
- remaining amount
- status

Statuses:

```text
PAID
DUE
UPCOMING
OVERDUE
WAIVED
```

Status must not rely solely on color.

---

# 12. Credit Card UI

## Card List

Display:

- card name
- bank
- masked last four digits
- credit limit
- available/used amount when supplied by backend
- statement due date
- status

Example:

```text
HDFC Regalia
•••• 4521

Limit
₹2,00,000

Available
₹1,35,000

Due
Oct 15
```

Never display:

- full card number
- CVV
- PIN

## Card Details

Sections:

```text
Card Summary
Current Statement
Minimum Due
Due Date
Available Limit
Statement History
```

---

# 13. Expense UI

## Expense List

Support:

- date grouping
- category
- amount
- payment method
- description

Example:

```text
October 3

Food
Lunch
₹450

Shopping
Clothing
₹2,500

Fuel
Petrol
₹2,000
```

## Add Expense

Fields:

```text
Amount
Category
Payment Method
Date
Description
Notes
```

Validation:

```text
Amount > 0
Category valid
Payment method valid
Date valid
Description optional
Notes optional
```

Use React Hook Form + Zod.

---

# 14. Payment UI

Payment history must clearly distinguish:

```text
Loan EMI Payment
Credit Card Payment
Other Payment
```

Example:

```text
Oct 02

Credit Card Payment
HDFC Card
₹20,000

Oct 01

Loan EMI
Home Loan
₹28,500
```

Payments are not expenses.

## Loan EMI Payment

```text
Loan
Home Loan

EMI Amount
₹28,500

Already Paid
₹10,000

Remaining
₹18,500

Payment Amount
[____________]

Payment Method
[____________]

Payment Date
[____________]

[ Make Payment ]
```

The backend determines authoritative remaining amount.

## Credit Card Payment

```text
Card
HDFC Regalia

Statement Amount
₹50,000

Paid
₹20,000

Remaining
₹30,000

Payment Amount
[____________]

Payment Method
[____________]

[ Make Payment ]
```

Partial payment is supported.

---

# 15. Profile and Settings

Profile:

```text
Name
Email
Currency
Timezone
```

Actions:

- Edit Profile
- Settings
- Logout

Settings:

- Currency
- Timezone
- Theme
- Notifications
- Logout

Do not expose password hashes or sensitive authentication information.

---

# 16. Design System

Create a centralized theme:

```text
theme/
├── theme.ts
├── colors.ts
├── typography.ts
├── spacing.ts
└── dimensions.ts
```

Avoid hardcoding design values independently in screens.

## Typography

Define semantic levels:

```text
display
headline
title
body
label
caption
```

## Spacing

Use semantic tokens:

```text
xs
sm
md
lg
xl
xxl
```

---

# 17. Reusable Components

Create and reuse:

```text
AppButton
AppInput
AmountText
FinancialCard
StatusBadge
SectionHeader
EmptyState
LoadingState
ErrorState
ConfirmDialog
DateField
CurrencyInput
```

---

# 18. State Management

## Zustand

Use for client/application state:

- authentication session state
- UI preferences
- theme preference
- temporary UI state

Do not use Zustand as the primary store for financial server data.

## TanStack Query

Use for server state:

- dashboard
- loans
- EMI schedules
- credit cards
- statements
- expenses
- payments
- profile

Architecture:

```text
Backend API
    ↓
TanStack Query
    ↓
Screen
```

---

# 19. API Client

Create:

```text
services/api/client.ts
```

Responsibilities:

- base URL
- request headers
- authentication header
- response parsing
- common error handling

Feature APIs:

```text
authApi.ts
loanApi.ts
creditCardApi.ts
expenseApi.ts
paymentApi.ts
```

Do not place raw HTTP calls directly throughout screens.

---

# 20. Mock Data

During UI-first development:

```text
feature/
└── mock/
    ├── mockLoans.ts
    ├── mockCards.ts
    ├── mockExpenses.ts
    └── mockPayments.ts
```

Mock objects should resemble eventual API DTOs.

Example:

```ts
type LoanSummary = {
  id: string;
  name: string;
  lender: string;
  outstandingPrincipal: number;
  emiAmount: number;
  nextDueDate: string;
  status: "ACTIVE" | "CLOSED" | "FORECLOSED";
};
```

The screen should not know whether data is mock or API-backed.

---

# 21. Mock-to-API Migration

Initial:

```text
Mock Data
    ↓
Query/Hook abstraction
    ↓
Screen
```

Final:

```text
REST API
    ↓
TanStack Query
    ↓
Screen
```

The screen architecture should remain stable.

---

# 22. Loading, Empty and Error States

Every server-backed screen must define appropriate states.

### Loading

Use a consistent spinner/skeleton strategy.

### Empty

Example:

```text
No loans yet

Add your first loan to start
tracking your EMI schedule.

[ Add Loan ]
```

### Error

```text
Unable to load loans.

Please try again.

[ Retry ]
```

Never expose stack traces or database errors.

---

# 23. Forms

All forms use:

```text
React Hook Form
+
Zod
```

Responsibilities:

```text
React Hook Form
→ form state

Zod
→ client validation

Backend
→ authoritative validation
```

Client validation is not a security boundary.

---

# 24. Security

The mobile application must never store:

- passwords
- CVV
- PIN
- full card numbers
- JWT secrets

Authentication tokens must eventually use secure platform-appropriate storage.

Do not use AsyncStorage as secure authentication-token storage.

---

# 25. Accessibility

Support:

- readable contrast
- sufficient touch targets
- accessible labels
- meaningful error messages
- status information not dependent only on color
- screen-reader-friendly controls where applicable

---

# 26. Testing

## Component tests

Test:

- buttons
- inputs
- amount formatting
- status badges
- empty states

## Screen tests

Test:

- login validation
- registration validation
- loan list rendering
- credit card rendering
- expense form validation
- payment form validation

## Navigation tests

Test:

- authentication flow
- main navigation
- detail navigation

Backend integration tests remain separate.

---

# 27. UI Implementation Phases

### Phase 1 — Foundation

- React Native setup
- TypeScript
- React Native Paper
- theme
- navigation
- reusable components

### Phase 2 — Authentication UI

- Login
- Register

### Phase 3 — Main Shell

- Dashboard
- Bottom navigation
- Profile
- Settings

### Phase 4 — Loans

- Loan list
- Loan details
- Add/edit loan
- EMI schedule

### Phase 5 — Credit Cards

- Card list
- Card details
- Statements
- Add/edit card

### Phase 6 — Expenses

- Expense list
- Add/edit expense
- Categories
- Filtering

### Phase 7 — Payments

- Payment history
- Loan payment
- Credit-card payment
- Payment details

### Phase 8 — API Integration

Replace mock data progressively with backend APIs.

---

# 28. Recommended UI Task Sequence

```text
UI TASK 1
Mobile foundation + project structure

        ↓

UI TASK 2
Theme + reusable component system

        ↓

UI TASK 3
Navigation architecture

        ↓

UI TASK 4
Authentication screens

        ↓

UI TASK 5
Dashboard

        ↓

UI TASK 6
Loans UI

        ↓

UI TASK 7
Credit Cards UI

        ↓

UI TASK 8
Expenses UI

        ↓

UI TASK 9
Payments UI

        ↓

UI TASK 10
Profile + Settings

        ↓

UI TASK 11
Mock data + state integration

        ↓

UI TASK 12
UI testing + stabilization

        ↓

BACKEND API INTEGRATION
```

Kiro should stop after each meaningful task and verify the result before proceeding.

---

# 29. Definition of Done

A UI feature is complete when:

- screen is implemented
- navigation works
- loading state exists
- empty state exists where applicable
- error state exists where applicable
- form validation exists where applicable
- reusable components are used
- no financial business logic is duplicated in the client
- TypeScript is clean
- tests pass
- application runs locally
- mock data can be replaced by API data without redesign

---

# 30. Architecture Constraints

The UI implementation must NOT introduce:

- Redux unless architecture is explicitly changed
- SQLite
- offline-first synchronization
- local financial database
- microservice assumptions
- backend business logic duplicated in React Native
- insecure token storage
- full card-number storage
- password storage
- unnecessary UI libraries
- unnecessary state-management libraries

---

# 31. Future API Boundaries

Expected API boundaries:

```text
/api/v1/auth
/api/v1/users
/api/v1/dashboard
/api/v1/loans
/api/v1/credit-cards
/api/v1/expenses
/api/v1/payments
/api/v1/categories
```

The UI should align mock data with these future domain boundaries.

---

# 32. Final Architecture

```text
                  React Native
                       |
              React Navigation
                       |
          +------------+------------+
          |                         |
       Zustand                 TanStack Query
          |                         |
          |                    API Client
          |                         |
          +------------+------------+
                       |
                    HTTPS
                       |
                Spring Boot API
                       |
       +---------------+---------------+
       |               |               |
      Auth            Loans         Expenses
       |               |               |
       |          Credit Cards      Payments
       |               |               |
       +---------------+---------------+
                       |
                  PostgreSQL
```

The UI is developed early for product validation, but the backend remains the authoritative financial system.

---

# 33. Kiro Implementation Rules

Before implementing:

1. Read this document completely.
2. Inspect the existing mobile project.
3. Inspect existing `.kiro` steering/specification files.
4. Reuse existing architecture where appropriate.
5. Do not recreate existing components.
6. Do not change approved backend architecture to support UI.
7. Do not introduce a new state-management library.
8. Do not introduce offline storage.
9. Do not introduce Docker.
10. Do not introduce backend financial logic into React Native.
11. Keep mock data isolated.
12. Keep API integration isolated.
13. Run the application locally after each meaningful phase.
14. Run available tests.
15. Fix root causes rather than weakening tests.
16. Work incrementally.
17. Do not generate the entire mobile application in one uncontrolled operation.

---

# 34. Development Principle

```text
Design
  ↓
Build UI
  ↓
Validate UX
  ↓
Design backend domain
  ↓
Implement backend
  ↓
Connect API
  ↓
Integration testing
```

The UI-first strategy is intentional: validate the product experience early while keeping financial authority in the backend.
