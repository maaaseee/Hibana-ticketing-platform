<template>
  <div class="space-y-6">
    <!-- Page Header & Main Actions -->
    <div class="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
      <div>
        <h1 class="text-2xl font-display font-black tracking-tight text-high-emphasis flex items-center gap-2.5">
          Directorio de Eventos
          <span class="text-xs font-mono font-normal px-2.5 py-0.5 rounded-full bg-surface-container text-muted-light border border-border-subtle">
            {{ filteredEvents.length }} eventos
          </span>
        </h1>
        <p class="text-xs text-muted-light mt-1">
          Administración de ciclo de vida, apertura de boleterías y cancelaciones oficiales.
        </p>
      </div>

      <div class="flex items-center gap-3">
        <button
          type="button"
          @click="loadEvents"
          class="flex items-center gap-2 px-3 py-2.5 rounded-xl bg-surface-container border border-border-subtle hover:bg-surface-elevated text-xs font-mono text-muted-light hover:text-high-emphasis transition"
        >
          <span class="material-symbols-outlined text-sm" :class="{ 'animate-spin': loading }">refresh</span>
          Actualizar
        </button>

        <RouterLink
          to="/events/new"
          class="flex items-center gap-2 px-4 py-2.5 rounded-xl bg-primary text-on-primary hover:bg-primary-hover font-bold text-xs shadow-md shadow-primary/20 transition"
        >
          <span class="material-symbols-outlined text-sm">add</span>
          Crear Nuevo Evento
        </RouterLink>
      </div>
    </div>

    <!-- Feedback Toast Alert -->
    <div
      v-if="feedbackMessage"
      class="p-4 rounded-2xl bg-secondary/10 border border-secondary/30 text-secondary text-xs font-mono flex items-center justify-between"
    >
      <div class="flex items-center gap-2">
        <span class="material-symbols-outlined text-base">check_circle</span>
        <span>{{ feedbackMessage }}</span>
      </div>
      <button @click="feedbackMessage = null" class="text-secondary/70 hover:text-secondary">
        <span class="material-symbols-outlined text-sm">close</span>
      </button>
    </div>

    <!-- Error Alert -->
    <div
      v-if="errorMessage"
      class="p-4 rounded-2xl bg-error/10 border border-error/30 text-error text-xs font-mono flex items-center justify-between"
    >
      <div class="flex items-center gap-2">
        <span class="material-symbols-outlined text-base">error</span>
        <span>{{ errorMessage }}</span>
      </div>
      <button @click="errorMessage = null" class="text-error/70 hover:text-error">
        <span class="material-symbols-outlined text-sm">close</span>
      </button>
    </div>

    <!-- Filters & Search Toolbar -->
    <div class="flex flex-col md:flex-row items-center justify-between gap-4 p-4 rounded-2xl bg-surface-container border border-border-subtle">
      <!-- Status Tabs -->
      <div class="flex items-center gap-1.5 overflow-x-auto w-full md:w-auto">
        <button
          v-for="tab in statusTabs"
          :key="tab.value"
          type="button"
          @click="activeTab = tab.value"
          :class="[
            'px-3 py-1.5 rounded-xl text-xs font-mono transition uppercase tracking-wider',
            activeTab === tab.value
              ? 'bg-surface-elevated text-primary font-bold border border-primary/30 shadow-sm'
              : 'text-muted-light hover:text-high-emphasis hover:bg-surface-card'
          ]"
        >
          {{ tab.label }}
        </button>
      </div>

      <!-- Search Input -->
      <div class="relative w-full md:w-80">
        <span class="material-symbols-outlined absolute left-3 top-2.5 text-muted text-base">search</span>
        <input
          v-model="searchQuery"
          type="text"
          placeholder="Buscar por título o recinto..."
          class="w-full bg-surface-container-deep border border-border-subtle rounded-xl pl-9 pr-3.5 py-2 text-xs text-high-emphasis placeholder-muted focus:outline-none focus:border-primary transition font-mono"
        />
      </div>
    </div>

    <!-- Events Table Card -->
    <div class="bg-surface-container border border-border-subtle rounded-3xl overflow-hidden shadow-sm">
      <!-- Loading Skeleton -->
      <div v-if="loading" class="p-8 space-y-4">
        <div v-for="i in 4" :key="i" class="h-14 bg-surface-card/60 rounded-xl animate-pulse"></div>
      </div>

      <!-- Empty State -->
      <div v-else-if="filteredEvents.length === 0" class="p-12 text-center">
        <div class="w-14 h-14 rounded-2xl bg-surface-card flex items-center justify-center mx-auto text-muted mb-3">
          <span class="material-symbols-outlined text-2xl">event_busy</span>
        </div>
        <h3 class="font-display font-bold text-high-emphasis text-sm">No se encontraron espectáculos</h3>
        <p class="text-xs text-muted-light mt-1 max-w-sm mx-auto">
          No hay eventos que coincidan con los filtros seleccionados o la búsqueda actual.
        </p>
        <RouterLink
          to="/events/new"
          class="inline-flex items-center gap-2 mt-4 px-4 py-2 rounded-xl bg-primary text-on-primary text-xs font-bold transition hover:bg-primary-hover shadow-sm"
        >
          <span class="material-symbols-outlined text-sm">add</span>
          Crear Primer Evento
        </RouterLink>
      </div>

      <!-- High-Density Table -->
      <div v-else class="overflow-x-auto">
        <table class="w-full text-left border-collapse text-xs">
          <thead>
            <tr class="border-b border-border-subtle bg-surface-container-deep/60 text-muted font-mono uppercase text-[10px] tracking-wider">
              <th class="py-3 px-4">Espectáculo & Título</th>
              <th class="py-3 px-4">Recinto / Ubicación</th>
              <th class="py-3 px-4">Fechas</th>
              <th class="py-3 px-4">Tarifa Base</th>
              <th class="py-3 px-4">Estado</th>
              <th class="py-3 px-4 text-right">Acciones Operativas</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-border-subtle/50">
            <tr
              v-for="event in filteredEvents"
              :key="event.id"
              class="hover:bg-surface-elevated/40 transition group"
            >
              <!-- Event & Thumbnail -->
              <td class="py-3.5 px-4">
                <div class="flex items-center gap-3">
                  <img
                    :src="event.bannerUrl || event.posterUrl || 'https://images.unsplash.com/photo-1470225620780-dba8ba36b745?auto=format&fit=crop&w=150&q=80'"
                    alt="Thumbnail"
                    class="w-12 h-12 rounded-xl object-cover bg-surface-card border border-border-subtle shrink-0"
                  />
                  <div class="min-w-0">
                    <div class="font-display font-bold text-high-emphasis group-hover:text-primary transition line-clamp-1">
                      {{ event.title }}
                    </div>
                    <div class="text-[11px] font-mono text-muted flex items-center gap-2 mt-0.5">
                      <span class="text-primary truncate max-w-[120px]">/events/{{ event.slug }}</span>
                      <span class="text-muted">•</span>
                      <span class="uppercase text-[9px] px-1 py-0.2 rounded bg-surface-card text-muted-light border border-border-subtle">
                        {{ event.category }}
                      </span>
                    </div>
                  </div>
                </div>
              </td>

              <!-- Venue -->
              <td class="py-3.5 px-4 font-mono text-on-surface-variant">
                <div class="font-medium text-high-emphasis">{{ event.venueName || 'Recinto no asignado' }}</div>
                <div class="text-[11px] text-muted">{{ event.venueCity || '—' }}</div>
              </td>

              <!-- Schedule -->
              <td class="py-3.5 px-4 font-mono text-muted-light">
                <div class="text-high-emphasis">{{ formatDate(event.startsAt) }}</div>
                <div class="text-[10px] text-muted">{{ event.timezone }}</div>
              </td>

              <!-- Base Price -->
              <td class="py-3.5 px-4 font-mono">
                <span class="text-high-emphasis font-bold">
                  ${{ (event.minPrice || 0).toLocaleString() }}
                </span>
                <span class="text-[10px] text-muted ml-1">{{ event.currency || 'ARS' }}</span>
              </td>

              <!-- Status Badge -->
              <td class="py-3.5 px-4">
                <StatusBadge :status="event.status" />
              </td>

              <!-- Row Actions -->
              <td class="py-3.5 px-4 text-right">
                <div class="flex items-center justify-end gap-1.5">
                  <!-- View on B2C client -->
                  <a
                    :href="`http://localhost:5173/events/${event.id}`"
                    target="_blank"
                    rel="noopener noreferrer"
                    class="p-1.5 rounded-lg text-muted hover:text-primary hover:bg-surface-card transition"
                    title="Ver en Portal B2C"
                  >
                    <span class="material-symbols-outlined text-base">storefront</span>
                  </a>

                  <!-- Publish (Only for DRAFT) -->
                  <button
                    v-if="event.status === 'DRAFT'"
                    type="button"
                    :disabled="actionInProgress === event.id"
                    @click="publishEvent(event.id)"
                    class="px-2.5 py-1 rounded-lg bg-secondary/15 hover:bg-secondary/25 text-secondary border border-secondary/30 text-[11px] font-mono font-semibold transition flex items-center gap-1 disabled:opacity-50"
                    title="Publicar a Venta General"
                  >
                    <span v-if="actionInProgress === event.id" class="material-symbols-outlined text-xs animate-spin">refresh</span>
                    <span v-else class="material-symbols-outlined text-xs">rocket_launch</span>
                    Publicar
                  </button>

                  <!-- Cancel Event (For PUBLISHED or DRAFT) -->
                  <button
                    v-if="event.status !== 'CANCELLED' && authStore.isAdmin"
                    type="button"
                    :disabled="actionInProgress === event.id"
                    @click="openCancelModal(event)"
                    class="p-1.5 rounded-lg text-muted hover:text-error hover:bg-surface-card transition disabled:opacity-50"
                    title="Cancelar Espectáculo Oficial"
                  >
                    <span class="material-symbols-outlined text-base">cancel</span>
                  </button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- Cancel Confirmation Modal -->
    <ConfirmModal
      :is-open="isCancelModalOpen"
      title="Confirmar Cancelación de Evento"
      subtitle="Acción Crítica Administrativa"
      :message="`¿Estás seguro de cancelar el espectáculo '${eventToCancel?.title}'? Esta acción registrará la anulación y suspenderá la venta de tickets.`"
      confirm-text="Cancelar Evento Oficialmente"
      :requires-reason="true"
      @confirm="confirmCancel"
      @cancel="isCancelModalOpen = false"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { RouterLink } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { eventAdminService } from '@/services/eventAdminService'
