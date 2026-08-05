# ManekPay

All in one fintech web application

## Running locally

1. Start infra: `docker compose up -d postgres redis kafka nginx`
2. Start a backend service (repeat per service, each on its own port — see `docs/superpowers/specs/2026-08-03-repo-scaffolding-design.md` for the full port table):
   `mvn -pl backend/ledger-service spring-boot:run`
3. Start the frontend: `cd frontend && npm install && npm run dev`
4. Open http://localhost:8080 — routed through the nginx gateway to the frontend and, via `/api/<service>/*`, to each backend service.

See `docs/superpowers/specs/2026-08-03-repo-scaffolding-design.md` and `docs/superpowers/plans/2026-08-03-repo-scaffolding.md` for the full design and implementation history.
