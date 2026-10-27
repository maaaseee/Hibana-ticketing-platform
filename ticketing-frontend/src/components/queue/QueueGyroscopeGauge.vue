<script setup lang="ts">
import type { EventItem } from '../../types'

defineProps<{
  currentPosition: number
  progressPercentage: number
  formattedEstimatedTime: string
  event?: EventItem | null
}>()

defineEmits<{
  (e: 'skipDemo'): void
}>()
</script>

<template>
  <div class="flex flex-col items-center w-full">
    <!-- Icon / Status Indicator -->
    <div class="w-14 h-14 rounded-full bg-surface-elevated border border-border-subtle flex items-center justify-center mb-5 shadow-inner">
      <span class="material-symbols-outlined text-primary text-[28px] animate-pulse">hourglass_empty</span>
    </div>

    <!-- Headline & Subtitle -->
    <h1 class="font-display text-2xl sm:text-3xl font-bold text-high-emphasis mb-2 tracking-tight">
      Estás en la fila
    </h1>
    <p class="font-body text-sm sm:text-base text-muted-light mb-6 sm:mb-8">
      Por favor no cierres ni actualices esta página.
    </p>

    <!-- Circular Progress / Gyroscope Status Area -->
    <div class="relative w-52 h-52 sm:w-56 sm:h-56 flex items-center justify-center mb-7 sm:mb-8">
      <!-- Clean solid outer ring -->
      <div class="absolute inset-0 rounded-full border border-border-subtle"></div>
      
      <!-- Dashed counter-rotating ring -->
      <div
        class="absolute inset-3 rounded-full border border-dashed border-border-subtle/80 animate-spin-slow"
        style="animation-direction: reverse;"
      ></div>
      
      <!-- Rotating accent active ring -->
      <div class="absolute inset-0 rounded-full border-2 border-t-primary-container border-r-primary-container border-b-transparent border-l-transparent animate-spin-slow"></div>

      <!-- Dynamic SVG Progress Arc overlay -->
      <svg class="absolute inset-0 w-full h-full -rotate-90 pointer-events-none" viewBox="0 0 100 100">
        <circle
          cx="50"
          cy="50"
          r="44"
          fill="none"
          stroke="#10b981"
          stroke-width="2.5"
          stroke-dasharray="276.46"
          :stroke-dashoffset="276.46 - (276.46 * progressPercentage) / 100"
          stroke-linecap="round"
          class="transition-all duration-700 ease-out opacity-80"
        />
      </svg>

      <!-- Center Status Info -->
      <div class="relative z-10 flex flex-col items-center">
        <span class="font-mono text-xs text-muted-light uppercase tracking-widest mb-1 font-semibold">
          Tu posición
        </span>
        <span class="font-display text-4xl sm:text-5xl font-extrabold text-high-emphasis tabular-nums tracking-tight">
          {{ currentPosition.toLocaleString('es-AR') }}
        </span>
        <span v-if="event" class="font-mono text-[10px] text-primary/80 mt-1 max-w-[140px] truncate">
          {{ event.title }}
        </span>
      </div>
    </div>

    <!-- Estimated Time Footer Box -->
    <div class="w-full bg-surface-elevated border border-border-subtle flex flex-col items-center py-3.5 px-4 rounded-xl shadow-inner">
      <div class="flex items-center gap-2 text-high-emphasis">
        <span class="material-symbols-outlined text-[18px] text-primary">schedule</span>
        <span class="font-mono text-xs sm:text-sm font-medium text-medium-emphasis">
          Tiempo estimado: <strong class="text-primary font-semibold">{{ formattedEstimatedTime }}</strong>
        </span>
      </div>
      
      <div class="mt-2.5 w-full max-w-[220px] h-1.5 bg-canvas-base border border-border-subtle/60 rounded-full overflow-hidden">
        <div
          class="h-full bg-linear-to-r from-primary-container via-secondary to-primary rounded-full transition-all duration-700 ease-out"
          :style="{ width: `${progressPercentage}%` }"
        ></div>
      </div>
    </div>

    <!-- Dev Test Acceleration Shortcut -->
    <button
      @click="$emit('skipDemo')"
      class="mt-4 text-xs text-muted hover:text-primary font-mono underline cursor-pointer transition-colors"
      title="Acelerar turno para pruebas locales"
    >
      [Simular turno inmediato (Demo)]
    </button>
  </div>
</template>

<style scoped>
@keyframes spin-slow {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

.animate-spin-slow {
  animation: spin-slow 12s linear infinite;
}
</style>
