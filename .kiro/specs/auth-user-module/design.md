# Design — Authentication / User Module

Design for the Authentication/User module. **No code is implemented by this document.**
It builds on the approved, already-applied V2 schema (unchanged) and the existing
modular-monolith foundation (Spring Boot 3.3.4, Java 17, PostgreSQL 16, Flyway,
`ddl-auto=validate`).

Approved clarifications folded in:
- Password length **8–128**, never truncated.
- JWT **HS256**, default TTL **15 minutes**, TTL externally configurable, secret never
  committed.
- **Registration does not issue a JWT**; it returns the public profile. Login issues the
  token.
- Omitted `currency`/`timezone`: the **service** sets `INR` / `Asia/Kolkata` on the
  entity via normal JPA mappings (Correction 1). The PostgreSQL column defaults remain in
  the V2 schema, unchanged, as a database-level safety net only.
- `DataIntegrityViolationException` is translated to `EmailAlreadyUsedException` **only**
  when the violated constraint is specifically `uq_users_email` (Correction 2); other
  integrity violations are never reported as duplicate-email conflicts.

Explicitly NOT in this design: refresh tokens, Redis, Docker, Testcontainers, roles,
password reset, email verification, any DB token storage. Integration tests use the
configured local PostgreSQL. No V2 schema/migration change.

---

## 1. Package structure (modular monolith)

```
com.moneypilot
├── common
│   ├── ApiResponse               (existing record; add error(..) factory)
│   ├── ApiError                  (new: code, message, fieldErrors?)
│   ├── GlobalExceptionHandler    (new: @RestControllerAdvice)
│   └── SecurityConfig            (existing; tightened)
├── auth
│   ├── AuthController            (POST /auth/register, POST /auth/login)
│   ├── AuthService               (register + authenticate)
│   ├── InvalidCredentialsException
│   ├── EmailAlreadyUsedException
│   ├── dto
│   │   ├── RegisterRequest
│   │   ├── LoginRequest
│   │   └── TokenResponse
│   └── security
│       ├── JwtProperties         (@ConfigurationProperties "moneypilot.jwt")
│       ├── JwtService            (issue + validate)
│       ├── JwtAuthenticationFilter (OncePerRequestFilter)
│       ├── JwtAuthenticationToken  (Authentication carrying userId)
│       └── RestAuthenticationEntryPoint (401 → ApiResponse)
└── user
    ├── User                      (@Entity → existing users table)
    ├── UserRepository            (Spring Data JPA)
    ├── UserService               (profile lookups; load-for-auth)
    ├── UserController            (GET /users/me)
    ├── dto
    │   └── UserResponse
    └── UserMapper                (User → UserResponse; no password_hash)
```

Boundaries: `auth` owns credentials + token lifecycle; `user` owns the profile entity
and its read API; `common` owns the response envelope and global error handling.
`GET /users/me` sits in `user` (it returns a profile); register/login sit in `auth`.

## 2. Entity

`User` maps the V2 `users` table one-to-one. No schema change; Hibernate validates only.

```
@Entity @Table(name = "users")
class User {
  @Id @Column(columnDefinition="uuid")      UUID id;        // DB default gen_random_uuid()
  @Column(nullable=false, unique=true)       String email;
  @Column(name="display_name", nullable=false) String displayName;
  @Column(name="password_hash", nullable=false) String passwordHash; // never serialized
  @Column(name="currency", nullable=false)   String currency;  // service sets INR when omitted
  @Column(name="timezone", nullable=false)   String timezone;  // service sets Asia/Kolkata when omitted
  @Column(name="created_at", insertable=false, updatable=false) OffsetDateTime createdAt;
  @Column(name="updated_at", insertable=false, updatable=false) OffsetDateTime updatedAt;
}
```

Default-preservation strategy (Correction 1): `currency` and `timezone` use **normal JPA
mappings** — `@Column(name=..., nullable=false)` with default `insertable`/`updatable`
(both true). There is **no** `insertable=false` on these columns and **no** custom
Hibernate insert path or dynamic INSERT mechanism. The service always sets a concrete
value before persisting:

- `currency` omitted → service sets `"INR"`.
- `timezone` omitted → service sets `"Asia/Kolkata"`.
- Supplied values → validated (R7) and stored as given.

So the INSERT always carries explicit `currency`/`timezone` values and Hibernate maps
them like any ordinary column. The PostgreSQL column defaults (`INR` / `Asia/Kolkata`)
remain in the V2 schema **unchanged** as a database-level safety net for any writer that
omits them; the application does not rely on them for its own inserts. `id`, `created_at`,
and `updated_at` remain DB-generated (`insertable=false`) and are read back after save so
the returned profile reflects the stored row. No DDL change.

