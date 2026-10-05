# MoneyPilot Architecture Documentation

This document consolidates the approved MoneyPilot system architecture, backend architecture, database architecture, security architecture, and authentication LLD.

---

# System Architecture

# MoneyPilot System Architecture

## 1. Purpose

MoneyPilot is a personal finance management application designed to help users track loans, EMIs, credit cards, expenses, payments, and financial summaries.

The system is designed initially as a modular monolith with a mobile client.

The architecture prioritizes:

- Simplicity
- Strong financial data integrity
- Clear domain boundaries
- Transactional financial operations
- Security
- Maintainability
- Incremental development
- Local development without Docker
- Future AWS deployment without requiring architectural redesign

## 2. Architecture Style

MoneyPilot uses a **Modular Monolith Architecture**.

The backend is a single Spring Boot application containing independently organized business modules.

Microservices are explicitly out of scope for the initial product.

### Reason

The initial system does not require:

- independent service deployment
- independent scaling
- distributed transactions
- service-to-service communication
- service discovery
- distributed messaging

A modular monolith provides strong domain separation without introducing unnecessary distributed-system complexity.

## 3. High-Level Architecture

```text
                    +----------------------+
                    |   React Native App   |
                    |      Mobile          |
                    +----------+-----------+
                               |
                               | HTTPS / REST / JSON
                               |
                    +----------v-----------+
                    |    Spring Boot       |
                    |    Modular Monolith  |
                    +----------------------+
                               |
          +--------------------+--------------------+
          |                    |                    |
          v                    v                    v
      Auth Module          Finance Modules      Common
                              |
              +---------------+----------------+
              |               |                |
              v               v                v
            Loans        Credit Cards       Expenses
              |               |                |
              v               v                v
            Payments      Statements        Categories
                              |
                              v
                         PostgreSQL
```

## 4. Technology Stack

### Mobile

- React Native
- TypeScript
- React Navigation
- Zustand
- TanStack Query
- React Hook Form
- Zod
- React Native Paper

### Backend

- Java 17 LTS
- Spring Boot 3.3.x
- Spring Web
- Spring Security
- Spring Data JPA
- Hibernate
- Maven
- Flyway

### Database

- PostgreSQL 16

### Testing

- JUnit 5
- Mockito
- Spring Boot Test
- Local PostgreSQL integration testing

Docker and Testcontainers are not required for local development.

## 5. Source of Truth

The backend and PostgreSQL database are the authoritative source of financial state.

The mobile application must never be considered the authoritative source.

TanStack Query may cache server responses but must not become a second financial data store.

MoneyPilot is NOT an offline-first application.

SQLite/offline synchronization is out of scope.

## 6. Repository Structure

```text
moneypilot/
├── .kiro/
├── backend/
├── mobile/
├── database/
├── docs/
└── README.md
```

## 7. Backend Modules

```text
com.moneypilot/

├── common/
├── auth/
├── user/
├── dashboard/
├── loan/
├── creditcard/
├── expense/
├── payment/
├── category/
└── notification/
```

Each business module should own its:

- controllers
- services
- repositories
- entities
- DTOs
- mappers
- validation
- domain-specific exceptions

Modules must not bypass another module's service boundary unnecessarily.

## 8. Financial Data Principles

### Expense

A financial event representing money spent.

Example:

```text
Expense = ₹40,000
Payment method = Credit Card
```

### Payment

A financial event representing repayment of an existing liability.

Example:

```text
Payment = ₹40,000
Payment type = CREDIT_CARD
```

The credit-card payment must NOT create another expense.

## 9. Transactional Principle

Financial operations must be transactional.

```text
Validate payment
      |
      v
Create payment
      |
      v
Update EMI / statement
      |
      v
Update outstanding amount
      |
      v
Create audit log
      |
      v
Commit transaction
```

If any required operation fails, the complete transaction must roll back.

## 10. Money Representation

Java:

```java
BigDecimal
```

PostgreSQL:

```sql
NUMERIC(15,2)
```

Floating-point types must not be used for monetary values.

## 11. API Architecture

Base API path:

