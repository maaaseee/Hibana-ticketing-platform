<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { RouterLink } from 'vue-router'
import { useTicketsStore } from '../stores/tickets'
import { orderService, type OrderDetailDto } from '../services/orderService'
import type { Ticket } from '../types'
import DynamicQrCode from '../components/ui/DynamicQrCode.vue'
import TicketTransferModal from '../components/dashboard/TicketTransferModal.vue'

const ticketsStore = useTicketsStore()
const isLoadingOrders = ref(false)
const userOrders = ref<OrderDetailDto[]>([])

onMounted(async () => {
  try {
    isLoadingOrders.value = true
    const orders = await orderService.getMyOrders()
    userOrders.value = orders || []

    // If real paid orders exist on the backend, ensure tickets are in store
    if (orders && orders.length > 0) {
      for (const order of orders) {
        if (order.status === 'PAID') {
          const alreadyExists = ticketsStore.tickets.some(t => t.orderId === order.orderId)
          if (!alreadyExists) {
            ticketsStore.addIssuedTickets(
              'Evento Hibana',
              new Date(order.createdAt).toLocaleDateString('es-AR', {
                year: 'numeric',
                month: 'short',
                day: 'numeric',
                hour: '2-digit',
                minute: '2-digit',
              }) + ' ART',
              'Recinto Principal',
              [
                {
                  id: order.seatId || 'seat-1',
                  sector: 'Acceso General',
                  row: '1',
                  number: 1,
                  price: order.totalAmount,
                  status: 'RESERVED',
                  x: 0,
                  y: 0,
                },
              ],
              order.orderId,
              'Comprador Hibana',
              ''
            )
          }
        }
      }
    }
  } catch (err) {
    console.warn('[DASHBOARD] Could not fetch remote user orders:', err)
  } finally {
    isLoadingOrders.value = false
  }
})

// View Navigation Mode: 'list' (Order History) | 'detail' (Interactive Ticket Pass)
const currentMode = ref<'list' | 'detail'>('list')
const selectedOrderId = ref<string | null>(null)
const selectedTicketIndex = ref<number>(0)

// Active Tab Filter: 'upcoming' (PRÓXIMOS) | 'past' (ANTERIORES) | 'reservations' (RESERVAS)
const activeTab = ref<'upcoming' | 'past' | 'reservations'>('upcoming')

// Transfer Modal State
const showTransferModal = ref(false)
const selectedTicketForTransfer = ref<Ticket | null>(null)
const recipientEmail = ref('')
const transferSuccess = ref(false)

// Download & Feedback notifications
const showDownloadNotice = ref(false)
const showDirectionsNotice = ref(false)

interface OrderGroup {
  orderId: string
  eventId: string
  eventTitle: string
  eventDate: string
  venue: string
  tickets: Ticket[]
  status: 'ISSUED' | 'REDEEMED' | 'REFUNDED' | 'TRANSFERRED'
  imageUrl?: string
  doorsOpen?: string
  showTime?: string
}

// Group all tickets reactively by orderId
const orderGroups = computed<OrderGroup[]>(() => {
  const map = new Map<string, OrderGroup>()
  for (const t of ticketsStore.tickets) {
    if (!map.has(t.orderId)) {
      map.set(t.orderId, {
        orderId: t.orderId,
        eventId: t.eventId,
        eventTitle: t.eventTitle,
        eventDate: t.eventDate,
        venue: t.venue,
        tickets: [t],
        status: t.status,
        imageUrl: t.imageUrl,
        doorsOpen: t.doorsOpen,
        showTime: t.showTime,
      })
    } else {
      const g = map.get(t.orderId)!
      g.tickets.push(t)
      // Keep group status reflecting prioritized active tickets
      if (t.status === 'ISSUED') {
        g.status = 'ISSUED'
      }
    }
  }
  return Array.from(map.values())
})

// Counts per tab
const upcomingCount = computed(() => orderGroups.value.filter(g => g.status === 'ISSUED').length)
const pastCount = computed(() => orderGroups.value.filter(g => g.status === 'REDEEMED').length)
const reservationsCount = computed(() => orderGroups.value.filter(g => g.status === 'TRANSFERRED' || g.status === 'REFUNDED').length)

