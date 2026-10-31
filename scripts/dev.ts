import { spawn, type Subprocess } from 'bun'
import net from 'node:net'
import process from 'node:process'

const COLORS = {
  reset: '\x1b[0m',
  bright: '\x1b[1m',
  dim: '\x1b[2m',
  cyan: '\x1b[36m',
  magenta: '\x1b[35m',
  green: '\x1b[32m',
  yellow: '\x1b[33m',
  blue: '\x1b[34m',
  red: '\x1b[31m',
}

interface ServiceSpec {
  name: string
  color: string
  cmd: string[]
  port?: number
}

const activeProcesses: Subprocess[] = []
let isShuttingDown = false

function log(prefix: string, color: string, message: string) {
  const time = new Date().toLocaleTimeString('es-AR', { hour12: false })
  console.log(`${color}[${prefix}]${COLORS.reset} ${COLORS.dim}${time}${COLORS.reset} ${message}`)
}

async function isPortOpen(port: number, host = '127.0.0.1', timeoutMs = 800): Promise<boolean> {
  return new Promise((resolve) => {
    const socket = new net.Socket()
    socket.setTimeout(timeoutMs)
    socket.on('connect', () => {
      socket.destroy()
      resolve(true)
    })
    socket.on('error', () => {
      socket.destroy()
      resolve(false)
    })
    socket.on('timeout', () => {
      socket.destroy()
      resolve(false)
    })
    socket.connect(port, host)
  })
}

async function waitForPort(port: number, name: string, timeoutMs = 20000): Promise<boolean> {
  const start = Date.now()
  while (Date.now() - start < timeoutMs) {
    if (await isPortOpen(port)) {
      return true
    }
    await new Promise((r) => setTimeout(r, 600))
  }
  return false
}

async function ensureInfrastructure() {
  log('INFRA', COLORS.yellow, 'Verificando contenedores de infraestructura local (Docker)...')

  const pgReady = await isPortOpen(5432)
  const redisReady = await isPortOpen(6379)
  const flociReady = await isPortOpen(4577)

  if (pgReady && redisReady && flociReady) {
    log('INFRA', COLORS.green, 'Infraestructura activa (PostgreSQL:5432, Redis:6379, Floci-AZ:4577)')
    return
  }

  log('INFRA', COLORS.yellow, 'Levantando contenedores Docker Compose (floci-az, postgres, redis)...')
  const composeProc = spawn(['docker', 'compose', 'up', '-d'], {
    stdout: 'inherit',
    stderr: 'inherit',
  })
  await composeProc.exited

  log('INFRA', COLORS.yellow, 'Esperando disponibilidad de puertos...')
  const [pg, redis] = await Promise.all([
    waitForPort(5432, 'PostgreSQL'),
    waitForPort(6379, 'Redis'),
  ])

  if (!pg || !redis) {
    log('INFRA', COLORS.red, 'Advertencia: Algunos servicios de infraestructura tardaron en responder.')
  } else {
    log('INFRA', COLORS.green, 'Todos los contenedores de infraestructura listos.')
  }
}

function streamOutput(proc: Subprocess, prefix: string, color: string) {
  const reader = (stream: ReadableStream<Uint8Array> | null) => {
    if (!stream) return
    const decoder = new TextDecoder()
    const r = stream.getReader()
    const read = async () => {
      try {
        while (!isShuttingDown) {
          const { done, value } = await r.read()
          if (done) break
          const text = decoder.decode(value)
          const lines = text.split('\n')
          for (const line of lines) {
            const trimmed = line.trimEnd()
            if (trimmed) {
              console.log(`${color}[${prefix}]${COLORS.reset} ${trimmed}`)
            }
          }
        }
      } catch {
        // Stream closed
      }
    }
    read()
  }

  reader(proc.stdout)
  reader(proc.stderr)
}

function startService(spec: ServiceSpec): Subprocess {
  log('ORCHESTRATOR', COLORS.blue, `Iniciando ${spec.name}...`)

  const proc = spawn(spec.cmd, {
    stdout: 'pipe',
    stderr: 'pipe',
    env: { ...process.env },
  })

  activeProcesses.push(proc)
  streamOutput(proc, spec.name, spec.color)

  proc.exited.then((code) => {
    if (!isShuttingDown && code !== 0 && code !== null) {
      log(spec.name, COLORS.red, `Proceso finalizado con código ${code}`)
    }
  })

  return proc
}

