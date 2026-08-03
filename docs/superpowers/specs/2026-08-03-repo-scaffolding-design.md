# ManekPay Repo Scaffolding — Design Spec

**Date:** 2026-08-03
**Status:** Approved
**Scope:** Sub-project 0 of the ManekPay build — monorepo + infra scaffolding only. No business logic.

## Context

ManekPay is a multi-service fintech platform (see `PLANNING.md`) with five backend
microservices, one React frontend, and shared infra (Postgres, Redis, Kafka). The
full platform is too large for a single design/plan cycle, so it's decomposed into
phases matching the existing roadmap. This spec covers the **first** phase: standing
up the repo shape and local dev infra so later phases (starting with the core ledger
service) build on working scaffolding instead of blank folders.

Explicitly out of scope for this phase: any controller/entity/business logic, DB
schema or Flyway migrations, authentication, real Kafka topics/consumers, CI
pipelines. Those belong to later phases (Phase 1: Core Ledger, etc.).

## Repo Layout

```
ManekPay/
├── docker-compose.yml
├── pom.xml                          # Maven parent (packaging=pom), Java 21, Spring Boot 3.x BOM
├── services/
│   ├── ledger-service/
│   ├── fx-service/
│   ├── vaults-service/
│   ├── risk-service/
│   └── wealth-service/
├── frontend/                        # single React app (not one frontend per service)
├── gateway/
│   └── nginx.conf
└── PLANNING.md
```

Each service module:
- `pom.xml` inheriting the root parent POM (shared dependency versions).
- One `@SpringBootApplication` main class, package `com.manekpay.<service>`.
- `application.yml` with the service's own port (see Ports table).
- Spring Boot Actuator enabled so `/actuator/health` works immediately.
- `Dockerfile` — multi-stage build (Maven build stage → slim JRE runtime stage).

No controllers, entities, repositories, or dependencies beyond
`spring-boot-starter-web` + `spring-boot-starter-actuator` at this stage.

## Ports

| Service | Port |
|---|---|
| ledger-service | 8081 |
| fx-service | 8082 |
| vaults-service | 8083 |
| risk-service | 8084 |
| wealth-service | 8085 |
| nginx gateway | 8080 |
| frontend (Vite dev server) | 5173 |
| postgres | 5432 |
| redis | 6379 |
| kafka (KRaft) | 9092 |

## Infra (docker-compose.yml)

- **postgres**: single instance. One shared database/schema for all services (tables
  namespaced per service, e.g. `ledger_accounts`, `fx_rates`) — not
  database-per-service. Schema/migrations are added when each service's business
  logic phase happens (Flyway, starting with the ledger service).
- **redis**: cache, idempotency key storage, distributed locking.
- **kafka**: KRaft mode (no Zookeeper container) — single-container event bus. No
  topics created yet; topic creation happens when producers/consumers are built.
- **nginx**: the API gateway. Path-based reverse proxy:
  - `/` → frontend (or the built static frontend, once it exists)
  - `/api/ledger/*` → `ledger-service:8081`
  - `/api/fx/*` → `fx-service:8082`
  - `/api/vaults/*` → `vaults-service:8083`
  - `/api/risk/*` → `risk-service:8084`
  - `/api/wealth/*` → `wealth-service:8085`

  Chosen over a Spring Cloud Gateway module to avoid running a 6th JVM service for
  what a single `nginx.conf` handles. Revisit only if gateway-level auth/routing
  logic outgrows what nginx config can express.

During early dev, services are expected to run locally via `mvn spring-boot:run`
against the dockerized postgres/redis/kafka rather than being rebuilt into
containers on every change; the per-service Dockerfiles exist so `docker compose up`
becomes viable as each service matures.

## Frontend

Vite + React 19 + TypeScript + Tailwind CSS skeleton. One app for the whole
platform (not split per backend service) — it talks to services through the nginx
gateway using `/api/<service>/...` paths, so no CORS config or per-service base
URLs are needed in the frontend.

## Out of Scope / Deferred

- DB schema, Flyway migrations — deferred to Phase 1 (Core Ledger).
- Kafka topics, producers, consumers — deferred to whichever phase introduces them
  (`transaction.created` in Phase 1/2, `fx.converted` in Phase 2, etc.).
- Auth (Spring Security/JWT/OAuth2) — deferred until a service needs to protect
  endpoints.
- CI/CD — not addressed by this spec.
- Database-per-service — explicitly rejected in favor of one shared schema, per
  project decision.

## Success Criteria

- `docker compose up` brings up postgres, redis, kafka (KRaft), and nginx cleanly.
- Each of the 5 service modules builds (`mvn -pl services/<name> spring-boot:run`)
  and responds on `/actuator/health`.
- Frontend skeleton runs (`npm run dev`) and loads a blank page.
- nginx routes `/` to the frontend and `/api/<service>/*` to the right backend port
  (verifiable once at least one service exposes a trivial endpoint, or via
  `/actuator/health` proxying).
