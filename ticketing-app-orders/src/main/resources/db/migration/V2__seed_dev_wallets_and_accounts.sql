-- V2__seed_dev_wallets_and_accounts.sql: Deterministic seeding for development and E2E tests
SET search_path = orders;

-- 1. Master Platform Ledger Accounts (com.aieta.pocket)
INSERT INTO ledger_accounts (id, owner_id, account_type, balance, currency)
VALUES
    ('a0000000-0000-0000-0000-000000000001', 'PLATFORM', 'ASSET', 0.00, 'EUR'),
    ('a0000000-0000-0000-0000-000000000002', 'PLATFORM', 'LIABILITY', 0.00, 'EUR'),
    ('a0000000-0000-0000-0000-000000000003', 'PLATFORM', 'REVENUE', 0.00, 'EUR')
ON CONFLICT (id) DO NOTHING;

-- 2. Pre-funded virtual wallets for local development and E2E scenarios
INSERT INTO pocket_wallets (id, user_id, balance, currency)
VALUES
    ('b0000000-0000-0000-0000-000000000001', 'user-test-001', 250.00, 'EUR'),
    ('b0000000-0000-0000-0000-000000000002', 'e2e-user-011',  500.00, 'EUR'),
    ('b0000000-0000-0000-0000-000000000003', 'e2e-user-012',  100.00, 'EUR')
ON CONFLICT (user_id) DO UPDATE SET
    balance = EXCLUDED.balance,
    updated_at = now();
