<script setup lang="ts">
import { computed } from 'vue'
import type { EventItem } from '../../types'

const props = withDefaults(
  defineProps<{
    event: EventItem
    variant?: 'default' | 'light' | 'dark'
    accentColor?: 'primary' | 'secondary'
    showVenueIcon?: 'location' | 'stadium'
  }>(),
  {
    variant: 'default',
    accentColor: 'primary',
    showVenueIcon: 'location'
  }
)

const emit = defineEmits<{
  (e: 'click', eventId: string): void
}>()

const isLight = computed(() => props.variant === 'light')

// Card Shell Classes: Background, Borderless, Shadow, Hover (rounded-lg reduced radius)
const cardClasses = computed(() => {
  if (isLight.value) {
    return 'bg-white border-0 text-slate-900 shadow-md hover:shadow-2xl'
  }
  return 'bg-surface-elevated border-0 text-high-emphasis shadow-md hover:shadow-2xl'
})

// State Badge (Top Right Corner) - rounded-bl-md
const badgeClasses = computed(() => {
  if (isLight.value) {
    return 'bg-btn-buy text-white shadow-sm'
  }
  if (props.event.badgeText === 'Confirmado') {
    return 'bg-surface-module/95 text-secondary backdrop-blur-sm'
  }
  return 'bg-primary-container text-white'
})

// Date text color
const dateClasses = computed(() => {
  if (isLight.value) {
    return 'text-accent-celeste-deep font-bold'
  }
  return props.accentColor === 'secondary' ? 'text-secondary font-semibold' : 'text-primary font-semibold'
})

// Title color & Light theme hover color (vibrant celeste-deep instead of dark/black)
const titleClasses = computed(() => {
  if (isLight.value) {
    return 'text-slate-950 font-black group-hover:text-accent-celeste-deep'
  }
  return props.accentColor === 'secondary'
    ? 'text-high-emphasis font-bold group-hover:text-secondary'
    : 'text-high-emphasis font-bold group-hover:text-primary'
})

// Venue text color
const venueTextClasses = computed(() => {
  if (isLight.value) {
    return 'text-slate-600 font-medium'
  }
  return 'text-muted-light'
})

// Venue icon color
const venueIconClasses = computed(() => {
  if (isLight.value) {
    return 'text-accent-celeste-deep'
  }
  return props.accentColor === 'secondary' ? 'text-secondary' : 'text-primary'
})

// Footer divider
const footerDividerClasses = computed(() => {
  if (isLight.value) {
    return 'border-slate-200'
  }
  return 'border-border-subtle/50'
})

// Action buttons
const moreInfoClasses = computed(() => {
  if (isLight.value) {
    return 'text-rose-600 hover:text-rose-700 hover:bg-rose-50 font-bold'
  }
  return 'text-accent-rose hover:text-accent-rose-hover font-semibold'
})

const buyBtnClasses = computed(() => {
  if (isLight.value) {
    return 'bg-btn-buy hover:bg-btn-buy-hover active:scale-95 text-black font-bold shadow-md hover:shadow-lg'
  }
  return 'bg-btn-buy hover:bg-btn-buy-hover active:scale-95 text-white font-bold shadow-md hover:shadow-lg'
})

function navigate() {
  emit('click', props.event.id)
}
</script>

<template>
  <article
    data-testid="event-card"
    :class="[
      'rounded-2xl border-0 overflow-hidden flex flex-col group relative transition-[transform,box-shadow] duration-200 ease-out will-change-transform',
      cardClasses
    ]"
  >
    <div
      v-if="event.badgeText"
      :class="[
        'absolute top-0 right-0 z-10 font-mono text-[10px] px-3 py-1 uppercase tracking-wider font-bold rounded-bl-md',
        badgeClasses
      ]"
    >
      {{ event.badgeText }}
    </div>

    <!-- Imagen de Portada Clickable (navega al detalle) -->
    <div
      @click="navigate"
      class="w-full h-56 sm:h-64 bg-slate-900 overflow-hidden cursor-pointer relative"
      data-testid="event-card-image"
    >
      <img
        :src="event.imageUrl"
        :alt="event.title"
        class="w-full h-full object-cover group-hover:scale-105 transition-transform duration-500"
        loading="lazy"
      />
    </div>

    <!-- Contenido y Metadatos -->
    <div class="p-5 flex flex-col flex-1 justify-between gap-4">
      <div class="flex flex-col gap-2.5">
        <!-- Fecha -->
        <span :class="['font-mono text-xs', dateClasses]">
          {{ event.date }}
        </span>

        <!-- Título del Evento -->
        <h3
          :class="[
            'font-display text-lg line-clamp-2 leading-snug transition-colors',
            titleClasses
          ]"
        >
          {{ event.title }}
        </h3>

        <!-- Recinto -->
        <p :class="['flex items-center gap-1.5 font-body text-xs', venueTextClasses]">
          <span :class="['material-symbols-outlined text-[16px]', venueIconClasses]">
            {{ showVenueIcon === 'stadium' ? 'stadium' : 'location_on' }}
          </span>
          <span class="truncate">{{ event.venue }}</span>
        </p>
      </div>

      <!-- Botones de Acción: Comprar y Más info -->
      <div :class="['pt-3 border-t flex items-center justify-between gap-3 mt-auto', footerDividerClasses]">
        <button
          type="button"
          @click.stop="navigate"
          data-testid="event-card-more-info"
          :class="['font-display text-sm px-2.5 py-1.5 rounded-lg transition-colors cursor-pointer', moreInfoClasses]"
        >
          <span>Más info</span>
        </button>

        <button
          type="button"
          @click.stop="navigate"
          data-testid="event-card-buy"
          :class="['font-display text-xs px-4 py-2 rounded-full transition-all cursor-pointer flex items-center gap-1', buyBtnClasses]"
        >
          <span class="text-sm font-bold">Comprar</span>
        </button>
      </div>
    </div>
  </article>
</template>

<style scoped>
/* Aceleración por hardware y prevención de desenfoque de fuentes en hover */
article {
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  text-rendering: geometricPrecision;
  backface-visibility: hidden;
  transform: translate3d(0, 0, 0);
}

article:hover {
  transform: translate3d(0, -4px, 0);
}
</style>
