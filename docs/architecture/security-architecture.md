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
