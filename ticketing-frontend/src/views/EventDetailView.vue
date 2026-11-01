<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useCatalogStore, getMinEventPrice } from '../stores/catalog'
import { useQueueStore } from '../stores/queue'
import { getVenueById, getVenueByName } from '../data/mockVenues'
import type { EventFunction, SectorPricing, PaymentPromotion, VenueParking, VenueTransit } from '../types'
import { TooltipProvider } from 'radix-vue'

import EventVenueModalCard from '../components/event-detail/EventVenueModalCard.vue'
import EventScheduleLineup from '../components/event-detail/EventScheduleLineup.vue'
import EventFaqAccordion from '../components/event-detail/EventFaqAccordion.vue'
import EventInteractiveMapSection from '../components/event-detail/EventInteractiveMapSection.vue'
import EventTransitGuide from '../components/event-detail/EventTransitGuide.vue'
import StickyPurchaseWidget from '../components/event-detail/StickyPurchaseWidget.vue'
import SecurityTrustBadges from '../components/event-detail/SecurityTrustBadges.vue'

const route = useRoute()
const router = useRouter()
const catalog = useCatalogStore()
const queue = useQueueStore()

const isLoading = ref(true)
const eventId = computed(() => (route.params.id as string) || 'symphony-of-lights')
const event = computed(() => catalog.getEventById(eventId.value))

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
    return event.value.sectors.map(s => {
      const base = s.basePrice !== undefined && s.basePrice !== null ? s.basePrice : s.price
      return {
        ...s,
        price: base,
        basePrice: base
      }
    })
  }
  if (event.value?.ticketTypes && event.value.ticketTypes.length > 0) {
    return event.value.ticketTypes.map((t, idx) => ({
      id: `sec-${idx}`,
      name: t.name,
      price: t.price,
      basePrice: t.price,
      currency: 'ARS',
      type: 'GENERAL_ADMISSION',
      status: 'AVAILABLE'
    }))
  }
  if (event.value) {
    const base = event.value.minPrice || event.value.price || 35000
    return [
      {
        id: `sec-${event.value.id}-gen`,
        name: 'General',
        price: base,
        basePrice: base,
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

const formattedMinPrice = computed(() => {
  return Number(minPrice.value || 0).toLocaleString('es-AR')
})

const currentTimestamp = ref<number>(Date.now())
let countdownInterval: ReturnType<typeof setInterval> | null = null

const openSidebarAccordions = ref<string[]>([])
const openDescriptionAccordions = ref<string[]>([])

const targetConcertDate = computed<Date | null>(() => {
  if (functions.value.length > 0 && functions.value[0]?.dateIso) {
    const d = new Date(functions.value[0].dateIso)
    if (!isNaN(d.getTime())) return d
  }
  if (event.value?.dateIso) {
    const d = new Date(event.value.dateIso)
    if (!isNaN(d.getTime())) return d
  }
  return null
})

const countdownFormatted = computed<string | null>(() => {
  const target = targetConcertDate.value
  if (!target) return null
  const diffMs = target.getTime() - currentTimestamp.value
  if (diffMs <= 0) return 'Concierto finalizado'

  const totalSecs = Math.floor(diffMs / 1000)
  const days = Math.floor(totalSecs / 86400)
  const hours = Math.floor((totalSecs % 86400) / 3600)
  const minutes = Math.floor((totalSecs % 3600) / 60)
  const seconds = totalSecs % 60

  const pad = (n: number) => String(n).padStart(2, '0')
  return `Faltan ${days}d, ${pad(hours)}h, ${pad(minutes)}m, ${pad(seconds)}s`
})

function getPaymentLogo(bankOrMethod: string): string {
  const b = (bankOrMethod || '').toLowerCase()
  if (b.includes('santander')) return '/images/payments/santander.svg'
  if (b.includes('bbva')) return '/images/payments/bbva.svg'
  if (b.includes('galicia')) return '/images/payments/galicia.svg'
  if (b.includes('mercado') || b.includes('mp')) return '/images/payments/mercadopago.svg'
  if (b.includes('modo')) return '/images/payments/modo.svg'
  if (b.includes('visa')) return '/images/payments/visa.svg'
  if (b.includes('master')) return '/images/payments/mastercard.svg'
  if (b.includes('amex') || b.includes('american')) return '/images/payments/amex.svg'
  return '/images/payments/visa.svg'
}

const eventFaqs = computed(() => {
  const isAdo = (event.value?.title || '').toLowerCase().includes('ado')
  const isColdplay = (event.value?.title || '').toLowerCase().includes('coldplay')

  const setlist = isAdo
    ? ['Usseewa', 'Odo', 'Gira Gira', 'Show', 'New Genesis', 'Kura Kura', 'Rule', 'Backlight', 'Mirror', 'Readymade']
    : isColdplay
      ? ['Higher Power', 'Adventure of a Lifetime', 'Paradise', 'The Scientist', 'Viva La Vida', 'Yellow', 'Clocks', 'Fix You', 'A Sky Full of Stars']
      : ['Apertura acústica orquestal', 'Grandes éxitos de gira', 'Repertorio principal en vivo', 'Bloque solista', 'Bises y cierre sinfónico']

  return [
    {
      id: 'faq-policies',
      question: 'Políticas de Acceso y Elementos Prohibidos',
      icon: 'policy',
      answer: 'Ingreso habilitado a partir del horario de apertura de puertas con DNI y e-ticket digital oficial. Prohibido el ingreso con cámaras fotográficas o filmadoras profesionales, pirotecnia, paraguas de punta rígida, botellas de vidrio, termos y elementos punzantes. Menores de 14 años deben ingresar acompañados por un adulto responsable.'
    },
    {
      id: 'faq-setlist',
      question: 'Cancionero y Repertorio del Show',
      icon: 'queue_music',
      items: setlist,
      note: 'Repertorio oficial referencial sujeto a variaciones durante la gira.'
    },
    {
      id: 'faq-ticket-limit',
      question: 'Cantidad Máxima de Entradas por Persona',
      icon: 'confirmation_number',
      answer: 'Límite estricto de hasta 4 entradas por usuario registrado. Esta medida protege la experiencia del público garantizando equidad y bloqueando esquemas de acopio o reventa masiva automatizada. Todas las entradas son nominativas y requieren datos del titular.'
    },
    {
      id: 'faq-accessibility',
      question: 'Accesibilidad y Movilidad Reducida',
      icon: 'accessible',
      answer: 'El recinto cuenta con accesos prioritarios con rampas reglamentarias, áreas exclusivas para sillas de ruedas con visibilidad despejada y baños adaptados. Para coordinar asistencia personalizada, presentarse en el puesto de acreditaciones especiales en el acceso principal.'
    },
    {
      id: 'faq-refunds',
      question: 'Políticas de Reprogramación y Reembolso',
      icon: 'assignment_return',
      answer: 'Si el show sufre reprogramaciones por razones de fuerza mayor, las entradas conservan validez plena para la nueva fecha. Los usuarios que no puedan concurrir contarán con un plazo de 15 días corridos desde el anuncio para solicitar el reembolso total a través del Centro de Ayuda.'
    }
  ]
})

const acceptedPaymentBrands = [
  { name: 'Visa', logo: '/images/payments/visa.svg' },
  { name: 'Mastercard', logo: '/images/payments/mastercard.svg' },
  { name: 'American Express', logo: '/images/payments/amex.svg' },
  { name: 'Mercado Pago', logo: '/images/payments/mercadopago.svg' },
  { name: 'MODO', logo: '/images/payments/modo.svg' },
  { name: 'Santander', logo: '/images/payments/santander.svg' },
  { name: 'BBVA', logo: '/images/payments/bbva.svg' },
  { name: 'Galicia', logo: '/images/payments/galicia.svg' }
]

function parseFunctionDate(fn: EventFunction): { day: string; month: string } {
  if (fn.dateIso) {
    const d = new Date(fn.dateIso)
    if (!isNaN(d.getTime())) {
      const day = String(d.getDate()).padStart(2, '0')
      const rawMonth = d.toLocaleDateString('es-AR', { month: 'long' })
      const month = rawMonth.charAt(0).toUpperCase() + rawMonth.slice(1)
      return { day, month }
    }
  }
  const parts = (fn.formattedDate || '').split(' ')
  const numPart = parts.find(p => /^\d{1,2}$/.test(p))
  const day = numPart ? numPart.padStart(2, '0') : '01'
  const monthIdx = parts.findIndex(p => p.toLowerCase() === 'de')
  const month = monthIdx !== -1 && parts[monthIdx + 1]
    ? parts[monthIdx + 1]!.charAt(0).toUpperCase() + parts[monthIdx + 1]!.slice(1)
    : 'Fecha'
  return { day, month }
}

function cleanTime(timeStr?: string): string {
  if (!timeStr) return '20:00 hs'
  const match = timeStr.match(/(\d{1,2}:\d{2})/)
  if (match && match[1]) {
    return `${match[1]} hs`
  }
  return timeStr.replace(/HRS/gi, '').replace(/p\.\s*m\./gi, '').replace(/hs/gi, '').trim() + ' hs'
}

const primaryCurrency = computed(() => {
  if (sectors.value.length > 0 && sectors.value[0]?.currency) {
    return sectors.value[0].currency
  }
  return 'ARS'
})

const currentVenue = computed(() => {
  if (!event.value) return undefined
  if (event.value.venueId) {
    const v = getVenueById(event.value.venueId)
    if (v) return v
  }
  return getVenueByName(event.value.venue)
})

const paymentPromotions = computed<PaymentPromotion[]>(() => {
  if (event.value?.paymentPromotions && event.value.paymentPromotions.length > 0) {
    return event.value.paymentPromotions
  }
  if (currentVenue.value?.paymentMethods && currentVenue.value.paymentMethods.length > 0) {
    return currentVenue.value.paymentMethods
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

const transitInfo = computed<VenueTransit | undefined>(() => {
  if (event.value?.transit) {
    return event.value.transit
  }
  return currentVenue.value?.transit
})

const venueAddress = computed<string>(() => {
  return currentVenue.value?.address || event.value?.location || 'Buenos Aires, Argentina'
})

const venueCoordinates = computed(() => {
  const vId = (event.value?.venueId || currentVenue.value?.id || '').toLowerCase()
  const vName = (event.value?.venue || currentVenue.value?.name || '').toLowerCase()
  if (vId.includes('monumental') || vName.includes('monumental')) return { lat: -34.545278, lng: -58.449722 }
  if (vId.includes('arena') || vName.includes('arena')) return { lat: -34.593611, lng: -58.446111 }
  if (vId.includes('rex') || vName.includes('rex')) return { lat: -34.603333, lng: -58.378889 }
  if (vId.includes('nacional') || vName.includes('nacional')) return { lat: -33.464167, lng: -70.610556 }
  return { lat: -34.6037, lng: -58.3816 }
})

const mapEmbedUrl = computed(() => {
  const { lat, lng } = venueCoordinates.value
  return `https://maps.google.com/maps?q=${lat},${lng}&hl=es&z=15&output=embed`
})

const venueExteriorImage = computed<string | null>(() => {
  if (event.value?.venueImageUrl) {
    return event.value.venueImageUrl
  }
  if (currentVenue.value?.imageUrl) {
    return currentVenue.value.imageUrl
  }
  const vId = (event.value?.venueId || currentVenue.value?.id || '').toLowerCase()
  const vName = (event.value?.venue || currentVenue.value?.name || '').toLowerCase()
  if (vId.includes('arena') || vName.includes('arena')) {
    return '/images/venues/arena_buenos_aires.jpg'
  }
  return null
})

const eventDescription = computed(() => {
  if (event.value?.description) return event.value.description
  if (!event.value) return ''
  return `${event.value.title} se presenta en ${event.value.venue}. Un espectáculo de primer nivel con una puesta en escena y sistema de sonido acústico diseñado para una experiencia en vivo inolvidable.`
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

onMounted(async () => {
  countdownInterval = setInterval(() => {
    currentTimestamp.value = Date.now()
  }, 1000)
  try {
    if (eventId.value) {
      await catalog.fetchEventById(eventId.value)
    }
  } finally {
    isLoading.value = false
  }
})

onUnmounted(() => {
  if (countdownInterval) {
    clearInterval(countdownInterval)
    countdownInterval = null
  }
})

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

function goToStressTestQueue() {
  const ev = event.value
  if (!ev) return
  queue.enterQueue(ev.id)
  router.push({
    name: 'queue',
    params: { id: ev.id },
    query: { stressTest: 'true' }
  })
}
</script>

<template>
  <TooltipProvider>
    <div v-if="event" class="flex flex-col w-full min-h-[calc(100vh-64px)] bg-canvas-base text-high-emphasis">
      <!-- Panoramic Cover Banner matching wireframe lines 141-145 -->
      <div class="w-full h-[46vh] min-h-90 max-h-125 relative bg-canvas-base">
        <div
          class="absolute inset-0 bg-cover bg-center"
          :style="{ backgroundImage: `url(${event.imageUrl})` }"
        ></div>
        <div class="absolute inset-0 bg-linear-to-t from-canvas-base via-canvas-base/50 to-transparent"></div>
      </div>

      <!-- Content Container matching wireframe lines 146-253 -->
      <div class="max-w-[80vw] w-full mx-auto px-4 sm:px-6 lg:px-8 -mt-36 md:-mt-44 relative z-10 flex flex-col gap-6 pb-16">
        <!-- Main 2-Column Layout (7 cols + 5 cols) -->
        <div class="grid grid-cols-1 lg:grid-cols-12 gap-8">
          <!-- Central Info Column (7 cols) -->
          <div class="lg:col-span-7 flex flex-col gap-6">
            <div>
              <div class="flex flex-wrap items-center gap-3 mb-3">
                <span class="inline-block px-3 py-1 bg-surface-card text-primary border border-border-subtle font-mono text-xs font-semibold rounded-md tracking-wide uppercase">
                  {{ event.category || 'Música & Conciertos' }}
                </span>
              </div>
              <h1 class="font-display text-3xl sm:text-4xl lg:text-5xl font-extrabold text-high-emphasis tracking-tight mb-3">
                {{ event.title }}
              </h1>
              <p v-if="event.subtitle" class="font-body text-base sm:text-lg text-muted-light max-w-2xl leading-relaxed mb-4">
                {{ event.subtitle }}
              </p>

              <!-- Countdown Urgency Bar (por debajo del título y tags del concierto) -->
              <div
                v-if="countdownFormatted"
                class="w-full flex items-center justify-center gap-3 px-5 py-3.5 rounded-2xl bg-linear-to-r from-navy-dark/95 via-[#0f2847]/95 to-navy-dark/95 border border-[#0099ff]/50 shadow-xl backdrop-blur-md"
                data-testid="concert-countdown-bar"
              >
                <span class="material-symbols-outlined text-[22px] text-[#00d2ff] animate-pulse">timer</span>
                <span class="font-display font-black text-base sm:text-xl text-white tracking-wider">
                  {{ countdownFormatted }}
                </span>
              </div>
            </div>

            <!-- Metadata Bento Grid (2 cards) -->
            <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
              <EventVenueModalCard
                :event="event"
                :venue-exterior-image="venueExteriorImage"
                :current-venue="currentVenue"
                :sectors="sectors"
              />

              <EventScheduleLineup :event="event" />
            </div>

            <!-- Section E: About the Event (Synopsis & Show Narrative) -->
            <EventFaqAccordion
              :event-description="eventDescription"
              :event-faqs="eventFaqs"
              v-model:open-description-accordions="openDescriptionAccordions"
            />

            <!-- Section E: Plano Interactivo del Escenario & Precios por Sector (Estilo Google Maps) -->
            <EventInteractiveMapSection
              :event="event"
              :current-venue="currentVenue"
              :sectors="sectors"
            />

            <!-- Section F: Ubicación del Recinto, Mapa Interactivo & Transporte Público -->
            <EventTransitGuide
              :event="event"
              :venue-address="venueAddress"
              :transit-info="transitInfo"
              :map-embed-url="mapEmbedUrl"
            />
          </div>

          <!-- Sticky Sidebar: Unified Purchase Card Widget (5 cols) -->
          <div class="lg:col-span-5">
            <div class="sticky top-20 flex flex-col gap-6">
              <StickyPurchaseWidget
                :formatted-min-price="formattedMinPrice"
                :min-price="minPrice"
                :primary-currency="primaryCurrency"
                :functions="functions"
                v-model:open-sidebar-accordions="openSidebarAccordions"
                :payment-promotions="paymentPromotions"
                :accepted-payment-brands="acceptedPaymentBrands"
                :parking-info="parkingInfo"
                :parse-function-date="parseFunctionDate"
                :clean-time="cleanTime"
                :get-function-status-label="getFunctionStatusLabel"
                :get-payment-logo="getPaymentLogo"
                @purchase="goToPurchase"
              />

              <SecurityTrustBadges @enter-queue="goToStressTestQueue" />
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Estado de carga mientras sincroniza con el backend -->
    <div v-else-if="isLoading" class="flex flex-col items-center justify-center min-h-[60vh] gap-4 text-center px-4">
      <span class="material-symbols-outlined text-4xl text-primary animate-spin">progress_activity</span>
      <h2 class="font-display text-xl font-bold text-high-emphasis">Cargando evento...</h2>
      <p class="font-mono text-xs text-muted-light">Sincronizando información oficial del catálogo</p>
    </div>

    <!-- Estado evento no encontrado -->
    <div v-else class="flex flex-col items-center justify-center min-h-[60vh] gap-4 text-center px-4 max-w-md mx-auto">
      <div class="w-16 h-16 rounded-2xl bg-surface-card border border-border-subtle flex items-center justify-center text-secondary shadow-lg">
        <span class="material-symbols-outlined text-3xl">event_busy</span>
      </div>
      <h2 class="font-display text-2xl font-bold text-high-emphasis">Evento no encontrado</h2>
      <p class="font-body text-sm text-muted-light leading-relaxed">
        El espectáculo que estás buscando no existe o ya no se encuentra disponible en la cartelera oficial.
      </p>
      <RouterLink
        to="/events"
        class="mt-2 inline-flex items-center gap-2 px-5 py-2.5 rounded-xl bg-primary-container text-white font-display text-sm font-bold shadow-md hover:bg-primary-container/80 transition-all cursor-pointer"
      >
        <span class="material-symbols-outlined text-lg">arrow_back</span>
        <span>Ver todos los eventos</span>
      </RouterLink>
    </div>
  </TooltipProvider>
</template>
