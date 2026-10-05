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
