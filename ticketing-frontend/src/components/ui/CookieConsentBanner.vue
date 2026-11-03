<script setup lang="ts">
import { ref, onMounted } from 'vue'

export interface CookiePreferences {
  necessary: boolean
  analytics: boolean
  timestamp: number
}

const STORAGE_KEY = 'hibana_cookie_consent'

const isVisible = ref(false)
const showDetails = ref(false)

onMounted(() => {
  if (typeof window !== 'undefined') {
    const saved = localStorage.getItem(STORAGE_KEY)
    if (!saved) {
      // Pequeño retardo para no saturar al usuario inmediatamente al cargar
      setTimeout(() => {
        isVisible.value = true
      }, 700)
    }
  }
})

function saveConsent(analytics: boolean) {
  const preferences: CookiePreferences = {
    necessary: true,
    analytics,
    timestamp: Date.now()
  }

  if (typeof window !== 'undefined') {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(preferences))
  }
  isVisible.value = false
}

function acceptNecessary() {
  saveConsent(false)
}

function acceptAll() {
  saveConsent(true)
}
</script>

<template>
  <Transition
    enter-active-class="transition duration-300 ease-out"
    enter-from-class="transform translate-y-8 opacity-0 scale-95"
    enter-to-class="transform translate-y-0 opacity-100 scale-100"
    leave-active-class="transition duration-200 ease-in"
    leave-from-class="transform translate-y-0 opacity-100 scale-100"
    leave-to-class="transform translate-y-8 opacity-0 scale-95"
  >
    <div
      v-if="isVisible"
      data-testid="cookie-consent-banner"
      role="region"
      aria-label="Aviso de cookies y privacidad"
      class="fixed bottom-4 left-4 right-4 sm:left-auto sm:right-6 sm:max-w-md z-50 p-5 rounded-2xl bg-[#0c1524]/95 border border-[#23354d] shadow-[0_20px_50px_rgba(0,0,0,0.85)] backdrop-blur-xl text-white font-sans select-none"
    >
      <div class="flex items-start gap-3.5">
        <div class="w-10 h-10 rounded-xl bg-[#00688f]/30 border border-[#38bdf8]/40 flex items-center justify-center text-[#38bdf8] shrink-0 text-xl">
          🍪
        </div>

        <div class="flex-1 min-w-0">
          <h4 class="font-display font-bold text-sm text-white flex items-center gap-2">
            <span>Tu Privacidad en Hibana</span>
            <span class="px-2 py-0.5 rounded-full bg-[#142033] border border-[#23354d] text-[10px] font-mono text-[#86cffb]">
              Conexión Segura
            </span>
          </h4>
          <p class="font-sans text-xs text-[#94a3b8] mt-1.5 leading-relaxed">
            Utilizamos cookies técnicas para garantizar tu lugar en la sala de espera virtual, proteger tus pagos y recordarte tus entradas oficiales.
          </p>

          <!-- Detalles desplegables -->
          <div v-if="showDetails" class="mt-3 p-3 rounded-xl bg-[#08101c] border border-[#1b2b3f] space-y-2 text-xs">
            <div class="flex items-center justify-between">
              <span class="font-bold text-white">Esenciales (Obligatorias)</span>
              <span class="text-emerald-400 font-mono text-[11px]">Siempre activas</span>
            </div>
            <p class="text-[11px] text-[#64748b]">
              Fila virtual en vivo, tokens de checkout cifrados y estado de sesión.
            </p>
            <div class="flex items-center justify-between pt-1 border-t border-[#1b2b3f]">
              <span class="font-bold text-white">Rendimiento y Métricas</span>
              <span class="text-[#86cffb] font-mono text-[11px]">Opcional</span>
            </div>
            <p class="text-[11px] text-[#64748b]">
              Nos ayuda a optimizar los tiempos de respuesta de los servidores de tickets.
            </p>
          </div>

          <div class="mt-3 flex items-center justify-between">
            <button
              type="button"
              @click="showDetails = !showDetails"
              class="text-[11px] font-mono text-[#38bdf8] hover:underline cursor-pointer"
            >
              {{ showDetails ? 'Ocultar detalles' : 'Configurar preferencias' }}
            </button>
          </div>

          <!-- Acciones -->
          <div class="mt-3.5 flex items-center gap-2.5">
            <button
              type="button"
              @click="acceptNecessary"
              data-testid="cookie-accept-necessary"
              class="flex-1 py-2 px-3 rounded-xl bg-[#142033] hover:bg-[#1a2b45] border border-[#23354d] text-xs font-display font-semibold text-[#cbd5e1] hover:text-white transition-colors cursor-pointer text-center"
            >
              Solo Necesarias
            </button>

            <button
              type="button"
              @click="acceptAll"
              data-testid="cookie-accept-all"
              class="flex-1 py-2 px-3 rounded-xl bg-gradient-to-r from-[#00688f] to-[#0284c7] hover:from-[#0284c7] hover:to-[#38bdf8] text-xs font-display font-bold text-white transition-all shadow-md shadow-[#00688f]/30 cursor-pointer text-center active:scale-98"
            >
              Aceptar Todas
            </button>
          </div>
        </div>
      </div>
    </div>
  </Transition>
</template>