async function shutdown() {
  if (isShuttingDown) return
  isShuttingDown = true

  console.log('\n')
  log('ORCHESTRATOR', COLORS.yellow, 'Deteniendo todos los servicios en ejecución...')

  for (const proc of activeProcesses) {
    try {
      if (process.platform === 'win32' && proc.pid) {
        // Windows process tree cleanup (gradlew creates child java processes)
        spawn(['taskkill', '/pid', proc.pid.toString(), '/T', '/F'], {
          stdout: 'ignore',
          stderr: 'ignore',
        })
      } else {
        proc.kill('SIGTERM')
      }
    } catch {
      // Ignore
    }
  }

  setTimeout(() => {
    log('ORCHESTRATOR', COLORS.green, 'Entorno detenido correctamente. ¡Hasta luego!')
    process.exit(0)
  }, 1000)
}

process.on('SIGINT', shutdown)
process.on('SIGTERM', shutdown)

async function main() {
  console.clear()
  console.log(`${COLORS.cyan}${COLORS.bright}================================================================${COLORS.reset}`)
  console.log(`${COLORS.cyan}${COLORS.bright}      HIBANA.TK TICKETING PLATFORM - UNIFIED DEV RUNNER         ${COLORS.reset}`)
  console.log(`${COLORS.dim}      Java 25 • Spring Boot 4 • Vue 3 / Vite • Bun • Floci-AZ   ${COLORS.reset}`)
  console.log(`${COLORS.cyan}${COLORS.bright}================================================================${COLORS.reset}\n`)

  await ensureInfrastructure()

  const isWindows = process.platform === 'win32'
  const gradlewCatalog = isWindows
    ? '.\\ticketing-app-catalog\\gradlew.bat'
    : './ticketing-app-catalog/gradlew'
  const gradlewInventory = isWindows
    ? '.\\ticketing-app-inventory\\gradlew.bat'
    : './ticketing-app-inventory/gradlew'
  const gradlewGateway = isWindows
    ? '.\\ticketing-app-gateway\\gradlew.bat'
    : './ticketing-app-gateway/gradlew'
  const gradlewOrders = isWindows
    ? '.\\ticketing-app-orders\\gradlew.bat'
    : './ticketing-app-orders/gradlew'

  const services: ServiceSpec[] = [
    {
      name: 'GATEWAY',
      color: COLORS.yellow,
      cmd: [gradlewGateway, '-p', 'ticketing-app-gateway', 'bootRun'],
      port: 8080,
    },
    {
      name: 'CATALOG',
      color: COLORS.cyan,
      cmd: [gradlewCatalog, '-p', 'ticketing-app-catalog', 'bootRun'],
      port: 8081,
    },
    {
      name: 'INVENTORY',
      color: COLORS.magenta,
      cmd: [gradlewInventory, '-p', 'ticketing-app-inventory', 'bootRun'],
      port: 8082,
    },
    {
      name: 'ORDERS',
      color: COLORS.blue,
      cmd: [gradlewOrders, '-p', 'ticketing-app-orders', 'bootRun'],
      port: 8083,
    },
    {
      name: 'FRONTEND',
      color: COLORS.green,
      cmd: ['bun', '--cwd', 'ticketing-frontend', 'dev'],
      port: 5173,
    },
  ]

  console.log('\n' + COLORS.bright + '--- Iniciando Microservicios & Cliente Web ---' + COLORS.reset)
  for (const service of services) {
    startService(service)
  }

  log('ORCHESTRATOR', COLORS.yellow, 'Todos los procesos iniciados. Presiona Ctrl+C para detener todo.')
  console.log(`${COLORS.dim}API Gateway:     ${COLORS.bright}http://localhost:8080${COLORS.reset}`)
  console.log(`${COLORS.dim}Frontend:        ${COLORS.bright}http://localhost:5173${COLORS.reset}`)
  console.log(`${COLORS.dim}API Catálogo:    ${COLORS.bright}http://localhost:8081/api/v1/events${COLORS.reset}`)
  console.log(`${COLORS.dim}API Inventario:  ${COLORS.bright}http://localhost:8082/api/v1/events/.../seats${COLORS.reset}`)
  console.log(`${COLORS.dim}API Órdenes:     ${COLORS.bright}http://localhost:8083/api/v1/orders${COLORS.reset}\n`)
}

main()
