<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
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
        price: event.value.price || 45000,
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

function getEstimatedExit(showStart?: string): string {
  if (!showStart) return '23:30 hs'
  const match = showStart.match(/(\d{1,2}):(\d{2})/)
  if (!match || !match[1] || !match[2]) return '23:30 hs'
  let hours = parseInt(match[1], 10) + 2
  let mins = parseInt(match[2], 10) + 30
  if (mins >= 60) {
    hours += 1
    mins -= 60
  }
  if (hours >= 24) hours -= 24
  return `${String(hours).padStart(2, '0')}:${String(mins).padStart(2, '0')} hs`
}

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
  return timeStr.replace(/HRS/i, 'hs').replace(/p\.\s*m\./i, 'hs').trim()
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

              <!-- Vista exterior del recinto (Arena Buenos Aires) -->
              <div
                v-if="venueExteriorImage"
                class="w-full h-44 rounded-xl overflow-hidden border border-border-subtle mt-2 relative group shadow-inner"
                data-testid="venue-exterior-image"
              >
                <img
                  :src="venueExteriorImage"
                  :alt="event.venue"
                  class="w-full h-full object-cover transition-transform duration-500 group-hover:scale-105"
                />
                <div class="absolute inset-0 bg-linear-to-t from-black/85 via-black/25 to-transparent flex items-end p-3">
                  <span class="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-lg bg-black/60 border border-white/20 text-[11px] font-mono text-white/95 backdrop-blur-md">
                    <span class="material-symbols-outlined text-[14px] text-primary">photo_camera</span>
                    <span>Fachada y acceso exterior</span>
                  </span>
                </div>
              </div>

              <!-- Seat Map Preview with Accessible Radix Dialog Modal -->
              <DialogRoot v-else>
                <DialogTrigger as-child>
                  <button
                    class="w-full h-44 rounded-xl overflow-hidden border border-border-subtle mt-2 bg-surface-container-deep relative flex items-center justify-center group hover:border-primary/60 transition-all cursor-pointer p-2 shadow-inner"
                    aria-label="Ampliar plano del recinto"
                  >
                    <!-- Renderizador Dinámico de Sectores del Recinto (Miniatura interactiva) -->
                    <VenueSectorMap
                      :venue-id="event.seatMapUrl || event.venueId || currentVenue?.id"
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
                    <div class="relative w-full h-115 sm:h-130 rounded-2xl overflow-hidden bg-surface-container-deep border border-border-subtle p-3">
                      <VenueSectorMap
                        :venue-id="event.seatMapUrl || event.venueId || currentVenue?.id"
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
                  <p class="font-mono text-xs text-primary uppercase tracking-wider font-semibold mb-1">
                    {{ event.lineup ? 'Lineup' : 'Géneros y Estilo' }}
                  </p>
                  <p class="font-body text-sm text-high-emphasis font-medium leading-relaxed">
                    {{ event.lineup || (Array.isArray(event.genre) ? event.genre.join(' • ') : event.genre || event.category || 'Música en vivo') }}
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
              {{ eventDescription }}
            </p>
            <p class="font-body text-sm sm:text-base text-muted-light leading-relaxed">
              Medidas estrictas de seguridad en accesos. Entradas validadas mediante códigos QR criptográficos dinámicos con refresco temporal continuo para garantizar cero duplicaciones y protección total contra la reventa.
            </p>

            <!-- Acordeón de Preguntas Frecuentes y Políticas del Show -->
            <div class="mt-6 pt-5 border-t border-border-subtle/80 flex flex-col gap-3">
              <h3 class="font-display text-lg font-bold text-high-emphasis flex items-center gap-2">
                <span class="material-symbols-outlined text-primary text-xl">help_outline</span>
                <span>Preguntas Frecuentes y Políticas del Evento</span>
              </h3>

              <AccordionRoot
                type="multiple"
                v-model="openDescriptionAccordions"
                class="divide-y divide-border-subtle/70 rounded-2xl border border-border-subtle bg-surface-container-deep overflow-hidden"
              >
                <AccordionItem
                  v-for="faq in eventFaqs"
                  :key="faq.id"
                  :value="faq.id"
                  class="overflow-hidden"
                >
                  <AccordionHeader class="flex">
                    <AccordionTrigger
                      class="w-full flex items-center justify-between p-4 font-display text-sm font-bold text-high-emphasis hover:bg-surface-interactive/60 transition-colors group cursor-pointer outline-none"
                    >
                      <div class="flex items-center gap-3 text-left">
                        <span class="material-symbols-outlined text-primary text-lg">{{ faq.icon }}</span>
                        <span>{{ faq.question }}</span>
                      </div>
                      <span class="material-symbols-outlined text-muted-light text-lg transition-transform duration-200 group-data-[state=open]:rotate-180 shrink-0">
                        expand_more
                      </span>
                    </AccordionTrigger>
                  </AccordionHeader>

                  <AccordionContent
                    class="overflow-hidden data-[state=closed]:hidden p-4 pt-1 bg-surface-card/60 text-sm font-body text-muted-light border-t border-border-subtle/50"
                  >
                    <p v-if="faq.answer" class="leading-relaxed pt-2">{{ faq.answer }}</p>
                    <div v-if="faq.items" class="pt-2">
                      <p class="font-mono text-xs text-primary font-semibold mb-2">Canciones destacadas del repertorio:</p>
                      <div class="flex flex-wrap gap-1.5">
                        <span
                          v-for="(song, sIdx) in faq.items"
                          :key="sIdx"
                          class="px-2.5 py-1 rounded-lg bg-surface-interactive border border-border-subtle text-xs font-mono text-white flex items-center gap-1.5 shadow-sm"
                        >
                          <span class="text-secondary text-[11px] font-bold">#{{ sIdx + 1 }}</span>
                          <span>{{ song }}</span>
                        </span>
                      </div>
                      <p v-if="faq.note" class="font-mono text-[11px] text-muted-light mt-2 italic">{{ faq.note }}</p>
                    </div>
                  </AccordionContent>
                </AccordionItem>
              </AccordionRoot>
            </div>
          </div>

          <!-- Section E: Plano Interactivo del Escenario & Precios por Sector (Estilo Google Maps) -->
          <div
            class="bg-surface-card rounded-2xl p-6 border border-border-subtle shadow-xl flex flex-col gap-4"
            data-testid="interactive-venue-map-section"
          >
            <div class="flex flex-col sm:flex-row sm:items-center justify-between gap-3 pb-3 border-b border-border-subtle">
              <div class="flex items-center gap-3">
                <div class="w-10 h-10 rounded-xl bg-surface-interactive border border-border-subtle flex items-center justify-center text-primary shrink-0 shadow-sm">
                  <span class="material-symbols-outlined text-[22px]">stadium</span>
                </div>
                <div>
                  <h2 class="font-display text-xl font-bold text-high-emphasis">Plano Interactivo & Precios por Sector</h2>
                  <p class="font-mono text-xs text-muted-light">{{ event.venue }} • Mapa del escenario y ubicaciones</p>
                </div>
              </div>

              <!-- Indicador destacado de que los precios se exploran desde el mapa -->
              <div class="inline-flex items-center gap-2 px-3.5 py-1.5 rounded-full bg-primary/10 border border-primary/30 text-primary text-xs font-mono font-bold shadow-xs">
                <span class="material-symbols-outlined text-[16px] animate-pulse">touch_app</span>
                <span>Precios visibles al interactuar en el mapa</span>
              </div>
            </div>

            <!-- Banner Explicativo de Navegación Interactiva -->
            <div class="p-3.5 rounded-xl bg-surface-container-deep/80 border border-border-subtle flex items-start gap-3">
              <span class="material-symbols-outlined text-primary text-[20px] mt-0.5 shrink-0">explore</span>
              <p class="font-body text-xs text-medium-emphasis leading-relaxed">
                <strong class="text-white font-semibold">Exploración interactiva estilo Google Maps:</strong>
                Haz clic o pasa el cursor sobre cada sector del plano para consultar su <span class="text-primary font-bold">precio oficial</span>, tipo de ubicación y disponibilidad. Puedes <strong class="text-white">arrastrar para moverte</strong> por el recinto y usar los controles o la <strong class="text-white">rueda del mouse (+ / -)</strong> para hacer zoom sobre el escenario.
              </p>
            </div>

            <!-- Contenedor del Mapa Interactivo -->
            <div class="relative w-full h-115 sm:h-140 rounded-2xl overflow-hidden bg-[#071322] border border-border-structural/80 shadow-2xl">
              <VenueSectorMap
                :venue-id="event.seatMapUrl || event.venueId || currentVenue?.id"
                :sectors-pricing="sectors"
                :compact="false"
                :interactive="true"
              />
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
                :href="transitInfo?.googleMapsUrl || `https://www.google.com/maps/search/?api=1&query=${encodeURIComponent(event.venue + ' ' + venueAddress)}`"
                target="_blank"
                rel="noopener noreferrer"
                class="self-start sm:self-auto inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-surface-container-deep hover:bg-surface-interactive border border-border-subtle text-secondary hover:text-white font-mono text-xs font-semibold transition-all group shadow-sm"
              >
                <span>Abrir en Google Maps</span>
                <span class="material-symbols-outlined text-[15px] group-hover:translate-x-0.5 transition-transform">open_in_new</span>
              </a>
            </div>

            <!-- Mapa de Ubicación del Recinto (Google Maps) -->
            <div class="relative w-full h-64 sm:h-72 rounded-xl overflow-hidden border border-border-subtle bg-surface">
              <iframe
                title="Mapa de ubicación del recinto en Google Maps"
                width="100%"
                height="100%"
                style="border:0;"
                loading="lazy"
                referrerpolicy="no-referrer-when-downgrade"
                :src="mapEmbedUrl"
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

        <!-- Sticky Sidebar: Unified Purchase Card Widget matching user design (5 cols) -->
        <div class="lg:col-span-5">
          <div class="sticky top-20 flex flex-col gap-6">
            <!-- Main Unified Card -->
            <div class="rounded-3xl overflow-hidden border border-border-structural/80 bg-[#0c1c2e] shadow-2xl flex flex-col">
              <!-- Top Vibrant Blue Header: Entradas desde $ 75.000 -->
              <div
                class="bg-[#0084d1] px-6 py-5 flex items-center justify-between text-white"
                data-testid="banner-min-price-badge"
              >
                <span class="font-display font-extrabold text-xl sm:text-2xl tracking-tight">Entradas desde</span>
                <div class="flex items-baseline gap-1">
                  <span class="font-display font-black text-3xl sm:text-4xl tracking-tight">$ {{ formattedMinPrice }}</span>
                  <span class="sr-only">${{ minPrice }} {{ primaryCurrency }}</span>
                </div>
              </div>

              <!-- Available Functions List -->
              <div class="divide-y divide-border-structural/60 bg-[#0c1c2e]">
                <div
                  v-for="fn in functions"
                  :key="fn.id"
                  class="p-5 sm:p-6 flex flex-col sm:flex-row sm:items-center justify-between gap-4 transition-colors hover:bg-[#10243a]"
                  data-testid="function-row"
                >
                  <!-- High Contrast Date Box -->
                  <div class="flex items-center gap-4">
                    <div class="w-20 h-22 rounded-2xl bg-linear-to-br from-[#0284c7] via-[#0369a1] to-[#1e3a8a] border-2 border-[#38bdf8] flex flex-col items-center justify-center shrink-0 shadow-lg shadow-sky-500/25">
                      <span class="text-3xl font-black text-white leading-none tracking-tight drop-shadow">
                        {{ parseFunctionDate(fn).day }}
                      </span>
                      <span class="mt-1 px-1.5 py-0.5 rounded bg-amber-400 text-slate-950 text-[10px] sm:text-[11px] font-black uppercase tracking-wider shadow-xs">
                        {{ parseFunctionDate(fn).month }}
                      </span>
                      <span class="sr-only">{{ fn.formattedDate }}</span>
                    </div>

                    <!-- Horarios Entrada / Show / Salida con colores de alto contraste -->
                    <div class="flex flex-col gap-2 min-w-0">
                      <div class="flex items-center gap-2">
                        <span class="text-[11px] font-mono font-black text-rose-400 uppercase tracking-wider flex items-center gap-1">
                          <span class="material-symbols-outlined text-[14px]">schedule</span>
                          <span>HORARIOS</span>
                        </span>
                        <span class="sr-only">{{ getFunctionStatusLabel(fn.status) }}</span>
                      </div>

                      <!-- Horario Unificado: Apertura, Show y Salida dentro de un solo Pill Segmentado -->
                      <div class="inline-flex items-stretch rounded-xl sm:rounded-full overflow-hidden border border-slate-700/60 shadow-md divide-x divide-white/10 w-fit max-w-full">
                        <!-- Apertura / Puertas (Emerald/Verde) -->
                        <div class="flex items-center gap-1.5 px-3 py-1.5 bg-emerald-950/80 text-emerald-300">
                          <span class="material-symbols-outlined text-[15px] text-emerald-400 shrink-0">door_front</span>
                          <div class="flex flex-col leading-none">
                            <span class="text-[9px] font-mono text-emerald-400 font-bold uppercase tracking-wider">Apertura</span>
                            <span class="font-display font-extrabold text-xs sm:text-sm text-white whitespace-nowrap">{{ cleanTime(fn.doorsOpen) }}</span>
                          </div>
                          <span class="sr-only">Apertura de puertas: {{ fn.doorsOpen }}</span>
                        </div>

                        <!-- Show (Amber/Naranja cálido) -->
                        <div class="flex items-center gap-1.5 px-3 py-1.5 bg-amber-950/80 text-amber-300">
                          <span class="material-symbols-outlined text-[15px] text-amber-400 shrink-0">music_note</span>
                          <div class="flex flex-col leading-none">
                            <span class="text-[9px] font-mono text-amber-400 font-bold uppercase tracking-wider">Show</span>
                            <span class="font-display font-extrabold text-xs sm:text-sm text-white whitespace-nowrap">{{ cleanTime(fn.showStart) }}</span>
                          </div>
                          <span class="sr-only">Inicio del show: {{ fn.showStart }}</span>
                        </div>

                        <!-- Salida estimada (Violet/Púrpura) -->
                        <div class="flex items-center gap-1.5 px-3 py-1.5 bg-purple-950/80 text-purple-300">
                          <span class="material-symbols-outlined text-[15px] text-purple-400 shrink-0">logout</span>
                          <div class="flex flex-col leading-none">
                            <span class="text-[9px] font-mono text-purple-400 font-bold uppercase tracking-wider">Salida</span>
                            <span class="font-display font-extrabold text-xs sm:text-sm text-white whitespace-nowrap">{{ getEstimatedExit(fn.showStart) }}</span>
                          </div>
                        </div>
                      </div>
                    </div>
                  </div>

                  <!-- Comprar Button -->
                  <div class="shrink-0 self-end sm:self-center">
                    <button
                      @click="goToPurchase(fn)"
                      :disabled="fn.status === 'SOLD_OUT'"
                      :class="[
                        'px-7 sm:px-8 py-2.5 sm:py-3 rounded-full font-display font-extrabold text-sm sm:text-base transition-all flex items-center justify-center shadow-lg cursor-pointer',
                        fn.status === 'SOLD_OUT'
                          ? 'bg-surface-interactive text-outline border border-border-subtle cursor-not-allowed'
                          : 'bg-[#00e5ff] hover:bg-[#33ebff] text-slate-950 hover:scale-105 active:scale-95 shadow-[#00e5ff]/30'
                      ]"
                    >
                      <span>{{ fn.status === 'SOLD_OUT' ? 'Agotado' : 'Comprar' }}</span>
                    </button>
                  </div>
                </div>
              </div>

              <!-- Stress Test Simulation Banner for Virtual Waiting Room -->
              <div class="mt-4 p-4 rounded-2xl bg-[#101825] border border-border-subtle flex flex-col sm:flex-row items-start sm:items-center justify-between gap-3 shadow-md">
                <div class="flex items-center gap-3">
                  <div class="w-9 h-9 rounded-xl bg-[#00688f]/20 border border-[#00688f]/40 flex items-center justify-center text-[#86cffb]">
                    <span class="material-symbols-outlined text-[20px]">speed</span>
                  </div>
                  <div>
                    <div class="flex items-center gap-2">
                      <span class="font-display font-bold text-white text-sm">Stress Test: Fila de Acceso Virtual</span>
                      <span class="px-2 py-0.5 rounded-full text-[10px] font-mono bg-cyan-950 text-cyan-300 border border-cyan-500/40 font-semibold">Simulación</span>
                    </div>
                    <p class="font-body text-xs text-muted-light mt-0.5">Pon a prueba el sistema de concurrencia y sala de espera distribuida (Redis Sorted Sets).</p>
                  </div>
                </div>
                <button
                  @click="goToStressTestQueue"
                  class="px-5 py-2.5 rounded-xl bg-[#00688f] hover:bg-[#0084ff] text-white font-mono text-xs font-bold transition-all flex items-center gap-1.5 shadow-md cursor-pointer shrink-0"
                >
                  <span>Entrar a la Fila</span>
                  <span class="material-symbols-outlined text-[16px]">arrow_forward</span>
                </button>
              </div>

              <!-- Accordions: Medios de pago & Estacionamiento (Ambos colapsados por defecto) -->
              <AccordionRoot
                type="multiple"
                v-model="openSidebarAccordions"
                class="w-full divide-y divide-border-structural/60 border-t border-border-structural/80"
              >
                <!-- Section C: Medios de pago -->
                <AccordionItem
                  value="payment-methods"
                  data-testid="payment-methods-accordion"
                  class="overflow-hidden"
                >
                  <AccordionHeader class="flex">
                    <AccordionTrigger
                      class="w-full flex items-center justify-between p-4 sm:p-5 font-display text-sm sm:text-base font-bold text-white hover:bg-[#10243a] transition-colors group cursor-pointer outline-none"
                    >
                      <div class="flex items-center gap-3">
                        <span class="material-symbols-outlined text-[#10b981] text-2xl font-bold">credit_card</span>
                        <span>Medios de pago</span>
                        <span class="sr-only">Medios de Pago y Financiación - Hasta 6 cuotas sin interés</span>
                      </div>
                      <span class="material-symbols-outlined text-muted-light text-xl transition-transform duration-200 group-data-[state=open]:rotate-180">
                        expand_more
                      </span>
                    </AccordionTrigger>
                  </AccordionHeader>

                  <AccordionContent class="overflow-hidden data-[state=closed]:hidden p-4 sm:p-5 pt-1 border-t border-border-structural/40 bg-[#081524]/70 text-sm">
                    <div class="space-y-3 pt-2">
                      <!-- Tarjetas de promociones bancarias con imágenes oficiales -->
                      <div
                        v-for="promo in paymentPromotions"
                        :key="promo.id"
                        class="p-3.5 rounded-xl bg-surface-card border border-border-subtle/70 flex items-center justify-between gap-3 shadow-sm hover:border-[#0099ff]/40 transition-colors"
                      >
                        <div class="flex items-center gap-3 min-w-0">
                          <img
                            :src="getPaymentLogo(promo.bank)"
                            :alt="promo.bank"
                            class="w-10 h-7 object-contain rounded bg-white/95 p-0.5 shrink-0 shadow-xs"
                          />
                          <div class="flex flex-col min-w-0">
                            <span class="font-display font-bold text-sm text-high-emphasis truncate">{{ promo.bank }}</span>
                            <span class="font-mono text-[11px] text-muted-light truncate">{{ promo.card }}</span>
                          </div>
                        </div>
                        <span class="px-2.5 py-1 rounded-full bg-emerald-950/80 border border-emerald-500/40 font-mono text-xs text-emerald-400 font-bold whitespace-nowrap shrink-0">
                          {{ promo.badgeText || promo.installments }}
                        </span>
                      </div>

                      <!-- Galería de logos de medios aceptados -->
                      <div class="pt-3 border-t border-border-subtle/30 flex flex-col gap-2">
                        <p class="font-semibold text-medium-emphasis text-xs">Tarjetas y Billeteras Aceptadas:</p>
                        <div class="flex flex-wrap items-center gap-2 pt-1">
                          <img
                            v-for="brand in acceptedPaymentBrands"
                            :key="brand.name"
                            :src="brand.logo"
                            :alt="brand.name"
                            class="h-6 w-auto rounded shadow-xs"
                          />
                        </div>
                        <p class="sr-only">Santander, BBVA, Galicia, Mercado Pago, Visa, Mastercard, American Express, Cabal, MODO.</p>
                      </div>
                    </div>
                  </AccordionContent>
                </AccordionItem>

                <!-- Section D: Estacionamiento -->
                <AccordionItem
                  value="venue-parking"
                  data-testid="venue-parking-accordion"
                  class="overflow-hidden"
                >
                  <AccordionHeader class="flex">
                    <AccordionTrigger
                      class="w-full flex items-center justify-between p-4 sm:p-5 font-display text-sm sm:text-base font-bold text-white hover:bg-[#10243a] transition-colors group cursor-pointer outline-none"
                    >
                      <div class="flex items-center gap-3">
                        <span class="material-symbols-outlined text-[#10b981] text-2xl font-bold">directions_car</span>
                        <span>Estacionamiento</span>
                        <span class="sr-only">Estacionamiento Oficial y Accesos - Estacionamiento disponible</span>
                      </div>
                      <span class="material-symbols-outlined text-muted-light text-xl transition-transform duration-200 group-data-[state=open]:rotate-180">
                        expand_more
                      </span>
                    </AccordionTrigger>
                  </AccordionHeader>

                  <AccordionContent class="overflow-hidden data-[state=closed]:hidden p-4 sm:p-5 pt-1 border-t border-border-structural/40 bg-[#081524]/70 text-sm">
                    <div class="space-y-3 pt-2">
                      <div class="flex flex-col gap-1">
                        <h4 class="font-display text-sm font-bold text-high-emphasis">{{ parkingInfo.title }}</h4>
                        <p class="font-body text-xs text-medium-emphasis leading-relaxed">
                          {{ parkingInfo.description }}
                        </p>
                        <p v-if="parkingInfo.available && parkingInfo.price" class="font-mono text-xs text-primary font-semibold mt-1">
                          Tarifa oficial estimada: ${{ parkingInfo.price.toLocaleString('es-AR') }} ARS por vehículo
                        </p>
                      </div>

                      <div v-if="parkingInfo.accessRoutes && parkingInfo.accessRoutes.length > 0" class="pt-2 border-t border-border-subtle/30">
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
            </div>

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
