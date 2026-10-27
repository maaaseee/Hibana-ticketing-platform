<script setup lang="ts">
export interface MonthOption {
  label: string
  value: string
}

export interface VenueOption {
  label: string
  value: string
}

defineProps<{
  searchName: string
  selectedMonth: string
  selectedVenue: string
  availableMonths: MonthOption[]
  venueOptions: VenueOption[]
}>()

const emit = defineEmits<{
  (e: 'update:searchName', val: string): void
  (e: 'update:selectedMonth', val: string): void
  (e: 'update:selectedVenue', val: string): void
  (e: 'input', event: Event): void
  (e: 'clearSearch'): void
}>()
</script>

<template>
  <div class="w-full max-w-4xl flex flex-col md:flex-row items-center justify-center gap-3">
    <!-- 1. Buscador por Nombre -->
    <div class="relative w-full md:flex-1">
      <input
        :value="searchName"
        @input="emit('input', $event)"
        type="text"
        placeholder="Buscar por show o artista..."
        class="w-full bg-surface-card/90 backdrop-blur-md border border-border-subtle text-high-emphasis placeholder-muted rounded-xl py-2.5 pl-10 pr-9 font-mono text-xs focus:border-primary focus:ring-1 focus:ring-primary outline-none transition-all shadow-inner"
      />
      <span class="material-symbols-outlined absolute left-3 top-2.5 text-muted-light text-[18px] pointer-events-none">
        search
      </span>
      <button
        v-if="searchName"
        @click="emit('clearSearch')"
        class="absolute right-3 top-2.5 text-muted-light hover:text-white transition-colors cursor-pointer"
        title="Limpiar búsqueda"
      >
        <span class="material-symbols-outlined text-[18px]">close</span>
      </button>
    </div>

    <!-- 2. Filtro por Fecha (Solo por Meses del Año) -->
    <div class="relative w-full md:w-64">
      <select
        :value="selectedMonth"
        @change="emit('update:selectedMonth', ($event.target as HTMLSelectElement).value)"
        class="w-full bg-surface-card/90 backdrop-blur-md border border-border-subtle text-high-emphasis rounded-xl py-2.5 pl-10 pr-8 font-mono text-xs focus:border-primary focus:ring-1 focus:ring-primary outline-none transition-all cursor-pointer appearance-none shadow-inner"
      >
        <option value="all">Todos los meses</option>
        <option v-for="month in availableMonths" :key="month.value" :value="month.value">
          {{ month.label }}
        </option>
      </select>
      <span class="material-symbols-outlined absolute left-3 top-2.5 text-muted-light text-[18px] pointer-events-none">
        calendar_month
      </span>
      <span class="material-symbols-outlined absolute right-2.5 top-2.5 text-muted-light text-[18px] pointer-events-none">
        arrow_drop_down
      </span>
    </div>

    <!-- 3. Filtro por Estadios que maneja la ticketera -->
    <div class="relative w-full md:w-64">
      <select
        :value="selectedVenue"
        @change="emit('update:selectedVenue', ($event.target as HTMLSelectElement).value)"
        class="w-full bg-surface-card/90 backdrop-blur-md border border-border-subtle text-high-emphasis rounded-xl py-2.5 pl-10 pr-8 font-mono text-xs focus:border-primary focus:ring-1 focus:ring-primary outline-none transition-all cursor-pointer appearance-none shadow-inner"
      >
        <option v-for="venue in venueOptions" :key="venue.value" :value="venue.value">
          {{ venue.label }}
        </option>
      </select>
      <span class="material-symbols-outlined absolute left-3 top-2.5 text-muted-light text-[18px] pointer-events-none">
        stadium
      </span>
      <span class="material-symbols-outlined absolute right-2.5 top-2.5 text-muted-light text-[18px] pointer-events-none">
        arrow_drop_down
      </span>
    </div>
  </div>
</template>