// Filtered orders according to the active tab
const filteredOrders = computed(() => {
  if (activeTab.value === 'upcoming') {
    return orderGroups.value.filter(g => g.status === 'ISSUED')
  }
  if (activeTab.value === 'past') {
    return orderGroups.value.filter(g => g.status === 'REDEEMED')
  }
  return orderGroups.value.filter(g => g.status === 'TRANSFERRED' || g.status === 'REFUNDED')
})

// Currently inspected order & ticket in detail mode
const currentOrder = computed<OrderGroup | null>(() => {
  if (!selectedOrderId.value) return null
  return orderGroups.value.find(g => g.orderId === selectedOrderId.value) || null
})

const currentTicket = computed<Ticket | null>(() => {
  if (!currentOrder.value || currentOrder.value.tickets.length === 0) return null
  const idx = Math.min(Math.max(0, selectedTicketIndex.value), currentOrder.value.tickets.length - 1)
  return currentOrder.value.tickets[idx] || null
})

function viewOrderTickets(orderId: string) {
  selectedOrderId.value = orderId
  selectedTicketIndex.value = 0
  currentMode.value = 'detail'
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

function returnToList() {
  currentMode.value = 'list'
  selectedOrderId.value = null
  selectedTicketIndex.value = 0
}

function prevTicket() {
  if (selectedTicketIndex.value > 0) {
    selectedTicketIndex.value--
  }
}

function nextTicket() {
  if (currentOrder.value && selectedTicketIndex.value < currentOrder.value.tickets.length - 1) {
    selectedTicketIndex.value++
  }
}

function openTransfer(ticket: Ticket) {
  selectedTicketForTransfer.value = ticket
  recipientEmail.value = ''
  transferSuccess.value = false
  showTransferModal.value = true
}

function handleTransferSubmit() {
  if (!recipientEmail.value || !selectedTicketForTransfer.value) return
  ticketsStore.transferTicket(selectedTicketForTransfer.value.id, recipientEmail.value)
  transferSuccess.value = true
  setTimeout(() => {
    showTransferModal.value = false
  }, 1600)
}

function triggerDownload() {
  showDownloadNotice.value = true
  setTimeout(() => {
    showDownloadNotice.value = false
  }, 3500)
}

function triggerDirections() {
  showDirectionsNotice.value = true
  setTimeout(() => {
    showDirectionsNotice.value = false
  }, 4000)
}
</script>

<template>
  <div class="min-h-screen py-8 px-4 sm:px-6 lg:px-8 max-w-6xl mx-auto selection:bg-primary-container selection:text-white">

    <!-- Toast Notifications for Simulated Pass Download & Directions -->
    <Transition
      enter-active-class="transition duration-300 ease-out"
      enter-from-class="opacity-0 -translate-y-4"
      enter-to-class="opacity-100 translate-y-0"
      leave-active-class="transition duration-200 ease-in"
      leave-from-class="opacity-100 translate-y-0"
      leave-to-class="opacity-0 -translate-y-4"
    >
      <div
        v-if="showDownloadNotice"
        class="fixed top-20 right-6 z-50 bg-[#162438] border border-secondary/50 text-[#f8fafc] px-4 py-3 rounded-2xl shadow-2xl flex items-center gap-3 font-mono text-xs backdrop-blur-md"
      >
        <span class="material-symbols-outlined text-secondary text-xl">download_done</span>
        <div>
          <p class="font-bold text-white">Pase Digital Descargado</p>
          <p class="text-[#94a3b8] text-[11px]">Guardado en formato PKPass / PDF cifrado</p>
        </div>
      </div>
    </Transition>

    <Transition
      enter-active-class="transition duration-300 ease-out"
      enter-from-class="opacity-0 -translate-y-4"
      enter-to-class="opacity-100 translate-y-0"
      leave-active-class="transition duration-200 ease-in"
      leave-from-class="opacity-100 translate-y-0"
      leave-to-class="opacity-0 -translate-y-4"
    >
      <div
        v-if="showDirectionsNotice"
        class="fixed top-20 right-6 z-50 bg-[#162438] border border-primary/50 text-[#f8fafc] px-4 py-3 rounded-2xl shadow-2xl flex items-center gap-3 font-mono text-xs backdrop-blur-md"
      >
        <span class="material-symbols-outlined text-primary text-xl">map</span>
        <div>
          <p class="font-bold text-white">{{ currentTicket?.venue || 'Recinto Oficial' }}</p>
          <p class="text-[#94a3b8] text-[11px]">Apertura de puertas: {{ currentTicket?.doorsOpen || '19:00 hs' }}</p>
        </div>
      </div>
    </Transition>

    <!-- ================================================================= -->
    <!-- MODE 1: ORDER HISTORY & PURCHASES (Wireframe + Firefox Tabs) -->
    <!-- ================================================================= -->
    <div v-if="currentMode === 'list'" class="space-y-8 animate-in fade-in duration-300">
      <!-- Header Section -->
      <header class="flex flex-col sm:flex-row sm:items-end justify-between gap-4 border-b border-[#283d59]/40 pb-6">
        <div>
          <div class="flex items-center gap-2 mb-2">
            <span class="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-mono font-bold bg-[#00688f]/20 text-[#86cffb] border border-[#00688f]/40 tracking-wider">
              <span class="w-1.5 h-1.5 rounded-full bg-[#4fdbc8] animate-pulse"></span>
              Credenciales Cifradas
            </span>
          </div>
          <h1 class="font-display text-3xl sm:text-4xl font-extrabold text-[#f8fafc] tracking-tight">
            Mis compras
          </h1>
          <p class="text-sm sm:text-base text-[#94a3b8] mt-1.5 max-w-2xl font-body">
            ¡Ya tenés tus entradas! Presentá tu código QR en el acceso para ingresar al espectáculo.
          </p>
        </div>

        <div class="hidden sm:flex items-center gap-2 text-xs font-mono bg-[#141f30] px-4 py-2.5 rounded-xl border border-[#283d59]/60 text-[#94a3b8] shadow-inner">
          <span class="material-symbols-outlined text-[18px] text-[#86cffb]">lock</span>
          <span>Acceso Oficial &bull; Anti-Reventa</span>
        </div>
      </header>

      <!-- Segmented Tabs Bar (Matches firefox_cASs9VDQDd.png) -->
      <nav class="bg-[#141f30] p-1.5 rounded-2xl border border-[#283d59]/60 flex items-center gap-2 shadow-inner overflow-x-auto">
        <button
          @click="activeTab = 'upcoming'"
          class="flex-1 min-w-[120px] py-2.5 px-4 rounded-xl font-display text-xs sm:text-sm font-bold tracking-wider transition-all cursor-pointer flex items-center justify-center gap-2 relative"
          :class="[
            activeTab === 'upcoming'
              ? 'bg-[#00688f] text-white shadow-md'
              : 'text-[#94a3b8] hover:text-[#f8fafc] hover:bg-[#162438]'
          ]"
        >
          <span>PRÓXIMOS</span>
          <span
            v-if="upcomingCount > 0"
            class="px-2 py-0.5 rounded-full text-[10px] font-mono font-bold"
            :class="activeTab === 'upcoming' ? 'bg-white/20 text-white' : 'bg-[#1b2b42] text-[#86cffb]'"
          >
            {{ upcomingCount }}
          </span>
        </button>

        <button
          @click="activeTab = 'past'"
          class="flex-1 min-w-[120px] py-2.5 px-4 rounded-xl font-display text-xs sm:text-sm font-bold tracking-wider transition-all cursor-pointer flex items-center justify-center gap-2 relative"
          :class="[
            activeTab === 'past'
              ? 'bg-[#00688f] text-white shadow-md'
              : 'text-[#94a3b8] hover:text-[#f8fafc] hover:bg-[#162438]'
          ]"
        >
          <span>ANTERIORES</span>
          <span
            v-if="pastCount > 0"
            class="px-2 py-0.5 rounded-full text-[10px] font-mono font-bold"
            :class="activeTab === 'past' ? 'bg-white/20 text-white' : 'bg-[#1b2b42] text-[#86cffb]'"
          >
            {{ pastCount }}
          </span>
        </button>

        <button
          @click="activeTab = 'reservations'"
          class="flex-1 min-w-[120px] py-2.5 px-4 rounded-xl font-display text-xs sm:text-sm font-bold tracking-wider transition-all cursor-pointer flex items-center justify-center gap-2 relative"
          :class="[
            activeTab === 'reservations'
              ? 'bg-[#00688f] text-white shadow-md'
              : 'text-[#94a3b8] hover:text-[#f8fafc] hover:bg-[#162438]'
          ]"
        >
          <span>RESERVAS</span>
          <span
            v-if="reservationsCount > 0"
            class="px-2 py-0.5 rounded-full text-[10px] font-mono font-bold"
            :class="activeTab === 'reservations' ? 'bg-white/20 text-white' : 'bg-[#1b2b42] text-[#86cffb]'"
          >
            {{ reservationsCount }}
          </span>
        </button>
      </nav>

      <!-- Order History Box (Matches screen.png Order History Container) -->
      <section class="bg-[#0e1927] border border-[#283d59]/50 rounded-2xl shadow-[0px_8px_24px_rgba(0,0,0,0.35)] p-4 sm:p-6 space-y-4">
        <div class="flex items-center justify-between">
          <h2 class="font-display text-xl sm:text-2xl font-bold text-[#f8fafc] tracking-tight">
            Order History
          </h2>
          <span class="font-mono text-xs text-[#94a3b8]">
            {{ filteredOrders.length }} {{ filteredOrders.length === 1 ? 'orden registrada' : 'órdenes registradas' }}
          </span>
        </div>

        <!-- Orders Cards List -->
        <div v-if="filteredOrders.length > 0" class="flex flex-col gap-4">
          <article
            v-for="order in filteredOrders"
            :key="order.orderId"
            class="relative bg-[#162438] rounded-2xl p-4 sm:p-5 flex flex-col sm:flex-row gap-4 sm:gap-6 items-start sm:items-center overflow-hidden shadow-md hover:-translate-y-0.5 hover:border-[#86cffb]/40 transition-all duration-300 border border-[#283d59]"
          >
            <!-- Top-Left Pinned Badge (xN entradas) -->
            <div class="absolute top-0 left-0 bg-[#00688f] text-white px-3 py-1 rounded-br-xl flex items-center gap-1.5 font-mono text-xs font-semibold tracking-wide shadow-sm z-10">
              <span class="material-symbols-outlined text-[15px]">confirmation_number</span>
              <span>x{{ order.tickets.length }} {{ order.tickets.length === 1 ? 'entrada' : 'entradas' }}</span>
            </div>

            <!-- Left Event Artwork / Thumbnail -->
            <div class="w-full sm:w-36 h-28 shrink-0 rounded-xl overflow-hidden border border-[#283d59]/50 relative mt-4 sm:mt-0 bg-[#1b2d46]">
              <img
                v-if="order.imageUrl"
                :src="order.imageUrl"
                :alt="order.eventTitle"
                class="w-full h-full object-cover group-hover:scale-105 transition-transform duration-500"
              />
              <div v-else class="w-full h-full flex flex-col items-center justify-center text-primary/70">
                <span class="material-symbols-outlined text-4xl">theater_comedy</span>
              </div>
              <div class="absolute inset-0 bg-gradient-to-t from-black/60 via-transparent to-transparent"></div>
            </div>

            <!-- Middle Information -->
            <div class="flex-1 flex flex-col justify-between w-full">
              <div>
                <h3 class="text-[#f8fafc] font-display text-lg sm:text-xl font-bold uppercase leading-tight tracking-tight">
                  {{ order.eventTitle }}
                </h3>
                <p class="text-[#94a3b8] font-body text-sm mt-1 flex items-center gap-1.5">
                  <span class="material-symbols-outlined text-[16px] text-secondary">calendar_today</span>
                  <span>{{ order.eventDate }}</span>
                </p>
                <p class="text-[#94a3b8] font-body text-xs mt-0.5 flex items-center gap-1.5">
                  <span class="material-symbols-outlined text-[16px] text-primary">location_on</span>
                  <span>{{ order.venue }} &bull; {{ order.tickets[0]?.sector }}</span>
                </p>
              </div>

              <!-- Order Reference & Status Indicators -->
              <div class="mt-3 flex flex-wrap items-center gap-3 text-xs font-mono text-muted-light">
                <span>Orden: <strong class="text-high-emphasis">{{ order.orderId }}</strong></span>
                <span>&bull;</span>
                <span v-if="order.status === 'ISSUED'" class="text-secondary flex items-center gap-1">
                  <span class="w-1.5 h-1.5 rounded-full bg-secondary"></span>
                  Pases Activos
                </span>
                <span v-else-if="order.status === 'REDEEMED'" class="text-slate-400 flex items-center gap-1">
                  <span class="w-1.5 h-1.5 rounded-full bg-slate-400"></span>
                  Pases Canjeados
                </span>
                <span v-else class="text-rose-400 flex items-center gap-1">
                  <span class="w-1.5 h-1.5 rounded-full bg-rose-400"></span>
                  {{ order.status }}
                </span>
              </div>
            </div>

            <!-- Right Action: 'Ver entrada' or 'Cancelado' status -->
            <div class="w-full sm:w-auto flex sm:flex-col items-end justify-between sm:justify-center self-stretch gap-2 shrink-0 pt-2 sm:pt-0 border-t sm:border-t-0 border-[#283d59]/40">
              <span
                v-if="order.status === 'REFUNDED'"
                class="text-[#ffb4ab] font-mono text-xs px-3 py-1.5 rounded-full bg-rose-950/40 border border-rose-500/30 font-semibold"
              >
                Cancelado
              </span>
              <button
                v-else
                @click="viewOrderTickets(order.orderId)"
                class="w-full sm:w-auto px-5 py-2 rounded-full border border-[#00688f] bg-[#00688f]/25 text-[#c5e7ff] font-mono text-xs font-semibold hover:bg-[#00688f] hover:text-white transition-all cursor-pointer flex items-center justify-center gap-1.5 shadow-sm group"
              >
                <span>Ver entrada</span>
                <span class="material-symbols-outlined text-[16px] group-hover:translate-x-0.5 transition-transform">arrow_forward</span>
              </button>
            </div>
          </article>
        </div>

        <!-- Empty State (Matches firefox_m0NvC0kW5h.png) -->
        <div
          v-else
          class="bg-[#141f30] border border-[#283d59]/60 rounded-2xl p-10 sm:p-14 text-center space-y-4"
        >
          <div class="w-16 h-16 mx-auto rounded-2xl bg-[#1b2d46] border border-[#283d59] flex items-center justify-center text-[#86cffb]">
            <span class="material-symbols-outlined text-3xl">confirmation_number</span>
          </div>
          <div class="space-y-1">
            <h3 class="font-display text-lg sm:text-xl font-bold text-[#f8fafc]">
              Todavía no tenés entradas en esta sección.
            </h3>
            <p class="text-sm text-[#94a3b8] max-w-md mx-auto font-body">
              Cuando adquieras tus entradas las verás reflejadas acá con sus códigos QR de acceso.
            </p>
          </div>
          <div class="pt-2">
            <RouterLink
              :to="{ name: 'events-catalog' }"
              class="inline-flex items-center gap-2 px-6 py-2.5 rounded-xl text-xs font-mono font-bold bg-[#00688f] text-white hover:bg-[#005a7d] transition-all shadow-md"
            >
              <span class="material-symbols-outlined text-[16px]">search</span>
              <span>Explorar Cartelera Oficial</span>
            </RouterLink>
          </div>
        </div>
      </section>
    </div>

    <!-- ================================================================= -->
    <!-- MODE 2: TICKET INSPECTION VIEWER (Matches firefox_xtPlbl8pr4.png) -->
    <!-- ================================================================= -->
    <div v-else-if="currentMode === 'detail' && currentOrder && currentTicket" class="space-y-6 animate-in fade-in duration-300">
      <!-- Drilldown Header with Back Button -->
      <div class="flex flex-col gap-2">
        <button
          @click="returnToList"
          class="inline-flex items-center gap-2 text-xs font-mono font-bold text-[#86cffb] hover:text-white transition-colors cursor-pointer w-fit group py-1"
        >
          <span class="material-symbols-outlined text-sm group-hover:-translate-x-1 transition-transform">arrow_back</span>
          <span>Volver a mis compras</span>
        </button>

        <div class="flex flex-col sm:flex-row sm:items-baseline justify-between gap-2 border-b border-[#283d59]/40 pb-4">
          <div>
            <h1 class="font-display text-2xl sm:text-3xl font-extrabold text-[#f8fafc] uppercase tracking-tight">
              {{ currentOrder.eventTitle }}
            </h1>
            <p class="font-body text-sm text-[#94a3b8] mt-0.5">
              {{ currentOrder.eventDate }} &bull; {{ currentOrder.venue }}
            </p>
          </div>

          <div class="font-mono text-xs text-[#94a3b8]">
            Orden: <strong class="text-[#f8fafc]">{{ currentOrder.orderId }}</strong>
          </div>
        </div>
      </div>

      <!-- Ticket Pagination Bar (< Entrada 1 de N >) -->
      <div class="flex items-center justify-center gap-4 py-2">
        <button
          @click="prevTicket"
          :disabled="selectedTicketIndex === 0"
          class="w-9 h-9 rounded-full bg-[#162438] hover:bg-[#203450] disabled:opacity-30 disabled:cursor-not-allowed border border-[#283d59] flex items-center justify-center text-white transition-all cursor-pointer shadow"
          title="Entrada anterior"
        >
          <span class="material-symbols-outlined text-[18px]">chevron_left</span>
        </button>

        <span class="font-display text-sm font-bold text-[#f8fafc] tracking-wider px-3">
          Entrada {{ selectedTicketIndex + 1 }} de {{ currentOrder.tickets.length }}
        </span>

        <button
          @click="nextTicket"
          :disabled="selectedTicketIndex === currentOrder.tickets.length - 1"
          class="w-9 h-9 rounded-full bg-[#162438] hover:bg-[#203450] disabled:opacity-30 disabled:cursor-not-allowed border border-[#283d59] flex items-center justify-center text-white transition-all cursor-pointer shadow"
          title="Entrada siguiente"
        >
          <span class="material-symbols-outlined text-[18px]">chevron_right</span>
        </button>
      </div>

      <!-- Physical Boarding Pass Card Structure -->
      <div class="w-full max-w-lg mx-auto bg-[#141f30] border border-[#283d59] rounded-3xl shadow-[0px_20px_50px_rgba(0,0,0,0.55)] overflow-hidden flex flex-col relative transition-all">

        <!-- Top Event Artwork Header -->
        <div class="h-44 bg-cover bg-center w-full relative bg-[#1b2d46]" :style="currentOrder.imageUrl ? `background-image: url('${currentOrder.imageUrl}')` : ''">
          <div class="absolute inset-0 bg-gradient-to-t from-[#141f30] via-[#141f30]/60 to-transparent"></div>
          
          <div class="absolute top-3 right-3 bg-[#00688f] text-white font-mono text-[10px] px-3 py-1 rounded-full font-bold tracking-wider shadow">
            {{ currentTicket.status === 'ISSUED' ? 'ACCESO CONFIRMADO' : (currentTicket.status === 'REDEEMED' ? 'SHOW CONCLUIDO' : currentTicket.status) }}
          </div>

          <div class="absolute bottom-3 left-4 right-4">
            <h2 class="font-display text-xl sm:text-2xl font-black text-white tracking-tight uppercase leading-none drop-shadow-md">
              {{ currentTicket.eventTitle }}
            </h2>
            <p class="font-body text-xs text-white/90 mt-1 font-medium drop-shadow">
              {{ currentTicket.eventDate }}
            </p>
          </div>
        </div>

        <!-- Upper Pass Section: QR Code or Redeemed / Transferred Status -->
        <div class="p-5 sm:p-6 flex flex-col items-center justify-center bg-[#141f30] space-y-4">
          
          <!-- State A: Active & Issued -> Generic QR Code -->
          <div v-if="currentTicket.status === 'ISSUED'" class="flex flex-col items-center w-full max-w-xs">
            <DynamicQrCode
              :ticket-id="currentTicket.id"
              :payload="currentTicket.qrPayload"
            />
          </div>

          <!-- State B: Redeemed -> Clock Icon + Attended Info (Matches firefox_xtPlbl8pr4.png) -->
          <div
            v-else-if="currentTicket.status === 'REDEEMED'"
            class="w-full py-8 px-6 rounded-2xl bg-[#1b283d] border border-[#283d59] text-center space-y-2"
          >
            <div class="w-12 h-12 mx-auto rounded-full bg-[#243754] text-[#86cffb] flex items-center justify-center">
              <span class="material-symbols-outlined text-2xl">schedule</span>
            </div>
            <h3 class="font-display text-base font-bold text-[#f8fafc]">
              ¡Ya usaste tu entrada!
            </h3>
            <p class="font-body text-xs text-[#94a3b8]">
              Ingresaste el {{ currentTicket.redeemedDate || '25 noviembre 2025' }}
            </p>
          </div>

          <!-- State C: Transferred or Refunded -->
          <div
            v-else
            class="w-full py-8 px-6 rounded-2xl bg-[#1b283d] border border-rose-500/30 text-center space-y-2"
          >
            <span class="material-symbols-outlined text-3xl text-rose-400">info</span>
            <p class="font-display text-base font-bold text-rose-300">
              Entrada {{ currentTicket.status === 'TRANSFERRED' ? 'Transferida' : 'Anulada' }}
            </p>
            <p class="font-mono text-xs text-[#94a3b8]">
              Destinatario: {{ currentTicket.customerEmail }}
            </p>
          </div>

          <!-- Action Buttons Bar: Descargar | Transferir | Cómo llegar -->
          <div class="flex flex-wrap items-center justify-center gap-2 pt-1 w-full">
            <button
              @click="triggerDownload"
              class="flex-1 min-w-[110px] py-2 px-3 rounded-full text-xs font-mono font-bold bg-[#00688f] hover:bg-[#005a7d] text-white transition-all cursor-pointer flex items-center justify-center gap-1.5 shadow"
            >
              <span class="material-symbols-outlined text-[15px]">download</span>
              <span>Descargar</span>
            </button>

            <button
              v-if="currentTicket.status === 'ISSUED'"
              @click="openTransfer(currentTicket)"
              class="flex-1 min-w-[110px] py-2 px-3 rounded-full text-xs font-mono font-bold bg-[#1b2d46] hover:bg-[#243c5e] text-[#86cffb] border border-[#283d59] transition-all cursor-pointer flex items-center justify-center gap-1.5"
            >
              <span class="material-symbols-outlined text-[15px]">sync_alt</span>
              <span>Transferir</span>
            </button>

            <button
              @click="triggerDirections"
              class="flex-1 min-w-[110px] py-2 px-3 rounded-full text-xs font-mono font-bold bg-[#1b2d46] hover:bg-[#243c5e] text-white border border-[#283d59] transition-all cursor-pointer flex items-center justify-center gap-1.5"
            >
              <span class="material-symbols-outlined text-[15px]">near_me</span>
              <span>Cómo llegar</span>
            </button>
          </div>
        </div>

        <!-- Perforation Notch Divider Line (Physical Ticket Punch Effect) -->
        <div class="relative w-full border-b border-dashed border-[#283d59] my-1">
          <div class="absolute -left-3.5 -top-3 w-6 h-6 bg-[#0c141f] rounded-full border border-[#283d59] border-l-0 z-20"></div>
          <div class="absolute -right-3.5 -top-3 w-6 h-6 bg-[#0c141f] rounded-full border border-[#283d59] border-r-0 z-20"></div>
        </div>

        <!-- Lower Pass Section: Ticket Specification Sheet (Matches firefox_xtPlbl8pr4.png) -->
        <div class="p-5 sm:p-6 bg-[#101826] text-xs font-body space-y-4">
          
          <!-- Holder Identity -->
          <div class="space-y-1.5">
            <div class="flex items-center gap-2 text-[#f8fafc]">
              <span class="material-symbols-outlined text-[16px] text-primary">person</span>
              <span class="font-bold text-sm">{{ currentTicket.customerName }}</span>
            </div>
            <div class="flex items-center gap-2 text-[#94a3b8] font-mono text-[11px]">
              <span class="material-symbols-outlined text-[16px] text-secondary">badge</span>
              <span>Documento: <strong class="text-white">{{ currentTicket.customerDocument || currentTicket.documentNumber || currentTicket.dni || 'Sin registrar' }}</strong></span>
            </div>
          </div>

          <div class="border-t border-[#283d59]/50 pt-3">
            <span class="text-[10px] uppercase font-mono tracking-wider text-[#94a3b8]">Show</span>
            <p class="font-display text-sm font-bold text-white mt-0.5">
              {{ currentTicket.eventTitle }} &bull; {{ currentTicket.eventDate }}
            </p>
          </div>

          <!-- Times Row -->
          <div class="grid grid-cols-2 gap-4 border-t border-[#283d59]/50 pt-3">
            <div>
              <span class="text-[10px] uppercase font-mono tracking-wider text-[#94a3b8] block">Apertura de puertas</span>
              <span class="font-mono text-sm font-bold text-white">{{ currentTicket.doorsOpen || '19:00 hs' }}</span>
            </div>
            <div>
              <span class="text-[10px] uppercase font-mono tracking-wider text-[#94a3b8] block">Hora show</span>
              <span class="font-mono text-sm font-bold text-white">{{ currentTicket.showTime || '21:00 hs' }}</span>
            </div>
          </div>

          <!-- 4-Column Seat Breakdown Grid -->
          <div class="grid grid-cols-4 gap-2 border-t border-[#283d59]/50 pt-3 text-center bg-[#141f30] p-3 rounded-xl border border-[#283d59]/60">
            <div>
              <span class="text-[10px] uppercase font-mono text-[#94a3b8] block">Zona</span>
              <span class="font-display text-xs sm:text-sm font-bold text-primary truncate block">{{ currentTicket.sector }}</span>
            </div>
            <div>
              <span class="text-[10px] uppercase font-mono text-[#94a3b8] block">Sector</span>
              <span class="font-display text-xs sm:text-sm font-bold text-white">116</span>
            </div>
            <div>
              <span class="text-[10px] uppercase font-mono text-[#94a3b8] block">Fila</span>
              <span class="font-display text-xs sm:text-sm font-bold text-white">{{ currentTicket.row }}</span>
            </div>
            <div>
              <span class="text-[10px] uppercase font-mono text-[#94a3b8] block">Asiento</span>
              <span class="font-display text-xs sm:text-sm font-bold text-secondary">{{ currentTicket.seatNumber }}</span>
            </div>
          </div>

          <!-- Financials & Order Reference -->
          <div class="grid grid-cols-2 gap-4 border-t border-[#283d59]/50 pt-3 text-[11px] font-mono">
            <div>
              <span class="text-[#94a3b8] block">Ticket</span>
              <span class="font-bold text-white text-sm">$ {{ (currentTicket.price || 68000).toLocaleString('es-AR') }}</span>
            </div>
            <div>
              <span class="text-[#94a3b8] block">Service Charge</span>
              <span class="font-bold text-white text-sm">$ {{ (currentTicket.serviceCharge || 10200).toLocaleString('es-AR') }}</span>
            </div>
          </div>

          <div class="border-t border-[#283d59]/50 pt-2 flex items-center justify-between font-mono text-[10px] text-[#94a3b8]">
            <span>N° de orden: <strong class="text-white">{{ currentTicket.orderId }}</strong></span>
          </div>

          <div class="pt-2 text-center text-[10px] font-mono text-[#94a3b8]/80 leading-relaxed border-t border-[#283d59]/30">
            Entrada oficial y válida para el ingreso en molinete &bull; Hibana.tk
          </div>
        </div>
      </div>
    </div>

    <!-- Transfer Pass Dialog Modal -->
    <TicketTransferModal
      v-model:open="showTransferModal"
      v-model:recipient-email="recipientEmail"
      :ticket="selectedTicketForTransfer"
      :transfer-success="transferSuccess"
      @submit="handleTransferSubmit"
      @close="showTransferModal = false"
    />
  </div>
</template>
