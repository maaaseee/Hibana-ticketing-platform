<script setup lang="ts">
defineProps<{
  checkoutToken: string | null
}>()

defineEmits<{
  (e: 'proceed'): void
}>()
</script>

<template>
  <div class="flex flex-col items-center w-full">
    <!-- Verified Success Icon -->
    <div class="w-16 h-16 rounded-full bg-emerald-950/80 border-2 border-emerald-500/50 flex items-center justify-center mb-5 text-emerald-400 shadow-lg shadow-emerald-500/20 animate-bounce">
      <span class="material-symbols-outlined text-[34px]">verified</span>
    </div>

    <span class="inline-block px-3 py-1 rounded-full text-xs font-mono font-bold bg-emerald-950 text-emerald-300 border border-emerald-500/40 mb-2">
      Token Emitido • X-Checkout-Token Habilitado
    </span>

    <h2 class="font-display text-3xl sm:text-4xl font-extrabold text-high-emphasis mb-2 tracking-tight">
      ¡Es tu turno!
    </h2>
    <p class="font-body text-sm text-muted-light mb-6 max-w-md leading-relaxed">
      Tu sesión criptográfica ha sido validada. Cuentas con 15 minutos reservados para seleccionar tus butacas en el mapa interactivo.
    </p>

    <!-- Token ID Info Card -->
    <div class="w-full bg-surface-elevated border border-border-subtle p-3.5 rounded-xl text-left mb-6 font-mono text-xs">
      <div class="flex items-center justify-between text-muted-light mb-1">
        <span>Token de Seguridad</span>
        <span class="text-emerald-400 font-bold">ACTIVO</span>
      </div>
      <p class="text-white text-[11px] truncate bg-canvas-base p-2 rounded-lg border border-border-subtle/70 font-mono">
        {{ checkoutToken }}
      </p>
    </div>

    <!-- Action Button to proceed to seat selection -->
    <button
      @click="$emit('proceed')"
      class="w-full py-3.5 px-8 rounded-full font-display font-extrabold text-sm sm:text-base bg-btn-buy hover:bg-btn-buy-hover text-white transition-all shadow-lg hover:shadow-sky-500/25 active:scale-95 cursor-pointer flex items-center justify-center gap-2"
    >
      <span>Seleccionar mis butacas</span>
      <span class="material-symbols-outlined text-lg">arrow_forward</span>
    </button>
  </div>
</template>
