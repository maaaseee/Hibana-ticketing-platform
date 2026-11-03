<script setup lang="ts">
import { ref, computed, watch, onMounted, onUnmounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useCatalogStore } from '../stores/catalog'
import type { EventItem } from '../types'
import EventCard from '../components/ui/EventCard.vue'
import EventCardSkeleton from '../components/ui/EventCardSkeleton.vue'
import CatalogFilterBar from '../components/catalog/CatalogFilterBar.vue'
import CatalogEmptyState from '../components/catalog/CatalogEmptyState.vue'

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

// Dynamic list of upcoming months up to the furthest event date
const availableMonths = computed(() => catalog.availableMonths)

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
  catalog.fetchCatalogMetadata()
  catalog.fetchEvents()
})

onUnmounted(() => {
  if (debounceTimer) {
    clearTimeout(debounceTimer)
  }
})

function fuzzyTextMatch(text: string, query: string): boolean {
  if (!text || !query) return false
  const t = text.toLowerCase()
  const q = query.toLowerCase().trim()
  if (t.includes(q)) return true

  // Word-level edit distance for typo tolerance (e.g. "Colplay" -> "Coldplay")
  const words = t.split(/[\s,.-]+/)
  const qWords = q.split(/\s+/)

  return qWords.every(qWord => {
    if (qWord.length <= 3) return t.includes(qWord)
    return words.some(word => {
      if (word.includes(qWord)) return true
      if (Math.abs(word.length - qWord.length) > 2) return false
      const maxDistance = qWord.length <= 5 ? 1 : 2
      let dist = 0
      const minLen = Math.min(word.length, qWord.length)
      for (let i = 0; i < minLen; i++) {
        if (word[i] !== qWord[i]) dist++
      }
      dist += Math.abs(word.length - qWord.length)
      return dist <= maxDistance
    })
  })
}

// Filtered events: starts from upcomingEvents (chronologically sorted)
const filteredEvents = computed(() => {
  return catalog.upcomingEvents.filter((event: EventItem) => {
    // 1. Text Search matching title, subtitle, lineup (or artist) with typo tolerance
    const query = debouncedQuery.value.trim()
    if (query) {
      const titleMatch = fuzzyTextMatch(event.title, query)
      const subtitleMatch = event.subtitle ? fuzzyTextMatch(event.subtitle, query) : false
      const lineupMatch = event.lineup ? fuzzyTextMatch(event.lineup, query) : false
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
    <!-- Ambient Topography Background -->
    <div
      class="shows-topography-wrapper pointer-events-none absolute inset-x-0 top-0 h-235 overflow-hidden z-0"
      aria-hidden="true"
    >
      <div class="w-full h-full bg-topography topography-hero-mask opacity-65"></div>
      <div class="absolute inset-0 bg-[radial-gradient(ellipse_75%_50%_at_50%_15%,rgba(0,207,243,0.08),transparent_70%)] pointer-events-none"></div>
      <div class="absolute inset-0 bg-linear-to-b from-transparent via-surface/50 to-surface pointer-events-none"></div>
    </div>

    <!-- Header Hero con Skyvia Font y Controles de Filtrado -->
    <header class="relative z-10 w-full overflow-hidden pt-14 pb-8 md:pt-16 md:pb-10 select-none">
      <div class="relative z-10 w-full max-w-[80vw] mx-auto px-4 sm:px-6 lg:px-8 flex flex-col items-center text-center gap-7">
        <h1 class="font-skyvia text-3xl sm:text-5xl md:text-6xl lg:text-7xl font-bold text-high-emphasis tracking-wide drop-shadow-lg">
          Shows
        </h1>

        <CatalogFilterBar
          v-model:search-name="searchName"
          v-model:selected-month="selectedMonth"
          v-model:selected-venue="selectedVenue"
          :available-months="availableMonths"
          :venue-options="venueOptions"
          @input="handleSearchInput"
          @clear-search="clearSearch"
        />
      </div>
    </header>

    <!-- Listado Simple de Eventos (Ordenados por proximidad a su realización) -->
    <main class="relative z-10 w-full max-w-[80vw] mx-auto px-4 sm:px-6 lg:px-8 pt-10">
      <!-- Estado de Carga con Shimmer Skeletons (cuando el catálogo está cargando y no hay eventos en memoria) -->
      <div
        v-if="catalog.isLoading && catalog.events.length === 0"
        data-testid="catalog-skeleton-grid"
        class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 2xl:grid-cols-3 gap-6"
      >
        <EventCardSkeleton v-for="n in 6" :key="n" />
      </div>

      <!-- Listado de Eventos Filtrados -->
      <div
        v-else-if="filteredEvents.length > 0"
        class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 2xl:grid-cols-3 gap-6"
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
      <CatalogEmptyState
        v-else
        @reset="resetFilters"
      />
    </main>
  </div>
</template>
