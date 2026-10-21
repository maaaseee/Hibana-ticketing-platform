<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useCatalogStore } from '../stores/catalog'
import { useQueueStore } from '../stores/queue'

const route = useRoute()
const router = useRouter()
const catalogStore = useCatalogStore()
const queueStore = useQueueStore()

const eventId = computed(() => (route.params.id as string) || 'chronos-fest-2026')
const event = computed(() => catalogStore.getEventById(eventId.value) || catalogStore.events[0])

onMounted(() => {
  queueStore.enterQueue(eventId.value)
})

import { onMounted } from 'vue'

function proceedToSeatSelection() {
  if (queueStore.checkoutToken) {
    router.push({ name: 'seat-selection', params: { id: eventId.value } })
  }
}
</script>

<template>
  <div class="relative py-12 px-4 sm:px-6 lg:px-8 max-w-4xl mx-auto min-h-[75vh] flex items-center justify-center">
    <!-- Ambient glow behind card -->
    <div class="absolute -top-12 left-1/2 -translate-x-1/2 w-96 h-96 bg-primary-500/10 rounded-full blur-3xl pointer-events-none"></div>

    <div class="relative w-full bg-surface-card border border-border-default/80 rounded-2xl p-6 sm:p-10 shadow-2xl backdrop-blur-md">
      
      <!-- Event Header Snippet -->
      <div v-if="event" class="flex items-center gap-4 pb-6 border-b border-border-default">
        <img :src="event.imageUrl" :alt="event.title" class="w-16 h-16 rounded-xl object-cover border border-border-default shadow" />
        <div>
          <span class="inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full text-xs font-semibold bg-primary-500/10 text-primary-300 border border-primary-500/20 mb-1">
            <span class="w-1.5 h-1.5 rounded-full bg-primary-400 animate-ping"></span>
            Virtual Waiting Room Active
          </span>
          <h1 class="text-xl sm:text-2xl font-bold font-display text-text-primary tracking-tight">{{ event.title }}</h1>
          <p class="text-xs sm:text-sm text-text-secondary">{{ event.venue }} &bull; {{ event.date }}</p>
        </div>
      </div>

      <!-- Queue State: Active Waiting -->
      <div v-if="queueStore.isInQueue && !queueStore.checkoutToken" class="py-8 space-y-8 text-center">
        <div class="inline-flex items-center justify-center w-20 h-20 rounded-full bg-primary-500/10 border-2 border-primary-500/30 text-primary-400 shadow-inner">
          <svg class="w-10 h-10 animate-spin" fill="none" viewBox="0 0 24 24">
            <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="3"></circle>
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v8H4z"></path>
          </svg>
        </div>

        <div class="space-y-2">
          <p class="text-xs font-mono uppercase tracking-widest text-primary-400">High Demand Traffic Gate</p>
          <h2 class="text-3xl sm:text-4xl font-extrabold font-display text-text-primary">
            You are currently in line
          </h2>
          <p class="text-sm text-text-secondary max-w-lg mx-auto">
            To guarantee fairness and prevent scalper bot floods, admissions are queued via Redis Sorted Sets. Please keep this tab open.
          </p>
        </div>

        <!-- Position & ETA Metrics -->
        <div class="grid grid-cols-2 gap-4 max-w-md mx-auto">
          <div class="p-4 rounded-xl bg-surface-base border border-border-default">
            <p class="text-xs text-text-muted uppercase tracking-wider font-mono">Your Position</p>
            <p class="text-2xl sm:text-3xl font-extrabold font-mono text-primary-300">
              #{{ queueStore.currentPosition.toLocaleString() }}
            </p>
          </div>
          <div class="p-4 rounded-xl bg-surface-base border border-border-default">
            <p class="text-xs text-text-muted uppercase tracking-wider font-mono">Estimated Wait</p>
            <p class="text-2xl sm:text-3xl font-extrabold font-mono text-secondary-400">
              {{ queueStore.formattedEstimatedTime }}
            </p>
          </div>
        </div>

        <!-- Progress Bar -->
        <div class="max-w-md mx-auto space-y-2">
          <div class="flex justify-between text-xs font-mono text-text-muted">
            <span>Admission Progress</span>
            <span>{{ queueStore.progressPercentage }}%</span>
          </div>
          <div class="w-full bg-surface-base h-2.5 rounded-full overflow-hidden border border-border-default">
            <div
              class="h-full bg-gradient-to-r from-primary-500 via-secondary-400 to-primary-300 rounded-full transition-all duration-700 ease-out"
              :style="{ width: `${queueStore.progressPercentage}%` }"
            ></div>
          </div>
        </div>

        <!-- Dev test acceleration button -->
        <div class="pt-2">
          <button
            @click="queueStore.skipQueueForDemo()"
            class="text-xs text-text-muted hover:text-primary-300 underline font-mono cursor-pointer transition-colors"
          >
            [Dev Bypass: Simulate Immediate Admission]
          </button>
        </div>
      </div>

      <!-- Queue State: Admitted -->
      <div v-else-if="queueStore.checkoutToken" class="py-8 space-y-6 text-center">
        <div class="inline-flex items-center justify-center w-20 h-20 rounded-full bg-secondary-500/10 border-2 border-secondary-500/40 text-secondary-400">
          <svg class="w-10 h-10" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7" />
          </svg>
        </div>

        <div class="space-y-2">
          <span class="inline-block px-3 py-1 rounded-full text-xs font-mono font-semibold bg-secondary-500/20 text-secondary-300 border border-secondary-500/30">
            Token Issued: X-Checkout-Token Granted
          </span>
          <h2 class="text-3xl sm:text-4xl font-extrabold font-display text-text-primary">
            It's your turn!
          </h2>
          <p class="text-sm text-text-secondary max-w-md mx-auto">
            Your cryptographic checkout session is verified. You now have access to select your seats on the interactive venue map.
          </p>
        </div>

        <div class="p-3 bg-surface-base rounded-lg border border-border-default max-w-md mx-auto text-left">
          <div class="flex items-center justify-between text-xs font-mono text-text-muted">
            <span>Session Token ID</span>
            <span class="text-secondary-400 font-bold">ACTIVE</span>
          </div>
          <p class="text-xs font-mono text-text-secondary truncate mt-1">
            {{ queueStore.checkoutToken }}
          </p>
        </div>

        <div class="pt-4">
          <button
            @click="proceedToSeatSelection"
            class="w-full sm:w-auto px-8 py-3.5 rounded-xl font-bold bg-gradient-to-r from-primary-400 to-secondary-400 text-surface-base hover:opacity-95 shadow-lg shadow-primary-500/20 cursor-pointer transition-all transform hover:-translate-y-0.5"
          >
            Enter Seat Map & Selection &rarr;
          </button>
        </div>
      </div>

      <!-- Trust & Rules Notice -->
      <div class="mt-8 pt-6 border-t border-border-default/60 flex flex-col sm:flex-row items-center justify-between gap-4 text-xs text-text-muted font-mono">
        <div class="flex items-center gap-2">
          <svg class="w-4 h-4 text-primary-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z" />
          </svg>
          <span>Anti-Bot Cryptographic Check Active</span>
        </div>
        <span>Do not refresh or open multiple windows</span>
      </div>

    </div>
  </div>
</template>
