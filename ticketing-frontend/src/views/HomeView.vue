<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter, RouterLink } from 'vue-router'
import {
  DialogRoot,
  DialogPortal,
  DialogOverlay,
  DialogContent,
  DialogClose,
  DialogTitle,
  DialogDescription
} from 'radix-vue'
import { useCatalogStore } from '../stores/catalog'
import { mockVenues } from '../data/mockVenues'
import type { VenueInfo } from '../types'
import EventCard from '../components/ui/EventCard.vue'

const catalog = useCatalogStore()
const router = useRouter()

// Carousel State
const currentSlide = ref(0)
const isAutoPlayPaused = ref(false)
let autoPlayTimer: ReturnType<typeof setInterval> | null = null

const featuredSlides = computed(() => {
  return catalog.featuredEvents && catalog.featuredEvents.length > 0
    ? catalog.featuredEvents
    : catalog.events.slice(0, 3)
})

const activeSlide = computed(() => {
  return featuredSlides.value[currentSlide.value] || featuredSlides.value[0]
})

function nextSlide() {
  if (featuredSlides.value.length === 0) return
  currentSlide.value = (currentSlide.value + 1) % featuredSlides.value.length
}

function prevSlide() {
  if (featuredSlides.value.length === 0) return
  currentSlide.value = (currentSlide.value - 1 + featuredSlides.value.length) % featuredSlides.value.length
}

function setSlide(idx: number) {
  currentSlide.value = idx
}

onMounted(() => {
  autoPlayTimer = setInterval(() => {
    if (!isAutoPlayPaused.value) {
      nextSlide()
    }
  }, 6000)

  promoTimer = setInterval(() => {
    if (!isPromoPaused.value) {
      nextPromo()
    }
  }, 4500)
})

onUnmounted(() => {
  if (autoPlayTimer) clearInterval(autoPlayTimer)
  if (promoTimer) clearInterval(promoTimer)
})

// Secciones fijas: muestran a lo sumo 3 eventos
const recentEvents = computed(() => catalog.recentlyAddedEvents.slice(0, 3))
const upcomingEvents = computed(() => catalog.upcomingEvents.slice(0, 3))

function goToEvent(id: string) {
  router.push({ name: 'event-detail', params: { id } })
}

// 1. Servicios del Recinto (Comida, Bebidas, Merchandising)
const services = [
  {
    id: 'food',
    title: 'Puntos Gastronómicos',
    subtitle: 'Comida & Snacks',
    description: 'Variedad de opciones gourmet, vegetarianas y clásicas distribuidas en todos los niveles del recinto.',
    icon: 'restaurant',
    imageUrl: 'https://images.unsplash.com/photo-1555396273-367ea4eb4db5?auto=format&fit=crop&w=800&q=80'
  },
  {
    id: 'drinks',
    title: 'Barras de Bebidas',
    subtitle: 'Cervezas & Tragos',
    description: 'Barras rápidas con bebidas con y sin alcohol, agua mineral y servicio de recarga sin filas largas.',
    icon: 'local_bar',
    imageUrl: 'https://images.unsplash.com/photo-1514933651103-005eec06c04b?auto=format&fit=crop&w=800&q=80'
  },
  {
    id: 'merch',
    title: 'Merchandising',
    subtitle: 'Tienda Oficial',
    description: 'Stands oficiales con remeras, buzos, gorras, vinilos y artículos exclusivos de cada banda y espectáculo.',
    icon: 'shopping_bag',
    imageUrl: 'https://images.unsplash.com/photo-1529374255404-311a2a4f1fd9?auto=format&fit=crop&w=800&q=80'
  }
]

// 2. Ofertas de Medios de Pago (9 promociones rotativas con transición y centro destacado)
export interface BankPromotion {
  id: string
  bank: string
  card: string
  title: string
  subtitle: string
  tag: string
  imageUrl: string
  accentColor: string
}

