-- Utang MVP — initial schema
-- Ledger-first design. Balances are denormalized onto customers for fast reads
-- and updated atomically alongside ledger inserts.

CREATE TABLE stores (
    id           BIGSERIAL PRIMARY KEY,
    phone_number VARCHAR(32)  NOT NULL UNIQUE,
    name         VARCHAR(120) NOT NULL DEFAULT 'My Store',
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- One-time passwords for phone-based auth.
CREATE TABLE otp_codes (
    id           BIGSERIAL PRIMARY KEY,
    phone_number VARCHAR(32)  NOT NULL,
    code         VARCHAR(6)   NOT NULL,
    expires_at   TIMESTAMPTZ  NOT NULL,
    consumed     BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX idx_otp_phone ON otp_codes (phone_number);

CREATE TABLE customers (
    id              BIGSERIAL PRIMARY KEY,
    store_id        BIGINT        NOT NULL REFERENCES stores (id),
    name            VARCHAR(120)  NOT NULL,
    phone_number    VARCHAR(32),
    current_balance NUMERIC(12,2) NOT NULL DEFAULT 0,
    pay_token       VARCHAR(64)   NOT NULL UNIQUE,
    created_at      TIMESTAMPTZ   NOT NULL DEFAULT now()
);
CREATE INDEX idx_customers_store ON customers (store_id);

CREATE TABLE ledger_entries (
    id          BIGSERIAL PRIMARY KEY,
    customer_id BIGINT        NOT NULL REFERENCES customers (id),
    type        VARCHAR(8)    NOT NULL CHECK (type IN ('DEBIT', 'CREDIT')),
    amount      NUMERIC(12,2) NOT NULL CHECK (amount > 0),
    note        VARCHAR(255),
    created_at  TIMESTAMPTZ   NOT NULL DEFAULT now()
);
CREATE INDEX idx_ledger_customer ON ledger_entries (customer_id, created_at DESC);

CREATE TABLE payments (
    id              BIGSERIAL PRIMARY KEY,
    customer_id     BIGINT        NOT NULL REFERENCES customers (id),
    amount          NUMERIC(12,2) NOT NULL CHECK (amount > 0),
    method          VARCHAR(8)    NOT NULL CHECK (method IN ('CASH', 'LINK')),
    provider        VARCHAR(32),
    provider_ref_id VARCHAR(128),
    status          VARCHAR(16)   NOT NULL DEFAULT 'PAID',
    created_at      TIMESTAMPTZ   NOT NULL DEFAULT now()
);
-- Idempotency: a provider reference can only be recorded once.
CREATE UNIQUE INDEX uq_payment_provider_ref
    ON payments (provider, provider_ref_id)
    WHERE provider IS NOT NULL AND provider_ref_id IS NOT NULL;

CREATE TABLE reminder_logs (
    id          BIGSERIAL PRIMARY KEY,
    customer_id BIGINT      NOT NULL REFERENCES customers (id),
    method      VARCHAR(16) NOT NULL DEFAULT 'manual',
    channel     VARCHAR(16) NOT NULL DEFAULT 'copy',
    sent_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    sent_on     DATE        NOT NULL
);
-- Lock rule: max 1 manual reminder per customer per day.
CREATE UNIQUE INDEX uq_reminder_customer_day ON reminder_logs (customer_id, sent_on);

CREATE TABLE webhook_events (
    id                BIGSERIAL PRIMARY KEY,
    provider          VARCHAR(32)  NOT NULL,
    external_event_id VARCHAR(128) NOT NULL,
    payload           TEXT         NOT NULL,
    processed         BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT now()
);
-- Idempotency: each provider event id is stored once.
CREATE UNIQUE INDEX uq_webhook_provider_event ON webhook_events (provider, external_event_id);

-- Links a PayMongo payment link/reference to the customer it was created for,
-- so the webhook can resolve the customer from the reference id.
CREATE TABLE payment_links (
    id           BIGSERIAL PRIMARY KEY,
    customer_id  BIGINT        NOT NULL REFERENCES customers (id),
    provider     VARCHAR(32)   NOT NULL DEFAULT 'paymongo',
    reference_id VARCHAR(128)  NOT NULL,
    amount       NUMERIC(12,2) NOT NULL,
    checkout_url VARCHAR(512)  NOT NULL,
    created_at   TIMESTAMPTZ   NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX uq_payment_link_ref ON payment_links (provider, reference_id);
CREATE INDEX idx_payment_link_customer ON payment_links (customer_id);
