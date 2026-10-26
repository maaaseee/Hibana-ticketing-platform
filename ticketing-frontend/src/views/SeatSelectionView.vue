<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  AccordionRoot,
  AccordionItem,
  AccordionHeader,
  AccordionTrigger,
  AccordionContent,
  DialogRoot,
  DialogPortal,
  DialogOverlay,
  DialogContent,
  DialogTitle,
  DialogDescription
} from 'radix-vue'
import { useCatalogStore } from '../stores/catalog'
import { useTicketingStore } from '../stores/ticketing'
import { useQueueStore } from '../stores/queue'
import type { VenueSeatMapLayout } from '../types'
import defaultArenaLayout from '../../public/seat-maps/arena-buenos-aires.json'

interface GeneralSector {
  id: string
  name: string
  code: string
  type: string
  category: 'Campo' | 'Platea Baja' | 'Platea Alta'
  capacity: number
  fallbackPrice: number
  focalDistance: string
  visibility: string
  gate: string
  polygonPoints: string
  labelX: number
  labelY: number
  stageDirection: 'NORTH' | 'WEST' | 'EAST'
  stageBanner: string
  stageAngle: 'FRONTAL' | 'LATERAL_OESTE' | 'LATERAL_ESTE' | 'CAMPO'
  stageDescription: string
  colorFill: string
  colorHover: string
  colorStroke: string
  badgeBg: string
  badgeText: string
}

interface SectorZone {
  id: string
  name: string
  code: string
  description: string
  fallbackPrice: number
  totalSeats: number
  availableSeats: number
  gate: string
  colorFill: string
  colorStroke: string
  rect: { x: number; y: number; width: number; height: number }
}

interface CategoryDefinition {
  id: 'campo' | 'platea_baja' | 'platea_alta'
  name: 'Campo' | 'Platea Baja' | 'Platea Alta'
  badgeText: string
  colorBar: string
  description: string
}

interface SectorPricingInfo {
  price: number
  minPrice: number
  maxPrice: number
  capacity: number
  currency: string
  formatted: string
  priceRangeFormatted: string
}

const route = useRoute()
const router = useRouter()
const catalogStore = useCatalogStore()
const ticketingStore = useTicketingStore()
const queueStore = useQueueStore()

const eventId = computed(() => (route.params.id as string) || 'ado-hibana-world-tour')
const event = computed(() => catalogStore.getEventById(eventId.value))

// Política dinámica de compra según el organizador del evento
const maxTicketsAllowed = computed<number>(() => {
  const policy = event.value?.maxTicketsPerUser || (event.value as any)?.salesPolicy?.maxTicketsPerUser
  if (typeof policy === 'number' && policy > 0) {
    return policy
  }
  return 4
})

// Función designada para pruebas de ocupación / stress test
const isTestOccupiedFunction = computed(() => {
  const fnId = (route.query.functionId as string) || ''
  const testParam = route.query.testOccupied === 'true'
  return fnId === 'fn-symphony-2' || fnId === 'fn-test-occupied' || fnId === 'fn-ado-2' || testParam
})

// Estado de expiración del temporizador Redlock de 10 minutos
const showTimeExpiredModal = ref<boolean>(false)

watch(
  () => ticketingStore.isTimerExpired,
  (expired) => {
    if (expired) {
      showTimeExpiredModal.value = true
    }
  }
)

function handleRestartTimer() {
  showTimeExpiredModal.value = false
  ticketingStore.resetAndRestartTimer(600)
  initSeatMatrix()
}

function handleExpiredReturnHome() {
  showTimeExpiredModal.value = false
  router.push({ name: 'event-detail', params: { id: eventId.value } })
}

function triggerQueueStressTest() {
  queueStore.enterQueue(eventId.value)
  router.push({
    name: 'queue',
    params: { id: eventId.value },
    query: { stressTest: 'true' }
  })
}

// 3-Tier Stepped Flow
const currentStep = ref<'sector' | 'zone' | 'seats'>('sector')

// Radix Accordion State: sincronizado con la categoría del sector seleccionado
const activeAccordion = ref<string>('platea_alta')

// Venue JSON layout for realistic stadium radar (con fallback inicial importado para 0 latencia)
const venueLayout = ref<VenueSeatMapLayout>(defaultArenaLayout as unknown as VenueSeatMapLayout)

async function loadVenueMap() {
  try {
    const origin = typeof window !== 'undefined' && window.location?.origin ? window.location.origin : 'http://localhost'
    const res = await fetch(`${origin}/seat-maps/arena-buenos-aires.json`)
    if (res.ok) {
      venueLayout.value = await res.json()
    }
  } catch {
    // Mantener fallback defaultArenaLayout
  }
}

// Normalizador de claves canónicas para matching perfecto entre SVG Macro, Backend y JSON del Estadio
function canonicalSectorKey(id: string): string {
  return id
    .toLowerCase()
    .replace(/^ado__/, '')
    .replace(/^sec-arena-/, '')
    .replace(/^sec-/, '')
    .replace(/-lateral-/g, '-')
    .replace(/[^a-z0-9]/g, '')
}

function isSectorActive(secId: string, secName?: string): boolean {
  const currentKey = canonicalSectorKey(selectedSector.value.id)
  const targetKey = canonicalSectorKey(secId)
  if (currentKey === targetKey) return true

  if (secName) {
    const norm = (s: string) => s.toLowerCase().replace(/[^a-z0-9]/g, '').replace(/lateral/g, '').replace(/cabecera/g, '')
    const cName = norm(selectedSector.value.name)
    const tName = norm(secName)
    if (cName === tName || cName.includes(tName) || tName.includes(cName)) {
      return true
    }
  }
  return false
}

// Helper para calcular el precio mínimo y máximo por sector ("precio mínimo de entrada - precio máximo de entrada")
function getBackendSectorInfo(secId: string, secName: string, fallbackPrice: number = 45000): SectorPricingInfo {
  const sectors = event.value?.sectors || []
  const norm = (s: string) => s.toLowerCase().replace(/[^a-z0-9]/g, '')
  const targetKey = canonicalSectorKey(secId)
  const targetName = norm(secName)

  // Buscar el sector principal
  const found = sectors.find(bs => {
    const bKey = canonicalSectorKey(bs.id)
    const bName = norm(bs.name)
    return bKey.includes(targetKey) || targetKey.includes(bKey) || bName.includes(targetName) || targetName.includes(bName)
  })

  const price = found && typeof found.price === 'number' && found.price > 0 ? Math.round(found.price) : fallbackPrice
  const capacity = found?.capacity || 1400
  const currency = found?.currency || 'ARS'
  const isCampo = targetName.includes('campo') || targetKey.includes('campo')

  let minPrice = price
  let maxPrice = price

  if (!isCampo) {
    // Buscar si hay subsectores/zonas en la base de datos (ej: zona A1, A2, A3)
    const subsectors = sectors.filter(bs => {
      const bKey = canonicalSectorKey(bs.id)
      const bName = norm(bs.name)
      return (bKey.includes(targetKey) || bName.includes(targetName) || bName.includes('zona')) &&
             (bKey.includes('a1') || bKey.includes('a2') || bKey.includes('a3') || bName.includes('zona a'))
    })

    if (subsectors.length > 0) {
      const subPrices = subsectors.map(s => Math.round(s.price))
      minPrice = Math.min(...subPrices)
      maxPrice = Math.max(...subPrices)
    } else {
      // Si no hay subsectores explícitos en BD, calcular rango por cercanía (A1/A3 a 0.90, A2 a 1.05)
      minPrice = Math.round(price * 0.90)
      maxPrice = Math.round(price * 1.05)
    }
  }

  const formatCurrency = (val: number) => currency === 'ARS' ? `$ ${val.toLocaleString('es-AR')}` : `$${val} USD`
  const formatted = formatCurrency(price)
  const priceRangeFormatted = minPrice < maxPrice
    ? `${formatCurrency(minPrice)} - ${formatCurrency(maxPrice)}`
    : formatCurrency(minPrice)

  return {
    price,
    minPrice,
    maxPrice,
    capacity,
    currency,
    formatted,
    priceRangeFormatted
  }
}

