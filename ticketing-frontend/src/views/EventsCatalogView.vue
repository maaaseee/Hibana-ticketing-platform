<script setup lang="ts">
import { ref, computed, watch, onMounted, onUnmounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useCatalogStore, getMinEventPrice } from '../stores/catalog'
import type { EventItem } from '../types'
import EventCard from '../components/ui/EventCard.vue'

const catalog = useCatalogStore()
const router = useRouter()
const route = useRoute()

// 1. Search state (raw input + 300ms debounced value)
const searchName = ref('')
const debouncedQuery = ref('')
let debounceTimer: ReturnType<typeof setTimeout> | null = null

// 2. Month filter state ('all' or 'Octubre 2026', 'Noviembre 2026', etc.)
const selectedMonth = ref<string>('all')

// 3. Venue filter state ('all' or canonical venue name)
const selectedVenue = ref<string>('all')

// Spanish month names for formatting & dateIso extraction
const SPANISH_MONTHS = [
  'Enero', 'Febrero', 'Marzo', 'Abril', 'Mayo', 'Junio',
  'Julio', 'Agosto', 'Septiembre', 'Octubre', 'Noviembre', 'Diciembre'
]

// Canonical Buenos Aires venues filter options
const venueOptions = [
  { label: 'Todos los recintos', value: 'all' },
  { label: 'Estadio Monumental (Buenos Aires)', value: 'Estadio Monumental (Buenos Aires)' },
  { label: 'Arena Buenos Aires (Buenos Aires)', value: 'Arena Buenos Aires (Buenos Aires)' },
  { label: 'Teatro Rex (Buenos Aires)', value: 'Teatro Rex (Buenos Aires)' }
]

// Dynamic list of upcoming months up to 8 months into future (Octubre 2026 to Mayo 2027)
const availableMonths = computed(() => {
  const baseYear = 2026
  const baseMonth = 9 // October (0-indexed)
  const list: { label: string; value: string }[] = []

  for (let i = 0; i < 8; i++) {
    const totalMonths = baseMonth + i
    const year = baseYear + Math.floor(totalMonths / 12)
    const monthIndex = totalMonths % 12
    const monthName = SPANISH_MONTHS[monthIndex]
    const label = `${monthName} ${year}`
    list.push({ label, value: label })
  }

  return list
})

// Debounced search handler (300ms)
function handleSearchInput(e: Event) {
  const target = e.target as HTMLInputElement
  searchName.value = target.value
  if (debounceTimer) clearTimeout(debounceTimer)
  debounceTimer = setTimeout(() => {
    debouncedQuery.value = searchName.value.trim()
    updateRouteQuery()
  }, 300)
}

function clearSearch() {
  if (debounceTimer) clearTimeout(debounceTimer)
  searchName.value = ''
  debouncedQuery.value = ''
  updateRouteQuery()
}

// Two-way URL synchronization
function syncFromRoute() {
  const q = typeof route.query.q === 'string' ? route.query.q : ''
  const month = typeof route.query.month === 'string' ? route.query.month : 'all'
  const venue = typeof route.query.venue === 'string' ? route.query.venue : 'all'

  if (searchName.value !== q) searchName.value = q
  if (debouncedQuery.value !== q) debouncedQuery.value = q
  if (selectedMonth.value !== month) selectedMonth.value = month
  if (selectedVenue.value !== venue) selectedVenue.value = venue
}

function updateRouteQuery() {
  const query: Record<string, string> = {}
  if (debouncedQuery.value) {
    query.q = debouncedQuery.value
  }
  if (selectedMonth.value && selectedMonth.value !== 'all') {
    query.month = selectedMonth.value
  }
  if (selectedVenue.value && selectedVenue.value !== 'all') {
    query.venue = selectedVenue.value
  }

  const currentQ = typeof route.query.q === 'string' ? route.query.q : ''
  const currentMonth = typeof route.query.month === 'string' ? route.query.month : 'all'
  const currentVenue = typeof route.query.venue === 'string' ? route.query.venue : 'all'

  const targetQ = query.q || ''
  const targetMonth = query.month || 'all'
  const targetVenue = query.venue || 'all'

  if (currentQ !== targetQ || currentMonth !== targetMonth || currentVenue !== targetVenue) {
    router.replace({ query })
  }
}

