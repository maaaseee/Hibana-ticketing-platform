<script setup lang="ts">
import { RouterLink } from 'vue-router'
</script>

<template>
  <div class="text-center py-20 px-4 rounded-3xl bg-surface-card border border-border-subtle space-y-4">
    <div class="w-16 h-16 mx-auto rounded-full bg-canvas-base flex items-center justify-center text-muted-light">
      <svg class="w-8 h-8" fill="none" viewBox="0 0 24 24" stroke="currentColor">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M15 5v2m0 4v2m0 4v2M5 5a2 2 0 00-2 2v3a2 2 0 110 4v3a2 2 0 002 2h14a2 2 0 002-2v-3a2 2 0 110-4V7a2 2 0 00-2-2H5z" />
      </svg>
    </div>
    <h2 class="text-xl font-bold font-display text-high-emphasis">No tienes pases emitidos</h2>
    <p class="text-sm text-medium-emphasis max-w-sm mx-auto">
      Aún no has adquirido entradas para próximos espectáculos.
    </p>
    <div class="pt-2">
      <RouterLink
        :to="{ name: 'home' }"
        class="inline-flex items-center gap-2 px-6 py-3 rounded-xl font-bold bg-btn-buy text-white hover:bg-btn-buy-hover transition-colors"
      >
        Explorar Cartelera &rarr;
      </RouterLink>
    </div>
  </div>
</template>
