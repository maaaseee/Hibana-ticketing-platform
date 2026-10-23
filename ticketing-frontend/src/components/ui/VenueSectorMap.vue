<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import type { VenueSeatMapLayout, SectorGeometry, SectorPricing } from '../../types'

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

// Mapear los nombres de archivos .json según el venueId
function resolveVenueJson(id?: string): string {
  if (!id) return '/seat-maps/arena-buenos-aires.json'
  if (id.includes('monumental')) return '/seat-maps/estadio-monumental.json'
  if (id.includes('rex')) return '/seat-maps/teatro-rex.json'
  if (id.includes('arena')) return '/seat-maps/arena-buenos-aires.json'
  return `/seat-maps/${id}.json`
}

async function loadVenueMap() {
  loading.value = true
  error.value = null
  const url = resolveVenueJson(props.venueId)

  try {
    const res = await fetch(url)
    if (!res.ok) {
      throw new Error(`No se pudo cargar el mapa (${res.status})`)
    }
    const data: VenueSeatMapLayout = await res.json()
    layout.value = data
  } catch (err: any) {
    console.warn('Error loading seat map json:', err)
    error.value = err?.message || 'Error al cargar mapa'
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

// Obtener precio correspondiente al sector según la lista de precios de la función
function getSectorPrice(sec: SectorGeometry): number | undefined {
  if (!props.sectorsPricing || props.sectorsPricing.length === 0) return undefined
  // 1. Coincidencia por ID
  const byId = props.sectorsPricing.find(p => p.id === sec.id)
  if (byId) return byId.price

  // 2. Coincidencia por Tipo o Nombre aproximado
  const byType = props.sectorsPricing.find(p => p.type === sec.type)
  if (byType) return byType.price

  return undefined
}

function handleSectorClick(sec: SectorGeometry) {
  if (!props.interactive) return
  if (sec.selectable === false || sec.vendible === false) return
  emit('select-sector', sec)
}
</script>

<template>
  <div class="relative w-full h-full flex flex-col items-center justify-center select-none overflow-hidden">
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
    <div v-else class="relative w-full h-full flex items-center justify-center">
      <svg
        :viewBox="`0 0 ${layout.canvasWidth} ${layout.canvasHeight}`"
        class="w-full h-full max-h-full transition-all duration-300 drop-shadow-md"
        xmlns="http://www.w3.org/2000/svg"
      >
        <!-- Filtros y Efectos Luminiscentes -->
        <defs>
          <filter id="glow-sector" x="-20%" y="-20%" width="140%" height="140%">
            <feGaussianBlur stdDeviation="6" result="blur" />
            <feComposite in="SourceGraphic" in2="blur" operator="over" />
          </filter>
        </defs>

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
              hoveredSector?.id === sec.id ? 'opacity-100 filter-drop-shadow' : 'opacity-85 hover:opacity-100'
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
                :stroke="hoveredSector?.id === sec.id ? '#ffffff' : sec.strokeColor"
                :stroke-width="hoveredSector?.id === sec.id ? 3 : 1.5"
                class="transition-all"
              />
              <text
                :x="sec.labelPosition ? sec.labelPosition[0] : (sec.x + sec.width / 2)"
                :y="(sec.labelPosition ? sec.labelPosition[1] : (sec.y + sec.height / 2)) + 4"
                text-anchor="middle"
                fill="#f1f5f9"
                font-family="Space Grotesk, sans-serif"
                font-weight="600"
                :font-size="compact ? '9' : '11'"
                class="pointer-events-none select-none drop-shadow"
              >
                <template v-if="splitSectorLabel(sec.name).line2">
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
                :stroke="hoveredSector?.id === sec.id ? '#ffffff' : sec.strokeColor"
                :stroke-width="hoveredSector?.id === sec.id ? 3 : 1.8"
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

            <!-- 3. Sector PATH (SVG directo de trazado arquitectónico) -->
            <template v-else-if="sec.shapeType === 'PATH'">
              <path
                :d="sec.pathData"
                :fill="sec.color"
                :stroke="hoveredSector?.id === sec.id ? '#ffffff' : sec.strokeColor"
                :stroke-width="hoveredSector?.id === sec.id ? 3 : 1.8"
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
                :stroke="hoveredSector?.id === sec.id ? '#ffffff' : sec.strokeColor"
                :stroke-width="hoveredSector?.id === sec.id ? 3 : 1.5"
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
      </svg>

      <!-- Tooltip Flotante de Sector en Hover -->
      <transition name="fade">
        <div
          v-if="hoveredSector && interactive"
          class="absolute top-3 left-3 bg-surface-dim/95 border border-border-subtle rounded-xl p-3 shadow-2xl backdrop-blur-md pointer-events-none flex flex-col gap-1 z-20 max-w-xs animate-in fade-in-0 duration-150"
        >
          <div class="flex items-center gap-2">
            <span
              class="w-3 h-3 rounded-full shrink-0"
              :style="{ backgroundColor: hoveredSector.strokeColor }"
            ></span>
            <span class="font-display font-bold text-xs text-high-emphasis truncate">
              {{ hoveredSector.name }}
            </span>
          </div>

          <div class="flex items-center justify-between gap-4 font-mono text-[11px] text-muted-light mt-0.5">
            <span>Tipo: {{ hoveredSector.type.replace('_', ' ') }}</span>
            <span v-if="hoveredSector.vendible !== false && hoveredSector.selectable !== false" class="text-secondary font-semibold">
              Capacidad: {{ hoveredSector.capacity.toLocaleString('es-AR') }}
            </span>
            <span v-else class="text-amber-400 font-semibold text-[10px]">
              No vendible (Uso técnico)
            </span>
          </div>

          <div v-if="getSectorPrice(hoveredSector)" class="mt-1 pt-1.5 border-t border-border-subtle/50 flex items-center justify-between text-xs">
            <span class="font-mono text-[11px] text-medium-emphasis">Precio oficial:</span>
            <span class="font-mono font-extrabold text-primary">
              ${{ getSectorPrice(hoveredSector)?.toLocaleString('es-AR') }} ARS
            </span>
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
