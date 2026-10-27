<script setup lang="ts">
import type { EventItem, VenueTransit } from '../../types'

export type TransitInfo = VenueTransit

defineProps<{
  event: EventItem
  venueAddress: string
  transitInfo?: VenueTransit | null
  mapEmbedUrl: string
}>()
</script>

<template>
  <!-- Section F: Ubicación del Recinto, Mapa Interactivo & Transporte Público -->
  <div class="bg-surface-card rounded-xl p-6 border border-border-subtle shadow-xl flex flex-col gap-5">
    <div class="flex flex-col sm:flex-row sm:items-center justify-between gap-3 pb-3 border-b border-border-subtle">
      <div class="flex items-center gap-3">
        <div class="w-10 h-10 rounded-lg bg-surface-interactive border border-border-subtle flex items-center justify-center text-primary shrink-0">
          <span class="material-symbols-outlined text-[20px]">map</span>
        </div>
        <div>
          <h2 class="font-display text-xl font-bold text-high-emphasis">Ubicación y Cómo Llegar</h2>
          <p class="font-mono text-xs text-muted-light">{{ event.venue }} • {{ venueAddress }}</p>
        </div>
      </div>

      <a
        :href="transitInfo?.googleMapsUrl || `https://www.google.com/maps/search/?api=1&query=${encodeURIComponent(event.venue + ' ' + venueAddress)}`"
        target="_blank"
        rel="noopener noreferrer"
        class="self-start sm:self-auto inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-surface-container-deep hover:bg-surface-interactive border border-border-subtle text-secondary hover:text-white font-mono text-xs font-semibold transition-all group shadow-sm"
      >
        <span>Abrir en Google Maps</span>
        <span class="material-symbols-outlined text-[15px] group-hover:translate-x-0.5 transition-transform">open_in_new</span>
      </a>
    </div>

    <!-- Mapa de Ubicación del Recinto (Google Maps) -->
    <div class="relative w-full h-64 sm:h-72 rounded-xl overflow-hidden border border-border-subtle bg-surface">
      <iframe
        title="Mapa de ubicación del recinto en Google Maps"
        width="100%"
        height="100%"
        style="border:0;"
        loading="lazy"
        referrerpolicy="no-referrer-when-downgrade"
        :src="mapEmbedUrl"
      ></iframe>
    </div>

    <!-- Desglose de Transporte Público Cercano (Colectivos, Trenes, Subtes) -->
    <div v-if="transitInfo" class="grid grid-cols-1 md:grid-cols-3 gap-4 pt-2">
      <!-- Colectivos -->
      <div class="p-4 rounded-xl bg-surface-container-deep border border-border-subtle flex flex-col gap-2">
        <div class="flex items-center gap-2 text-primary font-mono text-xs uppercase font-bold tracking-wider">
          <span class="material-symbols-outlined text-[17px]">directions_bus</span>
          <span>Líneas de Colectivo</span>
        </div>
        <div class="flex flex-wrap gap-1.5 pt-1">
          <span
            v-for="bus in transitInfo.busLines"
            :key="bus"
            class="px-2.5 py-0.5 rounded-md bg-surface-card border border-border-subtle text-high-emphasis font-mono text-xs font-bold shadow-sm"
          >
            {{ bus }}
          </span>
        </div>
        <p class="font-body text-[11px] text-muted-light mt-auto pt-1">
          Paradas activas en un radio de 300m del acceso principal.
        </p>
      </div>

      <!-- Trenes -->
      <div class="p-4 rounded-xl bg-surface-container-deep border border-border-subtle flex flex-col gap-2">
        <div class="flex items-center gap-2 text-secondary font-mono text-xs uppercase font-bold tracking-wider">
          <span class="material-symbols-outlined text-[17px]">train</span>
          <span>Estaciones de Tren</span>
        </div>
        <ul class="space-y-1.5 font-body text-xs text-high-emphasis pt-1">
          <li
            v-for="(train, tIdx) in transitInfo.trains"
            :key="tIdx"
            class="flex items-start gap-1.5 leading-snug"
          >
            <span class="material-symbols-outlined text-[14px] text-secondary mt-0.5 shrink-0">fiber_manual_record</span>
            <span>{{ train }}</span>
          </li>
        </ul>
      </div>

      <!-- Subte / Metro -->
      <div class="p-4 rounded-xl bg-surface-container-deep border border-border-subtle flex flex-col gap-2">
        <div class="flex items-center gap-2 text-primary font-mono text-xs uppercase font-bold tracking-wider">
          <span class="material-symbols-outlined text-[17px]">subway</span>
          <span>Líneas de Subte</span>
        </div>
        <ul class="space-y-1.5 font-body text-xs text-high-emphasis pt-1">
          <li
            v-for="(sub, sIdx) in transitInfo.subway"
            :key="sIdx"
            class="flex items-start gap-1.5 leading-snug"
          >
            <span class="material-symbols-outlined text-[14px] text-primary mt-0.5 shrink-0">fiber_manual_record</span>
            <span>{{ sub }}</span>
          </li>
        </ul>
      </div>
    </div>
  </div>
</template>
