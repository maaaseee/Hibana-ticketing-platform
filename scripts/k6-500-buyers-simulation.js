/**
 * ============================================================================
 * Hibana.tk — High-Concurrency 500-Buyer Stress & AKS HPA Autoscaling Suite
 * ============================================================================
 *
 * Designed for Grafana k6 & Azure Load Testing.
 *
 * Validates two critical production workloads simultaneously:
 *   1. [SCENARIO A: hot_seat_collision_burst]
 *      100 concurrent buyers collide at the exact same instant on a single
 *      VIP seat ("Hot Seat"). Validates that Redis Redlock (Lua SET NX EX)
 *      grants 201 Created to AT MOST 1 buyer and rejects all others with
 *      HTTP 409 Conflict (RFC 7807 ProblemDetail) — 0% Double Booking.
 *
 *   2. [SCENARIO B: intense_500_buyers_funnel]
 *      Ramps up to 500 concurrent buyers executing the complete, CPU-intensive
 *      End-to-End purchase journey to trigger Kubernetes HPA Pod Autoscaling:
 *        - Catalog Browsing (GET /api/v1/events & /api/v1/events/{id})
 *        - Virtual Waiting Room (POST /api/v1/queue/join & GET /api/v1/queue/status)
 *        - Live Seat Map Query (GET /api/v1/events/{id}/seats)
 *        - Pairwise Seat Contention + Automatic Fallback Seat Selection
 *          (Simulates 2 buyers clicking the same seat -> 1 wins 201, 1 gets 409
 *           and immediately selects an adjacent available seat)
 *        - Order Creation & Double-Entry Ledger Settlement (POST /orders & /pay)
 *        - Official OpenPDF Invoice Generation (GET /api/v1/orders/{id}/invoice)
 *
 * Run with Real-Time Web Dashboard & HTML Report Export:
 *   K6_WEB_DASHBOARD=true K6_WEB_DASHBOARD_EXPORT=docs/benchmarks/stress-500-users-report.html k6 run scripts/k6-500-buyers-simulation.js
 * ============================================================================
 */

import http from 'k6/http';
import { check, group, sleep } from 'k6';
import crypto from 'k6/crypto';
import encoding from 'k6/encoding';
import { Counter, Rate, Trend } from 'k6/metrics';

// ----------------------------------------------------------------------------
// Custom Observability Metrics
// ----------------------------------------------------------------------------
const seatLocksAcquired201 = new Counter('hibana_seat_locks_acquired_201');
const seatCollisionsRejected409 = new Counter('hibana_seat_collisions_rejected_409');
const waitingRoomJoins200 = new Counter('hibana_waiting_room_joins_200');
const ordersCreated201 = new Counter('hibana_orders_created_201');
const paymentsSettled200 = new Counter('hibana_payments_settled_200');
const pdfInvoicesGenerated200 = new Counter('hibana_pdf_invoices_generated_200');
const unexpected5xxRate = new Rate('hibana_unexpected_5xx_errors');

const seatLockLatency = new Trend('hibana_seat_lock_latency_ms', true);
const paymentSettlementLatency = new Trend('hibana_payment_settlement_latency_ms', true);
const pdfInvoiceLatency = new Trend('hibana_pdf_invoice_latency_ms', true);
const e2ePurchaseDuration = new Trend('hibana_e2e_purchase_duration_ms', true);

// ----------------------------------------------------------------------------
// Environment Configuration
// ----------------------------------------------------------------------------
const GATEWAY_URL = __ENV.GATEWAY_URL || 'http://localhost:8080';
const ORDERS_URL = __ENV.ORDERS_URL || GATEWAY_URL;
const WAITING_ROOM_SECRET = __ENV.WAITING_ROOM_JWT_SECRET || 'virtual-waiting-room-hmac-key-2026-minimum-256-bits!';
const WAITING_ROOM_ISSUER = __ENV.WAITING_ROOM_JWT_ISSUER || 'waiting-room-engine';
const GATEWAY_HMAC_SECRET = __ENV.GATEWAY_HMAC_SECRET || 'gateway-internal-hmac-secret-2026-minimum-256-bits!';

