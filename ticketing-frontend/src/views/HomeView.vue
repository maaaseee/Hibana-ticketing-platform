<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { useCatalogStore } from '../stores/catalog'
import { mockVenues } from '../data/mockVenues'
import type { VenueInfo } from '../types'

import HeroFeaturedCarousel from '../components/home/HeroFeaturedCarousel.vue'
import ShowcaseSection from '../components/home/ShowcaseSection.vue'
import VenueServicesGrid from '../components/home/VenueServicesGrid.vue'
import VenueShowcasePortals from '../components/home/VenueShowcasePortals.vue'
import BankingOffersTicker, { type BankPromotion } from '../components/home/BankingOffersTicker.vue'
import HelpFaqCtaBanner from '../components/home/HelpFaqCtaBanner.vue'
import VenueDetailModal, { type VenueWithImage } from '../components/home/VenueDetailModal.vue'

export type { BankPromotion, VenueWithImage }

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
  catalog.fetchHomeHighlights()

  autoPlayTimer = setInterval(() => {
    if (!isAutoPlayPaused.value) {
      nextSlide()
    }
  }, 6000)
})

onUnmounted(() => {
  if (autoPlayTimer) clearInterval(autoPlayTimer)
})

// Secciones fijas: muestran a lo sumo 3 eventos cada una
const recentEvents = computed(() => {
  if (catalog.homeHighlights.recentlyAdded && catalog.homeHighlights.recentlyAdded.length > 0) {
    return catalog.homeHighlights.recentlyAdded.slice(0, 3)
  }
  return catalog.recentlyAddedEvents.slice(0, 3)
})

const upcomingEvents = computed(() => {
  if (catalog.homeHighlights.upcoming && catalog.homeHighlights.upcoming.length > 0) {
    return catalog.homeHighlights.upcoming.slice(0, 3)
  }
  return catalog.upcomingEvents.slice(0, 3)
})

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

// 2. Conoce Nuestros Recintos (Lugares, Sedes, Accesos y Estacionamiento)
const venueImages: Record<string, string> = {
  'estadio-monumental': 'https://images.unsplash.com/photo-1577223625816-7546f13df25d?auto=format&fit=crop&w=1200&q=80',
  'arena-buenos-aires': 'https://images.unsplash.com/photo-1516450360452-9312f5e86fc7?auto=format&fit=crop&w=1200&q=80',
  'teatro-rex': 'https://images.unsplash.com/photo-1507676184212-d03ab07a01bf?auto=format&fit=crop&w=1200&q=80'
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
  <div class="flex flex-col gap-16 md:gap-24 pb-20">
    <!-- 1. HERO CAROUSEL DESTACADOS -->
    <HeroFeaturedCarousel
      :slides="featuredSlides"
      v-model:current-slide="currentSlide"
      v-model:is-paused="isAutoPlayPaused"
      @prev="prevSlide"
      @next="nextSlide"
      @set-slide="setSlide"
      @go-to-event="goToEvent"
    />

    <!-- 2. SECCIÓN RECINTOS RECIENTEMENTE AGREGADOS (Con SVG Wave dinámico de fondo) -->
    <ShowcaseSection
      tag="Nuevos en Cartelera"
      title="Recientemente Agregados"
      description="Los últimos espectáculos anunciados con disponibilidad de tickets oficiales."
      bg-svg="/images/backgrounds/wave-haikei.svg"
      bg-variant="wave"
      catalog-link-text="Ver Todos los Eventos"
      :events="recentEvents"
      @select-event="goToEvent"
    />

    <!-- 3. SECCIÓN PRÓXIMOS A OCURRIR (Con SVG oooscillate dinámico de fondo) -->
    <ShowcaseSection
      tag="En Agenda Cercana"
      title="Próximos a Ocurrir"
      description="Espectáculos programados para las próximas semanas. Últimas localidades disponibles."
      bg-svg="/images/backgrounds/oooscillate.svg"
      bg-variant="oooscillate"
      tag-color="secondary"
      accent-color="secondary"
      event-card-variant="light"
      catalog-link-text="Ver Todos los Eventos"
      :events="upcomingEvents"
      @select-event="goToEvent"
    />

    <!-- 4. SECCIÓN RECINTOS: SERVICIOS Y EXPERIENCIA -->
    <VenueServicesGrid :services="services" />

    <!-- 5. SECCIÓN RECINTOS Y ACCESOS -->
    <VenueShowcasePortals
      :venues="venuesList"
      @select-venue="openVenueModal"
    />

    <!-- 6. OFERTAS DE MEDIOS DE PAGO -->
    <BankingOffersTicker />

    <!-- 7. REDIRECCIÓN A FAQ -->
    <HelpFaqCtaBanner />

    <!-- MODAL DETALLE DEL RECINTO -->
    <VenueDetailModal
      v-model:open="isVenueModalOpen"
      :venue="selectedVenue"
    />
  </div>
</template>
