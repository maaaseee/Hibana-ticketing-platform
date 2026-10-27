<script setup lang="ts">
defineEmits<{
  (e: 'clear'): void
}>()
</script>

<template>
  <div class="flex items-center justify-between pb-4 border-b border-border-subtle shrink-0">
    <div class="flex items-center gap-3">
      <div class="w-10 h-10 rounded-xl bg-linear-to-tr from-primary to-secondary flex items-center justify-center text-canvas-base font-bold shadow-md shadow-primary/20">
        <svg class="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 10V3L4 14h7v7l9-11h-7z" />
        </svg>
      </div>
      <div>
        <div class="flex items-center gap-2">
          <h1 class="text-base sm:text-lg font-bold font-display text-high-emphasis">
            Amaryllis Intelligent Support
          </h1>
          <span class="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[10px] font-mono font-semibold bg-secondary/15 text-secondary border border-secondary/30">
            <span class="w-1.5 h-1.5 rounded-full bg-secondary animate-pulse"></span>
            Live RAG
          </span>
        </div>
        <p class="text-xs text-medium-emphasis">
          Canal Seguro de Atención al Espectador &bull; Encriptación de extremo a extremo
        </p>
      </div>
    </div>

    <button
      @click="$emit('clear')"
      class="text-xs font-mono text-muted-light hover:text-high-emphasis transition-colors cursor-pointer"
      title="Clear conversation"
    >
      Reset Chat
    </button>
  </div>
</template>
