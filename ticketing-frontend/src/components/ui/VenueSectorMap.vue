<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import type { VenueSeatMapLayout, SectorGeometry, SectorPricing } from '../../types'
import { venueLayoutService } from '../../services/venueLayoutService'

const props = withDefaults(
  defineProps<{
    venueId?: string
    sectorsPricing?: SectorPricing[]
    interactive?: boolean
    compact?: boolean
  }>(),
  {
    venueId: 'arena-buenos-aires',
    sectorsPricing: () => [],
    interactive: true,
    compact: false
  }
)

const emit = defineEmits<{
  (e: 'select-sector', sector: SectorGeometry): void
}>()

const layout = ref<VenueSeatMapLayout | null>(null)
const loading = ref(true)
const error = ref<string | null>(null)
const hoveredSector = ref<SectorGeometry | null>(null)
const selectedSector = ref<SectorGeometry | null>(null)

// Google Maps Style Navigation & Exploration State
const zoom = ref(1)
const panX = ref(0)
const panY = ref(0)
const isDragging = ref(false)
const dragStart = ref({ x: 0, y: 0 })
const initialPan = ref({ x: 0, y: 0 })
const hasDragged = ref(false)

const activeSector = computed(() => selectedSector.value || hoveredSector.value)

function zoomIn() {
  zoom.value = Math.min(parseFloat((zoom.value + 0.25).toFixed(2)), 3.5)
}

function zoomOut() {
  zoom.value = Math.max(parseFloat((zoom.value - 0.25).toFixed(2)), 0.7)
}

function resetView() {
  zoom.value = 1
  panX.value = 0
  panY.value = 0
  selectedSector.value = null
}

function handleWheel(e: WheelEvent) {
  if (!props.interactive || props.compact) return
  const delta = e.deltaY < 0 ? 0.15 : -0.15
  const newZoom = Math.min(Math.max(zoom.value + delta, 0.7), 3.5)
  zoom.value = parseFloat(newZoom.toFixed(2))
}

function handleMouseDown(e: MouseEvent) {
  if (!props.interactive || props.compact) return
  if ((e.target as HTMLElement)?.closest('.map-control-btn') || (e.target as HTMLElement)?.closest('.sector-inspector-card')) return
  isDragging.value = true
  hasDragged.value = false
  dragStart.value = { x: e.clientX, y: e.clientY }
  initialPan.value = { x: panX.value, y: panY.value }
}

function handleMouseMove(e: MouseEvent) {
  if (!isDragging.value) return
  const dx = e.clientX - dragStart.value.x
  const dy = e.clientY - dragStart.value.y
  if (Math.abs(dx) > 4 || Math.abs(dy) > 4) {
    hasDragged.value = true
  }
  panX.value = initialPan.value.x + dx
  panY.value = initialPan.value.y + dy
}

function handleMouseUp() {
  isDragging.value = false
}

// Soporte gestual táctil para dispositivos móviles (Pan & Pinch to zoom)
let initialTouchDist = 0
function handleTouchStart(e: TouchEvent) {
  if (!props.interactive || props.compact) return
  if (e.touches.length === 1 && e.touches[0]) {
    isDragging.value = true
    hasDragged.value = false
    dragStart.value = { x: e.touches[0].clientX, y: e.touches[0].clientY }
    initialPan.value = { x: panX.value, y: panY.value }
  } else if (e.touches.length === 2 && e.touches[0] && e.touches[1]) {
    const t1 = e.touches[0]
    const t2 = e.touches[1]
    initialTouchDist = Math.hypot(t1.clientX - t2.clientX, t1.clientY - t2.clientY)
  }
}