// Watch dropdown changes to synchronize to URL query
watch([selectedMonth, selectedVenue], () => {
  updateRouteQuery()
})

// Watch route.query changes (e.g. back/forward navigation or external links)
watch(
  () => route.query,
  () => {
    syncFromRoute()
  },
  { deep: true }
)

onMounted(() => {
  syncFromRoute()
})

onUnmounted(() => {
  if (debounceTimer) {
    clearTimeout(debounceTimer)
  }
})

// Filtered events: starts from upcomingEvents (chronologically sorted)
const filteredEvents = computed(() => {
  return catalog.upcomingEvents.filter((event: EventItem) => {
    // 1. Text Search matching title, subtitle, lineup (or artist)
    const query = debouncedQuery.value.toLowerCase().trim()
    if (query) {
      const titleMatch = event.title.toLowerCase().includes(query)
      const subtitleMatch = event.subtitle ? event.subtitle.toLowerCase().includes(query) : false
      const lineupMatch = event.lineup ? event.lineup.toLowerCase().includes(query) : false
      if (!titleMatch && !subtitleMatch && !lineupMatch) {
        return false
      }
    }

    // 2. Month Filter matching event.dateIso (or formatted event.date)
    if (selectedMonth.value !== 'all') {
      let matchesMonth = false
      if (event.dateIso) {
        const d = new Date(event.dateIso)
        if (!isNaN(d.getTime())) {
          const eventMonthName = SPANISH_MONTHS[d.getMonth()]
          const eventYear = d.getFullYear()
          const eventMonthLabel = `${eventMonthName} ${eventYear}`
          if (eventMonthLabel.toLowerCase() === selectedMonth.value.toLowerCase()) {
            matchesMonth = true
          }
        }
      }
      if (!matchesMonth && event.date) {
        // Fallback for "15 de Octubre de 2026"
        const parts = event.date.split(' de ')
        if (parts.length >= 3 && parts[1] && parts[2]) {
          const fallbackMonthLabel = `${parts[1]} ${parts[2]}`.toLowerCase()
          if (fallbackMonthLabel === selectedMonth.value.toLowerCase()) {
            matchesMonth = true
          }
        }
      }
      if (!matchesMonth) {
        return false
      }
    }

    // 3. Venue Filter comparing event.venue === selectedVenue
    if (selectedVenue.value !== 'all') {
      if (event.venue.toLowerCase() !== selectedVenue.value.toLowerCase()) {
        return false
      }
    }

    return true
  })
})

function goToEvent(id: string) {
  router.push({ name: 'event-detail', params: { id } })
}

function resetFilters() {
  if (debounceTimer) {
    clearTimeout(debounceTimer)
  }
  searchName.value = ''
  debouncedQuery.value = ''
  selectedMonth.value = 'all'
  selectedVenue.value = 'all'
  updateRouteQuery()
}
</script>

