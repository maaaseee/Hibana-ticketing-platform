import { spawn } from 'bun'
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
}

interface TestSuite {
  name: string
  cmd: string[]
  color: string
}

async function runSuite(suite: TestSuite): Promise<boolean> {
  console.log(`\n${suite.color}${COLORS.bright}▶ Ejecutando suite: ${suite.name}${COLORS.reset}`)
  console.log(`${COLORS.dim}Comando: ${suite.cmd.join(' ')}${COLORS.reset}\n`)

  const start = Date.now()
  const proc = spawn(suite.cmd, {
    stdout: 'inherit',
    stderr: 'inherit',
  })

  const exitCode = await proc.exited
  const duration = ((Date.now() - start) / 1000).toFixed(2)

  if (exitCode === 0) {
    console.log(`${COLORS.green}${COLORS.bright}✔ ${suite.name} PASÓ EXITOSAMENTE (${duration}s)${COLORS.reset}`)
    return true
  } else {
    console.log(`${COLORS.red}${COLORS.bright}✖ ${suite.name} FALLÓ con código ${exitCode} (${duration}s)${COLORS.reset}`)
    return false
  }
}

async function main() {
  console.log(`${COLORS.cyan}${COLORS.bright}================================================================${COLORS.reset}`)
  console.log(`${COLORS.cyan}${COLORS.bright}      AMARYLLIS TICKETING PLATFORM - TEST ALL SUITES            ${COLORS.reset}`)
  console.log(`${COLORS.cyan}${COLORS.bright}================================================================${COLORS.reset}`)

  const isWindows = process.platform === 'win32'
  const gradlewCatalog = isWindows
    ? '.\\ticketing-app-catalog\\gradlew.bat'
    : './ticketing-app-catalog/gradlew'
  const gradlewInventory = isWindows
    ? '.\\ticketing-app-inventory\\gradlew.bat'
    : './ticketing-app-inventory/gradlew'

  const suites: TestSuite[] = [
    {
      name: 'Frontend Vitest Suite (Unit + Services Integration)',
      cmd: ['bun', 'run', '--cwd', 'ticketing-frontend', 'test:unit', '--', '--run'],
      color: COLORS.green,
    },
    {
      name: 'Backend Catalog Service (Spring Boot 4 / Java 25)',
      cmd: [gradlewCatalog, '-p', 'ticketing-app-catalog', 'test'],
      color: COLORS.cyan,
    },
    {
      name: 'Backend Inventory Service (Redlock / Virtual Threads IT)',
      cmd: [gradlewInventory, '-p', 'ticketing-app-inventory', 'test'],
      color: COLORS.magenta,
    },
  ]

  const results: { name: string; success: boolean }[] = []

  for (const suite of suites) {
    const success = await runSuite(suite)
    results.push({ name: suite.name, success })
    if (!success) {
      console.log(`\n${COLORS.red}Se interrumpe la ejecución por falla en ${suite.name}.${COLORS.reset}`)
      process.exit(1)
    }
  }

  console.log(`\n${COLORS.cyan}${COLORS.bright}================================================================${COLORS.reset}`)
  console.log(`${COLORS.green}${COLORS.bright}🎉 TODAS LAS SUITES DE PRUEBAS PASARON EXITOSAMENTE 🎉${COLORS.reset}`)
  console.log(`${COLORS.cyan}${COLORS.bright}================================================================${COLORS.reset}\n`)
}

main()