function handleTouchMove(e: TouchEvent) {
  if (e.touches.length === 1 && isDragging.value && e.touches[0]) {
    const dx = e.touches[0].clientX - dragStart.value.x
    const dy = e.touches[0].clientY - dragStart.value.y
    if (Math.abs(dx) > 4 || Math.abs(dy) > 4) {
      hasDragged.value = true
    }
    panX.value = initialPan.value.x + dx
    panY.value = initialPan.value.y + dy
  } else if (e.touches.length === 2 && initialTouchDist > 0 && e.touches[0] && e.touches[1]) {
    const t1 = e.touches[0]
    const t2 = e.touches[1]
    const currentDist = Math.hypot(t1.clientX - t2.clientX, t1.clientY - t2.clientY)
    const factor = currentDist / initialTouchDist
    zoom.value = Math.min(Math.max(parseFloat((zoom.value * factor).toFixed(2)), 0.7), 3.5)
    initialTouchDist = currentDist
  }
}

function handleTouchEnd() {
  isDragging.value = false
  initialTouchDist = 0
}

async function loadVenueMap() {
  loading.value = true
  error.value = null

  try {
    const data = await venueLayoutService.getVenueLayout(props.venueId)
    if (data) {
      layout.value = data
    } else {
      throw new Error(`No se pudo cargar el mapa para ${props.venueId}`)
    }
  } catch (err: unknown) {
    if (import.meta.env?.MODE !== 'test') {
      console.warn('Error loading seat map json:', err)
    }
    error.value = err instanceof Error ? err.message : 'Error al cargar mapa'
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadVenueMap()
})

watch(
  () => props.venueId,
  () => {
    loadVenueMap()
  }
)

// Generar path SVG para sectores tipo ARC (Anillo o Corona Circular)
function getArcPath(
  cx: number,
  cy: number,
  rInner: number,
  rOuter: number,
  startDeg: number,
  endDeg: number
): string {
  let end = endDeg
  if (end < startDeg) end += 360
  const toRad = Math.PI / 180
  const a1 = startDeg * toRad
  const a2 = end * toRad

  const x1Outer = cx + rOuter * Math.cos(a1)
  const y1Outer = cy + rOuter * Math.sin(a1)
  const x2Outer = cx + rOuter * Math.cos(a2)
  const y2Outer = cy + rOuter * Math.sin(a2)

  const x1Inner = cx + rInner * Math.cos(a2)
  const y1Inner = cy + rInner * Math.sin(a2)
  const x2Inner = cx + rInner * Math.cos(a1)
  const y2Inner = cy + rInner * Math.sin(a1)

  const delta = Math.abs(end - startDeg)
  const largeArcFlag = delta > 180 ? 1 : 0

  return [
    `M ${x1Outer.toFixed(2)} ${y1Outer.toFixed(2)}`,
    `A ${rOuter} ${rOuter} 0 ${largeArcFlag} 1 ${x2Outer.toFixed(2)} ${y2Outer.toFixed(2)}`,
    `L ${x1Inner.toFixed(2)} ${y1Inner.toFixed(2)}`,
    `A ${rInner} ${rInner} 0 ${largeArcFlag} 0 ${x2Inner.toFixed(2)} ${y2Inner.toFixed(2)}`,
    'Z'
  ].join(' ')
}

// Calcular centro de label para sectores ARC
function getArcLabelCenter(
  cx: number,
  cy: number,
  rInner: number,
  rOuter: number,
  startDeg: number,
  endDeg: number
): [number, number] {
  let end = endDeg
  if (end < startDeg) end += 360
  const midRad = ((startDeg + end) / 2) * (Math.PI / 180)
  const midR = (rInner + rOuter) / 2
  return [cx + midR * Math.cos(midRad), cy + midR * Math.sin(midRad)]
}

// Generar path SVG para polígonos
function getPolygonPath(points: [number, number][]): string {
  if (!points || points.length === 0) return ''
  return `M ${points.map(([x, y]) => `${x},${y}`).join(' L ')} Z`
}

// Dividir nombres largos en dos líneas para evitar desbordes en sectores de gradas
function splitSectorLabel(name: string): { line1: string; line2?: string } {
  const parts = name.split(' ')
  if (parts.length >= 3 && name.length > 15) {
    return {
      line1: parts.slice(0, 2).join(' '),
      line2: parts.slice(2).join(' ')
    }
  }
  return { line1: name }
}

