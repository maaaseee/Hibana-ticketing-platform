<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useCatalogStore, getMinEventPrice } from '../stores/catalog'
import { useQueueStore } from '../stores/queue'
import type { EventFunction, SectorPricing, PaymentPromotion, VenueParking } from '../types'
import {
  AccordionRoot,
  AccordionItem,
  AccordionHeader,
  AccordionTrigger,
  AccordionContent,
  DialogRoot,
  DialogTrigger,
  DialogPortal,
  DialogOverlay,
  DialogContent,
  DialogClose,
  DialogTitle,
  DialogDescription,
  TooltipProvider,
  TooltipRoot,
  TooltipTrigger,
  TooltipContent
} from 'radix-vue'

const route = useRoute()
const router = useRouter()
const catalog = useCatalogStore()
const queue = useQueueStore()

const eventId = computed(() => (route.params.id as string) || 'symphony-of-lights')
const event = computed(() => catalog.getEventById(eventId.value) || catalog.events[0])

const functions = computed<EventFunction[]>(() => {
  if (event.value?.functions && event.value.functions.length > 0) {
    return event.value.functions
  }
  if (event.value) {
    return [
      {
        id: `fn-${event.value.id}-1`,
        dateIso: event.value.dateIso || '2026-10-15T20:00:00',
        formattedDate: event.value.date || 'Jueves 15 de Octubre de 2026',
        doorsOpen: event.value.doorsOpen || '18:00 hs',
        showStart: event.value.time || '20:00 hs',
        status: event.value.status || 'AVAILABLE'
      }
    ]
  }
  return []
})

const sectors = computed<SectorPricing[]>(() => {
  if (event.value?.sectors && event.value.sectors.length > 0) {
    return event.value.sectors
  }
  if (event.value?.ticketTypes && event.value.ticketTypes.length > 0) {
    return event.value.ticketTypes.map((t, idx) => ({
      id: `sec-${idx}`,
      name: t.name,
      price: t.price,
      currency: 'USD',
      type: 'GENERAL_ADMISSION',
      status: 'AVAILABLE'
    }))
  }
  if (event.value) {
    return [
      {
        id: `sec-${event.value.id}-gen`,
        name: 'General',
        price: event.value.price || 45,
        currency: 'USD',
        type: 'GENERAL_ADMISSION',
        status: 'AVAILABLE'
      }
    ]
  }
  return []
})

const minPrice = computed(() => {
  if (!event.value) return 0
  return getMinEventPrice(event.value)
})

const primaryCurrency = computed(() => {
  if (sectors.value.length > 0 && sectors.value[0]?.currency) {
    return sectors.value[0].currency
  }
  return 'USD'
})

const paymentPromotions = computed<PaymentPromotion[]>(() => {
  if (event.value?.paymentPromotions && event.value.paymentPromotions.length > 0) {
    return event.value.paymentPromotions
  }
  return [
    {
      id: 'pm-santander-fallback',
      bank: 'Santander',
      card: 'Visa / Mastercard Santander',
      installments: 'Hasta 6 cuotas sin interés',
      description: '6 cuotas sin interés en todas las ubicaciones con tarjetas Santander American Express, Visa y Mastercard.',
      badgeText: '6 cuotas sin interés'
    },
    {
      id: 'pm-galicia-fallback',
      bank: 'Galicia',
      card: 'Visa / Mastercard Galicia',
      installments: 'Hasta 3 cuotas sin interés',
      description: '3 cuotas sin interés abonando con tarjetas Galicia Visa o Mastercard.',
      badgeText: '3 cuotas sin interés'
    },
    {
      id: 'pm-modo-fallback',
      bank: 'MODO',
      card: 'Tarjetas vinculadas en app MODO',
      installments: '1 pago con reintegro',
      description: '15% de reintegro los días viernes pagando con código QR desde MODO o app bancaria.',
      badgeText: '15% de reintegro'
    }
  ]
})

