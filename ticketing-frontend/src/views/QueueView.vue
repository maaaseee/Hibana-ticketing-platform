<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useCatalogStore } from '../stores/catalog'
import { useQueueStore } from '../stores/queue'
import {
  DialogRoot,
  DialogPortal,
  DialogOverlay,
  DialogContent,
  DialogTitle,
  DialogDescription
} from 'radix-vue'

const route = useRoute()
const router = useRouter()
const catalogStore = useCatalogStore()
const queueStore = useQueueStore()
const logoUrl = '/logo.png'

const eventId = computed(() => (route.params.id as string) || 'chronos-fest-2026')
const event = computed(() => catalogStore.getEventById(eventId.value) || catalogStore.events[0])

// Modal de advertencia para salir al home sin perder el lugar
const showExitModal = ref(false)

function handleLogoClick() {
  // Disparar advertencia si está en la cola o ya tiene turno asignado
  showExitModal.value = true
}

function confirmExitToHome() {
  showExitModal.value = false
  queueStore.leaveQueue()
  router.push({ name: 'home' })
}

function proceedToSeatSelection() {
  if (queueStore.checkoutToken) {
    router.push({ name: 'seat-selection', params: { id: eventId.value } })
  }
}

// Advertencia nativa del navegador al intentar cerrar o recargar la pestaña
function handleBeforeUnload(e: BeforeUnloadEvent) {
  if (queueStore.isInQueue && !queueStore.checkoutToken) {
    e.preventDefault()
    e.returnValue = 'Si sales o recargas la página perderás tu posición en la fila virtual.'
    return e.returnValue
  }
}

onMounted(() => {
  queueStore.enterQueue(eventId.value)
  window.addEventListener('beforeunload', handleBeforeUnload)
})

onUnmounted(() => {
  window.removeEventListener('beforeunload', handleBeforeUnload)
})
</script>

