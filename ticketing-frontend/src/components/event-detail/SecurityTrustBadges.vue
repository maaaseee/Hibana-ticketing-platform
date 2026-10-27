<script setup lang="ts">
import {
  TooltipRoot,
  TooltipTrigger,
  TooltipContent
} from 'radix-vue'

const emit = defineEmits<{
  (e: 'enterQueue'): void
}>()
</script>

<template>
  <div class="flex flex-col gap-6">
    <!-- Stress Test Simulation Banner for Virtual Waiting Room (Exterior a la tarjeta principal) -->
    <div class="p-4 rounded-2xl bg-[#101825] border border-border-subtle flex flex-col sm:flex-row items-start sm:items-center justify-between gap-3 shadow-md">
      <div class="flex items-center gap-3">
        <div class="w-9 h-9 rounded-xl bg-[#00688f]/20 border border-[#00688f]/40 flex items-center justify-center text-[#86cffb]">
          <span class="material-symbols-outlined text-[20px]">speed</span>
        </div>
        <div>
          <div class="flex items-center gap-2">
            <span class="font-display font-bold text-white text-sm">Stress Test: Fila de Acceso Virtual</span>
            <span class="px-2 py-0.5 rounded-full text-[10px] font-mono bg-cyan-950 text-cyan-300 border border-cyan-500/40 font-semibold">Simulación</span>
          </div>
          <p class="font-body text-xs text-muted-light mt-0.5">Pon a prueba el sistema de concurrencia y sala de espera distribuida (Redis Sorted Sets).</p>
        </div>
      </div>
      <button
        @click="emit('enterQueue')"
        class="px-5 py-2.5 rounded-xl bg-[#00688f] hover:bg-[#0084ff] text-white font-mono text-xs font-bold transition-all flex items-center gap-1.5 shadow-md cursor-pointer shrink-0"
      >
        <span>Entrar a la Fila</span>
        <span class="material-symbols-outlined text-[16px]">arrow_forward</span>
      </button>
    </div>

    <!-- Redlock & Zero-overselling Guarantee Trust Badges -->
    <div class="bg-surface-card/60 rounded-xl p-4 border border-border-subtle/60 flex flex-col gap-2.5 text-[11px] font-mono text-muted-light">
      <TooltipRoot>
        <TooltipTrigger as-child>
          <div class="flex items-center gap-2 cursor-help hover:text-medium-emphasis transition-colors">
            <span class="material-symbols-outlined text-[16px] text-secondary">lock_clock</span>
            <span>Bloqueo Atómico Redlock 10m TTL</span>
          </div>
        </TooltipTrigger>
        <TooltipContent class="bg-surface-container-deep border border-border-subtle text-xs font-mono text-medium-emphasis p-2 rounded-lg shadow-xl z-50">
          Reserva atómica distribuida en Redis durante el checkout sin riesgo de colisión
        </TooltipContent>
      </TooltipRoot>

      <TooltipRoot>
        <TooltipTrigger as-child>
          <div class="flex items-center gap-2 cursor-help hover:text-medium-emphasis transition-colors">
            <span class="material-symbols-outlined text-[16px] text-primary">verified_user</span>
            <span>Garantía de Asiento Sin Sobreventa</span>
          </div>
        </TooltipTrigger>
        <TooltipContent class="bg-surface-container-deep border border-border-subtle text-xs font-mono text-medium-emphasis p-2 rounded-lg shadow-xl z-50">
          Verificado por el outbox transaccional y orquestación Saga
        </TooltipContent>
      </TooltipRoot>
    </div>
  </div>
</template>
