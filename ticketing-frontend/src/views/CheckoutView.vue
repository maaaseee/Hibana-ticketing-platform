<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useCatalogStore } from '../stores/catalog'
import { useTicketingStore } from '../stores/ticketing'
import { useCheckoutStore } from '../stores/checkout'
import { useTicketsStore } from '../stores/tickets'

const route = useRoute()
const router = useRouter()
const catalogStore = useCatalogStore()
const ticketingStore = useTicketingStore()
const checkoutStore = useCheckoutStore()
const ticketsStore = useTicketsStore()

const eventId = computed(() => (route.params.id as string) || 'chronos-fest-2026')
const event = computed(() => catalogStore.getEventById(eventId.value) ?? catalogStore.events[0])

const selectedSeats = computed(() => ticketingStore.selectedSeats)
const subtotal = computed(() => ticketingStore.subtotal)
const fees = computed(() => ticketingStore.serviceFee)
const grandTotal = computed(() => ticketingStore.total)

// Payment form state
const selectedPaymentMethod = ref<'pocket_wallet' | 'credit_card'>('pocket_wallet')
const cardHolder = ref('Carlos Mendoza')
const cardNumber = ref('•••• •••• •••• 4242')
const cardExpiry = ref('12/28')
const cardCvc = ref('888')

const isProcessing = ref(false)
const orderCompleted = ref(false)
const orderId = ref('')

async function handleCompleteOrder() {
  if (selectedSeats.value.length === 0) return
  isProcessing.value = true

  checkoutStore.paymentMethod = selectedPaymentMethod.value
  const success = checkoutStore.completeOrder(grandTotal.value)

  if (success) {
    const newOrderId = `ORD-${Date.now().toString().slice(-5)}`
    orderId.value = newOrderId

    const currentEvent = event.value || catalogStore.events[0]
    if (!currentEvent) {
      isProcessing.value = false
      return
    }

    // Confirm remote reservation in inventory service
    await ticketingStore.confirmAllActiveReservations(newOrderId)

    ticketsStore.addIssuedTickets(
      currentEvent.title,
      currentEvent.date,
      currentEvent.venue,
      selectedSeats.value,
      newOrderId,
      checkoutStore.buyerName,
      checkoutStore.buyerEmail
    )

    setTimeout(() => {
      ticketingStore.releaseExpiredLocks()
      isProcessing.value = false
      orderCompleted.value = true
    }, 1000)
  } else {
    isProcessing.value = false
  }
}

function navigateToTickets() {
  router.push({ name: 'tickets' })
}
</script>