## 3. Repository

```
interface UserRepository extends JpaRepository<User, UUID> {
  Optional<User> findByEmail(String email);
  boolean existsByEmail(String email);
}
```

- `existsByEmail` is a fast pre-check for registration UX.
- The DB unique constraint `uq_users_email` remains the source of truth for the race
  (see §13).

## 4. Services

**AuthService**
- `UserResponse register(RegisterRequest)`:
  1. optional `existsByEmail` fast path → `EmailAlreadyUsedException` (generic conflict);
  2. build `User`, `passwordHash = passwordEncoder.encode(rawPassword)` (Argon2id);
  3. set `currency = request.currency() != null ? request.currency() : "INR"` and
     `timezone = request.timezone() != null ? request.timezone() : "Asia/Kolkata"` — the
     service always supplies concrete values (Correction 1); no reliance on DB defaults
     and no dynamic insert;
  4. `save`; if a `DataIntegrityViolationException` occurs AND the violated constraint is
     specifically `uq_users_email`, translate to `EmailAlreadyUsedException`; any other
     integrity violation is **not** treated as a duplicate email and propagates
     (Correction 2);
  5. map to `UserResponse` and return. **No token issued.**
- `TokenResponse authenticate(LoginRequest)`:
  1. `findByEmail`; if empty OR `!passwordEncoder.matches(raw, hash)` →
     `InvalidCredentialsException` (single generic error);
  2. `JwtService.issue(user.id)`;
  3. return `TokenResponse`.

**UserService**
- `User getById(UUID)` / `UserResponse getProfile(UUID)` for `/users/me`; missing user →
  treated as unauthorized (do not leak existence).

Password handling never logs the raw value; raw password is a local variable only.

## 5. DTOs

```
record RegisterRequest(String email, String password, String displayName,
                       String currency /*opt*/, String timezone /*opt*/) {}
record LoginRequest(String email, String password) {}
record TokenResponse(String accessToken, String tokenType /*"Bearer"*/, long expiresInSeconds) {}
record UserResponse(UUID id, String email, String displayName,
                    String currency, String timezone, OffsetDateTime createdAt) {}
```

`UserResponse` has no password field. `RegisterRequest.password` is input-only and never
echoed.

## 6. Mapper

`UserMapper.toResponse(User)` → `UserResponse`. Plain, explicit field copy; there is no
code path copying `passwordHash`. A unit test asserts no password data leaks.

## 7. Controllers

```
@RestController @RequestMapping("/api/v1/auth")
class AuthController {
  @PostMapping("/register") @ResponseStatus(CREATED)
  ApiResponse<UserResponse> register(@Valid @RequestBody RegisterRequest r) { ... }

  @PostMapping("/login")
  ApiResponse<TokenResponse> login(@Valid @RequestBody LoginRequest r) { ... }
}

@RestController @RequestMapping("/api/v1/users")
class UserController {
  @GetMapping("/me")
  ApiResponse<UserResponse> me(Authentication authentication) { ... } // userId = principal
}
```

All responses wrapped by `ApiResponse`. `/users/me` reads the user id from the security
principal only.

## 8. Validation

Jakarta Bean Validation on request records; `@Valid` on controller params.

| Field       | Constraints                                                              |
| ----------- | ------------------------------------------------------------------------ |
| email       | `@NotBlank`, `@Email`, `@Size(max=254)`                                  |
| password    | `@NotBlank`, `@Size(min=8, max=128)` — **never truncated**               |
| displayName | `@NotBlank`, `@Size(min=1, max=100)`                                     |
| currency    | optional; `@Pattern(regexp="^[A-Z]{3}$")` when present                   |
| timezone    | optional; validated against `ZoneId.getAvailableZoneIds()` when present  |

Violations → `MethodArgumentNotValidException`, handled into a `400` `VALIDATION_ERROR`
with field messages. The 128 max bounds request size without ever trimming the password;
Argon2id accepts the full string.

## 9. Argon2id configuration

- Bean: `PasswordEncoder` implemented with Spring Security's `Argon2PasswordEncoder`
  configured for the **Argon2id** variant (Spring Security 6 default factory produces
  Argon2id), e.g. sensible parameters: saltLength=16, hashLength=32, parallelism=1,
  memory≈19456 KiB (19 MiB), iterations=2 — tunable, documented in config.
