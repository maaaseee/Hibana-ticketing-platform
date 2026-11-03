<template>
  <div class="space-y-6">
    <div class="border-b border-border-subtle pb-4">
      <h2 class="text-xl font-display font-bold text-high-emphasis">
        Paso 1: Información General & Recinto
      </h2>
      <p class="text-xs text-muted-light mt-1">
        Define la identidad del espectáculo, medios gráficos y el recinto habilitado.
      </p>
    </div>

    <!-- Title & Slug -->
    <div class="space-y-4">
      <div>
        <label class="block text-xs font-mono uppercase tracking-wider text-muted-light mb-1.5">
          Título Oficial del Evento <span class="text-error">*</span>
        </label>
        <input
          :value="wizardStore.title"
          @input="onTitleInput"
          type="text"
          placeholder="Ej: Cosquín Rock 2026 - Edición 25 Años"
          class="w-full bg-surface-container-deep border border-border-subtle rounded-xl px-4 py-2.5 text-sm text-high-emphasis placeholder-muted focus:outline-none focus:border-primary focus:ring-1 focus:ring-primary transition"
        />
      </div>

      <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
        <div>
          <label class="block text-xs font-mono uppercase tracking-wider text-muted-light mb-1.5">
            Slug / Identificador URL <span class="text-error">*</span>
          </label>
          <div class="relative">
            <span class="absolute left-3.5 top-2.5 text-xs font-mono text-muted">/events/</span>
            <input
              v-model="wizardStore.slug"
              type="text"
              placeholder="cosquin-rock-2026"
              class="w-full bg-surface-container-deep border border-border-subtle rounded-xl pl-20 pr-4 py-2.5 text-sm font-mono text-primary placeholder-muted focus:outline-none focus:border-primary transition"
            />
          </div>
        </div>

        <div>
          <label class="block text-xs font-mono uppercase tracking-wider text-muted-light mb-1.5">
            Categoría Principal <span class="text-error">*</span>
          </label>
          <select
            v-model="wizardStore.category"
            class="w-full bg-surface-container-deep border border-border-subtle rounded-xl px-4 py-2.5 text-sm text-high-emphasis focus:outline-none focus:border-primary transition"
          >
            <option value="CONCIERTOS">Conciertos & Recitales</option>
            <option value="FESTIVALES">Festivales Masivos</option>
            <option value="TEATRO">Teatro & Comedia</option>
            <option value="DEPORTES">Deportes & Torneos</option>
            <option value="ELECTRONICA">Música Electrónica</option>
            <option value="OTRO">Otros Espectáculos</option>
          </select>
        </div>
      </div>
    </div>

    <!-- Venue Selector -->
    <div class="p-4 rounded-2xl bg-surface-container-deep border border-border-subtle space-y-3">
      <div class="flex items-center justify-between">
        <label class="block text-xs font-mono uppercase tracking-wider text-muted-light">
          Recinto Oficial Seleccionado <span class="text-error">*</span>
        </label>
        <span v-if="selectedVenue" class="text-xs font-mono text-secondary flex items-center gap-1">
          <span class="material-symbols-outlined text-sm">verified</span>
          Aforo Máx: {{ resolveVenueCapacity(selectedVenue).toLocaleString() }} personas
        </span>
      </div>

      <select
        v-model="wizardStore.venueId"
        class="w-full bg-surface-card border border-border-subtle rounded-xl px-4 py-2.5 text-sm text-high-emphasis focus:outline-none focus:border-primary transition"
      >
        <option value="" disabled>Selecciona un estadio o arena habilitada...</option>
        <option v-for="venue in venues" :key="venue.id" :value="venue.id">
          {{ venue.name }} — {{ venue.city }} (Aforo: {{ resolveVenueCapacity(venue).toLocaleString() }})
        </option>
      </select>

      <div v-if="selectedVenue" class="text-xs text-muted-light flex items-center gap-1.5 pt-1">
        <span class="material-symbols-outlined text-sm text-muted">pin_drop</span>
        <span>Dirección: {{ selectedVenue.address }}, {{ selectedVenue.city }}, {{ selectedVenue.country }}</span>
      </div>
    </div>

    <!-- Media Assets -->
    <div class="space-y-4">
      <h3 class="text-xs font-mono uppercase tracking-wider text-muted-light">
        Recursos Gráficos (URLs de CDN o Hosting)
      </h3>

      <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
        <div>
          <label class="block text-[11px] font-mono text-muted mb-1">
            Banner Hero Landscape (16:9)
          </label>
          <input
            v-model="wizardStore.heroBannerUrl"
            type="url"
            placeholder="https://..."
            class="w-full bg-surface-container-deep border border-border-subtle rounded-xl px-3.5 py-2 text-xs text-high-emphasis placeholder-muted focus:outline-none focus:border-primary transition"
          />
        </div>

        <div>
          <label class="block text-[11px] font-mono text-muted mb-1">
            Póster Cartelera Vertical (3:4)
          </label>
          <input
            v-model="wizardStore.posterUrl"
            type="url"
            placeholder="https://..."
            class="w-full bg-surface-container-deep border border-border-subtle rounded-xl px-3.5 py-2 text-xs text-high-emphasis placeholder-muted focus:outline-none focus:border-primary transition"
          />
        </div>
      </div>
    </div>

    <!-- Description -->
    <div>
      <label class="block text-xs font-mono uppercase tracking-wider text-muted-light mb-1.5">
        Descripción del Evento & Line-up
      </label>
      <textarea
        v-model="wizardStore.description"
        rows="4"
        placeholder="Detalla los artistas participantes, condiciones de acceso y line-up oficial..."
        class="w-full bg-surface-container-deep border border-border-subtle rounded-xl px-4 py-2.5 text-sm text-high-emphasis placeholder-muted focus:outline-none focus:border-primary transition"
      ></textarea>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useEventWizardStore } from '@/stores/eventWizard'
import type { VenueResponse } from '@/types/venue'
import { resolveVenueCapacity } from '@/services/venueService'

const props = defineProps<{
  venues: VenueResponse[]
}>()

const wizardStore = useEventWizardStore()

const selectedVenue = computed(() => {
  return props.venues.find((v) => v.id === wizardStore.venueId)
})

function onTitleInput(e: Event) {
  const target = e.target as HTMLInputElement
  wizardStore.updateTitle(target.value)
}
</script>
