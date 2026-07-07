-- Utang V1 consolidated schema (single baseline migration).
-- This replaces the previous multi-file Flyway history with one clear deploy script.

CREATE TABLE stores (
    id                   BIGSERIAL PRIMARY KEY,
    phone_number         VARCHAR(32)  NOT NULL UNIQUE,
    username             VARCHAR(60) UNIQUE,
    password_hash        VARCHAR(100),
    email                VARCHAR(255) UNIQUE,
    name                 VARCHAR(120) NOT NULL DEFAULT 'My Store',
    owner_name           VARCHAR(120),
    onboarded            BOOLEAN      NOT NULL DEFAULT FALSE,
    qr_code_image        BYTEA,
    qr_code_content_type VARCHAR(100),
    created_at           TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX uq_stores_name_lower ON stores (LOWER(name));

CREATE TABLE customers (
    id              BIGSERIAL PRIMARY KEY,
    store_id        BIGINT        NOT NULL REFERENCES stores (id),
    name            VARCHAR(120)  NOT NULL,
    phone_number    VARCHAR(32)   NOT NULL,
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