import type { EventResponse } from '@/types/event'
import StatusBadge from '@/components/common/StatusBadge.vue'
import ConfirmModal from '@/components/common/ConfirmModal.vue'

const authStore = useAuthStore()

const events = ref<EventResponse[]>([])
const loading = ref<boolean>(true)
const actionInProgress = ref<string | null>(null)
const feedbackMessage = ref<string | null>(null)
const errorMessage = ref<string | null>(null)

// Filters
const activeTab = ref<string>('ALL')
const searchQuery = ref<string>('')

const statusTabs = [
  { label: 'Todos', value: 'ALL' },
  { label: 'Publicados', value: 'PUBLISHED' },
  { label: 'Borradores', value: 'DRAFT' },
  { label: 'Cancelados', value: 'CANCELLED' },
]

// Modal state
const isCancelModalOpen = ref<boolean>(false)
const eventToCancel = ref<EventResponse | null>(null)

async function loadEvents() {
  loading.value = true
  errorMessage.value = null
  try {
    events.value = await eventAdminService.getEvents()
  } catch (err: any) {
    errorMessage.value = err.message || 'Error al cargar eventos del catálogo.'
  } finally {
    loading.value = false
  }
}

const filteredEvents = computed(() => {
  return events.value.filter((event) => {
    // Filter by tab
    if (activeTab.value !== 'ALL' && event.status !== activeTab.value) {
      return false
    }

    // Filter by search query
    if (searchQuery.value.trim()) {
      const q = searchQuery.value.toLowerCase()
      const matchTitle = event.title?.toLowerCase().includes(q)
      const matchVenue = event.venueName?.toLowerCase().includes(q)
      const matchSlug = event.slug?.toLowerCase().includes(q)
      return matchTitle || matchVenue || matchSlug
    }

    return true
  })
})

