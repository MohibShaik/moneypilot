# Implementation Plan — Authentication / User Module

Tasks are not implemented yet. This plan is for review only. Each task references the
requirements it satisfies. Keep controller/service/repository/dto/mapper/security
responsibilities separated and respect the modular monolith.

- [ ] 1. Add dependencies/config for JWT and Argon2id.
  - JWT library (e.g. jjwt) and Argon2 support (Spring Security crypto / bouncycastle as
    needed). Add `moneypilot.jwt.*` config keys; read secret from `MONEYPILOT_JWT_SECRET`.
  - _Requirements: R4, R5, R6_

- [ ] 2. Extend the common error envelope.
  - Add `ApiError { code, message, fieldErrors? }`, an `ApiResponse.error(...)` factory,
    and a `GlobalExceptionHandler` (`@RestControllerAdvice`) mapping validation, conflict,
    invalid-credentials, auth, and generic errors to `ApiResponse`.
  - _Requirements: R8, R10_

- [ ] 3. User persistence (`com.moneypilot.user`).
  - `User` `@Entity` mapped to the existing `users` table; `UserRepository`
    (`findByEmail`, `existsByEmail`). No schema change; `ddl-auto=validate` stays.
  - _Requirements: R1, R3, R9, R10_

- [ ] 4. DTOs + mapper.
  - `RegisterRequest`, `LoginRequest`, `TokenResponse`, `UserResponse`; `UserMapper`
    that never exposes `passwordHash`.
  - _Requirements: R1, R2, R3, R7_

- [ ] 5. Password encoder bean.
  - Argon2id `PasswordEncoder` wired for hashing and verification.
  - _Requirements: R4_

- [ ] 6. JWT security components (`com.moneypilot.auth.security`).
  - `JwtProperties`, `JwtService` (issue/validate, minimal claims), `JwtAuthenticationFilter`,
    `RestAuthEntryPoint`.
  - _Requirements: R5, R6, R8_

- [ ] 7. Auth service + controller.
  - `AuthService.register` (Argon2id encode, duplicate→generic conflict) and
    `authenticate` (generic invalid-credentials); `AuthController` for
    `POST /auth/register`, `POST /auth/login` with `@Valid`.
  - _Requirements: R1, R2, R7, R8, R9_

- [ ] 8. User controller.
  - `UserController.me` for `GET /users/me`, resolving the user from the JWT subject via
    `UserService`; returns `UserResponse`.
  - _Requirements: R3, R8_

- [ ] 9. Tighten `SecurityConfig`.
  - Keep STATELESS; permit register/login/health/actuator; authenticate everything else;
    register the JWT filter and entry point; no formLogin/httpBasic/session.
  - _Requirements: R3, R6, R8, R10_

- [ ] 10. Tests.
  - Unit (JwtService, encoder, AuthService, UserMapper) and integration (register/login/
    me happy + error paths; no session; no token persistence; hash is Argon2id).
  - _Requirements: R1–R9_

- [ ] 11. Verify.
  - Run `mvnw test` against local PostgreSQL; confirm green and that `password_hash`
    never appears in responses or logs. Do not weaken constraints/tests to pass.
  - _Requirements: all_
