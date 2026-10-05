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
