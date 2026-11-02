-- V2__seed_dev_wallets_and_accounts.sql: Deterministic seeding for development and E2E tests
SET search_path = orders;

-- Master Platform Ledger Accounts (com.aieta.pocket)
INSERT INTO ledger_accounts (id, owner_id, account_type, balance, currency)
VALUES
    ('a0000000-0000-0000-0000-000000000001', 'PLATFORM', 'ASSET', 0.00, 'EUR'),
    ('a0000000-0000-0000-0000-000000000002', 'PLATFORM', 'LIABILITY', 0.00, 'EUR'),
    ('a0000000-0000-0000-0000-000000000003', 'PLATFORM', 'REVENUE', 0.00, 'EUR')
ON CONFLICT (id) DO NOTHING;