const allBankPromotions: BankPromotion[] = [
  {
    id: 'promo-santander',
    bank: 'Santander',
    card: 'Visa & Mastercard Santander',
    title: 'Hasta 6 Cuotas Sin Interés',
    subtitle: 'En todas las funciones, ubicaciones y fases de venta oficial.',
    tag: '6 Cuotas Sin Interés',
    imageUrl: 'https://images.unsplash.com/photo-1559526324-4b87b5e36e44?auto=format&fit=crop&w=1000&q=80',
    accentColor: '#e11d48'
  },
  {
    id: 'promo-galicia',
    bank: 'Galicia',
    card: 'Visa & Mastercard Galicia',
    title: '3 Cuotas Sin Interés + 15% OFF',
    subtitle: 'Abonando con tarjetas de crédito seleccionadas a través de Nave.',
    tag: '3 Cuotas + Reintegro',
    imageUrl: 'https://images.unsplash.com/photo-1563013544-824ae1b704d3?auto=format&fit=crop&w=1000&q=80',
    accentColor: '#f59e0b'
  },
  {
    id: 'promo-bbva',
    bank: 'BBVA',
    card: 'Visa & Mastercard BBVA',
    title: 'Preventa Exclusiva & 6 Cuotas',
    subtitle: 'Acceso prioritario a tickets y financiación en cuotas fijas.',
    tag: 'Preventa Exclusiva',
    imageUrl: 'https://images.unsplash.com/photo-1556740738-b6a63e27c4df?auto=format&fit=crop&w=1000&q=80',
    accentColor: '#0284c7'
  },
  {
    id: 'promo-modo',
    bank: 'MODO',
    card: 'Billetera Virtual & Débito',
    title: '20% de Reintegro Inmediato',
    subtitle: 'Tope de ahorro pagando con QR desde tu app bancaria o MODO.',
    tag: '20% Reintegro QR',
    imageUrl: 'https://images.unsplash.com/photo-1556742049-0a67c5574f73?auto=format&fit=crop&w=1000&q=80',
    accentColor: '#14b8a6'
  },
  {
    id: 'promo-macro',
    bank: 'Banco Macro',
    card: 'Visa Macro & Macro BMA',
    title: 'Hasta 6 Cuotas Sin Interés',
    subtitle: 'Financiación sin recargo en entradas para grandes estadios.',
    tag: '6 Cuotas Macro',
    imageUrl: 'https://images.unsplash.com/photo-1556742111-a301076d9d18?auto=format&fit=crop&w=1000&q=80',
    accentColor: '#0055b3'
  },
  {
    id: 'promo-naranja',
    bank: 'Naranja X',
    card: 'Tarjeta Naranja X',
    title: 'Plan Z: 3 Cuotas Cero Interés',
    subtitle: 'Elegí cómo pagar tu resumen con el clásico beneficio de Plan Z.',
    tag: 'Plan Z Cero Interés',
    imageUrl: 'https://images.unsplash.com/photo-1607604276583-eef5d076aa5f?auto=format&fit=crop&w=1000&q=80',
    accentColor: '#f97316'
  },
  {
    id: 'promo-nacion',
    bank: 'Banco Nación',
    card: 'BNA+ & Visa Nación',
    title: '15% de Ahorro + 3 Cuotas',
    subtitle: 'Comprando tus entradas a través de la billetera virtual BNA+.',
    tag: '15% Ahorro BNA+',
    imageUrl: 'https://images.unsplash.com/photo-1589758438368-0ad531db3366?auto=format&fit=crop&w=1000&q=80',
    accentColor: '#38bdf8'
  },
  {
    id: 'promo-amex',
    bank: 'American Express',
    card: 'The Platinum & Gold Card',
    title: 'Beneficios VIP & Cuotas Fijas',
    subtitle: 'Acceso a salas Lounge en recintos seleccionados y cuotas fijas.',
    tag: 'VIP Lounge & Cuotas',
    imageUrl: 'https://images.unsplash.com/photo-1563013544-b03a8d11634b?auto=format&fit=crop&w=1000&q=80',
    accentColor: '#94a3b8'
  },
  {
    id: 'promo-ciudad',
    bank: 'Banco Ciudad',
    card: 'Visa & Mastercard Ciudad',
    title: 'Hasta 25% de Ahorro en Teatro',
    subtitle: 'Descuento especial en funciones teatrales y espectáculos clásicos.',
    tag: '25% Ahorro Teatro',
    imageUrl: 'https://images.unsplash.com/photo-1526304640581-d334cdbbf45e?auto=format&fit=crop&w=1000&q=80',
    accentColor: '#10b981'
  }
]

// Promociones visibles (3 simultáneas: izquierda, centro ancho, derecha)
const promoCurrentIndex = ref(0)
const isPromoPaused = ref(false)
let promoTimer: ReturnType<typeof setInterval> | null = null

const visiblePromotions = computed(() => {
  const len = allBankPromotions.length
  const idx = promoCurrentIndex.value
  return [
    allBankPromotions[idx % len]!,
    allBankPromotions[(idx + 1) % len]!,
    allBankPromotions[(idx + 2) % len]!
  ]
})

function nextPromo() {
  promoCurrentIndex.value = (promoCurrentIndex.value + 1) % allBankPromotions.length
}

function prevPromo() {
  promoCurrentIndex.value = (promoCurrentIndex.value - 1 + allBankPromotions.length) % allBankPromotions.length
}

function setPromo(idx: number) {
  promoCurrentIndex.value = idx
}

// 3. Conoce Nuestros Recintos (Lugares, Sedes, Accesos y Estacionamiento)
const venueImages: Record<string, string> = {
  'estadio-monumental': 'https://images.unsplash.com/photo-1577223625816-7546f13df25d?auto=format&fit=crop&w=1200&q=80',
  'arena-buenos-aires': 'https://images.unsplash.com/photo-1516450360452-9312f5e86fc7?auto=format&fit=crop&w=1200&q=80',
  'teatro-rex': 'https://images.unsplash.com/photo-1507676184212-d03ab07a01bf?auto=format&fit=crop&w=1200&q=80'
}

export interface VenueWithImage extends VenueInfo {
  heroImage: string
}

const venuesList = computed<VenueWithImage[]>(() => {
  return mockVenues.map(v => ({
    ...v,
    heroImage: venueImages[v.id] || 'https://images.unsplash.com/photo-1514525253161-7a46d19cd819?auto=format&fit=crop&w=1200&q=80'
  }))
})

const selectedVenue = ref<VenueWithImage | null>(null)
const isVenueModalOpen = ref(false)

function openVenueModal(venue: VenueWithImage) {
  selectedVenue.value = venue
  isVenueModalOpen.value = true
}
</script>

