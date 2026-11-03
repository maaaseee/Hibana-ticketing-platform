<script setup lang="ts">
import { ref, onErrorCaptured } from 'vue'

const hasError = ref(false)
const errorMessage = ref('')
const errorDetails = ref('')

onErrorCaptured((err, instance, info) => {
  console.error('[GLOBAL-ERROR-BOUNDARY] Capturado error de renderizado:', err, info)
  hasError.value = true
  errorMessage.value = err instanceof Error ? err.message : String(err)
  errorDetails.value = info
  // Detener propagación para evitar que rompa toda la aplicación
  return false
})

function handleRetry() {
  hasError.value = false
  errorMessage.value = ''
  errorDetails.value = ''
}

function handleGoHome() {
  hasError.value = false
  window.location.href = '/'
}
</script>

<template>
  <div v-if="hasError" class="min-h-screen bg-[#020b1f] text-white flex flex-col items-center justify-center p-6 text-center select-none">
    <!-- Ambient Stage Glow -->
    <div class="absolute inset-0 bg-radial from-rose-900/20 via-transparent to-transparent pointer-events-none"></div>

    <div class="relative z-10 max-w-lg w-full bg-[#0c1524]/90 border border-rose-500/40 rounded-3xl p-8 backdrop-blur-2xl shadow-[0_20px_60px_rgba(0,0,0,0.8)]">
      <div class="w-16 h-16 rounded-2xl bg-rose-950/80 border border-rose-500/50 flex items-center justify-center mx-auto mb-5 text-rose-400 shadow-[0_0_30px_rgba(225,29,72,0.35)]">
        <span class="material-symbols-outlined text-3xl">warning</span>
      </div>

      <h1 class="font-display font-black text-2xl sm:text-3xl tracking-tight uppercase text-white mb-2">
        Interferencia Técnica
      </h1>
      <p class="font-body text-sm text-[#94a3b8] mb-6 leading-relaxed">
        Ocurrió un problema inesperado al cargar este sector del escenario. No te preocupes, tus datos de cuenta siguen seguros.
      </p>

      <div v-if="errorMessage" class="mb-6 p-3 rounded-xl bg-black/60 border border-white/10 text-[11px] font-mono text-rose-300 text-left overflow-x-auto max-h-32">
        {{ errorMessage }}
      </div>

      <div class="flex flex-col sm:flex-row items-center gap-3">
        <button
          type="button"
          @click="handleRetry"
          class="w-full sm:w-1/2 py-3 rounded-full bg-[#0084ff] hover:bg-[#0070df] text-white font-display font-bold text-sm tracking-wide transition-all shadow-[0_0_20px_rgba(0,132,255,0.4)] cursor-pointer"
        >
          Reintentar
        </button>
        <button
          type="button"
          @click="handleGoHome"
          class="w-full sm:w-1/2 py-3 rounded-full bg-[#141d2b] hover:bg-[#1c293d] text-[#cbd5e1] font-display font-semibold text-sm transition-colors cursor-pointer"
        >
          Volver a Cartelera
        </button>
      </div>
    </div>
  </div>

  <slot v-else />
</template>
