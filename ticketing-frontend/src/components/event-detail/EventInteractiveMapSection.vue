<script setup lang="ts">
import VenueSectorMap from '../ui/VenueSectorMap.vue'
import type { EventItem, VenueInfo, SectorPricing } from '../../types'

defineProps<{
  event: EventItem
  currentVenue?: VenueInfo
  sectors: SectorPricing[]
}>()
</script>

<template>
  <!-- Section E: Plano Interactivo del Escenario & Precios por Sector (Estilo Google Maps) -->
  <div
    class="bg-surface-card rounded-2xl p-6 border border-border-subtle shadow-xl flex flex-col gap-4"
    data-testid="interactive-venue-map-section"
  >
    <div class="flex flex-col sm:flex-row sm:items-center justify-between gap-3 pb-3 border-b border-border-subtle">
      <div class="flex items-center gap-3">
        <div class="w-10 h-10 rounded-xl bg-surface-interactive border border-border-subtle flex items-center justify-center text-primary shrink-0 shadow-sm">
          <span class="material-symbols-outlined text-[22px]">stadium</span>
        </div>
        <div>
          <h2 class="font-display text-xl font-bold text-high-emphasis">Plano Interactivo & Precios por Sector</h2>
          <p class="font-mono text-xs text-muted-light">{{ event.venue }} • Mapa del escenario y ubicaciones</p>
        </div>
      </div>

      <!-- Indicador destacado de que los precios se exploran desde el mapa -->
      <div class="inline-flex items-center gap-2 px-3.5 py-1.5 rounded-full bg-primary/10 border border-primary/30 text-primary text-xs font-mono font-bold shadow-xs">
        <span class="material-symbols-outlined text-[16px] animate-pulse">touch_app</span>
        <span>Precios visibles al interactuar en el mapa</span>
      </div>
    </div>

    <!-- Contenedor del Mapa Interactivo -->
    <div class="relative w-full h-115 sm:h-140 rounded-2xl overflow-hidden bg-[#071322] border border-border-structural/80 shadow-2xl">
      <VenueSectorMap
        :venue-id="event.seatMapUrl || event.venueId || currentVenue?.id"
        :sectors-pricing="sectors"
        :compact="false"
        :interactive="true"
      />
    </div>
  </div>
</template>
