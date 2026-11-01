<script setup lang="ts">
import { ref } from 'vue'
import type { GeneralSector } from './MacroVenueMap.vue'
import type { VenueSeatMapLayout } from '../../types'

export interface SectorZone {
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

defineProps<{
  selectedSector: GeneralSector
  sectorZones: SectorZone[]
  selectedZone: SectorZone
  venueLayout?: VenueSeatMapLayout | null
  getZoneStats: (zone: SectorZone) => { available: number; total: number }
  getZonePrice: (zone: SectorZone) => number
  formatMoney: (val: number) => string
  isSectorActive: (secId: string, secName?: string) => boolean
}>()

const emit = defineEmits<{
  (e: 'selectZone', zone: SectorZone): void
  (e: 'advance'): void
  (e: 'goBack'): void
}>()

const hoveredZoneId = ref<string | null>(null)
</script>

<template>
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
          @click="emit('selectZone', sectorZones[0]!)"
          @dblclick="emit('advance')"
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
          @click="emit('selectZone', sectorZones[1]!)"
          @dblclick="emit('advance')"
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
          @click="emit('selectZone', sectorZones[2]!)"
          @dblclick="emit('advance')"
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
        @click="emit('goBack')"
        class="flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-[#141c27] hover:bg-[#1b2533] border border-[#23354d] text-[#86cffb] transition-colors cursor-pointer"
      >
        <span class="material-symbols-outlined text-[16px]">arrow_back</span>
        <span>Volver a Sectores Generales</span>
      </button>
      <span>Sector: <strong class="text-white">{{ selectedSector.name }}</strong> (Doble clic en cualquier zona para entrar)</span>
    </div>
  </section>
</template>