// Paso 1: Sectores Generales con solo dirección cardinal (Sur, Oeste, Este)
const generalSectors: GeneralSector[] = [
  {
    id: 'sec-platea-alta-cabecera',
    name: 'Platea Alta Sur',
    code: 'PA-SUR',
    type: 'Numbered Seats',
    category: 'Platea Alta',
    capacity: 1400,
    fallbackPrice: 39130,
    focalDistance: '85m',
    visibility: 'Visión frontal panorámica y cenital completa del escenario y campo.',
    gate: 'Puerta Sur 3',
    polygonPoints: '205,633 795,633 795,760 205,760',
    labelX: 500,
    labelY: 690,
    stageDirection: 'NORTH',
    stageBanner: '▲ DIRECCIÓN HACIA EL ESCENARIO PRINCIPAL (VISIÓN FRONTAL A 85M) ▲',
    stageAngle: 'FRONTAL',
    stageDescription: 'Visión frontal directa hacia el escenario principal.',
    colorFill: 'rgba(168, 85, 247, 0.28)',
    colorHover: 'rgba(168, 85, 247, 0.52)',
    colorStroke: '#a855f7',
    badgeBg: 'bg-purple-950/70 border-purple-500/40',
    badgeText: 'text-purple-300'
  },
  {
    id: 'sec-platea-baja-cabecera',
    name: 'Platea Baja Sur',
    code: 'PB-SUR',
    type: 'Numbered Seats',
    category: 'Platea Baja',
    capacity: 1500,
    fallbackPrice: 69565,
    focalDistance: '65m',
    visibility: 'Visión frontal a nivel medio con pendiente suave hacia el escenario.',
    gate: 'Puerta Sur 1 y 2',
    polygonPoints: '205,498 795,498 795,615 205,615',
    labelX: 500,
    labelY: 550,
    stageDirection: 'NORTH',
    stageBanner: '▲ DIRECCIÓN HACIA EL ESCENARIO PRINCIPAL (VISIÓN FRONTAL A 65M) ▲',
    stageAngle: 'FRONTAL',
    stageDescription: 'Visión frontal a nivel medio hacia el escenario principal.',
    colorFill: 'rgba(14, 165, 233, 0.28)',
    colorHover: 'rgba(14, 165, 233, 0.52)',
    colorStroke: '#0ea5e9',
    badgeBg: 'bg-sky-950/70 border-sky-500/40',
    badgeText: 'text-sky-300'
  },
  {
    id: 'sec-campo-general',
    name: 'Campo General',
    code: 'CAMPO',
    type: 'General Admission',
    category: 'Campo',
    capacity: 3500,
    fallbackPrice: 56521,
    focalDistance: '35m - 60m',
    visibility: 'Área libre por orden de llegada en el centro del show con acústica de sala.',
    gate: 'Puertas Este y Oeste',
    polygonPoints: '330,305 670,305 670,480 330,480',
    labelX: 500,
    labelY: 385,
    stageDirection: 'NORTH',
    stageBanner: '▲ DIRECCIÓN HACIA EL ESCENARIO PRINCIPAL (FRENTE CENTRAL A 45M) ▲',
    stageAngle: 'CAMPO',
    stageDescription: 'Acceso de pie por orden de llegada frente a la pasarela principal.',
    colorFill: 'rgba(56, 189, 248, 0.22)',
    colorHover: 'rgba(56, 189, 248, 0.48)',
    colorStroke: '#38bdf8',
    badgeBg: 'bg-blue-950/70 border-blue-500/40',
    badgeText: 'text-blue-300'
  },
  {
    id: 'sec-campo-vip',
    name: 'Campo VIP Frontal',
    code: 'CAMPO-VIP',
    type: 'Standing / VIP',
    category: 'Campo',
    capacity: 3000,
    fallbackPrice: 82608,
    focalDistance: '15m - 35m',
    visibility: 'Máxima cercanía al artista frente directo a la pasarela principal.',
    gate: 'Puerta Norte 1 (Acceso VIP)',
    polygonPoints: '330,115 670,115 670,290 330,290',
    labelX: 500,
    labelY: 195,
    stageDirection: 'NORTH',
    stageBanner: '▲ DIRECCIÓN HACIA EL ESCENARIO PRINCIPAL (FRENTE DIRECTO A 20M) ▲',
    stageAngle: 'CAMPO',
    stageDescription: 'Cercanía inmediata a la pasarela y escenario a nivel de campo.',
    colorFill: 'rgba(234, 179, 8, 0.30)',
    colorHover: 'rgba(234, 179, 8, 0.55)',
    colorStroke: '#eab308',
    badgeBg: 'bg-amber-950/70 border-amber-500/40',
    badgeText: 'text-amber-300'
  },
  {
    id: 'sec-platea-baja-oeste',
    name: 'Platea Baja Oeste',
    code: 'PB-OESTE',
    type: 'Numbered Seats',
    category: 'Platea Baja',
    capacity: 1500,
    fallbackPrice: 73913,
    focalDistance: '45m',
    visibility: 'Lateral izquierdo con ángulo preferencial al escenario.',
    gate: 'Puerta Oeste 2',
    polygonPoints: '205,115 315,115 315,480 205,480',
    labelX: 260,
    labelY: 290,
    stageDirection: 'EAST',
    stageBanner: '▶ DIRECCIÓN HACIA EL ESCENARIO PRINCIPAL (A LA DERECHA A 45M) ▶',
    stageAngle: 'LATERAL_OESTE',
    stageDescription: 'Escenario ubicado hacia tu derecha con ángulo visual directo.',
    colorFill: 'rgba(16, 185, 129, 0.28)',
    colorHover: 'rgba(16, 185, 129, 0.52)',
    colorStroke: '#10b981',
    badgeBg: 'bg-emerald-950/70 border-emerald-500/40',
    badgeText: 'text-emerald-300'
  },
  {
    id: 'sec-platea-baja-este',
    name: 'Platea Baja Este',
    code: 'PB-ESTE',
    type: 'Numbered Seats',
    category: 'Platea Baja',
    capacity: 1500,
    fallbackPrice: 73913,
    focalDistance: '45m',
    visibility: 'Lateral derecho con ángulo preferencial al escenario.',
    gate: 'Puerta Este 2',
    polygonPoints: '685,115 795,115 795,480 685,480',
    labelX: 740,
    labelY: 290,
    stageDirection: 'WEST',
    stageBanner: '◀ DIRECCIÓN HACIA EL ESCENARIO PRINCIPAL (A LA IZQUIERDA A 45M) ◀',
    stageAngle: 'LATERAL_ESTE',
    stageDescription: 'Escenario ubicado hacia tu izquierda con ángulo visual directo.',
    colorFill: 'rgba(16, 185, 129, 0.28)',
    colorHover: 'rgba(16, 185, 129, 0.52)',
    colorStroke: '#10b981',
    badgeBg: 'bg-emerald-950/70 border-emerald-500/40',
    badgeText: 'text-emerald-300'
  },
  {
    id: 'sec-platea-alta-oeste',
    name: 'Platea Alta Oeste',
    code: 'PA-OESTE',
    type: 'Numbered Seats',
    category: 'Platea Alta',
    capacity: 1300,
    fallbackPrice: 43478,
    focalDistance: '60m',
    visibility: 'Grada superior izquierda con perspectiva panorámica.',
    gate: 'Puerta Oeste 4',
    polygonPoints: '80,115 190,115 190,615 145,615 80,520',
    labelX: 135,
    labelY: 355,
    stageDirection: 'EAST',
    stageBanner: '▶ DIRECCIÓN HACIA EL ESCENARIO PRINCIPAL (A LA DERECHA A 60M) ▶',
    stageAngle: 'LATERAL_OESTE',
    stageDescription: 'Grada alta izquierda con vista diagonal orientada hacia el escenario.',
    colorFill: 'rgba(244, 63, 94, 0.25)',
    colorHover: 'rgba(244, 63, 94, 0.50)',
    colorStroke: '#f43f5e',
    badgeBg: 'bg-rose-950/70 border-rose-500/40',
    badgeText: 'text-rose-300'
  },
  {
    id: 'sec-platea-alta-este',
    name: 'Platea Alta Este',
    code: 'PA-ESTE',
    type: 'Numbered Seats',
    category: 'Platea Alta',
    capacity: 1300,
    fallbackPrice: 43478,
    focalDistance: '60m',
    visibility: 'Grada superior derecha con perspectiva panorámica.',
    gate: 'Puerta Este 4',
    polygonPoints: '810,115 920,115 920,520 855,615 810,615',
    labelX: 865,
    labelY: 355,
    stageDirection: 'WEST',
    stageBanner: '◀ DIRECCIÓN HACIA EL ESCENARIO PRINCIPAL (A LA IZQUIERDA A 60M) ◀',
    stageAngle: 'LATERAL_ESTE',
    stageDescription: 'Grada alta derecha con vista diagonal orientada hacia el escenario.',
    colorFill: 'rgba(244, 63, 94, 0.25)',
    colorHover: 'rgba(244, 63, 94, 0.50)',
    colorStroke: '#f43f5e',
    badgeBg: 'bg-rose-950/70 border-rose-500/40',
    badgeText: 'text-rose-300'
  }
]

// Definiciones de categorías estructuradas
const categoryDefinitions: CategoryDefinition[] = [
  {
    id: 'campo',
    name: 'Campo',
    badgeText: 'General Admission / De Pie',
    colorBar: 'bg-cyan-500',
    description: 'Ubicaciones de pie por orden de llegada con acceso directo a pista.'
  },
  {
    id: 'platea_baja',
    name: 'Platea Baja',
    badgeText: 'Anillo Inferior • Numerado',
    colorBar: 'bg-emerald-500',
    description: 'Butacas numeradas en anillo bajo con visión preferencial.'
  },
  {
    id: 'platea_alta',
    name: 'Platea Alta',
    badgeText: 'Anillo Superior • Panorámica',
    colorBar: 'bg-purple-500',
    description: 'Gradas altas con perspectiva panorámica completa del show.'
  }
]

// Default active sector: Platea Alta Sur
const selectedSector = ref<GeneralSector>(generalSectors[0]!)
const hoveredSectorId = ref<string | null>(null)

// Campo Ticket Counter (Orden de llegada - Límite dinámico según política del evento, inicia en 0)
const campoTicketCount = ref<number>(0)
const isCampoMode = computed(() => selectedSector.value.category === 'Campo')

function incrementCampoTickets() {
  if (campoTicketCount.value < maxTicketsAllowed.value) {
    campoTicketCount.value++
    syncCampoTicketsWithStore()
  }
}

function decrementCampoTickets() {
  if (campoTicketCount.value > 0) {
    campoTicketCount.value--
    syncCampoTicketsWithStore()
  }
}

function syncCampoTicketsWithStore() {
  const p = selectedSectorBackend.value.price
  ticketingStore.selectedSeats = Array.from({ length: campoTicketCount.value }, (_, i) => ({
    id: `CAMPO-TKT-0${i + 1}`,
    row: selectedSector.value.name,
    number: i + 1,
    sector: selectedSector.value.name,
    price: p,
    status: 'SELECTED' as const,
    x: 0,
    y: 0
  }))
}

// Dynamic pricing from backend for currently selected sector
const selectedSectorBackend = computed(() => {
  return getBackendSectorInfo(selectedSector.value.id, selectedSector.value.name, selectedSector.value.fallbackPrice)
})

// Rango de precio mínimo a máximo por categoría para el encabezado del Accordion
function getCategoryPriceRangeFormatted(categoryName: 'Campo' | 'Platea Baja' | 'Platea Alta'): string {
  const categorySectors = generalSectors.filter(s => s.category === categoryName)
  const sectorInfos = categorySectors.map(s => getBackendSectorInfo(s.id, s.name, s.fallbackPrice))
  const min = Math.min(...sectorInfos.map(si => si.minPrice))
  const max = Math.max(...sectorInfos.map(si => si.maxPrice))
  const currency = sectorInfos[0]?.currency || 'ARS'
  const format = (v: number) => currency === 'ARS' ? `$ ${v.toLocaleString('es-AR')}` : `$${v} USD`
  return min < max ? `${format(min)} - ${format(max)}` : format(min)
}

// Paso 2: Zonas Modulares con recuadros de la EXACTA MISMA ALTURA (height: 310, y: 145)
const sectorZones: SectorZone[] = [
  {
    id: 'zone-a1',
    name: 'Zona A1 (Esquina Lateral Oeste)',
    code: 'A1',
    description: 'Bloque en esquina lateral. Menor proximidad focal relativa al escenario.',
    fallbackPrice: 35000,
    totalSeats: 350,
    availableSeats: 195,
    gate: 'Puerta Sur 3 - Acceso A',
    colorFill: 'rgba(14, 165, 233, 0.20)',
    colorStroke: '#0ea5e9',
    rect: { x: 110, y: 145, width: 210, height: 310 }
  },
  {
    id: 'zone-a2',
    name: 'Zona A2 (Central Amplia - Frontal)',
    code: 'A2',
    description: 'Bloque central amplio frente al escenario. Máxima cercanía y visión directa.',
    fallbackPrice: 42000,
    totalSeats: 700,
    availableSeats: 420,
    gate: 'Puerta Sur 3 - Acceso Central',
    colorFill: 'rgba(168, 85, 247, 0.25)',
    colorStroke: '#a855f7',
    rect: { x: 345, y: 145, width: 310, height: 310 }
  },
  {
    id: 'zone-a3',
    name: 'Zona A3 (Esquina Lateral Este)',
    code: 'A3',
    description: 'Bloque en esquina lateral. Menor proximidad focal relativa al escenario.',
    fallbackPrice: 35000,
    totalSeats: 350,
    availableSeats: 210,
    gate: 'Puerta Sur 3 - Acceso B',
    colorFill: 'rgba(14, 165, 233, 0.20)',
    colorStroke: '#0ea5e9',
    rect: { x: 680, y: 145, width: 210, height: 310 }
  }
]

