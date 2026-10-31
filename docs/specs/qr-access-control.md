# Architectural Proposal: Secure Single-Use QR Access Control

## 1. Executive Summary
This document specifies the technical design, security model, database schema, and verification protocols for single-use, event-scoped QR code access control across venues managed by Hibana Software.

The goal is to guarantee that every issued ticket can only be admitted once, is strictly bound to its assigned event and venue, and cannot be duplicated, forged, or scalped via screenshots.

---

## 2. Security Architecture & Threat Model

### 2.1. Anti-Patterns to Avoid
* **Raw Database IDs:** Never encode URLs containing sequential or raw ticket IDs (e.g., `https://api.domain.com/admit?id=12345`). Attackers can enumerate and forge entry passes.
* **Static Plaintext Passes:** Static QR codes can be easily shared or sold to multiple buyers simultaneously, leading to gate collisions.

### 2.2. Defense Mechanisms

#### A. Cryptographically Signed Token
The QR payload is an asymmetric, signed compact token (JWT or Ed25519 signature):

```json
{
  "tid": "tkt_01J9X8A1B2C3D4E5",
  "eid": "evt_01J9X8A1Z7K8B2C4",
  "vid": "ven_estadio_nacional",
  "sec": "sec_vip_standing",
  "exp": 1795219200,
  "sig": "MEQCIAx...base64Signature..."
}
```

* **Offline Scanner Validation:** The turnstile or handheld scanner maintains the public key (synced from Azure Key Vault). If network connectivity drops at a high-density venue, the scanner can verify the digital signature locally.
* **Strict Event Scoping:** The scanner is bound to `scanner.event_id`. If `payload.eid != scanner.event_id`, entry is rejected with **"WRONG EVENT"**.

#### B. Anti-Screenshot Dynamic QR (TOTP / Rotating Seed)
For high-demand events with elevated scalping risk:
* The mobile client generates a dynamic QR code refreshed every 15–30 seconds using an HMAC-SHA256 Time-based One-Time Password (TOTP) algorithm seeded during purchase.
* Screenshots taken and forwarded to third parties expire within seconds, rendering fraudulent copies useless at the turnstile.

---

## 3. Database Schema Design (PostgreSQL)

This schema resides within the **Ticketing & Inventory / Access Control** domain.

```sql
-- 1. Physical Issued Tickets Table
CREATE TABLE tickets (
    id VARCHAR(60) PRIMARY KEY,                         -- 'tkt_01J9X8A1B2C3D4E5'
    order_id VARCHAR(60) NOT NULL,                      -- Purchase order reference
    event_id VARCHAR(60) NOT NULL REFERENCES events(id),-- Strict event boundary
    event_sector_id VARCHAR(60) NOT NULL REFERENCES event_sectors(id),
    seat_id VARCHAR(60),                                -- NULL for General Admission
    customer_id VARCHAR(60) NOT NULL,
    
    -- Single-Use Lifecycle State
    status VARCHAR(30) NOT NULL DEFAULT 'ISSUED',       -- ISSUED, REDEEMED, CANCELLED, REFUNDED
    
    -- Security & Verification
    qr_hash VARCHAR(128) NOT NULL UNIQUE,               -- Hash of secret verification token
    
    -- Redemption Audit Trail
    redeemed_at TIMESTAMPTZ,
    redeemed_by_device VARCHAR(60),                     -- Turnstile / Handheld scanner ID
    gate_name VARCHAR(50),                              -- 'Gate 4 - North'
    
    version BIGINT NOT NULL DEFAULT 0,                  -- Optimistic locking for concurrency
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_tickets_event_lookup ON tickets(event_id, status);
CREATE INDEX idx_tickets_qr_hash ON tickets(qr_hash);

-- 2. Audit Trail for All Scan Attempts (Security & Fraud Forensics)
CREATE TABLE ticket_access_logs (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    ticket_id VARCHAR(60) NOT NULL REFERENCES tickets(id),
    event_id VARCHAR(60) NOT NULL,
    device_id VARCHAR(60) NOT NULL,
    gate_name VARCHAR(50),
    scan_result VARCHAR(40) NOT NULL,                   -- 'GRANTED', 'DENIED_DUPLICATE', 'DENIED_WRONG_EVENT', 'DENIED_CANCELLED'
    notes TEXT,
    scanned_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_access_logs_ticket ON ticket_access_logs(ticket_id, scanned_at DESC);
```

---

## 4. Atomic Redemption & Race Condition Prevention

When multiple copies of the same ticket attempt to scan simultaneously across different turnstiles:

```sql
UPDATE tickets
SET status = 'REDEEMED',
    redeemed_at = NOW(),
    redeemed_by_device = :scannerDeviceId,
    gate_name = :gateName,
    version = version + 1
WHERE id = :ticketId
  AND event_id = :currentEventId        -- Enforces correct event
  AND status = 'ISSUED'                 -- Enforces ONE-TIME use
RETURNING *;
```

### Scan Evaluation Matrix:
1. **1 row returned:** **ACCESS GRANTED (GREEN)**. The ticket was `ISSUED` and is now atomically marked `REDEEMED`.
2. **0 rows returned:** Query the ticket to determine the exact failure reason:
   * `status = 'REDEEMED'`: **DENIED — ALREADY USED** (Display previous timestamp and gate to security personnel).
   * `event_id != :currentEventId`: **DENIED — INVALID TICKET FOR THIS EVENT**.
   * `status IN ('CANCELLED', 'REFUNDED')`: **DENIED — TICKET VOID/REFUNDED**.

---

## 5. Domain Boundary Placement (DDD)
* **`catalog-service`:** Holds the definitions of `events`, `venues`, and `event_sectors` (metadata, dates, and sector capacities).
* **`ticketing-service`:** Owns ticket generation, QR token signing, and the high-throughput check-in endpoint (`POST /api/v1/access/scan`).