- Requires the BouncyCastle dependency that Spring Security's Argon2 encoder uses.
- Stored hash format `$argon2id$v=19$m=...,t=...,p=...$salt$hash` fits `VARCHAR(255)`.
- Verification via `passwordEncoder.matches(raw, storedHash)`. Parameters may be upgraded
  later; `matches` still validates older encodings.

## 10. JWT generation and validation

**JwtProperties** (`@ConfigurationProperties(prefix="moneypilot.jwt")`): `secret`
(String, from env), `ttl` (Duration, default `PT15M`), `issuer` (optional).

**JwtService**
- `String issue(UUID userId)`: builds HS256 token with claims `sub=userId`,
  `iat=now`, `exp=now+ttl` (+ `iss` if configured). Signs with the configured secret.
- `UUID validateAndGetSubject(String token)`: parses with the same secret, verifies
  signature + expiry (+ issuer if set), returns the subject UUID; throws on any failure.
- Algorithm fixed to **HS256**; the parser rejects tokens using a different alg
  (prevents alg-confusion). TTL comes from `JwtProperties.ttl` (default 15 min).

Minimal claims only: no email, roles, or PII unless later required.

## 11. JWT authentication filter

`JwtAuthenticationFilter extends OncePerRequestFilter`:
1. Read `Authorization`; if absent or not `Bearer `, continue the chain unauthenticated.
2. Else `JwtService.validateAndGetSubject(token)`; on success set a
   `JwtAuthenticationToken` (authenticated, principal = userId, no authorities beyond the
   implicit authenticated state) into the `SecurityContext`.
3. On any token error, clear the context and continue; the entry point returns 401 when a
   protected resource is reached.
Registered before `UsernamePasswordAuthenticationFilter`.

## 12. SecurityConfig changes

Starting from the existing stateless config:
- Keep `SessionCreationPolicy.STATELESS`, CSRF disabled (token API).
- Replace blanket `permitAll` with:
  - `permitAll`: `POST /api/v1/auth/register`, `POST /api/v1/auth/login`,
    `GET /api/v1/health`, `/actuator/health`.
  - `authenticated`: `GET /api/v1/users/me` and `anyRequest()`.
- Add the `JwtAuthenticationFilter`.
- Set `exceptionHandling().authenticationEntryPoint(RestAuthenticationEntryPoint)`.
- Beans: `PasswordEncoder` (Argon2id), `JwtService`, filter, entry point.
- No `formLogin`, no `httpBasic`, no session, no default login page.

## 13. DB unique-constraint race handling

Two users registering the same email concurrently both pass `existsByEmail`, then one
`save` wins and the other hits `uq_users_email`. Handling (Correction 2 — constraint
must be identified specifically):
- In `AuthService`, catch `DataIntegrityViolationException` and inspect the violated
  constraint. **Only when the constraint is specifically `uq_users_email`** rethrow
  `EmailAlreadyUsedException`.
- Constraint identification: unwrap the cause to the underlying
  `org.hibernate.exception.ConstraintViolationException` and read
  `getConstraintName()`; match case-insensitively against `uq_users_email`. (Where the
  driver does not surface the name, fall back to inspecting the PostgreSQL SQLState `23505`
  together with the constraint name in the message — still requiring the `uq_users_email`
  name, never a blanket `23505` match.)
- Any **other** integrity violation (a different unique index, a check constraint, a
  foreign-key violation, a not-null violation, etc.) is **NOT** translated to a
  duplicate-email conflict. It is not caught here and propagates to the
  `GlobalExceptionHandler`, which maps a generic `DataIntegrityViolationException` to a
  non-specific `409 DATA_INTEGRITY_VIOLATION` (or `500` if it indicates a server fault) —
  never `REGISTRATION_CONFLICT`.
- `GlobalExceptionHandler` maps `EmailAlreadyUsedException` to the **generic**
  `409 REGISTRATION_CONFLICT` — the same response as the fast-path duplicate — so the
  race is invisible to clients and no stack trace leaks. The DB unique constraint
  `uq_users_email`, not the app `existsByEmail` check, is the source of truth for the
  race.

## 14. ApiResponse / error handling

Extend the existing envelope:
```
record ApiResponse<T>(boolean success, T data, Object error) {
  static <T> ApiResponse<T> ok(T data)        // existing
  static <T> ApiResponse<T> error(ApiError e) // new → (false, null, e)
}
record ApiError(String code, String message, Map<String,String> fieldErrors /*nullable*/) {}
```
Success keeps `error=null`; failures carry a structured `ApiError` and `data=null`.

## 15. GlobalExceptionHandler (`@RestControllerAdvice`)