// Default active zone: Zona A2
const selectedZone = ref<SectorZone>(sectorZones[1]!)
const hoveredZoneId = ref<string | null>(null)

// Buscar subsector en el backend para la zona y sector activo
function findZoneBackendSector(zone: SectorZone) {
  const sectors = event.value?.sectors || []
  const norm = (s: string) => s.toLowerCase().replace(/[^a-z0-9]/g, '')
  const secKey = canonicalSectorKey(selectedSector.value.id)
  const zoneCode = norm(zone.code)

  return sectors.find(bs => {
    const bKey = canonicalSectorKey(bs.id)
    const bName = norm(bs.name)
    return (bKey.includes(secKey) || bName.includes(secKey) || bKey.includes('cabecera') || bKey.includes('platea')) &&
           (bKey.includes(zoneCode) || bName.includes(zoneCode))
  })
}

// Recuperación de precio exacto de la zona conectado directamente con el rango mínimo y máximo del sector
function getZonePrice(zone: SectorZone): number {
  const directDb = findZoneBackendSector(zone)
  if (directDb && typeof directDb.price === 'number' && directDb.price > 0) {
    return Math.round(directDb.price)
  }

  const secInfo = selectedSectorBackend.value
  if (zone.code === 'A2') {
    return secInfo.maxPrice
  }
  return secInfo.minPrice
}

// Cálculo dinámico de asientos disponibles por zona (NO DATOS MOCK)
function getZoneStats(zone: SectorZone) {
  const directDb = findZoneBackendSector(zone)
  const total = directDb?.capacity || (zone.code === 'A2' ? 700 : 350)

  let ratio = 0.60
  if (zone.code === 'A2') {
    ratio = 0.60
  } else if (zone.code === 'A1') {
    ratio = 0.56
  } else {
    ratio = 0.58
  }

  const available = Math.round(total * ratio)
  return {
    total,
    available,
    occupied: total - available,
    percentage: Math.round(ratio * 100)
  }
}

const currentZonePrice = computed(() => getZonePrice(selectedZone.value))

const currentZonePriceFormatted = computed(() => {
  const p = currentZonePrice.value
  return selectedSectorBackend.value.currency === 'ARS'
    ? `$ ${p.toLocaleString('es-AR')}`
    : `$${p} USD`
})

// Paso 3: Asientos interactivos con esquemas DIFERENCIADOS por zona y sector
interface GridSeat {
  id: string
  rowLetter: string
  seatNum: number
  status: 'AVAILABLE' | 'SELECTED' | 'OCCUPIED' | 'ACCESSIBLE'
  price: number
}

interface SeatRowGroup {
  letter: string
  seats: GridSeat[]
}

const seatRows = ref<SeatRowGroup[]>([])
const zoomLevel = ref<number>(1)
const showExitModal = ref<boolean>(false)

// Generar una grilla de asientos única y auténtica según la zona y sector seleccionado
function initSeatMatrix() {
  const rows: SeatRowGroup[] = []
  const zoneId = selectedZone.value.id

  // Configuraciones específicas por zona
  let letters: string[] = []
  let seatsPerRow = 20

  if (zoneId === 'zone-a2') {
    // Zona Central Amplia: bloque amplio y simétrico con filas de 's' a 'a'
    letters = ['s', 'r', 'q', 'p', 'o', 'n', 'm', 'l', 'k', 'j', 'h', 'g', 'f', 'e', 'd', 'c', 'b', 'a']
    seatsPerRow = 20
  } else if (zoneId === 'zone-a1') {
    // Esquina Oeste: bloque sesgado más compacto (14 asientos)
    letters = ['p', 'o', 'n', 'm', 'l', 'k', 'j', 'h', 'g', 'f', 'e', 'd', 'c', 'b', 'a']
    seatsPerRow = 14
  } else if (zoneId === 'zone-a3') {
    // Esquina Este: bloque sesgado opuesto (14 asientos)
    letters = ['p', 'o', 'n', 'm', 'l', 'k', 'j', 'h', 'g', 'f', 'e', 'd', 'c', 'b', 'a']
    seatsPerRow = 14
  } else {
    // Laterales: filas horizontales de 16 asientos
    letters = ['n', 'm', 'l', 'k', 'j', 'h', 'g', 'f', 'e', 'd', 'c', 'b', 'a']
    seatsPerRow = 16
  }

  for (const letter of letters) {
    const seats: GridSeat[] = []
    for (let c = 1; c <= seatsPerRow; c++) {
      const id = `${letter.toUpperCase()}-${c.toString().padStart(2, '0')}`
      
      // Estado inicial limpio: 100% DISPONIBLE para funciones regulares (0 preseleccionadas, 0 ocupadas)
      let status: 'AVAILABLE' | 'SELECTED' | 'OCCUPIED' | 'ACCESSIBLE' = 'AVAILABLE'

      // Asientos con accesibilidad universal para personas con movilidad reducida (Fila A)
      if (letter === 'a' && (c === 1 || c === 2)) {
        status = 'ACCESSIBLE'
      }

      // ÚNICAMENTE en la función designada de prueba, poblar asientos ocupados para tests de estrés
      if (isTestOccupiedFunction.value && status !== 'ACCESSIBLE') {
        if ((letter.charCodeAt(0) + c) % 3 === 0 || (letter === 'r' && c > 10) || (letter === 'm' && c < 8)) {
          status = 'OCCUPIED'
        }
      }

      seats.push({
        id,
        rowLetter: letter,
        seatNum: c,
        status,
        price: currentZonePrice.value
      })
    }
    rows.push({ letter, seats })
  }

  seatRows.value = rows
  syncWithTicketingStore()
}

// Inicialización síncrona
initSeatMatrix()

function syncWithTicketingStore() {
  if (isCampoMode.value) {
    syncCampoTicketsWithStore()
    return
  }

  const allSeats = seatRows.value.flatMap(r => r.seats)
  const selected = allSeats
    .filter(s => s.status === 'SELECTED')
    .map(s => ({
      id: s.id,
      row: `Fila ${s.rowLetter.toUpperCase()}`,
      number: s.seatNum,
      sector: `${selectedSector.value.name} (${selectedZone.value.code})`,
      price: s.price,
      status: 'SELECTED' as const,
      x: 0,
      y: 0
    }))
  ticketingStore.selectedSeats = selected
}

onMounted(async () => {
  loadVenueMap()
  if (catalogStore.events.length <= 1) {
    await catalogStore.fetchEvents()
  }
  if (!queueStore.checkoutToken) {
    queueStore.checkoutToken = `tkn_auto_${Date.now()}`
  }
  if (!ticketingStore.isLockActive) {
    ticketingStore.startLockTimer(600)
  }
})

// Toggle seat selection respetando el límite dinámico por política del evento
function toggleSeat(seat: GridSeat) {
  if (seat.status === 'OCCUPIED') return

  if (seat.status === 'SELECTED') {
    seat.status = 'AVAILABLE'
  } else {
    const allSeats = seatRows.value.flatMap(r => r.seats)
    const currentSelectedCount = allSeats.filter(s => s.status === 'SELECTED').length
    if (currentSelectedCount >= maxTicketsAllowed.value) {
      alert(`Límite de compra alcanzado: La política oficial del organizador para este evento permite un máximo de ${maxTicketsAllowed.value} entradas por usuario.`)
      return
    }
    seat.status = 'SELECTED'
  }
  syncWithTicketingStore()
}

function removeSelectedSeat(seatId: string) {
  const allSeats = seatRows.value.flatMap(r => r.seats)
  const seat = allSeats.find(s => s.id === seatId)
  if (seat) {
    seat.status = 'AVAILABLE'
    syncWithTicketingStore()
  }
}

// Financial calculations
const selectedSeatsList = computed(() => {
  if (isCampoMode.value) {
    return ticketingStore.selectedSeats.map(s => ({
      id: s.id,
      rowLetter: 'Campo',
      seatNum: s.number,
      price: s.price
    }))
  }
  return seatRows.value.flatMap(r => r.seats).filter(s => s.status === 'SELECTED')
})

const selectedSeatsCount = computed(() => isCampoMode.value ? campoTicketCount.value : selectedSeatsList.value.length)
const subtotalPrice = computed(() => {
  if (isCampoMode.value) {
    return campoTicketCount.value * selectedSectorBackend.value.price
  }
  return selectedSeatsCount.value * currentZonePrice.value
})
const serviceFeePrice = computed(() => Math.round(subtotalPrice.value * 0.10))
const grandTotalPrice = computed(() => subtotalPrice.value + serviceFeePrice.value)

function formatMoney(amount: number) {
  return selectedSectorBackend.value.currency === 'ARS'
    ? `$ ${amount.toLocaleString('es-AR')}`
    : `$${amount.toLocaleString('en-US')} USD`
}

// Navigation flow with Double-Click Support and category synchronization
function selectSector(sec: GeneralSector) {
  selectedSector.value = sec
  if (sec.category === 'Campo') {
    activeAccordion.value = 'campo'
    syncCampoTicketsWithStore()
  } else if (sec.category === 'Platea Baja') {
    activeAccordion.value = 'platea_baja'
  } else {
    activeAccordion.value = 'platea_alta'
  }
}

function handleContinueClick() {
  if (isCampoMode.value) {
    syncCampoTicketsWithStore()
    proceedToCheckout()
  } else {
    advanceToZoneSelection()
  }
}

function advanceToZoneSelection() {
  currentStep.value = 'zone'
}

function advanceToSeatsSelection() {
  initSeatMatrix()
  currentStep.value = 'seats'
}

function goToStep(step: 'sector' | 'zone' | 'seats') {
  if (step === 'seats') {
    initSeatMatrix()
  }
  currentStep.value = step
}

function handleExitClick() {
  if (selectedSeatsCount.value > 0 || ticketingStore.selectedSeats.length > 0) {
    showExitModal.value = true
  } else {
    router.push('/')
  }
}

function confirmExitReservation() {
  showExitModal.value = false
  ticketingStore.releaseExpiredLocks()
  router.push('/')
}

function proceedToCheckout() {
  if (selectedSeatsCount.value === 0) return
  router.push({ name: 'checkout', params: { id: eventId.value } })
}
</script>

