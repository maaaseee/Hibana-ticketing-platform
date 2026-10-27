<script setup lang="ts">
defineProps<{
  currentStep: 'sector' | 'zone' | 'seats'
  selectedZoneCode: string
  isCampoMode: boolean
  lockTimeRemaining: number
  formattedLockTime: string
}>()

const emit = defineEmits<{
  (e: 'exit'): void
  (e: 'goToStep', step: 'sector' | 'zone' | 'seats'): void
  (e: 'triggerStressTest'): void
}>()
</script>

<template>
  <header class="h-12 bg-[#101825] border-b border-[#23354d] text-xs px-4 sm:px-6 flex items-center justify-between z-50 shrink-0">
    <!-- Exit CTA Button -->
    <button
      @click="emit('exit')"
      class="flex items-center gap-2 px-3 py-1.5 rounded-lg bg-[#141c27] hover:bg-[#1b2533] border border-[#23354d] text-[#94a3b8] hover:text-[#f1f5f9] transition-colors font-mono text-xs cursor-pointer"
      title="Salir de la reserva y volver al inicio"
    >
      <span class="material-symbols-outlined text-[16px] text-[#86cffb]">arrow_back</span>
      <span>Salir al Home</span>
    </button>

    <!-- Stepped Breadcrumbs Trail -->
    <nav class="hidden sm:flex items-center gap-2 font-mono text-xs">
      <button
        @click="emit('goToStep', 'sector')"
        class="px-2.5 py-1 rounded transition-colors cursor-pointer"
        :class="currentStep === 'sector' ? 'bg-[#00688f] text-white font-bold' : 'text-[#94a3b8] hover:text-white'"
      >
        1. Sector General
      </button>
      <span class="text-[#23354d]">&gt;</span>
      <button
        @click="emit('goToStep', 'zone')"
        class="px-2.5 py-1 rounded transition-colors"
        :class="currentStep === 'zone' ? 'bg-[#00688f] text-white font-bold cursor-pointer' : currentStep === 'seats' ? 'text-[#94a3b8] hover:text-white cursor-pointer' : 'text-[#475569] cursor-not-allowed'"
        :disabled="currentStep === 'sector' || isCampoMode"
      >
        2. Selección de Zona ({{ selectedZoneCode }})
      </button>
      <span class="text-[#23354d]">&gt;</span>
      <button
        class="px-2.5 py-1 rounded transition-colors"
        :class="currentStep === 'seats' ? 'bg-[#00688f] text-white font-bold' : 'text-[#475569] cursor-not-allowed'"
        :disabled="currentStep !== 'seats'"
      >
        3. Selección de Butacas
      </button>
    </nav>

    <!-- Actions & Urgency Banner -->
    <div class="flex items-center gap-2">
      <!-- Stress Test Fila Virtual -->
      <button
        @click="emit('triggerStressTest')"
        class="flex items-center gap-1.5 px-2.5 py-1 rounded-lg bg-[#141c27] hover:bg-[#1b2533] border border-[#00688f]/40 text-[#86cffb] hover:text-white transition-colors font-mono text-xs cursor-pointer"
        title="Simular sala de espera de alta concurrencia (Redis Sorted Sets)"
      >
        <span class="material-symbols-outlined text-[15px] text-[#86cffb]">groups</span>
        <span class="hidden sm:inline">Stress Test Fila</span>
      </button>

      <!-- Urgency Countdown Pill (pulsing when time <= 120s) -->
      <div
        class="flex items-center gap-2 font-mono text-xs px-3 py-1 rounded border transition-colors"
        :class="lockTimeRemaining <= 120 ? 'bg-rose-950/80 border-rose-500/60 text-rose-300 animate-pulse' : 'bg-[#141c27] border-[#23354d] text-[#86cffb]'"
      >
        <span class="material-symbols-outlined text-[17px]" :class="lockTimeRemaining <= 120 ? 'text-rose-400' : 'text-[#86cffb]'">
          {{ lockTimeRemaining <= 120 ? 'alarm' : 'timer' }}
        </span>
        <span>
          {{ lockTimeRemaining <= 120 ? '¡Tiempo crítico! ' : 'Asientos reservados: ' }}
          <strong :class="lockTimeRemaining <= 120 ? 'text-rose-200' : 'text-white'">{{ formattedLockTime }}</strong>
        </span>
      </div>
    </div>
  </header>
</template>
