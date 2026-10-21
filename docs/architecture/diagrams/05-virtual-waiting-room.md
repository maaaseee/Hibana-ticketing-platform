# Architecture Diagram 05: Virtual Waiting Room & Traffic-Shaping Algorithm

```mermaid
flowchart TD
    SURGE["Surge Traffic Hits System<br/>(Up to 100,000 Concurrent Users / Flash Sale)"] --> GW_INSPECT["Spring Cloud Gateway: Inspection Filter"]

    GW_INSPECT --> CHECK_FLAG{"Redis GET event:id:queue_enabled<br/>Is Virtual Waiting Room Active?"}

    CHECK_FLAG -->|No: Low Traffic Event| DIRECT_PASS["Direct Forwarding to Catalog / Ticketing Service<br/>(Rate limited to 100 req/min/IP)"]
    
    CHECK_FLAG -->|Yes: High Demand Queue Active| ENQUEUE["Enqueue in Redis Sorted Set (FIFO)<br/>ZADD queue:event:id epoch_ms userId"]

    ENQUEUE --> FEEDBACK_LOOP["Real-Time User Feedback (SSE / WebSocket / Polling)<br/>Calculate Position: ZRANK queue:event:id userId<br/>ETA = (Rank / DrainRate) * Interval"]

    FEEDBACK_LOOP --> ADMISSION_WORKER["Scheduled Admission Engine (@Scheduled every 1 sec)<br/>ZPOPMIN queue:event:{eventId} {batchSize: N}"]

    ADMISSION_WORKER --> GENERATE_TOKEN["Mint Cryptographically Signed JWT / HMAC Token<br/>'X-Checkout-Token' (Valid for 15 minutes)<br/>Claims: userId, eventId, allowedSectors, exp"]

    GENERATE_TOKEN --> CLIENT_RECEIVES["Client Receives Checkout Token via SSE/WebSocket<br/>Redirects from Waiting Screen -> Interactive Seat Map"]

    CLIENT_RECEIVES --> GATEWAY_CHECKOUT["Client Sends Reserve Request with 'X-Checkout-Token'<br/>POST /api/v1/reservations"]

    GATEWAY_CHECKOUT --> FILTER_VAL{"CheckoutTokenFilterFactory<br/>1. Signature valid?<br/>2. Not expired?<br/>3. EventId matches URL?"}

    FILTER_VAL -->|Invalid / Tampered / Expired| REJECT["HTTP 403 Forbidden<br/>(Purge Local State & Return to Waiting Room)"]
    FILTER_VAL -->|Signature Verified & Active| ALLOW["Route to Ticketing Service<br/>(Execute Redis Redlock Atomically)"]
```

---

## Redis Commands & Algorithmic Complexity Analysis

| Phase / Operation | Redis Command | Algorithmic Complexity | Execution Rationale & Performance Impact |
| :--- | :--- | :--- | :--- |
| **Queue State Check** | `GET event:{id}:queue_enabled` | $O(1)$ | Microsecond boolean check per incoming request at the Gateway perimeter. |
| **User Admission (Join)** | `ZADD queue:event:{id} <timestamp_ms> <user_id>` | $O(\log N)$ | Preserves strictly fair FIFO ordering using arrival millisecond timestamp as the score. $N = 100{,}000$ requires $\approx 17$ internal skiplist pointer operations. |
| **Position Query** | `ZRANK queue:event:{id} <user_id>` | $O(\log N)$ | Retrieves 0-based rank. Enables accurate, dynamic queue progression without full table scans. |
| **Batch Admission (Pop)** | `ZPOPMIN queue:event:{id} <batch_size>` | $O(\log N + M)$ | Atomically pops top $M$ users from the front of the queue to admit into checkout. $M$ is dynamically adjusted based on downstream database write IOPS. |
| **Queue Depth (Telemetry)**| `ZCARD queue:event:{id}` | $O(1)$ | Instantaneous total waiting queue size for Prometheus / Azure Monitor dashboards. |
| **Atomic Seat Locking** | `SET lock:event:{e}:seat:{s} {token} NX EX 600` | $O(1)$ | Atomic Redlock hold; guarantees no two buyers can hold the same seat key concurrently. |
| **Active Release** | `DEL lock:event:{e}:seat:{s}` | $O(1)$ | Millisecond compensation executed upon payment failure or cart abandonment. |