<template>
  <div class="relative w-screen h-screen overflow-hidden bg-[#0c141f] text-[#f1f5f9] font-body selection:bg-primary-container selection:text-white flex flex-col justify-between p-4 sm:p-6 md:p-10 select-none antialiased">
    
    <!-- Background Layer: Subtle concert backdrop with deep vignette -->
    <div class="absolute inset-0 z-0 pointer-events-none">
      <div
        v-if="event?.imageUrl"
        class="absolute inset-0 bg-cover bg-center opacity-20 blur-3xl scale-110 transition-opacity duration-1000"
        :style="{ backgroundImage: `url(${event.imageUrl})` }"
      ></div>
      <!-- Deep navy vignette overlay -->
      <div class="absolute inset-0 bg-linear-to-b from-[#0c141f]/85 via-[#0c141f]/95 to-[#0c141f]"></div>
      <!-- Radial ambient light -->
      <div class="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 w-[600px] h-[600px] bg-[#00688f]/10 rounded-full blur-[140px]"></div>
    </div>

    <!-- Header: Minimalist Navbar with only interactive Logo and Exit Warning -->
    <header class="relative z-20 w-full flex items-center justify-between">
      <!-- Logo Button con Advertencia de salida -->
      <button
        @click="handleLogoClick"
        class="flex items-center gap-2.5 sm:gap-3 px-3 py-2 rounded-xl bg-[#141c27]/70 hover:bg-[#1b2533] border border-[#23354d] text-left transition-all group cursor-pointer shadow-lg backdrop-blur-md focus:outline-hidden focus:ring-2 focus:ring-[#86cffb]/50"
        title="Volver al inicio (Aviso: Se perderá el lugar en la fila)"
        data-testid="queue-logo-home-btn"
      >
        <img
          :src="logoUrl"
          alt="Amaryllis"
          class="w-7 h-7 sm:w-8 sm:h-8 object-contain drop-shadow-[0_0_10px_rgba(134,207,251,0.4)] group-hover:scale-105 transition-transform"
        />
        <div class="flex items-center gap-2">
          <span class="font-display text-xl sm:text-2xl font-black text-[#86cffb] tracking-tight group-hover:text-white transition-colors">
            Amaryllis
          </span>
          <span class="w-1.5 h-1.5 rounded-full bg-[#00688f]"></span>
          <span class="font-mono text-[11px] sm:text-xs text-[#94a3b8] uppercase tracking-wider font-semibold">
            Queue System
          </span>
        </div>
      </button>

      <!-- Live High Demand Tag -->
      <div class="hidden sm:flex items-center gap-2 px-3 py-1.5 rounded-full bg-[#141c27]/80 border border-[#23354d] text-xs font-mono text-[#cbd5e1] backdrop-blur-md shadow-sm">
        <span class="w-2 h-2 rounded-full bg-emerald-400 animate-pulse"></span>
        <span>Sala de Espera Concurrente • Redis Gate</span>
      </div>
    </header>

    <!-- Main Content Container: Waiting Room Card matching wireframe -->
    <main class="relative z-10 w-full flex flex-col items-center justify-center my-auto">
      <div class="bg-[#141c27] border border-[#23354d] rounded-3xl p-6 sm:p-10 flex flex-col items-center max-w-lg w-full text-center shadow-[0px_8px_32px_rgba(0,0,0,0.55)] backdrop-blur-md">
        
        <!-- State A: Active Queue Waiting -->
        <template v-if="!queueStore.checkoutToken">
          <!-- Icon / Status Indicator -->
          <div class="w-14 h-14 rounded-full bg-[#1b2533] border border-[#23354d] flex items-center justify-center mb-5 shadow-inner">
            <span class="material-symbols-outlined text-[#86cffb] text-[28px] animate-pulse">hourglass_empty</span>
          </div>

          <!-- Headline & Subtitle -->
          <h1 class="font-display text-2xl sm:text-3xl font-bold text-[#f1f5f9] mb-2 tracking-tight">
            Estás en la fila
          </h1>
          <p class="font-body text-sm sm:text-base text-[#94a3b8] mb-6 sm:mb-8">
            Por favor no cierres ni actualices esta página.
          </p>

          <!-- Circular Progress / Gyroscope Status Area -->
          <div class="relative w-52 h-52 sm:w-56 sm:h-56 flex items-center justify-center mb-7 sm:mb-8">
            <!-- Clean solid outer ring -->
            <div class="absolute inset-0 rounded-full border border-[#23354d]"></div>
            
            <!-- Dashed counter-rotating ring -->
            <div
              class="absolute inset-3 rounded-full border border-dashed border-[#23354d]/80 animate-spin-slow"
              style="animation-direction: reverse;"
            ></div>
            
            <!-- Rotating accent active ring -->
            <div class="absolute inset-0 rounded-full border-2 border-t-[#00688f] border-r-[#00688f] border-b-transparent border-l-transparent animate-spin-slow"></div>

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
                :stroke-dashoffset="276.46 - (276.46 * queueStore.progressPercentage) / 100"
                stroke-linecap="round"
                class="transition-all duration-700 ease-out opacity-80"
              />
            </svg>

            <!-- Center Status Info -->
            <div class="relative z-10 flex flex-col items-center">
              <span class="font-mono text-xs text-[#94a3b8] uppercase tracking-widest mb-1 font-semibold">
                Tu posición
              </span>
              <span class="font-display text-4xl sm:text-5xl font-extrabold text-[#f1f5f9] tabular-nums tracking-tight">
                {{ queueStore.currentPosition.toLocaleString('es-AR') }}
              </span>
              <span v-if="event" class="font-mono text-[10px] text-[#86cffb]/80 mt-1 max-w-[140px] truncate">
                {{ event.title }}
              </span>
            </div>
          </div>

          <!-- Estimated Time Footer Box -->
          <div class="w-full bg-[#1b2533] border border-[#23354d] flex flex-col items-center py-3.5 px-4 rounded-xl shadow-inner">
            <div class="flex items-center gap-2 text-[#f1f5f9]">
              <span class="material-symbols-outlined text-[18px] text-[#86cffb]">schedule</span>
              <span class="font-mono text-xs sm:text-sm font-medium text-[#cbd5e1]">
                Tiempo estimado: <strong class="text-[#86cffb] font-semibold">{{ queueStore.formattedEstimatedTime }}</strong>
              </span>
            </div>
            
            <div class="mt-2.5 w-full max-w-[220px] h-1.5 bg-[#0c141f] border border-[#23354d]/60 rounded-full overflow-hidden">
              <div
                class="h-full bg-linear-to-r from-[#00688f] via-[#4fdbc8] to-[#86cffb] rounded-full transition-all duration-700 ease-out"
                :style="{ width: `${queueStore.progressPercentage}%` }"
              ></div>
            </div>
          </div>

          <!-- Dev Test Acceleration Shortcut -->
          <button
            @click="queueStore.skipQueueForDemo()"
            class="mt-4 text-xs text-[#64748b] hover:text-[#86cffb] font-mono underline cursor-pointer transition-colors"
            title="Acelerar turno para pruebas locales"
          >
            [Simular turno inmediato (Demo)]
          </button>
        </template>

        <!-- State B: Admitted / Turn Ready -->
        <template v-else>
          <!-- Verified Success Icon -->
          <div class="w-16 h-16 rounded-full bg-emerald-950/80 border-2 border-emerald-500/50 flex items-center justify-center mb-5 text-emerald-400 shadow-lg shadow-emerald-500/20 animate-bounce">
            <span class="material-symbols-outlined text-[34px]">verified</span>
          </div>

          <span class="inline-block px-3 py-1 rounded-full text-xs font-mono font-bold bg-emerald-950 text-emerald-300 border border-emerald-500/40 mb-2">
            Token Emitido • X-Checkout-Token Habilitado
          </span>

          <h2 class="font-display text-3xl sm:text-4xl font-extrabold text-[#f1f5f9] mb-2 tracking-tight">
            ¡Es tu turno!
          </h2>
          <p class="font-body text-sm text-[#94a3b8] mb-6 max-w-md leading-relaxed">
            Tu sesión criptográfica ha sido validada. Cuentas con 15 minutos reservados para seleccionar tus butacas en el mapa interactivo.
          </p>

          <!-- Token ID Info Card -->
          <div class="w-full bg-[#1b2533] border border-[#23354d] p-3.5 rounded-xl text-left mb-6 font-mono text-xs">
            <div class="flex items-center justify-between text-[#94a3b8] mb-1">
              <span>Token de Seguridad</span>
              <span class="text-emerald-400 font-bold">ACTIVO</span>
            </div>
            <p class="text-white text-[11px] truncate bg-[#0c141f] p-2 rounded-lg border border-[#23354d]/70 font-mono">
              {{ queueStore.checkoutToken }}
            </p>
          </div>

          <!-- Action Button to proceed to seat selection -->
          <button
            @click="proceedToSeatSelection"
            class="w-full py-3.5 px-8 rounded-full font-display font-extrabold text-sm sm:text-base bg-btn-buy hover:bg-btn-buy-hover text-white transition-all shadow-lg hover:shadow-sky-500/25 active:scale-95 cursor-pointer flex items-center justify-center gap-2"
          >
            <span>Seleccionar mis butacas</span>
            <span class="material-symbols-outlined text-lg">arrow_forward</span>
          </button>
        </template>

      </div>
    </main>

    <!-- Trust / Info Footer matching wireframe -->
    <footer class="relative z-10 w-full flex justify-center text-center px-4">
      <p class="font-mono text-xs text-[#94a3b8]/80 max-w-md leading-relaxed">
        Protegido por el sistema de colas de alta disponibilidad de Amaryllis (Redis Sorted Sets). Las asignaciones de asientos son definitivas una vez iniciado el pago.
      </p>
    </footer>

    <!-- Radix Vue Dialog: Exit Confirmation Warning Modal -->
    <DialogRoot v-model:open="showExitModal">
      <DialogPortal>
        <DialogOverlay class="fixed inset-0 z-50 bg-black/80 backdrop-blur-xs animate-fadeIn" />
        <DialogContent class="fixed top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 z-50 bg-[#141c27] border border-[#23354d] rounded-2xl max-w-md w-[92vw] p-6 space-y-4 shadow-2xl outline-none">
          
          <div class="flex items-center gap-3 pb-2 border-b border-[#23354d]">
            <div class="w-10 h-10 rounded-xl bg-amber-950/80 border border-amber-500/40 flex items-center justify-center text-amber-400 shrink-0">
              <span class="material-symbols-outlined text-2xl">warning</span>
            </div>
            <DialogTitle class="text-lg font-bold text-white font-display">
              ¿Deseas salir de la fila virtual?
            </DialogTitle>
          </div>

          <DialogDescription class="text-xs text-[#94a3b8] font-mono leading-relaxed">
            Actualmente te encuentras en la posición <strong class="text-white">#{{ queueStore.currentPosition.toLocaleString('es-AR') }}</strong>. Si sales al home ahora, <span class="text-rose-400 font-bold">perderás tu lugar en la fila</span> y tendrás que volver a ingresar desde el final si decides volver.
          </DialogDescription>

          <div class="flex items-center justify-end gap-3 pt-3 font-mono text-xs">
            <button
              @click="showExitModal = false"
              class="px-4 py-2.5 rounded-xl bg-[#1b2533] hover:bg-[#23354d] text-white border border-[#23354d] cursor-pointer transition-colors font-semibold"
            >
              Continuar en la fila
            </button>
            <button
              @click="confirmExitToHome"
              class="px-4 py-2.5 rounded-xl bg-rose-700 hover:bg-rose-600 text-white font-bold cursor-pointer transition-colors shadow-md"
              data-testid="confirm-exit-queue-btn"
            >
              Salir y perder mi lugar
            </button>
          </div>
        </DialogContent>
      </DialogPortal>
    </DialogRoot>

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
