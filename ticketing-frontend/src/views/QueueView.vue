<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useCatalogStore } from '../stores/catalog'
import { useQueueStore } from '../stores/queue'

import QueueHeader from '../components/queue/QueueHeader.vue'
import QueueGyroscopeGauge from '../components/queue/QueueGyroscopeGauge.vue'
import QueueAdmittedCard from '../components/queue/QueueAdmittedCard.vue'
import QueueExitModal from '../components/queue/QueueExitModal.vue'

const route = useRoute()
const router = useRouter()
const catalogStore = useCatalogStore()
const queueStore = useQueueStore()

const eventId = computed(() => (route.params.id as string) || 'chronos-fest-2026')
const event = computed(() => catalogStore.getEventById(eventId.value) || catalogStore.events[0])

// Modal de advertencia para salir al home sin perder el lugar
const showExitModal = ref(false)

function handleLogoClick() {
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
  <div class="relative w-screen h-screen overflow-hidden bg-canvas-base text-high-emphasis font-body selection:bg-primary-container selection:text-white flex flex-col justify-between p-4 sm:p-6 md:p-10 select-none antialiased">
    <!-- Background Layer: Subtle concert backdrop with deep vignette -->
    <div class="absolute inset-0 z-0 pointer-events-none">
      <div
        v-if="event?.imageUrl"
        class="absolute inset-0 bg-cover bg-center opacity-20 blur-3xl scale-110 transition-opacity duration-1000"
        :style="{ backgroundImage: `url(${event.imageUrl})` }"
      ></div>
      <div class="absolute inset-0 bg-linear-to-b from-canvas-base/85 via-canvas-base/95 to-canvas-base"></div>
      <div class="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 w-[600px] h-[600px] bg-primary-container/10 rounded-full blur-[140px]"></div>
    </div>

    <!-- Header: Minimalist Navbar with only interactive Logo and Exit Warning -->
    <QueueHeader @logo-click="handleLogoClick" />

    <!-- Main Content Container: Waiting Room Card -->
    <main class="relative z-10 w-full flex flex-col items-center justify-center my-auto">
      <div class="bg-surface-card border border-border-subtle rounded-3xl p-6 sm:p-10 flex flex-col items-center max-w-lg w-full text-center shadow-[0px_8px_32px_rgba(0,0,0,0.55)] backdrop-blur-md">
        <!-- State A: Active Queue Waiting -->
        <QueueGyroscopeGauge
          v-if="!queueStore.checkoutToken"
          :current-position="queueStore.currentPosition"
          :progress-percentage="queueStore.progressPercentage"
          :formatted-estimated-time="queueStore.formattedEstimatedTime"
          :event="event"
          @skip-demo="queueStore.skipQueueForDemo()"
        />

        <!-- State B: Admitted / Turn Ready -->
        <QueueAdmittedCard
          v-else
          :checkout-token="queueStore.checkoutToken"
          @proceed="proceedToSeatSelection"
        />
      </div>
    </main>

    <!-- Trust / Info Footer -->
    <footer class="relative z-10 w-full flex justify-center text-center px-4">
      <p class="font-mono text-xs text-muted-light/80 max-w-md leading-relaxed">
        Sistema oficial de asignación equitativa. No cierres tu navegador para mantener tu posición.
      </p>
    </footer>

    <!-- Radix Vue Dialog: Exit Confirmation Warning Modal -->
    <QueueExitModal
      v-model:open="showExitModal"
      :current-position="queueStore.currentPosition"
      @confirm="confirmExitToHome"
    />
  </div>
</template>
