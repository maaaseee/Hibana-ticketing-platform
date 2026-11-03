<script setup lang="ts">
import { computed } from 'vue'
import type { GeneralSector, SectorPricingInfo } from './MacroVenueMap.vue'
import type { SectorZone } from './ZoneSelectorCanvas.vue'
import type { VenueSeatMapLayout } from '../../types'

export interface SeatItem {
  id: string
  rowLetter: string
  seatNum: number
  price: number
  status: 'AVAILABLE' | 'SELECTED' | 'OCCUPIED' | 'ACCESSIBLE'
}

export interface SeatRow {
  letter: string
  seats: SeatItem[]
}

const props = defineProps<{
  selectedSector: GeneralSector
  selectedSectorBackend: SectorPricingInfo
  selectedZone: SectorZone
  currentZonePriceFormatted: string
  venueLayout?: VenueSeatMapLayout | null
  seatRows: SeatRow[]
  zoomLevel: number
  isSectorActive: (secId: string, secName?: string) => boolean
}>()

const columnNumbers = computed(() => {
  if (!props.seatRows.length || !props.seatRows[0]?.seats.length) return []
  return props.seatRows[0].seats.map(s => s.seatNum)
})

const emit = defineEmits<{
  (e: 'changeZone'): void
  (e: 'toggleSeat', seat: SeatItem): void
  (e: 'update:zoomLevel', val: number): void
  (e: 'autoSelectBestSeat'): void
}>()
</script>

