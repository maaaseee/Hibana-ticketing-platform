-- V3__remove_pocket_wallets_add_discount_vouchers.sql
-- Remove internal wallet storage and establish compensation discount vouchers
SET search_path = orders;

-- 1. Drop internal user pocket wallets
DROP TABLE IF EXISTS pocket_wallets CASCADE;

-- 2. Discount Vouchers for compensation, refunds and marketing promotions
CREATE TABLE IF NOT EXISTS discount_vouchers (
    id              UUID            PRIMARY KEY,
    code            VARCHAR(40)     NOT NULL UNIQUE,
    user_id         VARCHAR(60)     NOT NULL,
    discount_type   VARCHAR(20)     NOT NULL, -- 'PERCENTAGE', 'FIXED_AMOUNT'
    amount          NUMERIC(10,2)   NOT NULL CHECK (amount > 0),
    min_purchase    NUMERIC(10,2)   DEFAULT 0.00,
    currency        VARCHAR(3)      NOT NULL DEFAULT 'ARS',
    is_used         BOOLEAN         NOT NULL DEFAULT FALSE,
    used_at         TIMESTAMPTZ,
    expires_at      TIMESTAMPTZ     NOT NULL,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_vouchers_user_id ON discount_vouchers (user_id);
CREATE INDEX IF NOT EXISTS idx_vouchers_code ON discount_vouchers (code);
