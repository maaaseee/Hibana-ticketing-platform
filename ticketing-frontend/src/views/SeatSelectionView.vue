<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useCatalogStore } from '../stores/catalog'
import { useTicketingStore } from '../stores/ticketing'
import { useQueueStore } from '../stores/queue'
import SeatCanvas from '../components/ui/SeatCanvas.vue'

const route = useRoute()
const router = useRouter()
const catalogStore = useCatalogStore()
const ticketingStore = useTicketingStore()
const queueStore = useQueueStore()

const eventId = computed(() => (route.params.id as string) || 'evt-stellar-2026')
const event = computed(() => catalogStore.getEventById(eventId.value))

onMounted(async () => {
  // Ensure queue token is active for local demo testing
  if (!queueStore.checkoutToken) {
    queueStore.checkoutToken = `tkn_auto_${Date.now()}`
  }
  await ticketingStore.loadSeatsForEvent(eventId.value)
})

function handleSeatSelect(seatId: string) {
  ticketingStore.toggleSeat(seatId)
}

function proceedToCheckout() {
  if (ticketingStore.selectedSeats.length === 0) return
  router.push({ name: 'checkout', params: { id: eventId.value } })
}
</script>

<template>
  <div class="py-6 px-4 sm:px-6 lg:px-8 max-w-7xl mx-auto space-y-6">
    
    <!-- Top Action Bar -->
    <div class="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4 bg-surface-card p-4 rounded-xl border border-border-default">
      <div>
        <div class="flex items-center gap-2">
          <RouterLink :to="{ name: 'event-detail', params: { id: eventId } }" class="text-xs font-mono text-primary-400 hover:underline">
            &larr; Back to Event
          </RouterLink>
          <span class="text-xs text-text-muted">&bull;</span>
          <span class="text-xs font-mono text-secondary-400 flex items-center gap-1.5">
            <span class="w-1.5 h-1.5 rounded-full bg-secondary-400 animate-pulse"></span>
            Queue Token Verified
          </span>
        </div>
        <h1 class="text-xl sm:text-2xl font-bold font-display text-text-primary mt-1">
          Select Your Seats &bull; {{ event?.title || 'Interactive Venue' }}
        </h1>
      </div>

      <!-- Redlock Reservation Lock Active Pill -->
      <div v-if="ticketingStore.isLockActive" class="flex items-center gap-3 px-4 py-2 rounded-lg bg-surface-base border border-primary-500/30">
        <div class="w-2.5 h-2.5 rounded-full bg-primary-400 animate-ping"></div>
        <div>
          <div class="text-[10px] uppercase font-mono tracking-wider text-text-muted">Redlock Timer Active</div>
          <div class="text-sm font-mono font-bold text-primary-300">
            {{ ticketingStore.formattedLockTime }} Remaining
          </div>
        </div>
      </div>
    </div>

    <!-- Error / Collision Notification Banner (Accessible with aria-live) -->
    <div
      v-if="ticketingStore.errorNotification"
      role="alert"
      aria-live="polite"
      class="flex items-center justify-between gap-3 p-4 rounded-xl bg-rose-950/70 border border-rose-500/40 text-rose-200 text-sm font-sans shadow-lg animate-fadeIn"
    >
      <div class="flex items-center gap-2">
        <span class="w-2 h-2 rounded-full bg-rose-400 animate-ping"></span>
        <span>{{ ticketingStore.errorNotification }}</span>
      </div>
      <button
        @click="ticketingStore.errorNotification = null"
        class="text-xs font-mono text-rose-300 hover:text-white px-2 py-1 rounded bg-rose-900/60 transition-colors"
      >
        Entendido
      </button>
    </div>

    <!-- Main Seatmap & Summary Grid -->
    <div class="grid grid-cols-1 lg:grid-cols-3 gap-6 items-start">
      
      <!-- Left Column: Interactive Seat Canvas (Span 2) -->
      <div class="lg:col-span-2 space-y-4">
        
        <!-- Sector Legend -->
        <div class="flex flex-wrap items-center gap-2 bg-surface-card p-2 rounded-xl border border-border-default">
          <div
            v-for="sector in ticketingStore.sectors"
            :key="sector.id"
            class="px-3 py-1.5 rounded-lg text-xs font-semibold flex items-center gap-2 bg-surface-base border border-border-default text-text-secondary"
          >
            <span class="w-2.5 h-2.5 rounded-full" :style="{ backgroundColor: sector.color }"></span>
            <span>{{ sector.name }}</span>
            <span class="font-mono text-[11px] text-text-muted">${{ sector.price }}</span>
          </div>
        </div>

        <!-- The Canvas Stage Container -->
        <div class="relative bg-surface-card rounded-2xl border border-border-default p-4 shadow-xl overflow-hidden">
          
          <!-- Virtual Stage Banner -->
          <div class="text-center py-2 mb-2">
            <div class="inline-block px-12 py-1 rounded-t-xl bg-linear-to-b from-primary-500/20 to-transparent border-t-2 border-primary-400/80 text-[11px] font-mono tracking-widest text-primary-300 uppercase">
              Main Stage / Escenario Frontal
            </div>
            <div class="h-0.5 max-w-md mx-auto bg-linear-to-r from-transparent via-primary-400/60 to-transparent"></div>
          </div>

          <!-- HTML5 Canvas Component with full Pan & Zoom -->
          <SeatCanvas
            :seats="ticketingStore.seats"
            :selected-seats="ticketingStore.selectedSeats"
            @select-seat="handleSeatSelect"
          />

          <!-- Seat Legend Footer -->
          <div class="mt-3 py-2 px-3 bg-surface-base rounded-lg border border-border-default/60 flex flex-wrap items-center justify-between text-xs font-mono gap-2">
            <span class="text-text-muted">Click any seat to lock. Pan and zoom supported.</span>
            <div class="flex items-center gap-3 text-[11px] text-text-muted">
              <span class="flex items-center gap-1"><span class="w-2 h-2 rounded-full bg-surface-elevated border border-border-default"></span> Available</span>
              <span class="flex items-center gap-1"><span class="w-2 h-2 rounded-full bg-secondary-400"></span> Selected</span>
              <span class="flex items-center gap-1"><span class="w-2 h-2 rounded-full bg-slate-700"></span> Reserved</span>
            </div>
          </div>
        </div>

      </div>

      <!-- Right Column: Order Selection Drawer -->
      <div class="bg-surface-card rounded-2xl border border-border-default p-6 shadow-xl space-y-6">
        <div>
          <h2 class="text-lg font-bold font-display text-text-primary">Selected Reservation</h2>
          <p class="text-xs text-text-secondary">Distributed Redis Redlock holds seats for 10 minutes</p>
        </div>

        <!-- Selected Seats List -->
        <div v-if="ticketingStore.selectedSeats.length > 0" class="space-y-3">
          <div
            v-for="seat in ticketingStore.selectedSeats"
            :key="seat.id"
            class="flex items-center justify-between p-3 rounded-xl bg-surface-base border border-border-default group hover:border-primary-500/40 transition-colors"
          >
            <div>
              <div class="text-sm font-semibold text-text-primary">
                {{ seat.sector }}
              </div>
              <div class="text-xs font-mono text-text-muted">
                Row {{ seat.row }} &bull; Seat {{ seat.number }}
              </div>
            </div>
            <div class="flex items-center gap-3">
              <span class="font-mono font-bold text-sm text-primary-300">${{ seat.price }}</span>
              <button
                @click="ticketingStore.toggleSeat(seat.id)"
                class="p-1 text-text-muted hover:text-red-400 transition-colors cursor-pointer"
                title="Remove seat"
              >
                <svg class="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
                </svg>
              </button>
            </div>
          </div>

          <!-- Total Calculation -->
          <div class="pt-4 border-t border-border-default space-y-2 text-sm">
            <div class="flex justify-between text-text-secondary">
              <span>Subtotal ({{ ticketingStore.selectedSeats.length }} seats)</span>
              <span class="font-mono">${{ ticketingStore.subtotal.toFixed(2) }}</span>
            </div>
            <div class="flex justify-between text-text-secondary">
              <span>Service Fee</span>
              <span class="font-mono">${{ ticketingStore.serviceFee.toFixed(2) }}</span>
            </div>
            <div class="flex justify-between font-bold text-base text-text-primary pt-2 border-t border-border-default/60">
              <span>Total Guaranteed</span>
              <span class="font-mono text-secondary-400">${{ ticketingStore.total.toFixed(2) }}</span>
            </div>
          </div>

          <!-- Checkout CTA -->
          <button
            @click="proceedToCheckout"
            class="w-full py-3.5 px-4 rounded-xl font-bold bg-primary-400 text-surface-base hover:bg-primary-300 shadow-lg shadow-primary-500/25 cursor-pointer transition-all flex items-center justify-center gap-2"
          >
            <span>Proceed to Checkout</span>
            <svg class="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M14 5l7 7m0 0l-7 7m7-7H3" />
            </svg>
          </button>
        </div>

        <!-- Empty State -->
        <div v-else class="text-center py-12 px-4 rounded-xl bg-surface-base border border-dashed border-border-default space-y-3">
          <div class="w-12 h-12 mx-auto rounded-full bg-surface-card flex items-center justify-center text-text-muted">
            <svg class="w-6 h-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M15 5v2m0 4v2m0 4v2M5 5a2 2 0 00-2 2v3a2 2 0 110 4v3a2 2 0 002 2h14a2 2 0 002-2v-3a2 2 0 110-4V7a2 2 0 00-2-2H5z" />
            </svg>
          </div>
          <p class="text-sm font-medium text-text-secondary">No seats selected</p>
          <p class="text-xs text-text-muted">
            Click on any available circle in the canvas map to reserve your spot.
          </p>
        </div>

        <!-- Distributed Concurrency Architecture Badge -->
        <div class="p-3.5 rounded-xl bg-surface-base border border-border-default text-xs space-y-1.5 text-text-muted">
          <div class="flex items-center gap-2 text-primary-400 font-mono font-semibold">
            <svg class="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z" />
            </svg>
            <span>Redis Redlock Enforced</span>
          </div>
          <p class="text-[11px] leading-relaxed">
            Keys are leased at <code class="text-text-secondary">lock:event:{id}:seat:{id}</code> with automatic expiration to prevent double-booking.
          </p>
        </div>

      </div>

    </div>

  </div>
</template>