<template>
  <section class="flex-1 relative bg-[#0c141f] p-4 flex flex-col items-center justify-between overflow-auto border-r border-[#23354d]">
    <!-- Top Indicator Bar with Dynamic Stage Compass -->
    <div class="w-full max-w-5xl flex items-center justify-between z-20 shrink-0">
      <button
        @click="emit('changeZone')"
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
          <div class="w-3.5 h-3.5 rounded bg-[#0084ff] border border-[#38bdf8] font-bold shadow-[0_0_8px_rgba(56,189,248,0.5)]"></div>
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
        @click="emit('update:zoomLevel', Math.min(zoomLevel + 0.15, 1.6))"
        class="w-8 h-8 flex items-center justify-center text-[#94a3b8] hover:text-white rounded cursor-pointer"
        title="Acercar"
      >
        <span class="material-symbols-outlined text-[18px]">zoom_in</span>
      </button>
      <div class="w-full h-px bg-[#23354d]"></div>
      <button
        @click="emit('update:zoomLevel', Math.max(zoomLevel - 0.15, 0.75))"
        class="w-8 h-8 flex items-center justify-center text-[#94a3b8] hover:text-white rounded cursor-pointer"
        title="Alejar"
      >
        <span class="material-symbols-outlined text-[18px]">zoom_out</span>
      </button>
      <div class="w-full h-px bg-[#23354d]"></div>
      <button
        @click="emit('update:zoomLevel', 1)"
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
      <!-- Elemento de Escenario (Orientación Clara) -->
      <div class="w-full max-w-md mb-6 flex flex-col items-center select-none">
        <div class="w-full h-8 bg-gradient-to-b from-[#00688f]/60 to-[#101825] border-t-2 border-[#38bdf8] rounded-t-lg flex items-center justify-center shadow-lg">
          <span class="font-display font-bold text-xs uppercase tracking-widest text-[#86cffb]">ESCENARIO</span>
        </div>
      </div>

      <!-- Seat Rows Container -->
      <div class="flex flex-col gap-1.5 p-4 bg-[#101825]/90 border border-[#23354d] rounded-xl shadow-2xl">
        <!-- Fila Superior con Números de Cada Columna de Butacas -->
        <div class="flex items-center gap-2 pb-1.5 border-b border-[#23354d]/70 mb-1">
          <!-- Espaciador izquierdo alineado con la letra de fila -->
          <div class="w-5 h-5 shrink-0 flex items-center justify-center font-mono text-[9px] text-[#64748b]">
            #
          </div>

          <!-- Números de columnas alineados con cada columna de asientos -->
          <div class="flex items-center gap-1.5">
            <div
              v-for="colNum in columnNumbers"
              :key="colNum"
              class="w-4 sm:w-4.5 flex items-center justify-center font-mono text-[10px] text-[#86cffb] font-bold select-none shrink-0"
            >
              {{ colNum }}
            </div>
          </div>

          <!-- Espaciador derecho alineado con la letra de fila -->
          <div class="w-5 h-5 shrink-0"></div>
        </div>

        <div
          v-for="row in seatRows"
          :key="row.letter"
          class="flex items-center gap-2"
        >
          <!-- Left Row Badge (a..s) -->
          <div class="w-5 h-5 rounded bg-[#1b2533] border border-[#23354d] flex items-center justify-center font-mono text-[10px] text-[#86cffb] font-bold uppercase shrink-0">
            {{ row.letter }}
          </div>

          <!-- Seats in this row with authentic chair shapes and bioluminescent interactions -->
          <div class="flex items-center gap-1.5">
            <div
              v-for="seat in row.seats"
              :key="seat.id"
              @click="emit('toggleSeat', seat)"
              class="relative transition-all duration-200 flex items-center justify-center select-none"
              :class="[
                seat.status === 'OCCUPIED' ? 'cursor-not-allowed opacity-70' : 'cursor-pointer hover:scale-125 active:scale-95',
                seat.status === 'SELECTED' ? 'scale-115' : ''
              ]"
              :title="`Fila ${seat.rowLetter.toUpperCase()}, Butaca ${seat.seatNum}`"
            >
              <!-- Ripple Expansive Feedback on Selected State -->
              <span
                v-if="seat.status === 'SELECTED'"
                class="absolute -inset-1 rounded-full border-2 border-[#38bdf8] pointer-events-none animate-seat-pulse"
              ></span>

              <!-- Realistic Stadium Chair SVG Glyph -->
              <svg
                viewBox="0 0 20 20"
                class="w-4 h-4 sm:w-4.5 sm:h-4.5 transition-all duration-200"
                :style="seat.status === 'SELECTED' ? { filter: 'drop-shadow(0 0 5px rgba(0, 180, 255, 0.85))' } : {}"
              >
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

      <!-- Bottom Metadata: Unified Zona Label without Range Price -->
      <div class="mt-4 flex items-center gap-3 font-mono text-xs text-[#94a3b8]">
        <span>Zona: <strong class="text-[#86cffb]">{{ selectedSector.name }} • {{ selectedZone.name }}</strong></span>
        <span>•</span>
        <span>Tarifa: <strong class="text-white">{{ currentZonePriceFormatted }} c/u</strong></span>
      </div>
    </div>

    <!-- Mobile Assisted Seat Selector Pill (Fase 4.1) -->
    <button
      type="button"
      @click="emit('autoSelectBestSeat')"
      class="sm:hidden fixed bottom-6 left-1/2 -translate-x-1/2 z-40 bg-surface-card/95 hover:bg-surface-elevated border border-primary/50 text-white font-mono text-xs font-bold py-2.5 px-5 rounded-full shadow-[0_0_20px_rgba(0,104,143,0.5)] flex items-center gap-2 backdrop-blur-md active:scale-95 transition-all cursor-pointer whitespace-nowrap"
    >
      <span class="material-symbols-outlined text-[18px] text-secondary">auto_fix_high</span>
      <span>Elegir mejor asiento disponible</span>
    </button>

    <div class="w-full text-center text-xs font-mono text-[#64748b] pb-2">
      Haz clic en cualquier butaca verde disponible para agregarla o quitarla de tu selección.
    </div>
  </section>
</template>

<style scoped>
@keyframes seat-pulse {
  0% {
    transform: scale(0.85);
    opacity: 0.9;
  }
  50% {
    transform: scale(1.35);
    opacity: 0.4;
  }
  100% {
    transform: scale(1.6);
    opacity: 0;
  }
}

.animate-seat-pulse {
  animation: seat-pulse 0.5s cubic-bezier(0, 0, 0.2, 1) forwards;
}
</style>
