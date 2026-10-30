-- V1__init_catalog_schema.sql: Initialize complete relational schema for Catalog
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "vector";
CREATE EXTENSION IF NOT EXISTS "pg_trgm";

-- 1. Automatic Timestamp Function
CREATE OR REPLACE FUNCTION set_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- 2. Organizadores / Productoras
CREATE TABLE IF NOT EXISTS organizers (
    id VARCHAR(60) PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    legal_name VARCHAR(200),
    contact_email VARCHAR(150),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 3. Recintos / Venues
CREATE TABLE IF NOT EXISTS venues (
    id VARCHAR(60) PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    city VARCHAR(100) NOT NULL,
    country VARCHAR(100) NOT NULL,
    address VARCHAR(255) NOT NULL,
    latitude NUMERIC(9, 6) NOT NULL,
    longitude NUMERIC(9, 6) NOT NULL,
    seat_map_url VARCHAR(500),
    image_url VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 4. Eventos
CREATE TABLE IF NOT EXISTS events (
    id VARCHAR(60) PRIMARY KEY,
    organizer_id VARCHAR(60) NOT NULL REFERENCES organizers(id),
    venue_id VARCHAR(60) NOT NULL REFERENCES venues(id),
    slug VARCHAR(200) NOT NULL UNIQUE,
    title VARCHAR(200) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'DRAFT', -- DRAFT, PUBLISHED, ON_SALE, CANCELLED
    category VARCHAR(50) NOT NULL,              -- CONCERT, THEATER, SPORTS
    genres TEXT[] NOT NULL DEFAULT '{}',
    
    -- Schedule
    doors_open_at TIMESTAMPTZ NOT NULL,
    starts_at TIMESTAMPTZ NOT NULL,
    ends_at TIMESTAMPTZ NOT NULL,
    timezone VARCHAR(50) NOT NULL DEFAULT 'UTC',
    
    -- Flexible metadata
    media JSONB NOT NULL DEFAULT '{}'::jsonb,
    sales_policy JSONB NOT NULL DEFAULT '{}'::jsonb,
    
    -- Semantic Vector Embeddings (Azure OpenAI 1536 dim)
    embedding VECTOR(1536),
    
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT chk_event_schedule CHECK (doors_open_at <= starts_at AND starts_at <= ends_at)
);

-- Triggers
DROP TRIGGER IF EXISTS trg_events_updated_at ON events;
CREATE TRIGGER trg_events_updated_at
    BEFORE UPDATE ON events
    FOR EACH ROW
    EXECUTE FUNCTION set_updated_at();

-- Indexes for Events
CREATE INDEX IF NOT EXISTS idx_events_slug ON events(slug);
CREATE INDEX IF NOT EXISTS idx_events_b2c_upcoming ON events(starts_at ASC, category) WHERE status = 'PUBLISHED';
CREATE INDEX IF NOT EXISTS idx_events_genres ON events USING gin(genres);
CREATE INDEX IF NOT EXISTS idx_events_title_trgm ON events USING gin(title gin_trgm_ops);
CREATE INDEX IF NOT EXISTS idx_events_embedding ON events USING hnsw (embedding vector_cosine_ops);

-- 5. Sectores y Precios
CREATE TABLE IF NOT EXISTS event_sectors (
    id VARCHAR(60) PRIMARY KEY,
    event_id VARCHAR(60) NOT NULL REFERENCES events(id) ON DELETE CASCADE,
    name VARCHAR(100) NOT NULL,
    type VARCHAR(30) NOT NULL,   -- 'GENERAL_ADMISSION', 'NUMBERED_SEATS'
    total_capacity INT NOT NULL CHECK (total_capacity > 0),
    currency VARCHAR(3) NOT NULL DEFAULT 'ARS',
    base_price NUMERIC(12, 2) NOT NULL CHECK (base_price >= 0),
    service_fee NUMERIC(12, 2) NOT NULL CHECK (service_fee >= 0),
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_event_sector_name UNIQUE (event_id, name)
);

CREATE INDEX IF NOT EXISTS idx_sectors_event_id ON event_sectors(event_id);

-- 6. Transactional Outbox (Pattern for Azure Service Bus relay)
CREATE TABLE IF NOT EXISTS outbox_events (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    aggregate_type VARCHAR(50) NOT NULL,
    aggregate_id VARCHAR(60) NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    payload JSONB NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    retry_count INT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    processed_at TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_outbox_pending ON outbox_events (created_at) WHERE status = 'PENDING';