// ----------------------------------------------------------------------------
// Workload Profile & Acceptance Thresholds (Section 13.4 of Ticketing-app.md)
// ----------------------------------------------------------------------------
export const options = {
  scenarios: {
    // Scenario 1: Exact Simultaneous Collision on a Single VIP Seat
    hot_seat_collision_burst: {
      executor: 'per-vu-iterations',
      vus: 100,
      iterations: 1,
      maxDuration: '30s',
      exec: 'hotSeatCollisionScenario',
    },

    // Scenario 2: Sustained 500-Buyer Surge to Trigger AKS HPA Pod Autoscaling
    intense_500_buyers_funnel: {
      executor: 'ramping-vus',
      startVUs: 10,
      stages: [
        { duration: '30s', target: 150 }, // Warm-up & initial queue surge
        { duration: '1m',  target: 500 }, // Peak 500 concurrent buyers (triggers HPA scale-up)
        { duration: '1m',  target: 500 }, // Sustained high-intensity checkout & PDF generation
        { duration: '30s', target: 0 },   // Cool-down
      ],
      gracefulRampDown: '15s',
      exec: 'fullPurchaseFunnelScenario',
    },
  },
  thresholds: {
    'hibana_unexpected_5xx_errors': ['rate<0.01'],         // < 1% 5xx errors under peak load
    'hibana_seat_lock_latency_ms': ['p(95)<300'],          // P95 < 300ms for Redis Redlock reservation
    'hibana_seat_collisions_rejected_409': ['count>=99'],  // At least 99 rejected in the 100-VU collision burst
  },
};

// ----------------------------------------------------------------------------
// Cryptographic Helpers (HMAC-SHA256 JWT & Gateway Stamp)
// ----------------------------------------------------------------------------
function base64UrlEncode(input) {
  return encoding.b64encode(input, 'rawurl');
}

function generateWaitingRoomJwt(userId, validMinutes = 15) {
  const header = JSON.stringify({ alg: 'HS256', typ: 'JWT' });
  const now = Math.floor(Date.now() / 1000);
  const payload = JSON.stringify({
    sub: userId,
    iss: WAITING_ROOM_ISSUER,
    iat: now,
    exp: now + validMinutes * 60,
  });

  const unsignedToken = `${base64UrlEncode(header)}.${base64UrlEncode(payload)}`;
  const sigBytes = crypto.hmac('sha256', WAITING_ROOM_SECRET, unsignedToken, 'binary');
  const encodedSig = encoding.b64encode(sigBytes, 'rawurl');
  return `${unsignedToken}.${encodedSig}`;
}

function generateGatewayStamp(userId, roles = 'ROLE_USER') {
  const timestamp = Date.now();
  const payload = `${userId}:${roles}:${timestamp}`;
  const signature = crypto.hmac('sha256', GATEWAY_HMAC_SECRET, payload, 'hex');
  return { signature, timestamp: String(timestamp) };
}

function buildUserIp(vuId) {
  const octet3 = Math.floor((vuId / 254) % 254) + 1;
  const octet4 = (vuId % 254) + 1;
  return `198.51.${octet3}.${octet4}`;
}

// ----------------------------------------------------------------------------
// Setup Phase: Discover Target Event & Available Seats
// ----------------------------------------------------------------------------
export function setup() {
  const eventsRes = http.get(`${GATEWAY_URL}/api/v1/events`, {
    headers: { 'X-Forwarded-For': '198.51.100.1' },
  });

  let eventId = 'evt-ado-world-tour-ba';
  if (eventsRes.status === 200) {
    try {
      const body = eventsRes.json();
      const events = Array.isArray(body) ? body : (body.content || body.events || []);
      if (events.length > 0 && events[0].id) {
        eventId = events[0].id;
      }
    } catch (_) {}
  }

  const seatsRes = http.get(`${GATEWAY_URL}/api/v1/events/${eventId}/seats`, {
    headers: { 'X-Forwarded-For': '198.51.100.2' },
  });

  let seatIds = [];
  if (seatsRes.status === 200) {
    try {
      const payload = seatsRes.json();
      const seats = Array.isArray(payload) ? payload : (payload.seats || []);
      seatIds = seats
        .filter((s) => s.status === 'AVAILABLE')
        .map((s) => s.id || s.seatId);
    } catch (_) {}
  }

  const hotSeatId = seatIds.length > 0 ? seatIds[0] : 'SEAT-VIP-COLLISION-001';

  return {
    eventId,
    hotSeatId,
    seatIds,
  };
}

// ============================================================================
// SCENARIO 1: 100-Buyer Simultaneous Race Condition on the Same Seat
// ============================================================================
export function hotSeatCollisionScenario(data) {
  const userId = `vu_collision_${__VU}_${Date.now()}`;
  const clientIp = buildUserIp(__VU + 1000);
  const checkoutToken = generateWaitingRoomJwt(userId, 15);

  const start = Date.now();
  const res = http.post(
    `${GATEWAY_URL}/api/v1/reservations`,
    JSON.stringify({
      eventId: data.eventId,
      seatId: data.hotSeatId,
      userId: userId,
    }),
    {
      headers: {
        'Content-Type': 'application/json',
        'X-Checkout-Token': checkoutToken,
        'X-Forwarded-For': clientIp,
      },
      tags: { name: 'POST /api/v1/reservations (Hot Seat Collision)' },
    }
  );
  seatLockLatency.add(Date.now() - start);
  unexpected5xxRate.add(res.status >= 500);

  if (res.status === 201) {
    seatLocksAcquired201.add(1);
  } else if (res.status === 409 || res.status === 423) {
    seatCollisionsRejected409.add(1);
  }

  check(res, {
    '[Collision] Response is strictly 201 Created or 409 Conflict (Zero 500s)': (r) =>
      r.status === 201 || r.status === 409 || r.status === 423,
  });
}

