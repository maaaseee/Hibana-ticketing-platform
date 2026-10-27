<script setup lang="ts">
const logoUrl = '/logo.png'

defineEmits<{
  (e: 'logoClick'): void
}>()
</script>

<template>
  <header class="relative z-20 w-full flex items-center justify-between">
    <!-- Logo Button con Advertencia de salida -->
    <button
      @click="$emit('logoClick')"
      class="flex items-center gap-2.5 sm:gap-3 px-3 py-2 rounded-xl bg-surface-card/70 hover:bg-surface-elevated border border-border-subtle text-left transition-all group cursor-pointer shadow-lg backdrop-blur-md focus:outline-hidden focus:ring-2 focus:ring-primary/50"
      title="Volver al inicio (Aviso: Se perderá el lugar en la fila)"
      data-testid="queue-logo-home-btn"
    >
      <img
        :src="logoUrl"
        alt="Amaryllis"
        class="w-7 h-7 sm:w-8 sm:h-8 object-contain drop-shadow-[0_0_10px_rgba(134,207,251,0.4)] group-hover:scale-105 transition-transform"
      />
      <div class="flex items-center gap-2">
        <span class="font-display text-xl sm:text-2xl font-black text-primary tracking-tight group-hover:text-white transition-colors">
          Amaryllis
        </span>
        <span class="w-1.5 h-1.5 rounded-full bg-primary-container"></span>
        <span class="font-mono text-[11px] sm:text-xs text-muted-light uppercase tracking-wider font-semibold">
          Queue System
        </span>
      </div>
    </button>

    <!-- Live High Demand Tag -->
    <div class="hidden sm:flex items-center gap-2 px-3 py-1.5 rounded-full bg-surface-card/80 border border-border-subtle text-xs font-mono text-medium-emphasis backdrop-blur-md shadow-sm">
      <span class="w-2 h-2 rounded-full bg-emerald-400 animate-pulse"></span>
      <span>Fila Oficial de Acceso • Alta concurrencia protegida</span>
    </div>
  </header>
</template>
