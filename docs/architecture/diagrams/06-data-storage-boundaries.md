# Architecture Diagram 06: Distributed Relational & Vector Data Boundaries

```mermaid
erDiagram
    %% ==========================================
    %% DATABASE: catalog_db (Event & Venue Domain)
    %% ==========================================
    ORGANIZERS {
        varchar id PK
        varchar name
        varchar email
        timestamptz created_at
    }

    VENUES {
        varchar id PK
        varchar name
        varchar city
        varchar country
        numeric latitude
        numeric longitude
        timestamptz created_at
    }

    EVENTS {
        varchar id PK
        varchar organizer_id FK
        varchar venue_id FK
        varchar slug UK
        varchar title
        varchar status
        varchar category
        jsonb media
        jsonb sales_policy
        vector embedding
        timestamptz starts_at
        timestamptz created_at
    }

    EVENT_SECTORS {
        varchar id PK
        varchar event_id FK
        varchar name
        varchar type
        int total_capacity
        numeric base_price
        numeric service_fee
        varchar currency
    }

    ORGANIZERS ||--o{ EVENTS : "owns"
    VENUES ||--o{ EVENTS : "hosts"
    EVENTS ||--|{ EVENT_SECTORS : "divides_into"

    %% ==========================================
    %% DATABASE: ticketing_db (Inventory Domain)
    %% ==========================================
    SEATS {
        varchar id PK
        varchar event_id "logical reference"
        varchar sector_id "logical reference"
        varchar row_identifier
        varchar seat_number
        varchar status
        bigint version
    }

    TICKET_RESERVATIONS {
        varchar id PK
        varchar event_id "logical reference"
        varchar seat_id FK
        varchar user_id "logical reference"
        varchar status
        timestamptz expires_at
        timestamptz created_at
    }

    TICKETING_OUTBOX_EVENTS {
        uuid id PK
        varchar aggregate_type
        varchar aggregate_id
        varchar event_type
        jsonb payload
        varchar status
        timestamptz created_at
    }

    SEATS ||--o{ TICKET_RESERVATIONS : "held_by"

    %% ==========================================
    %% DATABASE: order_db (Financial & Saga Domain)
    %% ==========================================
    ORDERS {
        varchar id PK
        varchar user_id "logical reference"
        varchar reservation_id "logical reference"
        numeric total_amount
        varchar currency
        varchar status
        timestamptz created_at
    }

    ORDER_ITEMS {
        varchar id PK
        varchar order_id FK
        varchar ticket_id "logical reference"
        numeric unit_price
        numeric fee_amount
    }

    PAYMENTS {
        varchar id PK
        varchar order_id FK
        varchar provider
        varchar provider_transaction_ref
        numeric amount
        varchar status
        timestamptz paid_at
    }

    SAGA_INSTANCES {
        uuid id PK
        varchar saga_type
        varchar current_state
        varchar correlation_id UK
        jsonb payload
        timestamptz updated_at
    }

    ORDER_OUTBOX_EVENTS {
        uuid id PK
        varchar aggregate_type
        varchar aggregate_id
        varchar event_type
        jsonb payload
        varchar status
        timestamptz created_at
    }

    ORDERS ||--|{ ORDER_ITEMS : "contains"
    ORDERS ||--o{ PAYMENTS : "billed_under"

    %% ==========================================
    %% DISTRIBUTED LOGICAL BOUNDARIES (Dashed Lines)
    %% ==========================================
    EVENTS ..o{ SEATS : "generates_seats"
    EVENT_SECTORS ..o{ SEATS : "partitions"
    TICKET_RESERVATIONS ..o{ ORDERS : "fulfills"
```

---

## Architectural Data Boundary Principles

1. **Zero Cross-Database Foreign Keys:**
   - Physical constraints (`FOREIGN KEY`) exist strictly within single datastores (`catalog_db`, `ticketing_db`, or `order_db`).
   - Cross-service references use immutable string/UUID business identifiers (e.g. `event_id`, `reservation_id`, `user_id`).
2. **AI & Vector Embeddings (`catalog_db`):**
   - The `embedding` field in `events` uses PostgreSQL `pgvector` with HNSW indexing to calculate cosine distance for recommendations without external vector database licensing costs.
3. **Optimistic Locking:**
   - The `version` column in `seats` and `event_sectors` guards against concurrent update anomalies at the database level.
4. **Independent Transactional Outbox Tables:**
   - Both `ticketing_db` and `order_db` maintain their own isolated `outbox_events` table, preventing shared state while ensuring guaranteed event delivery.