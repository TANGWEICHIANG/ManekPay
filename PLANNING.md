# PLANNING.md — ManekPay Engine

## 1. Project Overview
**ManekPay** is a multi-currency fintech super app platform inspired by the global capabilities of Revolut and the historical trade legacy of Melaka. Built as an enterprise-grade portfolio project, ManekPay showcases microservice patterns, double-entry bookkeeping precision, event-driven architecture, and cross-border currency conversion.

---

## 2. Technology Stack

### Backend
* **Language & Framework:** Java 21 + Spring Boot 3.x
* **Build Tool:** Maven / Gradle
* **Security:** Spring Security + JWT (JSON Web Tokens) + OAuth2
* **Persistence & ORM:** Spring Data JPA / Hibernate, Flyway / Liquibase (Schema Migrations)

### Frontend
* **Core Framework:** React 18+ (Vite / Next.js)
* **State Management:** Zustand or Redux Toolkit
* **Styling & Components:** Tailwind CSS + Shadcn UI / Material UI
* **Charts & Visualizations:** Recharts / Financial Lightweight Charts

### Data & Messaging Infrastructure
* **Primary Relational Database:** PostgreSQL (Using `NUMERIC/DECIMAL` types for zero rounding error)
* **Message Broker / Event Bus:** Apache Kafka or RabbitMQ (Event-driven asynchronous processing)
* **Caching & Locking:** Redis (Distributed locking for idempotency, session storage, and rate-limiting)

### DevOps & Tooling
* **Containerization:** Docker & Docker Compose
* **API Documentation:** OpenAPI 3.0 / Swagger UI
* **Testing:** JUnit 5, Mockito, Testcontainers (for real PostgreSQL & Kafka testing)

---

## 3. High-Level Architecture

---

## 4. Business Requirements Specification (BRS)

### Module 1: Core Ledger & Accounts (`manek-ledger-service`)
> **Goal:** High-precision, double-entry financial ledger managing balances and account-to-account transfers.

* **BR-1.1: Double-Entry Bookkeeping:** Every financial transaction must consist of balanced debit and credit entries. Standard floating-point math is strictly forbidden; Java's `BigDecimal` and SQL `NUMERIC(18, 4)` must be used.
* **BR-1.2: Multi-Currency Wallets:** Support primary balances in MYR, SGD, USD, EUR, and GBP.
* **BR-1.3: Idempotency Control:** All transactional endpoints must enforce an `X-Idempotency-Key` header cached in Redis to prevent duplicate operations from network retries.
* **BR-1.4: Malaysian Proxy Transfers:** Allow funds transfer using standard account numbers or proxy lookup (e.g., MyKad/IC format, Mobile Numbers).

---

### Module 2: Cross-Border FX Engine (`selat-fx-service`)
> **Goal:** Facilitate real-time FX rate generation, conversion locking, and automated multi-currency debit settlement.

* **BR-2.1: Live Exchange Rate Ingestion:** Poll or stream live FX rates and update Redis cache every 10 seconds.
* **BR-2.2: Rate Lock-in Window:** Allow users to execute currency conversions with guaranteed rates locked for 15 seconds.
* **BR-2.3: Multi-Currency Auto-Conversion:** If a card transaction occurs in SGD and the SGD wallet has insufficient funds, automatically execute a real-time FX swap from MYR at checkout.

---

### Module 3: Savings Vaults (`kupang-vaults-service`)
> **Goal:** Behavioral savings tools based on real-time transaction events.

* **BR-3.1: Spare Change Round-ups:** Consume `transaction.created` events from Kafka/RabbitMQ, calculate spare-change roundups to the nearest integer, and transfer the delta into a designated vault.
* **BR-3.2: Goal Vaults & Recurring Sweeps:** Allow users to define target savings amounts with scheduled recurring automated deposits.

---

### Module 4: Fraud & Risk Engine (`beadguard-risk-service`)
> **Goal:** Real-time transaction scoring and risk control.

* **BR-4.1: Velocity Rules Engine:** Detect and block accounts executing more than 5 high-value transactions within 60 seconds.
* **BR-4.2: Location Anomaly Detection:** Flag transactions occurring in conflicting geographic locations within impossible travel timeframes.
* **BR-4.3: High-Risk Alerts:** Publish `transaction.flagged` events to Kafka/RabbitMQ to trigger immediate account restrictions.

---

### Module 5: Fractional Wealth & Trading (`manek-wealth-service`)
> **Goal:** Allow fractional stock purchases and portfolio monitoring.

* **BR-5.1: Fractional Trading:** Support purchasing fractions of high-value equities down to 4 decimal places using internal ledger pools.
* **BR-5.2: Shariah-Compliance Tagging:** Provide filter flags (`is_shariah_compliant`) on asset catalogs based on Islamic finance screening criteria.

---

## 5. Non-Functional Requirements (NFRs)

| Category | Requirement Standard |
| :--- | :--- |
| **Financial Accuracy** | Zero rounding drift. All math executed via `BigDecimal.ROUND_HALF_EVEN` (Banker's Rounding). |
| **Data Integrity** | Isolation level `SERIALIZABLE` or pessimistic locking (`SELECT ... FOR UPDATE`) on balance updates. |
| **Auditability** | Ledger tables are strictly append-only. Updates and deletes are disabled on transaction tables. |
| **Resilience** | Failed message consumption must be routed to Dead Letter Queues (DLQ) in Kafka/RabbitMQ. |

---

## 6. Implementation Roadmap

### Phase 1: Foundation & Core Banking (Weeks 1–2)
- [ ] Set up Docker Compose for PostgreSQL, Redis, and Kafka/RabbitMQ.
- [ ] Implement `manek-ledger-service` with `BigDecimal` double-entry ledger database schema.
- [ ] Implement API Idempotency filter using Redis.
- [ ] Build unit tests covering debit/credit equilibrium.

### Phase 2: FX Engine & Event Integration (Weeks 3–4)
- [ ] Implement `selat-fx-service` with rate caching in Redis.
- [ ] Configure Kafka/RabbitMQ topics for `transaction.created` and `fx.converted`.
- [ ] Implement `kupang-vaults-service` to consume transaction events and perform round-ups.

### Phase 3: Risk Engine & Wealth Modules (Weeks 5–6)
- [ ] Implement `beadguard-risk-service` for transaction velocity checks.
- [ ] Build `manek-wealth-service` for fractional stock allocation.
- [ ] Implement WebSockets for live trading and rate ticker updates.

### Phase 4: Frontend Integration & Final Polish (Weeks 7–8)
- [ ] Build React UI dashboard featuring balance toggles, analytics, and stock charts.
- [ ] Add Swagger/OpenAPI documentation.
- [ ] Write system architecture breakdown and GitHub README.
