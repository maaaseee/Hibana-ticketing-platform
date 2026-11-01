<script setup lang="ts">
import { ref } from 'vue'

export interface GeneralSector {
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

export interface SectorPricingInfo {
  price: number
  minPrice: number
  maxPrice: number
  capacity: number
  currency: string
  formatted: string
  priceRangeFormatted: string
}

defineProps<{
  generalSectors: GeneralSector[]
  selectedSectorId: string
  getBackendSectorInfo: (secId: string, secName: string, fallbackPrice?: number) => SectorPricingInfo
}>()

const emit = defineEmits<{
  (e: 'selectSector', sec: GeneralSector): void
  (e: 'advanceStep'): void
}>()

const hoveredSectorId = ref<string | null>(null)
</script>

<template>
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
          @click="emit('selectSector', sec)"
          @dblclick="emit('advanceStep')"
          @mouseenter="hoveredSectorId = sec.id"
          @mouseleave="hoveredSectorId = null"
        >
          <!-- Sector Polygon -->
          <polygon
            :points="sec.polygonPoints"
            :fill="selectedSectorId === sec.id ? sec.colorHover : (hoveredSectorId === sec.id ? sec.colorHover : sec.colorFill)"
            :stroke="selectedSectorId === sec.id ? '#ffffff' : sec.colorStroke"
            :stroke-width="selectedSectorId === sec.id ? 3.5 : (hoveredSectorId === sec.id ? 2.5 : 1.5)"
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
</template>
