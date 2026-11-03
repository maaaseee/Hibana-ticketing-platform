<template>
  <div class="space-y-6">
    <!-- Page Header -->
    <div class="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
      <div>
        <h1 class="text-2xl font-display font-black tracking-tight text-high-emphasis">
          Recintos & Capacidades
        </h1>
        <p class="text-xs text-muted-light mt-1">
          Directorio de estadios, arenas y teatros habilitados para la venta de localidades.
        </p>
      </div>

      <div class="flex items-center gap-3">
        <button
          type="button"
          @click="loadVenues"
          class="flex items-center gap-2 px-3 py-2 rounded-xl bg-surface-container border border-border-subtle hover:bg-surface-elevated text-xs font-mono text-muted-light hover:text-high-emphasis transition"
        >
          <span class="material-symbols-outlined text-sm" :class="{ 'animate-spin': loading }">refresh</span>
          Actualizar
        </button>
      </div>
    </div>

    <!-- Loading Skeleton -->
    <div v-if="loading" class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-5">
      <div
        v-for="i in 3"
        :key="i"
        class="h-48 rounded-2xl bg-surface-container/50 border border-border-subtle animate-pulse"
      ></div>
    </div>

    <!-- Venues Grid -->
    <div v-else class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-5">
      <div
        v-for="venue in venues"
        :key="venue.id"
        class="bg-surface-container border border-border-subtle hover:border-primary/40 rounded-2xl p-5 transition flex flex-col justify-between group shadow-sm"
      >
        <div>
          <div class="flex items-start justify-between gap-3 mb-3">
            <div class="w-10 h-10 rounded-xl bg-surface-elevated border border-border-subtle flex items-center justify-center text-primary group-hover:scale-105 transition">
              <span class="material-symbols-outlined">apartment</span>
            </div>
            <span class="text-[10px] font-mono px-2 py-0.5 rounded-full bg-secondary/10 text-secondary border border-secondary/30 uppercase font-semibold">
              {{ venue.status || 'Habilitado' }}
            </span>
          </div>

          <h3 class="font-display font-bold text-base text-high-emphasis group-hover:text-primary transition line-clamp-1">
            {{ venue.name }}
          </h3>
          <p class="text-xs text-muted-light mt-1 flex items-center gap-1.5">
            <span class="material-symbols-outlined text-sm text-muted">location_on</span>
            {{ venue.address }}, {{ venue.city }}
          </p>
        </div>

        <div class="mt-6 pt-4 border-t border-border-subtle/60 flex items-center justify-between text-xs font-mono">
          <div>
            <span class="text-muted block text-[10px] uppercase tracking-wider">Aforo Legal Total</span>
            <span class="font-bold text-high-emphasis text-sm">
              {{ resolveVenueCapacity(venue).toLocaleString() }}
            </span>
            <span class="text-muted-light text-[10px] ml-1">espectadores</span>
          </div>

          <div class="text-right">
            <span class="text-muted block text-[10px] uppercase tracking-wider">País</span>
            <span class="text-muted-light font-medium">{{ venue.country || 'Argentina' }}</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import type { VenueResponse } from '@/types/venue'
import { venueService, resolveVenueCapacity } from '@/services/venueService'

const venues = ref<VenueResponse[]>([])
const loading = ref<boolean>(true)

async function loadVenues() {
  loading.value = true
  try {
    venues.value = await venueService.getVenues()
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadVenues()
})
</script>
