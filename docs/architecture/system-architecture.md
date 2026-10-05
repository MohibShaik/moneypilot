# MoneyPilot System Architecture

## 1. Purpose

MoneyPilot is a personal finance management application designed to help users
track loans, EMIs, credit cards, expenses, payments, and financial summaries.

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

---

# 2. Architecture Style

MoneyPilot uses a:

**Modular Monolith Architecture**

The backend is a single Spring Boot application containing independently
organized business modules.

Microservices are explicitly out of scope for the initial product.

## Reason

The initial system does not require:

- independent service deployment
- independent scaling
- distributed transactions
- service-to-service communication
- service discovery
- distributed messaging

A modular monolith provides strong domain separation without introducing
unnecessary distributed-system complexity.

---

# 3. High-Level Architecture

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