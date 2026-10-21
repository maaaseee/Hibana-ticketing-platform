# Architecture Diagram 03: Distributed Checkout Saga State Machine (CheckoutSaga)

```mermaid
stateDiagram-v2
    [*] --> RESERVATION_HELD: Seat hold requested via Redis Redlock (10m TTL)

    state "Reservation Held (10m TTL)" as RESERVATION_HELD
    state "Order Created (Draft)" as ORDER_CREATED
    state "Payment Pending (Method Selected)" as PAYMENT_PENDING
    state "Payment Processing (Gateway Auth)" as PAYMENT_PROCESSING
    state "Tickets Issued (Signed QR)" as TICKETS_ISSUED
    state "Order Completed (Final Success)" as ORDER_COMPLETED
    
    state "Hold Expired (Timeout 10m)" as RESERVATION_EXPIRED
    state "Payment Failed (Card Error / Decline)" as PAYMENT_FAILED
    state "Saga Compensating (Active Reversal)" as SAGA_COMPENSATING
    state "Inventory Released (Quota Restored)" as INVENTORY_RELEASED
    state "Saga Failed (Order Aborted)" as SAGA_FAILED

    %% Happy Path Transitions
    RESERVATION_HELD --> ORDER_CREATED: User initiates checkout and OrderService initializes draft order
    ORDER_CREATED --> PAYMENT_PENDING: Customer selects payment method and confirms total
    PAYMENT_PENDING --> PAYMENT_PROCESSING: Payment authorization request dispatched to Gateway
    PAYMENT_PROCESSING --> TICKETS_ISSUED: Payment authorized successfully (HTTP 200 or Webhook)
    TICKETS_ISSUED --> ORDER_COMPLETED: Tickets minted with signed QR and double-entry ledger recorded
    ORDER_COMPLETED --> [*]

    %% Failure & Timeout Transitions
    RESERVATION_HELD --> RESERVATION_EXPIRED: 10-minute hold TTL expires without checkout
    ORDER_CREATED --> RESERVATION_EXPIRED: User abandons cart or countdown reaches zero
    PAYMENT_PENDING --> RESERVATION_EXPIRED: 10-minute hold window elapses
    PAYMENT_PROCESSING --> PAYMENT_FAILED: Gateway returns card declined, fraud alert, or timeout

    %% Compensation & Rollback Flow
    PAYMENT_FAILED --> SAGA_COMPENSATING: OrderService publishes PaymentFailedEvent
    RESERVATION_EXPIRED --> SAGA_COMPENSATING: TicketingService detects hold expiration
    SAGA_COMPENSATING --> INVENTORY_RELEASED: Active compensation: DEL Redis lock and restore sector quota
    INVENTORY_RELEASED --> SAGA_FAILED: Order marked CANCELLED and customer notified
    SAGA_FAILED --> [*]
```

---

## Saga Execution & Compensation Matrix

| Current State | Trigger Event | Handling Service | Success Action | Compensating Action (Upon Failure / Timeout) |
| :--- | :--- | :--- | :--- | :--- |
| **RESERVATION_HELD** | `SeatHoldRequested` | `ticketing-service` | Acquire Redlock in Redis with 10m TTL (600s), insert `ticket_reservations`. | Passive TTL expiration; key deleted from Redis if user cancels early. |
| **ORDER_CREATED** | `ReservationCreatedEvent` | `order-service` | Create Order aggregate with status `PENDING_PAYMENT`, calculate taxes & fees. | Publish `OrderCancelledEvent` if customer abandons cart. |
| **PAYMENT_PENDING** | `PaymentInitiatedEvent` | `order-service` | Submit charge request with idempotency token to payment gateway. | Mark order `ABORTED`; trigger inventory release compensation. |
| **PAYMENT_PROCESSING** | `PaymentCapturedEvent` | Payment Gateway / `order-service` | Transition order to `PAID`, trigger ticket issuance and ledger entry. | Gateway decline publishes `PaymentFailedEvent` -> triggers `SAGA_COMPENSATING`. |
| **SAGA_COMPENSATING** | `PaymentFailedEvent` or `HoldExpiredEvent` | `ticketing-service` & `order-service` | Delete Redis reservation lock (`DEL lock:event:{e}:seat:{s}`), `INCR` quota. | Retry compensation via dead-letter queue (DLQ) if Redis connection fails. |
| **INVENTORY_RELEASED** | `InventoryCompensatedEvent` | `ticketing-service` | Update `ticket_reservations` to `CANCELLED`; return seat to `AVAILABLE`. | N/A (Terminal compensation completed). |
| **TICKETS_ISSUED** | `OrderPaidEvent` | `ticketing-service` & `order-service` | Generate cryptographically signed QR codes (`Ed25519`), write double-entry ledger. | Non-reversible once committed; requires administrative manual dispute/refund. |
| **ORDER_COMPLETED** | `TicketsDeliveredEvent` | Notification Worker | Dispatch confirmation email with QR ticket attachments. | Queue email retry on SMTP provider backoff. |