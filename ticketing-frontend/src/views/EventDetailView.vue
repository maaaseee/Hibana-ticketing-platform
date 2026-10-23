<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useCatalogStore, getMinEventPrice } from '../stores/catalog'
import { useQueueStore } from '../stores/queue'
import { getVenueById, getVenueByName } from '../data/mockVenues'
import VenueSectorMap from '../components/ui/VenueSectorMap.vue'
import type { EventFunction, SectorPricing, PaymentPromotion, VenueParking, VenueTransit } from '../types'
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
      currency: 'ARS',
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
        currency: 'ARS',
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
  return 'ARS'
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

const currentVenue = computed(() => {
  if (!event.value) return undefined
  if (event.value.venueId) {
    const v = getVenueById(event.value.venueId)
    if (v) return v
  }
  return getVenueByName(event.value.venue)
})

const transitInfo = computed<VenueTransit | undefined>(() => {
  if (event.value?.transit) {
    return event.value.transit
  }
  return currentVenue.value?.transit
})

const venueAddress = computed<string>(() => {
  return currentVenue.value?.address || event.value?.location || 'Buenos Aires, Argentina'
})

const parkingInfo = computed<VenueParking>(() => {
  if (event.value?.parking) {
    return event.value.parking
  }
  if (currentVenue.value?.parking) {
    return currentVenue.value.parking
  }
  return {
    available: true,
    title: 'Estacionamiento Oficial del Recinto',
    description: 'Estacionamiento vigilado con control de acceso automatizado y seguridad privada durante todo el evento.',
    price: 20000,
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
    <div v-if="event" class="flex flex-col w-full min-h-[calc(100vh-64px)] bg-canvas-base text-high-emphasis">
      <!-- Panoramic Cover Banner matching wireframe lines 141-145 -->
      <div class="w-full h-[46vh] min-h-[360px] max-h-[500px] relative bg-canvas-base">
        <div
          class="absolute inset-0 bg-cover bg-center"
          :style="{ backgroundImage: `url(${event.imageUrl})` }"
        ></div>
        <div class="absolute inset-0 bg-gradient-to-t from-canvas-base via-canvas-base/50 to-transparent"></div>

        <!-- Section A: Base Pricing Headline Badge inside Header Banner -->
        <div class="absolute bottom-6 right-4 sm:right-8 z-20">
          <div
            class="inline-flex items-center gap-2.5 px-4 py-2.5 rounded-xl bg-canvas-base/90 border border-primary/40 shadow-2xl backdrop-blur-md"
            data-testid="banner-min-price-badge"
          >
            <span class="material-symbols-outlined text-primary text-xl">sell</span>
            <span class="font-body text-xs sm:text-sm font-semibold text-medium-emphasis">Entradas desde</span>
            <span class="font-display text-2xl sm:text-3xl font-black text-white tracking-tight">${{ minPrice }}</span>
            <span class="font-mono text-xs uppercase font-bold text-primary px-2 py-0.5 rounded bg-surface-card border border-border-subtle">
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
              <span class="inline-block px-3 py-1 bg-surface-card text-primary border border-border-subtle font-mono text-xs font-semibold rounded-md tracking-wide uppercase">
                {{ event.category || 'Música & Conciertos' }}
              </span>
              <span
                data-testid="inline-min-price-badge"
                class="inline-flex items-center gap-1.5 px-3 py-1 rounded-md bg-primary-container/80 text-primary border border-primary/30 font-mono text-xs font-bold shadow-sm"
              >
                <span>Entradas desde</span>
                <span class="text-white">${{ minPrice }} {{ primaryCurrency }}</span>
              </span>
            </div>
            <h1 class="font-display text-3xl sm:text-4xl lg:text-5xl font-extrabold text-high-emphasis tracking-tight mb-3">
              {{ event.title }}
            </h1>
            <p class="font-body text-base sm:text-lg text-muted-light max-w-2xl leading-relaxed">
              {{ event.subtitle }}
            </p>
          </div>

          <!-- Metadata Bento Grid (2 cards) matching wireframe lines 162-200 -->
          <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
            <!-- Venue Card with Radix Dialog for Zoomable Map -->
            <div class="bg-surface-card rounded-xl p-5 border border-border-subtle flex flex-col justify-between">
              <div class="flex items-center gap-3.5 mb-3">
                <div class="w-10 h-10 rounded-lg bg-surface-interactive border border-border-subtle flex items-center justify-center text-primary shrink-0">
                  <span class="material-symbols-outlined text-[20px]">location_on</span>
                </div>
                <div>
                  <h3 class="font-display text-lg font-bold text-high-emphasis">{{ event.venue }}</h3>
                  <p class="font-mono text-xs text-muted-light">{{ event.location }}</p>
                </div>
              </div>

              <!-- Seat Map Preview with Accessible Radix Dialog Modal -->
              <DialogRoot>
                <DialogTrigger as-child>
                  <button
                    class="w-full h-44 rounded-xl overflow-hidden border border-border-subtle mt-2 bg-surface-container-deep relative flex items-center justify-center group hover:border-primary/60 transition-all cursor-pointer p-2 shadow-inner"
                    aria-label="Ampliar plano del recinto"
                  >
                    <!-- Renderizador Dinámico de Sectores del Recinto (Miniatura interactiva) -->
                    <VenueSectorMap
                      :venue-id="event.venueId || currentVenue?.id"
                      :sectors-pricing="sectors"
                      :compact="true"
                      :interactive="false"
                      class="pointer-events-none"
                    />

                    <div class="absolute bottom-2.5 right-2.5 px-2.5 py-1 rounded-lg bg-surface-card/90 border border-border-subtle text-xs font-mono text-primary group-hover:bg-primary-container group-hover:text-white transition-all flex items-center gap-1.5 shadow-md backdrop-blur-md">
                      <span class="material-symbols-outlined text-[15px]">zoom_in</span>
                      <span>Ampliar Plano</span>
                    </div>
                  </button>
                </DialogTrigger>

                <DialogPortal>
                  <DialogOverlay class="fixed inset-0 bg-black/85 backdrop-blur-md z-50 animate-fadeIn" />
                  <DialogContent class="fixed top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 bg-surface-card border border-border-subtle rounded-3xl p-6 sm:p-8 shadow-2xl z-50 w-[94vw] max-w-4xl max-h-[92vh] overflow-y-auto outline-none flex flex-col gap-5">
                    <div class="flex items-center justify-between pb-4 border-b border-border-subtle">
                      <div class="flex items-center gap-2.5">
                        <div class="w-9 h-9 rounded-xl bg-surface-interactive border border-border-subtle flex items-center justify-center text-primary">
                          <span class="material-symbols-outlined text-[20px]">stadium</span>
                        </div>
                        <div>
                          <DialogTitle class="font-display text-xl sm:text-2xl font-bold text-high-emphasis">
                            Plano Oficial de Sectores: {{ event.venue }}
                          </DialogTitle>
                          <p class="font-mono text-xs text-muted-light mt-0.5">
                            Visualización de sectores habilitados, capacidades y distribución escénica.
                          </p>
                        </div>
                      </div>
                      <DialogClose class="w-9 h-9 rounded-full bg-surface-interactive hover:bg-surface-interactive/80 text-muted-light hover:text-white flex items-center justify-center transition-colors cursor-pointer border border-border-subtle">
                        <span class="material-symbols-outlined text-base">close</span>
                      </DialogClose>
                    </div>

                    <DialogDescription class="sr-only">
                      Mapa interactivo de sectores y distribución arquitectónica del recinto {{ event.venue }}.
                    </DialogDescription>

                    <!-- Renderizador Dinámico de Sectores en Modal Ampliado -->
                    <div class="relative w-full h-[460px] sm:h-[520px] rounded-2xl overflow-hidden bg-surface-container-deep border border-border-subtle p-3">
                      <VenueSectorMap
                        :venue-id="event.venueId || currentVenue?.id"
                        :sectors-pricing="sectors"
                        :compact="false"
                        :interactive="true"
                      />
                    </div>

                    <!-- Leyenda y Nota de Sectores -->
                    <div class="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-3 pt-3 border-t border-border-subtle/60 text-xs font-mono text-muted-light">
                      <div class="flex items-center gap-2 text-primary">
                        <span class="material-symbols-outlined text-[16px]">touch_app</span>
                        <span>Pasa el cursor sobre cada sector para ver detalles, capacidad y precio base</span>
                      </div>
                      <span class="text-secondary font-semibold">
                        Garantía oficial Amaryllis • Cero sobreventa
                      </span>
                    </div>
                  </DialogContent>
                </DialogPortal>
              </DialogRoot>
            </div>

            <!-- Schedule & Lineup Card -->
            <div class="bg-surface-card rounded-xl p-5 border border-border-subtle flex flex-col justify-between">
              <div>
                <div class="flex items-center gap-3.5 mb-4">
                  <div class="w-10 h-10 rounded-lg bg-surface-interactive border border-border-subtle flex items-center justify-center text-primary shrink-0">
                    <span class="material-symbols-outlined text-[20px]">calendar_month</span>
                  </div>
                  <div>
                    <h3 class="font-display text-lg font-bold text-high-emphasis">{{ event.date }}</h3>
                    <p class="font-mono text-xs text-muted-light">Doors open: {{ event.doorsOpen || '19:00 HRS' }}</p>
                  </div>
                </div>

                <div class="pt-3 border-t border-border-subtle/60">
                  <p class="font-mono text-xs text-primary uppercase tracking-wider font-semibold mb-1">Lineup</p>
                  <p class="font-body text-sm text-high-emphasis font-medium leading-relaxed">
                    {{ event.lineup || 'The Lumineers, Of Monsters and Men' }}
                  </p>
                </div>
              </div>

              <TooltipRoot>
                <TooltipTrigger as-child>
                  <div class="pt-4 flex items-center gap-2 text-xs text-muted-light font-mono border-t border-border-subtle/40 mt-3 cursor-help">
                    <span class="material-symbols-outlined text-sm text-primary">verified</span>
                    <span>Evento oficial confirmado</span>
                  </div>
                </TooltipTrigger>
                <TooltipContent class="bg-surface-container-deep border border-border-subtle text-xs font-mono text-medium-emphasis p-2 rounded-lg shadow-xl z-50">
                  Verificado criptográficamente por la productora y recinto
                </TooltipContent>
              </TooltipRoot>
            </div>
          </div>

          <!-- Section E: About the Event (Synopsis & Show Narrative) -->
          <div class="bg-surface-card rounded-xl p-6 border border-border-subtle shadow-xl">
            <h2 class="font-display text-xl font-bold text-high-emphasis mb-4">Sobre el Espectáculo</h2>
            <p class="font-body text-sm sm:text-base text-muted-light mb-4 leading-relaxed">
              {{ event.description || "The Symphony of Lights regresa para un espectáculo audiovisual sin precedentes. Esta producción cuenta con iluminación volumétrica de última generación y un sistema de sonido 360 grados inmersivo diseñado a medida para el recinto." }}
            </p>
            <p class="font-body text-sm sm:text-base text-muted-light leading-relaxed">
              Medidas estrictas de seguridad en accesos. Entradas validadas mediante códigos QR criptográficos dinámicos con refresco temporal continuo para garantizar cero duplicaciones y protección total contra la reventa.
            </p>
          </div>

          <!-- Section E: Sector Pricing Breakdown Table -->
          <div class="bg-surface-card rounded-xl p-6 border border-border-subtle shadow-xl flex flex-col gap-4">
            <div class="flex flex-col sm:flex-row sm:items-center justify-between gap-2 pb-3 border-b border-border-subtle">
              <div>
                <h2 class="font-display text-xl font-bold text-high-emphasis">Tabla de Precios por Sector</h2>
                <p class="font-mono text-xs text-muted-light">
                  Valores base oficiales para {{ event.venue }}
                </p>
              </div>
              <div class="flex items-center gap-2 font-mono text-xs text-primary">
                <span class="material-symbols-outlined text-[16px]">info</span>
                <span>Precios finales sin cargos ocultos</span>
              </div>
            </div>

            <div class="overflow-x-auto">
              <table class="w-full text-left border-collapse" data-testid="sector-price-table">
                <thead>
                  <tr class="border-b border-border-subtle text-[11px] font-mono uppercase tracking-wider text-muted-light">
                    <th class="py-3 px-3">Sector</th>
                    <th class="py-3 px-3">Tipo / Ubicación</th>
                    <th class="py-3 px-3">Precio Base</th>
                    <th class="py-3 px-3">Disponibilidad</th>
                  </tr>
                </thead>
                <tbody class="divide-y border-border-subtle/50 font-body text-sm">
                  <tr
                    v-for="sec in sectors"
                    :key="sec.id"
                    class="hover:bg-surface-interactive/50 transition-colors"
                    data-testid="sector-row"
                  >
                    <td class="py-3.5 px-3 font-semibold text-high-emphasis">
                      <div class="flex flex-col">
                        <span>{{ sec.name }}</span>
                        <span v-if="sec.description" class="font-mono text-[11px] text-muted-light font-normal">
                          {{ sec.description }}
                        </span>
                      </div>
                    </td>
                    <td class="py-3.5 px-3 font-mono text-xs text-medium-emphasis">
                      <span class="px-2 py-1 rounded bg-surface-container-deep border border-border-subtle">
                        {{ formatSectorType(sec.type) }}
                      </span>
                    </td>
                    <td class="py-3.5 px-3 font-display font-bold text-white">
                      ${{ sec.price }} <span class="font-mono text-xs text-primary font-normal">{{ sec.currency }}</span>
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

          <!-- Section F: Ubicación del Recinto, Mapa Interactivo & Transporte Público -->
          <div class="bg-surface-card rounded-xl p-6 border border-border-subtle shadow-xl flex flex-col gap-5">
            <div class="flex flex-col sm:flex-row sm:items-center justify-between gap-3 pb-3 border-b border-border-subtle">
              <div class="flex items-center gap-3">
                <div class="w-10 h-10 rounded-lg bg-surface-interactive border border-border-subtle flex items-center justify-center text-primary shrink-0">
                  <span class="material-symbols-outlined text-[20px]">map</span>
                </div>
                <div>
                  <h2 class="font-display text-xl font-bold text-high-emphasis">Ubicación y Cómo Llegar</h2>
                  <p class="font-mono text-xs text-muted-light">{{ event.venue }} • {{ venueAddress }}</p>
                </div>
              </div>

              <a
                :href="transitInfo?.googleMapsUrl || `https://maps.google.com/?q=${encodeURIComponent(venueAddress)}`"
                target="_blank"
                rel="noopener noreferrer"
                class="self-start sm:self-auto inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-surface-container-deep hover:bg-surface-interactive border border-border-subtle text-secondary hover:text-white font-mono text-xs font-semibold transition-all group shadow-sm"
              >
                <span>Abrir en Google Maps</span>
                <span class="material-symbols-outlined text-[15px] group-hover:translate-x-0.5 transition-transform">open_in_new</span>
              </a>
            </div>

            <!-- Google Maps Embed Iframe -->
            <div class="relative w-full h-64 sm:h-72 rounded-xl overflow-hidden border border-border-subtle bg-surface">
              <iframe
                title="Mapa de ubicación del recinto del evento"
                width="100%"
                height="100%"
                style="border:0;"
                loading="lazy"
                allowfullscreen
                referrerpolicy="no-referrer-when-downgrade"
                :src="`https://maps.google.com/maps?q=${encodeURIComponent(transitInfo?.embedQuery || venueAddress)}&t=&z=15&ie=UTF8&iwloc=&output=embed`"
              ></iframe>
            </div>

            <!-- Desglose de Transporte Público Cercano (Colectivos, Trenes, Subtes) -->
            <div v-if="transitInfo" class="grid grid-cols-1 md:grid-cols-3 gap-4 pt-2">
              <!-- Colectivos -->
              <div class="p-4 rounded-xl bg-surface-container-deep border border-border-subtle flex flex-col gap-2">
                <div class="flex items-center gap-2 text-primary font-mono text-xs uppercase font-bold tracking-wider">
                  <span class="material-symbols-outlined text-[17px]">directions_bus</span>
                  <span>Líneas de Colectivo</span>
                </div>
                <div class="flex flex-wrap gap-1.5 pt-1">
                  <span
                    v-for="bus in transitInfo.busLines"
                    :key="bus"
                    class="px-2.5 py-0.5 rounded-md bg-surface-card border border-border-subtle text-high-emphasis font-mono text-xs font-bold shadow-sm"
                  >
                    {{ bus }}
                  </span>
                </div>
                <p class="font-body text-[11px] text-muted-light mt-auto pt-1">
                  Paradas activas en un radio de 300m del acceso principal.
                </p>
              </div>

              <!-- Trenes -->
              <div class="p-4 rounded-xl bg-surface-container-deep border border-border-subtle flex flex-col gap-2">
                <div class="flex items-center gap-2 text-secondary font-mono text-xs uppercase font-bold tracking-wider">
                  <span class="material-symbols-outlined text-[17px]">train</span>
                  <span>Estaciones de Tren</span>
                </div>
                <ul class="space-y-1.5 font-body text-xs text-high-emphasis pt-1">
                  <li
                    v-for="(train, tIdx) in transitInfo.trains"
                    :key="tIdx"
                    class="flex items-start gap-1.5 leading-snug"
                  >
                    <span class="material-symbols-outlined text-[14px] text-secondary mt-0.5 shrink-0">fiber_manual_record</span>
                    <span>{{ train }}</span>
                  </li>
                </ul>
              </div>

              <!-- Subte / Metro -->
              <div class="p-4 rounded-xl bg-surface-container-deep border border-border-subtle flex flex-col gap-2">
                <div class="flex items-center gap-2 text-primary font-mono text-xs uppercase font-bold tracking-wider">
                  <span class="material-symbols-outlined text-[17px]">subway</span>
                  <span>Líneas de Subte</span>
                </div>
                <ul class="space-y-1.5 font-body text-xs text-high-emphasis pt-1">
                  <li
                    v-for="(sub, sIdx) in transitInfo.subway"
                    :key="sIdx"
                    class="flex items-start gap-1.5 leading-snug"
                  >
                    <span class="material-symbols-outlined text-[14px] text-primary mt-0.5 shrink-0">fiber_manual_record</span>
                    <span>{{ sub }}</span>
                  </li>
                </ul>
              </div>
            </div>
          </div>
        </div>

        <!-- Sticky Sidebar: Dates List & Accordions (5 cols) -->
        <div class="lg:col-span-5">
          <div class="sticky top-20 flex flex-col gap-6">
            <!-- Section B: Available Dates & Functions List (Funciones Disponibles) -->
            <div class="bg-surface-card rounded-xl p-5 border border-border-subtle shadow-xl flex flex-col gap-4">
              <div class="flex items-center justify-between pb-3 border-b border-border-subtle">
                <div class="flex items-center gap-2.5">
                  <div class="w-8 h-8 rounded-lg bg-surface-interactive border border-border-subtle flex items-center justify-center text-primary">
                    <span class="material-symbols-outlined text-[18px]">calendar_month</span>
                  </div>
                  <div>
                    <h2 class="font-display text-base font-bold text-high-emphasis">Funciones Disponibles</h2>
                    <p class="font-mono text-[11px] text-muted-light">Selecciona tu fecha y compra directamente</p>
                  </div>
                </div>
                <span class="font-mono text-xs text-primary font-semibold px-2 py-0.5 rounded bg-surface-interactive border border-border-subtle">
                  {{ functions.length }} {{ functions.length === 1 ? 'función' : 'funciones' }}
                </span>
              </div>

              <div class="space-y-3">
                <div
                  v-for="fn in functions"
                  :key="fn.id"
                  class="bg-surface-container-deep border border-border-subtle hover:border-primary/50 rounded-xl p-4 transition-all flex flex-col sm:flex-row sm:items-center justify-between gap-3"
                  data-testid="function-row"
                >
                  <div class="flex flex-col gap-1.5 flex-1">
                    <div class="flex flex-wrap items-center gap-2">
                      <span class="font-display text-sm sm:text-base font-bold text-high-emphasis">
                        {{ fn.formattedDate }}
                      </span>
                      <span
                        :class="['px-2 py-0.5 rounded text-[10px] font-mono font-bold uppercase border', getFunctionStatusClass(fn.status)]"
                      >
                        {{ getFunctionStatusLabel(fn.status) }}
                      </span>
                    </div>
                    <div class="flex flex-wrap items-center gap-x-4 gap-y-1 font-mono text-xs text-muted-light">
                      <span class="flex items-center gap-1">
                        <span class="material-symbols-outlined text-[14px] text-primary">door_open</span>
                        <span>Apertura de puertas: {{ fn.doorsOpen }}</span>
                      </span>
                      <span class="flex items-center gap-1">
                        <span class="material-symbols-outlined text-[14px] text-primary">schedule</span>
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
                          ? 'bg-surface-interactive text-outline border border-border-subtle cursor-not-allowed'
                          : 'bg-primary-container hover:bg-primary-container/80 text-white border border-primary-container hover:scale-[1.02] active:scale-[0.98]'
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
                class="bg-surface-card border border-border-subtle rounded-xl overflow-hidden shadow-md"
              >
                <AccordionHeader class="flex">
                  <AccordionTrigger
                    class="w-full flex items-center justify-between p-4 font-display text-sm font-bold text-high-emphasis hover:bg-surface-interactive transition-colors group cursor-pointer outline-none"
                  >
                    <div class="flex items-center gap-2.5">
                      <div class="w-8 h-8 rounded-lg bg-surface-interactive border border-border-subtle flex items-center justify-center text-primary">
                        <span class="material-symbols-outlined text-[18px]">credit_card</span>
                      </div>
                      <div class="text-left">
                        <span>Medios de Pago y Financiación</span>
                        <p class="font-mono text-[11px] text-primary font-normal">Hasta 6 cuotas sin interés</p>
                      </div>
                    </div>
                    <span class="material-symbols-outlined text-muted-light text-xl transition-transform duration-200 group-data-[state=open]:rotate-180">
                      expand_more
                    </span>
                  </AccordionTrigger>
                </AccordionHeader>

                <AccordionContent :force-mount="true" class="p-4 pt-1 border-t border-border-subtle/50 bg-surface-container-deep/60 text-sm">
                  <div class="space-y-3 pt-2">
                    <!-- Dynamic Bank Promotions -->
                    <div
                      v-for="promo in paymentPromotions"
                      :key="promo.id"
                      class="p-3 rounded-lg bg-surface-card border border-border-subtle flex flex-col gap-1.5"
                    >
                      <div class="flex items-center justify-between">
                        <div class="flex items-center gap-2">
                          <span class="font-display font-bold text-high-emphasis">{{ promo.bank }}</span>
                          <span class="font-mono text-xs text-muted-light">({{ promo.card }})</span>
                        </div>
                        <span v-if="promo.badgeText || promo.installments" class="px-2 py-0.5 rounded bg-primary-container/30 border border-primary/30 font-mono text-[10px] text-primary font-bold">
                          {{ promo.badgeText || promo.installments }}
                        </span>
                      </div>
                      <p class="font-body text-xs text-medium-emphasis leading-relaxed">
                        {{ promo.description }}
                      </p>
                    </div>

                    <!-- General Accepted Methods Note -->
                    <div class="pt-2 border-t border-border-subtle/40 font-mono text-xs text-muted-light">
                      <p class="font-medium text-medium-emphasis mb-1">Tarjetas y Billeteras Aceptadas:</p>
                      <p>Visa, Mastercard, American Express, Cabal, Maestro, Débito Visa/Mastercard, MODO y Mercado Pago. Todas las transacciones procesadas con cifrado SSL de 256 bits.</p>
                    </div>
                  </div>
                </AccordionContent>
              </AccordionItem>

              <!-- Section D: Parking Accordion -->
              <AccordionItem
                value="venue-parking"
                data-testid="venue-parking-accordion"
                class="bg-surface-card border border-border-subtle rounded-xl overflow-hidden shadow-md"
              >
                <AccordionHeader class="flex">
                  <AccordionTrigger
                    class="w-full flex items-center justify-between p-4 font-display text-sm font-bold text-high-emphasis hover:bg-surface-interactive transition-colors group cursor-pointer outline-none"
                  >
                    <div class="flex items-center gap-2.5">
                      <div class="w-8 h-8 rounded-lg bg-surface-interactive border border-border-subtle flex items-center justify-center text-primary">
                        <span class="material-symbols-outlined text-[18px]">local_parking</span>
                      </div>
                      <div class="text-left">
                        <span>Estacionamiento Oficial y Accesos</span>
                        <p class="font-mono text-[11px] text-primary font-normal">
                          {{ parkingInfo.available ? 'Estacionamiento disponible' : 'Sin estacionamiento propio' }}
                        </p>
                      </div>
                    </div>
                    <span class="material-symbols-outlined text-muted-light text-xl transition-transform duration-200 group-data-[state=open]:rotate-180">
                      expand_more
                    </span>
                  </AccordionTrigger>
                </AccordionHeader>

                <AccordionContent :force-mount="true" class="p-4 pt-1 border-t border-border-subtle/50 bg-surface-container-deep/60 text-sm">
                  <div class="space-y-3 pt-2">
                    <div class="flex flex-col gap-1">
                      <h4 class="font-display text-sm font-bold text-high-emphasis">{{ parkingInfo.title }}</h4>
                      <p class="font-body text-xs text-medium-emphasis leading-relaxed">
                        {{ parkingInfo.description }}
                      </p>
                      <p v-if="parkingInfo.price" class="font-mono text-xs text-primary font-semibold mt-1">
                        Tarifa oficial estimada: ${{ parkingInfo.price }} ARS por vehículo
                      </p>
                    </div>

                    <!-- Designated Access Routes -->
                    <div v-if="parkingInfo.accessRoutes && parkingInfo.accessRoutes.length > 0" class="pt-2 border-t border-border-subtle/40">
                      <p class="font-mono text-xs font-semibold text-primary uppercase tracking-wider mb-2">
                        Rutas de Acceso Designadas:
                      </p>
                      <ul class="space-y-1.5 font-body text-xs text-muted-light">
                        <li
                          v-for="(route, rIdx) in parkingInfo.accessRoutes"
                          :key="rIdx"
                          class="flex items-start gap-2"
                        >
                          <span class="material-symbols-outlined text-[14px] text-primary mt-0.5 shrink-0">turn_right</span>
                          <span>{{ route }}</span>
                        </li>
                      </ul>
                    </div>
                  </div>
                </AccordionContent>
              </AccordionItem>
            </AccordionRoot>

            <!-- Redlock & Zero-overselling Guarantee Trust Badges -->
            <div class="bg-surface-card/60 rounded-xl p-4 border border-border-subtle/60 flex flex-col gap-2.5 text-[11px] font-mono text-muted-light">
              <TooltipRoot>
                <TooltipTrigger as-child>
                  <div class="flex items-center gap-2 cursor-help hover:text-medium-emphasis transition-colors">
                    <span class="material-symbols-outlined text-[16px] text-secondary">lock_clock</span>
                    <span>Bloqueo Atómico Redlock 10m TTL</span>
                  </div>
                </TooltipTrigger>
                <TooltipContent class="bg-surface-container-deep border border-border-subtle text-xs font-mono text-medium-emphasis p-2 rounded-lg shadow-xl z-50">
                  Reserva atómica distribuida en Redis durante el checkout sin riesgo de colisión
                </TooltipContent>
              </TooltipRoot>

              <TooltipRoot>
                <TooltipTrigger as-child>
                  <div class="flex items-center gap-2 cursor-help hover:text-medium-emphasis transition-colors">
                    <span class="material-symbols-outlined text-[16px] text-primary">verified_user</span>
                    <span>Garantía de Asiento Sin Sobreventa</span>
                  </div>
                </TooltipTrigger>
                <TooltipContent class="bg-surface-container-deep border border-border-subtle text-xs font-mono text-medium-emphasis p-2 rounded-lg shadow-xl z-50">
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
