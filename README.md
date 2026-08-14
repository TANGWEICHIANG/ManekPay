# ManekPay

ManekPay is an all-in-one fintech super-app platform — multi-currency wallets, cross-border transfers, and behavioral savings tools — built as an enterprise-grade portfolio project. It's inspired by neobanks like Revolut, with double-entry bookkeeping precision and an event-driven microservice architecture at its core.

Six independent Spring Boot services back a single React frontend, fronted by an nginx gateway:

| Service | Codename | Port | Responsibility |
|---|---|---|---|
| `auth-service` | — | 8086 | Registration, login, JWT issuance, e-KYC verification |
| `ledger-service` | manek-ledger-service | 8081 | Accounts, multi-currency wallets, transfers, double-entry ledger |
| `fx-service` | selat-fx-service | 8082 | Live FX rates, conversion |
| `vaults-service` | kupang-vaults-service | 8083 | Spare-change round-up savings vaults |
| `risk-service` | beadguard-risk-service | 8084 | Fraud/risk checks |
| `wealth-service` | manek-wealth-service | 8085 | Wealth/investment features |

## Tech Stack

- **Backend:** Java 21, Spring Boot 3.3.5, Spring Security (JWT/OAuth2 resource server), Spring Data JPA, Flyway, Maven multi-module reactor
- **Frontend:** React 19, Vite, TypeScript, Zustand, TanStack Query, React Router
- **Data & messaging:** PostgreSQL (`NUMERIC(18,4)` for money, never floats), Apache Kafka, Redis
- **Gateway:** nginx
- **Containers:** Docker / Docker Compose

See [`PLANNING.md`](PLANNING.md) for the full business requirements and architecture rationale.

## Prerequisites

- Java 21 (JDK)
- Maven
- Node.js + npm
- Docker Desktop (or another Docker engine)

## Running Locally (from a fresh clone)

1. **Clone and start infra** (Postgres, Redis, Kafka, nginx gateway):
   ```bash
   git clone <repo-url>
   cd ManekPay
   docker compose up -d postgres redis kafka nginx
   ```
2. **Start each backend service** you need, in its own terminal (each is an independent Spring Boot app on the port listed above):
   ```bash
   mvn -pl backend/auth-service spring-boot:run
   mvn -pl backend/ledger-service spring-boot:run
   mvn -pl backend/vaults-service spring-boot:run
   # ...and so on for fx-service, risk-service, wealth-service as needed
   ```
   Each service runs its own Flyway migrations against the shared `manekpay` Postgres database on startup.
3. **Start the frontend dev server:**
   ```bash
   cd frontend
   npm install
   npm run serve
   ```
4. **Open the app:** http://localhost:8080 — nginx routes `/` to the Vite dev server and `/api/<service>/*` to the matching backend service.

To run the whole backend test suite: `mvn test` from the repo root (DB/Kafka integration tests that need Testcontainers require a working Docker environment).

## API Documentation

Each service exposes interactive Swagger UI directly on its own port (not routed through the nginx gateway): `http://localhost:<port>/swagger-ui.html`, e.g. `http://localhost:8081/swagger-ui.html` for `ledger-service`. Use the "Authorize" button to attach a JWT and try protected endpoints directly from the docs.

## Running in Dev

The `dev` mode targets a shared, longer-lived environment rather than your own machine, using the same services but built as containers instead of run via `mvn spring-boot:run`:

1. Build a service's image: `docker build -t manekpay/ledger-service backend/ledger-service` (repeat per service — each has its own `Dockerfile`).
2. Point each container at the shared environment's Postgres/Redis/Kafka/`auth-service` via env vars (e.g. `AUTH_SERVICE_URL`, `KAFKA_BOOTSTRAP_SERVERS` — see each service's `application.yml` for the full list of overridable settings).
3. Build the frontend against the dev API target: `cd frontend && npm run build:dev` (uses Vite's `dev` mode — add a `.env.dev` in `frontend/` to override the API base URL and any other dev-specific config as the app grows).

## Running in Prod

Production follows the same containerized shape as dev, with production credentials/URLs and `prod` build modes:

1. Build each backend service's image from its `Dockerfile` (as above) and run it with production datasource/Kafka/Redis/JWT configuration supplied via environment variables — never commit production secrets.
2. Build the frontend for production: `cd frontend && npm run build:prod`, then serve the resulting `dist/` output (e.g. behind nginx) instead of the Vite dev server.
3. Point nginx (or your production gateway) at the deployed service addresses instead of `host.docker.internal`.

There's no managed CI/CD or cloud deployment pipeline yet — this section documents the current, manual containerized path. As that tooling is added, this section will be updated to match.

## More Detail

Design docs and implementation plans for each phase live under `docs/superpowers/` locally (gitignored — not part of this repo, kept for local reference during development).