<template>
  <div class="py-8 px-4 sm:px-6 lg:px-8 max-w-5xl mx-auto space-y-8">
    
    <!-- Breadcrumb / Header -->
    <div class="space-y-1">
      <div class="flex items-center gap-2 text-xs font-mono text-text-muted">
        <RouterLink :to="{ name: 'seat-selection', params: { id: eventId } }" class="text-primary-400 hover:underline">
          &larr; Retornar a Selección de Butacas
        </RouterLink>
        <span>&bull;</span>
        <span>Paso 3 de 3: Liquidación & Pago</span>
      </div>
      <h1 class="text-2xl sm:text-3xl font-extrabold font-display text-text-primary">
        Revisar & Finalizar Reserva
      </h1>
    </div>

    <!-- Active Redlock Timer Pill -->
    <div v-if="ticketingStore.isLockActive" class="flex items-center justify-between p-3.5 rounded-xl bg-primary-500/10 border border-primary-500/20 text-xs text-primary-300">
      <div class="flex items-center gap-2">
        <span class="w-2 h-2 rounded-full bg-primary-400 animate-pulse"></span>
        <span>Asientos reservados vía Redis Redlock: <strong>{{ ticketingStore.formattedLockTime }}</strong> restantes</span>
      </div>
      <span class="font-mono text-text-muted hidden sm:inline">Transacción revierte al expirar el tiempo</span>
    </div>

    <!-- Main Grid -->
    <div class="grid grid-cols-1 lg:grid-cols-3 gap-8">
      
      <!-- Left 2 Cols: Payment method & Billing details -->
      <div class="lg:col-span-2 space-y-6">
        
        <!-- Payment Selector -->
        <div class="bg-surface-card rounded-2xl border border-border-default p-6 space-y-5">
          <h2 class="text-base font-bold font-display text-text-primary">Método de Liquidación</h2>

          <div class="grid grid-cols-1 sm:grid-cols-2 gap-4">
            
            <!-- Virtual Wallet Option (com.aieta.pocket) -->
            <label
              :class="[
                'p-4 rounded-xl border cursor-pointer transition-all flex flex-col justify-between space-y-3',
                selectedPaymentMethod === 'pocket_wallet'
                  ? 'border-primary-400 bg-primary-500/10 ring-1 ring-primary-400/30'
                  : 'border-border-default bg-surface-base hover:border-border-default/80'
              ]"
            >
              <div class="flex items-start justify-between">
                <div class="flex items-center gap-2.5">
                  <div class="w-8 h-8 rounded-lg bg-primary-500/20 flex items-center justify-center text-primary-400">
                    <svg class="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 10h18M7 15h1m4 0h1m-7 4h12a3 3 0 003-3V8a3 3 0 00-3-3H6a3 3 0 00-3 3v8a3 3 0 003 3z" />
                    </svg>
                  </div>
                  <div>
                    <p class="text-sm font-bold text-text-primary">Billetera Pocket</p>
                    <p class="text-[11px] font-mono text-text-muted">Partida Doble (com.aieta.pocket)</p>
                  </div>
                </div>
                <input type="radio" value="pocket_wallet" v-model="selectedPaymentMethod" class="accent-primary-400" />
              </div>

              <div class="pt-2 border-t border-border-default/60 flex items-center justify-between text-xs font-mono">
                <span class="text-text-muted">Saldo Disponible:</span>
                <span :class="checkoutStore.pocketBalance >= grandTotal ? 'text-secondary-400 font-bold' : 'text-red-400 font-bold'">
                  ${{ checkoutStore.pocketBalance.toLocaleString('es-AR') }} ARS
                </span>
              </div>
            </label>

            <!-- Credit Card Option -->
            <label
              :class="[
                'p-4 rounded-xl border cursor-pointer transition-all flex flex-col justify-between space-y-3',
                selectedPaymentMethod === 'credit_card'
                  ? 'border-primary-400 bg-primary-500/10 ring-1 ring-primary-400/30'
                  : 'border-border-default bg-surface-base hover:border-border-default/80'
              ]"
            >
              <div class="flex items-start justify-between">
                <div class="flex items-center gap-2.5">
                  <div class="w-8 h-8 rounded-lg bg-secondary-500/20 flex items-center justify-center text-secondary-400">
                    <svg class="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 14l6-6m-5.5.5h.01m4.99 5h.01M19 21H5a2 2 0 01-2-2V5a2 2 0 012-2h14a2 2 0 012 2v14a2 2 0 01-2 2z" />
                    </svg>
                  </div>
                  <div>
                    <p class="text-sm font-bold text-text-primary">Tarjeta de Crédito / Débito</p>
                    <p class="text-[11px] font-mono text-text-muted">Gateway PCI-DSS Nivel 1</p>
                  </div>
                </div>
                <input type="radio" value="credit_card" v-model="selectedPaymentMethod" class="accent-primary-400" />
              </div>

              <div class="pt-2 border-t border-border-default/60 text-xs font-mono text-text-muted">
                Sin comisiones adicionales
              </div>
            </label>

          </div>

          <!-- Card Input Form if credit_card selected -->
          <div v-if="selectedPaymentMethod === 'credit_card'" class="p-4 rounded-xl bg-surface-base border border-border-default space-y-4">
            <div>
              <label class="block text-xs font-medium text-text-secondary mb-1">Titular de la Tarjeta</label>
              <input
                v-model="cardHolder"
                type="text"
                class="w-full px-3 py-2 rounded-lg bg-surface-card border border-border-default text-text-primary text-sm focus:outline-none focus:border-primary-400"
              />
            </div>
            <div>
              <label class="block text-xs font-medium text-text-secondary mb-1">Número de Tarjeta</label>
              <input
                v-model="cardNumber"
                type="text"
                class="w-full px-3 py-2 rounded-lg bg-surface-card border border-border-default text-text-primary text-sm font-mono focus:outline-none focus:border-primary-400"
              />
            </div>
            <div class="grid grid-cols-2 gap-4">
              <div>
                <label class="block text-xs font-medium text-text-secondary mb-1">Vencimiento</label>
                <input
                  v-model="cardExpiry"
                  type="text"
                  class="w-full px-3 py-2 rounded-lg bg-surface-card border border-border-default text-text-primary text-sm font-mono focus:outline-none focus:border-primary-400"
                />
              </div>
              <div>
                <label class="block text-xs font-medium text-text-secondary mb-1">Código de Seguridad (CVV)</label>
                <input
                  v-model="cardCvc"
                  type="password"
                  maxlength="4"
                  class="w-full px-3 py-2 rounded-lg bg-surface-card border border-border-default text-text-primary text-sm font-mono focus:outline-none focus:border-primary-400"
                />
              </div>
            </div>
          </div>

          <!-- Double-entry notice for Wallet -->
          <div v-else class="p-4 rounded-xl bg-surface-base border border-border-default text-xs text-text-secondary space-y-1">
            <span class="font-bold text-primary-300">Asiento Contable Inmutable (Pocket Ledger)</span>
            <p class="text-text-muted leading-relaxed">
              Al confirmar, el servicio financiero generará un débito atómico en tu cuenta y crédito en cuenta escrow en una sola transacción ACID con Transactional Outbox.
            </p>
          </div>
        </div>

        <!-- Billing Info Preview -->
        <div class="bg-surface-card rounded-2xl border border-border-default p-6 space-y-3">
          <h2 class="text-base font-bold font-display text-text-primary">Datos del Titular de Entradas</h2>
          <div class="grid grid-cols-1 sm:grid-cols-2 gap-3 text-sm">
            <div class="p-3 bg-surface-base rounded-xl border border-border-default">
              <span class="text-xs text-text-muted uppercase font-mono block mb-0.5">Correo Registrado</span>
              <span class="font-medium text-text-primary">{{ checkoutStore.buyerEmail }}</span>
            </div>
            <div class="p-3 bg-surface-base rounded-xl border border-border-default">
              <span class="text-xs text-text-muted uppercase font-mono block mb-0.5">Tipo de Entrega</span>
              <span class="font-medium text-secondary-400">Pase Digital Rotativo Cifrado</span>
            </div>
          </div>
        </div>

      </div>

      <!-- Right Col: Order Summary & Place Order -->
      <div class="space-y-6">
        <div class="bg-surface-card rounded-2xl border border-border-default p-6 space-y-5">
          <h2 class="text-base font-bold font-display text-text-primary">Resumen de Compra</h2>

          <!-- Event snippet -->
          <div v-if="event" class="pb-4 border-b border-border-default space-y-1">
            <h3 class="font-bold text-text-primary text-sm">{{ event.title }}</h3>
            <p class="text-xs text-text-muted">{{ event.date }} &bull; {{ event.venue }}</p>
          </div>

          <!-- Items list -->
          <div class="space-y-2 max-h-56 overflow-y-auto pr-1">
            <div
              v-for="seat in selectedSeats"
              :key="seat.id"
              class="flex items-center justify-between text-xs py-1.5 border-b border-border-default/40"
            >
              <div>
                <span class="font-semibold text-text-primary">{{ seat.sector }}</span>
                <span class="text-text-muted font-mono block text-[11px]">Fila {{ seat.row }}, Butaca {{ seat.number }}</span>
              </div>
              <span class="font-mono font-medium text-text-secondary">${{ seat.price.toLocaleString('es-AR') }} ARS</span>
            </div>
          </div>

          <!-- Pricing Breakdown -->
          <div class="pt-2 space-y-2 text-xs">
            <div class="flex justify-between text-text-secondary">
              <span>Total Entradas ({{ selectedSeats.length }})</span>
              <span class="font-mono">${{ subtotal.toLocaleString('es-AR') }}</span>
            </div>
            <div class="flex justify-between text-text-secondary">
              <span>Costo por Servicio (12%)</span>
              <span class="font-mono">${{ fees.toLocaleString('es-AR') }}</span>
            </div>
            <div class="flex justify-between font-bold text-base text-text-primary pt-3 border-t border-border-default">
              <span>Total a Liquidar</span>
              <span class="font-mono text-secondary-400">${{ grandTotal.toLocaleString('es-AR') }} ARS</span>
            </div>
          </div>

          <!-- Submit Order CTA -->
          <button
            @click="handleCompleteOrder"
            :disabled="isProcessing || selectedSeats.length === 0"
            class="w-full py-4 px-4 rounded-xl font-bold bg-linear-to-r from-primary-400 to-secondary-400 text-surface-base hover:opacity-95 shadow-xl shadow-primary-500/20 cursor-pointer disabled:opacity-50 disabled:cursor-not-allowed transition-all flex items-center justify-center gap-2"
          >
            <span v-if="isProcessing" class="inline-block w-4 h-4 border-2 border-surface-base border-t-transparent rounded-full animate-spin"></span>
            <span>{{ isProcessing ? 'Autorizando Transacción...' : `Pagar $${grandTotal.toLocaleString('es-AR')} ARS` }}</span>
          </button>

          <p class="text-[11px] text-center text-text-muted font-mono">
            Conexión Cifrada TLS 1.3 &bull; Emisión Inmediata de Pases
          </p>
        </div>
      </div>

    </div>

    <!-- Order Completed Modal Dialog -->
    <div
      v-if="orderCompleted"
      class="fixed inset-0 z-50 bg-surface-base/80 backdrop-blur-md flex items-center justify-center p-4"
    >
      <div class="w-full max-w-lg bg-surface-card border border-border-default rounded-3xl p-8 shadow-2xl text-center space-y-6 animate-in fade-in zoom-in-95 duration-200">
        
        <div class="w-20 h-20 mx-auto rounded-full bg-secondary-500/20 border-2 border-secondary-400 text-secondary-400 flex items-center justify-center shadow-lg shadow-secondary-500/20">
          <svg class="w-10 h-10" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2.5" d="M5 13l4 4L19 7" />
          </svg>
        </div>

        <div class="space-y-2">
          <span class="text-xs font-mono font-bold text-secondary-400 uppercase tracking-widest">
            Saga Completada con Éxito
          </span>
          <h2 class="text-2xl sm:text-3xl font-extrabold font-display text-text-primary">
            ¡Entradas Emitidas Correctamente!
          </h2>
          <p class="text-sm text-text-secondary max-w-sm mx-auto">
            El pago fue aprobado y tus pases digitales con QR rotativo ya se encuentran registrados en tu billetera.
          </p>
        </div>

        <div class="p-4 rounded-xl bg-surface-base border border-border-default text-left font-mono text-xs space-y-1">
          <div class="flex justify-between text-text-muted">
            <span>Referencia de Orden</span>
            <span class="text-text-primary font-bold">{{ orderId }}</span>
          </div>
          <div class="flex justify-between text-text-muted">
            <span>Formato de Pase</span>
            <span class="text-secondary-400 font-bold">QR Dinámico Anti-Clonación (15s)</span>
          </div>
        </div>

        <div class="pt-2">
          <button
            @click="navigateToTickets"
            class="w-full py-3.5 px-6 rounded-xl font-bold bg-primary-400 text-surface-base hover:bg-primary-300 shadow-lg shadow-primary-500/20 cursor-pointer transition-all"
          >
            Ver Mis Entradas Digitales &rarr;
          </button>
        </div>

      </div>
    </div>

  </div>
</template>
