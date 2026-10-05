# MoneyPilot Backend

Initial Spring Boot backend foundation for **MoneyPilot**, a personal finance
application. This is the foundation only: no business/domain features (loans,
EMIs, credit cards, expenses, payments, dashboard, notifications,
authentication) are implemented yet.

## Tech stack

| Concern            | Choice                                  |
| ------------------ | --------------------------------------- |
| Language           | Java 17 (LTS)                           |
| Framework          | Spring Boot 3.3.x                       |
| Build tool         | Maven (via the Maven Wrapper)           |
| Database           | PostgreSQL 16                           |
| Schema migrations  | Flyway                                  |
| API style          | REST + JSON, base path `/api/v1`        |
| Architecture       | Modular monolith                        |
| Money values       | `BigDecimal` / `NUMERIC(15,2)` in SQL   |

## Requirements

- **Java 17** (LTS). Verify with:

  ```bash
  java -version
  ```

- **PostgreSQL 16** running and reachable (see Database configuration below).
- **Docker** — required only to run the integration/context tests, which use
  Testcontainers to spin up a disposable PostgreSQL 16 instance.
- Maven does **not** need to be installed globally; use the bundled Maven
  Wrapper (`mvnw` / `mvnw.cmd`).

## Database configuration

The application connects to PostgreSQL using the following defaults, which are
read from environment variables so they can be overridden per environment:

| Setting  | Environment variable      | Local default                                   |
| -------- | ------------------------- | ----------------------------------------------- |
| JDBC URL | `MONEYPILOT_DB_URL`       | `jdbc:postgresql://localhost:5432/moneypilot`   |
| Username | `MONEYPILOT_DB_USERNAME`  | `moneypilot`                                    |
| Password | `MONEYPILOT_DB_PASSWORD`  | `moneypilot_local`                              |

Key behavior:

- **The database is the source of truth.** Hibernate runs with
  `spring.jpa.hibernate.ddl-auto=validate`; it will **never** create or modify
  tables.
- **Flyway manages all schema changes.** Migrations live in
  `src/main/resources/db/migration` (`V1__baseline.sql` is the initial, no-op
  baseline that creates no domain tables).

### Starting PostgreSQL locally

A ready-to-use PostgreSQL 16 service is defined in the repository root
`docker-compose.yml` with matching credentials. From the repository root:

```bash
docker compose up -d postgres
```

This starts PostgreSQL on `localhost:5432` with database `moneypilot`, user
`moneypilot`, and password `moneypilot_local`.

## How to run the application

From the `backend/` directory.

On macOS/Linux:

```bash
./mvnw spring-boot:run
```

On Windows (cmd / PowerShell):

```bat
mvnw.cmd spring-boot:run
```

The application starts on **http://localhost:8080**.

To build a runnable jar instead:

```bash
./mvnw clean package
java -jar target/moneypilot-backend-0.0.1-SNAPSHOT.jar
```

> Note: the application requires a reachable PostgreSQL instance at startup
> because Flyway runs migrations and Hibernate validates the schema.

## How to run tests

From the `backend/` directory.

On macOS/Linux:

```bash
./mvnw clean test
```

On Windows (cmd / PowerShell):

```bat
mvnw.cmd clean test
```

Tests included:

- `HealthControllerTest` — a web slice test for the health endpoint (no
  database required).
- `MoneyPilotApplicationTests` — a full application context test backed by a
  Testcontainers PostgreSQL 16 instance (**Docker must be running**).

## API

Base URL (local): **http://localhost:8080/api/v1**

### Health check

```
GET /api/v1/health
```

Example response:

```json
{
  "success": true,
  "data": {
    "application": "MoneyPilot",
    "status": "UP"
  },
  "error": null
}
```

Spring Boot Actuator is also enabled, exposing `GET /actuator/health`.

## Package structure

Base package: `com.moneypilot`

```
com.moneypilot
├── common        (foundation: ApiResponse, HealthController, SecurityConfig)
├── auth          (reserved)
├── user          (reserved)
├── dashboard     (reserved)
├── loan          (reserved)
├── creditcard    (reserved)
├── expense       (reserved)
├── payment       (reserved)
├── category      (reserved)
└── notification  (reserved)
```

Reserved modules are intentionally empty placeholders for future features.
