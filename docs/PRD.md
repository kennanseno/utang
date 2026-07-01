# Utang — Product Requirements & Build Spec

Product: **Utang** · Version: **MVP v1** · Author: Kennan Seno · Date: 2026-07-01

Mobile-first web app for Philippine micro-businesses (sari-sari stores) to track customer
debt ("utang") and collect payments via SMS-copyable reminders and PayMongo links.

## Core value proposition

> "Track utang easily and get paid faster—without awkward conversations."

## Domain model

| Entity        | Key fields |
|---------------|-----------|
| Customer      | id, store_id, name, phone_number (optional), current_balance |
| LedgerEntry   | id, customer_id, type (DEBIT/CREDIT), amount, note, created_at |
| Payment       | id, customer_id, amount, method (CASH/LINK), provider, provider_ref_id (unique), status |
| ReminderLog   | id, customer_id, sent_at, method (manual), channel (copy/sms) |
| WebhookEvent  | id, provider, external_event_id (unique), payload, processed |

## Business rules

- `balance = sum(debits) - sum(credits)`
- DEBIT increases balance; CREDIT decreases balance.
- Balance updates are atomic.
- Cash → manual CREDIT. PayMongo → webhook → CREDIT.
- Idempotency: Payments `UNIQUE(provider, provider_ref_id)`; Webhooks `UNIQUE(provider, external_event_id)`.
- Max **1 manual reminder per customer per day** (copy counts as a reminder).

## API contract (MVP)

| Method | Path | Purpose |
|--------|------|---------|
| POST | `/auth/request-otp` | Request OTP for a phone number |
| POST | `/auth/verify-otp` | Verify OTP, create account, return token |
| GET  | `/me` | Current store/user |
| POST | `/customers` | Add customer (name required) |
| GET  | `/customers` | List customers |
| GET  | `/customers/{id}` | Customer detail |
| POST | `/ledger/debit` | Add utang |
| POST | `/ledger/credit` | Record payment/credit |
| GET  | `/customers/{id}/ledger` | Ledger history |
| POST | `/payments/link` | Create PayMongo payment link |
| GET  | `/customers/{id}/reminder-preview` | Preview reminder message |
| POST | `/customers/{id}/remind` | Log reminder (once/day) |
| GET  | `/public/pay/{token}` | Public payment page data |
| POST | `/webhooks/paymongo` | PayMongo webhook receiver |

## Reminder message format

```
Hi {name}! May utang ka na ₱{amount} sa {store}.
Pwede ka magbayad dito: {link}
```

## Build order

1. **Phase 1** — Auth, Customers, Ledger
2. **Phase 2** — Reminder system (copy flow)
3. **Phase 3** — Payment link + Webhook

## Hard exclusions (do NOT build)

Auto-reminders · QR payments · SMS API integration · Customer login · Editing/deleting
ledger entries · Dashboards/analytics · Multiple PSPs · Complex retry queues.

## Acceptance criteria

A user can: sign up via OTP · add a customer · add utang · record cash payment · see correct
balance · copy reminder message · send payment link · receive webhook payment.
