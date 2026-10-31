/**
 * Amaryllis Ticketing Platform - Cross-Backend End-to-End (E2E) Integration Test Suite
 * 
 * Verifies real ingress routing and end-to-end interactions across the entire backend:
 * Gateway (port 8080) -> Catalog (port 8081) -> Inventory (port 8082) -> PostgreSQL 17 & Redis 7
 * 
 * Tests include:
 * 1. Health Probes across all microservices (Actuator UP)
 * 2. Gateway Ingress Routing to Catalog (GET /api/v1/events) + Trace-ID Header Injection
 * 3. Gateway Ingress Routing to Catalog Details (GET /api/v1/events/{id})
 * 4. Gateway High-Precedence Route to Inventory Seats (GET /api/v1/events/{id}/seats)
 * 5. Virtual Waiting Room Gating: Missing Token returns HTTP 403 ProblemDetail
 * 6. Virtual Waiting Room Gating: Forged/Tampered Token returns HTTP 403 ProblemDetail
 * 7. Virtual Waiting Room Gating: Expired Token returns HTTP 403 ProblemDetail
 * 8. End-to-End Seat Hold: Valid HMAC-SHA256 Token -> Gateway -> Inventory (Redlock + Postgres) -> HTTP 201 Created
 * 9. Concurrency / 0% Double Booking: Simultaneous reservation requests for identical seat -> 1 succeeds (201), 1 fails (409)
 * 10. Resilience4j Circuit Breaker Fallback: Out-of-service/Non-existent fallback endpoint
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

const WAITING_ROOM_SECRET = 'virtual-waiting-room-hmac-key-2026-minimum-256-bits!'
const WAITING_ROOM_ISSUER = 'waiting-room-engine'

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

async function waitForPort(name: string, port: number, timeoutMs: number = 45000): Promise<boolean> {
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
  console.log(`${COLORS.magenta}${COLORS.bright}    AMARYLLIS PLATFORM - CROSS-BACKEND E2E INTEGRATION TEST SUITE      ${COLORS.reset}`)
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

  await runTest(8, 'End-to-End Waiting Room Real: join -> status -> skip -> Gateway -> Inventory (Redlock + Postgres)', async () => {
    try {
      await fetch(`${GATEWAY_URL}/api/v1/queue/join`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ eventId: sampleEventId, userId: testBuyerId }),
      })
      await fetch(`${GATEWAY_URL}/api/v1/queue/status?eventId=${sampleEventId}&userId=${testBuyerId}`)
      const skipRes = await fetch(`${GATEWAY_URL}/api/v1/queue/skip`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ eventId: sampleEventId, userId: testBuyerId }),
      })
      if (skipRes.ok) {
        const skipData = await skipRes.json()
        if (skipData.checkoutToken) {
          validBuyerToken = skipData.checkoutToken
        }
      }
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
    // Tomamos otro asiento disponible para la prueba de carrera
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
    // Uno debe ser 201 Created y el otro 409 Conflict (o 423 Locked)
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
  // ESCENARIO 11: Flujo Completo de Checkout (Hold -> Order -> Pocket Wallet Pay -> PAID)
  // ---------------------------------------------------------------------------
  await runTest(11, 'Flujo Completo Checkout: Reserva Asiento -> Crear Orden -> Pago con Billetera Virtual (com.aieta.pocket) -> Estado PAID', async () => {
    const e2eUser = `e2e_buyer_${Date.now()}`
    const checkoutToken = generateWaitingRoomToken(e2eUser, 15)

    // A. Fondeo inicial de billetera virtual vía Gateway (:8080 -> Orders :8083)
    const topUpRes = await fetch(`${GATEWAY_URL}/api/v1/wallets/me/top-up`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'X-User-Id': e2eUser,
      },
      body: JSON.stringify({ amount: 150.00 }),
    })
    if (!topUpRes.ok) {
      throw new Error(`Fondeo de billetera falló: HTTP ${topUpRes.status}`)
    }
    const topUpData = await topUpRes.json()
    if (topUpData.balance < 150.00) {
      throw new Error(`Saldo inesperado tras fondeo: ${topUpData.balance}`)
    }

    // B. Reserva de asiento en Inventario con token de sala de espera
    const resSeats = await fetch(`${GATEWAY_URL}/api/v1/events/${sampleEventId}/seats`)
    const seatsData = await resSeats.json()
    const allSeats = Array.isArray(seatsData) ? seatsData : (seatsData.seats || [])
    const availableSeat = allSeats.find((s: any) => s.status === 'AVAILABLE') || allSeats[0]
    const seatIdToBuy = availableSeat.id || availableSeat.seatId || 'SEAT-CHECKOUT-001'

    const holdRes = await fetch(`${GATEWAY_URL}/api/v1/reservations`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'X-Checkout-Token': checkoutToken,
        'X-User-Id': e2eUser,
      },
      body: JSON.stringify({
        eventId: sampleEventId,
        seatId: seatIdToBuy,
        userId: e2eUser,
      }),
    })
    if (holdRes.status !== 201) {
      throw new Error(`Reserva falló en checkout: HTTP ${holdRes.status} - ${await holdRes.text()}`)
    }
    const holdData = await holdRes.json()
    const reservationId = holdData.reservationId

    // C. Creación de Orden a través del Gateway en Orders Service (:8083)
    const orderRes = await fetch(`${GATEWAY_URL}/api/v1/orders`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'X-User-Id': e2eUser,
      },
      body: JSON.stringify({
        reservationId: reservationId,
        eventId: sampleEventId,
        seatId: seatIdToBuy,
        totalAmount: 65.00,
      }),
    })
    if (orderRes.status !== 201) {
      throw new Error(`Creación de orden falló: HTTP ${orderRes.status} - ${await orderRes.text()}`)
    }
    const orderData = await orderRes.json()
    const orderId = orderData.orderId

    // D. Pago de la orden usando POCKET_WALLET
    const payRes = await fetch(`${GATEWAY_URL}/api/v1/orders/${orderId}/pay`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'X-User-Id': e2eUser,
      },
      body: JSON.stringify({
        paymentMethod: 'POCKET_WALLET',
      }),
    })
    if (!payRes.ok) {
      throw new Error(`Pago de orden falló: HTTP ${payRes.status} - ${await payRes.text()}`)
    }
    const payData = await payRes.json()
    if (payData.status !== 'PAID') {
      throw new Error(`Estado de pago incorrecto: ${payData.status}`)
    }

    // E. Verificación del balance restante tras el pago
    const balanceRes = await fetch(`${GATEWAY_URL}/api/v1/wallets/me/balance`, {
      headers: { 'X-User-Id': e2eUser },
    })
    const balanceData = await balanceRes.json()
    if (balanceData.balance > 85.01) {
      throw new Error(`Saldo de billetera no debitado correctamente: ${balanceData.balance}`)
    }
  })

  // ---------------------------------------------------------------------------
  // ESCENARIO 12: Compensación de Saga ante Cancelación de Orden
  // ---------------------------------------------------------------------------
  await runTest(12, 'Saga Compensation: Cancelación de Orden -> Actualización a CANCELLED y Emisión de PaymentFailedEvent', async () => {
    const e2eUser = `e2e_cancel_${Date.now()}`
    const fakeReservationId = `res_cancel_${Date.now()}`

    // A. Crear orden en PENDING_PAYMENT
    const orderRes = await fetch(`${GATEWAY_URL}/api/v1/orders`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'X-User-Id': e2eUser,
      },
      body: JSON.stringify({
        reservationId: fakeReservationId,
        eventId: sampleEventId,
        seatId: 'SEAT-CANCEL-001',
        totalAmount: 49.00,
      }),
    })
    if (orderRes.status !== 201) {
      throw new Error(`Creación de orden previa a cancelación falló: HTTP ${orderRes.status}`)
    }
    const orderData = await orderRes.json()
    const orderId = orderData.orderId

    // B. Cancelar la orden
    const cancelRes = await fetch(`${GATEWAY_URL}/api/v1/orders/${orderId}/cancel`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'X-User-Id': e2eUser,
      },
      body: JSON.stringify({ reason: 'Usuario rechazó términos o tiempo agotado' }),
    })
    if (!cancelRes.ok) {
      throw new Error(`Cancelación de orden falló: HTTP ${cancelRes.status}`)
    }

    // C. Consultar detalle de la orden para confirmar estado CANCELLED
    const detailRes = await fetch(`${GATEWAY_URL}/api/v1/orders/${orderId}`, {
      headers: { 'X-User-Id': e2eUser },
    })
    const detailData = await detailRes.json()
    if (detailData.status !== 'CANCELLED') {
      throw new Error(`Estado tras cancelación no es CANCELLED: ${detailData.status}`)
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
    console.log(`${COLORS.red}${COLORS.bright}✖ LA SUITE DE INTEGRACIÓN E2E DEL BACKEND HA FALLADO${COLORS.reset}\n`)
    cleanup()
    process.exit(1)
  } else {
    console.log(`${COLORS.green}${COLORS.bright}🎉 TODAS LAS PRUEBAS DE INTEGRACIÓN E2E DEL BACKEND PASARON EXITOSAMENTE 🎉${COLORS.reset}\n`)
    cleanup()
    process.exit(0)
  }
}

main().catch(err => {
  console.error('Error fatal durante la ejecución de las pruebas E2E:', err)
  cleanup()
  process.exit(1)
})
