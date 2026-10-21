<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useCatalogStore } from '../stores/catalog'
import type { EventItem } from '../types'

const catalog = useCatalogStore()
const router = useRouter()
const route = useRoute()

// 1. Buscador por nombre
const searchName = ref('')
// 2. Filtro por fecha (solo meses del año)
const selectedMonth = ref<string>('all')
// 3. Filtro por estadios / recintos
const selectedVenue = ref<string>('all')

onMounted(() => {
  if (route.query.q && typeof route.query.q === 'string') {
    searchName.value = route.query.q
  }
})

// Meses disponibles extraídos de las fechas de los eventos ("Mes Año", ej. "Octubre 2026")
const availableMonths = computed(() => {
  const monthsSet = new Set<string>()
  catalog.events.forEach((event: EventItem) => {
    const parts = event.date.split(' de ')
    if (parts.length >= 3) {
      monthsSet.add(`${parts[1]} ${parts[2]}`)
    }
  })
  return Array.from(monthsSet)
})

// Estadios y recintos que maneja la ticketera
const availableVenues = computed(() => {
  const venuesSet = new Set<string>()
  catalog.events.forEach((event: EventItem) => {
    if (event.venue) {
      venuesSet.add(event.venue)
    }
  })
  return Array.from(venuesSet).sort()
})

