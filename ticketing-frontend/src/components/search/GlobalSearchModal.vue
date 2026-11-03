<script setup lang="ts">
import { ref, watch, onMounted, onUnmounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import {
  DialogRoot,
  DialogPortal,
  DialogOverlay,
  DialogContent,
  DialogTitle,
  DialogDescription
} from 'radix-vue'
import { catalogService } from '../../services/catalogService'
import type { EventItem } from '../../types'

const props = defineProps<{
  open: boolean
}>()

const emit = defineEmits<{
  (e: 'update:open', val: boolean): void
}>()

const router = useRouter()
const searchInput = ref('')
const searchResults = ref<EventItem[]>([])
const isLoading = ref(false)
const inputRef = ref<HTMLInputElement | null>(null)
let debounceTimer: ReturnType<typeof setTimeout> | null = null

function formatMoney(amount: number) {
  return new Intl.NumberFormat('es-AR', {
    style: 'currency',
    currency: 'ARS',
    maximumFractionDigits: 0
  }).format(amount)
}

async function executeSearch(query: string) {
  if (!query.trim()) {
    searchResults.value = []
    isLoading.value = false
    return
  }

  isLoading.value = true
  try {
    const results = await catalogService.searchEvents(query)
    searchResults.value = results
  } catch {
    searchResults.value = []
  } finally {
    isLoading.value = false
  }
}

watch(searchInput, (newVal) => {
  if (debounceTimer) clearTimeout(debounceTimer)
  debounceTimer = setTimeout(() => {
    executeSearch(newVal)
  }, 220)
})

watch(
  () => props.open,
  (isOpen) => {
    if (isOpen) {
      searchInput.value = ''
      searchResults.value = []
      nextTick(() => {
        inputRef.value?.focus()
      })
    }
  }
)

function handleSelectEvent(event: EventItem) {
  emit('update:open', false)
  router.push({ name: 'event-detail', params: { id: event.slug || event.id } })
}

function handleKeyDown(e: KeyboardEvent) {
  if ((e.metaKey || e.ctrlKey) && e.key.toLowerCase() === 'k') {
    e.preventDefault()
    emit('update:open', !props.open)
  }
}

onMounted(() => {
  if (typeof window !== 'undefined') {
    window.addEventListener('keydown', handleKeyDown)
  }
})

onUnmounted(() => {
  if (typeof window !== 'undefined') {
    window.removeEventListener('keydown', handleKeyDown)
  }
  if (debounceTimer) clearTimeout(debounceTimer)
})
</script>

<template>
  <DialogRoot :open="open" @update:open="emit('update:open', $event)">
    <DialogPortal>
      <DialogOverlay class="fixed inset-0 z-50 bg-black/80 backdrop-blur-md transition-opacity" />
      <DialogContent
        class="fixed top-[15%] left-1/2 -translate-x-1/2 z-50 w-full max-w-2xl bg-[#0c1524]/95 border border-[#22354e] rounded-3xl p-5 shadow-[0_25px_70px_rgba(0,0,0,0.85)] text-white outline-none"
      >
        <DialogTitle class="sr-only">Buscador global de espectáculos y recintos</DialogTitle>
        <DialogDescription class="sr-only">Escribe el nombre de tu artista, festival o estadio favorito</DialogDescription>

        <!-- Search Input Bar -->
        <div class="relative flex items-center border-b border-[#22354e] pb-3 mb-3">
          <span class="material-symbols-outlined text-[24px] text-[#38bdf8] mr-3">search</span>
          <input
            ref="inputRef"
            v-model="searchInput"
            type="text"
            placeholder="Buscar por artista, show, festival o estadio..."
            class="w-full bg-transparent text-base sm:text-lg font-body text-white placeholder-[#64748b] outline-none"
            aria-label="Buscar shows"
          />
          <button
            v-if="searchInput"
            type="button"
            @click="searchInput = ''"
            class="text-[#64748b] hover:text-white p-1 cursor-pointer"
            aria-label="Limpiar búsqueda"
          >
            <span class="material-symbols-outlined text-[18px]">close</span>
          </button>
          <kbd class="hidden sm:inline-block ml-2 px-2 py-0.5 text-[10px] font-mono text-[#64748b] bg-black/40 border border-[#22354e] rounded">
            ESC
          </kbd>
        </div>

        <!-- Results / Loading / Empty State Area -->
        <div class="max-h-96 overflow-y-auto space-y-2 pr-1 custom-scrollbar">
          <!-- Loading Spinner -->
          <div v-if="isLoading" class="py-8 flex flex-col items-center justify-center text-[#38bdf8] gap-2">
            <span class="material-symbols-outlined text-3xl animate-spin">progress_activity</span>
            <span class="text-xs font-mono text-[#94a3b8]">Buscando en la cartelera...</span>
          </div>

          <!-- Empty query initial hint -->
          <div v-else-if="!searchInput.trim()" class="py-8 text-center text-[#64748b] text-xs font-body">
            <span class="material-symbols-outlined text-3xl mb-1 text-[#38bdf8]/50">stadium</span>
            <p>Escribe al menos una letra para encontrar shows, recitales y estadios en vivo.</p>
          </div>

          <!-- No results found -->
          <div v-else-if="searchResults.length === 0" class="py-8 text-center text-[#94a3b8] text-xs font-body">
            <span class="material-symbols-outlined text-3xl mb-1 text-rose-400">sentiment_dissatisfied</span>
            <p>No encontramos ningún espectáculo con "<span class="text-white font-semibold">{{ searchInput }}</span>".</p>
          </div>

          <!-- Results List -->
          <div
            v-for="event in searchResults"
            :key="event.id"
            @click="handleSelectEvent(event)"
            class="flex items-center justify-between p-3 rounded-2xl bg-[#141f30]/60 hover:bg-[#1c2c44] border border-transparent hover:border-[#38bdf8]/40 transition-all cursor-pointer group"
          >
            <div class="flex items-center gap-3 min-w-0">
              <img
                :src="event.imageUrl"
                :alt="event.title"
                class="w-12 h-12 rounded-xl object-cover shrink-0 border border-white/10"
              />
              <div class="min-w-0">
                <h4 class="font-display font-bold text-sm text-white group-hover:text-[#38bdf8] transition-colors truncate">
                  {{ event.title }}
                </h4>
                <p class="font-body text-xs text-[#94a3b8] truncate flex items-center gap-2 mt-0.5">
                  <span>{{ event.date }}</span>
                  <span class="text-[#475569]">•</span>
                  <span>{{ event.venue }}</span>
                </p>
              </div>
            </div>

            <!-- Price & Action -->
            <div class="flex items-center gap-3 shrink-0 pl-3">
              <div class="text-right">
                <span class="block text-[10px] font-mono uppercase text-[#64748b]">Desde</span>
                <span class="font-mono font-bold text-xs sm:text-sm text-emerald-400">
                  {{ formatMoney(event.minPrice || event.price) }}
                </span>
              </div>
              <span class="material-symbols-outlined text-[#64748b] group-hover:text-white group-hover:translate-x-0.5 transition-all text-[20px]">
                arrow_forward
              </span>
            </div>
          </div>
        </div>

        <!-- Footer helper -->
        <div class="mt-4 pt-3 border-t border-[#22354e] flex items-center justify-between text-[11px] font-mono text-[#64748b]">
          <span>Tip: Presiona <kbd class="px-1 py-0.5 bg-black/40 rounded border border-[#22354e]">↑</kbd> <kbd class="px-1 py-0.5 bg-black/40 rounded border border-[#22354e]">↓</kbd> para navegar</span>
          <span class="text-[#38bdf8]">{{ searchResults.length }} resultados</span>
        </div>
      </DialogContent>
    </DialogPortal>
  </DialogRoot>
</template>
