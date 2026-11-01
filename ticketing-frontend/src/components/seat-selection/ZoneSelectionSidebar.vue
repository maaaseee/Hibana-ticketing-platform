<script setup lang="ts">
import type { GeneralSector, SectorPricingInfo } from './MacroVenueMap.vue'
import type { SectorZone } from './ZoneSelectorCanvas.vue'

defineProps<{
  selectedZone: SectorZone
  selectedSector: GeneralSector
  selectedSectorBackend: SectorPricingInfo
  currentZonePriceFormatted: string
  sectorZones: SectorZone[]
  getZoneStats: (zone: SectorZone) => { available: number; total: number; percentage: number }
  getZonePrice: (zone: SectorZone) => number
  formatMoney: (val: number) => string
}>()

const emit = defineEmits<{
  (e: 'selectZone', zone: SectorZone): void
  (e: 'advance'): void
}>()
</script>

<template>
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
            @click="emit('selectZone', z)"
            @dblclick="emit('advance')"
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
        @click="emit('advance')"
        class="w-full bg-[#00688f] hover:bg-[#005a7d] text-white font-mono text-sm font-bold py-3.5 rounded-xl border border-[#00688f] transition-colors flex items-center justify-center gap-2 cursor-pointer shadow-lg"
      >
        <span>Continuar a Selección de Butacas</span>
        <span class="material-symbols-outlined text-[18px]">arrow_forward</span>
      </button>
    </div>
  </aside>
</template>