| Exception                                  | HTTP | code                   |
| ------------------------------------------ | ---- | ---------------------- |
| `MethodArgumentNotValidException`          | 400  | `VALIDATION_ERROR`     |
| `HttpMessageNotReadableException`          | 400  | `BAD_REQUEST`          |
| `EmailAlreadyUsedException` (only `uq_users_email`) | 409 | `REGISTRATION_CONFLICT` |
| `DataIntegrityViolationException` (other, non-email) | 409 | `DATA_INTEGRITY_VIOLATION` (generic) |
| `InvalidCredentialsException`              | 401  | `INVALID_CREDENTIALS`  |
| `AuthenticationException` (entry point)    | 401  | `UNAUTHORIZED`         |
| any other `Exception`                      | 500  | `INTERNAL_ERROR`       |

Only `EmailAlreadyUsedException` — raised exclusively for a `uq_users_email` violation
(Correction 2) — maps to `REGISTRATION_CONFLICT`. A `DataIntegrityViolationException` is
**never** blanket-mapped to a duplicate-email conflict; a non-email integrity violation
gets a distinct generic code. Messages are generic; login failure is always "invalid
email or password"; the registration conflict never says "email already registered". No
raw Spring error page is returned.

## 16. Authentication flow

**Register** → validate → (fast `existsByEmail`) → encode password (Argon2id) → insert
(omit currency/timezone when absent) → on unique violation translate to generic conflict
→ map to `UserResponse` → `201 ApiResponse.ok(profile)`. No token.

**Login** → validate → `findByEmail` → `matches`; failure of either → single
`InvalidCredentialsException` (`401`) → on success `JwtService.issue(id)` →
`200 ApiResponse.ok(TokenResponse)`.

**Protected** → `JwtAuthenticationFilter` validates Bearer token → sets principal
(userId) → `UserController.me` loads profile → `200 ApiResponse.ok(profile)`; any token
problem → `401` via entry point.

## 17. Authorization flow

Single model: authenticated or not. `/users/me` and all non-public `/api/v1/**` require
authentication. The acting user is the JWT `sub`; services derive identity from the
security context only, never from client-supplied ids. This is the integration point for
the per-resource ownership checks defined in the database-foundation spec (not
implemented here).

## 18. Configuration properties

`application.yml` (values externalized; secret from env, never committed):
```
moneypilot:
  jwt:
    secret: ${MONEYPILOT_JWT_SECRET}      # required; no default in source
    ttl: ${MONEYPILOT_JWT_TTL:PT15M}      # default 15 minutes
    issuer: ${MONEYPILOT_JWT_ISSUER:moneypilot}
```
- Startup should fail fast if `secret` is missing/blank or too short for HS256.
- `.gitignore` / docs must note the secret is provided via environment only.

## 19. Endpoint request/response examples

**POST /api/v1/auth/register**
```json
// request
{ "email": "ada@example.com", "password": "s3cretValue!", "displayName": "Ada" }
// 201 response (no token, no password_hash; defaults preserved)
{ "success": true,
  "data": { "id": "7f...e2", "email": "ada@example.com", "displayName": "Ada",
            "currency": "INR", "timezone": "Asia/Kolkata",
            "createdAt": "2026-10-03T10:00:00Z" },
  "error": null }
```

**POST /api/v1/auth/login**
```json
// request
{ "email": "ada@example.com", "password": "s3cretValue!" }
// 200 response
{ "success": true,
  "data": { "accessToken": "eyJhbGciOiJIUzI1Ni␣...", "tokenType": "Bearer",
            "expiresInSeconds": 900 },
  "error": null }
// 401 (unknown email OR wrong password — identical)
{ "success": false, "data": null,
  "error": { "code": "INVALID_CREDENTIALS", "message": "Invalid email or password" } }
```

**GET /api/v1/users/me** (`Authorization: Bearer <jwt>`)
```json
// 200
{ "success": true,
  "data": { "id": "7f...e2", "email": "ada@example.com", "displayName": "Ada",
            "currency": "INR", "timezone": "Asia/Kolkata",
            "createdAt": "2026-10-03T10:00:00Z" },
  "error": null }
// 401 (missing/expired/invalid token)
{ "success": false, "data": null,
  "error": { "code": "UNAUTHORIZED", "message": "Authentication required" } }
```

**Validation error (register)** → `400`
```json
{ "success": false, "data": null,
  "error": { "code": "VALIDATION_ERROR", "message": "Validation failed",
             "fieldErrors": { "password": "size must be between 8 and 128",
                              "email": "must be a well-formed email address" } } }
```

## 20. Security considerations

- **Argon2id** with memory/iteration cost tuned for the environment; parameters in config
  and upgradable.