<template>
  <div class="flex flex-col w-full min-h-[calc(100vh-64px)] bg-surface text-high-emphasis pb-24 relative overflow-hidden">
    <!-- Ambient Topography Background: Top side of the shows list, slowly vanishing downwards (+300px extended, softened opacity) -->
    <div
      class="shows-topography-wrapper pointer-events-none absolute inset-x-0 top-0 h-[940px] overflow-hidden z-0"
      aria-hidden="true"
    >
      <!-- Base Topographic Elevation Map with Progressive Downward Mask & Subdued Opacity -->
      <div class="w-full h-full bg-topography topography-hero-mask opacity-65"></div>

      <!-- Radial Atmospheric Glow (Subtle Electric Cyan accent on title area) -->
      <div class="absolute inset-0 bg-[radial-gradient(ellipse_75%_50%_at_50%_15%,rgba(0,207,243,0.08),transparent_70%)] pointer-events-none"></div>

      <!-- Seamless Downward Vanish Gradient Overlay into Deep Surface Canvas -->
      <div class="absolute inset-0 bg-gradient-to-b from-transparent via-surface/50 to-surface pointer-events-none"></div>
    </div>

    <!-- Header Hero con Skyvia Font y Controles de Filtrado (Sin barra/border separador) -->
    <header class="relative z-10 w-full overflow-hidden pt-14 pb-8 md:pt-16 md:pb-10">
      <div class="relative z-10 w-full max-w-[80vw] mx-auto px-4 sm:px-6 lg:px-8 flex flex-col items-center text-center gap-7">
        <!-- Título con Skyvia Demo Bold -->
        <h1 class="font-skyvia text-3xl sm:text-4xl md:text-5xl lg:text-6xl font-bold text-high-emphasis tracking-tight drop-shadow-lg">
          Cartelera de Shows
        </h1>

        <!-- Controles de Filtrado Centrados (Nombre, Fecha por Meses, y Estadios) -->
        <div class="w-full max-w-4xl flex flex-col md:flex-row items-center justify-center gap-3">
          <!-- 1. Buscador por Nombre -->
          <div class="relative w-full md:flex-1">
            <input
              :value="searchName"
              @input="handleSearchInput"
              type="text"
              placeholder="Buscar por show o artista..."
              class="w-full bg-surface-card/90 backdrop-blur-md border border-border-subtle text-high-emphasis placeholder-muted rounded-xl py-2.5 pl-10 pr-9 font-mono text-xs focus:border-primary focus:ring-1 focus:ring-primary outline-none transition-all shadow-inner"
            />
            <span class="material-symbols-outlined absolute left-3 top-2.5 text-muted-light text-[18px] pointer-events-none">
              search
            </span>
            <button
              v-if="searchName"
              @click="clearSearch"
              class="absolute right-3 top-2.5 text-muted-light hover:text-white transition-colors cursor-pointer"
              title="Limpiar búsqueda"
            >
              <span class="material-symbols-outlined text-[18px]">close</span>
            </button>
          </div>

          <!-- 2. Filtro por Fecha (Solo por Meses del Año) -->
          <div class="relative w-full md:w-64">
            <select
              v-model="selectedMonth"
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
              v-model="selectedVenue"
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
      </div>
    </header>

    <!-- Listado Simple de Eventos (Ordenados por proximidad a su realización) -->
    <main class="relative z-10 w-full max-w-[80vw] mx-auto px-4 sm:px-6 lg:px-8 pt-10">
      <div
        v-if="filteredEvents.length > 0"
        class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6"
      >
        <EventCard
          v-for="event in filteredEvents"
          :key="event.id"
          :event="event"
          accent-color="primary"
          show-venue-icon="stadium"
          @click="goToEvent"
        />
      </div>

      <!-- Estado Vacío -->
      <div
        v-else
        class="bg-surface-card/50 border border-border-subtle rounded-2xl p-10 flex flex-col items-center justify-center text-center gap-3 my-8"
      >
        <span class="material-symbols-outlined text-4xl text-muted-light">search_off</span>
        <h4 class="font-display text-lg font-bold text-high-emphasis">No se encontraron espectáculos</h4>
        <p class="font-body text-sm text-muted-light max-w-md">
          No hay eventos que coincidan con los filtros seleccionados. Prueba con otros criterios o limpia la búsqueda.
        </p>
        <button
          @click="resetFilters"
          class="mt-2 bg-surface-interactive hover:bg-border-subtle text-primary font-mono text-xs px-4 py-2 rounded-lg border border-border-subtle transition-colors cursor-pointer"
        >
          Limpiar filtros
        </button>
      </div>
    </main>
  </div>
</template>
