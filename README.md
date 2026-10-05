# MoneyPilot

A personal finance app: track loans and EMIs, credit cards and statements,
expenses, and payments, with a dashboard summary. Spring Boot REST API +
React Native (Expo) mobile client.

## Repository layout

```
moneypilot/
├── backend/     Spring Boot API (Java 17, PostgreSQL, Flyway, JWT auth)
├── mobile/      Expo / React Native app (TypeScript)
├── docs/        Architecture & design docs (ERD, HLD/LLD)
├── .kiro/       Specs (requirements/design per module)
└── render.yaml  Render deployment blueprint for the backend
```

## Backend

Spring Boot 3.3 service exposing `/api/v1`. Stateless JWT auth (HS256), Argon2id
password hashing, PostgreSQL with Flyway migrations, and an OpenAPI/Swagger UI.

Modules: auth/users, categories, payment methods, loans + EMIs, credit cards +
statements, expenses, payments, dashboard.

### Run locally

Requires JDK 17 and a local PostgreSQL. Defaults connect to
`jdbc:postgresql://localhost:5432/moneypilot` (override via `MONEYPILOT_DB_*`
env vars — see `backend/src/main/resources/application.yml`).

```bash
cd backend
./mvnw spring-boot:run
```

- API base: `http://localhost:8080/api/v1`
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI spec: `http://localhost:8080/v3/api-docs`
- Health: `http://localhost:8080/api/v1/health`

### Deploy

See [`backend/DEPLOYMENT.md`](backend/DEPLOYMENT.md) for the free deployment
runbook (Render for the app + Neon for persistent PostgreSQL).

## Mobile

Expo (React Native, TypeScript) client. Navigation via React Navigation, UI via
React Native Paper, forms via React Hook Form + Zod, server state via TanStack
Query, HTTP via axios.

### Run locally

Requires Node.js. The app points at the local backend; the base URL per
platform is configured in `app.json` `extra` (`apiBaseUrl`,
`apiBaseUrlAndroid`).

```bash
cd mobile
npm install
npx expo start
```

## Status

- Backend: all domain modules implemented; OpenAPI docs and deployment artifacts
  ready.
- Mobile: UI screens built; API integration in progress (axios client, real
  auth, swapping mock-backed hooks to TanStack Query).
