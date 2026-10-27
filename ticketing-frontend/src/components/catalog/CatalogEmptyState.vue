<script setup lang="ts">
defineEmits<{
  (e: 'reset'): void
}>()
</script>

<template>
  <div class="bg-surface-card/50 border border-border-subtle rounded-2xl p-10 flex flex-col items-center justify-center text-center gap-3 my-8">
    <span class="material-symbols-outlined text-4xl text-muted-light">search_off</span>
    <h4 class="font-display text-lg font-bold text-high-emphasis">No se encontraron espectáculos</h4>
    <p class="font-body text-sm text-muted-light max-w-md">
      No hay eventos que coincidan con los filtros seleccionados. Prueba con otros criterios o limpia la búsqueda.
    </p>
    <button
      @click="$emit('reset')"
      class="mt-2 bg-surface-interactive hover:bg-border-subtle text-primary font-mono text-xs px-4 py-2 rounded-lg border border-border-subtle transition-colors cursor-pointer"
    >
      Limpiar filtros
    </button>
  </div>
</template>