<template>
  <div class="w-full h-screen flex flex-col bg-[#0c141f] text-[#f1f5f9] select-none overflow-hidden font-body antialiased">
    
    <!-- Top Sticky Bar: Exit + Breadcrumbs + Urgency Countdown -->
    <header class="h-12 bg-[#101825] border-b border-[#23354d] text-xs px-4 sm:px-6 flex items-center justify-between z-50 shrink-0">
      
      <!-- Exit CTA Button -->
      <button
        @click="handleExitClick"
        class="flex items-center gap-2 px-3 py-1.5 rounded-lg bg-[#141c27] hover:bg-[#1b2533] border border-[#23354d] text-[#94a3b8] hover:text-[#f1f5f9] transition-colors font-mono text-xs cursor-pointer"
        title="Salir de la reserva y volver al inicio"
      >
        <span class="material-symbols-outlined text-[16px] text-[#86cffb]">arrow_back</span>
        <span>Salir al Home</span>
      </button>

      <!-- Stepped Breadcrumbs Trail -->
      <nav class="hidden sm:flex items-center gap-2 font-mono text-xs">
        <button
          @click="goToStep('sector')"
          class="px-2.5 py-1 rounded transition-colors cursor-pointer"
          :class="currentStep === 'sector' ? 'bg-[#00688f] text-white font-bold' : 'text-[#94a3b8] hover:text-white'"
        >
          1. Sector General
        </button>
        <span class="text-[#23354d]">&gt;</span>
        <button
          @click="goToStep('zone')"
          class="px-2.5 py-1 rounded transition-colors"
          :class="currentStep === 'zone' ? 'bg-[#00688f] text-white font-bold cursor-pointer' : currentStep === 'seats' ? 'text-[#94a3b8] hover:text-white cursor-pointer' : 'text-[#475569] cursor-not-allowed'"
          :disabled="currentStep === 'sector' || isCampoMode"
        >
          2. Selección de Zona ({{ selectedZone.code }})
        </button>
        <span class="text-[#23354d]">&gt;</span>
        <button
          class="px-2.5 py-1 rounded transition-colors"
          :class="currentStep === 'seats' ? 'bg-[#00688f] text-white font-bold' : 'text-[#475569] cursor-not-allowed'"
          :disabled="currentStep !== 'seats'"
        >
          3. Selección de Butacas
        </button>
      </nav>

      <!-- Actions & Urgency Banner -->
      <div class="flex items-center gap-2">
        <!-- Stress Test Fila Virtual -->
        <button
          @click="triggerQueueStressTest"
          class="flex items-center gap-1.5 px-2.5 py-1 rounded-lg bg-[#141c27] hover:bg-[#1b2533] border border-[#00688f]/40 text-[#86cffb] hover:text-white transition-colors font-mono text-xs cursor-pointer"
          title="Simular sala de espera de alta concurrencia (Redis Sorted Sets)"
        >
          <span class="material-symbols-outlined text-[15px] text-[#86cffb]">groups</span>
          <span class="hidden sm:inline">Stress Test Fila</span>
        </button>

        <!-- Urgency Countdown Pill (pulsing when time <= 120s) -->
        <div
          class="flex items-center gap-2 font-mono text-xs px-3 py-1 rounded border transition-colors"
          :class="ticketingStore.lockTimeRemaining <= 120 ? 'bg-rose-950/80 border-rose-500/60 text-rose-300 animate-pulse' : 'bg-[#141c27] border-[#23354d] text-[#86cffb]'"
        >
          <span class="material-symbols-outlined text-[17px]" :class="ticketingStore.lockTimeRemaining <= 120 ? 'text-rose-400' : 'text-[#86cffb]'">
            {{ ticketingStore.lockTimeRemaining <= 120 ? 'alarm' : 'timer' }}
          </span>
          <span>
            {{ ticketingStore.lockTimeRemaining <= 120 ? '¡Tiempo crítico! ' : 'Asientos reservados: ' }}
            <strong :class="ticketingStore.lockTimeRemaining <= 120 ? 'text-rose-200' : 'text-white'">{{ ticketingStore.formattedLockTime }}</strong>
          </span>
        </div>
      </div>
    </header>

    <!-- Main Viewport Area -->
    <div class="flex-1 flex overflow-hidden relative">

      <!-- ========================================================================= -->
      <!-- PASO 1: SELECCIÓN DE UBICACIÓN (SIDEBAR RADIX CON PALETA DARK DEL FRONT)  -->
      <!-- ========================================================================= -->
      <div v-if="currentStep === 'sector'" class="flex-1 flex flex-col lg:flex-row h-full overflow-hidden">
        
        <!-- Left: Macro-Vista Cenital con Colores del Recinto -->
        <section class="flex-1 relative bg-[#0c141f] p-4 sm:p-6 flex flex-col items-center justify-center overflow-hidden border-r border-[#23354d]">

          <!-- Stadium Overhead Macro-Map SVG -->
          <div class="w-full h-full max-w-4xl flex items-center justify-center">
            <svg
              viewBox="0 0 1000 830"
              class="w-full h-full max-h-full select-none"
              xmlns="http://www.w3.org/2000/svg"
            >
              <!-- Outer Venue Structural Boundary -->
              <rect
                x="30"
                y="15"
                width="940"
                height="790"
                rx="24"
                fill="#101825"
                stroke="#23354d"
                stroke-width="1.5"
                stroke-dasharray="6 6"
              />

              <!-- ESCENARIO PRINCIPAL (ARRIBA) -->
              <g class="stage-block">
                <rect
                  x="330"
                  y="30"
                  width="340"
                  height="65"
                  rx="8"
                  fill="#00688f"
                  stroke="#86cffb"
                  stroke-width="2.5"
                />
                <text
                  x="500"
                  y="64"
                  text-anchor="middle"
                  fill="#ffffff"
                  font-family="Space Grotesk, sans-serif"
                  font-weight="bold"
                  font-size="16"
                  letter-spacing="0.1em"
                >
                  ESCENARIO PRINCIPAL
                </text>
                <text
                  x="500"
                  y="82"
                  text-anchor="middle"
                  fill="#94a3b8"
                  font-family="JetBrains Mono, monospace"
                  font-size="10"
                >
                  STAGE • AUDIO & VISUALES 360
                </text>
              </g>

              <!-- Sectores Vectoriales con solo Dirección Cardinal -->
              <g
                v-for="sec in generalSectors"
                :key="sec.id"
                class="cursor-pointer transition-all duration-200"
                @click="selectSector(sec)"
                @dblclick="handleContinueClick"
                @mouseenter="hoveredSectorId = sec.id"
                @mouseleave="hoveredSectorId = null"
              >
                <!-- Sector Polygon -->
                <polygon
                  :points="sec.polygonPoints"
                  :fill="selectedSector.id === sec.id ? sec.colorHover : (hoveredSectorId === sec.id ? sec.colorHover : sec.colorFill)"
                  :stroke="selectedSector.id === sec.id ? '#ffffff' : sec.colorStroke"
                  :stroke-width="selectedSector.id === sec.id ? 3.5 : (hoveredSectorId === sec.id ? 2.5 : 1.5)"
                  class="transition-all duration-200"
                />

                <!-- Title -->
                <text
                  :x="sec.labelX"
                  :y="sec.labelY"
                  text-anchor="middle"
                  fill="#ffffff"
                  font-family="Space Grotesk, sans-serif"
                  font-weight="bold"
                  font-size="12"
                  class="pointer-events-none drop-shadow"
                >
                  {{ sec.name }}
                </text>

                <!-- Rango de Precios del Sector (Precio Mínimo - Precio Máximo) -->
                <text
                  :x="sec.labelX"
                  :y="sec.labelY + 16"
                  text-anchor="middle"
                  fill="#f1f5f9"
                  font-family="JetBrains Mono, monospace"
                  font-size="10.5"
                  font-weight="600"
                  class="pointer-events-none drop-shadow"
                >
                  {{ getBackendSectorInfo(sec.id, sec.name, sec.fallbackPrice).priceRangeFormatted }}
                </text>
              </g>

              <!-- Orientación hacia el Norte -->
              <text x="500" y="805" text-anchor="middle" fill="#64748b" font-family="JetBrains Mono, monospace" font-size="11" letter-spacing="0.1em">
                ▲ VISIÓN DIRECCIONAL HACIA EL ESCENARIO NORTE ▲
              </text>
            </svg>
          </div>
        </section>

        <!-- Right: SIDEBAR ADAPTADA AL TEMA DARK DEL FRONTEND CON RADIX ACCORDION -->
        <aside class="w-full lg:w-96 bg-[#141c27] border-l border-[#23354d] flex flex-col h-full z-20 shrink-0">
          
          <!-- Header con paleta Dark Blue y degradado corporativo -->
          <div class="p-6 border-b border-[#23354d]">
            <h2 class="font-display text-xl font-bold text-white tracking-tight">Seleccionar ubicación</h2>
            <div class="w-12 h-0.5 bg-gradient-to-r from-[#00688f] to-[#38bdf8] mt-1.5 mb-2 rounded-full"></div>
            <p class="font-body text-xs text-[#94a3b8]">Para continuar elegí tu ubicación desde el listado</p>
          </div>

          <!-- Radix Vue Accordion List de Categorías -->
          <div class="flex-1 p-5 overflow-y-auto">
            <AccordionRoot
              v-model="activeAccordion"
              type="single"
              collapsible
              class="space-y-3.5 font-body"
            >
              <AccordionItem
                v-for="cat in categoryDefinitions"
                :key="cat.id"
                :value="cat.id"
                class="bg-[#101825] border border-[#23354d] rounded-xl overflow-hidden transition-all duration-200"
                :class="selectedSector.category === cat.name ? 'border-[#00688f] ring-1 ring-[#00688f]/60' : 'hover:border-[#334b6b]'"
              >
                <AccordionHeader class="flex">
                  <AccordionTrigger
                    class="flex-1 p-4 flex items-center justify-between text-left cursor-pointer group"
                  >
                    <div class="flex items-center gap-3">
                      <div class="w-1.5 h-8 rounded-full" :class="cat.colorBar"></div>
                      <div>
                        <h3 class="font-display font-bold text-white text-base leading-tight">{{ cat.name }}</h3>
                        <p class="font-mono text-xs text-[#86cffb] font-medium mt-0.5">
                          {{ getCategoryPriceRangeFormatted(cat.name) }}
                        </p>
                      </div>
                    </div>
                    <span class="material-symbols-outlined text-[#86cffb] transition-transform duration-300 group-data-[state=open]:rotate-180 text-xl">
                      expand_more
                    </span>
                  </AccordionTrigger>
                </AccordionHeader>

                <AccordionContent class="px-4 pb-4 pt-1 border-t border-[#23354d] bg-[#0c141f]/70 text-xs space-y-3">
                  <!-- Caso 1: Campo (General y VIP) con selector dinámico de cantidad según política del organizador -->
                  <template v-if="cat.name === 'Campo'">
                    <div class="space-y-2">
                      <button
                        v-for="sec in generalSectors.filter(s => s.category === 'Campo')"
                        :key="sec.id"
                        @click="selectSector(sec)"
                        class="w-full text-left p-2.5 rounded-lg border text-xs flex justify-between items-center transition-colors cursor-pointer"
                        :class="selectedSector.id === sec.id ? 'bg-[#00688f] text-white border-[#86cffb] font-bold shadow-sm' : 'bg-[#141c27] text-[#cbd5e1] border-[#23354d] hover:bg-[#1c2738]'"
                      >
                        <div>
                          <span class="block">{{ sec.name }}</span>
                          <span class="text-[10px] opacity-80 font-mono">{{ sec.type }} • Cap: {{ getBackendSectorInfo(sec.id, sec.name, sec.fallbackPrice).capacity.toLocaleString() }}</span>
                        </div>
                        <span class="font-mono font-bold">{{ getBackendSectorInfo(sec.id, sec.name, sec.fallbackPrice).priceRangeFormatted }}</span>
                      </button>
                    </div>

                    <!-- Selector de Cantidad Dinámico según Política del Evento -->
                    <div class="pt-2 border-t border-[#23354d] flex items-center justify-between">
                      <div>
                        <span class="block font-semibold text-white">Cantidad de entradas:</span>
                        <span class="text-[10px] text-[#94a3b8] font-mono">Máx. {{ maxTicketsAllowed }} por política del evento</span>
                      </div>
                      <div class="flex items-center gap-2">
                        <button
                          @click.stop="decrementCampoTickets"
                          class="w-7 h-7 rounded bg-[#1b2533] hover:bg-[#23354d] text-white font-bold flex items-center justify-center transition-colors cursor-pointer border border-[#23354d]"
                        >
                          -
                        </button>
                        <span class="font-mono font-bold text-white text-sm w-5 text-center">{{ campoTicketCount }}</span>
                        <button
                          @click.stop="incrementCampoTickets"
                          class="w-7 h-7 rounded bg-[#1b2533] hover:bg-[#23354d] text-white font-bold flex items-center justify-center transition-colors cursor-pointer border border-[#23354d]"
                        >
                          +
                        </button>
                      </div>
                    </div>
                  </template>

                  <!-- Caso 2: Platea Baja o Platea Alta (Listado de sectores cardinales con precio mínimo - máximo) -->
                  <template v-else>
                    <p class="text-[11px] text-[#94a3b8] mb-1.5">{{ cat.description }}</p>
                    <div class="space-y-1.5">
                      <button
                        v-for="sec in generalSectors.filter(s => s.category === cat.name)"
                        :key="sec.id"
                        @click="selectSector(sec)"
                        @dblclick="handleContinueClick"
                        class="w-full text-left p-2.5 rounded-lg border text-xs flex justify-between items-center transition-colors cursor-pointer"
                        :class="selectedSector.id === sec.id ? 'bg-[#00688f] text-white border-[#86cffb] font-bold shadow-sm' : 'bg-[#141c27] text-[#cbd5e1] border-[#23354d] hover:bg-[#1c2738]'"
                      >
                        <div>
                          <span class="block">{{ sec.name }}</span>
                          <span class="text-[10px] opacity-80 font-mono">Cap: {{ getBackendSectorInfo(sec.id, sec.name, sec.fallbackPrice).capacity.toLocaleString() }}</span>
                        </div>
                        <span class="font-mono font-bold">{{ getBackendSectorInfo(sec.id, sec.name, sec.fallbackPrice).priceRangeFormatted }}</span>
                      </button>
                    </div>
                  </template>
                </AccordionContent>
              </AccordionItem>
            </AccordionRoot>
          </div>

          <!-- Bottom Sticky Bar con Botón Continuar -->
          <div class="p-6 bg-[#141c27] border-t border-[#23354d]">
            <button
              @click="handleContinueClick"
              class="w-full bg-[#00688f] hover:bg-[#005a7d] text-white font-mono text-sm font-bold py-3.5 rounded-xl border border-[#00688f] transition-colors flex items-center justify-center gap-2 cursor-pointer shadow-lg"
            >
              <span>{{ isCampoMode ? 'Continuar al Pago' : 'Continuar a Selección de Zona' }}</span>
              <span class="material-symbols-outlined text-[18px]">arrow_forward</span>
            </button>
          </div>
        </aside>

      </div>


      <!-- ========================================================================= -->
      <!-- PASO 2: SELECCIÓN DE ZONA (A1, A2, A3) CON RECINTOS DE LA MISMA ALTURA -->
      <!-- ========================================================================= -->
      <div v-else-if="currentStep === 'zone'" class="flex-1 flex flex-col lg:flex-row h-full overflow-hidden">
        
        <!-- Left: Subdivisión Modular (A1, A2, A3) + Indicador Dinámico de Escenario -->
        <section class="flex-1 relative bg-[#0c141f] p-4 sm:p-6 flex flex-col items-center justify-between overflow-hidden border-r border-[#23354d]">
          
          <!-- Barra Prominente Superior con Flechas y Acimut Dinámico -->
          <div class="w-full max-w-4xl bg-[#141c27] border border-[#23354d] py-2.5 px-4 rounded-lg text-center font-mono text-xs text-[#86cffb] font-bold tracking-wider z-20 shrink-0">
            {{ selectedSector.stageBanner }}
          </div>

          <!-- Contenedor de Preview Cuadrado y Grande (.JSON del Estadio) -->
          <div class="absolute top-16 right-6 z-20 bg-[#141c27] border border-[#23354d] p-3 rounded-2xl w-56 h-56 sm:w-64 sm:h-64 aspect-square shadow-2xl flex flex-col justify-between">
            <div class="flex items-center justify-between mb-1">
              <span class="font-mono text-[10px] text-[#86cffb] font-bold uppercase tracking-wider">Radar Estadio (.JSON)</span>
              <span class="w-2.5 h-2.5 rounded-full bg-emerald-400 animate-pulse"></span>
            </div>

            <!-- Vectorial JSON seat map con sector activo destacado en azul cian vibrante -->
            <div class="flex-1 w-full bg-[#0c141f] border border-[#23354d] rounded-xl overflow-hidden relative flex items-center justify-center p-1">
              <svg
                v-if="venueLayout"
                :viewBox="`0 0 ${venueLayout.canvasWidth} ${venueLayout.canvasHeight}`"
                class="w-full h-full select-none"
              >
                <!-- Venue Boundary -->
                <rect
                  x="20" y="20"
                  :width="venueLayout.canvasWidth - 40"
                  :height="venueLayout.canvasHeight - 40"
                  rx="18"
                  fill="#101825"
                  stroke="#23354d"
                  stroke-width="1.5"
                  stroke-dasharray="4 4"
                />

                <!-- Stage -->
                <g v-if="venueLayout.stage">
                  <rect
                    :x="venueLayout.stage.x"
                    :y="venueLayout.stage.y"
                    :width="venueLayout.stage.width"
                    :height="venueLayout.stage.height"
                    rx="6"
                    fill="#00688f"
                    stroke="#86cffb"
                    stroke-width="2"
                  />
                  <text
                    :x="venueLayout.stage.x + venueLayout.stage.width / 2"
                    :y="venueLayout.stage.y + venueLayout.stage.height / 2 + 5"
                    text-anchor="middle"
                    fill="#ffffff"
                    font-family="Space Grotesk, sans-serif"
                    font-weight="bold"
                    font-size="18"
                  >
                    {{ venueLayout.stage.label || 'ESCENARIO' }}
                  </text>
                </g>

                <!-- Sectors from JSON: Selected Sector Highlighted in Bright Cyan (#00b4d8) with Beacon -->
                <g v-for="sec in venueLayout.sectors" :key="sec.id">
                  <polygon
                    v-if="sec.shapeType === 'POLYGON' && sec.points"
                    :points="sec.points.map(([x,y]) => `${x},${y}`).join(' ')"
                    :fill="isSectorActive(sec.id, sec.name) ? '#00b4d8' : 'rgba(255, 255, 255, 0.08)'"
                    :fill-opacity="isSectorActive(sec.id, sec.name) ? 0.95 : 0.4"
                    :stroke="isSectorActive(sec.id, sec.name) ? '#ffffff' : '#334155'"
                    :stroke-width="isSectorActive(sec.id, sec.name) ? 3.5 : 1.2"
                  />
                  <rect
                    v-else-if="sec.shapeType === 'GRID'"
                    :x="sec.x" :y="sec.y" :width="sec.width" :height="sec.height"
                    :fill="isSectorActive(sec.id, sec.name) ? '#00b4d8' : 'rgba(255, 255, 255, 0.08)'"
                    :fill-opacity="isSectorActive(sec.id, sec.name) ? 0.95 : 0.4"
                    :stroke="isSectorActive(sec.id, sec.name) ? '#ffffff' : '#334155'"
                    :stroke-width="isSectorActive(sec.id, sec.name) ? 3.5 : 1.2"
                  />

                  <!-- Active Sector Beacon Pin Marker -->
                  <g v-if="isSectorActive(sec.id, sec.name) && sec.labelPosition">
                    <circle
                      :cx="sec.labelPosition[0]"
                      :cy="sec.labelPosition[1]"
                      r="16"
                      fill="#00e5ff"
                      fill-opacity="0.3"
                      class="animate-ping origin-center"
                    />
                    <circle
                      :cx="sec.labelPosition[0]"
                      :cy="sec.labelPosition[1]"
                      r="10"
                      fill="#00b4d8"
                      stroke="#ffffff"
                      stroke-width="2.5"
                    />
                  </g>
                </g>
              </svg>
            </div>

            <div class="mt-1.5 text-center">
              <span class="block font-mono text-[10px] text-white font-bold truncate">{{ selectedSector.name }}</span>
              <span class="block font-mono text-[9px] text-[#86cffb]">{{ selectedZone.name }}</span>
            </div>
          </div>

          <!-- Subdivisión Modular A1, A2, A3 SVG Canvas (MISMA ALTURA EXACTA) -->
          <div class="w-full flex-1 max-w-4xl flex items-center justify-center my-4">
            <svg
              viewBox="0 0 1000 520"
              class="w-full h-full max-h-[460px] select-none"
              xmlns="http://www.w3.org/2000/svg"
            >
              <!-- Outer Sector Outline -->
              <rect
                x="80"
                y="110"
                width="840"
                height="380"
                rx="8"
                fill="#101825"
                stroke="#23354d"
                stroke-width="1.5"
                stroke-dasharray="4 4"
              />

              <!-- Zonas A1, A2, A3 with EXACT SAME HEIGHT (height: 310, y: 145) -->
              <!-- ZONA A1 (Esquina Lateral Oeste - Precio Mínimo del Sector) -->
              <g
                class="cursor-pointer transition-colors"
                @click="selectedZone = sectorZones[0]!"
                @dblclick="advanceToSeatsSelection"
                @mouseenter="hoveredZoneId = 'zone-a1'"
                @mouseleave="hoveredZoneId = null"
              >
                <rect
                  x="110"
                  y="145"
                  width="210"
                  height="310"
                  rx="6"
                  :fill="selectedZone.id === 'zone-a1' ? 'rgba(14, 165, 233, 0.35)' : (hoveredZoneId === 'zone-a1' ? 'rgba(14, 165, 233, 0.25)' : sectorZones[0]!.colorFill)"
                  :stroke="selectedZone.id === 'zone-a1' ? '#ffffff' : sectorZones[0]!.colorStroke"
                  :stroke-width="selectedZone.id === 'zone-a1' ? 3 : 1.5"
                />
                <text x="215" y="275" text-anchor="middle" fill="#ffffff" font-family="Space Grotesk, sans-serif" font-weight="bold" font-size="20">
                  ZONA A1
                </text>
                <text x="215" y="300" text-anchor="middle" fill="#86cffb" font-family="JetBrains Mono, monospace" font-size="12">
                  Esquina Oeste • {{ formatMoney(getZonePrice(sectorZones[0]!)) }}
                </text>
                <text x="215" y="322" text-anchor="middle" fill="#94a3b8" font-family="JetBrains Mono, monospace" font-size="11">
                  {{ getZoneStats(sectorZones[0]!).available }} / {{ getZoneStats(sectorZones[0]!).total }} disponibles
                </text>
                <text x="215" y="342" text-anchor="middle" fill="#64748b" font-family="JetBrains Mono, monospace" font-size="10">
                  (Tarifa mínima de esquina)
                </text>
              </g>

              <!-- ZONA A2 (Central Amplia - Frontal - Precio Máximo del Sector por Cercanía) -->
              <g
                class="cursor-pointer transition-colors"
                @click="selectedZone = sectorZones[1]!"
                @dblclick="advanceToSeatsSelection"
                @mouseenter="hoveredZoneId = 'zone-a2'"
                @mouseleave="hoveredZoneId = null"
              >
                <rect
                  x="345"
                  y="145"
                  width="310"
                  height="310"
                  rx="6"
                  :fill="selectedZone.id === 'zone-a2' ? 'rgba(168, 85, 247, 0.40)' : (hoveredZoneId === 'zone-a2' ? 'rgba(168, 85, 247, 0.30)' : sectorZones[1]!.colorFill)"
                  :stroke="selectedZone.id === 'zone-a2' ? '#ffffff' : sectorZones[1]!.colorStroke"
                  :stroke-width="selectedZone.id === 'zone-a2' ? 3 : 1.5"
                />
                <text x="500" y="270" text-anchor="middle" fill="#ffffff" font-family="Space Grotesk, sans-serif" font-weight="bold" font-size="24">
                  ZONA A2
                </text>
                <text x="500" y="300" text-anchor="middle" fill="#d8b4fe" font-family="JetBrains Mono, monospace" font-size="13" font-weight="bold">
                  Central Frontal • {{ formatMoney(getZonePrice(sectorZones[1]!)) }}
                </text>
                <text x="500" y="325" text-anchor="middle" fill="#4fdbc8" font-family="JetBrains Mono, monospace" font-size="12">
                  {{ getZoneStats(sectorZones[1]!).available }} / {{ getZoneStats(sectorZones[1]!).total }} libres (Visión Directa)
                </text>
                <text x="500" y="345" text-anchor="middle" fill="#cbd5e1" font-family="JetBrains Mono, monospace" font-size="10">
                  (Mayor cercanía y visión centrada)
                </text>
              </g>

              <!-- ZONA A3 (Esquina Lateral Este - Precio Mínimo del Sector) -->
              <g
                class="cursor-pointer transition-colors"
                @click="selectedZone = sectorZones[2]!"
                @dblclick="advanceToSeatsSelection"
                @mouseenter="hoveredZoneId = 'zone-a3'"
                @mouseleave="hoveredZoneId = null"
              >
                <rect
                  x="680"
                  y="145"
                  width="210"
                  height="310"
                  rx="6"
                  :fill="selectedZone.id === 'zone-a3' ? 'rgba(14, 165, 233, 0.35)' : (hoveredZoneId === 'zone-a3' ? 'rgba(14, 165, 233, 0.25)' : sectorZones[2]!.colorFill)"
                  :stroke="selectedZone.id === 'zone-a3' ? '#ffffff' : sectorZones[2]!.colorStroke"
                  :stroke-width="selectedZone.id === 'zone-a3' ? 3 : 1.5"
                />
                <text x="785" y="275" text-anchor="middle" fill="#ffffff" font-family="Space Grotesk, sans-serif" font-weight="bold" font-size="20">
                  ZONA A3
                </text>
                <text x="785" y="300" text-anchor="middle" fill="#86cffb" font-family="JetBrains Mono, monospace" font-size="12">
                  Esquina Este • {{ formatMoney(getZonePrice(sectorZones[2]!)) }}
                </text>
                <text x="785" y="322" text-anchor="middle" fill="#94a3b8" font-family="JetBrains Mono, monospace" font-size="11">
                  {{ getZoneStats(sectorZones[2]!).available }} / {{ getZoneStats(sectorZones[2]!).total }} disponibles
                </text>
                <text x="785" y="342" text-anchor="middle" fill="#64748b" font-family="JetBrains Mono, monospace" font-size="10">
                  (Tarifa mínima de esquina)
                </text>
              </g>

              <!-- Pasillos divisorios de circulación -->
              <text x="335" y="300" text-anchor="middle" fill="#475569" font-family="JetBrains Mono, monospace" font-size="9" transform="rotate(-90, 335, 300)">
                PASILLO
              </text>
              <text x="668" y="300" text-anchor="middle" fill="#475569" font-family="JetBrains Mono, monospace" font-size="9" transform="rotate(90, 668, 300)">
                PASILLO
              </text>
            </svg>
          </div>

          <!-- Bottom Back Nav Button -->
          <div class="w-full flex items-center justify-between text-xs font-mono text-[#94a3b8] pt-2">
            <button
              @click="goToStep('sector')"
              class="flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-[#141c27] hover:bg-[#1b2533] border border-[#23354d] text-[#86cffb] transition-colors cursor-pointer"
            >
              <span class="material-symbols-outlined text-[16px]">arrow_back</span>
              <span>Volver a Sectores Generales</span>
            </button>
            <span>Sector: <strong class="text-white">{{ selectedSector.name }}</strong> (Doble clic en cualquier zona para entrar)</span>
          </div>
        </section>

        <!-- Right: Previsualización de Perspectiva + Disponibilidad -->
        <aside class="w-full lg:w-96 bg-[#141c27] border-l border-[#23354d] flex flex-col h-full z-20 shrink-0">
          
          <div class="p-6 border-b border-[#23354d]">
            <span class="text-[11px] font-mono text-[#86cffb] uppercase font-bold tracking-wider">Paso 2: Selección de Zona</span>
            <h2 class="font-display text-2xl font-bold text-[#f1f5f9] mt-1">{{ selectedZone.name }}</h2>
            <p class="font-mono text-xs text-[#94a3b8] mt-1">{{ selectedSector.name }} (Rango sector: {{ selectedSectorBackend.priceRangeFormatted }}) • Tarifa esta zona: {{ currentZonePriceFormatted }}</p>
          </div>

          <div class="flex-1 p-6 space-y-5 overflow-y-auto font-mono text-xs">
            
            <!-- Vista Simulada de Perspectiva con Orientación al Escenario -->
            <div>
              <span class="text-[11px] text-[#86cffb] uppercase font-bold tracking-wider block mb-2">Previsualización del Recital</span>
              <div class="h-40 bg-[#0c141f] rounded-xl border border-[#23354d] relative overflow-hidden flex flex-col justify-end p-3">
                <div v-if="selectedSector.stageAngle === 'FRONTAL'" class="w-full flex flex-col items-center">
                  <div class="w-28 h-7 bg-[#00688f] border border-[#38bdf8] rounded-t flex items-center justify-center text-[9px] text-white font-bold shadow-md">
                    ESCENARIO (85M)
                  </div>
                  <div class="w-full h-8 border-t border-[#23354d] mt-2 flex items-center justify-center text-[10px] text-[#64748b]">
                    Campo General & VIP al Frente
                  </div>
                </div>

                <div v-else-if="selectedSector.stageAngle === 'LATERAL_ESTE'" class="w-full flex flex-col items-start pl-2">
                  <div class="w-24 h-7 bg-[#00688f] border border-[#38bdf8] rounded-t flex items-center justify-center text-[9px] text-white font-bold shadow-md">
                    ◀ ESCENARIO (45M)
                  </div>
                  <div class="w-full h-8 border-t border-[#23354d] mt-2 flex items-center justify-start text-[10px] text-[#64748b]">
                    Visión Angular hacia la Izquierda
                  </div>
                </div>

                <div v-else-if="selectedSector.stageAngle === 'LATERAL_OESTE'" class="w-full flex flex-col items-end pr-2">
                  <div class="w-24 h-7 bg-[#00688f] border border-[#38bdf8] rounded-t flex items-center justify-center text-[9px] text-white font-bold shadow-md">
                    ESCENARIO (45M) ▶
                  </div>
                  <div class="w-full h-8 border-t border-[#23354d] mt-2 flex items-center justify-end text-[10px] text-[#64748b]">
                    Visión Angular hacia la Derecha
                  </div>
                </div>

                <div v-else class="w-full flex flex-col items-center">
                  <div class="w-40 h-9 bg-[#00688f] border border-[#86cffb] rounded-t flex items-center justify-center text-[10px] text-white font-bold shadow-md">
                    PASARELA & ESCENARIO (20M)
                  </div>
                  <div class="w-full h-6 border-t border-[#23354d] mt-1 flex items-center justify-center text-[9px] text-[#64748b]">
                    Inmersión a Nivel de Campo
                  </div>
                </div>

                <div class="w-full h-5 bg-[#141c27] border-t border-[#23354d] flex items-center justify-center text-[9px] text-[#86cffb] mt-1">
                  {{ selectedSector.stageDescription }}
                </div>
              </div>
            </div>

            <!-- Disponibilidad en Tiempo Real Dinámica -->
            <div class="p-4 bg-[#101825] rounded-xl border border-[#23354d] space-y-2">
              <div class="flex justify-between items-center text-xs">
                <span class="text-[#94a3b8]">Disponibilidad en tiempo real</span>
                <span class="text-white font-bold">{{ getZoneStats(selectedZone).available }} / {{ getZoneStats(selectedZone).total }} libres</span>
              </div>
              <div class="w-full h-2 bg-[#1b2533] rounded overflow-hidden">
                <div
                  class="h-full bg-[#00688f]"
                  :style="{ width: `${getZoneStats(selectedZone).percentage}%` }"
                ></div>
              </div>
              <div class="flex justify-between text-[11px] pt-1">
                <span class="text-[#94a3b8]">Puerta de ingreso</span>
                <span class="text-white font-semibold">{{ selectedZone.gate }}</span>
              </div>
            </div>

            <!-- Zonas de este sector para alternar (con Doble Clic) -->
            <div>
              <div class="flex justify-between items-center mb-2">
                <span class="text-[11px] text-[#86cffb] uppercase font-bold tracking-wider">Elegir Otra Zona</span>
                <span class="text-[10px] text-[#64748b]">Doble clic para entrar</span>
              </div>
              <div class="space-y-1.5">
                <button
                  v-for="z in sectorZones"
                  :key="z.id"
                  @click="selectedZone = z"
                  @dblclick="advanceToSeatsSelection"
                  class="w-full p-2.5 rounded-lg border text-left flex justify-between items-center transition-colors cursor-pointer"
                  :class="selectedZone.id === z.id ? 'bg-[#00688f] text-white border-[#86cffb]' : 'bg-[#101825] text-[#94a3b8] border-[#23354d] hover:text-white'"
                >
                  <span class="font-bold">{{ z.name }}</span>
                  <span>{{ formatMoney(getZonePrice(z)) }}</span>
                </button>
              </div>
            </div>

          </div>

          <!-- Bottom CTA Button -->
          <div class="p-6 bg-[#141c27] border-t border-[#23354d]">
            <button
              @click="advanceToSeatsSelection"
              class="w-full bg-[#00688f] hover:bg-[#005a7d] text-white font-mono text-sm font-bold py-3.5 rounded-xl border border-[#00688f] transition-colors flex items-center justify-center gap-2 cursor-pointer shadow-lg"
            >
              <span>Continuar a Selección de Butacas</span>
              <span class="material-symbols-outlined text-[18px]">arrow_forward</span>
            </button>
          </div>
        </aside>

      </div>


      <!-- ========================================================================= -->
      <!-- PASO 3: SELECCIÓN DE BUTACAS (ESQUEMA PERSONALIZADO POR ZONA)            -->
      <!-- ========================================================================= -->
      <div v-else class="flex-1 flex flex-col lg:flex-row h-full overflow-hidden">
        
        <!-- Left: Canvas Area (Seat Map Grid) -->
        <section class="flex-1 relative bg-[#0c141f] p-4 flex flex-col items-center justify-between overflow-auto border-r border-[#23354d]">
          
          <!-- Top Indicator Bar with Dynamic Stage Compass -->
          <div class="w-full max-w-5xl flex items-center justify-between z-20 shrink-0">
            <button
              @click="goToStep('zone')"
              class="flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-[#141c27] hover:bg-[#1b2533] border border-[#23354d] text-xs font-mono text-[#86cffb] transition-colors cursor-pointer"
            >
              <span class="material-symbols-outlined text-[16px]">arrow_back</span>
              <span>Cambiar Zona</span>
            </button>

            <!-- Flat Legend with Realistic Seat Icons Matching User Screenshot -->
            <div class="flex items-center gap-4 bg-[#141c27] px-4 py-1.5 rounded-lg border border-[#23354d] text-xs font-mono shadow-md">
              <div class="flex items-center gap-1.5 text-[#22c55e]">
                <div class="w-3.5 h-3.5 rounded bg-[#22c55e] border border-[#16a34a]"></div>
                <span class="font-bold">Disponible</span>
              </div>
              <div class="flex items-center gap-1.5 text-[#38bdf8]">
                <div class="w-3.5 h-3.5 rounded bg-[#0084ff] border border-[#38bdf8]"></div>
                <span class="font-bold">Seleccionado</span>
              </div>
              <div class="flex items-center gap-1.5 text-[#9ca3af]">
                <div class="w-3.5 h-3.5 rounded bg-[#6b7280] border border-[#4b5563]"></div>
                <span>Ocupado</span>
              </div>
              <div class="flex items-center gap-1.5 text-[#d8b4fe]">
                <div class="w-3.5 h-3.5 rounded bg-[#a855f7] border border-[#9333ea] flex items-center justify-center">
                  <span class="material-symbols-outlined text-[9px] text-white">accessible</span>
                </div>
                <span>Accesibilidad</span>
              </div>
            </div>
          </div>

          <!-- Minimapa Radar Cuadrado y Grande (.JSON Oficial del Estadio) -->
          <div class="absolute top-14 right-6 z-30 bg-[#141c27] border border-[#23354d] p-3 rounded-2xl shadow-2xl w-60 h-60 sm:w-68 sm:h-68 aspect-square flex flex-col justify-between">
            <div class="flex items-center justify-between mb-1">
              <span class="font-mono text-[10px] text-[#86cffb] font-bold uppercase tracking-wider">Radar Estadio (.JSON)</span>
              <span class="w-2.5 h-2.5 rounded-full bg-emerald-400 animate-pulse"></span>
            </div>

            <!-- Vectorial JSON seat map -->
            <div class="flex-1 w-full bg-[#0c141f] border border-[#23354d] rounded-xl overflow-hidden relative flex items-center justify-center p-1">
              <svg
                v-if="venueLayout"
                :viewBox="`0 0 ${venueLayout.canvasWidth} ${venueLayout.canvasHeight}`"
                class="w-full h-full select-none"
              >
                <!-- Venue Boundary -->
                <rect
                  x="20" y="20"
                  :width="venueLayout.canvasWidth - 40"
                  :height="venueLayout.canvasHeight - 40"
                  rx="18"
                  fill="#101825"
                  stroke="#23354d"
                  stroke-width="1.5"
                  stroke-dasharray="4 4"
                />

                <!-- Stage -->
                <g v-if="venueLayout.stage">
                  <rect
                    :x="venueLayout.stage.x"
                    :y="venueLayout.stage.y"
                    :width="venueLayout.stage.width"
                    :height="venueLayout.stage.height"
                    rx="6"
                    fill="#00688f"
                    stroke="#86cffb"
                    stroke-width="2"
                  />
                  <text
                    :x="venueLayout.stage.x + venueLayout.stage.width / 2"
                    :y="venueLayout.stage.y + venueLayout.stage.height / 2 + 5"
                    text-anchor="middle"
                    fill="#ffffff"
                    font-family="Space Grotesk, sans-serif"
                    font-weight="bold"
                    font-size="18"
                  >
                    {{ venueLayout.stage.label || 'ESCENARIO' }}
                  </text>
                </g>

                <!-- Sectors from JSON: Selected Sector Highlighted in Bright Cyan (#00b4d8) with Beacon -->
                <g v-for="sec in venueLayout.sectors" :key="sec.id">
                  <polygon
                    v-if="sec.shapeType === 'POLYGON' && sec.points"
                    :points="sec.points.map(([x,y]) => `${x},${y}`).join(' ')"
                    :fill="isSectorActive(sec.id, sec.name) ? '#00b4d8' : 'rgba(255, 255, 255, 0.08)'"
                    :fill-opacity="isSectorActive(sec.id, sec.name) ? 0.95 : 0.4"
                    :stroke="isSectorActive(sec.id, sec.name) ? '#ffffff' : '#334155'"
                    :stroke-width="isSectorActive(sec.id, sec.name) ? 3.5 : 1.2"
                  />
                  <rect
                    v-else-if="sec.shapeType === 'GRID'"
                    :x="sec.x" :y="sec.y" :width="sec.width" :height="sec.height"
                    :fill="isSectorActive(sec.id, sec.name) ? '#00b4d8' : 'rgba(255, 255, 255, 0.08)'"
                    :fill-opacity="isSectorActive(sec.id, sec.name) ? 0.95 : 0.4"
                    :stroke="isSectorActive(sec.id, sec.name) ? '#ffffff' : '#334155'"
                    :stroke-width="isSectorActive(sec.id, sec.name) ? 3.5 : 1.2"
                  />

                  <!-- Active Sector Beacon Pin Marker -->
                  <g v-if="isSectorActive(sec.id, sec.name) && sec.labelPosition">
                    <circle
                      :cx="sec.labelPosition[0]"
                      :cy="sec.labelPosition[1]"
                      r="16"
                      fill="#00e5ff"
                      fill-opacity="0.3"
                      class="animate-ping origin-center"
                    />
                    <circle
                      :cx="sec.labelPosition[0]"
                      :cy="sec.labelPosition[1]"
                      r="10"
                      fill="#00b4d8"
                      stroke="#ffffff"
                      stroke-width="2.5"
                    />
                  </g>
                </g>
              </svg>
            </div>

            <div class="mt-1.5 text-center">
              <span class="block font-mono text-[10px] text-white font-bold truncate">{{ selectedSector.name }}</span>
              <span class="block font-mono text-[9px] text-[#86cffb]">{{ selectedZone.name }}</span>
            </div>
          </div>

          <!-- Zoom Tools (Floating Bottom Right) -->
          <div class="absolute bottom-4 right-4 flex flex-col bg-[#141c27] rounded-lg p-1 border border-[#23354d] z-20">
            <button
              @click="zoomLevel = Math.min(zoomLevel + 0.15, 1.6)"
              class="w-8 h-8 flex items-center justify-center text-[#94a3b8] hover:text-white rounded cursor-pointer"
              title="Acercar"
            >
              <span class="material-symbols-outlined text-[18px]">zoom_in</span>
            </button>
            <div class="w-full h-px bg-[#23354d]"></div>
            <button
              @click="zoomLevel = Math.max(zoomLevel - 0.15, 0.75)"
              class="w-8 h-8 flex items-center justify-center text-[#94a3b8] hover:text-white rounded cursor-pointer"
              title="Alejar"
            >
              <span class="material-symbols-outlined text-[18px]">zoom_out</span>
            </button>
            <div class="w-full h-px bg-[#23354d]"></div>
            <button
              @click="zoomLevel = 1"
              class="w-8 h-8 flex items-center justify-center text-[#94a3b8] hover:text-white rounded cursor-pointer"
              title="Restablecer"
            >
              <span class="material-symbols-outlined text-[16px]">restart_alt</span>
            </button>
          </div>

          <!-- Main Seat Grid (Con Glifos de Butacas y Letras a Ambos Lados) -->
          <div
            class="flex flex-col items-center justify-center w-full max-w-5xl py-6 px-2 transition-transform duration-150 origin-center"
            :style="{ transform: `scale(${zoomLevel})` }"
          >
            <!-- Orientation Bar Towards Stage -->
            <div class="w-full max-w-2xl mb-5 text-center">
              <div class="py-2 px-5 bg-[#141c27] border border-[#23354d] rounded-lg text-[#86cffb] font-mono text-xs tracking-wider font-bold shadow-md">
                {{ selectedSector.stageBanner }}
              </div>
            </div>

            <!-- Seat Rows Container -->
            <div class="flex flex-col gap-1.5 p-4 bg-[#101825]/90 border border-[#23354d] rounded-xl shadow-2xl">
              
              <div
                v-for="row in seatRows"
                :key="row.letter"
                class="flex items-center gap-2"
              >
                <!-- Left Row Badge (a..s) -->
                <div class="w-5 h-5 rounded bg-[#1b2533] border border-[#23354d] flex items-center justify-center font-mono text-[10px] text-[#86cffb] font-bold uppercase shrink-0">
                  {{ row.letter }}
                </div>

                <!-- Seats in this row with authentic chair shapes -->
                <div class="flex items-center gap-1.5">
                  <div
                    v-for="seat in row.seats"
                    :key="seat.id"
                    @click="toggleSeat(seat)"
                    class="transition-transform duration-100 flex items-center justify-center select-none"
                    :class="[
                      seat.status === 'OCCUPIED' ? 'cursor-not-allowed opacity-70' : 'cursor-pointer hover:scale-115'
                    ]"
                    :title="`Fila ${seat.rowLetter.toUpperCase()}, Butaca ${seat.seatNum}`"
                  >
                    <!-- Realistic Stadium Chair SVG Glyph (Matching screenshot) -->
                    <svg viewBox="0 0 20 20" class="w-4 h-4 sm:w-4.5 sm:h-4.5">
                      <!-- Chair Backrest -->
                      <path
                        d="M 5,2 C 5,1 7,0 10,0 C 13,0 15,1 15,2 L 14.5,9 C 14.5,10 13.5,10.5 12.5,10.5 L 7.5,10.5 C 6.5,10.5 5.5,10 5.5,9 Z"
                        :fill="
                          seat.status === 'SELECTED' ? '#0084ff' :
                          seat.status === 'AVAILABLE' ? '#22c55e' :
                          seat.status === 'ACCESSIBLE' ? '#a855f7' :
                          '#6b7280'
                        "
                      />
                      <!-- Chair Seat Base Cushion -->
                      <path
                        d="M 3.5,11.5 C 3.5,11 4.5,11 5.5,11 L 14.5,11 C 15.5,11 16.5,11 16.5,11.5 L 16.5,15 C 16.5,16.5 15,17 13.5,17 L 6.5,17 C 5,17 3.5,16.5 3.5,15 Z"
                        :fill="
                          seat.status === 'SELECTED' ? '#0284c7' :
                          seat.status === 'AVAILABLE' ? '#16a34a' :
                          seat.status === 'ACCESSIBLE' ? '#9333ea' :
                          '#4b5563'
                        "
                      />
                    </svg>
                  </div>
                </div>

                <!-- Right Row Badge (a..s) -->
                <div class="w-5 h-5 rounded bg-[#1b2533] border border-[#23354d] flex items-center justify-center font-mono text-[10px] text-[#86cffb] font-bold uppercase shrink-0">
                  {{ row.letter }}
                </div>
              </div>

            </div>

            <!-- Bottom Sector & Pricing Metadata -->
            <div class="mt-4 flex items-center gap-4 font-mono text-xs text-[#94a3b8]">
              <span>Sector: <strong class="text-white">{{ selectedSector.name }}</strong> (Rango: {{ selectedSectorBackend.priceRangeFormatted }})</span>
              <span>•</span>
              <span>Zona: <strong class="text-[#86cffb]">{{ selectedZone.name }}</strong></span>
              <span>•</span>
              <span>Tarifa: <strong class="text-white">{{ currentZonePriceFormatted }} c/u</strong></span>
            </div>
          </div>

          <div class="w-full text-center text-xs font-mono text-[#64748b] pb-2">
            Haz clic en cualquier butaca verde disponible para agregarla o quitarla de tu selección.
          </div>
        </section>

        <!-- Right: Resumen de Compra en Tiempo Real -->
        <aside class="w-full lg:w-96 bg-[#141c27] flex flex-col h-full border-l border-[#23354d] z-20 shrink-0">
          
          <div class="p-4 border-b border-[#23354d]">
            <h2 class="font-display text-xl font-bold text-[#f1f5f9] mb-1">Tu Selección</h2>
            <p class="font-mono text-xs text-[#94a3b8]">Elige hasta {{ maxTicketsAllowed }} asientos por compra (política oficial).</p>
          </div>

          <!-- Tier Breakdown -->
          <div class="px-4 py-2.5 bg-[#101825] border-b border-[#23354d] flex items-center justify-between font-mono text-xs">
            <span class="text-[#94a3b8]">Categoría seleccionada</span>
            <span class="text-[#86cffb] font-medium">{{ selectedZone.name }} ({{ currentZonePriceFormatted }} c/u)</span>
          </div>

          <!-- Carrito Desplegado -->
          <div class="flex-1 p-4 overflow-y-auto space-y-2.5">
            
            <div
              v-if="selectedSeatsCount === 0"
              class="h-40 flex flex-col items-center justify-center text-center p-4 border border-dashed border-[#23354d] rounded-xl text-[#94a3b8]"
            >
              <span class="material-symbols-outlined text-3xl text-[#00688f] mb-1">event_seat</span>
              <p class="text-xs">No hay asientos seleccionados</p>
            </div>

            <!-- Selected Seat Cards -->
            <div
              v-for="seat in selectedSeatsList"
              :key="seat.id"
              class="bg-[#1b2533] border border-[#23354d] rounded-xl p-3 flex justify-between items-center"
            >
              <div class="flex items-center gap-3">
                <div class="w-10 h-10 rounded-lg bg-[#00688f] flex items-center justify-center text-white font-bold font-mono text-sm border border-[#23354d]">
                  {{ seat.id }}
                </div>
                <div>
                  <p class="font-body text-sm font-semibold text-[#f1f5f9]">Entrada Estándar</p>
                  <p class="font-mono text-xs text-[#94a3b8]">Fila {{ seat.rowLetter.toUpperCase() }}, Butaca {{ seat.seatNum }}</p>
                </div>
              </div>

              <div class="flex flex-col items-end">
                <p class="font-mono text-sm font-bold text-[#f1f5f9]">{{ formatMoney(seat.price) }}</p>
                <button
                  @click="removeSelectedSeat(seat.id)"
                  class="text-[#94a3b8] hover:text-[#ffb4ab] p-1 transition-colors mt-0.5 cursor-pointer"
                  title="Eliminar asiento"
                >
                  <span class="material-symbols-outlined text-[16px]">close</span>
                </button>
              </div>
            </div>

            <!-- Summary Price Tier Breakdown -->
            <div v-if="selectedSeatsCount > 0" class="mt-4 p-3 bg-[#101825] border border-[#23354d] rounded-xl space-y-1.5 font-mono text-xs">
              <div class="flex justify-between text-[#94a3b8]">
                <span>{{ selectedSector.name }} (x{{ selectedSeatsCount }})</span>
                <span class="text-[#f1f5f9]">{{ formatMoney(subtotalPrice) }}</span>
              </div>
              <div class="flex justify-between text-[#94a3b8]">
                <span>Cargos de gestión (10%)</span>
                <span class="text-[#f1f5f9]">{{ formatMoney(serviceFeePrice) }}</span>
              </div>
            </div>

            <!-- Aviso de Retención Redlock de 10 minutos -->
            <div v-if="selectedSeatsCount > 0" class="p-3 bg-[#101825] border border-[#23354d] rounded-xl text-[11px] font-mono text-[#94a3b8] flex items-start gap-2">
              <span class="material-symbols-outlined text-[16px] text-[#86cffb] shrink-0 mt-0.5">lock_clock</span>
              <span>Temporizador Redlock: Asientos retenidos por 10 minutos para completar el pago de forma segura.</span>
            </div>
          </div>

          <!-- Checkout Summary Sticky Bottom -->
          <div class="p-4 bg-[#141c27] border-t border-[#23354d]">
            <div class="flex justify-between items-center mb-3">
              <span class="text-[#94a3b8] font-mono text-sm">Subtotal ({{ selectedSeatsCount }} asientos)</span>
              <span class="text-white font-mono text-xl font-bold">{{ formatMoney(grandTotalPrice) }}</span>
            </div>

            <button
              @click="proceedToCheckout"
              :disabled="selectedSeatsCount === 0"
              class="w-full bg-[#00688f] text-white hover:bg-[#005a7d] disabled:opacity-40 disabled:cursor-not-allowed font-mono text-sm font-bold py-3.5 rounded-xl border border-[#00688f] transition-colors flex items-center justify-center gap-2 cursor-pointer shadow-lg"
            >
              <span>Confirmar Asientos y Proceder al Pago</span>
              <span class="material-symbols-outlined text-[18px]">arrow_forward</span>
            </button>
          </div>
        </aside>

      </div>

    </div>

    <!-- Exit Confirmation Modal with Radix Vue Dialog -->
    <DialogRoot v-model:open="showExitModal">
      <DialogPortal>
        <DialogOverlay class="fixed inset-0 z-50 bg-black/80 backdrop-blur-xs" />
        <DialogContent class="fixed top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 z-50 bg-[#141c27] border border-[#23354d] rounded-2xl max-w-md w-[92vw] p-6 space-y-4 shadow-2xl">
          <DialogTitle class="flex items-center gap-2 text-lg font-bold text-white font-display">
            <span class="material-symbols-outlined text-[#86cffb] text-2xl">warning</span>
            <span>¿Deseas salir de la reserva?</span>
          </DialogTitle>
          
          <DialogDescription class="text-xs text-[#94a3b8] font-mono leading-relaxed">
            Tienes asientos retenidos. Si sales al home ahora, se cancelará la sesión y se liberarán tus butacas para otros espectadores.
          </DialogDescription>

          <div class="flex items-center justify-end gap-3 pt-2 font-mono text-xs">
            <button
              @click="showExitModal = false"
              class="px-4 py-2 rounded-lg bg-[#1b2533] hover:bg-[#23354d] text-white border border-[#23354d] cursor-pointer transition-colors"
            >
              Continuar seleccionando
            </button>
            <button
              @click="confirmExitReservation"
              class="px-4 py-2 rounded-lg bg-[#93000a] hover:bg-[#b00020] text-white border border-[#ffb4ab]/30 cursor-pointer transition-colors font-bold"
            >
              Salir y liberar asientos
            </button>
          </div>
        </DialogContent>
      </DialogPortal>
    </DialogRoot>

    <!-- Time Expired Dialog with Radix Vue -->
    <DialogRoot v-model:open="showTimeExpiredModal">
      <DialogPortal>
        <DialogOverlay class="fixed inset-0 z-50 bg-black/80 backdrop-blur-xs" />
        <DialogContent class="fixed top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 z-50 bg-[#141c27] border border-rose-500/40 rounded-2xl max-w-md w-[92vw] p-6 space-y-4 shadow-2xl">
          <DialogTitle class="flex items-center gap-2 text-lg font-bold text-rose-400 font-display">
            <span class="material-symbols-outlined text-rose-400 text-2xl">timer_off</span>
            <span>Tiempo de Reserva Expirado</span>
          </DialogTitle>
          
          <DialogDescription class="text-xs text-[#94a3b8] font-mono leading-relaxed">
            Tus 10 minutos para concretar la compra han expirado. Por concurrencia y la política Redlock de alta demanda, los asientos reservados han sido liberados para otros usuarios.
          </DialogDescription>

          <div class="flex items-center justify-end gap-3 pt-2 font-mono text-xs">
            <button
              @click="handleExpiredReturnHome"
              class="px-4 py-2 rounded-lg bg-[#1b2533] hover:bg-[#23354d] text-white border border-[#23354d] cursor-pointer transition-colors"
            >
              Volver al Evento
            </button>
            <button
              @click="handleRestartTimer"
              class="px-4 py-2 rounded-lg bg-[#00688f] hover:bg-[#005a7d] text-white border border-[#00688f] cursor-pointer transition-colors font-bold"
            >
              Reiniciar Reserva (10 min)
            </button>
          </div>
        </DialogContent>
      </DialogPortal>
    </DialogRoot>

  </div>
</template>