- Passwords **8–128**, never truncated; raw password never logged.
- **HS256** with a strong env-provided secret (never committed); fail-fast on weak/missing
  secret; parser pins the algorithm to prevent alg-confusion.
- **Stateless**: no `HttpSession`, no `JSESSIONID`, no server-side/DB/Redis token store.
- Short **15-minute** TTL limits token replay window (refresh tokens deferred).
- `password_hash` never serialized; no endpoint or DTO exposes it.
- Error responses are generic and do not reveal whether an email exists.
- `/users/me` resolves identity from the token subject only.

## 21. Test strategy

Testing pyramid: fast unit tests for crypto/token/service logic; focused web-slice tests
for controller/validation/error mapping; full-context integration tests against the local
PostgreSQL for the real persistence + security chain.

### Unit tests
- `JwtService`: issues token with only `sub/iat/exp` (+`iss`); round-trips subject;
  rejects tampered signature; rejects expired token; rejects wrong-algorithm token;
  TTL honored (expiresInSeconds == 900 by default).
- `PasswordEncoder`: encodes to `$argon2id$...`; `matches` true for correct, false for
  wrong; two encodings of the same password differ (random salt); a 128-char password
  round-trips (no truncation) and a 127 vs 128 vs differing-at-char-100 case verifies no
  truncation.
- `AuthService.register`: encodes password; returns profile; never issues a token;
  omitted currency/timezone → service sets `INR` / `Asia/Kolkata` on the entity before
  save (Correction 1); supplied values are passed through.
- `AuthService` constraint translation (Correction 2): a
  `DataIntegrityViolationException` whose constraint is `uq_users_email` →
  `EmailAlreadyUsedException`; a `DataIntegrityViolationException` for a **different**
  constraint is NOT translated (propagates unchanged, not a duplicate-email conflict).
- `AuthService.authenticate`: unknown email and wrong password both throw the SAME
  `InvalidCredentialsException`; success returns a `TokenResponse`.
- `UserMapper`: output has no password data (field/reflection assertion).

### Integration tests (Spring Boot context + configured local PostgreSQL)
Integration tests run against the **configured local PostgreSQL** (the datasource in
`application.yml`). **No Docker and no Testcontainers** are used or added for these tests.

- Register happy path → `201`, body has no `password_hash`, DB row's `password_hash`
  starts with `$argon2id$`, and `currency=INR`, `timezone=Asia/Kolkata` when omitted
  (values set by the service via normal JPA insert, Correction 1).
- Register with explicit valid currency/timezone → stored values honored.
- Register duplicate email → `409 REGISTRATION_CONFLICT`; exactly one row persists. This
  exercises the `uq_users_email`-specific translation (Correction 2).
- Register invalid input (bad email, 7-char password, 129-char password, blank name) →
  `400 VALIDATION_ERROR`; no row created; the 129-char case is rejected, never trimmed.
- Login happy path → `200` with `accessToken`; token validates and `expiresInSeconds==900`.
- Login wrong password and unknown email → identical `401 INVALID_CREDENTIALS`.
- `/users/me` with valid token → `200`, correct profile, no `password_hash`.
- `/users/me` with no token / expired token / tampered token → consistent `401 UNAUTHORIZED`.
- Statelessness: response carries no `Set-Cookie: JSESSIONID`; no token table exists or is
  written.

## 22. Acceptance criteria (module-level)

- All three endpoints behave per R1–R3 and return the `ApiResponse` envelope.
- Registration returns the profile and issues **no** token; login issues the JWT.
- Passwords stored as Argon2id hashes; validated 8–128 and never truncated.
- JWT is HS256 with claims `sub/iat/exp` (+optional `iss`), default 15-min TTL from
  externalized config, secret never committed, validated on every protected request, and
  never persisted server-side.
- Omitted currency/timezone are set by the service (INR / Asia/Kolkata) via normal JPA
  mappings (Correction 1); PostgreSQL defaults remain unchanged as a DB-level safety net.
- `DataIntegrityViolationException` → `EmailAlreadyUsedException` only for the
  `uq_users_email` constraint (Correction 2); other integrity violations are never
  reported as duplicate-email conflicts.
- Login/registration errors are generic and do not reveal email existence; auth failures
  are consistent `401`s.
- `/users/me` is protected and resolves the user from the token subject only;
  `password_hash` never appears in any response or log.
- No V2 schema/migration change; no refresh tokens, Redis, Docker, Testcontainers, roles,
  password reset, email verification, or DB token storage; integration tests use the
  configured local PostgreSQL.
- Hibernate stays `ddl-auto=validate`; build and all new tests pass.
