<script setup lang="ts">
import { ref, computed, watch, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useCatalogStore } from '../stores/catalog'
import { useTicketingStore } from '../stores/ticketing'
import { useCheckoutStore } from '../stores/checkout'
import { useTicketsStore } from '../stores/tickets'
import { orderService, type PaymentMethodStatusDto } from '../services/orderService'
import { ApiError } from '../services/apiClient'
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

// Costo por servicio de la página por cada entrada comprada
const serviceFee = computed(() => {
  return (rawSeatsSubtotal.value * serviceFeePercentage.value) / 100
})

// Impuestos locales / IVA (21% del total)
const taxes = computed(() => {
  const taxableBase =
    subtotal.value +
    serviceFee.value +
    (includeParkingPass.value ? parkingPassPrice.value : 0)
  return taxableBase * 0.21
})

const grandTotal = computed(() => {
  return (
    subtotal.value +
    serviceFee.value +
    (includeParkingPass.value ? parkingPassPrice.value : 0) +
    taxes.value
  )
})

// Timer critical threshold (< 2 mins)
const isTimeCritical = computed(() => ticketingStore.lockTimeRemaining < 120)

const formattedCountdown = computed(() => {
  const mins = Math.floor(ticketingStore.lockTimeRemaining / 60)
  const secs = ticketingStore.lockTimeRemaining % 60
  return `${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`
})

// Payment Form State
const selectedPaymentTab = ref<'debit_card' | 'credit_card' | 'mercado_pago'>('debit_card')
const issuingBank = ref<string>('GALICIA')
const installments = ref<number>(1)
const cardNumber = ref('')
const cardHolder = ref('CARLOS MENDOZA')
const cardExpiry = ref('12/28')
const cardCvc = ref('888')
const saveCard = ref(true)

// Feature flag for payment methods from backend
const paymentMethods = ref<PaymentMethodStatusDto[]>([])
const mercadoPagoEnabled = computed(() => {
  const mp = paymentMethods.value.find((m) => m.id === 'MERCADO_PAGO')
  return mp ? mp.enabled : false
})

onMounted(async () => {
  paymentMethods.value = await orderService.getPaymentMethods()
})

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
const checkoutError = ref<string | null>(null)

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
  if (selectedPaymentTab.value === 'mercado_pago' && !mercadoPagoEnabled.value) {
    checkoutError.value =
      'Mercado Pago se encuentra en fase de homologación para el MVP. Por favor selecciona Débito o Crédito.'
    return
  }

  isProcessing.value = true
  checkoutError.value = null

  // 1. Identify primary held seat & reservation ID
  const primaryHold = ticketingStore.activeHolds[0]
  const primarySeat = selectedSeats.value[0]

  const reservationId = primaryHold?.reservationId || `res_${Date.now()}`
  const seatId = primaryHold?.seatId || primarySeat?.id || 'seat-default'

  // Map selected payment tab to backend PaymentMethod enum
  const paymentMethod: 'DEBIT_CARD' | 'CREDIT_CARD' | 'MERCADO_PAGO' =
    selectedPaymentTab.value === 'debit_card'
      ? 'DEBIT_CARD'
      : selectedPaymentTab.value === 'credit_card'
      ? 'CREDIT_CARD'
      : 'MERCADO_PAGO'

  try {
    // 2. Call Orders Backend: POST /api/v1/orders
    const createdOrder = await orderService.createOrder({
      reservationId,
      eventId: eventId.value,
      seatId,
      totalAmount: grandTotal.value,
      subtotalAmount: subtotal.value,
      serviceFeeAmount: serviceFee.value,
    })

    const realOrderId = createdOrder.orderId
    orderId.value = realOrderId
    checkoutStore.lastOrderId = realOrderId

    // 3. Call Orders Backend Settlement: POST /api/v1/orders/{orderId}/pay
    const paymentResult = await orderService.processPayment(realOrderId, {
      paymentMethod,
      buyerEmail: checkoutStore.buyerEmail,
      buyerName: checkoutStore.buyerName,
      buyerTaxId: checkoutStore.buyerDni,
      installments: installments.value,
      issuingBank: issuingBank.value,
    })

    if (paymentResult.status === 'PAID') {
      // 4. Confirm remote reservation holds in Inventory (Redlock hold -> Booked)
      if (selectedSeats.value.length > 0) {
        await ticketingStore.confirmAllActiveReservations(realOrderId)
      }

      // 5. Add issued tickets into the user's digital wallet in Pinia
      const seatsToIssue: Seat[] =
        selectedSeats.value.length > 0
          ? selectedSeats.value
          : [
              {
                id: 'seat-ticket-1',
                sector: 'Acceso General',
                row: '1',
                number: 1,
                price: grandTotal.value,
                status: 'RESERVED',
                x: 0,
                y: 0,
              },
            ]

      ticketsStore.addIssuedTickets(
        eventTitle.value,
        eventDateFormatted.value,
        eventVenue.value,
        seatsToIssue,
        realOrderId,
        checkoutStore.buyerName,
        checkoutStore.buyerEmail
      )

      ticketingStore.releaseExpiredLocks()
      checkoutStore.isCompleted = true
      isProcessing.value = false
      orderCompletedOpen.value = true
    } else {
      throw new Error(`El pago no pudo completarse. Estado: ${paymentResult.status}`)
    }
  } catch (err: unknown) {
    console.error('[CHECKOUT-ERROR] Error procesando la orden en el backend:', err)
    let msg = 'Hubo un inconveniente al procesar tu orden. Por favor verifica tus datos e intenta nuevamente.'
    if (err instanceof ApiError) {
      msg = err.problem?.detail || err.message || msg
    } else if (err instanceof Error) {
      msg = err.message
    }
    checkoutError.value = msg
    isProcessing.value = false
  }
}

function navigateToTickets() {
  orderCompletedOpen.value = false
  router.push({ name: 'tickets' })
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

      <!-- Error Banner if checkout or payment failed -->
      <div
        v-if="checkoutError"
        class="col-span-1 lg:col-span-12 p-4 rounded-xl bg-red-500/10 border border-red-500/30 flex items-center justify-between gap-3 text-red-300 text-sm"
      >
        <div class="flex items-center gap-2.5">
          <span class="material-symbols-outlined text-red-400">error</span>
          <span>{{ checkoutError }}</span>
        </div>
        <button
          type="button"
          @click="checkoutError = null"
          class="text-xs text-red-400 hover:text-red-200 underline cursor-pointer"
        >
          Descartar
        </button>
      </div>

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
        :mercado-pago-enabled="mercadoPagoEnabled"
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