// Obtener precio base correspondiente al sector según la lista de precios de la función
function getSectorPrice(sec: SectorGeometry): number | undefined {
  if (!props.sectorsPricing || props.sectorsPricing.length === 0) return undefined
  // 1. Coincidencia por ID (exacta, sufijo/prefijo, o equivalencia sivori/centenario)
  const byId = props.sectorsPricing.find(p =>
    p.id === sec.id ||
    p.id.endsWith(sec.id) ||
    sec.id.endsWith(p.id) ||
    (sec.id.includes('sivori') && p.id.includes('centenario')) ||
    (sec.id.includes('centenario') && p.id.includes('sivori'))
  )
  if (byId) return byId.basePrice !== undefined && byId.basePrice !== null ? byId.basePrice : byId.price

  // 2. Coincidencia por Nombre
  const normSec = sec.name.trim().toLowerCase()
  const byName = props.sectorsPricing.find(p => p.name.trim().toLowerCase() === normSec)
  if (byName) return byName.basePrice !== undefined && byName.basePrice !== null ? byName.basePrice : byName.price

  // 3. Coincidencia por Tipo
  const byType = props.sectorsPricing.find(p => p.type === sec.type)
  if (byType) return byType.basePrice !== undefined && byType.basePrice !== null ? byType.basePrice : byType.price

  return undefined
}

function formatDisplayPrice(price?: number): string {
  if (price === undefined || price === null) return ''
  return price.toLocaleString('es-AR', {
    minimumFractionDigits: Number.isInteger(price) ? 0 : 2,
    maximumFractionDigits: 2
  })
}

function handleSectorClick(sec: SectorGeometry) {
  if (!props.interactive) return
  if (hasDragged.value) return
  selectedSector.value = sec
  emit('select-sector', sec)
}
</script>