```text
/api/v1
```

API format:

```text
REST
JSON
```

Success:

```json
{
  "success": true,
  "data": {}
}
```

Error:

```json
{
  "success": false,
  "data": null,
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "Request validation failed",
    "fieldErrors": {}
  }
}
```

## 12. Authentication

Authentication uses:

- Spring Security
- JWT
- HS256
- Stateless authentication
- Argon2id password hashing

Initial scope intentionally excludes:

- refresh tokens
- Redis
- database sessions
- password reset
- email verification
- social login

## 13. Deployment Strategy

Initial development:

```text
Windows
    |
Spring Boot
    |
Local PostgreSQL
```

No Docker dependency.

Future production deployment may use AWS infrastructure.

AWS deployment is intentionally separated from MVP application development.

## 14. Architectural Constraints

1. Do not introduce microservices.
2. Do not introduce Redis without a documented requirement.
3. Do not introduce Docker as an application requirement.
4. Do not introduce offline-first synchronization.
5. Do not use floating-point values for money.
6. Do not store complete credit-card numbers.
7. Do not store CVV or PIN.
8. Do not expose password hashes through APIs.
9. Financial operations must be transactional.
10. Database migrations must be managed by Flyway.
11. Hibernate must use `ddl-auto=validate`.
12. Existing approved database schema must not be modified casually.
13. Architecture changes require documentation updates before implementation.

## 15. Development Principle

```text
Requirement
    ↓
HLD
    ↓
LLD
    ↓
Implementation
    ↓
Unit Tests
    ↓
Integration Tests
    ↓
Local Verification
    ↓
Commit
```

No large uncontrolled implementation should be generated in a single step.


---

# Backend Architecture

# MoneyPilot Backend Architecture

## 1. Architecture

MoneyPilot backend is a Spring Boot modular monolith.

Base package:

```text
com.moneypilot
```

## 2. Module Structure

```text
com.moneypilot

├── common
├── auth
├── user
├── dashboard
├── loan
├── creditcard
├── expense
├── payment
├── category
└── notification
```

## 3. Standard Module Structure

```text
module/
├── controller/
├── service/
├── repository/
├── entity/
├── dto/
├── mapper/
├── validation/
└── exception/
```

For smaller modules, packages may be flattened while maintaining the same logical separation.

## 4. Layer Responsibilities

### Controller

Responsible for:

- HTTP request handling
- request validation
- authentication context extraction
- response mapping

Controller must not contain business logic.

### Service

Responsible for:

- business rules
- authorization checks
- transaction boundaries
- domain workflows
- orchestration

Financial operations belong here.

### Repository

Responsible for:

- persistence
- database queries

Repositories must not contain business workflows.

### Entity

Responsible for:

- database mapping
- persistence state

Entities must not be exposed directly through REST APIs.

### DTO

DTOs define API contracts.

Never expose:

- passwordHash
- internal security fields
- sensitive database implementation details

### Mapper

Explicit mapping between:

```text
Entity <-> DTO
```

Avoid exposing entities directly.

## 5. Dependency Direction

```text
Controller
    ↓
Service
    ↓
Repository
    ↓
Database
```

DTOs and mappers support the boundary.

Controllers must not directly call repositories for business operations.

## 6. Transaction Management

Use:

```java
@Transactional
```

for financial state-changing workflows.

Example:

```text
PaymentService
    |
    +-- validate ownership
    +-- create payment
    +-- update EMI
    +-- update outstanding principal
    +-- create audit log
```

All operations execute in one transaction.

## 7. Error Handling

Centralized error handling:

```text
GlobalExceptionHandler
```

Domain exceptions are translated into API error responses.

Validation:

```text
MethodArgumentNotValidException
        ↓
400 VALIDATION_ERROR
```

Unexpected errors:

```text
Exception
        ↓
500 INTERNAL_ERROR
```

Do not expose stack traces or database internals to clients.

## 8. API Versioning

All public APIs use:

```text
/api/v1
```

Future breaking API changes use:

```text
/api/v2
```

## 9. Configuration

Configuration must be externalized.

