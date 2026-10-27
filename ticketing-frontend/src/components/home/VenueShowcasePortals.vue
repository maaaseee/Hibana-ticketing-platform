<script setup lang="ts">
import { computed } from 'vue'
import type { VenueWithImage } from './VenueDetailModal.vue'

const props = withDefaults(
  defineProps<{
    venues?: VenueWithImage[]
    venuesList?: VenueWithImage[]
  }>(),
  {
    venues: () => [],
    venuesList: () => []
  }
)

const activeVenues = computed(() => {
  return props.venues && props.venues.length > 0 ? props.venues : props.venuesList
})

const emit = defineEmits<{
  (e: 'selectVenue', venue: VenueWithImage): void
  (e: 'openVenueModal', venue: VenueWithImage): void
}>()

function onVenueClick(venue: VenueWithImage) {
  emit('selectVenue', venue)
  emit('openVenueModal', venue)
}
</script>

<template>
  <!-- 5. CONOCE NUESTROS RECINTOS Y SEDES -->
  <section class="relative w-full overflow-hidden py-10">
    <!-- Background SVG -->
    <div
      class="absolute inset-0 pointer-events-none z-0 overflow-hidden select-none"
      style="mask-image: linear-gradient(to bottom, transparent 0%, black 15%, black 85%, transparent 100%); -webkit-mask-image: linear-gradient(to bottom, transparent 0%, black 15%, black 85%, transparent 100%);"
    >
      <img
        :src="'/venue-architecture.svg'"
        alt=""
        aria-hidden="true"
        class="w-full h-full object-cover object-center opacity-35 select-none pointer-events-none"
      />
    </div>

    <div class="relative z-10 w-full max-w-[80vw] mx-auto px-4 sm:px-6 lg:px-8 flex flex-col gap-6">
      <div class="flex flex-col gap-1 border-b border-border-subtle pb-4">
        <span class="font-mono text-xs text-secondary uppercase tracking-wider font-semibold">
          Sedes, Accesos y Transporte
        </span>
        <h2 class="font-display text-2xl sm:text-3xl font-bold text-high-emphasis">
          Conoce los Lugares y Recintos
        </h2>
        <p class="font-body text-sm text-muted-light">
          Información clave sobre cómo llegar en transporte público, accesos vehiculares y estacionamiento oficial en cada estadio o teatro.
        </p>
      </div>

      <!-- Grid de Recintos con Zoom y Botón Circular -->
      <div class="grid grid-cols-1 md:grid-cols-3 gap-6">
        <article
          v-for="venue in activeVenues"
          :key="venue.id"
          @click="onVenueClick(venue)"
          class="relative h-96 rounded-2xl overflow-hidden border border-border-subtle hover:border-secondary/60 transition-all duration-300 group cursor-pointer shadow-lg bg-surface flex flex-col justify-between p-6"
        >
          <!-- Imagen de Fondo con Efecto Zoom al Hover -->
          <div class="absolute inset-0 overflow-hidden pointer-events-none">
            <img
              :src="venue.heroImage"
              :alt="venue.name"
              class="w-full h-full object-cover group-hover:scale-110 transition-transform duration-700 ease-out"
            />
            <!-- Gradiente para Oscurecer y Dar Alto Contraste al Texto -->
            <div class="absolute inset-0 bg-linear-to-t from-surface via-surface/60 to-surface/20"></div>
          </div>

          <!-- Cabecera de la Tarjeta: Tipo de Recinto -->
          <div class="relative z-10 flex items-center justify-between">
            <span class="inline-flex items-center gap-1.5 px-3 py-1 rounded-full bg-surface/90 border border-border-subtle text-secondary font-mono text-xs font-bold uppercase tracking-wider backdrop-blur-sm shadow-md">
              <span class="material-symbols-outlined text-[14px]">stadium</span>
              <span>{{ venue.type.replace('_', ' ') }}</span>
            </span>
          </div>

          <!-- Pie de la Tarjeta: Nombre, Ubicación y Botón Circular de Detalle -->
          <div class="relative z-10 flex items-end justify-between gap-4">
            <div class="flex flex-col gap-1.5">
              <h3 class="font-display text-xl sm:text-2xl font-black text-white group-hover:text-secondary transition-colors drop-shadow-sm leading-tight">
                {{ venue.name }}
              </h3>
              <p class="font-body text-xs text-muted-light flex items-center gap-1">
                <span class="material-symbols-outlined text-sm text-secondary">location_on</span>
                <span>{{ venue.city }}, {{ venue.country }}</span>
              </p>
            </div>

            <!-- Botón Circular con Flecha Diagonal (Abre Modal Informativo) -->
            <div
              class="w-12 h-12 rounded-full bg-surface/90 border border-border-subtle group-hover:border-secondary text-high-emphasis group-hover:text-secondary group-hover:bg-surface-interactive flex items-center justify-center shrink-0 shadow-lg transition-all group-hover:scale-110"
              title="Ver información del recinto"
            >
              <span class="material-symbols-outlined text-xl group-hover:translate-x-0.5 group-hover:-translate-y-0.5 transition-transform">
                arrow_outward
              </span>
            </div>
          </div>
        </article>
      </div>
    </div>
  </section>
</template>
