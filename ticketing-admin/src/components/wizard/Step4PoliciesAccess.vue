<template>
  <div class="space-y-6">
    <div class="border-b border-border-subtle pb-4">
      <h2 class="text-xl font-display font-bold text-high-emphasis">
        Paso 4: Políticas de Compra & Control de Acceso
      </h2>
      <p class="text-xs text-muted-light mt-1">
        Establece regulaciones de reventa, límites por usuario y medidas de seguridad antifraude.
      </p>
    </div>

    <!-- Max Tickets & Min Age -->
    <div class="grid grid-cols-1 md:grid-cols-2 gap-5">
      <div class="p-5 rounded-2xl bg-surface-container-deep border border-border-subtle space-y-3">
        <label class="block text-xs font-mono uppercase tracking-wider text-muted-light">
          Límite Máximo de Entradas por Comprador
        </label>
        <div class="flex items-center gap-3">
          <input
            v-model.number="wizardStore.maxTicketsPerUser"
            type="range"
            min="1"
            max="10"
            class="flex-1 accent-primary"
          />
          <span class="w-10 text-center font-mono font-bold text-base text-primary bg-surface-card py-1 rounded-lg border border-border-subtle">
            {{ wizardStore.maxTicketsPerUser }}
          </span>
        </div>
        <p class="text-xs text-muted">
          Evita acaparamiento y compras masivas no autorizadas limitando tickets por DNI o tarjeta.
        </p>
      </div>

      <div class="p-5 rounded-2xl bg-surface-container-deep border border-border-subtle space-y-3">
        <label class="block text-xs font-mono uppercase tracking-wider text-muted-light">
          Restricción de Edad Mínima
        </label>
        <div class="flex items-center gap-3">
          <input
            v-model.number="wizardStore.minAge"
            type="number"
            min="0"
            max="21"
            class="w-24 bg-surface-card border border-border-subtle rounded-xl px-3 py-2 text-sm font-mono text-high-emphasis focus:outline-none focus:border-primary"
          />
          <span class="text-xs text-muted-light font-mono">
            {{ wizardStore.minAge === 0 ? 'Apto para todo público' : `Mínimo ${wizardStore.minAge} años cumplidos` }}
          </span>
        </div>
        <p class="text-xs text-muted">
          Los compradores deberán validar identidad física en el ingreso al recinto.
        </p>
      </div>
    </div>

    <!-- Security Toggles -->
    <div class="space-y-4">
      <!-- Dynamic QR -->
      <div class="p-5 rounded-2xl bg-surface-container-deep border border-border-subtle flex items-start justify-between gap-4">
        <div>
          <div class="flex items-center gap-2">
            <span class="material-symbols-outlined text-secondary">qr_code_scanner</span>
            <h4 class="font-display font-bold text-sm text-high-emphasis">
              Código QR Dinámico Rotativo (Anti-captura de pantalla)
            </h4>
          </div>
          <p class="text-xs text-muted-light mt-1">
            Genera un token visual en la app que rota cada 15 segundos mediante HMAC SHA-256 para impedir la reventa de capturas estáticas.
          </p>
        </div>

        <label class="relative inline-flex items-center cursor-pointer shrink-0">
          <input
            v-model="wizardStore.dynamicQr"
            type="checkbox"
            class="sr-only peer"
          />
          <div
            class="w-11 h-6 bg-surface-card peer-focus:outline-none rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:border-gray-300 after:border after:rounded-full after:h-5 after:w-5 after:transition-all peer-checked:bg-secondary"
          ></div>
        </label>
      </div>

      <!-- Transferability -->
      <div class="p-5 rounded-2xl bg-surface-container-deep border border-border-subtle flex items-start justify-between gap-4">
        <div>
          <div class="flex items-center gap-2">
            <span class="material-symbols-outlined text-primary">swap_horiz</span>
            <h4 class="font-display font-bold text-sm text-high-emphasis">
              Transferencia Oficial Nominada
            </h4>
          </div>
          <p class="text-xs text-muted-light mt-1">
            Permite al comprador original ceder la entrada nominativamente a otro usuario registrado hasta 4 horas antes del inicio.
          </p>
        </div>

        <label class="relative inline-flex items-center cursor-pointer shrink-0">
          <input
            v-model="wizardStore.transferable"
            type="checkbox"
            class="sr-only peer"
          />
          <div
            class="w-11 h-6 bg-surface-card peer-focus:outline-none rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:border-gray-300 after:border after:rounded-full after:h-5 after:w-5 after:transition-all peer-checked:bg-primary"
          ></div>
        </label>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useEventWizardStore } from '@/stores/eventWizard'

const wizardStore = useEventWizardStore()
</script>