Sensitive configuration must use environment variables.

Example:

```yaml
moneypilot:
  jwt:
    secret: ${MONEYPILOT_JWT_SECRET}
```

Secrets must not be committed to Git.

## 10. Persistence

Spring Data JPA + Hibernate.

Database schema is managed by Flyway.

```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: validate
```

Hibernate must not automatically create or modify production schema.

## 11. Testing Strategy

### Unit tests

Test:

- services
- mappers
- validators
- JWT utilities
- domain rules

### Integration tests

Test:

- repository persistence
- controller/API behavior
- Spring Security
- PostgreSQL constraints

Initial integration tests use local PostgreSQL.

Do not introduce H2 merely to simplify testing.

## 12. Financial Integrity

The backend must never trust client-calculated financial state.

The client must not submit authoritative calculated balances and expect the server to accept them.

The server calculates authoritative financial state from persisted records.


---

# Database Architecture

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


---

# Security Architecture

# MoneyPilot Security Architecture

## 1. Authentication

Authentication uses:

```text
Spring Security
+
JWT
+
Argon2id
```

## 2. Password Storage

Passwords are never stored directly.

```text
Password
   ↓
Argon2id
   ↓
password_hash
```

The original password must never be persisted.

## 3. Password Rules

```text
Minimum: 8 characters
Maximum: 128 characters
```

Passwords must not be silently truncated.

## 4. JWT

Algorithm:

```text
HS256
```

Default TTL:

```text
15 minutes
```

JWT claims:

```text
sub
iat
exp
iss
```

`iss` is optional according to configuration.

No unnecessary claims should be added.

## 5. Stateless Authentication

Backend uses:

```java
SessionCreationPolicy.STATELESS
```

No:

- HttpSession
- server-side login session
- Redis session
- database token storage

## 6. Token Flow

```text
Login
  |
  v
Validate credentials
  |
  v
Generate JWT
  |
  v
Return Bearer token
```

Subsequent request:

```text
Authorization: Bearer <JWT>
              |
              v
       JWT filter
              |
              v
       Validate signature
              |
              v
       Validate expiration
              |
              v
       Extract user ID
              |
              v
    SecurityContext
```

## 7. User Identification

The JWT subject contains the user's UUID.

Protected APIs must resolve the authenticated user from:

```text
JWT subject
```

Never trust a client-supplied user ID for ownership.

## 8. Unauthorized Requests

The following must produce HTTP 401:

- missing token
- malformed token
- invalid token
- expired token
- invalid signature

## 9. Credential Errors

Login failures must not reveal whether an email exists.

Both:

```text
unknown email
wrong password
```

return:

```text
INVALID_CREDENTIALS
```

## 10. Registration

Registration does not issue a JWT.

Flow:

```text
Register
   |
   v
Validate request
   |
   v
Hash password
   |
   v
Persist user
   |
   v
Return public profile
```

Login is required to obtain a token.

## 11. Sensitive Information

Never return:

```text
password_hash
```

Never log:

```text
password
JWT secret
full JWT
full card number
CVV
PIN
```

## 12. Authorization

Authentication answers:

```text
Who is the user?
```

Authorization answers:

```text
Does this user own this financial record?
```

Every financial operation must enforce ownership.

Example:

```text
JWT user
   |
   v
PaymentService
   |
   +-- verify EMI belongs to user
   |
   +-- verify payment method belongs to user
   |
   +-- execute operation
```

## 13. Security Boundary

The React Native client is untrusted.

Never rely on:

- client-side validation alone
- hidden UI fields
- client-side user IDs
- client-calculated balances
- client-side authorization

All important validation and authorization occurs on the backend.


---

# Auth Lld

# MoneyPilot Authentication - Low Level Design

## 1. Scope

This document defines the implementation design for:

- user registration
- user login
- JWT authentication
- current-user profile

APIs:

```text
POST /api/v1/auth/register
POST /api/v1/auth/login
GET  /api/v1/users/me
```

## 2. Package Structure