<template>
  <div class="flex flex-col w-full gap-16 pb-20">
    <!-- 1. FEATURED EVENTS CAROUSEL (Simplificado según requerimiento) -->
    <section
      v-if="activeSlide"
      @mouseenter="isAutoPlayPaused = true"
      @mouseleave="isAutoPlayPaused = false"
      class="relative w-full overflow-hidden h-[55vh] min-h-[440px] max-h-[600px] bg-surface group border-b border-border-subtle"
    >
      <!-- Background Image with Fade Transition -->
      <transition name="carousel-fade" mode="out-in">
        <div
          :key="activeSlide.id"
          class="absolute inset-0 w-full h-full bg-cover bg-center transition-transform duration-1000 ease-out group-hover:scale-105"
          :style="{ backgroundImage: `url(${activeSlide.imageUrl})` }"
        ></div>
      </transition>

      <!-- Gradient Vignette & Dark Overlay -->
      <div
        class="absolute inset-0 z-10 pointer-events-none"
        style="background: linear-gradient(to top, var(--color-canvas-base) 0%, rgba(11, 19, 30, 0.85) 45%, rgba(11, 19, 30, 0.4) 75%, transparent 100%), linear-gradient(to right, rgba(11, 19, 30, 0.9) 0%, rgba(11, 19, 30, 0.5) 40%, transparent 80%);"
      ></div>

      <!-- Flecha Izquierda (Exactamente a la mitad de la altura del div) -->
      <button
        @click="prevSlide"
        class="absolute left-3 md:left-6 top-1/2 -translate-y-1/2 z-30 w-11 h-11 rounded-full bg-surface-card/80 hover:bg-primary-container border border-border-subtle text-white flex items-center justify-center transition-all shadow-lg cursor-pointer backdrop-blur-sm hover:scale-105 active:scale-95"
        aria-label="Evento anterior"
      >
        <span class="material-symbols-outlined text-2xl">chevron_left</span>
      </button>

      <!-- Flecha Derecha (Exactamente a la mitad de la altura del div) -->
      <button
        @click="nextSlide"
        class="absolute right-3 md:right-6 top-1/2 -translate-y-1/2 z-30 w-11 h-11 rounded-full bg-surface-card/80 hover:bg-primary-container border border-border-subtle text-white flex items-center justify-center transition-all shadow-lg cursor-pointer backdrop-blur-sm hover:scale-105 active:scale-95"
        aria-label="Evento siguiente"
      >
        <span class="material-symbols-outlined text-2xl">chevron_right</span>
      </button>

      <!-- Slide Content Overlay (Sin precio y sin botón de detalles) -->
      <div class="absolute inset-0 w-full max-w-[80vw] mx-auto px-12 md:px-16 p-6 md:p-10 flex flex-col justify-end pb-14 md:pb-16 z-20 pointer-events-none">
        <div class="flex flex-col gap-3 max-w-2xl text-white pointer-events-auto">
          <!-- Tag Categoría -->
          <div>
            <span class="inline-flex items-center gap-1.5 px-3 py-1 bg-surface-module/90 text-primary border border-border-subtle font-mono text-xs uppercase tracking-widest rounded-full font-semibold backdrop-blur-md shadow-sm">
              <span class="w-2 h-2 rounded-full bg-secondary animate-pulse"></span>
              <span>{{ activeSlide.badgeText || 'EVENTO DESTACADO' }}</span>
            </span>
          </div>

          <!-- Título -->
          <h1 class="font-display text-3xl sm:text-4xl md:text-5xl lg:text-[54px] md:leading-[56px] lg:leading-[62px] font-extrabold text-high-emphasis tracking-tight drop-shadow-md">
            {{ activeSlide.title }}
          </h1>

          <!-- Subtítulo -->
          <p class="font-body text-sm sm:text-base md:text-lg text-medium-emphasis opacity-90 max-w-2xl line-clamp-2">
            {{ activeSlide.subtitle }}
          </p>

          <!-- Fecha en Español (dd mes completo año) y Recinto -->
          <div class="flex flex-wrap items-center gap-4 pt-1 font-mono text-xs md:text-sm text-medium-emphasis">
            <span class="flex items-center gap-1.5 text-primary font-semibold">
              <span class="material-symbols-outlined text-[18px]">calendar_today</span>
              <span>{{ activeSlide.date }}</span>
            </span>
            <span class="flex items-center gap-1.5 text-medium-emphasis">
              <span class="material-symbols-outlined text-[18px] text-secondary">location_on</span>
              <span>{{ activeSlide.venue }}</span>
            </span>
          </div>

          <!-- Botón Único de Acción -->
          <div class="pt-2">
            <button
              @click="goToEvent(activeSlide.id)"
              class="bg-primary-container hover:bg-ocean-primary-hover text-white font-display text-sm md:text-base font-semibold px-7 py-3.5 rounded-xl transition-all duration-300 inline-flex items-center justify-center gap-2 border border-primary/30 shadow-[0_0_20px_rgba(0,104,143,0.35)] cursor-pointer hover:scale-[1.02] active:scale-[0.98]"
            >
              <span>Ver Entradas</span>
              <span class="material-symbols-outlined text-sm">arrow_forward</span>
            </button>
          </div>
        </div>
      </div>

      <!-- Control Inferior del Carrusel: Simples Círculos -->
      <div class="absolute bottom-4 left-1/2 -translate-x-1/2 flex items-center gap-2.5 z-20">
        <button
          v-for="(_, idx) in featuredSlides"
          :key="idx"
          @click="setSlide(idx)"
          class="w-3 h-3 rounded-full transition-all cursor-pointer"
          :class="currentSlide === idx ? 'bg-primary scale-125 ring-2 ring-primary/40' : 'bg-border-subtle hover:bg-primary/60'"
          :aria-label="`Ir al evento ${idx + 1}`"
        ></button>
      </div>
    </section>

    <!-- 2. SECCIÓN 1: SHOWS RECIENTEMENTE AGREGADOS (Fondo SVG 100% ancho de pantalla) -->
    <section class="relative w-full overflow-hidden py-10">
      <!-- Background SVG: Wave Line con ancho expandido (>100vw) y centrado para evitar cortes visuales en animación -->
      <div class="absolute inset-0 pointer-events-none z-0 overflow-hidden select-none flex justify-center items-start">
        <img
          :src="'/Wave Line.svg'"
          alt=""
          aria-hidden="true"
          class="w-[125vw] min-w-[125%] max-w-none h-auto min-h-[380px] object-cover object-top opacity-35 select-none pointer-events-none shrink-0"
        />
      </div>

      <div class="relative z-10 w-full max-w-[80vw] mx-auto px-4 sm:px-6 lg:px-8 flex flex-col gap-6">
        <!-- Header con Botón Desplegable para Ver Todos los Eventos Disponibles -->
        <div class="flex flex-col sm:flex-row sm:items-end justify-between gap-4 border-b border-border-subtle pb-4">
          <div class="flex flex-col gap-1">
            <span class="font-mono text-xs text-primary uppercase tracking-wider font-semibold">
              Nuevos en Cartelera
            </span>
            <h2 class="font-display text-2xl sm:text-3xl font-bold text-high-emphasis">
              Recientemente Agregados
            </h2>
            <p class="font-body text-sm text-muted-light">
              Los últimos espectáculos y presentaciones sumadas a la cartelera.
            </p>
          </div>

          <!-- Botón hacia Cartelera Completa (Recientemente Agregados) -->
          <RouterLink
            to="/events"
            class="self-start sm:self-auto bg-surface-card hover:bg-surface-interactive text-primary hover:text-white font-mono text-xs font-semibold px-4 py-2.5 rounded-lg border border-border-subtle hover:border-primary/50 transition-all flex items-center gap-1.5 cursor-pointer shadow-sm group"
          >
            <span>Ver Todos los Eventos</span>
            <span class="material-symbols-outlined text-[16px] group-hover:translate-x-0.5 transition-transform">arrow_forward</span>
          </RouterLink>
        </div>

        <!-- Grid de Shows Recientes usando EventCard con variante light (fondo blanco y texto azul oscuro) -->
        <div class="grid grid-cols-1 md:grid-cols-3 gap-6">
          <EventCard
            v-for="event in recentEvents"
            :key="event.id"
            :event="event"
            variant="light"
            show-venue-icon="location"
            @click="goToEvent"
          />
        </div>
      </div>
    </section>

    <!-- 3. SECCIÓN 2: SHOWS PRÓXIMOS A OCURRIR (Fondo SVG 100% ancho de pantalla) -->
    <section class="relative w-full overflow-hidden py-10">
      <!-- Background SVG: oooscillate cubre el 100% del ancho de la pantalla -->
      <div class="absolute inset-0 pointer-events-none z-0 overflow-hidden select-none">
        <img
          :src="'/oooscillate.svg'"
          alt=""
          aria-hidden="true"
          class="w-full h-full object-cover object-center opacity-25 select-none pointer-events-none"
        />
      </div>

      <div class="relative z-10 w-full max-w-[80vw] mx-auto px-4 sm:px-6 lg:px-8 flex flex-col gap-6">
        <!-- Header con Botón Desplegable para Ver Todos los Eventos Disponibles -->
        <div class="flex flex-col sm:flex-row sm:items-end justify-between gap-4 border-b border-border-subtle pb-4">
          <div class="flex flex-col gap-1">
            <span class="font-mono text-xs text-secondary uppercase tracking-wider font-semibold">
              En Agenda Cercana
            </span>
            <h2 class="font-display text-2xl sm:text-3xl font-bold text-high-emphasis">
              Próximos a Ocurrir
            </h2>
            <p class="font-body text-sm text-muted-light">
              Funciones y fechas programadas para las próximas semanas.
            </p>
          </div>

          <!-- Botón hacia Cartelera Completa (Próximos a Ocurrir) -->
          <RouterLink
            to="/events"
            class="self-start sm:self-auto bg-surface-card hover:bg-surface-interactive text-secondary hover:text-white font-mono text-xs font-semibold px-4 py-2.5 rounded-lg border border-border-subtle hover:border-secondary/50 transition-all flex items-center gap-1.5 cursor-pointer shadow-sm group"
          >
            <span>Ver Todos los Eventos</span>
            <span class="material-symbols-outlined text-[16px] group-hover:translate-x-0.5 transition-transform">arrow_forward</span>
          </RouterLink>
        </div>

        <!-- Grid de Shows Próximos usando EventCard -->
        <div class="grid grid-cols-1 md:grid-cols-3 gap-6">
          <EventCard
            v-for="event in upcomingEvents"
            :key="event.id"
            :event="event"
            accent-color="secondary"
            show-venue-icon="location"
            @click="goToEvent"
          />
        </div>
      </div>
    </section>

    <!-- 4. SERVICIOS DEL RECINTO (Comida, Bebidas, Merchandising) -->
    <section class="w-full max-w-[80vw] mx-auto px-4 sm:px-6 lg:px-8">
      <div class="flex flex-col gap-6">
        <div class="flex flex-col gap-1 border-b border-border-subtle pb-4">
          <span class="font-mono text-xs text-primary uppercase tracking-wider font-semibold">
            Servicios en el Recinto
          </span>
          <h2 class="font-display text-2xl sm:text-3xl font-bold text-high-emphasis">
            ¿Qué encontrarás en el lugar?
          </h2>
          <p class="font-body text-sm text-muted-light">
            Información sobre los servicios disponibles para tu comodidad durante el espectáculo.
          </p>
        </div>

        <!-- 3 Tarjetas Simples de Servicio -->
        <div class="grid grid-cols-1 md:grid-cols-3 gap-6">
          <article
            v-for="service in services"
            :key="service.id"
            class="bg-surface-card border border-border-subtle rounded-2xl overflow-hidden flex flex-col hover:border-primary/40 transition-colors"
          >
            <div class="h-44 w-full overflow-hidden relative">
              <img
                :src="service.imageUrl"
                :alt="service.title"
                class="w-full h-full object-cover"
              />
              <div class="absolute inset-0 bg-gradient-to-t from-surface-card via-transparent to-transparent"></div>
              <div class="absolute bottom-3 left-4 flex items-center gap-2">
                <div class="w-8 h-8 rounded-lg bg-surface-dim/90 border border-border-subtle flex items-center justify-center text-primary">
                  <span class="material-symbols-outlined text-[18px]">{{ service.icon }}</span>
                </div>
                <span class="font-mono text-xs font-bold text-high-emphasis bg-surface/80 px-2 py-0.5 rounded border border-border-subtle">
                  {{ service.subtitle }}
                </span>
              </div>
            </div>

            <div class="p-5 flex flex-col gap-2 flex-1">
              <h3 class="font-display text-xl font-bold text-high-emphasis">
                {{ service.title }}
              </h3>
              <p class="font-body text-sm text-muted-light leading-relaxed">
                {{ service.description }}
              </p>
            </div>
          </article>
        </div>
      </div>
    </section>

    <!-- 5. CONOCE NUESTROS RECINTOS Y SEDES (Fondo SVG 100% Ancho: Blueprint Arquitectónico de Anfiteatro con Desvanecimiento Suave) -->
    <section class="relative w-full overflow-hidden py-10">
      <!-- Background SVG: Plano Arquitectónico / Propagación Acústica de Anfiteatro a 100% width con máscara de desvanecimiento -->
      <div
        class="absolute inset-0 pointer-events-none z-0 overflow-hidden select-none"
        style="mask-image: linear-gradient(to bottom, transparent 0%, black 15%, black 85%, transparent 100%); -webkit-mask-image: linear-gradient(to bottom, transparent 0%, black 15%, black 85%, transparent 100%);"
      >
        <img
          :src="'/venue-architecture.svg'"
          alt=""
          aria-hidden="true"
          class="w-full h-full object-cover object-center opacity-35 select-none pointer-events-none"
        />
      </div>

      <div class="relative z-10 w-full max-w-[80vw] mx-auto px-4 sm:px-6 lg:px-8 flex flex-col gap-6">
        <div class="flex flex-col gap-1 border-b border-border-subtle pb-4">
          <span class="font-mono text-xs text-primary uppercase tracking-wider font-semibold">
            Nuestros Escenarios
          </span>
          <h2 class="font-display text-2xl sm:text-3xl font-bold text-high-emphasis">
            Conoce los Lugares y Recintos
          </h2>
          <p class="font-body text-sm text-muted-light">
            Explora los principales escenarios donde suceden los espectáculos: conoce cómo son, dónde quedan, cómo llegar y sus opciones de estacionamiento oficial.
          </p>
        </div>

        <!-- 3 Portales Arquitectónicos Minimalistas (Fotografía Limpia y Tipografía Clara) -->
        <div class="grid grid-cols-1 lg:grid-cols-3 gap-6">
          <article
            v-for="venue in venuesList"
            :key="venue.id"
            @click="openVenueModal(venue)"
            class="relative h-[340px] sm:h-[380px] rounded-3xl overflow-hidden group cursor-pointer border border-border-subtle hover:border-primary/60 transition-all duration-500 shadow-xl hover:shadow-[0_16px_40px_rgba(0,104,143,0.3)] flex flex-col justify-end p-6"
          >
            <!-- Fotografía de Fondo Completa con Zoom Dinámico en Hover -->
            <img
              :src="venue.heroImage"
              :alt="venue.name"
              class="absolute inset-0 w-full h-full object-cover group-hover:scale-110 transition-transform duration-700 ease-out"
              loading="lazy"
            />

            <!-- Gradiente de Contraste Cinematográfico Suave -->
            <div
              class="absolute inset-0 pointer-events-none"
              style="background: linear-gradient(to top, rgba(11, 19, 30, 0.95) 0%, rgba(11, 19, 30, 0.5) 40%, transparent 75%);"
            ></div>

            <!-- Contenido Limpio y Reducido: Solo Tipo, Nombre, Dirección y Botón Circular -->
            <div class="relative z-10 flex items-end justify-between gap-4">
              <div class="flex flex-col gap-1 min-w-0">
                <span class="font-mono text-[11px] text-secondary uppercase tracking-widest font-bold">
                  {{ venue.type.replace('_', ' ') }}
                </span>
                <h3 class="font-display text-2xl font-black text-white group-hover:text-primary transition-colors tracking-tight truncate drop-shadow-md">
                  {{ venue.shortName || venue.name }}
                </h3>
                <p class="font-body text-xs text-medium-emphasis flex items-center gap-1.5 truncate">
                  <span class="material-symbols-outlined text-[15px] text-primary shrink-0">location_on</span>
                  <span class="truncate">{{ venue.address }}</span>
                </p>
              </div>

              <!-- Botón Circular Minimalista de Apertura -->
              <div
                class="w-11 h-11 rounded-full bg-surface-card/90 group-hover:bg-primary-container border border-border-subtle group-hover:border-primary/50 text-white flex items-center justify-center shrink-0 transition-all duration-300 shadow-lg group-hover:scale-110"
                title="Ver información del recinto"
              >
                <span class="material-symbols-outlined text-xl group-hover:translate-x-0.5 group-hover:-translate-y-0.5 transition-transform">
                  arrow_outward
                </span>
              </div>
            </div>
          </article>
        </div>
      </div>
    </section>

    <!-- 6. OFERTAS DE MEDIOS DE PAGO (Transición automática de imágenes, centro más ancho horizontalmente) -->
    <section class="w-full max-w-[80vw] mx-auto px-4 sm:px-6 lg:px-8">
      <div class="flex flex-col gap-6">
        <div class="flex flex-col gap-1 border-b border-border-subtle pb-4">
          <span class="font-mono text-xs text-secondary uppercase tracking-wider font-semibold">
            Financiación y Beneficios
          </span>
          <h2 class="font-display text-2xl sm:text-3xl font-bold text-high-emphasis">
            Promociones Bancarias
          </h2>
          <p class="font-body text-sm text-muted-light">
            Aprovecha cuotas sin interés y reintegros con tarjetas seleccionadas en el checkout oficial.
          </p>
        </div>

        <!-- Grid de 3 Contenedores: Misma altura uniforme, el del medio más ancho horizontalmente (1fr / 1.6fr / 1fr) -->
        <div class="grid grid-cols-1 md:grid-cols-[1fr_1.6fr_1fr] gap-6 items-stretch">
          <div
            v-for="(promo, slotIdx) in visiblePromotions"
            :key="slotIdx"
            class="relative h-64 sm:h-72 md:h-80 rounded-3xl overflow-hidden border border-border-subtle hover:border-primary/50 transition-colors shadow-lg bg-surface-card"
          >
            <!-- Transición suave de la imagen al cambiar automáticamente con el tiempo -->
            <transition name="promo-fade" mode="out-in">
              <div
                :key="promo.id"
                class="w-full h-full relative flex flex-col justify-between p-6 group cursor-pointer"
              >
                <!-- Imagen Fotográfica de la Promoción de Fondo -->
                <img
                  :src="promo.imageUrl"
                  :alt="promo.title"
                  class="absolute inset-0 w-full h-full object-cover group-hover:scale-105 transition-transform duration-700 ease-out"
                />

                <!-- Overlay de Gradiente para Legibilidad Óptima -->
                <div
                  class="absolute inset-0 pointer-events-none"
                  style="background: linear-gradient(to top, rgba(8, 14, 23, 0.96) 0%, rgba(8, 14, 23, 0.6) 50%, rgba(8, 14, 23, 0.25) 100%);"
                ></div>

                <!-- Cabecera: Nombre de la Entidad Bancaria y Tag -->
                <div class="relative z-10 flex items-center justify-between gap-2">
                  <span class="px-3 py-1 rounded-full font-mono text-xs font-bold shadow-md backdrop-blur-md border bg-slate-950/80 text-white border-white/10">
                    {{ promo.bank }}
                  </span>

                  <span class="font-mono text-xs font-bold text-secondary bg-surface-card/90 px-2.5 py-0.5 rounded-full border border-secondary/30 backdrop-blur-sm">
                    {{ promo.tag }}
                  </span>
                </div>

                <!-- Bloque Inferior con Información de la Promoción -->
                <div class="relative z-10 flex flex-col gap-1.5">
                  <h3
                    class="font-display font-black text-white group-hover:text-primary transition-colors leading-tight"
                    :class="slotIdx === 1 ? 'text-2xl sm:text-3xl' : 'text-xl'"
                  >
                    {{ promo.title }}
                  </h3>

                  <p
                    class="font-body text-xs text-muted-light line-clamp-1"
                    :class="slotIdx === 1 ? 'sm:text-sm sm:line-clamp-2' : ''"
                  >
                    {{ promo.subtitle }}
                  </p>

                  <div class="pt-2 border-t border-white/10 flex items-center justify-between text-[11px] font-mono text-medium-emphasis">
                    <span class="truncate max-w-[200px]">{{ promo.card }}</span>
                    <span class="text-primary font-semibold flex items-center gap-1 shrink-0">
                      <span class="material-symbols-outlined text-sm">credit_card</span>
                      <span>En Checkout</span>
                    </span>
                  </div>
                </div>
              </div>
            </transition>
          </div>
        </div>
      </div>
    </section>

    <!-- 7. REDIRECCIÓN A FAQ (Cartel Ampliado, Destacado y de Alto Impacto Visual) -->
    <section class="w-full max-w-[80vw] mx-auto px-4 sm:px-6 lg:px-8">
      <div class="relative overflow-hidden bg-gradient-to-br from-surface-card via-surface-elevated to-surface-container-deep border border-border-subtle rounded-3xl p-8 sm:p-12 md:p-16 flex flex-col lg:flex-row items-center justify-between gap-8 sm:gap-10 shadow-2xl">
        <!-- Resplandores ambientales de fondo -->
        <div class="absolute -right-24 -bottom-24 w-96 h-96 bg-primary/10 rounded-full blur-3xl pointer-events-none"></div>
        <div class="absolute -left-20 -top-20 w-72 h-72 bg-secondary/10 rounded-full blur-3xl pointer-events-none"></div>

        <div class="relative z-10 flex flex-col sm:flex-row items-center sm:items-start text-center sm:text-left gap-6 sm:gap-8 max-w-3xl">
          <!-- Icono de Gran Tamaño -->
          <div class="w-20 h-20 sm:w-24 sm:h-24 rounded-3xl bg-primary-container/20 border border-primary/40 flex items-center justify-center text-primary shrink-0 shadow-lg backdrop-blur-md">
            <span class="material-symbols-outlined text-4xl sm:text-5xl">help_outline</span>
          </div>

          <div class="flex flex-col gap-3">
            <div class="inline-flex items-center gap-2 self-center sm:self-start">
              <span class="px-3 py-1 rounded-full bg-surface-module border border-border-subtle font-mono text-[11px] font-bold text-secondary uppercase tracking-widest">
                Centro de Ayuda & Asistencia Oficial
              </span>
            </div>

            <h2 class="font-display text-2xl sm:text-3xl md:text-4xl font-extrabold text-high-emphasis tracking-tight">
              ¿Tienes preguntas o dudas sobre tu próximo evento?
            </h2>

            <p class="font-body text-base sm:text-lg text-muted-light leading-relaxed">
              Encuentra respuestas rápidas sobre entradas digitales con código QR dinámico, políticas de cancelación, estacionamiento oficial y accesibilidad.
            </p>

            <!-- Tags rápidos de temas comunes -->
            <div class="flex flex-wrap items-center justify-center sm:justify-start gap-2 pt-2 font-mono text-xs text-medium-emphasis">
              <span class="px-2.5 py-1 rounded-md bg-surface-card border border-border-subtle/80 flex items-center gap-1">
                <span class="material-symbols-outlined text-sm text-primary">qr_code_2</span>
                Entradas Digitales
              </span>
              <span class="px-2.5 py-1 rounded-md bg-surface-card border border-border-subtle/80 flex items-center gap-1">
                <span class="material-symbols-outlined text-sm text-secondary">local_parking</span>
                Acceso a Cocheras
              </span>
              <span class="px-2.5 py-1 rounded-md bg-surface-card border border-border-subtle/80 flex items-center gap-1">
                <span class="material-symbols-outlined text-sm text-primary">sync_alt</span>
                Transferencia de Tickets
              </span>
            </div>
          </div>
        </div>

        <div class="relative z-10 w-full sm:w-auto shrink-0 flex justify-center">
          <RouterLink
            to="/help"
            class="w-full sm:w-auto bg-primary-container hover:bg-ocean-primary-hover text-white font-display text-base sm:text-lg font-bold px-8 py-5 rounded-2xl border border-primary/40 shadow-[0_0_30px_rgba(0,104,143,0.4)] transition-all flex items-center justify-center gap-3 cursor-pointer whitespace-nowrap hover:scale-105 active:scale-95"
          >
            <span>Ir a Preguntas Frecuentes</span>
            <span class="material-symbols-outlined text-2xl">arrow_forward</span>
          </RouterLink>
        </div>
      </div>
    </section>

    <!-- MODAL DETALLE DEL RECINTO (Radix Vue Dialog) -->
    <DialogRoot v-model:open="isVenueModalOpen">
      <DialogPortal>
        <DialogOverlay class="fixed inset-0 bg-black/80 backdrop-blur-sm z-50 transition-opacity" />
        <DialogContent
          v-if="selectedVenue"
          class="fixed left-1/2 top-1/2 -translate-x-1/2 -translate-y-1/2 w-[92vw] max-w-xl max-h-[85vh] overflow-y-auto bg-surface-card border border-border-subtle rounded-2xl p-6 sm:p-8 text-high-emphasis z-50 shadow-2xl focus:outline-none"
        >
          <!-- Header con Imagen del Recinto -->
          <div class="relative -mx-6 -mt-6 sm:-mx-8 sm:-mt-8 mb-6 h-48 overflow-hidden rounded-t-2xl">
            <img
              :src="selectedVenue.heroImage"
              :alt="selectedVenue.name"
              class="w-full h-full object-cover"
            />
            <div class="absolute inset-0 bg-gradient-to-t from-surface-card via-surface-card/60 to-transparent"></div>

            <!-- Botón Cerrar (Radix DialogClose) -->
            <DialogClose
              class="absolute top-4 right-4 w-9 h-9 rounded-full bg-surface-card/80 hover:bg-surface-interactive border border-border-subtle text-high-emphasis flex items-center justify-center transition-all cursor-pointer backdrop-blur-sm focus:outline-none"
              aria-label="Cerrar modal"
            >
              <span class="material-symbols-outlined text-xl">close</span>
            </DialogClose>

            <div class="absolute bottom-4 left-6 sm:left-8">
              <span class="inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded bg-surface/90 border border-border-subtle text-secondary font-mono text-xs font-bold uppercase tracking-wider backdrop-blur-sm">
                <span class="material-symbols-outlined text-[14px]">stadium</span>
                <span>{{ selectedVenue.type.replace('_', ' ') }}</span>
              </span>
            </div>
          </div>

          <!-- Título y Subtítulo -->
          <DialogTitle class="font-display text-2xl sm:text-3xl font-extrabold text-high-emphasis mb-1">
            {{ selectedVenue.name }}
          </DialogTitle>
          <DialogDescription class="font-mono text-xs text-muted-light mb-6 flex flex-wrap items-center gap-2">
            <span>{{ selectedVenue.city }}, {{ selectedVenue.country }}</span>
            <span>•</span>
            <span class="text-primary font-semibold">Capacidad: {{ selectedVenue.capacity.toLocaleString('es-AR') }} espectadores</span>
          </DialogDescription>

          <div class="flex flex-col gap-5 text-sm">
            <!-- 1. DÓNDE QUEDA CON GOOGLE MAPS EMBED -->
            <div class="p-4 rounded-xl bg-surface-container-deep border border-border-subtle flex flex-col gap-3">
              <div class="flex items-center justify-between">
                <div class="flex items-center gap-2 text-primary font-mono text-xs uppercase font-bold tracking-wider">
                  <span class="material-symbols-outlined text-[18px]">location_on</span>
                  <span>¿Dónde queda?</span>
                </div>
                <a
                  :href="selectedVenue.transit?.googleMapsUrl || `https://maps.google.com/?q=${encodeURIComponent(selectedVenue.address)}`"
                  target="_blank"
                  rel="noopener noreferrer"
                  class="inline-flex items-center gap-1 text-[11px] font-mono font-semibold text-secondary hover:text-white transition-colors"
                >
                  <span>Abrir en Google Maps</span>
                  <span class="material-symbols-outlined text-[14px]">open_in_new</span>
                </a>
              </div>
              <p class="font-body text-base font-semibold text-high-emphasis">
                {{ selectedVenue.address }}
              </p>

              <!-- Iframe interactivo de Google Maps sin clave requerida -->
              <div class="relative w-full h-52 sm:h-60 rounded-xl overflow-hidden border border-border-subtle bg-surface">
                <iframe
                  title="Ubicación en Google Maps"
                  width="100%"
                  height="100%"
                  style="border:0;"
                  loading="lazy"
                  allowfullscreen
                  referrerpolicy="no-referrer-when-downgrade"
                  :src="`https://maps.google.com/maps?q=${encodeURIComponent(selectedVenue.transit?.embedQuery || selectedVenue.address)}&t=&z=15&ie=UTF8&iwloc=&output=embed`"
                ></iframe>
              </div>
            </div>

            <!-- 2. TRANSPORTE PÚBLICO: COLECTIVOS, TRENES Y SUBTE -->
            <div
              v-if="selectedVenue.transit"
              class="p-4 rounded-xl bg-surface-container-deep border border-border-subtle flex flex-col gap-3"
            >
              <div class="flex items-center gap-2 text-primary font-mono text-xs uppercase font-bold tracking-wider">
                <span class="material-symbols-outlined text-[18px]">directions_bus</span>
                <span>Transporte Público Cercano</span>
              </div>

              <!-- Colectivos / Buses -->
              <div v-if="selectedVenue.transit.busLines && selectedVenue.transit.busLines.length > 0" class="flex flex-col gap-1.5">
                <span class="font-mono text-xs text-medium-emphasis flex items-center gap-1.5 font-semibold">
                  <span class="material-symbols-outlined text-[15px] text-secondary">directions_bus</span>
                  <span>Líneas de Colectivo:</span>
                </span>
                <div class="flex flex-wrap gap-1.5 pt-0.5">
                  <span
                    v-for="bus in selectedVenue.transit.busLines"
                    :key="bus"
                    class="px-2.5 py-0.5 rounded-md bg-surface-card border border-border-subtle text-high-emphasis font-mono text-xs font-bold shadow-sm"
                  >
                    {{ bus }}
                  </span>
                </div>
              </div>

              <!-- Trenes -->
              <div v-if="selectedVenue.transit.trains && selectedVenue.transit.trains.length > 0" class="flex flex-col gap-1.5 pt-2 border-t border-border-subtle/50">
                <span class="font-mono text-xs text-medium-emphasis flex items-center gap-1.5 font-semibold">
                  <span class="material-symbols-outlined text-[15px] text-secondary">train</span>
                  <span>Líneas de Tren:</span>
                </span>
                <ul class="space-y-1">
                  <li
                    v-for="(train, tIdx) in selectedVenue.transit.trains"
                    :key="tIdx"
                    class="font-body text-xs text-high-emphasis flex items-start gap-1.5"
                  >
                    <span class="material-symbols-outlined text-[14px] text-secondary mt-0.5 shrink-0">fiber_manual_record</span>
                    <span>{{ train }}</span>
                  </li>
                </ul>
              </div>

              <!-- Subte / Metro -->
              <div v-if="selectedVenue.transit.subway && selectedVenue.transit.subway.length > 0" class="flex flex-col gap-1.5 pt-2 border-t border-border-subtle/50">
                <span class="font-mono text-xs text-medium-emphasis flex items-center gap-1.5 font-semibold">
                  <span class="material-symbols-outlined text-[15px] text-secondary">subway</span>
                  <span>Líneas de Subte / Conexiones:</span>
                </span>
                <ul class="space-y-1">
                  <li
                    v-for="(sub, sIdx) in selectedVenue.transit.subway"
                    :key="sIdx"
                    class="font-body text-xs text-high-emphasis flex items-start gap-1.5"
                  >
                    <span class="material-symbols-outlined text-[14px] text-secondary mt-0.5 shrink-0">fiber_manual_record</span>
                    <span>{{ sub }}</span>
                  </li>
                </ul>
              </div>
            </div>

            <!-- 3. CÓMO LLEGAR Y ACCESOS PEATONALES / VEHICULARES -->
            <div
              v-if="selectedVenue.parking.accessRoutes && selectedVenue.parking.accessRoutes.length > 0"
              class="p-4 rounded-xl bg-surface-container-deep border border-border-subtle flex flex-col gap-2.5"
            >
              <div class="flex items-center gap-2 text-secondary font-mono text-xs uppercase font-bold tracking-wider">
                <span class="material-symbols-outlined text-[18px]">alt_route</span>
                <span>Accesos y Rutas Recomendadas</span>
              </div>
              <ul class="space-y-2 font-body text-xs text-medium-emphasis">
                <li
                  v-for="(route, rIdx) in selectedVenue.parking.accessRoutes"
                  :key="rIdx"
                  class="flex items-start gap-2.5"
                >
                  <span class="material-symbols-outlined text-[16px] text-secondary mt-0.5 shrink-0">turn_right</span>
                  <span class="leading-relaxed">{{ route }}</span>
                </li>
              </ul>
            </div>

            <!-- 4. ESTACIONAMIENTO OFICIAL Y PRECIO -->
            <div class="p-4 rounded-xl bg-surface-container-deep border border-border-subtle flex flex-col gap-3">
              <div class="flex items-center justify-between">
                <div class="flex items-center gap-2 text-primary font-mono text-xs uppercase font-bold tracking-wider">
                  <span class="material-symbols-outlined text-[18px]">local_parking</span>
                  <span>Estacionamiento Oficial</span>
                </div>
                <span
                  v-if="selectedVenue.parking.available"
                  class="px-2.5 py-1 rounded bg-secondary/20 text-secondary border border-secondary/40 font-mono text-xs font-bold"
                >
                  Disponible
                </span>
              </div>

              <h4 class="font-display text-sm font-bold text-high-emphasis">
                {{ selectedVenue.parking.title }}
              </h4>

              <p class="font-body text-xs text-muted-light leading-relaxed">
                {{ selectedVenue.parking.description }}
              </p>

              <!-- Tarifa destacada -->
              <div class="pt-2 border-t border-border-subtle/50 flex items-center justify-between bg-surface-card/60 -mx-4 -mb-4 p-3.5 rounded-b-xl">
                <span class="font-mono text-xs text-medium-emphasis">Tarifa estimada por vehículo:</span>
                <span class="font-mono text-base font-extrabold text-primary bg-primary-container/20 px-3 py-1 rounded-lg border border-primary/40">
                  ${{ selectedVenue.parking.price?.toLocaleString('es-AR') }} ARS
                </span>
              </div>
            </div>
          </div>

          <!-- Botón de acción al pie -->
          <div class="mt-6 pt-4 border-t border-border-subtle flex justify-end">
            <DialogClose as-child>
              <button
                type="button"
                class="bg-surface-card hover:bg-surface-interactive text-high-emphasis font-display text-sm font-semibold px-5 py-2.5 rounded-xl border border-border-subtle transition-all cursor-pointer"
              >
                Cerrar
              </button>
            </DialogClose>
          </div>
        </DialogContent>
      </DialogPortal>
    </DialogRoot>
  </div>
</template>

<style scoped>
.carousel-fade-enter-active,
.carousel-fade-leave-active {
  transition: opacity 0.6s ease-in-out;
}
.carousel-fade-enter-from,
.carousel-fade-leave-to {
  opacity: 0;
}

.promo-fade-enter-active,
.promo-fade-leave-active {
  transition: opacity 0.5s ease-in-out, transform 0.5s ease-in-out;
}
.promo-fade-enter-from {
  opacity: 0;
  transform: scale(0.96);
}
.promo-fade-leave-to {
  opacity: 0;
  transform: scale(1.04);
}
</style>