<template>
  <div
    class="relative w-full h-full flex flex-col items-center justify-center select-none overflow-hidden"
    :class="[
      interactive && !compact ? (isDragging ? 'cursor-grabbing' : 'cursor-grab') : ''
    ]"
    @wheel.prevent="handleWheel"
    @mousedown="handleMouseDown"
    @mousemove="handleMouseMove"
    @mouseup="handleMouseUp"
    @mouseleave="handleMouseUp"
    @touchstart="handleTouchStart"
    @touchmove="handleTouchMove"
    @touchend="handleTouchEnd"
  >
    <!-- Estado de carga -->
    <div v-if="loading" class="flex flex-col items-center justify-center gap-2 p-8 text-primary">
      <span class="material-symbols-outlined text-3xl animate-spin">progress_activity</span>
      <span class="font-mono text-xs text-muted-light">Cargando sectores del recinto...</span>
    </div>

    <!-- Estado de error -->
    <div v-else-if="error || !layout" class="flex flex-col items-center justify-center gap-2 p-6 text-muted-light">
      <span class="material-symbols-outlined text-2xl text-secondary">map</span>
      <span class="font-mono text-xs">Vista previa esquemática no disponible</span>
    </div>

    <!-- Renderizado vectorial reactivo del mapa -->
    <div v-else class="relative w-full h-full flex items-center justify-center overflow-hidden">
      <svg
        :viewBox="`0 0 ${layout.canvasWidth} ${layout.canvasHeight}`"
        class="w-full h-full max-h-full transition-all duration-75 drop-shadow-md select-none"
        xmlns="http://www.w3.org/2000/svg"
      >
        <!-- Filtros y Efectos Luminiscentes -->
        <defs>
          <filter id="glow-sector" x="-20%" y="-20%" width="140%" height="140%">
            <feGaussianBlur stdDeviation="6" result="blur" />
            <feComposite in="SourceGraphic" in2="blur" operator="over" />
          </filter>
        </defs>

        <!-- Matriz de Transformación Pan & Zoom estilo Google Maps -->
        <g
          :transform="`translate(${panX}, ${panY}) scale(${zoom})`"
          :style="{
            transformOrigin: `${layout.canvasWidth / 2}px ${layout.canvasHeight / 2}px`,
            transition: isDragging ? 'none' : 'transform 0.15s ease-out'
          }"
        >
          <!-- Fondo del Recinto Esquemático -->
          <rect
            x="10"
            y="10"
            :width="layout.canvasWidth - 20"
            :height="layout.canvasHeight - 20"
            rx="24"
            fill="rgba(12, 20, 31, 0.65)"
            stroke="rgba(35, 53, 77, 0.7)"
            stroke-width="1.5"
            stroke-dasharray="6 6"
          />

          <!-- Escenario (Stage) -->
          <g v-if="layout.stage" class="stage-group">
            <!-- Rectángulo / Tarima del Escenario -->
            <rect
              :x="layout.stage.x"
              :y="layout.stage.y"
              :width="layout.stage.width"
              :height="layout.stage.height"
              rx="8"
              fill="#00688f"
              stroke="#86cffb"
              stroke-width="2.5"
              class="transition-all"
            />

            <!-- Label del Escenario -->
            <text
              :x="layout.stage.x + layout.stage.width / 2"
              :y="layout.stage.y + layout.stage.height / 2 + 5"
              text-anchor="middle"
              fill="#ffffff"
              font-family="Space Grotesk, sans-serif"
              font-weight="bold"
              :font-size="compact ? '12' : '15'"
              letter-spacing="0.08em"
              class="pointer-events-none select-none"
            >
              {{ layout.stage.label || 'ESCENARIO PRINCIPAL' }}
            </text>
          </g>

          <!-- Sectores Vectoriales del Recinto -->
          <g class="sectors-layer">
            <g
              v-for="sec in layout.sectors"
              :key="sec.id"
              @mouseenter="hoveredSector = sec"
              @mouseleave="hoveredSector = null"
              @click="handleSectorClick(sec)"
              :class="[
                'sector-group transition-all duration-200',
                (sec.selectable === false || sec.vendible === false)
                  ? 'cursor-not-allowed opacity-90'
                  : (interactive ? 'cursor-pointer' : 'cursor-default'),
                (hoveredSector?.id === sec.id || selectedSector?.id === sec.id) ? 'opacity-100 filter-drop-shadow' : 'opacity-85 hover:opacity-100'
              ]"
            >
              <!-- 1. Sector GRID / Rectangular (Plateas Teatro o Salas) -->
              <template v-if="sec.shapeType === 'GRID'">
                <rect
                  :x="sec.x"
                  :y="sec.y"
                  :width="sec.width"
                  :height="sec.height"
                  rx="6"
                  :fill="sec.color"
                  :stroke="(hoveredSector?.id === sec.id || selectedSector?.id === sec.id) ? '#ffffff' : sec.strokeColor"
                  :stroke-width="(hoveredSector?.id === sec.id || selectedSector?.id === sec.id) ? 3 : 1.5"
                  class="transition-all"
                />
                <text
                  :x="sec.labelPosition ? sec.labelPosition[0] : (sec.x + sec.width / 2)"
                  :y="(sec.labelPosition ? sec.labelPosition[1] : (sec.y + sec.height / 2)) + 4"
                  text-anchor="middle"
                  fill="#f1f5f9"
                  font-family="Space Grotesk, sans-serif"
                  font-weight="700"
                  :font-size="compact ? '9' : (sec.shortLabel ? '12' : '11')"
                  class="pointer-events-none select-none drop-shadow"
                >
                  <template v-if="sec.shortLabel">
                    <tspan :x="sec.labelPosition ? sec.labelPosition[0] : (sec.x + sec.width / 2)" :dy="sec.subLabel ? '-3' : '3'" font-size="12" font-weight="800">{{ sec.shortLabel }}</tspan>
                    <tspan v-if="sec.subLabel" :x="sec.labelPosition ? sec.labelPosition[0] : (sec.x + sec.width / 2)" dy="12" font-size="8" fill="#e2e8f0" font-weight="600">{{ sec.subLabel }}</tspan>
                  </template>
                  <template v-else-if="splitSectorLabel(sec.name).line2">
                    <tspan :x="sec.labelPosition ? sec.labelPosition[0] : (sec.x + sec.width / 2)" dy="-5">{{ splitSectorLabel(sec.name).line1 }}</tspan>
                    <tspan :x="sec.labelPosition ? sec.labelPosition[0] : (sec.x + sec.width / 2)" dy="13">{{ splitSectorLabel(sec.name).line2 }}</tspan>
                  </template>
                  <template v-else>
                    {{ sec.name }}
                  </template>
                </text>
              </template>

              <!-- 2. Sector POLYGON (Campo VIP, Campo General, Gradas, Codos) -->
              <template v-else-if="sec.shapeType === 'POLYGON'">
                <path
                  :d="getPolygonPath(sec.points)"
                  :fill="sec.color"
                  :stroke="(hoveredSector?.id === sec.id || selectedSector?.id === sec.id) ? '#ffffff' : sec.strokeColor"
                  :stroke-width="(hoveredSector?.id === sec.id || selectedSector?.id === sec.id) ? 3 : 1.8"
                  class="transition-all"
                />
                <text
                  :x="sec.labelPosition ? sec.labelPosition[0] : 0"
                  :y="sec.labelPosition ? sec.labelPosition[1] : 0"
                  text-anchor="middle"
                  fill="#f1f5f9"
                  font-family="Space Grotesk, sans-serif"
                  font-weight="700"
                  :font-size="compact ? '10' : (sec.shortLabel ? '12' : '12')"
                  class="pointer-events-none select-none drop-shadow"
                >
                  <template v-if="sec.shortLabel">
                    <tspan :x="sec.labelPosition ? sec.labelPosition[0] : 0" :dy="sec.subLabel ? '-3' : '3'" font-size="12" font-weight="800">{{ sec.shortLabel }}</tspan>
                    <tspan v-if="sec.subLabel" :x="sec.labelPosition ? sec.labelPosition[0] : 0" dy="12" font-size="8" fill="#e2e8f0" font-weight="600">{{ sec.subLabel }}</tspan>
                  </template>
                  <template v-else-if="splitSectorLabel(sec.name).line2">
                    <tspan :x="sec.labelPosition ? sec.labelPosition[0] : 0" dy="-5">{{ splitSectorLabel(sec.name).line1 }}</tspan>
                    <tspan :x="sec.labelPosition ? sec.labelPosition[0] : 0" dy="14">{{ splitSectorLabel(sec.name).line2 }}</tspan>
                  </template>
                  <template v-else>
                    {{ sec.name }}
                  </template>
                </text>
              </template>

              <!-- 3. Sector PATH (SVG directo de trazado arquitectónico) -->
              <template v-else-if="sec.shapeType === 'PATH'">
                <path
                  :d="sec.pathData"
                  :fill="sec.color"
                  :stroke="(hoveredSector?.id === sec.id || selectedSector?.id === sec.id) ? '#ffffff' : sec.strokeColor"
                  :stroke-width="(hoveredSector?.id === sec.id || selectedSector?.id === sec.id) ? 3 : 1.8"
                  class="transition-all"
                />
                <text
                  :x="sec.labelPosition ? sec.labelPosition[0] : 0"
                  :y="sec.labelPosition ? sec.labelPosition[1] : 0"
                  text-anchor="middle"
                  fill="#f1f5f9"
                  font-family="Space Grotesk, sans-serif"
                  font-weight="600"
                  :font-size="compact ? '10' : '12'"
                  class="pointer-events-none select-none drop-shadow"
                >
                  <template v-if="splitSectorLabel(sec.name).line2">
                    <tspan :x="sec.labelPosition ? sec.labelPosition[0] : 0" dy="-5">{{ splitSectorLabel(sec.name).line1 }}</tspan>
                    <tspan :x="sec.labelPosition ? sec.labelPosition[0] : 0" dy="14">{{ splitSectorLabel(sec.name).line2 }}</tspan>
                  </template>
                  <template v-else>
                    {{ sec.name }}
                  </template>
                </text>
              </template>

              <!-- 4. Sector ARC (Plateas Curvadas / Gradas Concéntricas) -->
              <template v-else-if="sec.shapeType === 'ARC'">
                <path
                  :d="getArcPath(sec.centerX, sec.centerY, sec.innerRadius, sec.outerRadius, sec.startAngleDeg, sec.endAngleDeg)"
                  :fill="sec.color"
                  :stroke="(hoveredSector?.id === sec.id || selectedSector?.id === sec.id) ? '#ffffff' : sec.strokeColor"
                  :stroke-width="(hoveredSector?.id === sec.id || selectedSector?.id === sec.id) ? 3 : 1.5"
                  class="transition-all"
                />
                <!-- Label Centrado en el Arco -->
                <text
                  :x="sec.labelPosition ? sec.labelPosition[0] : getArcLabelCenter(sec.centerX, sec.centerY, sec.innerRadius, sec.outerRadius, sec.startAngleDeg, sec.endAngleDeg)[0]"
                  :y="(sec.labelPosition ? sec.labelPosition[1] : getArcLabelCenter(sec.centerX, sec.centerY, sec.innerRadius, sec.outerRadius, sec.startAngleDeg, sec.endAngleDeg)[1]) + 4"
                  text-anchor="middle"
                  fill="#f1f5f9"
                  font-family="Space Grotesk, sans-serif"
                  font-weight="600"
                  :font-size="compact ? '9' : '11'"
                  class="pointer-events-none select-none drop-shadow"
                >
                  <template v-if="splitSectorLabel(sec.name).line2">
                    <tspan
                      :x="sec.labelPosition ? sec.labelPosition[0] : getArcLabelCenter(sec.centerX, sec.centerY, sec.innerRadius, sec.outerRadius, sec.startAngleDeg, sec.endAngleDeg)[0]"
                      dy="-5"
                    >
                      {{ splitSectorLabel(sec.name).line1 }}
                    </tspan>
                    <tspan
                      :x="sec.labelPosition ? sec.labelPosition[0] : getArcLabelCenter(sec.centerX, sec.centerY, sec.innerRadius, sec.outerRadius, sec.startAngleDeg, sec.endAngleDeg)[0]"
                      dy="13"
                    >
                      {{ splitSectorLabel(sec.name).line2 }}
                    </tspan>
                  </template>
                  <template v-else>
                    {{ sec.name }}
                  </template>
                </text>
              </template>
            </g>
          </g>
        </g>
      </svg>

      <!-- Botonera Flotante Estilo Google Maps -->
      <div
        v-if="interactive && !compact"
        class="absolute bottom-4 right-4 flex flex-col items-center bg-[#081524]/90 border border-border-structural rounded-xl shadow-2xl backdrop-blur-md overflow-hidden z-20"
      >
        <button
          @click.stop="zoomIn"
          type="button"
          class="map-control-btn w-10 h-10 flex items-center justify-center text-white hover:bg-white/10 active:scale-95 transition-all cursor-pointer"
          aria-label="Acercar mapa (+)"
          title="Acercar mapa"
        >
          <span class="material-symbols-outlined text-xl">add</span>
        </button>
        <div class="w-full h-px bg-border-structural"></div>
        <button
          @click.stop="zoomOut"
          type="button"
          class="map-control-btn w-10 h-10 flex items-center justify-center text-white hover:bg-white/10 active:scale-95 transition-all cursor-pointer"
          aria-label="Alejar mapa (-)"
          title="Alejar mapa"
        >
          <span class="material-symbols-outlined text-xl">remove</span>
        </button>
        <div class="w-full h-px bg-border-structural"></div>
        <button
          @click.stop="resetView"
          type="button"
          class="map-control-btn w-10 h-10 flex items-center justify-center text-[#38bdf8] hover:bg-white/10 active:scale-95 transition-all cursor-pointer"
          aria-label="Restablecer vista centrada"
          title="Centrar y restablecer vista"
        >
          <span class="material-symbols-outlined text-xl">restart_alt</span>
        </button>
      </div>

      <!-- Ayuda Flotante en la esquina inferior izquierda -->
      <div
        v-if="interactive && !compact"
        class="absolute bottom-4 left-4 hidden sm:flex items-center gap-2 px-3 py-1.5 rounded-lg bg-[#081524]/85 border border-border-structural/70 text-[11px] font-mono text-muted-light backdrop-blur-md pointer-events-none z-10 shadow-lg"
      >
        <span class="material-symbols-outlined text-xs text-primary">pan_tool</span>
        <span>Arrastra para mover</span>
        <span>•</span>
        <span class="material-symbols-outlined text-xs text-primary">zoom_in</span>
        <span>Rueda o botones para zoom</span>
      </div>

      <!-- Ficha de Sector Simplificada: Nombre, Precio mínimo y Capacidad -->
      <transition name="fade">
        <div
          v-if="activeSector && interactive"
          :class="[
            'sector-inspector-card absolute top-4 left-4 max-w-65 sm:max-w-70 w-full bg-[#0c1c2e]/95 border border-[#0099ff]/50 rounded-2xl p-3.5 shadow-2xl backdrop-blur-md z-30 flex flex-col gap-2.5 animate-in fade-in-0 duration-150',
            selectedSector ? 'pointer-events-auto' : 'pointer-events-none'
          ]"
        >
          <!-- Nombre del Sector -->
          <div class="flex items-center justify-between gap-2.5">
            <div class="flex items-center gap-2.5 min-w-0">
              <span
                class="w-3.5 h-3.5 rounded-full border border-white/50 shrink-0 shadow-sm"
                :style="{ backgroundColor: activeSector.color || activeSector.strokeColor }"
              ></span>
              <span class="font-display font-black text-sm sm:text-base text-white tracking-tight truncate">
                {{ activeSector.name }}
              </span>
            </div>

            <button
              v-if="selectedSector"
              @click.stop="selectedSector = null"
              type="button"
              class="w-6 h-6 rounded-full bg-white/10 hover:bg-white/20 text-muted-light hover:text-white flex items-center justify-center transition-colors cursor-pointer shrink-0 pointer-events-auto"
              title="Cerrar detalles"
            >
              <span class="material-symbols-outlined text-sm">close</span>
            </button>
          </div>

          <!-- Precio del Sector -->
          <div class="flex flex-col gap-0.5 pt-2 border-t border-border-structural/60">
            <div class="flex items-baseline justify-between text-xs">
              <span class="font-mono text-muted-light">Precio:</span>
              <span v-if="getSectorPrice(activeSector)" class="font-display font-extrabold text-sm sm:text-base text-[#00d2ff]">
                ${{ formatDisplayPrice(getSectorPrice(activeSector)) }}
                <span class="text-[10px] font-mono text-white/80">ARS</span>
              </span>
              <span v-else class="font-mono text-xs text-muted-light italic">
                Consultar en boletería
              </span>
            </div>
            <div v-if="getSectorPrice(activeSector)" class="text-right">
              <span class="font-mono text-[11px] text-accent-sky-light font-semibold tracking-tight">
                + cargo por servicio
              </span>
            </div>
          </div>

          <!-- Capacidad -->
          <div class="flex items-center justify-between text-xs font-mono text-muted-light">
            <span>Capacidad:</span>
            <span class="text-white font-bold">{{ (activeSector.capacity || 1000).toLocaleString('es-AR') }} lugares</span>
          </div>
        </div>
      </transition>
    </div>
  </div>
</template>

<style scoped>
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.15s ease;
}
.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
