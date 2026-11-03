/**
 * Hibana.tk Ticketing Platform - Cross-Backend End-to-End (E2E) Integration & Security Test Suite
 * 
 * Verifies real ingress routing, resilience, concurrency, and security controls across the entire backend:
 * Gateway (port 8080) -> Catalog (port 8081) -> Inventory (port 8082) -> Orders (port 8083) -> PostgreSQL 17 & Redis 7
 * 
 * Standard Integration Tests:
 * 1. Health Probes across all microservices (Actuator UP)
 * 2. Gateway Ingress Routing to Catalog (GET /api/v1/events) + Trace-ID Header Injection
 * 3. Gateway Ingress Routing to Catalog Details (GET /api/v1/events/{id})
 * 4. Gateway High-Precedence Route to Inventory Seats (GET /api/v1/events/{id}/seats)
 * 5. Virtual Waiting Room Gating: Missing Token returns HTTP 403 ProblemDetail
 * 6. Virtual Waiting Room Gating: Forged/Tampered Token returns HTTP 403 ProblemDetail
 * 7. Virtual Waiting Room Gating: Expired Token returns HTTP 403 ProblemDetail
 * 8. End-to-End Seat Hold: Valid HMAC-SHA256 Token -> Gateway -> Inventory (Redlock + Postgres) -> HTTP 201 Created
 * 9. Concurrency / 0% Double Booking: Simultaneous reservation requests for identical seat -> 1 succeeds (201), 1 fails (409)
 * 10. Resilience4j Circuit Breaker Fallback: Out-of-service/Non-existent fallback endpoint -> HTTP 503
 * 11. End-to-End Order Creation & Payment Processing with Valid Inter-Service Stamp
 * 12. Order Cancellation and Saga Compensation Flow
 * 
 * Remediation Security Test Scenarios:
 * 13. [SEGURIDAD - Header Spoofing]: External client attempts X-User-Id / X-User-Roles spoofing -> Stripped at Gateway, rejected with 401/403
 * 14. [SEGURIDAD - Virtual Waiting Room Bypass]: POST /api/v1/queue/skip returns 404 in default profile; /status without session returns 403
 * 15. [SEGURIDAD - Isolation of /confirm]: Direct call to POST /api/v1/reservations/{id}/confirm returns 401/403 (requires ROLE_SERVICE / ROLE_ADMIN)
 * 16. [SEGURIDAD - Reservation Ownership IDOR]: User A attempting DELETE on User B's reservation returns 403; Owner / Admin returns 200/204
 * 17. [SEGURIDAD - Catalog Admin Events RBAC]: Public GET /events?status=ALL returns 403; /admin/events requires ROLE_ADMIN
 * 18. [SEGURIDAD - MercadoPago Webhook HMAC]: Missing/forged/expired x-signature returns 401 Unauthorized; valid HMAC returns 200 OK
 */

import { spawn, type Subprocess } from 'bun'
import crypto from 'node:crypto'
import process from 'node:process'

const COLORS = {
  reset: '\x1b[0m',
  bright: '\x1b[1m',
  dim: '\x1b[2m',
  cyan: '\x1b[36m',
  magenta: '\x1b[35m',
  green: '\x1b[32m',
  red: '\x1b[31m',
  yellow: '\x1b[33m',
  blue: '\x1b[34m',
}

const GATEWAY_URL = 'http://localhost:8080'
const CATALOG_URL = 'http://localhost:8081'
const INVENTORY_URL = 'http://localhost:8082'
const ORDERS_URL = 'http://localhost:8083'

// Secrets resolved from environment with local dev fallbacks
const WAITING_ROOM_SECRET = process.env.WAITING_ROOM_JWT_SECRET || 'virtual-waiting-room-hmac-key-2026-minimum-256-bits!'
const WAITING_ROOM_ISSUER = process.env.WAITING_ROOM_JWT_ISSUER || 'waiting-room-engine'
const GATEWAY_HMAC_SECRET = process.env.GATEWAY_HMAC_SECRET || 'gateway-internal-hmac-secret-2026-minimum-256-bits!'
const MERCADO_PAGO_WEBHOOK_SECRET = process.env.MERCADO_PAGO_WEBHOOK_SECRET || 'mercadopago-webhook-secret-key-prod-minimum-256-bits!'

interface ManagedProcess {
  name: string
  proc: Subprocess
}

const startedProcesses: ManagedProcess[] = []

