<template>
  <div class="sticky top-24 bg-surface-container border border-border-subtle rounded-3xl p-5 shadow-xl overflow-hidden">
    <!-- Header -->
    <div class="flex items-center justify-between pb-3 mb-4 border-b border-border-subtle">
      <div class="flex items-center gap-2">
        <span class="w-2 h-2 rounded-full bg-primary animate-pulse"></span>
        <span class="text-xs font-mono font-bold tracking-wider uppercase text-primary">
          Live B2C Preview
        </span>
      </div>
      <span class="text-[10px] font-mono text-muted">Vista del Comprador</span>
    </div>

    <!-- Event Card (B2C Style) -->
    <div class="bg-surface-card rounded-2xl overflow-hidden border border-border-subtle/80 shadow-md group">
      <!-- Banner Image -->
      <div class="relative aspect-video w-full bg-surface-container-deep overflow-hidden">
        <img
          :src="wizardStore.heroBannerUrl || 'https://images.unsplash.com/photo-1470225620780-dba8ba36b745?auto=format&fit=crop&w=800&q=80'"
          alt="Preview Banner"
          class="w-full h-full object-cover transition duration-500 group-hover:scale-105"
          @error="onImageError"
        />
        <div class="absolute inset-0 bg-gradient-to-t from-surface-card via-transparent to-black/30"></div>

        <!-- Badges -->
        <div class="absolute top-3 left-3 flex flex-wrap gap-1.5">
          <span class="text-[10px] font-mono px-2 py-0.5 rounded-full bg-primary/20 backdrop-blur-md text-primary border border-primary/30 uppercase font-bold">
            {{ wizardStore.category }}
          </span>
          <span
            v-if="wizardStore.waitingRoomEnabled"
            class="text-[10px] font-mono px-2 py-0.5 rounded-full bg-secondary/20 backdrop-blur-md text-secondary border border-secondary/30 uppercase font-semibold flex items-center gap-1"
          >
            <span class="w-1.5 h-1.5 rounded-full bg-secondary animate-ping"></span>
            Sala Espera
          </span>
        </div>
      </div>

      <!-- Content Info -->
      <div class="p-4 space-y-3">
        <div>
          <h4 class="font-display font-black text-lg text-high-emphasis leading-tight line-clamp-1">
            {{ wizardStore.title || 'Nombre Oficial del Evento' }}
          </h4>
          <p class="text-xs text-muted-light mt-1 flex items-center gap-1">
            <span class="material-symbols-outlined text-sm text-primary">location_on</span>
            {{ venueName }}
          </p>
        </div>

        <!-- Schedule -->
        <div class="flex items-center gap-2 text-xs font-mono text-on-surface-variant bg-surface-container-deep px-3 py-2 rounded-xl">
          <span class="material-symbols-outlined text-base text-secondary">calendar_month</span>
          <span>{{ formattedDate }}</span>
        </div>

        <!-- Price Range & Sectors -->
        <div class="pt-2 border-t border-border-subtle flex items-center justify-between">
          <div>
            <span class="text-[10px] font-mono uppercase text-muted block">Desde</span>
            <span class="text-sm font-bold text-high-emphasis font-mono">
              ${{ minPrice.toLocaleString() }}
              <span class="text-[10px] text-muted font-normal">ARS</span>
            </span>
          </div>

          <div class="text-right">
            <span class="text-[10px] font-mono uppercase text-muted block">Aforo</span>
            <span class="text-xs font-mono font-semibold text-secondary">
              {{ wizardStore.totalCapacity.toLocaleString() }}
            </span>
            <span class="text-[10px] text-muted ml-0.5">lugares</span>
          </div>
        </div>
      </div>
    </div>

    <!-- Live Telemetry / Audit Badge -->
    <div class="mt-4 p-3 rounded-xl bg-surface-container-deep border border-border-subtle space-y-1.5 text-[11px] font-mono">
      <div class="flex justify-between text-muted-light">
        <span>Slug:</span>
        <span class="text-primary truncate max-w-[160px]">{{ wizardStore.slug || '—' }}</span>
      </div>
      <div class="flex justify-between text-muted-light">
        <span>Sectores:</span>
        <span class="text-high-emphasis">{{ wizardStore.sectors.length }} configurados</span>
      </div>
      <div class="flex justify-between text-muted-light">
        <span>Recaudación estimada:</span>
        <span class="text-secondary font-bold">${{ wizardStore.grossRevenueProjection.toLocaleString() }}</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useEventWizardStore } from '@/stores/eventWizard'
import type { VenueResponse } from '@/types/venue'

const props = defineProps<{
  venues: VenueResponse[]
}>()

const wizardStore = useEventWizardStore()

const venueName = computed(() => {
  const v = props.venues.find((venue) => venue.id === wizardStore.venueId)
  return v ? `${v.name} (${v.city})` : 'Selecciona un recinto'
})

const formattedDate = computed(() => {
  if (!wizardStore.startsAt) return 'Fecha por definir'
  try {
    const d = new Date(wizardStore.startsAt)
    return d.toLocaleDateString('es-AR', {
      weekday: 'short',
      day: 'numeric',
      month: 'short',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    })
  } catch {
    return wizardStore.startsAt
  }
})

const minPrice = computed(() => {
  if (!wizardStore.sectors.length) return 0
  const prices = wizardStore.sectors.map((s) => Number(s.basePrice) || 0)
  return Math.min(...prices)
})

function onImageError(e: Event) {
  const target = e.target as HTMLImageElement
  target.src = 'https://images.unsplash.com/photo-1470225620780-dba8ba36b745?auto=format&fit=crop&w=800&q=80'
}
</script>
