<script setup lang="ts">
const searchQuery = defineModel<string>({ required: true })

defineEmits<{
  (e: 'clear'): void
}>()
</script>

<template>
  <section class="text-center flex flex-col items-center gap-6">
    <div class="flex flex-col items-center gap-2">
      <span class="inline-flex items-center gap-1.5 px-3 py-1 rounded-full bg-primary/10 border border-primary/20 text-primary text-xs font-mono font-semibold uppercase tracking-wider">
        <span class="material-symbols-outlined text-[15px]">help_center</span>
        Centro de Ayuda & Soporte
      </span>
      <h1 class="font-display text-3xl sm:text-4xl lg:text-5xl font-bold text-white tracking-tight drop-shadow-sm">
        ¿En qué podemos ayudarte hoy?
      </h1>
      <p class="font-body text-sm sm:text-base text-muted-light max-w-xl">
        Respuestas inmediatas sobre tus entradas digitales, transferencias seguras, accesos y medios de pago.
      </p>
    </div>

    <!-- Search Bar -->
    <div class="w-full max-w-2xl relative shadow-2xl">
      <div class="absolute inset-y-0 left-0 pl-4 flex items-center pointer-events-none text-muted-light">
        <span class="material-symbols-outlined text-[22px]">search</span>
      </div>
      <input
        v-model="searchQuery"
        class="w-full pl-12 pr-28 py-4 bg-surface-card/90 border border-border-subtle rounded-2xl text-high-emphasis placeholder:text-muted-light font-body text-sm sm:text-base focus:outline-none focus:border-primary/60 focus:ring-1 focus:ring-primary/40 focus:bg-surface-elevated/90 transition-all shadow-inner"
        placeholder="Buscar por término, ej: 'entradas', 'reembolso', 'QR', 'transferir'..."
        type="text"
        aria-label="Buscar en Centro de Ayuda"
      />
      <div class="absolute inset-y-0 right-2 flex items-center gap-1.5">
        <button
          v-if="searchQuery"
          @click="$emit('clear')"
          class="text-muted-light hover:text-white p-1.5 rounded-lg hover:bg-surface-interactive transition-colors cursor-pointer"
          title="Borrar búsqueda"
          aria-label="Borrar búsqueda"
        >
          <span class="material-symbols-outlined text-[18px]">close</span>
        </button>
        <button
          type="button"
          class="bg-primary hover:bg-btn-buy-hover text-on-primary-fixed font-body text-xs sm:text-sm font-bold px-4 py-2.5 rounded-xl transition-all flex items-center gap-1 shadow-sm select-none cursor-pointer"
        >
          <span>Buscar</span>
          <span class="material-symbols-outlined text-[16px]">arrow_forward</span>
        </button>
      </div>
    </div>
  </section>
</template>
