-- V1__create_orders_schema.sql: Initial DDL schema for Orders & Financial Engine
CREATE SCHEMA IF NOT EXISTS orders;
SET search_path = orders;

-- 1. Orders table
CREATE TABLE IF NOT EXISTS orders (
    id              UUID            PRIMARY KEY,
    user_id         VARCHAR(60)     NOT NULL,
    reservation_id  VARCHAR(60)     NOT NULL,
    event_id        VARCHAR(60)     NOT NULL,
    seat_id         VARCHAR(60)     NOT NULL,
    total_amount    NUMERIC(10,2)   NOT NULL CHECK (total_amount >= 0),
    currency        VARCHAR(3)      NOT NULL DEFAULT 'EUR',
    status          VARCHAR(25)     NOT NULL DEFAULT 'PENDING_PAYMENT',
    payment_method  VARCHAR(25),
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_orders_user_id ON orders (user_id);
CREATE INDEX IF NOT EXISTS idx_orders_status ON orders (status);
CREATE INDEX IF NOT EXISTS idx_orders_reservation ON orders (reservation_id);

-- 2. Order Items table
CREATE TABLE IF NOT EXISTS order_items (
    id          UUID            PRIMARY KEY,
    order_id    UUID            NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    event_id    VARCHAR(60)     NOT NULL,
    seat_id     VARCHAR(60)     NOT NULL,
    sector_id   VARCHAR(60),
    unit_price  NUMERIC(10,2)   NOT NULL CHECK (unit_price >= 0),
    quantity    INT             NOT NULL DEFAULT 1,
    description TEXT
);

CREATE INDEX IF NOT EXISTS idx_order_items_order_id ON order_items (order_id);

-- 3. Pocket Wallets (Virtual Wallets)
CREATE TABLE IF NOT EXISTS pocket_wallets (
    id          UUID            PRIMARY KEY,
    user_id     VARCHAR(60)     NOT NULL UNIQUE,
    balance     NUMERIC(10,2)   NOT NULL DEFAULT 0.00 CHECK (balance >= 0),
    currency    VARCHAR(3)      NOT NULL DEFAULT 'EUR',
    created_at  TIMESTAMPTZ     NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ     NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_pocket_wallets_user ON pocket_wallets (user_id);

-- 4. Ledger Accounts (Double-Entry Bookkeeping - com.aieta.pocket)
CREATE TABLE IF NOT EXISTS ledger_accounts (
    id              UUID            PRIMARY KEY,
    owner_id        VARCHAR(60)     NOT NULL,
    account_type    VARCHAR(15)     NOT NULL,
    balance         NUMERIC(10,2)   NOT NULL DEFAULT 0.00,
    currency        VARCHAR(3)      NOT NULL DEFAULT 'EUR',
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_ledger_accounts_owner ON ledger_accounts (owner_id);

-- 5. Ledger Entries (Immutable Audit Trail)
CREATE TABLE IF NOT EXISTS ledger_entries (
    id              UUID            PRIMARY KEY,
    account_id      UUID            NOT NULL REFERENCES ledger_accounts(id),
    entry_type      VARCHAR(6)      NOT NULL CHECK (entry_type IN ('DEBIT', 'CREDIT')),
    amount          NUMERIC(10,2)   NOT NULL CHECK (amount > 0),
    transaction_ref UUID            NOT NULL,
    description     TEXT,
    posted_at       TIMESTAMPTZ     NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_ledger_entries_account ON ledger_entries (account_id);
CREATE INDEX IF NOT EXISTS idx_ledger_entries_tx_ref ON ledger_entries (transaction_ref);

-- 6. Transactional Outbox Events
CREATE TABLE IF NOT EXISTS outbox_events (
    id              UUID            PRIMARY KEY,
    aggregate_type  VARCHAR(50)     NOT NULL,
    aggregate_id    VARCHAR(60)     NOT NULL,
    event_type      VARCHAR(100)    NOT NULL,
    payload         JSONB           NOT NULL,
    status          VARCHAR(20)     NOT NULL DEFAULT 'PENDING',
    retry_count     INT             NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT now(),
    processed_at    TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_outbox_status ON outbox_events (status) WHERE status = 'PENDING';
