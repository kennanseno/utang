# Utang

Mobile-first web app for Philippine micro-businesses (sari-sari stores) to track customer debt ("utang"), record cash payments, and send copyable reminder messages.

MVP v1 — see [docs/PRD.md](docs/PRD.md) for the full product spec.

## Monorepo layout

```
utang/
├── frontend/   # Next.js (App Router) — mobile-first UI
├── backend/    # Spring Boot (Java) — REST API, OpenAPI-first
├── docs/       # PRD & specs
└── docker-compose.yml  # local PostgreSQL
```

## Tech stack

| Layer     | Choice                          |
|-----------|---------------------------------|
| Frontend  | Next.js (App Router), TypeScript |
| Backend   | Spring Boot 3 (Java 17), REST   |
| Database  | PostgreSQL 16                   |

## Quick start

### Option A — No Docker, no Postgres (fastest)

Run the backend on an in-memory H2 database using the `dev` profile:

```bash
cd backend
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

Then start the frontend:

```bash
cd frontend
cp .env.local.example .env.local
npm install
npm run dev
```

The API runs on `http://localhost:8080` and the app on `http://localhost:3000`.
Data resets on each restart. Browse the dev DB at `http://localhost:8080/h2-console`
(JDBC URL `jdbc:h2:mem:utang`, user `sa`, no password).

### Option B — PostgreSQL

Use a real Postgres (Flyway migrations run on startup). Start it either with Docker
or a local install:

```bash
# With Docker:
docker compose up -d

# Or Homebrew (no Docker):
brew install postgresql@16
brew services start postgresql@16
createuser -s utang 2>/dev/null; createdb -O utang utang
psql -d utang -c "ALTER USER utang WITH PASSWORD 'utang';"
```

Then run the backend (default profile connects to Postgres on `localhost:5432`):

```bash
cd backend
./mvnw spring-boot:run
```

OpenAPI spec lives at [backend/src/main/resources/openapi/openapi.yaml](backend/src/main/resources/openapi/openapi.yaml).

## API docs (Swagger UI)

With the backend running, interactive docs are served from the spec-first
`openapi.yaml`:

- Swagger UI: `http://localhost:8080/swagger-ui.html`
- Raw spec: `http://localhost:8080/openapi.yaml`

## Core domain

- **Customer** — a store's suki, has a running `current_balance`.
- **LedgerEntry** — `DEBIT` (utang) increases balance, `CREDIT` (bayad) decreases it. Ledger is the source of truth.
- **ReminderLog** — max 1 manual reminder per customer per day.

## Business rules

```
balance = sum(debits) - sum(credits)
```

Balance updates are atomic. Reminders are locked to once per customer per day.

## Scope

Built: OTP auth, customers, ledger, copy-reminder flow, cash payments, public pay page with the store's QR code and a "paid" notification to the owner.

**Not built (out of scope):** auto-reminders, online payment gateways/PSPs, SMS API, customer login, editing/deleting ledger entries, dashboards/analytics, event queues.
