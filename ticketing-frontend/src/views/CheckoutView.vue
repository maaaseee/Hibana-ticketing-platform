<script setup lang="ts">
import { ref, computed, watch, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useCatalogStore } from '../stores/catalog'
import { useTicketingStore } from '../stores/ticketing'
import { useCheckoutStore } from '../stores/checkout'
import { useTicketsStore } from '../stores/tickets'
import { getVenueById } from '../data/mockVenues'
import type { EventItem, Seat } from '../types'

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

// Currency resolution
const currency = computed(() => event.value?.sectors?.[0]?.currency || 'ARS')

// Selected seats & Add-ons
const selectedSeats = computed(() => ticketingStore.selectedSeats)
const includeParkingPass = ref(true)

// Venue-defined parking price with fallback mockup
const venueParking = computed(() => {
  const vId = event.value?.venueId
  const found = vId ? getVenueById(vId) : null
  return found?.parking || event.value?.parking
})

const parkingPassPrice = computed(() => {
  if (venueParking.value?.price && venueParking.value.price > 0) {
    return venueParking.value.price
  }
  return currency.value === 'USD' ? 45.0 : 15000.0
})

// Administrator-configurable service fee percentage
const serviceFeePercentage = computed(() => {
  return (
    event.value?.serviceFeePercentage ??
    event.value?.salesPolicy?.serviceFeePercentage ??
    10
  )
})

// Financial Calculations: Normal Ticket Price -> Fee at Checkout
const rawSeatsSubtotal = computed(() => {
  if (selectedSeats.value.length > 0) {
    return ticketingStore.subtotal
  }
  // Fallback to match default wireframe if no live seats held
  return currency.value === 'USD' ? 300.0 : 90000.0
})

const subtotal = computed(() => rawSeatsSubtotal.value)

// Costo por servicio de la página por cada entrada comprada (calculado sobre el subtotal de entradas)
const serviceFee = computed(() => {
  return Math.round(subtotal.value * (serviceFeePercentage.value / 100) * 100) / 100
})

// Impuestos (0 por defecto a menos que estén fijados)
const taxes = computed(() => 0)

// Grand Total
const grandTotal = computed(() => {
  const parkingAmount = includeParkingPass.value ? parkingPassPrice.value : 0
  return Math.round((subtotal.value + serviceFee.value + parkingAmount) * 100) / 100
})

// Redlock 10-minute Timer (Atomic Redis Hold)
const fallbackSeconds = ref(600)
let fallbackTimer: ReturnType<typeof setInterval> | null = null

onMounted(() => {
  if (!ticketingStore.isLockActive) {
    fallbackTimer = setInterval(() => {
      if (fallbackSeconds.value > 0) {
        fallbackSeconds.value--
      } else {
        if (fallbackTimer) clearInterval(fallbackTimer)
        showTimeExpiredModal.value = true
      }
    }, 1000)
  }
})

onUnmounted(() => {
  if (fallbackTimer) clearInterval(fallbackTimer)
})

const isTimeCritical = computed(() => {
  const time = ticketingStore.isLockActive ? ticketingStore.lockTimeRemaining : fallbackSeconds.value
  return time <= 120
})

const formattedCountdown = computed(() => {
  if (ticketingStore.isLockActive) {
    return ticketingStore.formattedLockTime
  }
  const mins = Math.floor(fallbackSeconds.value / 60)
  const secs = fallbackSeconds.value % 60
  return `${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`
})

// Payment form state
const selectedPaymentTab = ref<'debit_card' | 'credit_card' | 'mercado_pago'>('debit_card')
const issuingBank = ref<string>('GALICIA')
const installments = ref<number>(1)
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

    // Confirm remote reservation holds
    if (selectedSeats.value.length > 0) {
      await ticketingStore.confirmAllActiveReservations(newOrderId)
    }

    // Add issued tickets into the user's digital wallet
    const seatsToIssue: Seat[] =
      selectedSeats.value.length > 0
        ? selectedSeats.value
        : [
            { id: 'seat-wireframe-1', sector: 'VIP Access', row: '12', number: 1, price: 150, status: 'RESERVED', x: 0, y: 0 },
            { id: 'seat-wireframe-2', sector: 'VIP Access', row: '12', number: 2, price: 150, status: 'RESERVED', x: 0, y: 0 },
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
        :service-fee-percentage="serviceFeePercentage"
        :parking-pass-price="parkingPassPrice"
        :parking-title="venueParking?.title"
        :parking-description="venueParking?.description"
        :currency="currency"
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
        v-model:issuing-bank="issuingBank"
        v-model:installments="installments"
        :grand-total="grandTotal"
        :is-processing="isProcessing"
        @format-card-number="formatCardNumber"
        @format-expiry="formatExpiry"
        @format-cvc="formatCvc"
        @complete-order="handleCompleteOrder"
      />
    </main>

    <!-- Modals and Overlays -->
    <PaymentProcessingOverlay :is-processing="isProcessing" />

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
