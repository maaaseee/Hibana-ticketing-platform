<script setup lang="ts">
import AppLogo from '../ui/AppLogo.vue'

defineProps<{
  isTimeCritical: boolean
  formattedCountdown: string
}>()

const emit = defineEmits<{
  (e: 'back'): void
}>()
</script>

<template>
  <header class="bg-[#0e1927]/90 sticky top-0 z-40 border-b border-[#23354d] backdrop-blur-md">
    <div class="max-w-[1280px] mx-auto px-4 md:px-12 py-3 flex justify-between items-center w-full">
      <!-- Return Button -->
      <button
        type="button"
        @click="emit('back')"
        class="flex items-center gap-2 cursor-pointer group bg-transparent border-0 p-0 text-left"
        aria-label="Volver a la vista anterior"
      >
        <span class="material-symbols-outlined text-[#94a3b8] group-hover:text-[#86cffb] transition-colors text-lg">arrow_back</span>
        <span class="font-mono text-sm text-[#94a3b8] group-hover:text-[#86cffb] transition-colors font-medium">Volver</span>
      </button>

      <!-- Brand Name con componente unificado AppLogo -->
      <AppLogo size="md" />

      <!-- Atomic Redlock Timer Chip -->
      <div
        class="flex items-center gap-1.5 px-3 py-1 rounded-full border transition-all"
        :class="isTimeCritical ? 'bg-rose-950/40 border-rose-500/60 text-rose-300 animate-pulse' : 'bg-[#141e2e] border-[#23354d] text-[#f1f5f9]'"
      >
        <span
          class="material-symbols-outlined text-[18px]"
          :class="isTimeCritical ? 'text-rose-400' : 'text-[#86cffb]'"
        >
          timer
        </span>
        <span class="font-mono text-sm font-bold tracking-wider" id="countdown">
          {{ formattedCountdown }}
        </span>
      </div>
    </div>
  </header>
</template>
