<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter, RouterLink } from 'vue-router'
import { useCatalogStore, getMinEventPrice } from '../stores/catalog'

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
})

onUnmounted(() => {
  if (autoPlayTimer) clearInterval(autoPlayTimer)
})

// Secciones fijas: muestran a lo sumo 3 eventos
const recentEvents = computed(() => {
  return catalog.recentlyAddedEvents.slice(0, 3)
})

const upcomingEvents = computed(() => {
  return catalog.upcomingEvents.slice(0, 3)
})

function goToEvent(id: string) {
  router.push({ name: 'event-detail', params: { id } })
}

// 1. Servicios Básicos en el Recinto (Comida, Bebidas, Merch)
const services = [
  {
    id: 'food',
    title: 'Comida',
    subtitle: 'Sector Gastronómico',
    description: 'Puntos de comidas rápidas, hamburguesas, minutas y opciones vegetarianas y sin TACC dentro del recinto.',
    icon: 'restaurant',
    imageUrl: 'https://images.unsplash.com/photo-1555396273-367ea4eb4db5?auto=format&fit=crop&w=800&q=80'
  },
  {
    id: 'drinks',
    title: 'Bebidas',
    subtitle: 'Barras & Hidratación',
    description: 'Puntos de hidratación gratuitos y barras de bebidas, gaseosas, agua y cerveza con despacho rápido en todos los sectores.',
    icon: 'local_bar',
    imageUrl: 'https://images.unsplash.com/photo-1551024709-8f23befc6f87?auto=format&fit=crop&w=800&q=80'
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

// 2. Ofertas de Medios de Pago (Imágenes Banners de Bancos y Tarjetas)
const promotions = [
  {
    id: 'santander-visa',
    bank: 'Santander',
    card: 'Visa & Mastercard',
    offer: 'Hasta 6 Cuotas Sin Interés',
    subtitle: 'En todas las funciones y fases de venta',
    tag: '6 Cuotas',
    imageUrl: 'https://images.unsplash.com/photo-1563013544-824ae1b704d3?auto=format&fit=crop&w=800&q=80',
    badgeColor: 'bg-[#86cffb] text-[#00344a]'
  },
  {
    id: 'galicia-mastercard',
    bank: 'Galicia',
    card: 'Mastercard',
    offer: '3 Cuotas Sin Interés',
    subtitle: 'Con tarjetas de crédito seleccionadas',
    tag: '3 Cuotas',
    imageUrl: 'https://images.unsplash.com/photo-1559526324-4b87b5e36e44?auto=format&fit=crop&w=800&q=80',
    badgeColor: 'bg-[#4fdbc8] text-[#003731]'
  },
  {
    id: 'modo-wallet',
    bank: 'MODO & Billeteras',
    card: 'Débito & Dinero en Cuenta',
    offer: '15% de Reintegro Inmediato',
    subtitle: 'Tope de reintegro en compras los días viernes y sábados',
    tag: '15% Reintegro',
    imageUrl: 'https://images.unsplash.com/photo-1556742049-0a67c5574f73?auto=format&fit=crop&w=800&q=80',
    badgeColor: 'bg-[#7bd0ff] text-[#00354a]'
  }
]
</script>

<template>
  <div class="flex flex-col w-full gap-16 pb-20">
    <!-- 1. FEATURED EVENTS CAROUSEL (Simplificado según requerimiento) -->
    <section
      v-if="activeSlide"
      @mouseenter="isAutoPlayPaused = true"
      @mouseleave="isAutoPlayPaused = false"
      class="relative w-full overflow-hidden h-[55vh] min-h-[440px] max-h-[600px] bg-[#0c141f] group border-b border-[#23354d]"
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
      <div class="absolute inset-0 bg-gradient-to-t from-[#0b131e] via-[#0b131e]/75 to-[#0b131e]/30"></div>

      <!-- Flecha Izquierda (Exactamente a la mitad de la altura del div) -->
      <button
        @click="prevSlide"
        class="absolute left-3 md:left-6 top-1/2 -translate-y-1/2 z-30 w-11 h-11 rounded-full bg-[#141e2e]/80 hover:bg-[#00688f] border border-[#23354d] text-white flex items-center justify-center transition-all shadow-lg cursor-pointer backdrop-blur-sm hover:scale-105 active:scale-95"
        aria-label="Evento anterior"
      >
        <span class="material-symbols-outlined text-2xl">chevron_left</span>
      </button>

      <!-- Flecha Derecha (Exactamente a la mitad de la altura del div) -->
      <button
        @click="nextSlide"
        class="absolute right-3 md:right-6 top-1/2 -translate-y-1/2 z-30 w-11 h-11 rounded-full bg-[#141e2e]/80 hover:bg-[#00688f] border border-[#23354d] text-white flex items-center justify-center transition-all shadow-lg cursor-pointer backdrop-blur-sm hover:scale-105 active:scale-95"
        aria-label="Evento siguiente"
      >
        <span class="material-symbols-outlined text-2xl">chevron_right</span>
      </button>

      <!-- Slide Content Overlay (Sin precio y sin botón de detalles) -->
      <div class="absolute inset-0 w-full max-w-[80vw] mx-auto px-12 md:px-16 p-6 md:p-10 flex flex-col justify-end pb-14 md:pb-16 pointer-events-none">
        <div class="flex flex-col gap-3 max-w-2xl text-white pointer-events-auto">
          <!-- Tag Categoría -->
          <div>
            <span class="inline-flex items-center gap-1.5 px-3 py-1 bg-[#162438]/90 text-[#86cffb] border border-[#23354d] font-mono text-xs uppercase tracking-widest rounded-full font-semibold backdrop-blur-md shadow-sm">
              <span class="w-2 h-2 rounded-full bg-[#4fdbc8] animate-pulse"></span>
              <span>{{ activeSlide.badgeText || 'EVENTO DESTACADO' }}</span>
            </span>
          </div>

          <!-- Título -->
          <h1 class="font-display text-3xl sm:text-4xl md:text-5xl lg:text-[54px] md:leading-[56px] lg:leading-[62px] font-extrabold text-[#f1f5f9] tracking-tight drop-shadow-md">
            {{ activeSlide.title }}
          </h1>

          <!-- Subtítulo -->
          <p class="font-body text-sm sm:text-base md:text-lg text-[#cbd5e1] opacity-90 max-w-2xl line-clamp-2">
            {{ activeSlide.subtitle }}
          </p>

          <!-- Fecha en Español (dd mes completo año) y Recinto -->
          <div class="flex flex-wrap items-center gap-4 pt-1 font-mono text-xs md:text-sm text-[#cbd5e1]">
            <span class="flex items-center gap-1.5 text-[#86cffb] font-semibold">
              <span class="material-symbols-outlined text-[18px]">calendar_today</span>
              <span>{{ activeSlide.date }}</span>
            </span>
            <span class="flex items-center gap-1.5 text-[#cbd5e1]">
              <span class="material-symbols-outlined text-[18px] text-[#4fdbc8]">location_on</span>
              <span>{{ activeSlide.venue }}</span>
            </span>
          </div>

          <!-- Botón Único de Acción -->
          <div class="pt-2">
            <button
              @click="goToEvent(activeSlide.id)"
              class="bg-[#00688f] hover:bg-[#007da8] text-white font-display text-sm md:text-base font-semibold px-7 py-3.5 rounded-xl transition-all duration-300 inline-flex items-center justify-center gap-2 border border-[#86cffb]/30 shadow-[0_0_20px_rgba(0,104,143,0.35)] cursor-pointer hover:scale-[1.02] active:scale-[0.98]"
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
          :class="currentSlide === idx ? 'bg-[#86cffb] scale-125 ring-2 ring-[#86cffb]/40' : 'bg-[#23354d] hover:bg-[#86cffb]/60'"
          :aria-label="`Ir al evento ${idx + 1}`"
        ></button>
      </div>
    </section>

    <!-- 2. SECCIÓN 1: SHOWS RECIENTEMENTE AGREGADOS (Fija, a lo sumo 3 eventos) -->
    <section class="w-full max-w-[80vw] mx-auto px-4 sm:px-6 lg:px-8">
      <div class="flex flex-col gap-6">
        <!-- Header con Botón Desplegable para Ver Todos los Eventos Disponibles -->
        <div class="flex flex-col sm:flex-row sm:items-end justify-between gap-4 border-b border-[#23354d] pb-4">
          <div class="flex flex-col gap-1">
            <span class="font-mono text-xs text-[#86cffb] uppercase tracking-wider font-semibold">
              Nuevos en Cartelera
            </span>
            <h2 class="font-display text-2xl sm:text-3xl font-bold text-[#f1f5f9]">
              Recientemente Agregados
            </h2>
            <p class="font-body text-sm text-[#94a3b8]">
              Los últimos espectáculos y presentaciones sumadas a la cartelera.
            </p>
          </div>

          <!-- Botón hacia Cartelera Completa (Recientemente Agregados) -->
          <RouterLink
            to="/events"
            class="self-start sm:self-auto bg-[#141e2e] hover:bg-[#1a293e] text-[#86cffb] hover:text-white font-mono text-xs font-semibold px-4 py-2.5 rounded-lg border border-[#23354d] hover:border-[#86cffb]/50 transition-all flex items-center gap-1.5 cursor-pointer shadow-sm group"
          >
            <span>Ver Todos los Eventos</span>
            <span class="material-symbols-outlined text-[16px] group-hover:translate-x-0.5 transition-transform">arrow_forward</span>
          </RouterLink>
        </div>

        <!-- Grid de Shows Recientes (A lo sumo 3 eventos, sin despliegue en la sección) -->
        <div class="grid grid-cols-1 md:grid-cols-3 gap-6">
          <article
            v-for="event in recentEvents"
            :key="event.id"
            @click="goToEvent(event.id)"
            class="bg-[#141e2e] rounded-2xl border border-[#23354d] shadow-md overflow-hidden flex flex-col group hover:-translate-y-1 hover:border-[#86cffb]/50 transition-all duration-300 relative cursor-pointer"
          >
            <div
              v-if="event.badgeText"
              class="absolute top-3 right-3 z-10 font-mono text-[10px] px-2.5 py-0.5 rounded-md uppercase tracking-wider font-semibold border border-[#23354d]"
              :class="event.badgeText === 'Confirmado' ? 'bg-[#162438]/90 text-[#4fdbc8] backdrop-blur-sm' : 'bg-[#00688f] text-white'"
            >
              {{ event.badgeText }}
            </div>

            <div class="w-full h-48 bg-[#1a293e] overflow-hidden">
              <img
                :src="event.imageUrl"
                :alt="event.title"
                class="w-full h-full object-cover group-hover:scale-105 transition-transform duration-500"
              />
            </div>

            <div class="p-5 flex flex-col flex-1 gap-2">
              <div class="flex items-center justify-between gap-2">
                <span class="font-mono text-xs text-[#86cffb] font-semibold">
                  {{ event.date }}
                </span>
                <span class="inline-flex items-center gap-1 px-2.5 py-1 rounded-md bg-[#00688f]/90 text-[#86cffb] border border-[#86cffb]/30 font-mono text-xs font-bold shadow-sm backdrop-blur-md">
                  <span>Entradas desde</span>
                  <span class="text-white">${{ getMinEventPrice(event) }}</span>
                </span>
              </div>
              <h3 class="font-display text-base text-[#f1f5f9] line-clamp-2 leading-tight group-hover:text-[#86cffb] transition-colors font-bold">
                {{ event.title }}
              </h3>
              <p class="font-body text-xs text-[#94a3b8] flex items-center gap-1 mt-auto pt-3 border-t border-[#23354d]/40">
                <span class="material-symbols-outlined text-[15px] text-[#86cffb]">location_on</span>
                <span class="truncate">{{ event.venue }}</span>
              </p>
            </div>
          </article>
        </div>
      </div>
    </section>

    <!-- 3. SECCIÓN 2: SHOWS PRÓXIMOS A OCURRIR (Fija, a lo sumo 3 eventos) -->
    <section class="w-full max-w-[80vw] mx-auto px-4 sm:px-6 lg:px-8">
      <div class="flex flex-col gap-6">
        <!-- Header con Botón Desplegable para Ver Todos los Eventos Disponibles -->
        <div class="flex flex-col sm:flex-row sm:items-end justify-between gap-4 border-b border-[#23354d] pb-4">
          <div class="flex flex-col gap-1">
            <span class="font-mono text-xs text-[#4fdbc8] uppercase tracking-wider font-semibold">
              En Agenda Cercana
            </span>
            <h2 class="font-display text-2xl sm:text-3xl font-bold text-[#f1f5f9]">
              Próximos a Ocurrir
            </h2>
            <p class="font-body text-sm text-[#94a3b8]">
              Funciones y fechas programadas para las próximas semanas.
            </p>
          </div>

          <!-- Botón hacia Cartelera Completa (Próximos a Ocurrir) -->
          <RouterLink
            to="/events"
            class="self-start sm:self-auto bg-[#141e2e] hover:bg-[#1a293e] text-[#4fdbc8] hover:text-white font-mono text-xs font-semibold px-4 py-2.5 rounded-lg border border-[#23354d] hover:border-[#4fdbc8]/50 transition-all flex items-center gap-1.5 cursor-pointer shadow-sm group"
          >
            <span>Ver Todos los Eventos</span>
            <span class="material-symbols-outlined text-[16px] group-hover:translate-x-0.5 transition-transform">arrow_forward</span>
          </RouterLink>
        </div>

        <!-- Grid de Shows Próximos (A lo sumo 3 eventos, sin despliegue en la sección) -->
        <div class="grid grid-cols-1 md:grid-cols-3 gap-6">
          <article
            v-for="event in upcomingEvents"
            :key="event.id"
            @click="goToEvent(event.id)"
            class="bg-[#141e2e] rounded-2xl border border-[#23354d] shadow-md overflow-hidden flex flex-col group hover:-translate-y-1 hover:border-[#4fdbc8]/50 transition-all duration-300 relative cursor-pointer"
          >
            <div
              v-if="event.badgeText"
              class="absolute top-3 right-3 z-10 font-mono text-[10px] px-2.5 py-0.5 rounded-md uppercase tracking-wider font-semibold border border-[#23354d]"
              :class="event.badgeText === 'Confirmado' ? 'bg-[#162438]/90 text-[#4fdbc8] backdrop-blur-sm' : 'bg-[#00688f] text-white'"
            >
              {{ event.badgeText }}
            </div>

            <div class="w-full h-48 bg-[#1a293e] overflow-hidden">
              <img
                :src="event.imageUrl"
                :alt="event.title"
                class="w-full h-full object-cover group-hover:scale-105 transition-transform duration-500"
              />
            </div>

            <div class="p-5 flex flex-col flex-1 gap-2">
              <div class="flex items-center justify-between gap-2">
                <span class="font-mono text-xs text-[#4fdbc8] font-semibold">
                  {{ event.date }}
                </span>
                <span class="inline-flex items-center gap-1 px-2.5 py-1 rounded-md bg-[#00688f]/90 text-[#86cffb] border border-[#86cffb]/30 font-mono text-xs font-bold shadow-sm backdrop-blur-md">
                  <span>Entradas desde</span>
                  <span class="text-white">${{ getMinEventPrice(event) }}</span>
                </span>
              </div>
              <h3 class="font-display text-base text-[#f1f5f9] line-clamp-2 leading-tight group-hover:text-[#4fdbc8] transition-colors font-bold">
                {{ event.title }}
              </h3>
              <p class="font-body text-xs text-[#94a3b8] flex items-center gap-1 mt-auto pt-3 border-t border-[#23354d]/40">
                <span class="material-symbols-outlined text-[15px] text-[#4fdbc8]">location_on</span>
                <span class="truncate">{{ event.venue }}</span>
              </p>
            </div>
          </article>
        </div>
      </div>
    </section>

    <!-- 4. SERVICIOS DEL RECINTO (Comida, Bebidas, Merchandising) -->
    <section class="w-full max-w-[80vw] mx-auto px-4 sm:px-6 lg:px-8">
      <div class="flex flex-col gap-6">
        <div class="flex flex-col gap-1 border-b border-[#23354d] pb-4">
          <span class="font-mono text-xs text-[#86cffb] uppercase tracking-wider font-semibold">
            Servicios en el Recinto
          </span>
          <h2 class="font-display text-2xl sm:text-3xl font-bold text-[#f1f5f9]">
            ¿Qué encontrarás en el lugar?
          </h2>
          <p class="font-body text-sm text-[#94a3b8]">
            Información sobre los servicios disponibles para tu comodidad durante el espectáculo.
          </p>
        </div>

        <!-- 3 Tarjetas Simples de Servicio -->
        <div class="grid grid-cols-1 md:grid-cols-3 gap-6">
          <article
            v-for="service in services"
            :key="service.id"
            class="bg-[#141e2e] border border-[#23354d] rounded-2xl overflow-hidden flex flex-col hover:border-[#86cffb]/40 transition-colors"
          >
            <div class="h-44 w-full overflow-hidden relative">
              <img
                :src="service.imageUrl"
                :alt="service.title"
                class="w-full h-full object-cover"
              />
              <div class="absolute inset-0 bg-gradient-to-t from-[#141e2e] via-transparent to-transparent"></div>
              <div class="absolute bottom-3 left-4 flex items-center gap-2">
                <div class="w-8 h-8 rounded-lg bg-[#0e1927]/90 border border-[#23354d] flex items-center justify-center text-[#86cffb]">
                  <span class="material-symbols-outlined text-[18px]">{{ service.icon }}</span>
                </div>
                <span class="font-mono text-xs font-bold text-[#f1f5f9] bg-[#0c141f]/80 px-2 py-0.5 rounded border border-[#23354d]">
                  {{ service.subtitle }}
                </span>
              </div>
            </div>

            <div class="p-5 flex flex-col gap-2 flex-1">
              <h3 class="font-display text-xl font-bold text-[#f1f5f9]">
                {{ service.title }}
              </h3>
              <p class="font-body text-sm text-[#94a3b8] leading-relaxed">
                {{ service.description }}
              </p>
            </div>
          </article>
        </div>
      </div>
    </section>

    <!-- 5. OFERTAS DE MEDIOS DE PAGO (Imágenes Banners de Bancos y Tarjetas) -->
    <section class="w-full max-w-[80vw] mx-auto px-4 sm:px-6 lg:px-8">
      <div class="flex flex-col gap-6">
        <div class="flex flex-col gap-1 border-b border-[#23354d] pb-4">
          <span class="font-mono text-xs text-[#4fdbc8] uppercase tracking-wider font-semibold">
            Promociones Bancarias
          </span>
          <h2 class="font-display text-2xl sm:text-3xl font-bold text-[#f1f5f9]">
            Medios de Pago y Financiación
          </h2>
          <p class="font-body text-sm text-[#94a3b8]">
            Aprovecha las promociones exclusivas con tarjetas y billeteras asociadas.
          </p>
        </div>

        <!-- Banners de Ofertas con Imágenes -->
        <div class="grid grid-cols-1 md:grid-cols-3 gap-6">
          <article
            v-for="promo in promotions"
            :key="promo.id"
            class="bg-[#141e2e] border border-[#23354d] rounded-2xl overflow-hidden flex flex-col group hover:border-[#86cffb]/50 transition-all shadow-md"
          >
            <div class="h-44 w-full overflow-hidden relative bg-[#101825]">
              <img
                :src="promo.imageUrl"
                :alt="promo.offer"
                class="w-full h-full object-cover group-hover:scale-105 transition-transform duration-500 opacity-75"
              />
              <div class="absolute inset-0 bg-gradient-to-t from-[#141e2e] via-[#141e2e]/40 to-transparent"></div>
              <div class="absolute top-3 right-3 font-mono text-xs font-bold px-2.5 py-1 rounded-md shadow-sm" :class="promo.badgeColor">
                {{ promo.tag }}
              </div>
              <div class="absolute bottom-3 left-4">
                <span class="font-mono text-xs text-[#cbd5e1] font-semibold bg-[#0c141f]/80 px-2 py-0.5 rounded border border-[#23354d]">
                  {{ promo.bank }}
                </span>
              </div>
            </div>

            <div class="p-5 flex flex-col gap-1.5 flex-1 justify-between">
              <div>
                <span class="font-mono text-[11px] text-[#94a3b8] uppercase">{{ promo.card }}</span>
                <h3 class="font-display text-lg font-bold text-[#f1f5f9] group-hover:text-[#86cffb] transition-colors">
                  {{ promo.offer }}
                </h3>
                <p class="font-body text-xs text-[#94a3b8] mt-1">
                  {{ promo.subtitle }}
                </p>
              </div>
              <div class="pt-3 border-t border-[#23354d]/50 flex items-center justify-between text-[11px] font-mono text-[#86cffb]">
                <span>Válido online</span>
                <span class="material-symbols-outlined text-sm">credit_card</span>
              </div>
            </div>
          </article>
        </div>
      </div>
    </section>

    <!-- 6. REDIRECCIÓN SIMPLE A FAQ (Sin Preguntas) -->
    <section class="w-full max-w-[80vw] mx-auto px-4 sm:px-6 lg:px-8">
      <div class="bg-[#101825] border border-[#23354d] rounded-2xl p-6 sm:p-8 flex flex-col sm:flex-row items-center justify-between gap-6">
        <div class="flex items-center gap-4 text-left">
          <div class="w-12 h-12 rounded-xl bg-[#141e2e] border border-[#23354d] flex items-center justify-center text-[#86cffb] shrink-0">
            <span class="material-symbols-outlined text-2xl">help_outline</span>
          </div>
          <div class="flex flex-col gap-1">
            <h2 class="font-display text-xl sm:text-2xl font-bold text-[#f1f5f9]">
              ¿Tienes preguntas o dudas sobre el evento?
            </h2>
            <p class="font-body text-sm text-[#94a3b8]">
              Encuentra respuestas rápidas sobre entradas digitales, métodos de acceso, devoluciones y horarios.
            </p>
          </div>
        </div>

        <RouterLink
          to="/help"
          class="w-full sm:w-auto shrink-0 bg-[#00688f] hover:bg-[#007da8] text-white font-display text-sm font-semibold px-6 py-3.5 rounded-xl border border-[#86cffb]/30 shadow-md transition-all flex items-center justify-center gap-2 cursor-pointer whitespace-nowrap hover:scale-[1.02] active:scale-[0.98]"
        >
          <span>Ir a Preguntas Frecuentes</span>
          <span class="material-symbols-outlined text-[18px]">arrow_forward</span>
        </RouterLink>
      </div>
    </section>
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
</style>