const parkingInfo = computed<VenueParking>(() => {
  if (event.value?.parking) {
    return event.value.parking
  }
  return {
    available: true,
    title: 'Estacionamiento Oficial del Recinto',
    description: 'Estacionamiento vigilado con control de acceso automatizado y seguridad privada durante todo el evento.',
    price: 20,
    accessRoutes: [
      'Ingreso vehicular por avenidas principales de acceso señalizadas',
      'Acceso peatonal directo al recinto por pasarelas seguras',
      'Carriles exclusivos para ingreso ágil con ticket oficial de estacionamiento'
    ],
    bookingPassUrl: '#'
  }
})

function formatSectorType(type?: string): string {
  if (!type) return 'General'
  switch (type) {
    case 'VIP':
      return 'VIP / Preferencial'
    case 'NUMBERED_SEATS':
      return 'Butacas Numeradas'
    case 'GENERAL_ADMISSION':
      return 'Campo General'
    default:
      return type.replace(/_/g, ' ')
  }
}

function getSectorStatusLabel(status?: string): string {
  switch (status) {
    case 'AVAILABLE':
      return 'Disponible'
    case 'FEW_SEATS':
      return 'Últimos lugares'
    case 'SOLD_OUT':
      return 'Agotado'
    default:
      return 'Disponible'
  }
}

function getSectorStatusClass(status?: string): string {
  switch (status) {
    case 'AVAILABLE':
      return 'bg-emerald-950/80 text-emerald-400 border-emerald-800/60'
    case 'FEW_SEATS':
      return 'bg-amber-950/80 text-amber-400 border-amber-800/60'
    case 'SOLD_OUT':
      return 'bg-rose-950/80 text-rose-400 border-rose-800/60'
    default:
      return 'bg-emerald-950/80 text-emerald-400 border-emerald-800/60'
  }
}

function getFunctionStatusLabel(status: EventFunction['status']): string {
  switch (status) {
    case 'AVAILABLE':
      return 'Disponible'
    case 'FEW_SEATS':
      return 'Últimos lugares'
    case 'SOLD_OUT':
      return 'Agotado'
    case 'QUEUE_ACTIVE':
      return 'Fila Virtual'
    default:
      return 'Disponible'
  }
}

function getFunctionStatusClass(status: EventFunction['status']): string {
  switch (status) {
    case 'AVAILABLE':
      return 'bg-emerald-950/80 text-emerald-400 border-emerald-800/60'
    case 'FEW_SEATS':
      return 'bg-amber-950/80 text-amber-400 border-amber-800/60'
    case 'SOLD_OUT':
      return 'bg-rose-950/80 text-rose-400 border-rose-800/60'
    case 'QUEUE_ACTIVE':
      return 'bg-cyan-950/80 text-cyan-400 border-cyan-800/60'
    default:
      return 'bg-emerald-950/80 text-emerald-400 border-emerald-800/60'
  }
}

function goToPurchase(fn: EventFunction) {
  const ev = event.value
  if (!ev) return

  if (fn.status === 'SOLD_OUT') return

  if (ev.status === 'QUEUE_ACTIVE' || fn.status === 'QUEUE_ACTIVE') {
    queue.enterQueue(ev.id)
    router.push({
      name: 'queue',
      params: { id: ev.id },
      query: { functionId: fn.id, date: fn.dateIso }
    })
  } else {
    router.push({
      name: 'seat-selection',
      params: { id: ev.id },
      query: { functionId: fn.id, date: fn.dateIso }
    })
  }
}
</script>