// Listado simple: ordenado inicialmente por proximidad a su realización (upcomingEvents)
// y filtrado reactivamente por nombre, mes y estadio
const filteredEvents = computed(() => {
  return catalog.upcomingEvents.filter((event: EventItem) => {
    // 1. Filtro por nombre / artista
    const query = searchName.value.trim().toLowerCase()
    if (query) {
      const titleMatch = event.title.toLowerCase().includes(query)
      const subtitleMatch = event.subtitle?.toLowerCase().includes(query) || false
      const lineupMatch = event.lineup?.toLowerCase().includes(query) || false
      if (!titleMatch && !subtitleMatch && !lineupMatch) {
        return false
      }
    }

    // 2. Filtro por fecha (meses del año)
    if (selectedMonth.value !== 'all') {
      const parts = event.date.split(' de ')
      if (parts.length >= 3) {
        const eventMonthYear = `${parts[1]} ${parts[2]}`.toLowerCase()
        if (eventMonthYear !== selectedMonth.value.toLowerCase()) {
          return false
        }
      } else if (!event.date.toLowerCase().includes(selectedMonth.value.toLowerCase())) {
        return false
      }
    }

    // 3. Filtro por estadios
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
  searchName.value = ''
  selectedMonth.value = 'all'
  selectedVenue.value = 'all'
}
</script>

<template>
  <div class="flex flex-col w-full min-h-[calc(100vh-64px)] bg-[#0c141f] text-[#f1f5f9] pb-24">
    <!-- Header Hero con Fondo de Recital, Efecto de Carrusel para Bajar Intensidad e Ink Font (Neuegrid) -->
    <header class="relative w-full overflow-hidden bg-[#0c141f] border-b border-[#23354d] pt-14 pb-14 md:pt-16 md:pb-16 group">
      <!-- Imagen de Fondo de un Recital en Vivo -->
      <div
        class="absolute inset-0 w-full h-full bg-cover bg-center transition-transform duration-1000 ease-out group-hover:scale-105"
        style="background-image: url('https://images.unsplash.com/photo-1470225620780-dba8ba36b745?auto=format&fit=crop&w=2000&q=80');"
      ></div>

      <!-- Mismo efecto del carrusel para bajar la intensidad de la imagen (Gradiente y Overlay Oscuro) -->
      <div class="absolute inset-0 bg-gradient-to-t from-[#0c141f] via-[#0c141f]/80 to-[#0c141f]/40 backdrop-blur-[0.5px]"></div>

      <div class="relative z-10 w-full max-w-[80vw] mx-auto px-4 sm:px-6 lg:px-8 flex flex-col items-center text-center gap-7">
        <!-- Título con Ink Font (Neuegrid / Bricolage Grotesque) -->
        <h1 class="font-ink text-3xl sm:text-4xl md:text-5xl lg:text-6xl font-extrabold text-[#f1f5f9] tracking-tight drop-shadow-lg">
          Cartelera de Shows
        </h1>

        <!-- Controles de Filtrado Centrados (Nombre, Fecha por Meses, y Estadios) -->
        <div class="w-full max-w-4xl flex flex-col md:flex-row items-center justify-center gap-3">
          <!-- 1. Buscador por Nombre -->
          <div class="relative w-full md:flex-1">
            <input
              v-model="searchName"
              type="text"
              placeholder="Buscar por show o artista..."
              class="w-full bg-[#141e2e]/90 backdrop-blur-md border border-[#23354d] text-[#f1f5f9] placeholder-[#64748b] rounded-xl py-2.5 pl-10 pr-9 font-mono text-xs focus:border-[#86cffb] focus:ring-1 focus:ring-[#86cffb] outline-none transition-all shadow-inner"
            />
            <span class="material-symbols-outlined absolute left-3 top-2.5 text-[#94a3b8] text-[18px] pointer-events-none">
              search
            </span>
            <button
              v-if="searchName"
              @click="searchName = ''"
              class="absolute right-3 top-2.5 text-[#94a3b8] hover:text-white transition-colors cursor-pointer"
              title="Limpiar búsqueda"
            >
              <span class="material-symbols-outlined text-[18px]">close</span>
            </button>
          </div>

          <!-- 2. Filtro por Fecha (Solo por Meses del Año) -->
          <div class="relative w-full md:w-64">
            <select
              v-model="selectedMonth"
              class="w-full bg-[#141e2e]/90 backdrop-blur-md border border-[#23354d] text-[#f1f5f9] rounded-xl py-2.5 pl-10 pr-8 font-mono text-xs focus:border-[#86cffb] focus:ring-1 focus:ring-[#86cffb] outline-none transition-all cursor-pointer appearance-none shadow-inner"
            >
              <option value="all">Todos los meses</option>
              <option v-for="month in availableMonths" :key="month" :value="month">
                {{ month }}
              </option>
            </select>
            <span class="material-symbols-outlined absolute left-3 top-2.5 text-[#94a3b8] text-[18px] pointer-events-none">
              calendar_month
            </span>
            <span class="material-symbols-outlined absolute right-2.5 top-2.5 text-[#94a3b8] text-[18px] pointer-events-none">
              arrow_drop_down
            </span>
          </div>

          <!-- 3. Filtro por Estadios que maneja la ticketera -->
          <div class="relative w-full md:w-64">
            <select
              v-model="selectedVenue"
              class="w-full bg-[#141e2e]/90 backdrop-blur-md border border-[#23354d] text-[#f1f5f9] rounded-xl py-2.5 pl-10 pr-8 font-mono text-xs focus:border-[#86cffb] focus:ring-1 focus:ring-[#86cffb] outline-none transition-all cursor-pointer appearance-none shadow-inner"
            >
              <option value="all">Todos los estadios</option>
              <option v-for="venue in availableVenues" :key="venue" :value="venue">
                {{ venue }}
              </option>
            </select>
            <span class="material-symbols-outlined absolute left-3 top-2.5 text-[#94a3b8] text-[18px] pointer-events-none">
              stadium
            </span>
            <span class="material-symbols-outlined absolute right-2.5 top-2.5 text-[#94a3b8] text-[18px] pointer-events-none">
              arrow_drop_down
            </span>
          </div>
        </div>
      </div>
    </header>

    <!-- Listado Simple de Eventos (Ordenados por proximidad a su realización) -->
    <main class="w-full max-w-[80vw] mx-auto px-4 sm:px-6 lg:px-8 pt-10">
      <div
        v-if="filteredEvents.length > 0"
        class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6"
      >
        <article
          v-for="event in filteredEvents"
          :key="event.id"
          @click="goToEvent(event.id)"
          class="bg-[#141e2e] rounded-2xl border border-[#23354d] shadow-md overflow-hidden flex flex-col group hover:-translate-y-1 hover:border-[#86cffb]/60 transition-all duration-300 relative cursor-pointer"
        >
          <!-- Badge -->
          <div
            v-if="event.badgeText"
            class="absolute top-3 right-3 z-10 font-mono text-[10px] px-2.5 py-0.5 rounded-md uppercase tracking-wider font-semibold border border-[#23354d]"
            :class="event.badgeText === 'Confirmado' ? 'bg-[#162438]/90 text-[#4fdbc8] backdrop-blur-sm' : 'bg-[#00688f] text-white'"
          >
            {{ event.badgeText }}
          </div>

          <!-- Portada -->
          <div class="w-full h-44 bg-[#1a293e] overflow-hidden">
            <img
              :src="event.imageUrl"
              :alt="event.title"
              class="w-full h-full object-cover group-hover:scale-105 transition-transform duration-500"
              loading="lazy"
            />
          </div>

          <!-- Información del Show -->
          <div class="p-4 flex flex-col flex-1 gap-2">
            <div class="flex items-center justify-between gap-2">
              <span class="font-mono text-xs text-[#86cffb] font-semibold">
                {{ event.date }}
              </span>
              <span class="font-mono text-xs text-[#cbd5e1] font-semibold">
                Desde ${{ event.price }}
              </span>
            </div>

            <h3 class="font-ink text-base text-[#f1f5f9] line-clamp-2 leading-tight group-hover:text-[#86cffb] transition-colors font-bold">
              {{ event.title }}
            </h3>

            <p class="font-body text-xs text-[#94a3b8] line-clamp-2 leading-relaxed">
              {{ event.subtitle }}
            </p>

            <div class="flex items-center justify-between pt-3 mt-auto border-t border-[#23354d]/50 font-body text-xs text-[#94a3b8]">
              <span class="flex items-center gap-1 truncate">
                <span class="material-symbols-outlined text-[15px] text-[#86cffb]">stadium</span>
                <span class="truncate">{{ event.venue }}</span>
              </span>
              <span class="font-mono text-[#86cffb] group-hover:translate-x-0.5 transition-transform font-medium flex items-center gap-0.5 shrink-0">
                Ver
                <span class="material-symbols-outlined text-[15px]">arrow_forward</span>
              </span>
            </div>
          </div>
        </article>
      </div>

      <!-- Estado Vacío -->
      <div
        v-else
        class="bg-[#141e2e]/50 border border-[#23354d] rounded-2xl p-10 flex flex-col items-center justify-center text-center gap-3 my-8"
      >
        <span class="material-symbols-outlined text-4xl text-[#94a3b8]">search_off</span>
        <h4 class="font-ink text-lg font-bold text-[#f1f5f9]">No se encontraron espectáculos</h4>
        <p class="font-body text-sm text-[#94a3b8] max-w-md">
          No hay eventos que coincidan con los filtros seleccionados. Prueba con otros criterios o limpia la búsqueda.
        </p>
        <button
          @click="resetFilters"
          class="mt-2 bg-[#1a293e] hover:bg-[#23354d] text-[#86cffb] font-mono text-xs px-4 py-2 rounded-lg border border-[#23354d] transition-colors cursor-pointer"
        >
          Limpiar filtros
        </button>
      </div>
    </main>
  </div>
</template>
