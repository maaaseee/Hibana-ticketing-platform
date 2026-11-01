<script setup lang="ts">
import {
  DialogRoot,
  DialogTrigger,
  DialogPortal,
  DialogOverlay,
  DialogContent,
  DialogTitle,
  DialogDescription,
  DialogClose
} from 'radix-vue'
import VenueSectorMap from '../ui/VenueSectorMap.vue'
import type { EventItem, VenueInfo, SectorPricing } from '../../types'

defineProps<{
  event: EventItem
  venueExteriorImage?: string | null
  currentVenue?: VenueInfo
  sectors: SectorPricing[]
}>()
</script>

<template>
  <div class="bg-surface-card rounded-xl p-5 border border-border-subtle flex flex-col justify-between">
    <div class="flex items-center gap-3.5 mb-3">
      <div class="w-10 h-10 rounded-lg bg-surface-interactive border border-border-subtle flex items-center justify-center text-primary shrink-0">
        <span class="material-symbols-outlined text-[20px]">location_on</span>
      </div>
      <div>
        <h3 class="font-display text-lg font-bold text-high-emphasis">{{ event.venue }}</h3>
        <p class="font-mono text-xs text-muted-light">{{ event.location }}</p>
      </div>
    </div>

    <!-- Vista exterior del recinto (Arena Buenos Aires) -->
    <div
      v-if="venueExteriorImage"
      class="w-full h-44 rounded-xl overflow-hidden border border-border-subtle mt-2 relative group shadow-inner"
      data-testid="venue-exterior-image"
    >
      <img
        :src="venueExteriorImage"
        :alt="event.venue"
        class="w-full h-full object-cover transition-transform duration-500 group-hover:scale-105"
      />
      <div class="absolute inset-0 bg-linear-to-t from-black/85 via-black/25 to-transparent flex items-end p-3">
        <span class="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-lg bg-black/60 border border-white/20 text-[11px] font-mono text-white/95 backdrop-blur-md">
          <span class="material-symbols-outlined text-[14px] text-primary">photo_camera</span>
          <span>Fachada y acceso exterior</span>
        </span>
      </div>
    </div>

    <!-- Seat Map Preview with Accessible Radix Dialog Modal -->
    <DialogRoot v-else>
      <DialogTrigger as-child>
        <button
          class="w-full h-44 rounded-xl overflow-hidden border border-border-subtle mt-2 bg-surface-container-deep relative flex items-center justify-center group hover:border-primary/60 transition-all cursor-pointer p-2 shadow-inner"
          aria-label="Ampliar plano del recinto"
        >
          <!-- Renderizador Dinámico de Sectores del Recinto (Miniatura interactiva) -->
          <VenueSectorMap
            :venue-id="event.seatMapUrl || event.venueId || currentVenue?.id"
            :sectors-pricing="sectors"
            :compact="true"
            :interactive="false"
            class="pointer-events-none"
          />

          <div class="absolute bottom-2.5 right-2.5 px-2.5 py-1 rounded-lg bg-surface-card/90 border border-border-subtle text-xs font-mono text-primary group-hover:bg-primary-container group-hover:text-white transition-all flex items-center gap-1.5 shadow-md backdrop-blur-md">
            <span class="material-symbols-outlined text-[15px]">zoom_in</span>
            <span>Ampliar Plano</span>
          </div>
        </button>
      </DialogTrigger>

      <DialogPortal>
        <DialogOverlay class="fixed inset-0 bg-black/85 backdrop-blur-md z-50 animate-fadeIn" />
        <DialogContent class="fixed top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 bg-surface-card border border-border-subtle rounded-3xl p-6 sm:p-8 shadow-2xl z-50 w-[94vw] max-w-4xl max-h-[92vh] overflow-y-auto outline-none flex flex-col gap-5">
          <div class="flex items-center justify-between pb-4 border-b border-border-subtle">
            <div class="flex items-center gap-2.5">
              <div class="w-9 h-9 rounded-xl bg-surface-interactive border border-border-subtle flex items-center justify-center text-primary">
                <span class="material-symbols-outlined text-[20px]">stadium</span>
              </div>
              <div>
                <DialogTitle class="font-display text-xl sm:text-2xl font-bold text-high-emphasis">
                  Plano Oficial de Sectores: {{ event.venue }}
                </DialogTitle>
                <p class="font-mono text-xs text-muted-light mt-0.5">
                  Visualización de sectores habilitados, capacidades y distribución escénica.
                </p>
              </div>
            </div>
            <DialogClose class="w-9 h-9 rounded-full bg-surface-interactive hover:bg-surface-interactive/80 text-muted-light hover:text-white flex items-center justify-center transition-colors cursor-pointer border border-border-subtle">
              <span class="material-symbols-outlined text-base">close</span>
            </DialogClose>
          </div>

          <DialogDescription class="sr-only">
            Mapa interactivo de sectores y distribución arquitectónica del recinto {{ event.venue }}.
          </DialogDescription>

          <!-- Renderizador Dinámico de Sectores en Modal Ampliado -->
          <div class="relative w-full h-115 sm:h-130 rounded-2xl overflow-hidden bg-surface-container-deep border border-border-subtle p-3">
            <VenueSectorMap
              :venue-id="event.seatMapUrl || event.venueId || currentVenue?.id"
              :sectors-pricing="sectors"
              :compact="false"
              :interactive="true"
            />
          </div>

          <!-- Leyenda y Nota de Sectores -->
          <div class="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-3 pt-3 border-t border-border-subtle/60 text-xs font-mono text-muted-light">
            <div class="flex items-center gap-2 text-primary">
              <span class="material-symbols-outlined text-[16px]">touch_app</span>
              <span>Pasa el cursor sobre cada sector para ver detalles, capacidad y precio (+ cargo por servicio)</span>
            </div>
            <span class="text-secondary font-semibold">
              Garantía oficial Hibana.tk • Cero sobreventa
            </span>
          </div>
        </DialogContent>
      </DialogPortal>
    </DialogRoot>
  </div>
</template>