<template>
  <TooltipProvider>
    <div v-if="event" class="flex flex-col w-full min-h-[calc(100vh-64px)] bg-[#0c141f] text-[#f1f5f9]">
      <!-- Panoramic Cover Banner matching wireframe lines 141-145 -->
      <div class="w-full h-[46vh] min-h-[360px] max-h-[500px] relative bg-[#0c141f]">
        <div
          class="absolute inset-0 bg-cover bg-center"
          :style="{ backgroundImage: `url(${event.imageUrl})` }"
        ></div>
        <div class="absolute inset-0 bg-gradient-to-t from-[#0c141f] via-[#0c141f]/50 to-transparent"></div>

        <!-- Section A: Base Pricing Headline Badge inside Header Banner -->
        <div class="absolute bottom-6 right-4 sm:right-8 z-20">
          <div
            class="inline-flex items-center gap-2.5 px-4 py-2.5 rounded-xl bg-[#0c141f]/90 border border-[#86cffb]/40 shadow-2xl backdrop-blur-md"
            data-testid="banner-min-price-badge"
          >
            <span class="material-symbols-outlined text-[#86cffb] text-xl">sell</span>
            <span class="font-body text-xs sm:text-sm font-semibold text-[#cbd5e1]">Entradas desde</span>
            <span class="font-display text-2xl sm:text-3xl font-black text-white tracking-tight">${{ minPrice }}</span>
            <span class="font-mono text-xs uppercase font-bold text-[#86cffb] px-2 py-0.5 rounded bg-[#141e2e] border border-[#23354d]">
              {{ primaryCurrency }}
            </span>
          </div>
        </div>
      </div>

      <!-- Content Container matching wireframe lines 146-253 -->
      <div class="max-w-[80vw] w-full mx-auto px-4 sm:px-6 lg:px-8 -mt-36 md:-mt-44 relative z-10 grid grid-cols-1 lg:grid-cols-12 gap-8 pb-16">
        <!-- Central Info Column (7 cols) -->
        <div class="lg:col-span-7 flex flex-col gap-6">
          <div>
            <div class="flex flex-wrap items-center gap-3 mb-3">
              <span class="inline-block px-3 py-1 bg-[#141e2e] text-[#86cffb] border border-[#23354d] font-mono text-xs font-semibold rounded-md tracking-wide uppercase">
                {{ event.category || 'Música & Conciertos' }}
              </span>
              <span
                data-testid="inline-min-price-badge"
                class="inline-flex items-center gap-1.5 px-3 py-1 rounded-md bg-[#00688f]/80 text-[#86cffb] border border-[#86cffb]/30 font-mono text-xs font-bold shadow-sm"
              >
                <span>Entradas desde</span>
                <span class="text-white">${{ minPrice }} {{ primaryCurrency }}</span>
              </span>
            </div>
            <h1 class="font-display text-3xl sm:text-4xl lg:text-5xl font-extrabold text-[#f1f5f9] tracking-tight mb-3">
              {{ event.title }}
            </h1>
            <p class="font-body text-base sm:text-lg text-[#94a3b8] max-w-2xl leading-relaxed">
              {{ event.subtitle }}
            </p>
          </div>

          <!-- Metadata Bento Grid (2 cards) matching wireframe lines 162-200 -->
          <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
            <!-- Venue Card with Radix Dialog for Zoomable Map -->
            <div class="bg-[#141e2e] rounded-xl p-5 border border-[#23354d] flex flex-col justify-between">
              <div class="flex items-center gap-3.5 mb-3">
                <div class="w-10 h-10 rounded-lg bg-[#1a293e] border border-[#23354d] flex items-center justify-center text-[#86cffb] shrink-0">
                  <span class="material-symbols-outlined text-[20px]">location_on</span>
                </div>
                <div>
                  <h3 class="font-display text-lg font-bold text-[#f1f5f9]">{{ event.venue }}</h3>
                  <p class="font-mono text-xs text-[#94a3b8]">{{ event.location }}</p>
                </div>
              </div>

              <!-- Seat Map Preview with Accessible Radix Dialog Modal -->
              <DialogRoot>
                <DialogTrigger as-child>
                  <button
                    class="w-full h-36 rounded-lg overflow-hidden border border-[#23354d] mt-2 bg-[#101825] relative flex items-center justify-center group hover:border-[#86cffb]/60 transition-all cursor-pointer"
                    aria-label="Ampliar plano del recinto"
                  >
                    <svg viewBox="0 0 400 180" class="w-full h-full p-2 text-primary opacity-80 group-hover:opacity-100 transition-opacity" xmlns="http://www.w3.org/2000/svg">
                      <rect x="140" y="10" width="120" height="24" rx="4" fill="#00688f" stroke="#86cffb" stroke-width="1.5" />
                      <text x="200" y="26" text-anchor="middle" fill="#ffffff" font-size="10" font-family="Space Grotesk" font-weight="bold">ESCENARIO PRINCIPAL</text>

                      <rect x="120" y="44" width="160" height="40" rx="6" fill="#162438" stroke="#4fdbc8" stroke-width="1.5" />
                      <text x="200" y="68" text-anchor="middle" fill="#4fdbc8" font-size="11" font-family="Space Grotesk">VIP FRONT STAGE</text>

                      <path d="M 60 95 Q 200 80 340 95 L 360 160 Q 200 140 40 160 Z" fill="#141e2e" stroke="#23354d" stroke-width="1.5" />
                      <text x="200" y="130" text-anchor="middle" fill="#94a3b8" font-size="10" font-family="Space Grotesk">SECTORES GENERAL & PLATEAS</text>
                    </svg>
                    <div class="absolute bottom-2 right-2 px-2 py-0.5 rounded bg-[#0b131e]/90 border border-[#23354d] text-[10px] font-mono text-[#86cffb] group-hover:bg-[#00688f] group-hover:text-white transition-colors flex items-center gap-1 shadow-sm">
                      <span class="material-symbols-outlined text-[13px]">zoom_in</span>
                      <span>Ampliar Plano</span>
                    </div>
                  </button>
                </DialogTrigger>

                <DialogPortal>
                  <DialogOverlay class="fixed inset-0 bg-black/80 backdrop-blur-sm z-50 animate-fadeIn" />
                  <DialogContent class="fixed top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 bg-[#141e2e] border border-[#23354d] rounded-2xl p-6 shadow-2xl z-50 w-full max-w-2xl max-h-[90vh] overflow-y-auto outline-none">
                    <div class="flex items-center justify-between pb-4 border-b border-[#23354d]">
                      <div class="flex items-center gap-2">
                        <span class="material-symbols-outlined text-[#86cffb]">map</span>
                        <DialogTitle class="font-display text-lg font-bold text-[#f1f5f9]">
                          Plano Oficial del Recinto: {{ event.venue }}
                        </DialogTitle>
                      </div>
                      <DialogClose class="w-8 h-8 rounded-lg bg-[#1a293e] text-[#94a3b8] hover:text-white flex items-center justify-center transition-colors cursor-pointer">
                        <span class="material-symbols-outlined text-sm">close</span>
                      </DialogClose>
                    </div>
                    <DialogDescription class="sr-only">
                      Mapa esquemático con las ubicaciones, sectores y accesos del recinto {{ event.venue }}.
                    </DialogDescription>

                    <div class="py-6 flex flex-col items-center justify-center">
                      <svg viewBox="0 0 500 240" class="w-full max-w-lg" xmlns="http://www.w3.org/2000/svg">
                        <rect x="175" y="10" width="150" height="32" rx="6" fill="#00688f" stroke="#86cffb" stroke-width="2" />
                        <text x="250" y="31" text-anchor="middle" fill="#ffffff" font-size="12" font-family="Space Grotesk" font-weight="bold">ESCENARIO PRINCIPAL</text>

                        <rect x="150" y="55" width="200" height="50" rx="8" fill="#162438" stroke="#4fdbc8" stroke-width="1.5" />
                        <text x="250" y="85" text-anchor="middle" fill="#4fdbc8" font-size="13" font-family="Space Grotesk">VIP FRONT STAGE</text>

                        <path d="M 50 120 Q 250 100 450 120 L 480 210 Q 250 180 20 210 Z" fill="#101825" stroke="#23354d" stroke-width="2" />
                        <text x="250" y="165" text-anchor="middle" fill="#cbd5e1" font-size="12" font-family="Space Grotesk">SECTORES GENERAL & PLATEAS</text>
                      </svg>
                      <p class="font-mono text-xs text-[#94a3b8] mt-4 text-center">
                        Distribución de sectores y accesos controlados por torniquetes de alta velocidad.
                      </p>
                    </div>
                  </DialogContent>
                </DialogPortal>
              </DialogRoot>
            </div>

            <!-- Schedule & Lineup Card -->
            <div class="bg-[#141e2e] rounded-xl p-5 border border-[#23354d] flex flex-col justify-between">
              <div>
                <div class="flex items-center gap-3.5 mb-4">
                  <div class="w-10 h-10 rounded-lg bg-[#1a293e] border border-[#23354d] flex items-center justify-center text-[#86cffb] shrink-0">
                    <span class="material-symbols-outlined text-[20px]">calendar_month</span>
                  </div>
                  <div>
                    <h3 class="font-display text-lg font-bold text-[#f1f5f9]">{{ event.date }}</h3>
                    <p class="font-mono text-xs text-[#94a3b8]">Doors open: {{ event.doorsOpen || '19:00 HRS' }}</p>
                  </div>
                </div>

                <div class="pt-3 border-t border-[#23354d]/60">
                  <p class="font-mono text-xs text-[#86cffb] uppercase tracking-wider font-semibold mb-1">Lineup</p>
                  <p class="font-body text-sm text-[#f1f5f9] font-medium leading-relaxed">
                    {{ event.lineup || 'The Lumineers, Of Monsters and Men' }}
                  </p>
                </div>
              </div>

              <TooltipRoot>
                <TooltipTrigger as-child>
                  <div class="pt-4 flex items-center gap-2 text-xs text-[#94a3b8] font-mono border-t border-[#23354d]/40 mt-3 cursor-help">
                    <span class="material-symbols-outlined text-sm text-[#86cffb]">verified</span>
                    <span>Evento oficial confirmado</span>
                  </div>
                </TooltipTrigger>
                <TooltipContent class="bg-[#101825] border border-[#23354d] text-xs font-mono text-[#cbd5e1] p-2 rounded-lg shadow-xl z-50">
                  Verificado criptográficamente por la productora y recinto
                </TooltipContent>
              </TooltipRoot>
            </div>
          </div>

          <!-- Section E: About the Event (Synopsis & Show Narrative) -->
          <div class="bg-[#141e2e] rounded-xl p-6 border border-[#23354d] shadow-xl">
            <h2 class="font-display text-xl font-bold text-[#f1f5f9] mb-4">Sobre el Espectáculo</h2>
            <p class="font-body text-sm sm:text-base text-[#94a3b8] mb-4 leading-relaxed">
              {{ event.description || "The Symphony of Lights regresa para un espectáculo audiovisual sin precedentes. Esta producción cuenta con iluminación volumétrica de última generación y un sistema de sonido 360 grados inmersivo diseñado a medida para el recinto." }}
            </p>
            <p class="font-body text-sm sm:text-base text-[#94a3b8] leading-relaxed">
              Medidas estrictas de seguridad en accesos. Entradas validadas mediante códigos QR criptográficos dinámicos con refresco temporal continuo para garantizar cero duplicaciones y protección total contra la reventa.
            </p>
          </div>

          <!-- Section E: Sector Pricing Breakdown Table -->
          <div class="bg-[#141e2e] rounded-xl p-6 border border-[#23354d] shadow-xl flex flex-col gap-4">
            <div class="flex flex-col sm:flex-row sm:items-center justify-between gap-2 pb-3 border-b border-[#23354d]">
              <div>
                <h2 class="font-display text-xl font-bold text-[#f1f5f9]">Tabla de Precios por Sector</h2>
                <p class="font-mono text-xs text-[#94a3b8]">
                  Valores base oficiales para {{ event.venue }}
                </p>
              </div>
              <div class="flex items-center gap-2 font-mono text-xs text-[#86cffb]">
                <span class="material-symbols-outlined text-[16px]">info</span>
                <span>Precios finales sin cargos ocultos</span>
              </div>
            </div>

            <div class="overflow-x-auto">
              <table class="w-full text-left border-collapse" data-testid="sector-price-table">
                <thead>
                  <tr class="border-b border-[#23354d] text-[11px] font-mono uppercase tracking-wider text-[#94a3b8]">
                    <th class="py-3 px-3">Sector</th>
                    <th class="py-3 px-3">Tipo / Ubicación</th>
                    <th class="py-3 px-3">Precio Base</th>
                    <th class="py-3 px-3">Disponibilidad</th>
                  </tr>
                </thead>
                <tbody class="divide-y divide-[#23354d]/50 font-body text-sm">
                  <tr
                    v-for="sec in sectors"
                    :key="sec.id"
                    class="hover:bg-[#1a293e]/50 transition-colors"
                    data-testid="sector-row"
                  >
                    <td class="py-3.5 px-3 font-semibold text-[#f1f5f9]">
                      <div class="flex flex-col">
                        <span>{{ sec.name }}</span>
                        <span v-if="sec.description" class="font-mono text-[11px] text-[#94a3b8] font-normal">
                          {{ sec.description }}
                        </span>
                      </div>
                    </td>
                    <td class="py-3.5 px-3 font-mono text-xs text-[#cbd5e1]">
                      <span class="px-2 py-1 rounded bg-[#101825] border border-[#23354d]">
                        {{ formatSectorType(sec.type) }}
                      </span>
                    </td>
                    <td class="py-3.5 px-3 font-display font-bold text-white">
                      ${{ sec.price }} <span class="font-mono text-xs text-[#86cffb] font-normal">{{ sec.currency }}</span>
                    </td>
                    <td class="py-3.5 px-3">
                      <span
                        :class="['px-2 py-0.5 rounded text-[10px] font-mono font-bold uppercase border', getSectorStatusClass(sec.status)]"
                      >
                        {{ getSectorStatusLabel(sec.status) }}
                      </span>
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>
        </div>

        <!-- Sticky Sidebar: Dates List & Accordions (5 cols) -->
        <div class="lg:col-span-5">
          <div class="sticky top-20 flex flex-col gap-6">
            <!-- Section B: Available Dates & Functions List (Funciones Disponibles) -->
            <div class="bg-[#141e2e] rounded-xl p-5 border border-[#23354d] shadow-xl flex flex-col gap-4">
              <div class="flex items-center justify-between pb-3 border-b border-[#23354d]">
                <div class="flex items-center gap-2.5">
                  <div class="w-8 h-8 rounded-lg bg-[#1a293e] border border-[#23354d] flex items-center justify-center text-[#86cffb]">
                    <span class="material-symbols-outlined text-[18px]">calendar_month</span>
                  </div>
                  <div>
                    <h2 class="font-display text-base font-bold text-[#f1f5f9]">Funciones Disponibles</h2>
                    <p class="font-mono text-[11px] text-[#94a3b8]">Selecciona tu fecha y compra directamente</p>
                  </div>
                </div>
                <span class="font-mono text-xs text-[#86cffb] font-semibold px-2 py-0.5 rounded bg-[#1a293e] border border-[#23354d]">
                  {{ functions.length }} {{ functions.length === 1 ? 'función' : 'funciones' }}
                </span>
              </div>

              <div class="space-y-3">
                <div
                  v-for="fn in functions"
                  :key="fn.id"
                  class="bg-[#101825] border border-[#23354d] hover:border-[#86cffb]/50 rounded-xl p-4 transition-all flex flex-col sm:flex-row sm:items-center justify-between gap-3"
                  data-testid="function-row"
                >
                  <div class="flex flex-col gap-1.5 flex-1">
                    <div class="flex flex-wrap items-center gap-2">
                      <span class="font-display text-sm sm:text-base font-bold text-[#f1f5f9]">
                        {{ fn.formattedDate }}
                      </span>
                      <span
                        :class="['px-2 py-0.5 rounded text-[10px] font-mono font-bold uppercase border', getFunctionStatusClass(fn.status)]"
                      >
                        {{ getFunctionStatusLabel(fn.status) }}
                      </span>
                    </div>
                    <div class="flex flex-wrap items-center gap-x-4 gap-y-1 font-mono text-xs text-[#94a3b8]">
                      <span class="flex items-center gap-1">
                        <span class="material-symbols-outlined text-[14px] text-[#86cffb]">door_open</span>
                        <span>Apertura de puertas: {{ fn.doorsOpen }}</span>
                      </span>
                      <span class="flex items-center gap-1">
                        <span class="material-symbols-outlined text-[14px] text-[#86cffb]">schedule</span>
                        <span>Inicio del show: {{ fn.showStart }}</span>
                      </span>
                    </div>
                  </div>

                  <div class="sm:shrink-0">
                    <button
                      @click="goToPurchase(fn)"
                      :disabled="fn.status === 'SOLD_OUT'"
                      :class="[
                        'w-full sm:w-auto min-w-[110px] font-display text-sm font-bold py-2.5 px-4 rounded-lg transition-all flex items-center justify-center gap-1.5 cursor-pointer shadow-md',
                        fn.status === 'SOLD_OUT'
                          ? 'bg-[#1a293e] text-[#64748b] border border-[#23354d] cursor-not-allowed'
                          : 'bg-[#00688f] hover:bg-[#005a7d] text-white border border-[#00688f] hover:scale-[1.02] active:scale-[0.98]'
                      ]"
                    >
                      <span>{{ fn.status === 'SOLD_OUT' ? 'Agotado' : 'Comprar' }}</span>
                      <span class="material-symbols-outlined text-[16px]">
                        {{ fn.status === 'SOLD_OUT' ? 'block' : 'arrow_forward' }}
                      </span>
                    </button>
                  </div>
                </div>
              </div>
            </div>

            <!-- Sections C & D: Radix Vue Accordion (Payment Methods & Parking) -->
            <AccordionRoot type="multiple" :default-value="['payment-methods', 'venue-parking']" class="w-full space-y-3">
              <!-- Section C: Payment Methods Accordion -->
              <AccordionItem
                value="payment-methods"
                data-testid="payment-methods-accordion"
                class="bg-[#141e2e] border border-[#23354d] rounded-xl overflow-hidden shadow-md"
              >
                <AccordionHeader class="flex">
                  <AccordionTrigger
                    class="w-full flex items-center justify-between p-4 font-display text-sm font-bold text-[#f1f5f9] hover:bg-[#1a293e] transition-colors group cursor-pointer outline-none"
                  >
                    <div class="flex items-center gap-2.5">
                      <div class="w-8 h-8 rounded-lg bg-[#1a293e] border border-[#23354d] flex items-center justify-center text-[#86cffb]">
                        <span class="material-symbols-outlined text-[18px]">credit_card</span>
                      </div>
                      <div class="text-left">
                        <span>Medios de Pago y Financiación</span>
                        <p class="font-mono text-[11px] text-[#86cffb] font-normal">Hasta 6 cuotas sin interés</p>
                      </div>
                    </div>
                    <span class="material-symbols-outlined text-[#94a3b8] text-xl transition-transform duration-200 group-data-[state=open]:rotate-180">
                      expand_more
                    </span>
                  </AccordionTrigger>
                </AccordionHeader>

                <AccordionContent :force-mount="true" class="p-4 pt-1 border-t border-[#23354d]/50 bg-[#101825]/60 text-sm">
                  <div class="space-y-3 pt-2">
                    <!-- Dynamic Bank Promotions -->
                    <div
                      v-for="promo in paymentPromotions"
                      :key="promo.id"
                      class="p-3 rounded-lg bg-[#141e2e] border border-[#23354d] flex flex-col gap-1.5"
                    >
                      <div class="flex items-center justify-between">
                        <div class="flex items-center gap-2">
                          <span class="font-display font-bold text-[#f1f5f9]">{{ promo.bank }}</span>
                          <span class="font-mono text-xs text-[#94a3b8]">({{ promo.card }})</span>
                        </div>
                        <span v-if="promo.badgeText || promo.installments" class="px-2 py-0.5 rounded bg-[#00688f]/30 border border-[#86cffb]/30 font-mono text-[10px] text-[#86cffb] font-bold">
                          {{ promo.badgeText || promo.installments }}
                        </span>
                      </div>
                      <p class="font-body text-xs text-[#cbd5e1] leading-relaxed">
                        {{ promo.description }}
                      </p>
                    </div>

                    <!-- General Accepted Methods Note -->
                    <div class="pt-2 border-t border-[#23354d]/40 font-mono text-xs text-[#94a3b8]">
                      <p class="font-medium text-[#cbd5e1] mb-1">Tarjetas y Billeteras Aceptadas:</p>
                      <p>Visa, Mastercard, American Express, Cabal, Maestro, Débito Visa/Mastercard, MODO y Mercado Pago. Todas las transacciones procesadas con cifrado SSL de 256 bits.</p>
                    </div>
                  </div>
                </AccordionContent>
              </AccordionItem>

              <!-- Section D: Parking Accordion -->
              <AccordionItem
                value="venue-parking"
                data-testid="venue-parking-accordion"
                class="bg-[#141e2e] border border-[#23354d] rounded-xl overflow-hidden shadow-md"
              >
                <AccordionHeader class="flex">
                  <AccordionTrigger
                    class="w-full flex items-center justify-between p-4 font-display text-sm font-bold text-[#f1f5f9] hover:bg-[#1a293e] transition-colors group cursor-pointer outline-none"
                  >
                    <div class="flex items-center gap-2.5">
                      <div class="w-8 h-8 rounded-lg bg-[#1a293e] border border-[#23354d] flex items-center justify-center text-[#86cffb]">
                        <span class="material-symbols-outlined text-[18px]">local_parking</span>
                      </div>
                      <div class="text-left">
                        <span>Estacionamiento Oficial y Accesos</span>
                        <p class="font-mono text-[11px] text-[#86cffb] font-normal">
                          {{ parkingInfo.available ? 'Estacionamiento disponible' : 'Sin estacionamiento propio' }}
                        </p>
                      </div>
                    </div>
                    <span class="material-symbols-outlined text-[#94a3b8] text-xl transition-transform duration-200 group-data-[state=open]:rotate-180">
                      expand_more
                    </span>
                  </AccordionTrigger>
                </AccordionHeader>

                <AccordionContent :force-mount="true" class="p-4 pt-1 border-t border-[#23354d]/50 bg-[#101825]/60 text-sm">
                  <div class="space-y-3 pt-2">
                    <div class="flex flex-col gap-1">
                      <h4 class="font-display text-sm font-bold text-[#f1f5f9]">{{ parkingInfo.title }}</h4>
                      <p class="font-body text-xs text-[#cbd5e1] leading-relaxed">
                        {{ parkingInfo.description }}
                      </p>
                      <p v-if="parkingInfo.price" class="font-mono text-xs text-[#86cffb] font-semibold mt-1">
                        Tarifa oficial estimada: ${{ parkingInfo.price }} USD por vehículo
                      </p>
                    </div>

                    <!-- Designated Access Routes -->
                    <div v-if="parkingInfo.accessRoutes && parkingInfo.accessRoutes.length > 0" class="pt-2 border-t border-[#23354d]/40">
                      <p class="font-mono text-xs font-semibold text-[#86cffb] uppercase tracking-wider mb-2">
                        Rutas de Acceso Designadas:
                      </p>
                      <ul class="space-y-1.5 font-body text-xs text-[#94a3b8]">
                        <li
                          v-for="(route, rIdx) in parkingInfo.accessRoutes"
                          :key="rIdx"
                          class="flex items-start gap-2"
                        >
                          <span class="material-symbols-outlined text-[14px] text-[#86cffb] mt-0.5 shrink-0">turn_right</span>
                          <span>{{ route }}</span>
                        </li>
                      </ul>
                    </div>
                  </div>
                </AccordionContent>
              </AccordionItem>
            </AccordionRoot>

            <!-- Redlock & Zero-overselling Guarantee Trust Badges -->
            <div class="bg-[#141e2e]/60 rounded-xl p-4 border border-[#23354d]/60 flex flex-col gap-2.5 text-[11px] font-mono text-[#94a3b8]">
              <TooltipRoot>
                <TooltipTrigger as-child>
                  <div class="flex items-center gap-2 cursor-help hover:text-[#cbd5e1] transition-colors">
                    <span class="material-symbols-outlined text-[16px] text-[#4fdbc8]">lock_clock</span>
                    <span>Bloqueo Atómico Redlock 10m TTL</span>
                  </div>
                </TooltipTrigger>
                <TooltipContent class="bg-[#101825] border border-[#23354d] text-xs font-mono text-[#cbd5e1] p-2 rounded-lg shadow-xl z-50">
                  Reserva atómica distribuida en Redis durante el checkout sin riesgo de colisión
                </TooltipContent>
              </TooltipRoot>

              <TooltipRoot>
                <TooltipTrigger as-child>
                  <div class="flex items-center gap-2 cursor-help hover:text-[#cbd5e1] transition-colors">
                    <span class="material-symbols-outlined text-[16px] text-[#86cffb]">verified_user</span>
                    <span>Garantía de Asiento Sin Sobreventa</span>
                  </div>
                </TooltipTrigger>
                <TooltipContent class="bg-[#101825] border border-[#23354d] text-xs font-mono text-[#cbd5e1] p-2 rounded-lg shadow-xl z-50">
                  Verificado por el outbox transaccional y orquestación Saga
                </TooltipContent>
              </TooltipRoot>
            </div>
          </div>
        </div>
      </div>
    </div>
  </TooltipProvider>
</template>
