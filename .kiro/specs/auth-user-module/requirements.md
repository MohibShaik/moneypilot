# Requirements — Authentication / User Module

Specification for the Authentication/User module only. This is a design
specification; **no code is implemented by this spec**. It builds on the approved,
already-applied V2 schema and the existing modular-monolith foundation.

## Context (verified from the foundation)

- Spring Boot 3.3.4, Java 17, PostgreSQL 16, Flyway V2 applied.
- Modular monolith; `com.moneypilot.auth` and `com.moneypilot.user` packages exist but
  are empty placeholders.
- `com.moneypilot.common.ApiResponse<T>(boolean success, T data, Object error)` is the
  response envelope; only `ApiResponse.ok(data)` exists today.
- `SecurityConfig` is already `STATELESS` with CSRF disabled but currently permits all
  requests; it must be tightened by this module.
- `users` table (V2): `id UUID`, `email TEXT UNIQUE`, `display_name TEXT`,
  `password_hash VARCHAR(255)`, `currency CHAR(3) DEFAULT 'INR'`,
  `timezone VARCHAR(50) DEFAULT 'Asia/Kolkata'`, `created_at`, `updated_at`.

## In scope

Three endpoints, all under the existing base path `/api/v1`:

1. **Register** — `POST /api/v1/auth/register`
2. **Login** — `POST /api/v1/auth/login`
3. **Current user** — `GET /api/v1/users/me` (protected)

## Out of scope (explicitly deferred)

Refresh tokens, password reset, email verification, Redis, Docker, roles/authorities
beyond "authenticated user", and any change to the V2 schema.

---

## User stories and acceptance criteria

### R1 — User registration
As a new user, I want to register with email, password, and display name so I can
access MoneyPilot.

**Acceptance criteria (EARS):**
1. WHEN a client POSTs valid registration data to `/api/v1/auth/register`, THE SYSTEM
   SHALL create a `users` row with the password stored as an Argon2id hash in
   `password_hash` and return the created user's public profile. Registration SHALL NOT
   issue a JWT.
2. WHEN `currency` and/or `timezone` are omitted, THE SYSTEM SHALL preserve the
   PostgreSQL column defaults (`INR` / `Asia/Kolkata`) by not sending those columns on
   insert; valid supplied values SHALL be honored when present.
3. WHEN the submitted email already exists, THE SYSTEM SHALL return a generic conflict
   error that does not confirm the specific reason (see R8).
4. THE SYSTEM SHALL never return `password_hash` in any response.
5. WHEN input fails validation (R7), THE SYSTEM SHALL return a validation error and
   SHALL NOT create a user.

### R2 — User login
As a registered user, I want to log in with email and password so I receive a JWT.

**Acceptance criteria:**
1. WHEN valid credentials are POSTed to `/api/v1/auth/login`, THE SYSTEM SHALL verify
   the password against the stored Argon2id hash and return a signed JWT access token.
2. WHEN the email does not exist OR the password is wrong, THE SYSTEM SHALL return the
   SAME generic "invalid credentials" error (see R8), never distinguishing the two.
3. THE SYSTEM SHALL perform authentication statelessly and SHALL NOT create an HTTP
   session.
4. THE SYSTEM SHALL NOT persist the issued JWT in PostgreSQL or anywhere server-side.

### R3 — Current authenticated user
As an authenticated user, I want to retrieve my own profile.

**Acceptance criteria:**
1. WHEN a request to `/api/v1/users/me` carries a valid `Authorization: Bearer <jwt>`,
   THE SYSTEM SHALL return the authenticated user's public profile.
2. WHEN the token is missing, malformed, expired, or invalid, THE SYSTEM SHALL return a
   consistent `401 Unauthorized` ApiResponse error (see R8).
3. THE SYSTEM SHALL resolve the user from the JWT subject claim only; it SHALL NOT trust
   any user identifier supplied in the request body or query.
4. THE SYSTEM SHALL never return `password_hash`.

### R4 — Password hashing
1. THE SYSTEM SHALL hash passwords using **Argon2id** via a Spring Security
   `PasswordEncoder`.
2. THE SYSTEM SHALL store only the encoded hash (fits `VARCHAR(255)`); the plaintext
   password SHALL never be logged or persisted.

### R5 — JWT
1. THE SYSTEM SHALL issue signed JWTs containing only the minimum claims: `sub` (user
   id), `iat`, and `exp`. Email MAY be included only if required; default is `sub` only.
2. THE SYSTEM SHALL sign tokens with **HMAC-SHA256 (HS256)** using a server-held secret
   configured outside source control (never committed).
3. THE SYSTEM SHALL default the access-token TTL to **15 minutes**, configurable through
   externalized configuration.
4. THE SYSTEM SHALL validate signature and expiry on every protected request.
5. Registration SHALL NOT issue a JWT; only login issues a token.

### R6 — Statelessness
1. THE SYSTEM SHALL run Spring Security with `SessionCreationPolicy.STATELESS`.
2. THE SYSTEM SHALL NOT use `HttpSession`, server-side session storage, Redis, or a DB
   token store.

### R7 — Input validation
1. **Email** SHALL be present, non-blank, a valid email format, and reasonably bounded
   (<= 254 chars).
2. **Password** SHALL be present with a minimum length of 8 and a maximum length of 128
   characters. Passwords SHALL NEVER be truncated. Argon2id handles arbitrary-length
   input, so no byte-truncation is applied.
3. **Display name** SHALL be present, non-blank, bounded (e.g. 1–100 chars).
4. **Currency** (optional) SHALL be a 3-letter ISO code when provided.
5. **Timezone** (optional) SHALL be a valid IANA zone id when provided.
6. Validation failures SHALL yield a structured validation error (R8) with field-level
   messages that do not leak sensitive data.

### R8 — Consistent, non-revealing errors
1. ALL error responses SHALL use the existing `ApiResponse` envelope with
   `success=false` and a populated `error`.
2. Registration conflict and login failure messages SHALL be generic:
   - Registration duplicate email → generic "registration could not be completed"
     style conflict, not "email already registered".
   - Login failure (unknown email or bad password) → single "invalid email or password".
3. Authentication/authorization failures SHALL be consistent `401` responses regardless
   of the underlying cause (missing/invalid/expired token).

### R9 — Email uniqueness
1. THE SYSTEM SHALL rely on the DB unique constraint `uq_users_email` as the source of
   truth and SHALL translate a unique-violation into the generic conflict error (R8.2),
   handling the race cleanly without leaking a stack trace.

### R10 — Architecture and separation
1. THE SYSTEM SHALL respect the modular monolith: auth/security concerns in
   `com.moneypilot.auth`, user profile concerns in `com.moneypilot.user`, shared
   envelope/error handling in `com.moneypilot.common`.
2. THE SYSTEM SHALL keep controller / service / repository / dto / mapper / security
   responsibilities separated (see design).
3. THE SYSTEM SHALL NOT modify the V2 schema, introduce Redis/Docker, or implement the
   deferred features in "Out of scope".