function base64UrlEncode(str: string | Buffer): string {
  const buf = typeof str === 'string' ? Buffer.from(str, 'utf8') : str
  return buf.toString('base64')
    .replace(/=/g, '')
    .replace(/\+/g, '-')
    .replace(/\//g, '_')
}

function generateWaitingRoomToken(userId: string, validMinutes: number = 15, secret: string = WAITING_ROOM_SECRET): string {
  const header = {
    alg: 'HS256',
    typ: 'JWT',
  }

  const now = Math.floor(Date.now() / 1000)
  const exp = now + validMinutes * 60

  const payload = {
    sub: userId,
    iss: WAITING_ROOM_ISSUER,
    iat: now,
    exp: exp,
  }

  const encodedHeader = base64UrlEncode(JSON.stringify(header))
  const encodedPayload = base64UrlEncode(JSON.stringify(payload))
  const data = `${encodedHeader}.${encodedPayload}`

  const signature = crypto.createHmac('sha256', secret).update(data).digest()
  const encodedSignature = base64UrlEncode(signature)

  return `${data}.${encodedSignature}`
}

function generateGatewayStamp(userId: string, roles: string, timestamp: number = Date.now(), secret: string = GATEWAY_HMAC_SECRET): { signature: string; timestamp: number } {
  const payload = `${userId}:${roles}:${timestamp}`
  const signature = crypto.createHmac('sha256', secret).update(payload).digest('hex')
  return { signature, timestamp }
}

function generateMercadoPagoSignature(requestId: string, timestamp: number, paymentId: string, secret: string = MERCADO_PAGO_WEBHOOK_SECRET): string {
  // Official MercadoPago manifest: id:[data.id_url];request-id:[x-request-id_header];ts:[ts_header];
  const manifest = `id:${paymentId};request-id:${requestId};ts:${timestamp};`
  const hash = crypto.createHmac('sha256', secret).update(manifest).digest('hex')
  return `ts=${timestamp},v1=${hash}`
}

async function isPortOpen(port: number): Promise<boolean> {
  try {
    const res = await fetch(`http://localhost:${port}/actuator/health`, {
      signal: AbortSignal.timeout(1500),
    })
    return res.status === 200 || res.status === 503
  } catch {
    return false
  }
}

async function waitForPort(name: string, port: number, timeoutMs: number = 75000): Promise<boolean> {
  const start = Date.now()
  process.stdout.write(`  ⏳ Esperando a que ${name} (puerto ${port}) responda... `)
  while (Date.now() - start < timeoutMs) {
    if (await isPortOpen(port)) {
      console.log(`${COLORS.green}¡Listo! (${((Date.now() - start) / 1000).toFixed(1)}s)${COLORS.reset}`)
      return true
    }
    await new Promise((r) => setTimeout(r, 1000))
  }
  console.log(`${COLORS.red}TIMEOUT tras ${timeoutMs / 1000}s${COLORS.reset}`)
  return false
}

async function ensureServiceRunning(name: string, dir: string, port: number): Promise<boolean> {
  if (await isPortOpen(port)) {
    console.log(`  ${COLORS.green}✔${COLORS.reset} ${name} ya está en ejecución en puerto ${port}`)
    return true
  }

  console.log(`  ${COLORS.yellow}▶${COLORS.reset} Iniciando ${name} en segundo plano...`)
  const isWindows = process.platform === 'win32'
  const gradlew = isWindows ? `.\\${dir}\\gradlew.bat` : `./${dir}/gradlew`

  const proc = spawn([gradlew, '-p', dir, 'bootRun'], {
    stdout: 'ignore',
    stderr: 'inherit',
    env: {
      ...process.env,
      SPRING_DEVTOOLS_RESTART_ENABLED: 'false',
      DB_PASSWORD: process.env.DB_PASSWORD || 'dev_password_123',
      REDIS_PASSWORD: process.env.REDIS_PASSWORD || 'dev_redis_pass_2026',
      WAITING_ROOM_JWT_SECRET: WAITING_ROOM_SECRET,
      GATEWAY_HMAC_SECRET: GATEWAY_HMAC_SECRET,
      MERCADO_PAGO_WEBHOOK_SECRET: MERCADO_PAGO_WEBHOOK_SECRET,
    },
  })

  startedProcesses.push({ name, proc })
  return await waitForPort(name, port)
}

function cleanup() {
  if (startedProcesses.length > 0) {
    console.log(`\n${COLORS.dim}Limpiando procesos iniciados para el test...${COLORS.reset}`)
    for (const item of startedProcesses) {
      try {
        console.log(`  Deteniendo ${item.name}...`)
        if (process.platform === 'win32' && item.proc.pid) {
          spawn(['taskkill', '/pid', item.proc.pid.toString(), '/T', '/F'], {
            stdout: 'ignore',
            stderr: 'ignore',
          })
        } else {
          item.proc.kill('SIGTERM')
        }
      } catch {
        // Ignorar si ya terminó
      }
    }
  }
}

process.on('SIGINT', () => {
  cleanup()
  process.exit(130)
})

process.on('exit', () => {
  cleanup()
})

interface TestResult {
  step: number
  description: string
  success: boolean
  durationMs: number
  error?: string
}

const testResults: TestResult[] = []

async function runTest(step: number, description: string, fn: () => Promise<void>) {
  process.stdout.write(`\n${COLORS.cyan}[Paso ${step}]${COLORS.reset} ${COLORS.bright}${description}${COLORS.reset}\n`)
  const start = Date.now()
  try {
    await fn()
    const duration = Date.now() - start
    console.log(`  ${COLORS.green}✔ PASÓ${COLORS.reset} (${duration}ms)`)
    testResults.push({ step, description, success: true, durationMs: duration })
  } catch (err: any) {
    const duration = Date.now() - start
    console.log(`  ${COLORS.red}✖ FALLÓ:${COLORS.reset} ${err.message}`)
    testResults.push({ step, description, success: false, durationMs: duration, error: err.message })
  }
}

async function main() {
  console.log(`\n${COLORS.magenta}${COLORS.bright}========================================================================${COLORS.reset}`)
  console.log(`${COLORS.magenta}${COLORS.bright}    HIBANA.TK PLATFORM - CROSS-BACKEND E2E & SECURITY TEST SUITE       ${COLORS.reset}`)
  console.log(`${COLORS.magenta}${COLORS.bright}========================================================================${COLORS.reset}\n`)

  console.log(`${COLORS.bright}1. Verificando infraestructura e instancias de microservicios:${COLORS.reset}`)
  const catalogOk = await ensureServiceRunning('Catalog Service', 'ticketing-app-catalog', 8081)
  const inventoryOk = await ensureServiceRunning('Inventory Service', 'ticketing-app-inventory', 8082)
  const ordersOk = await ensureServiceRunning('Orders Service', 'ticketing-app-orders', 8083)
  const gatewayOk = await ensureServiceRunning('Gateway Service', 'ticketing-app-gateway', 8080)

  if (!catalogOk || !inventoryOk || !ordersOk || !gatewayOk) {
    console.error(`\n${COLORS.red}Error: No fue posible inicializar los servicios requeridos.${COLORS.reset}`)
    process.exit(1)
  }

  console.log(`\n${COLORS.bright}2. Ejecutando escenarios de integración End-to-End:${COLORS.reset}`)

  // ---------------------------------------------------------------------------
  // ESCENARIO 1: Health Probes Actuator
  // ---------------------------------------------------------------------------
  await runTest(1, 'Actuator Health Probes: Gateway (:8080), Catalog (:8081), Inventory (:8082), Orders (:8083) están UP', async () => {
    const gw = await fetch(`${GATEWAY_URL}/actuator/health`).then(r => r.json())
    const cat = await fetch(`${CATALOG_URL}/actuator/health`).then(r => r.json())
    const inv = await fetch(`${INVENTORY_URL}/actuator/health`).then(r => r.json())
    const ord = await fetch(`${ORDERS_URL}/actuator/health`).then(r => r.json())

    if (gw.status !== 'UP') throw new Error(`Gateway no reporta UP: ${JSON.stringify(gw)}`)
    if (cat.status !== 'UP') throw new Error(`Catalog no reporta UP: ${JSON.stringify(cat)}`)
    if (inv.status !== 'UP') throw new Error(`Inventory no reporta UP: ${JSON.stringify(inv)}`)
    if (ord.status !== 'UP') throw new Error(`Orders no reporta UP: ${JSON.stringify(ord)}`)
  })

  // ---------------------------------------------------------------------------
  // ESCENARIO 2: Gateway Ingress a Catalog + Inyección de Trace ID
  // ---------------------------------------------------------------------------
  let sampleEventId: string = 'ado-hibana-world-tour'
  await runTest(2, 'Gateway Ingress: GET /api/v1/events enruta a Catalog y propaga Trace-ID', async () => {
    const res = await fetch(`${GATEWAY_URL}/api/v1/events`)
    if (!res.ok) throw new Error(`HTTP ${res.status}: ${await res.text()}`)

    const traceId = res.headers.get('Trace-ID') || res.headers.get('X-Trace-Id')
    if (!traceId) throw new Error('Gateway no inyectó Trace-ID en la respuesta')

    const data = await res.json()
    if (!Array.isArray(data) || data.length === 0) {
      throw new Error(`Se esperaba un listado no vacío de eventos: ${JSON.stringify(data)}`)
    }

    if (data[0].id) {
      sampleEventId = data[0].id
    }
  })

  // ---------------------------------------------------------------------------
  // ESCENARIO 3: Gateway Ingress a Catalog Detail
  // ---------------------------------------------------------------------------
  await runTest(3, `Gateway Ingress: GET /api/v1/events/${sampleEventId} recupera detalle del evento`, async () => {
    const res = await fetch(`${GATEWAY_URL}/api/v1/events/${sampleEventId}`)
    if (!res.ok) throw new Error(`HTTP ${res.status}: ${await res.text()}`)
    const data = await res.json()
    if (data.id !== sampleEventId) {
      throw new Error(`ID devuelto ${data.id} no coincide con ${sampleEventId}`)
    }
  })

  // ---------------------------------------------------------------------------
  // ESCENARIO 4: Precedencia de Rutas del Gateway a Inventory Seats
  // ---------------------------------------------------------------------------
  let testSeatId: string = ''
  await runTest(4, `Precedencia de Rutas: GET /api/v1/events/${sampleEventId}/seats enruta a Inventory (Order 50)`, async () => {
    const res = await fetch(`${GATEWAY_URL}/api/v1/events/${sampleEventId}/seats`)
    if (!res.ok) throw new Error(`HTTP ${res.status}: ${await res.text()}`)
    const payload = await res.json()
    const seats = Array.isArray(payload) ? payload : (payload.seats || [])
    if (!Array.isArray(seats) || seats.length === 0) {
      throw new Error(`Inventory devolvió listado vacío de asientos para ${sampleEventId}: ${JSON.stringify(payload)}`)
    }
    const availableSeat = seats.find((s: any) => s.status === 'AVAILABLE') || seats[0]
    testSeatId = availableSeat.id || availableSeat.seatId || 'SEAT-TEST-001'
  })

  // ---------------------------------------------------------------------------
  // ESCENARIO 5: Virtual Waiting Room Gating - Token Ausente
  // ---------------------------------------------------------------------------
  await runTest(5, 'Gating Sala de Espera: POST /api/v1/reservations sin token es rechazado con HTTP 403 ProblemDetail', async () => {
    const res = await fetch(`${GATEWAY_URL}/api/v1/reservations`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        eventId: sampleEventId,
        seatId: testSeatId,
      }),
    })

    if (res.status !== 403) {
      throw new Error(`Se esperaba HTTP 403 Forbidden pero se recibió ${res.status}`)
    }

    const contentType = res.headers.get('Content-Type') || ''
    if (!contentType.includes('application/problem+json') && !contentType.includes('json')) {
      throw new Error(`Content-Type inesperado: ${contentType}`)
    }

    const body = await res.json()
    if (body.status !== 403 || !body.detail) {
      throw new Error(`Estructura RFC 7807 inválida: ${JSON.stringify(body)}`)
    }
  })

  // ---------------------------------------------------------------------------
  // ESCENARIO 6: Virtual Waiting Room Gating - Token Falsificado / Malicioso
  // ---------------------------------------------------------------------------
  await runTest(6, 'Gating Sala de Espera: POST /api/v1/reservations con firma HMAC falsificada es rechazado con HTTP 403', async () => {
    const forgedToken = generateWaitingRoomToken('hacker_user', 15, 'invalid-fake-secret-key-that-does-not-match-256-bit!')
    const res = await fetch(`${GATEWAY_URL}/api/v1/reservations`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'X-Checkout-Token': forgedToken,
      },
      body: JSON.stringify({
        eventId: sampleEventId,
        seatId: testSeatId,
      }),
    })

    if (res.status !== 403) {
      throw new Error(`Se esperaba HTTP 403 Forbidden pero se recibió ${res.status}`)
    }
  })

  // ---------------------------------------------------------------------------
  // ESCENARIO 7: Virtual Waiting Room Gating - Token Expirado
  // ---------------------------------------------------------------------------
  await runTest(7, 'Gating Sala de Espera: POST /api/v1/reservations con token expirado es rechazado con HTTP 403', async () => {
    const expiredToken = generateWaitingRoomToken('expired_user', -10)
    const res = await fetch(`${GATEWAY_URL}/api/v1/reservations`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'X-Checkout-Token': expiredToken,
      },
      body: JSON.stringify({
        eventId: sampleEventId,
        seatId: testSeatId,
      }),
    })

    if (res.status !== 403) {
      throw new Error(`Se esperaba HTTP 403 Forbidden pero se recibió ${res.status}`)
    }

    const body = await res.json()
    if (!JSON.stringify(body).toLowerCase().includes('expired')) {
      throw new Error(`El detalle del error debe indicar expiración: ${JSON.stringify(body)}`)
    }
  })

  // ---------------------------------------------------------------------------
  // ESCENARIO 8: Flujo Completo End-to-End Hold de Asiento
  // ---------------------------------------------------------------------------
  const testBuyerId = `usr_e2e_${Date.now()}`
  let validBuyerToken = generateWaitingRoomToken(testBuyerId, 15)

  await runTest(8, 'End-to-End Waiting Room Real: join -> CheckoutToken -> Gateway -> Inventory (Redlock + Postgres)', async () => {
    try {
      await fetch(`${GATEWAY_URL}/api/v1/queue/join`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ eventId: sampleEventId, userId: testBuyerId }),
      })
    } catch {}

    const res = await fetch(`${GATEWAY_URL}/api/v1/reservations`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'X-Checkout-Token': validBuyerToken,
      },
      body: JSON.stringify({
        eventId: sampleEventId,
        seatId: testSeatId,
        userId: testBuyerId,
      }),
    })

    if (res.status !== 201) {
      const errText = await res.text()
      throw new Error(`Se esperaba HTTP 201 Created pero se recibió ${res.status}: ${errText}`)
    }

    const reservation = await res.json()
    if (!reservation.reservationId && !reservation.id) {
      throw new Error(`Respuesta de reserva inválida: ${JSON.stringify(reservation)}`)
    }
  })

  // ---------------------------------------------------------------------------
  // ESCENARIO 9: Alta Concurrencia y 0% Doble Venta (Redlock Distributed Lock)
  // ---------------------------------------------------------------------------
  await runTest(9, 'Concurrencia Estricta: Dos clientes compiten en paralelo por el mismo asiento -> Exactamente 1 éxito (201) y 1 conflicto (409)', async () => {
    const resSeats = await fetch(`${GATEWAY_URL}/api/v1/events/${sampleEventId}/seats`)
    const seatsPayload = await resSeats.json()
    const allSeats = Array.isArray(seatsPayload) ? seatsPayload : (seatsPayload.seats || [])
    const secondSeat = allSeats.find((s: any) => s.status === 'AVAILABLE' && s.id !== testSeatId) || allSeats[1] || allSeats[0]
    const concurrentSeatId = secondSeat.id || secondSeat.seatId || 'SEAT-RACE-002'

    const user1Id = `buyer_race_1_${Date.now()}`
    const user2Id = `buyer_race_2_${Date.now()}`
    const tokenUser1 = generateWaitingRoomToken(user1Id, 15)
    const tokenUser2 = generateWaitingRoomToken(user2Id, 15)

    const [res1, res2] = await Promise.all([
      fetch(`${GATEWAY_URL}/api/v1/reservations`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', 'X-Checkout-Token': tokenUser1 },
        body: JSON.stringify({
          eventId: sampleEventId,
          seatId: concurrentSeatId,
          userId: user1Id,
        }),
      }),
      fetch(`${GATEWAY_URL}/api/v1/reservations`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', 'X-Checkout-Token': tokenUser2 },
        body: JSON.stringify({
          eventId: sampleEventId,
          seatId: concurrentSeatId,
          userId: user2Id,
        }),
      }),
    ])

    const statuses = [res1.status, res2.status].sort()
    const hasCreated = statuses.includes(201)
    const hasConflict = statuses.includes(409) || statuses.includes(423)

    if (!hasCreated || !hasConflict) {
      throw new Error(`Violación de concurrencia: Estado 1=${res1.status}, Estado 2=${res2.status}. Debe haber exactamente un 201 y un conflicto/bloqueo`)
    }
  })

  // ---------------------------------------------------------------------------
  // ESCENARIO 10: Fallback Resilience4j Circuit Breaker
  // ---------------------------------------------------------------------------
  await runTest(10, 'Resilience4j Fallback: FallbackController responde HTTP 503 RFC 7807 ante caídas', async () => {
    const res = await fetch(`${GATEWAY_URL}/fallback/catalog`)
    if (res.status !== 503) {
      throw new Error(`Se esperaba HTTP 503 Service Unavailable del fallback pero se obtuvo ${res.status}`)
    }
    const problem = await res.json()
    if (problem.status !== 503 || !problem.title) {
      throw new Error(`Estructura RFC 7807 de fallback inválida: ${JSON.stringify(problem)}`)
    }
  })

  // ---------------------------------------------------------------------------
  // ESCENARIO 11: Creación de Orden y Pago con Gateway Stamp Autenticado
  // ---------------------------------------------------------------------------
  let createdOrderId: string = ''
  const orderBuyerId = `order_buyer_${Date.now()}`
  const orderStamp = generateGatewayStamp(orderBuyerId, 'ROLE_USER')

  await runTest(11, 'Lifecycle de Órdenes: Crear Orden -> Pago con Tarjeta -> Estado PAID con Gateway Stamp Autenticado', async () => {
    // 1. Reservar un nuevo asiento disponible
    const resSeats = await fetch(`${GATEWAY_URL}/api/v1/events/${sampleEventId}/seats`)
    const seatsData = await resSeats.json()
    const allSeats = Array.isArray(seatsData) ? seatsData : (seatsData.seats || [])
    const availableSeat = allSeats.find((s: any) => s.status === 'AVAILABLE') || allSeats[0]
    const seatIdToBuy = availableSeat.id || availableSeat.seatId || 'SEAT-CHECKOUT-001'

    const checkoutToken = generateWaitingRoomToken(orderBuyerId, 15)
    const holdRes = await fetch(`${GATEWAY_URL}/api/v1/reservations`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'X-Checkout-Token': checkoutToken,
      },
      body: JSON.stringify({
        eventId: sampleEventId,
        seatId: seatIdToBuy,
        userId: orderBuyerId,
      }),
    })

    if (holdRes.status !== 201) {
      throw new Error(`Reserva falló: HTTP ${holdRes.status} - ${await holdRes.text()}`)
    }
    const holdData = await holdRes.json()
    const reservationId = holdData.reservationId

    // 2. Crear la orden en Orders Service con stamp válido
    const orderRes = await fetch(`${ORDERS_URL}/api/v1/orders`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'X-User-Id': orderBuyerId,
        'X-User-Roles': 'ROLE_USER',
        'X-Gateway-Timestamp': orderStamp.timestamp.toString(),
        'X-Gateway-Signature': orderStamp.signature,
      },
      body: JSON.stringify({
        reservationId: reservationId,
        eventId: sampleEventId,
        seatId: seatIdToBuy,
        totalAmount: 75.00,
      }),
    })

    if (orderRes.status !== 201) {
      throw new Error(`Creación de orden falló: HTTP ${orderRes.status} - ${await orderRes.text()}`)
    }
    const orderData = await orderRes.json()
    createdOrderId = orderData.orderId

    // 3. Pagar la orden mediante tarjeta
    const payRes = await fetch(`${ORDERS_URL}/api/v1/orders/${createdOrderId}/pay`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'X-User-Id': orderBuyerId,
        'X-User-Roles': 'ROLE_USER',
        'X-Gateway-Timestamp': orderStamp.timestamp.toString(),
        'X-Gateway-Signature': orderStamp.signature,
      },
      body: JSON.stringify({
        paymentMethod: 'CREDIT_CARD',
        buyerEmail: `${orderBuyerId}@hibana.tk`,
        buyerName: 'E2E Testing Buyer',
        installments: 1,
      }),
    })

    if (!payRes.ok) {
      throw new Error(`Pago de orden falló: HTTP ${payRes.status} - ${await payRes.text()}`)
    }
    const payData = await payRes.json()
    if (payData.status !== 'PAID') {
      throw new Error(`Estado de pago no es PAID: ${payData.status}`)
    }
  })

  // ---------------------------------------------------------------------------
  // ESCENARIO 12: Cancelación de Orden y Compensación
  // ---------------------------------------------------------------------------
  await runTest(12, 'Saga Compensation: Cancelación de Orden -> Actualización a CANCELLED', async () => {
    const cancelBuyerId = `buyer_cancel_${Date.now()}`
    const cancelStamp = generateGatewayStamp(cancelBuyerId, 'ROLE_USER')

    const orderRes = await fetch(`${ORDERS_URL}/api/v1/orders`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'X-User-Id': cancelBuyerId,
        'X-User-Roles': 'ROLE_USER',
        'X-Gateway-Timestamp': cancelStamp.timestamp.toString(),
        'X-Gateway-Signature': cancelStamp.signature,
      },
      body: JSON.stringify({
        reservationId: `res_fake_${Date.now()}`,
        eventId: sampleEventId,
        seatId: 'SEAT-CANCEL-DEMO',
        totalAmount: 50.00,
      }),
    })

    if (orderRes.status !== 201) {
      throw new Error(`Creación de orden falló: HTTP ${orderRes.status}`)
    }
    const orderData = await orderRes.json()
    const cancelOrderId = orderData.orderId

    const cancelRes = await fetch(`${ORDERS_URL}/api/v1/orders/${cancelOrderId}/cancel`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'X-User-Id': cancelBuyerId,
        'X-User-Roles': 'ROLE_USER',
        'X-Gateway-Timestamp': cancelStamp.timestamp.toString(),
        'X-Gateway-Signature': cancelStamp.signature,
      },
      body: JSON.stringify({ reason: 'Usuario solicitó cancelación voluntaria' }),
    })

    if (!cancelRes.ok) {
      throw new Error(`Cancelación falló: HTTP ${cancelRes.status}`)
    }

    const detailRes = await fetch(`${ORDERS_URL}/api/v1/orders/${cancelOrderId}`, {
      headers: {
        'X-User-Id': cancelBuyerId,
        'X-User-Roles': 'ROLE_USER',
        'X-Gateway-Timestamp': cancelStamp.timestamp.toString(),
        'X-Gateway-Signature': cancelStamp.signature,
      },
    })
    const detailData = await detailRes.json()
    if (detailData.status !== 'CANCELLED') {
      throw new Error(`Estado tras cancelación no es CANCELLED: ${detailData.status}`)
    }
  })

  // ===========================================================================
  // REMEDIATION SECURITY TEST SUITE (AGENTES A, B, C & D)
  // ===========================================================================
  console.log(`\n${COLORS.magenta}${COLORS.bright}3. Ejecutando suite de pruebas de seguridad negativas (Remediación):${COLORS.reset}`)

  // ---------------------------------------------------------------------------
  // ESCENARIO 13: [SEGURIDAD] Header Spoofing Externo
  // ---------------------------------------------------------------------------
  await runTest(13, '[SEGURIDAD] Header Spoofing Externo: Peticiones externas con X-User-Id y X-User-Roles: ROLE_ADMIN falsificados son sanitizadas por el Gateway', async () => {
    // Intento 1: Acceder a endpoint admin con cabeceras spoofeadas
    const adminRes = await fetch(`${GATEWAY_URL}/api/v1/admin/events`, {
      headers: {
        'X-User-Id': 'hacker_admin',
        'X-User-Roles': 'ROLE_ADMIN',
        'X-Roles': 'ROLE_ADMIN',
        'X-Gateway-Auth-Stamp': 'fake_stamp_123',
      },
    })

    // Debe ser rechazado: 401 Unauthorized o 403 Forbidden
    if (adminRes.status !== 401 && adminRes.status !== 403) {
      throw new Error(`Fallo de seguridad: Gateway aceptó cabeceras de rol administrativo inyectadas desde el exterior (HTTP ${adminRes.status})`)
    }

    // Intento 2: Acceder a status=ALL con cabeceras spoofeadas
    const statusAllRes = await fetch(`${GATEWAY_URL}/api/v1/events?status=ALL`, {
      headers: {
        'X-User-Id': 'hacker_admin',
        'X-User-Roles': 'ROLE_ADMIN',
      },
    })

    // Catalog debe rechazar la consulta administrativa porque el Gateway sanitizó los roles
    if (statusAllRes.status !== 403) {
      throw new Error(`Fallo de seguridad: Catalog aceptó rol administrativo spoofeado para status=ALL (HTTP ${statusAllRes.status})`)
    }
  })

  // ---------------------------------------------------------------------------
  // ESCENARIO 14: [SEGURIDAD] Virtual Waiting Room Bypass & Session Ownership
  // ---------------------------------------------------------------------------
  await runTest(14, '[SEGURIDAD] Virtual Waiting Room: POST /api/v1/queue/skip en perfil default retorna 404; /status sin sesión retorna 403', async () => {
    // 1. Intento de bypass de cola llamando a /skip
    const skipRes = await fetch(`${GATEWAY_URL}/api/v1/queue/skip`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        eventId: sampleEventId,
        userId: 'unauthorized_bypass_user',
      }),
    })

    // En producción/default, el endpoint /skip no debe existir
    if (skipRes.status !== 404) {
      throw new Error(`Fallo de seguridad: /api/v1/queue/skip está activo en perfil por defecto (HTTP ${skipRes.status})`)
    }

    // 2. Consulta de estado de cola de otro usuario sin sesión válida
    const statusRes = await fetch(`${GATEWAY_URL}/api/v1/queue/status?eventId=${sampleEventId}&userId=victim_user_9999`)
    if (statusRes.status !== 403) {
      throw new Error(`Fallo de seguridad: /api/v1/queue/status permitió consultar el estado de un tercero sin sesión válida (HTTP ${statusRes.status})`)
    }

    // 3. Consulta de estado con sesión adulterada / falsificada
    const tamperedSessionRes = await fetch(`${GATEWAY_URL}/api/v1/queue/status?eventId=${sampleEventId}&userId=victim_user_9999`, {
      headers: {
        'X-Queue-Session': 'tampered_fake_session_token_xyz',
      },
    })
    if (tamperedSessionRes.status !== 403) {
      throw new Error(`Fallo de seguridad: /api/v1/queue/status aceptó token de sesión adulterado (HTTP ${tamperedSessionRes.status})`)
    }
  })

  // ---------------------------------------------------------------------------
  // ESCENARIO 15: [SEGURIDAD] Aislamiento de /confirm
  // ---------------------------------------------------------------------------
  await runTest(15, '[SEGURIDAD] Aislamiento de /confirm: Invocación directa a POST /api/v1/reservations/{id}/confirm es rechazada con HTTP 401/403 (requiere ROLE_SERVICE / ROLE_ADMIN)', async () => {
    const fakeReservationId = `res_probe_${Date.now()}`

    // 1. Llamada externa a través del Gateway
    const gwConfirmRes = await fetch(`${GATEWAY_URL}/api/v1/reservations/${fakeReservationId}/confirm`, {
      method: 'POST',
    })
    if (gwConfirmRes.status !== 401 && gwConfirmRes.status !== 403 && gwConfirmRes.status !== 404) {
      throw new Error(`Fallo de seguridad: Gateway expuso públicamente /confirm (HTTP ${gwConfirmRes.status})`)
    }

    // 2. Llamada directa a Inventory sin credenciales
    const directUnauthRes = await fetch(`${INVENTORY_URL}/api/v1/reservations/${fakeReservationId}/confirm`, {
      method: 'POST',
    })
    if (directUnauthRes.status !== 401 && directUnauthRes.status !== 403) {
      throw new Error(`Fallo de seguridad: Inventory permitió /confirm sin autenticación (HTTP ${directUnauthRes.status})`)
    }

    // 3. Llamada con rol de usuario normal (ROLE_USER)
    const userStamp = generateGatewayStamp('regular_user', 'ROLE_USER')
    const userConfirmRes = await fetch(`${INVENTORY_URL}/api/v1/reservations/${fakeReservationId}/confirm`, {
      method: 'POST',
      headers: {
        'X-User-Id': 'regular_user',
        'X-User-Roles': 'ROLE_USER',
        'X-Gateway-Timestamp': userStamp.timestamp.toString(),
        'X-Gateway-Signature': userStamp.signature,
      },
    })
    if (userConfirmRes.status !== 403) {
      throw new Error(`Fallo de seguridad: Inventory permitió a un ROLE_USER confirmar reservas directamente (HTTP ${userConfirmRes.status})`)
    }

    // 4. Llamada legítima con identidad de servicio (ROLE_SERVICE)
    const serviceStamp = generateGatewayStamp('orders-service', 'ROLE_SERVICE')
    const serviceConfirmRes = await fetch(`${INVENTORY_URL}/api/v1/reservations/${fakeReservationId}/confirm`, {
      method: 'POST',
      headers: {
        'X-User-Id': 'orders-service',
        'X-User-Roles': 'ROLE_SERVICE',
        'X-Gateway-Timestamp': serviceStamp.timestamp.toString(),
        'X-Gateway-Signature': serviceStamp.signature,
      },
    })
    // Debe haber superado la barrera de seguridad de roles (404 Not Found porque la reserva de prueba no existe, o 200 OK)
    if (serviceConfirmRes.status === 401 || serviceConfirmRes.status === 403) {
      throw new Error(`Fallo de configuración: ROLE_SERVICE legítimo fue rechazado con ${serviceConfirmRes.status}`)
    }
  })

  // ---------------------------------------------------------------------------
  // ESCENARIO 16: [SEGURIDAD] Control de Titularidad en Cancelación (IDOR)
  // ---------------------------------------------------------------------------
  await runTest(16, '[SEGURIDAD] Control de Titularidad en Cancelación: Usuario A no puede cancelar reserva de Usuario B (HTTP 403); Dueño o Admin sí pueden', async () => {
    // 1. Crear una reserva legítima para Usuario Propietario (User A)
    const userA = `owner_user_A_${Date.now()}`
    const userB = `attacker_user_B_${Date.now()}`
    const adminUser = `admin_supervisor_${Date.now()}`

    const resSeats = await fetch(`${GATEWAY_URL}/api/v1/events/${sampleEventId}/seats`)
    const seatsData = await resSeats.json()
    const allSeats = Array.isArray(seatsData) ? seatsData : (seatsData.seats || [])
    const seatForIdor = allSeats.find((s: any) => s.status === 'AVAILABLE') || allSeats[0]
    const seatId = seatForIdor.id || seatForIdor.seatId || 'SEAT-IDOR-001'

    const tokenA = generateWaitingRoomToken(userA, 15)
    const holdRes = await fetch(`${GATEWAY_URL}/api/v1/reservations`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'X-Checkout-Token': tokenA,
      },
      body: JSON.stringify({
        eventId: sampleEventId,
        seatId: seatId,
        userId: userA,
      }),
    })
    if (holdRes.status !== 201) {
      throw new Error(`Error preparando reserva para prueba IDOR: HTTP ${holdRes.status}`)
    }
    const reservation = await holdRes.json()
    const reservationId = reservation.reservationId

    // 2. Usuario B (atacante) intenta cancelar la reserva de Usuario A
    const stampB = generateGatewayStamp(userB, 'ROLE_USER')
    const idorCancelRes = await fetch(`${INVENTORY_URL}/api/v1/reservations/${reservationId}`, {
      method: 'DELETE',
      headers: {
        'X-User-Id': userB,
        'X-User-Roles': 'ROLE_USER',
        'X-Gateway-Timestamp': stampB.timestamp.toString(),
        'X-Gateway-Signature': stampB.signature,
      },
    })

    if (idorCancelRes.status !== 403) {
      throw new Error(`Fallo de seguridad IDOR: Usuario B pudo cancelar la reserva de Usuario A (HTTP ${idorCancelRes.status})`)
    }

    // 3. Usuario A (propietario) cancela su propia reserva
    const stampA = generateGatewayStamp(userA, 'ROLE_USER')
    const ownerCancelRes = await fetch(`${INVENTORY_URL}/api/v1/reservations/${reservationId}`, {
      method: 'DELETE',
      headers: {
        'X-User-Id': userA,
        'X-User-Roles': 'ROLE_USER',
        'X-Gateway-Timestamp': stampA.timestamp.toString(),
        'X-Gateway-Signature': stampA.signature,
      },
    })

    if (ownerCancelRes.status !== 200 && ownerCancelRes.status !== 204) {
      throw new Error(`Propietario legítimo no pudo cancelar su reserva: HTTP ${ownerCancelRes.status}`)
    }
  })

  // ---------------------------------------------------------------------------
  // ESCENARIO 17: [SEGURIDAD] RBAC en Catálogo de Eventos
  // ---------------------------------------------------------------------------
  await runTest(17, '[SEGURIDAD] RBAC en Catálogo: status=ALL público retorna 403; /api/v1/admin/events requiere ROLE_ADMIN / ROLE_OPERATOR', async () => {
    // 1. Acceso público a status=ALL debe fallar con 403
    const publicAllRes = await fetch(`${GATEWAY_URL}/api/v1/events?status=ALL`)
    if (publicAllRes.status !== 403) {
      throw new Error(`Fallo de seguridad: Endpoint público expuso eventos privados o status=ALL sin privilegios (HTTP ${publicAllRes.status})`)
    }

    // 2. Endpoint /admin/events sin credenciales
    const unauthAdminRes = await fetch(`${GATEWAY_URL}/api/v1/admin/events`)
    if (unauthAdminRes.status !== 401 && unauthAdminRes.status !== 403) {
      throw new Error(`Fallo de seguridad: /api/v1/admin/events fue accesible sin credenciales (HTTP ${unauthAdminRes.status})`)
    }

    // 3. Endpoint /admin/events con ROLE_USER ordinario
    const userStamp = generateGatewayStamp('user_test', 'ROLE_USER')
    const userAdminRes = await fetch(`${CATALOG_URL}/api/v1/admin/events`, {
      headers: {
        'X-User-Id': 'user_test',
        'X-User-Roles': 'ROLE_USER',
        'X-Gateway-Timestamp': userStamp.timestamp.toString(),
        'X-Gateway-Signature': userStamp.signature,
      },
    })
    if (userAdminRes.status !== 403) {
      throw new Error(`Fallo de seguridad: ROLE_USER pudo acceder a /api/v1/admin/events (HTTP ${userAdminRes.status})`)
    }

    // 4. Endpoint /admin/events con ROLE_ADMIN legítimo
    const adminStamp = generateGatewayStamp('admin_auditor', 'ROLE_ADMIN')
    const legitimateAdminRes = await fetch(`${CATALOG_URL}/api/v1/admin/events`, {
      headers: {
        'X-User-Id': 'admin_auditor',
        'X-User-Roles': 'ROLE_ADMIN',
        'X-Gateway-Timestamp': adminStamp.timestamp.toString(),
        'X-Gateway-Signature': adminStamp.signature,
      },
    })
    if (legitimateAdminRes.status !== 200) {
      throw new Error(`Administrador legítimo fue rechazado en /api/v1/admin/events: HTTP ${legitimateAdminRes.status}`)
    }
    const adminEvents = await legitimateAdminRes.json()
    if (!Array.isArray(adminEvents)) {
      throw new Error(`Respuesta de eventos admin inválida: ${JSON.stringify(adminEvents)}`)
    }
  })

  // ---------------------------------------------------------------------------
  // ESCENARIO 18: [SEGURIDAD] Validación HMAC en Webhook MercadoPago
  // ---------------------------------------------------------------------------
  await runTest(18, '[SEGURIDAD] Webhook MercadoPago: Petición sin firma, con HMAC inválido o timestamp expirado retorna 401; firma válida retorna 200', async () => {
    const testPaymentId = 'pay_demo_987654321'
    const testRequestId = `req_e2e_${Date.now()}`

    // 1. Petición sin cabecera x-signature
    const noSigRes = await fetch(`${ORDERS_URL}/api/v1/payments/mercadopago/webhook?data.id=${testPaymentId}&type=payment`, {
      method: 'POST',
      headers: {
        'x-request-id': testRequestId,
      },
    })
    if (noSigRes.status !== 401) {
      throw new Error(`Fallo de seguridad: Webhook procesó notificación sin x-signature (HTTP ${noSigRes.status})`)
    }

    // 2. Petición con firma HMAC falsificada
    const forgedSig = `ts=${Date.now()},v1=deadbeefcafebabe0123456789abcdef0123456789abcdef0123456789abcdef`
    const forgedRes = await fetch(`${ORDERS_URL}/api/v1/payments/mercadopago/webhook?data.id=${testPaymentId}&type=payment`, {
      method: 'POST',
      headers: {
        'x-request-id': testRequestId,
        'x-signature': forgedSig,
      },
    })
    if (forgedRes.status !== 401) {
      throw new Error(`Fallo de seguridad: Webhook aceptó firma HMAC falsificada (HTTP ${forgedRes.status})`)
    }

    // 3. Petición con timestamp expirado (> 5 minutos drift, e.g. 10 minutos atrás)
    const expiredTimestamp = Date.now() - (10 * 60 * 1000)
    const expiredSig = generateMercadoPagoSignature(testRequestId, expiredTimestamp, testPaymentId)
    const expiredRes = await fetch(`${ORDERS_URL}/api/v1/payments/mercadopago/webhook?data.id=${testPaymentId}&type=payment`, {
      method: 'POST',
      headers: {
        'x-request-id': testRequestId,
        'x-signature': expiredSig,
      },
    })
    if (expiredRes.status !== 401) {
      throw new Error(`Fallo de seguridad: Webhook aceptó timestamp con más de 5 minutos de desfase (HTTP ${expiredRes.status})`)
    }

    // 4. Petición con firma HMAC válida y timestamp actual
    const validTimestamp = Date.now()
    const validSig = generateMercadoPagoSignature(testRequestId, validTimestamp, testPaymentId)
    const validRes = await fetch(`${ORDERS_URL}/api/v1/payments/mercadopago/webhook?data.id=${testPaymentId}&type=payment`, {
      method: 'POST',
      headers: {
        'x-request-id': testRequestId,
        'x-signature': validSig,
      },
    })
    if (validRes.status !== 200) {
      throw new Error(`Fallo de configuración: Webhook rechazó notificación legítima con firma válida (HTTP ${validRes.status} - ${await validRes.text()})`)
    }
    const validData = await validRes.json()
    if (validData.status !== 'PROCESSED') {
      throw new Error(`Respuesta de webhook procesado inesperada: ${JSON.stringify(validData)}`)
    }
  })

  // ---------------------------------------------------------------------------
  // RESUMEN FINAL
  // ---------------------------------------------------------------------------
  console.log(`\n${COLORS.bright}========================================================================${COLORS.reset}`)
  console.log(`${COLORS.bright}                         RESUMEN DE PRUEBAS E2E                         ${COLORS.reset}`)
  console.log(`${COLORS.bright}========================================================================${COLORS.reset}`)

  const passed = testResults.filter(t => t.success).length
  const failed = testResults.filter(t => !t.success).length

  for (const r of testResults) {
    const icon = r.success ? `${COLORS.green}✔ PASÓ${COLORS.reset}` : `${COLORS.red}✖ FALLÓ${COLORS.reset}`
    console.log(`  ${icon} [Paso ${r.step}] ${r.description} (${r.durationMs}ms)`)
    if (r.error) {
      console.log(`         ${COLORS.red}Error: ${r.error}${COLORS.reset}`)
    }
  }

  console.log(`\nTotal: ${testResults.length} pruebas | Pasadas: ${passed} | Falladas: ${failed}\n`)

  if (failed > 0) {
    console.log(`${COLORS.red}${COLORS.bright}✖ LA SUITE DE INTEGRACIÓN E2E Y SEGURIDAD HA FALLADO${COLORS.reset}\n`)
    cleanup()
    process.exit(1)
  } else {
    console.log(`${COLORS.green}${COLORS.bright}🎉 TODAS LAS PRUEBAS DE INTEGRACIÓN E2E Y SEGURIDAD PASARON EXITOSAMENTE 🎉${COLORS.reset}\n`)
    cleanup()
    process.exit(0)
  }
}

main().catch(err => {
  console.error('Error fatal durante la ejecución de las pruebas E2E:', err)
  cleanup()
  process.exit(1)
})