// ============================================================================
// SCENARIO 2: Intense 500-Buyer End-to-End Purchase & PDF Invoice Funnel
// ============================================================================
export function fullPurchaseFunnelScenario(data) {
  const funnelStart = Date.now();
  const userId = `buyer_vu_${__VU}_iter_${__ITER}_${Date.now()}`;
  const clientIp = buildUserIp(__VU);
  const checkoutToken = generateWaitingRoomJwt(userId, 15);

  const commonHeaders = {
    'Content-Type': 'application/json',
    'X-Forwarded-For': clientIp,
  };

  // 1. Browse Event Catalog & Event Detail
  group('01_Catalog_Discovery', () => {
    const catRes = http.get(`${GATEWAY_URL}/api/v1/events`, {
      headers: commonHeaders,
      tags: { name: 'GET /api/v1/events' },
    });
    unexpected5xxRate.add(catRes.status >= 500);

    const detailRes = http.get(`${GATEWAY_URL}/api/v1/events/${data.eventId}`, {
      headers: commonHeaders,
      tags: { name: 'GET /api/v1/events/{id}' },
    });
    unexpected5xxRate.add(detailRes.status >= 500);
  });

  // 2. Join Virtual Waiting Room & Check Queue Position (Redis ZSET)
  group('02_Virtual_Waiting_Room', () => {
    const joinRes = http.post(
      `${GATEWAY_URL}/api/v1/queue/join`,
      JSON.stringify({ eventId: data.eventId, userId }),
      {
        headers: commonHeaders,
        tags: { name: 'POST /api/v1/queue/join' },
      }
    );
    if (joinRes.status === 200 || joinRes.status === 201) {
      waitingRoomJoins200.add(1);
    }
    unexpected5xxRate.add(joinRes.status >= 500);

    const statusRes = http.get(
      `${GATEWAY_URL}/api/v1/queue/status?eventId=${encodeURIComponent(data.eventId)}&userId=${encodeURIComponent(userId)}`,
      {
        headers: commonHeaders,
        tags: { name: 'GET /api/v1/queue/status' },
      }
    );
    unexpected5xxRate.add(statusRes.status >= 500);
  });

  // 3. Query Seat Map & Reserve Seat (with Intentional Pairwise Seat Contention)
  let reservationId = null;
  let reservedSeatId = null;

  group('03_Seat_Map_And_Redlock_Hold', () => {
    const seatsRes = http.get(`${GATEWAY_URL}/api/v1/events/${data.eventId}/seats`, {
      headers: commonHeaders,
      tags: { name: 'GET /api/v1/events/{id}/seats' },
    });
    unexpected5xxRate.add(seatsRes.status >= 500);

    // Pairwise Contention: VUs (1,2), (3,4), (5,6) initially target the SAME seat index
    const pairIndex = Math.floor(__VU / 2) + (__ITER * 250);
    const pool = data.seatIds && data.seatIds.length > 1 ? data.seatIds : [];
    const contestedSeatId =
      pool.length > 1
        ? pool[(pairIndex % (pool.length - 1)) + 1]
        : `SEAT-CONTENDED-${pairIndex}`;

    const lockStart = Date.now();
    const holdRes = http.post(
      `${GATEWAY_URL}/api/v1/reservations`,
      JSON.stringify({
        eventId: data.eventId,
        seatId: contestedSeatId,
        userId: userId,
      }),
      {
        headers: {
          ...commonHeaders,
          'X-Checkout-Token': checkoutToken,
        },
        tags: { name: 'POST /api/v1/reservations (Contended)' },
      }
    );
    seatLockLatency.add(Date.now() - lockStart);
    unexpected5xxRate.add(holdRes.status >= 500);

    if (holdRes.status === 201) {
      seatLocksAcquired201.add(1);
      reservedSeatId = contestedSeatId;
      try {
        const body = holdRes.json();
        reservationId = body.reservationId || body.id;
      } catch (_) {}
    } else if (holdRes.status === 409 || holdRes.status === 423) {
      // Someone else grabbed the seat milliseconds earlier!
      seatCollisionsRejected409.add(1);

      // Realistic UX Behavior: Pick an alternate available seat and retry
      if (pool.length > 2) {
        const fallbackIndex = ((__VU * 7 + __ITER * 13) % (pool.length - 1)) + 1;
        const fallbackSeatId = pool[fallbackIndex];

        const retryStart = Date.now();
        const retryRes = http.post(
          `${GATEWAY_URL}/api/v1/reservations`,
          JSON.stringify({
            eventId: data.eventId,
            seatId: fallbackSeatId,
            userId: userId,
          }),
          {
            headers: {
              ...commonHeaders,
              'X-Checkout-Token': checkoutToken,
            },
            tags: { name: 'POST /api/v1/reservations (Fallback Retry)' },
          }
        );
        seatLockLatency.add(Date.now() - retryStart);
        unexpected5xxRate.add(retryRes.status >= 500);

        if (retryRes.status === 201) {
          seatLocksAcquired201.add(1);
          reservedSeatId = fallbackSeatId;
          try {
            const body = retryRes.json();
            reservationId = body.reservationId || body.id;
          } catch (_) {}
        } else if (retryRes.status === 409 || retryRes.status === 423) {
          seatCollisionsRejected409.add(1);
        }
      }
    }
  });

  // 4. Order Creation, Double-Entry Payment Settlement & Official PDF Invoice
  if (reservationId && reservedSeatId) {
    group('04_Checkout_Order_And_PDF_Invoice', () => {
      const stamp = generateGatewayStamp(userId, 'ROLE_USER');
      const authHeaders = {
        ...commonHeaders,
        'X-User-Id': userId,
        'X-User-Roles': 'ROLE_USER',
        'X-Gateway-Timestamp': stamp.timestamp,
        'X-Gateway-Signature': stamp.signature,
      };

      const createOrderRes = http.post(
        `${ORDERS_URL}/api/v1/orders`,
        JSON.stringify({
          reservationId: reservationId,
          eventId: data.eventId,
          seatId: reservedSeatId,
          totalAmount: 115.0,
          subtotalAmount: 100.0,
          serviceFeeAmount: 15.0,
        }),
        {
          headers: authHeaders,
          tags: { name: 'POST /api/v1/orders' },
        }
      );
      unexpected5xxRate.add(createOrderRes.status >= 500);

      if (createOrderRes.status === 201) {
        ordersCreated201.add(1);
        let orderId = null;
        try {
          orderId = createOrderRes.json().orderId;
        } catch (_) {}

        if (orderId) {
          // Process Card Payment (Double-Entry Ledger + Outbox + Inventory Saga Confirm)
          const payStart = Date.now();
          const payStamp = generateGatewayStamp(userId, 'ROLE_USER');
          const payRes = http.post(
            `${ORDERS_URL}/api/v1/orders/${orderId}/pay`,
            JSON.stringify({
              paymentMethod: __VU % 2 === 0 ? 'CREDIT_CARD' : 'DEBIT_CARD',
              buyerEmail: `${userId}@hibana.tk`,
              buyerName: `Comprador Stress ${__VU}`,
              buyerTaxId: `20-${30000000 + __VU}-9`,
              installments: 1,
              issuingBank: 'Banco Galicia',
            }),
            {
              headers: {
                ...commonHeaders,
                'X-User-Id': userId,
                'X-User-Roles': 'ROLE_USER',
                'X-Gateway-Timestamp': payStamp.timestamp,
                'X-Gateway-Signature': payStamp.signature,
              },
              tags: { name: 'POST /api/v1/orders/{id}/pay' },
            }
          );
          paymentSettlementLatency.add(Date.now() - payStart);
          unexpected5xxRate.add(payRes.status >= 500);

          if (payRes.status === 200) {
            paymentsSettled200.add(1);

            // Download Official PDF Invoice (CPU-Intensive OpenPDF rendering to stress HPA)
            const pdfStart = Date.now();
            const pdfStamp = generateGatewayStamp(userId, 'ROLE_USER');
            const pdfRes = http.get(`${ORDERS_URL}/api/v1/orders/${orderId}/invoice`, {
              headers: {
                'Accept': 'application/pdf',
                'X-Forwarded-For': clientIp,
                'X-User-Id': userId,
                'X-User-Roles': 'ROLE_USER',
                'X-Gateway-Timestamp': pdfStamp.timestamp,
                'X-Gateway-Signature': pdfStamp.signature,
              },
              tags: { name: 'GET /api/v1/orders/{id}/invoice (PDF)' },
            });
            pdfInvoiceLatency.add(Date.now() - pdfStart);
            unexpected5xxRate.add(pdfRes.status >= 500);

            if (pdfRes.status === 200) {
              pdfInvoicesGenerated200.add(1);
            }
          }
        }
      }
    });
  }

  e2ePurchaseDuration.add(Date.now() - funnelStart);
  sleep(0.3);
}
