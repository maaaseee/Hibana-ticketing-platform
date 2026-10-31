# Architecture Diagram 07: Reactive Frontend State Machine & Checkout Countdown Lifecycle

```mermaid
stateDiagram-v2
    [*] --> BrowsingCatalog: User lands on www.hibana.tk
    
    state "Browsing Catalog (Discovery)" as BrowsingCatalog
    state "Virtual Waiting Room (Queueing)" as InWaitingRoom
    state "Interactive Seat Selection" as SeatSelection
    state "Checkout Active (10m Countdown)" as CheckoutActive
    state "Processing Payment (UI Locked)" as ProcessingPayment
    state "Order Success (Ticket & QR)" as SuccessScreen
    state "Hold Timeout (00:00 Expired)" as TimeoutExpired

    %% Navigation & Admission Flow
    BrowsingCatalog --> InWaitingRoom: User clicks "Buy Tickets" on high-demand event
    BrowsingCatalog --> SeatSelection: Event traffic normal (no queue active)
    InWaitingRoom --> InWaitingRoom: SSE/WebSocket updates position and ETA (ZRANK)
    InWaitingRoom --> SeatSelection: Gateway issues X-Checkout-Token (valid for 15m)

    %% Seat Selection & Reservation
    SeatSelection --> CheckoutActive: Seat selected and POST /api/v1/reservations returns 201 Created
    SeatSelection --> BrowsingCatalog: User cancels selection

    %% Checkout & Payment Lifecycle
    CheckoutActive --> ProcessingPayment: User inputs payment details and clicks "Confirm & Pay"
    CheckoutActive --> TimeoutExpired: Countdown timer reaches 00:00 (Hold expired)

    ProcessingPayment --> SuccessScreen: HTTP 200/201: Order confirmed and tickets minted
    ProcessingPayment --> CheckoutActive: Payment declined (card error) - timer continues ticking
    ProcessingPayment --> TimeoutExpired: Payment gateway timeout past 10m TTL

    %% Terminal States
    SuccessScreen --> [*]: Dynamic TOTP QR rendered and download available
    TimeoutExpired --> InWaitingRoom: Pinia store purged and user redirected to queue
```

---

## Pinia Store Architecture & Clock Skew Reconciliation

### 1. Store Modules (`apps/b2c-web`)

- **`useWaitingRoomStore`:**
  - State: `queuePosition`, `estimatedWaitTimeSeconds`, `checkoutToken`, `connectionStatus` (SSE / WebSocket).
  - Actions: `connectQueueStream(eventId)`, `disconnect()`, `setToken(jwt)`.
- **`useCheckoutStore`:**
  - State: `reservationId`, `selectedSeats`, `totalAmount`, `serverExpiresAtMs`, `remainingSeconds`, `idempotencyKey`.
  - Actions: `startHoldTimer(serverExpiresAt)`, `submitPayment(paymentDetails)`, `resetCheckout()`.

### 2. Preventing Client-Side Clock Skew Errors

To prevent user device clock discrepancies from corrupting the 10-minute reservation countdown:

1. **Server Time Synchronization Offset:**
   - On response of `POST /api/v1/reservations`, the client extracts the HTTP `Date` header from the response.
   - Computes clock offset: 
     $$\text{clockOffset} = \text{serverTime} - \text{clientLocalTime}$$
2. **Reconciled Countdown Calculation:**
   - The countdown timer ticks locally via `requestAnimationFrame` or `setInterval(1000)` using the server reference:
     $$\text{remainingMs} = \text{serverExpiresAtMs} - (\text{Date.now()} + \text{clockOffset})$$
3. **Route Navigation Guard (`vue-router`):**
   - If user attempts to navigate back during `CheckoutActive`, `beforeRouteLeave` intercepts with a confirmation modal warning that their 10-minute hold will be forfeit.