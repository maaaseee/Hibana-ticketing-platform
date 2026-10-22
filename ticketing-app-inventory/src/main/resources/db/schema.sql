-- Inventory Domain: Physical Seats
CREATE TABLE IF NOT EXISTS seats (
    id VARCHAR(60) PRIMARY KEY,
    event_id VARCHAR(60) NOT NULL,
    sector_id VARCHAR(60) NOT NULL,
    row_identifier VARCHAR(20) NOT NULL,
    seat_number VARCHAR(20) NOT NULL,
    price NUMERIC(10, 2) NOT NULL DEFAULT 0.00,
    currency VARCHAR(10) NOT NULL DEFAULT 'USD',
    status VARCHAR(30) NOT NULL DEFAULT 'AVAILABLE',
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_seat_coordinates UNIQUE (event_id, sector_id, row_identifier, seat_number)
);

CREATE INDEX IF NOT EXISTS idx_seats_event_status ON seats(event_id, status);
CREATE INDEX IF NOT EXISTS idx_seats_sector ON seats(sector_id);

-- Inventory Domain: Ticket Reservations
CREATE TABLE IF NOT EXISTS ticket_reservations (
    id VARCHAR(60) PRIMARY KEY,
    event_id VARCHAR(60) NOT NULL,
    seat_id VARCHAR(60) NOT NULL,
    user_id VARCHAR(60) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING_PAYMENT',
    expires_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    confirmed_at TIMESTAMPTZ,
    order_id VARCHAR(60)
);

CREATE INDEX IF NOT EXISTS idx_reservations_user ON ticket_reservations(user_id);
CREATE INDEX IF NOT EXISTS idx_reservations_event_seat ON ticket_reservations(event_id, seat_id);
CREATE INDEX IF NOT EXISTS idx_reservations_status_expires ON ticket_reservations(status, expires_at);

-- Transactional Outbox Table
CREATE TABLE IF NOT EXISTS outbox_events (
    id UUID PRIMARY KEY,
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
