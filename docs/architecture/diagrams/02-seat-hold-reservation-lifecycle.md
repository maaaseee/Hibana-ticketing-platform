# Architecture Diagram 02: End-to-End High-Concurrency Seat Hold & Reservation Lifecycle

```mermaid
sequenceDiagram
    autonumber
    actor User as User (apps/b2c-web)
    participant GW as Spring Cloud Gateway
    participant Redis as Redis Cluster (Cache & Locks)
    participant Ticketing as Ticketing Service (API & Worker)
    participant DB as PostgreSQL (ticketing_db)
    participant Bus as Azure Service Bus (Topics)

    User->>GW: POST /api/v1/reservations<br/>Headers: X-Checkout-Token, Idempotency-Key<br/>Payload: eventId, seatId, sectorId
    
    Note over GW: CheckoutTokenFilterFactory executes:<br/>1. Verifies HMAC-SHA256 signature<br/>2. Validates token expiration (under 15 mins)<br/>3. Confirms user ID and event scope match
    
    alt Token Invalid or Expired
        GW-->>User: HTTP 403 Forbidden (Token Expired or Invalid)
    else Token Valid
        GW->>Ticketing: Forward POST /api/v1/reservations<br/>Headers: X-User-Id, X-Trace-Id, Idempotency-Key

        Note over Ticketing,Redis: Phase 1: Atomic Concurrency Control (Zero Overselling)
        Ticketing->>Redis: EVALSHA atomic_reserve.lua 2 lock:event:seat quota:event:sector 600 userId
        
        Note over Redis: Lua Script (Atomic Execution):<br/>1. Check if seat lock key exists<br/>2. Verify sector quota is positive<br/>3. SET seat lock with 10 min TTL (EX 600)<br/>4. DECR sector quota
        
        alt Seat Already Held or Sector Sold Out
            Redis-->>Ticketing: Return 0 (Lock Failed or Quota Exhausted)
            Ticketing-->>GW: HTTP 409 Conflict (Seat Unavailable)
            GW-->>User: HTTP 409 Conflict (Prompt User to Select Another Seat)
        else Lock Acquired Successfully
            Redis-->>Ticketing: Return 1 (Hold Granted, TTL 600s)

            Note over Ticketing,DB: Phase 2: Transactional Outbox Pattern (Single Local ACID Transaction)
            Ticketing->>DB: BEGIN TRANSACTION
            Ticketing->>DB: INSERT INTO ticket_reservations (status: PENDING_PAYMENT, expires_at: NOW + 10m)
            Ticketing->>DB: INSERT INTO outbox_events (eventType: ReservationCreatedEvent, status: PENDING)
            Ticketing->>DB: COMMIT TRANSACTION

            Ticketing-->>GW: HTTP 201 Created (reservationId, expiresAt, holdDuration: 600s)
            GW-->>User: HTTP 201 Created (Client Starts 10-Minute Checkout Countdown)

            Note over Ticketing,Bus: Phase 3: Decoupled Asynchronous Outbox Relay (At-Least-Once Delivery)
            loop Async Poller / CDC Worker (Every 500ms)
                Ticketing->>DB: SELECT outbox_events WHERE status = PENDING FOR UPDATE SKIP LOCKED
                DB-->>Ticketing: Batch of Pending Events
                Ticketing->>Bus: Publish ReservationCreatedEvent (Topic: ticket.reservation.v1)
                Bus-->>Ticketing: ACK (Message Persisted)
                Ticketing->>DB: UPDATE outbox_events SET status = PUBLISHED
            end
        end
    end
```

---

## Technical Concurrency Guarantees

1. **Race-Condition Elimination:**
   - The Redis Lua script executes atomically within Redis's single-threaded event loop. If 500 parallel requests hit the exact same seat simultaneously, exactly 1 request acquires the 10-minute key; the remaining 499 evaluate the existing key and return `0` immediately without touching PostgreSQL.
2. **Dual-Write Safety (Transactional Outbox):**
   - The reservation entity and domain event are committed inside the exact same local ACID transaction in `ticketing_db`. Database writes and event emissions can never fall out of sync.
3. **Passive Expiration vs Active Saga Compensation:**
   - **Passive:** If the buyer abandons checkout, the Redis lock evaporates after 600 seconds, and a background reconciliation cleaner transitions `ticket_reservations` to `EXPIRED`.
   - **Active:** If payment fails in the Saga, an explicit compensating action deletes the Redis key and updates status to `CANCELLED` within milliseconds.