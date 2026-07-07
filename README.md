# Utang

Mobile-first web app for Philippine micro-businesses (sari-sari stores) to track customer debt ("utang"), record cash payments, and send copyable reminder messages.

MVP v1 — see [docs/PRD.md](docs/PRD.md) for the full product spec.

## Monorepo layout

```
utang/
├── app/        # Next.js (App Router) pages and API routes
├── lib/        # shared frontend/server modules
├── docs/       # PRD & specs
└── docker-compose.yml  # local PostgreSQL
```

## Tech stack

| Layer     | Choice                          |
|-----------|---------------------------------|
| Frontend  | Next.js (App Router), TypeScript |
| Backend   | Next.js API routes (Node.js runtime) |
| Database  | PostgreSQL 16                   |

## Quick start

### Option A — Single app (Next.js full-stack)

Run only the Next.js app at repository root; it serves both UI and API routes under `/api`.

```bash
cp .env.local.example .env.local
# Ensure DB_URL or DATABASE_URL points to your Postgres instance.
npm install
npm run dev
```

By default the API client calls `/api/*` in the same app.

### Option B — PostgreSQL via Docker

Start Postgres with Docker:

```bash
docker compose up -d
```

Then run the app:

```bash
cp .env.local.example .env.local
npm install
npm run dev
```

## Core domain

- **Customer** — a store's suki, has a running `current_balance`.
- **LedgerEntry** — `DEBIT` (utang) increases balance, `CREDIT` (bayad) decreases it. Ledger is the source of truth.

## Business rules

```
balance = sum(debits) - sum(credits)
```

Balance updates are atomic.

## Database bootstrap (V1)

Use the consolidated baseline schema at:

`db/migration/V1__init.sql`

## Scope

Built: username/password auth, store email capture for future communication, customers, ledger, copy-reminder flow, cash payments, public pay page with the store's QR code and a "paid" notification to the owner.

**Not built (out of scope):** auto-reminders, online payment gateways/PSPs, SMS API, customer login, editing/deleting ledger entries, dashboards/analytics, event queues.

The repo sets npm to verbose logging by default via `.npmrc`.