```text
com.moneypilot

├── common
│   ├── ApiResponse
│   ├── ApiError
│   ├── GlobalExceptionHandler
│   └── SecurityConfig
│
├── auth
│   ├── AuthController
│   ├── AuthService
│   ├── InvalidCredentialsException
│   ├── EmailAlreadyUsedException
│   │
│   ├── dto
│   │   ├── RegisterRequest
│   │   ├── LoginRequest
│   │   └── TokenResponse
│   │
│   └── security
│       ├── JwtProperties
│       ├── JwtService
│       ├── JwtAuthenticationFilter
│       ├── JwtAuthenticationToken
│       └── RestAuthenticationEntryPoint
│
└── user
    ├── User
    ├── UserRepository
    ├── UserService
    ├── UserController
    ├── UserMapper
    └── dto
        └── UserResponse
```

## 3. User Entity

Table:

```text
users
```

Fields:

```text
id
email
display_name
password_hash
currency
timezone
created_at
updated_at
```

The entity must never expose password hash through DTOs.

## 4. UUID Generation

The PostgreSQL schema uses:

```sql
gen_random_uuid()
```

for user IDs.

Hibernate mapping must correctly support database-generated UUID values.

Before registration logic is implemented, verify that:

```java
userRepository.save(user)
```

successfully persists the user and that the generated UUID is available on the persisted entity.

No database migration should be introduced for this.

## 5. Registration Request

Fields:

```text
email
password
displayName
currency
timezone
```

Validation:

```text
email:
  required
  valid email
  maximum 254

password:
  required
  minimum 8
  maximum 128

displayName:
  required
  minimum 1
  maximum 100

currency:
  optional
  exactly 3 uppercase letters

timezone:
  optional
  valid IANA ZoneId
```

## 6. Registration Defaults

If omitted:

```text
currency = INR
timezone = Asia/Kolkata
```

Defaults are explicitly assigned by the service.

Database defaults remain a safety net.

## 7. Registration Flow

```text
HTTP Request
     |
     v
RegisterRequest validation
     |
     v
AuthController
     |
     v
AuthService.register()
     |
     +-- check email existence
     |
     +-- hash password using Argon2id
     |
     +-- create User
     |
     +-- assign defaults
     |
     +-- persist User
     |
     +-- verify duplicate constraint
     |
     v
UserMapper
     |
     v
UserResponse
```

Registration must NOT issue JWT.

## 8. Duplicate Email

Database constraint:

```text
uq_users_email
```

is the final source of truth.

A pre-check:

```text
existsByEmail()
```

may be used as an optimization.

It does not replace the database unique constraint because concurrent requests can race.

Only violations of:

```text
uq_users_email
```

are translated to:

```text
EMAIL_ALREADY_USED
```

Other integrity violations must not be incorrectly classified as duplicate email errors.

## 9. Login Request

Fields:

```text
email
password
```

Both are required.

## 10. Login Flow

```text
HTTP Request
     |
     v
LoginRequest validation
     |
     v
AuthController
     |
     v
AuthService.login()
     |
     +-- find user by email
     |
     +-- verify password
     |
     +-- invalid?
     |      |
     |      +--> INVALID_CREDENTIALS
     |
     +-- generate JWT
     |
     v
TokenResponse
```

## 11. Credential Security

The following must produce the same logical error:

```text
email does not exist
password incorrect
```

Response:

```text
401
INVALID_CREDENTIALS
```

Do not reveal which condition occurred.

## 12. JWT Configuration

```yaml
moneypilot:
  jwt:
    secret: ${MONEYPILOT_JWT_SECRET}
    ttl: ${MONEYPILOT_JWT_TTL:PT15M}
    issuer: ${MONEYPILOT_JWT_ISSUER:moneypilot}
```

Secret must be externally supplied.

Application startup should fail if the JWT secret is missing or unusably short for HS256.

## 13. JWT Generation

```text
Algorithm: HS256
```

Claims:

```text
sub = user UUID
iat = issued-at timestamp
exp = expiration timestamp
iss = configured issuer
```

No password or sensitive user data is included.

## 14. JWT Validation

Validation must verify:

1. signature
2. algorithm
3. expiration
4. issuer when configured

