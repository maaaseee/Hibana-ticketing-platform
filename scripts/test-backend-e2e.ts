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

const WAITING_ROOM_SECRET = 'amaryllis-super-secret-waiting-room-hmac-key-2026-minimum-256-bits!'
const WAITING_ROOM_ISSUER = 'amaryllis-waiting-room'

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
  const gatewayOk = await ensureServiceRunning('Gateway Service', 'ticketing-app-gateway', 8080)

  if (!catalogOk || !inventoryOk || !gatewayOk) {
    console.error(`\n${COLORS.red}Error: No fue posible inicializar los servicios requeridos.${COLORS.reset}`)
    process.exit(1)
  }

  console.log(`\n${COLORS.bright}2. Ejecutando escenarios de integración End-to-End:${COLORS.reset}`)

  // ---------------------------------------------------------------------------
  // ESCENARIO 1: Health Probes Actuator
  // ---------------------------------------------------------------------------
  await runTest(1, 'Actuator Health Probes: Gateway (:8080), Catalog (:8081), Inventory (:8082) están UP', async () => {
    const gw = await fetch(`${GATEWAY_URL}/actuator/health`).then(r => r.json())
    const cat = await fetch(`${CATALOG_URL}/actuator/health`).then(r => r.json())
    const inv = await fetch(`${INVENTORY_URL}/actuator/health`).then(r => r.json())

    if (gw.status !== 'UP') throw new Error(`Gateway no reporta UP: ${JSON.stringify(gw)}`)
    if (cat.status !== 'UP') throw new Error(`Catalog no reporta UP: ${JSON.stringify(cat)}`)
    if (inv.status !== 'UP') throw new Error(`Inventory no reporta UP: ${JSON.stringify(inv)}`)
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
  const validBuyerToken = generateWaitingRoomToken(testBuyerId, 15)

  await runTest(8, 'End-to-End Seat Hold: Token Válido -> Gateway -> Inventory (Redlock Redis + Postgres)', async () => {
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
    process.exit(1)
  } else {
    console.log(`${COLORS.green}${COLORS.bright}🎉 TODAS LAS PRUEBAS DE INTEGRACIÓN E2E DEL BACKEND PASARON EXITOSAMENTE 🎉${COLORS.reset}\n`)
  }
}

main().catch(err => {
  console.error('Error fatal durante la ejecución de las pruebas E2E:', err)
  cleanup()
  process.exit(1)
})
