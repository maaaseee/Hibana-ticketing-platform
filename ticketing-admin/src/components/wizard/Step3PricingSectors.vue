<template>
  <div class="space-y-6">
    <div class="border-b border-border-subtle pb-4 flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3">
      <div>
        <h2 class="text-xl font-display font-bold text-high-emphasis">
          Paso 3: Mapeo de Sectores, Aforos & Tarifas
        </h2>
        <p class="text-xs text-muted-light mt-1">
          Configura cada sector, tipo de admisión, cantidad de localidades y precio base.
        </p>
      </div>

      <button
        type="button"
        @click="addNewSector"
        class="flex items-center gap-1.5 px-3 py-2 rounded-xl bg-primary text-on-primary hover:bg-primary-hover font-semibold text-xs transition shadow-sm"
      >
        <span class="material-symbols-outlined text-base">add</span>
        Añadir Sector
      </button>
    </div>

    <!-- Capacity Bar vs Venue Limit -->
    <div class="p-4 rounded-2xl bg-surface-container-deep border border-border-subtle space-y-2">
      <div class="flex items-center justify-between text-xs font-mono">
        <span class="text-muted-light flex items-center gap-1.5">
          <span class="material-symbols-outlined text-sm text-primary">groups</span>
          Aforo Asignado Total:
          <strong :class="isCapacityExceeded ? 'text-error' : 'text-high-emphasis'">
            {{ wizardStore.totalCapacity.toLocaleString() }}
          </strong>
          / {{ venueMaxCapacity.toLocaleString() }}
        </span>
        <span
          :class="[
            'text-[11px] font-bold',
            isCapacityExceeded ? 'text-error animate-pulse' : 'text-secondary'
          ]"
        >
          {{ capacityPercentage }}% utilizado
        </span>
      </div>

      <div class="w-full h-2.5 bg-surface-card rounded-full overflow-hidden relative">
        <div
          :class="[
            'h-full transition-all duration-300 rounded-full',
            isCapacityExceeded ? 'bg-error' : 'bg-gradient-to-r from-primary to-secondary'
          ]"
          :style="{ width: `${Math.min(capacityPercentage, 100)}%` }"
        ></div>
      </div>

      <div v-if="isCapacityExceeded" class="text-xs text-error font-mono flex items-center gap-1.5 pt-1">
        <span class="material-symbols-outlined text-sm">error</span>
        ¡Alerta! El aforo de los sectores sobrepasa la capacidad legal máxima permitida para este recinto.
      </div>
    </div>

    <!-- Sectors List Table / Cards -->
    <div class="space-y-3">
      <div
        v-for="(sector, idx) in wizardStore.sectors"
        :key="idx"
        class="p-4 rounded-2xl bg-surface-card border border-border-subtle space-y-3 relative group"
      >
        <div class="flex items-center justify-between">
          <div class="flex items-center gap-2">
            <span class="w-6 h-6 rounded-lg bg-surface-elevated text-primary font-mono text-xs flex items-center justify-center font-bold">
              {{ idx + 1 }}
            </span>
            <input
              v-model="sector.name"
              type="text"
              placeholder="Nombre del sector (ej: Campo VIP)"
              class="bg-transparent border-b border-border-subtle focus:border-primary text-sm font-bold text-high-emphasis px-1 py-0.5 focus:outline-none transition min-w-[200px]"
            />
          </div>

          <button
            v-if="wizardStore.sectors.length > 1"
            type="button"
            @click="wizardStore.removeSector(idx)"
            class="text-muted hover:text-error transition p-1"
            title="Eliminar sector"
          >
            <span class="material-symbols-outlined text-base">delete</span>
          </button>
        </div>

        <div class="grid grid-cols-2 md:grid-cols-5 gap-3 pt-2">
          <!-- Type -->
          <div>
            <label class="block text-[10px] font-mono uppercase text-muted mb-1">Tipo de Admisión</label>
            <select
              v-model="sector.type"
              class="w-full bg-surface-container-deep border border-border-subtle rounded-xl px-2.5 py-1.5 text-xs text-high-emphasis focus:outline-none focus:border-primary"
            >
              <option value="GENERAL_ADMISSION">Campo / General</option>
              <option value="SEATED">Numerada / Asientos</option>
            </select>
          </div>

          <!-- Total Capacity -->
          <div>
            <label class="block text-[10px] font-mono uppercase text-muted mb-1">Capacidad (Lugares)</label>
            <input
              v-model.number="sector.totalCapacity"
              type="number"
              min="1"
              class="w-full bg-surface-container-deep border border-border-subtle rounded-xl px-2.5 py-1.5 text-xs font-mono text-high-emphasis focus:outline-none focus:border-primary"
            />
          </div>

          <!-- Base Price -->
          <div>
            <label class="block text-[10px] font-mono uppercase text-muted mb-1">Precio Base</label>
            <input
              v-model.number="sector.basePrice"
              type="number"
              min="0"
              step="500"
              class="w-full bg-surface-container-deep border border-border-subtle rounded-xl px-2.5 py-1.5 text-xs font-mono text-high-emphasis focus:outline-none focus:border-primary"
            />
          </div>

          <!-- Service Fee -->
          <div>
            <label class="block text-[10px] font-mono uppercase text-muted mb-1">Service Fee</label>
            <input
              v-model.number="sector.serviceFee"
              type="number"
              min="0"
              step="100"
              class="w-full bg-surface-container-deep border border-border-subtle rounded-xl px-2.5 py-1.5 text-xs font-mono text-high-emphasis focus:outline-none focus:border-primary"
            />
          </div>

          <!-- Currency -->
          <div>
            <label class="block text-[10px] font-mono uppercase text-muted mb-1">Moneda</label>
            <select
              v-model="sector.currency"
              class="w-full bg-surface-container-deep border border-border-subtle rounded-xl px-2.5 py-1.5 text-xs font-mono text-high-emphasis focus:outline-none focus:border-primary"
            >
              <option value="ARS">ARS ($)</option>
              <option value="USD">USD (U$S)</option>
            </select>
          </div>
        </div>
      </div>
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

const venueMaxCapacity = computed(() => {
  return selectedVenue.value ? resolveVenueCapacity(selectedVenue.value) : 15000
})

const capacityPercentage = computed(() => {
  if (!venueMaxCapacity.value) return 0
  return Math.round((wizardStore.totalCapacity / venueMaxCapacity.value) * 100)
})

const isCapacityExceeded = computed(() => {
  return wizardStore.totalCapacity > venueMaxCapacity.value
})

function addNewSector() {
  wizardStore.addSector({
    name: `Nuevo Sector ${wizardStore.sectors.length + 1}`,
    type: 'GENERAL_ADMISSION',
    totalCapacity: 1000,
    basePrice: 50000,
    serviceFee: 5000,
    currency: 'ARS',
  })
}
</script>
