-- Initialize PostgreSQL extensions for Ticketing App
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS vector;

-- Output confirmation
DO $$
BEGIN
    RAISE NOTICE 'Ticketing DB initialized with pgvector and uuid-ossp extensions.';
END $$;
