<script setup lang="ts">
import { ref, computed, watch, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useCatalogStore } from '../stores/catalog'
import { useTicketingStore } from '../stores/ticketing'
import { useCheckoutStore } from '../stores/checkout'
import { useTicketsStore } from '../stores/tickets'
import type { EventItem } from '../types'

import CheckoutHeader from '../components/checkout/CheckoutHeader.vue'
import CheckoutStepper from '../components/checkout/CheckoutStepper.vue'
import OrderSummaryCard from '../components/checkout/OrderSummaryCard.vue'
import PaymentTabs from '../components/checkout/PaymentTabs.vue'
import PaymentProcessingOverlay from '../components/checkout/PaymentProcessingOverlay.vue'
import CheckoutModals from '../components/checkout/CheckoutModals.vue'

const route = useRoute()
const router = useRouter()
const catalogStore = useCatalogStore()
const ticketingStore = useTicketingStore()
const checkoutStore = useCheckoutStore()
const ticketsStore = useTicketsStore()

// Event resolution
const eventId = computed(() => (route.params.id as string) || 'chronos-fest-2026')
const event = computed<Partial<EventItem>>(() => {
  const found = catalogStore.getEventById(eventId.value)
  if (found) return found
  return (
    catalogStore.events[0] || {
      id: 'neon-nights-2024',
      title: 'Neon Nights Festival 2024',
      date: '24 Oct, 2024',
      time: '20:00 h',
      venue: 'Arena Central',
      imageUrl:
        'https://images.unsplash.com/photo-1470225620780-dba8ba36b745?auto=format&fit=crop&w=600&q=80',
    }
  )
})

// Wireframe fallback event details
const eventTitle = computed(() => event.value.title || 'Neon Nights Festival 2024')
const eventDateFormatted = computed(() => {
  if (event.value.date) {
    return event.value.time ? `${event.value.date} • ${event.value.time}` : event.value.date
  }
  return '24 Oct, 2024 • 20:00 h'
})
const eventVenue = computed(() => event.value.venue || 'Arena Central')
const eventThumbnail = computed(() => {
  return (
    event.value.imageUrl ||
    'https://images.unsplash.com/photo-1470225620780-dba8ba36b745?auto=format&fit=crop&w=600&q=80'
  )
})

// Selected seats & Add-ons
const selectedSeats = computed(() => ticketingStore.selectedSeats)
const includeParkingPass = ref(true)
const parkingPassPrice = 45.0

// Financial Calculations matching Wireframe ($345 subtotal, $24.15 service fee, $29.53 tax -> $398.68)
const rawSeatsSubtotal = computed(() => {
  if (selectedSeats.value.length > 0) {
    return ticketingStore.subtotal
  }
  // Fallback to match exact wireframe: 2x VIP Access ($300.00)
  return 300.0
})

const subtotal = computed(() => {
  return rawSeatsSubtotal.value + (includeParkingPass.value ? parkingPassPrice : 0)
})

// 7% Service fee (matches wireframe $345 * 0.07 = $24.15)
const serviceFee = computed(() => {
  return Math.round(subtotal.value * 0.07 * 100) / 100
})

// Impuestos (matches wireframe $29.53 for subtotal $345)
const taxes = computed(() => {
  return Math.round((subtotal.value + serviceFee.value) * 0.08 * 100) / 100
})

// Grand Total ($398.68 in wireframe)
const grandTotal = computed(() => {
  return Math.round((subtotal.value + serviceFee.value + taxes.value) * 100) / 100
})

// Redlock 10-minute Timer (Atomic Redis Hold)
const fallbackSeconds = ref(600)
let fallbackTimer: any = null

onMounted(() => {
  if (!ticketingStore.isLockActive) {
    fallbackTimer = setInterval(() => {
      if (fallbackSeconds.value > 0) {
        fallbackSeconds.value--
      } else {
        clearInterval(fallbackTimer)
        showTimeExpiredModal.value = true
      }
    }, 1000)
  }
})

onUnmounted(() => {
  if (fallbackTimer) clearInterval(fallbackTimer)
})

const currentCountdownSeconds = computed(() => {
  return ticketingStore.isLockActive ? ticketingStore.lockTimeRemaining : fallbackSeconds.value
})

const formattedCountdown = computed(() => {
  const totalSecs = currentCountdownSeconds.value
  const mins = Math.floor(totalSecs / 60)
  const secs = totalSecs % 60
  return `${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`
})

const isTimeCritical = computed(() => currentCountdownSeconds.value <= 120)

// Payment form state
const selectedPaymentTab = ref<'credit_card' | 'pocket_wallet' | 'bank_transfer'>('credit_card')
const cardNumber = ref('')
const cardHolder = ref('CARLOS MENDOZA')
const cardExpiry = ref('12/28')
const cardCvc = ref('888')
const saveCard = ref(true)

// Formatting helpers
function formatCardNumber(e: Event) {
  const target = e.target as HTMLInputElement
  let raw = target.value.replace(/\D/g, '')
  const isAmex = /^3[47]/.test(raw)
  if (isAmex) {
    raw = raw.slice(0, 15)
    if (raw.length > 10) {
      cardNumber.value = `${raw.slice(0, 4)} ${raw.slice(4, 10)} ${raw.slice(10)}`
    } else if (raw.length > 4) {
      cardNumber.value = `${raw.slice(0, 4)} ${raw.slice(4)}`
    } else {
      cardNumber.value = raw
    }
  } else {
    raw = raw.slice(0, 16)
    const parts = raw.match(/[\s\S]{1,4}/g) || []
    cardNumber.value = parts.join(' ')
  }
}