function formatDate(isoDate?: string): string {
  if (!isoDate) return '—'
  try {
    const d = new Date(isoDate)
    return d.toLocaleDateString('es-AR', {
      day: 'numeric',
      month: 'short',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    })
  } catch {
    return isoDate
  }
}

async function publishEvent(id: string) {
  actionInProgress.value = id
  errorMessage.value = null
  try {
    await eventAdminService.publishEvent(id)
    feedbackMessage.value = '¡Evento publicado exitosamente! Ya se encuentra disponible en la tienda B2C.'
    await loadEvents()
  } catch (err: any) {
    errorMessage.value = err.problem?.detail || err.message || 'Error al publicar el evento.'
  } finally {
    actionInProgress.value = null
  }
}

function openCancelModal(event: EventResponse) {
  eventToCancel.value = event
  isCancelModalOpen.value = true
}

async function confirmCancel(reason?: string) {
  if (!eventToCancel.value) return
  const id = eventToCancel.value.id
  isCancelModalOpen.value = false
  actionInProgress.value = id
  errorMessage.value = null

  try {
    await eventAdminService.cancelEvent(id, reason)
    feedbackMessage.value = `El evento '${eventToCancel.value.title}' fue cancelado oficialmente.`
    await loadEvents()
  } catch (err: any) {
    errorMessage.value = err.problem?.detail || err.message || 'Error al cancelar el evento.'
  } finally {
    actionInProgress.value = null
    eventToCancel.value = null
  }
}

onMounted(() => {
  loadEvents()
})
</script>
