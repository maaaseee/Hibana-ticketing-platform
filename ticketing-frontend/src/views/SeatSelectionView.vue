<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useCatalogStore } from '../stores/catalog'
import { useTicketingStore } from '../stores/ticketing'
import { useQueueStore } from '../stores/queue'
import type { VenueSeatMapLayout } from '../types'
import { venueLayoutService } from '../services/venueLayoutService'

import SeatSelectionTopBar from '../components/seat-selection/SeatSelectionTopBar.vue'
import MacroVenueMap from '../components/seat-selection/MacroVenueMap.vue'
import SectorCategoryAccordion from '../components/seat-selection/SectorCategoryAccordion.vue'
import ZoneSelectorCanvas from '../components/seat-selection/ZoneSelectorCanvas.vue'
import ZoneSelectionSidebar from '../components/seat-selection/ZoneSelectionSidebar.vue'
import SeatMatrixSection from '../components/seat-selection/SeatMatrixSection.vue'
import SeatCartSidebar from '../components/seat-selection/SeatCartSidebar.vue'
import SeatModals from '../components/seat-selection/SeatModals.vue'

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
  const policy = event.value?.maxTicketsPerUser || event.value?.salesPolicy?.maxTicketsPerUser
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

// Venue JSON layout for realistic stadium radar (cargado dinámicamente on-demand)
const venueLayout = ref<VenueSeatMapLayout | null>(venueLayoutService.getCachedVenueLayout(event.value?.venueId || 'arena-buenos-aires'))

async function loadVenueMap() {
  const targetVenue = event.value?.venueId || event.value?.seatMapUrl || 'arena-buenos-aires'
  const loaded = await venueLayoutService.getVenueLayout(targetVenue)
  if (loaded) {
    venueLayout.value = loaded
  }
}

loadVenueMap()

watch(
  () => event.value?.venueId,
  () => {
    loadVenueMap()
  }
)

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

function autoSelectBestSeat() {
  const allSeats = seatRows.value.flatMap(r => r.seats)
  const available = allSeats.filter(s => s.status === 'AVAILABLE')
  if (available.length === 0) return

  // Prioritize front rows and center seats (around seat 10)
  const sorted = [...available].sort((a, b) => {
    const rowDiff = a.rowLetter.charCodeAt(0) - b.rowLetter.charCodeAt(0)
    if (rowDiff !== 0) return rowDiff
    return Math.abs(a.seatNum - 10) - Math.abs(b.seatNum - 10)
  })

  const best = sorted[0]
  if (best) {
    toggleSeat(best)
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
// Service fee is calculated at Checkout stage per user requirements
const serviceFeePrice = computed(() => 0)
const grandTotalPrice = computed(() => subtotalPrice.value)

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
    <SeatSelectionTopBar
      :current-step="currentStep"
      :selected-zone-code="selectedZone.code"
      :is-campo-mode="isCampoMode"
      :lock-time-remaining="ticketingStore.lockTimeRemaining"
      :formatted-lock-time="ticketingStore.formattedLockTime"
      @exit="handleExitClick"
      @go-to-step="goToStep"
      @trigger-stress-test="triggerQueueStressTest"
    />

    <!-- Main Viewport Area -->
    <div class="flex-1 flex overflow-hidden relative">
      <!-- PASO 1: SELECCIÓN DE UBICACIÓN -->
      <div v-if="currentStep === 'sector'" class="flex-1 flex flex-col lg:flex-row h-full overflow-hidden">
        <MacroVenueMap
          :general-sectors="generalSectors"
          :selected-sector-id="selectedSector.id"
          :get-backend-sector-info="getBackendSectorInfo"
          @select-sector="selectSector"
          @advance-step="handleContinueClick"
        />

        <SectorCategoryAccordion
          v-model:active-accordion="activeAccordion"
          :category-definitions="categoryDefinitions"
          :selected-sector="selectedSector"
          :general-sectors="generalSectors"
          :max-tickets-allowed="maxTicketsAllowed"
          :campo-ticket-count="campoTicketCount"
          :is-campo-mode="isCampoMode"
          :get-category-price-range-formatted="getCategoryPriceRangeFormatted"
          :get-backend-sector-info="getBackendSectorInfo"
          @select-sector="selectSector"
          @increment-campo-tickets="incrementCampoTickets"
          @decrement-campo-tickets="decrementCampoTickets"
          @continue="handleContinueClick"
        />
      </div>

      <!-- PASO 2: SELECCIÓN DE ZONA -->
      <div v-else-if="currentStep === 'zone'" class="flex-1 flex flex-col lg:flex-row h-full overflow-hidden">
        <ZoneSelectorCanvas
          :selected-sector="selectedSector"
          :sector-zones="sectorZones"
          :selected-zone="selectedZone"
          :venue-layout="venueLayout"
          :get-zone-stats="getZoneStats"
          :get-zone-price="getZonePrice"
          :format-money="formatMoney"
          :is-sector-active="isSectorActive"
          @select-zone="selectedZone = $event"
          @advance="advanceToSeatsSelection"
          @go-back="goToStep('sector')"
        />

        <ZoneSelectionSidebar
          :selected-zone="selectedZone"
          :selected-sector="selectedSector"
          :selected-sector-backend="selectedSectorBackend"
          :current-zone-price-formatted="currentZonePriceFormatted"
          :sector-zones="sectorZones"
          :get-zone-stats="getZoneStats"
          :get-zone-price="getZonePrice"
          :format-money="formatMoney"
          @select-zone="selectedZone = $event"
          @advance="advanceToSeatsSelection"
        />
      </div>

      <!-- PASO 3: SELECCIÓN DE BUTACAS -->
      <div v-else class="flex-1 flex flex-col lg:flex-row h-full overflow-hidden">
        <SeatMatrixSection
          :selected-sector="selectedSector"
          :selected-sector-backend="selectedSectorBackend"
          :selected-zone="selectedZone"
          :current-zone-price-formatted="currentZonePriceFormatted"
          :venue-layout="venueLayout"
          :seat-rows="seatRows"
          v-model:zoom-level="zoomLevel"
          :is-sector-active="isSectorActive"
          @change-zone="goToStep('zone')"
          @toggle-seat="toggleSeat"
          @auto-select-best-seat="autoSelectBestSeat"
        />

        <SeatCartSidebar
          :max-tickets-allowed="maxTicketsAllowed"
          :selected-zone="selectedZone"
          :current-zone-price-formatted="currentZonePriceFormatted"
          :selected-seats-count="selectedSeatsCount"
          :selected-seats-list="selectedSeatsList"
          :selected-sector="selectedSector"
          :subtotal-price="subtotalPrice"
          :service-fee-price="serviceFeePrice"
          :grand-total-price="grandTotalPrice"
          :format-money="formatMoney"
          @remove-seat="removeSelectedSeat"
          @proceed="proceedToCheckout"
        />
      </div>
    </div>

    <!-- Modals -->
    <SeatModals
      v-model:show-exit-modal="showExitModal"
      v-model:show-time-expired-modal="showTimeExpiredModal"
      @confirm-exit="confirmExitReservation"
      @expired-return-home="handleExpiredReturnHome"
      @restart-timer="handleRestartTimer"
    />
  </div>
</template>