function formatExpiry(e: Event) {
  const target = e.target as HTMLInputElement
  let raw = target.value.replace(/\D/g, '').slice(0, 4)
  if (raw.length >= 2) {
    raw = `${raw.slice(0, 2)}/${raw.slice(2)}`
  }
  cardExpiry.value = raw
}

function formatCvc(e: Event) {
  const target = e.target as HTMLInputElement
  const isAmex = /^3[47]/.test(cardNumber.value.replace(/\D/g, ''))
  cardCvc.value = target.value.replace(/\D/g, '').slice(0, isAmex ? 4 : 3)
}

// Processing & Modals
const isProcessing = ref(false)
const orderCompletedOpen = ref(false)
const showTimeExpiredModal = ref(false)
const orderId = ref('')
const copiedNotice = ref('')

watch(
  () => ticketingStore.isTimerExpired,
  (expired) => {
    if (expired && !orderCompletedOpen.value) {
      showTimeExpiredModal.value = true
    }
  }
)

function handleBack() {
  if (window.history.length > 1) {
    router.back()
  } else {
    router.push({ name: 'seat-selection', params: { id: eventId.value } })
  }
}

function handleReturnToSeats() {
  showTimeExpiredModal.value = false
  ticketingStore.resetAndRestartTimer(600)
  router.push({ name: 'seat-selection', params: { id: eventId.value } })
}

async function handleCompleteOrder() {
  isProcessing.value = true

  // Sync payment method with Pinia store
  checkoutStore.paymentMethod = selectedPaymentTab.value
  const success = checkoutStore.completeOrder(grandTotal.value)

  if (success) {
    const newOrderId = `ORD-${Date.now().toString().slice(-5)}`
    orderId.value = newOrderId

    const currentEvent = event.value || catalogStore.events[0]

    // Confirm remote reservation holds
    if (selectedSeats.value.length > 0) {
      await ticketingStore.confirmAllActiveReservations(newOrderId)
    }

    // Add issued tickets into the user's digital wallet
    const seatsToIssue =
      selectedSeats.value.length > 0
        ? selectedSeats.value
        : [
            { id: 'seat-wireframe-1', sector: 'VIP Access', row: '12', number: 'A', price: 150 },
            { id: 'seat-wireframe-2', sector: 'VIP Access', row: '12', number: 'B', price: 150 },
          ]

    ticketsStore.addIssuedTickets(
      eventTitle.value,
      eventDateFormatted.value,
      eventVenue.value,
      seatsToIssue,
      newOrderId,
      checkoutStore.buyerName,
      checkoutStore.buyerEmail
    )

    // Simulate secure transaction validation as in wireframe
    setTimeout(() => {
      ticketingStore.releaseExpiredLocks()
      isProcessing.value = false
      orderCompletedOpen.value = true
    }, 2800)
  } else {
    isProcessing.value = false
  }
}

function navigateToTickets() {
  orderCompletedOpen.value = false
  router.push({ name: 'tickets' })
}

function copyToClipboard(text: string, label: string) {
  navigator.clipboard.writeText(text).then(() => {
    copiedNotice.value = `¡${label} copiado!`
    setTimeout(() => {
      copiedNotice.value = ''
    }, 2500)
  })
}
</script>

<template>
  <div class="min-h-screen flex flex-col bg-[#0c141f] text-[#f1f5f9] selection:bg-[#00688f] selection:text-white font-body">
    <!-- Minimal Header for Checkout -->
    <CheckoutHeader
      :is-time-critical="isTimeCritical"
      :formatted-countdown="formattedCountdown"
      @back="handleBack"
    />

    <!-- Main Content -->
    <main class="flex-grow w-full max-w-[1280px] mx-auto px-4 sm:px-6 md:px-12 py-8 md:py-12 grid grid-cols-1 lg:grid-cols-12 gap-8">
      <!-- Progress Stepper -->
      <CheckoutStepper />

      <!-- Left Column: Order Summary -->
      <OrderSummaryCard
        :event-title="eventTitle"
        :event-date-formatted="eventDateFormatted"
        :event-venue="eventVenue"
        :event-thumbnail="eventThumbnail"
        :selected-seats="selectedSeats"
        v-model:include-parking-pass="includeParkingPass"
        :subtotal="subtotal"
        :service-fee="serviceFee"
        :taxes="taxes"
        :grand-total="grandTotal"
      />

      <!-- Right Column: Payment Methods -->
      <PaymentTabs
        v-model:selected-payment-tab="selectedPaymentTab"
        v-model:card-number="cardNumber"
        v-model:card-holder="cardHolder"
        v-model:card-expiry="cardExpiry"
        v-model:card-cvc="cardCvc"
        v-model:save-card="saveCard"
        :pocket-balance="checkoutStore.pocketBalance"
        :grand-total="grandTotal"
        :copied-notice="copiedNotice"
        :is-processing="isProcessing"
        @format-card-number="formatCardNumber"
        @format-expiry="formatExpiry"
        @format-cvc="formatCvc"
        @copy="copyToClipboard"
        @complete-order="handleCompleteOrder"
      />
    </main>

    <!-- Processing Overlay (Simulating Secure Saga Validation) -->
    <PaymentProcessingOverlay :is-processing="isProcessing" />

    <!-- Modals -->
    <CheckoutModals
      v-model:order-completed-open="orderCompletedOpen"
      v-model:show-time-expired-modal="showTimeExpiredModal"
      :order-id="orderId"
      :grand-total="grandTotal"
      :buyer-email="checkoutStore.buyerEmail"
      @navigate-to-tickets="navigateToTickets"
      @return-to-seats="handleReturnToSeats"
    />
  </div>
</template>
