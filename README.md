# Utang

Mobile-first web app for Philippine micro-businesses (sari-sari stores) to track customer debt ("utang"), record cash payments, send copyable reminder messages, and accept payments via PayMongo links.

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
| Payments  | PayMongo (adapter pattern)      |

## Quick start

### 1. Start PostgreSQL

```bash
docker compose up -d
```

### 2. Run the backend

```bash
cd backend
./mvnw spring-boot:run
```

API runs on `http://localhost:8080`. OpenAPI spec lives at [backend/src/main/resources/openapi/openapi.yaml](backend/src/main/resources/openapi/openapi.yaml).

### 3. Run the frontend

```bash
cd frontend
cp .env.local.example .env.local
npm install
npm run dev
```

App runs on `http://localhost:3000`.

## Core domain

- **Customer** — a store's suki, has a running `current_balance`.
- **LedgerEntry** — `DEBIT` (utang) increases balance, `CREDIT` (bayad) decreases it. Ledger is the source of truth.
- **Payment** — `CASH` (manual credit) or `LINK` (PayMongo webhook credit). Idempotent via `UNIQUE(provider, provider_ref_id)`.
- **ReminderLog** — max 1 manual reminder per customer per day.
- **WebhookEvent** — idempotent via `UNIQUE(provider, external_event_id)`.

## Business rules

```
balance = sum(debits) - sum(credits)
```

Balance updates are atomic. Reminders are locked to once per customer per day. Payments and webhooks are idempotent.

## Scope

Built: OTP auth, customers, ledger, copy-reminder flow, PayMongo payment link, webhook credit.

**Not built (out of scope):** auto-reminders, QR Ph, SMS API, customer login, editing/deleting ledger entries, dashboards/analytics, multiple PSPs, event queues.
