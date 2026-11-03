<template>
  <div class="space-y-6">
    <div class="border-b border-border-subtle pb-4">
      <h2 class="text-xl font-display font-bold text-high-emphasis">
        Paso 2: Fechas, Horarios & Sala de Espera
      </h2>
      <p class="text-xs text-muted-light mt-1">
        Configura el cronograma del evento y la política de descongestión de tráfico.
      </p>
    </div>

    <!-- Times Grid -->
    <div class="grid grid-cols-1 md:grid-cols-3 gap-4">
      <div>
        <label class="block text-xs font-mono uppercase tracking-wider text-muted-light mb-1.5">
          Apertura de Puertas <span class="text-error">*</span>
        </label>
        <input
          v-model="wizardStore.doorsOpenAt"
          type="datetime-local"
          class="w-full bg-surface-container-deep border border-border-subtle rounded-xl px-3.5 py-2.5 text-xs font-mono text-high-emphasis focus:outline-none focus:border-primary transition"
        />
      </div>

      <div>
        <label class="block text-xs font-mono uppercase tracking-wider text-muted-light mb-1.5">
          Inicio del Espectáculo <span class="text-error">*</span>
        </label>
        <input
          v-model="wizardStore.startsAt"
          type="datetime-local"
          class="w-full bg-surface-container-deep border border-border-subtle rounded-xl px-3.5 py-2.5 text-xs font-mono text-high-emphasis focus:outline-none focus:border-primary transition"
        />
      </div>

      <div>
        <label class="block text-xs font-mono uppercase tracking-wider text-muted-light mb-1.5">
          Finalización Estimada <span class="text-error">*</span>
        </label>
        <input
          v-model="wizardStore.endsAt"
          type="datetime-local"
          class="w-full bg-surface-container-deep border border-border-subtle rounded-xl px-3.5 py-2.5 text-xs font-mono text-high-emphasis focus:outline-none focus:border-primary transition"
        />
      </div>
    </div>

    <!-- Timezone -->
    <div>
      <label class="block text-xs font-mono uppercase tracking-wider text-muted-light mb-1.5">
        Zona Horaria Oficial
      </label>
      <select
        v-model="wizardStore.timezone"
        class="w-full bg-surface-container-deep border border-border-subtle rounded-xl px-4 py-2.5 text-sm text-high-emphasis focus:outline-none focus:border-primary transition font-mono"
      >
        <option value="America/Argentina/Buenos_Aires">America/Argentina/Buenos_Aires (ART, UTC-3)</option>
        <option value="America/Santiago">America/Santiago (CLT, UTC-3)</option>
        <option value="America/Montevideo">America/Montevideo (UYT, UTC-3)</option>
        <option value="America/Sao_Paulo">America/Sao_Paulo (BRT, UTC-3)</option>
        <option value="UTC">UTC (Universal Coordinated Time)</option>
      </select>
    </div>

    <!-- Virtual Waiting Room Configuration -->
    <div class="p-5 rounded-2xl bg-surface-container-deep border border-border-subtle space-y-4">
      <div class="flex items-start justify-between gap-4">
        <div>
          <div class="flex items-center gap-2">
            <span class="material-symbols-outlined text-secondary">queue</span>
            <h3 class="font-display font-bold text-sm text-high-emphasis">
              Sala de Espera Virtual (Virtual Waiting Room)
            </h3>
            <span class="text-[9px] font-mono px-2 py-0.5 rounded-full bg-secondary/10 text-secondary border border-secondary/20 uppercase font-semibold">
              Gateway CheckoutToken
            </span>
          </div>
          <p class="text-xs text-muted-light mt-1">
            Activa una cola FIFO en Redis con tokens criptográficos para eventos de altísima demanda y evitar saturación del backend.
          </p>
        </div>

        <label class="relative inline-flex items-center cursor-pointer">
          <input
            v-model="wizardStore.waitingRoomEnabled"
            type="checkbox"
            class="sr-only peer"
          />
          <div
            class="w-11 h-6 bg-surface-card peer-focus:outline-none rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:border-gray-300 after:border after:rounded-full after:h-5 after:w-5 after:transition-all peer-checked:bg-secondary"
          ></div>
        </label>
      </div>

      <div v-if="wizardStore.waitingRoomEnabled" class="pt-3 border-t border-border-subtle grid grid-cols-1 md:grid-cols-2 gap-4">
        <div>
          <label class="block text-[11px] font-mono text-muted mb-1">
            Umbral de Tráfico Simultáneo (Conexiones concurrentes)
          </label>
          <input
            v-model.number="wizardStore.waitingRoomQueueThreshold"
            type="number"
            min="100"
            step="100"
            class="w-full bg-surface-card border border-border-subtle rounded-xl px-3 py-2 text-xs font-mono text-high-emphasis focus:outline-none focus:border-secondary transition"
          />
        </div>

        <div class="flex items-center text-xs text-muted-light font-mono">
          <span class="material-symbols-outlined text-secondary mr-2 text-base">info</span>
          Al superar los {{ wizardStore.waitingRoomQueueThreshold.toLocaleString() }} usuarios concurrentes, el Gateway redirigirá al checkout token gate.
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useEventWizardStore } from '@/stores/eventWizard'

const wizardStore = useEventWizardStore()
</script>