A token using a different algorithm must not be accepted.

## 15. JWT Filter

Implementation:

```text
OncePerRequestFilter
```

Behavior:

### No Authorization header

Continue without authentication.

### Non-Bearer Authorization header

Continue without authentication.

### Valid Bearer token

Create authenticated principal using JWT `sub`.

### Invalid Bearer token

Clear security context and continue.

Protected endpoint subsequently returns:

```text
401
```

## 16. Security Configuration

Security requirements:

```text
STATELESS
CSRF disabled for REST API
formLogin disabled
httpBasic disabled
```

Public:

```text
POST /api/v1/auth/register
POST /api/v1/auth/login
GET  /api/v1/health
```

Protected:

```text
GET /api/v1/users/me
```

All other APIs should require authentication by default.

## 17. Current User API

Endpoint:

```text
GET /api/v1/users/me
```

The user ID must come from:

```text
JWT subject
```

not from a request parameter.

Flow:

```text
JWT
 |
 v
authenticated user UUID
 |
 v
UserController
 |
 v
UserService.getCurrentUser()
 |
 v
UserRepository.findById()
 |
 v
UserMapper
 |
 v
UserResponse
```

## 18. User Response

```json
{
  "id": "UUID",
  "email": "user@example.com",
  "displayName": "User",
  "currency": "INR",
  "timezone": "Asia/Kolkata",
  "createdAt": "timestamp"
}
```

Never include:

```text
passwordHash
```

## 19. Token Response

```json
{
  "accessToken": "...",
  "tokenType": "Bearer",
  "expiresInSeconds": 900
}
```

## 20. Error Contract

Validation:

```text
400 VALIDATION_ERROR
```

Duplicate email:

```text
409 EMAIL_ALREADY_USED
```

Invalid credentials:

```text
401 INVALID_CREDENTIALS
```

Unauthorized request:

```text
401 UNAUTHORIZED
```

Unexpected error:

```text
500 INTERNAL_ERROR
```

Generic database integrity error:

```text
409 DATA_INTEGRITY_VIOLATION
```

Do not expose database constraint names to clients.

## 21. Authentication Sequence

### Registration

```text
Mobile
  |
  | POST /auth/register
  v
AuthController
  |
  v
AuthService
  |
  +--> Argon2id
  |
  +--> UserRepository
  |
  v
PostgreSQL
  |
  v
UserResponse
  |
  v
Mobile
```

### Login

```text
Mobile
  |
  | POST /auth/login
  v
AuthController
  |
  v
AuthService
  |
  +--> UserRepository
  |
  +--> Argon2id verification
  |
  +--> JwtService
  |
  v
TokenResponse
```

### Protected Request

```text
Mobile
  |
  | Authorization: Bearer JWT
  v
JwtAuthenticationFilter
  |
  v
SecurityContext
  |
  v
UserController
  |
  v
UserService
  |
  v
PostgreSQL
```

## 22. Testing

### Unit

Test:

- JWT generation
- JWT expiration
- JWT signature validation
- invalid JWT
- password hashing
- password verification
- UserMapper
- timezone validation
- DTO validation
- exception mapping

### Integration

Test with local PostgreSQL:

- registration
- duplicate registration
- login
- invalid login
- current user
- expired JWT
- tampered JWT
- missing JWT
- generated UUID persistence
- generated timestamps
- password hash persistence
- no JSESSIONID/session creation

Do not use H2.

Do not use Testcontainers.

Do not add Docker as a test requirement.

## 23. Security Test Requirements

Explicitly verify:

```text
password_hash never appears in UserResponse
```

Verify:

```text
wrong password == unknown email
```

from an API response perspective.

Verify:

```text
JWT subject controls user identity
```

Verify a user cannot request another user's profile by modifying request parameters because `/users/me` has no user ID parameter.

## 24. Non-Goals

The authentication implementation must NOT introduce:

- refresh tokens
- password reset
- email verification
- MFA
- OAuth
- Google login
- Apple login
- Redis
- session database
- role/permission system
- Docker
- offline authentication
