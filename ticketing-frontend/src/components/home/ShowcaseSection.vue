<script setup lang="ts">
import { RouterLink } from 'vue-router'
import EventCard from '../ui/EventCard.vue'
import type { EventItem } from '../../types'

withDefaults(
  defineProps<{
    tag: string
    title: string
    description: string
    events: EventItem[]
    bgSvg?: string
    bgVariant?: 'wave' | 'oooscillate'
    tagColor?: 'primary' | 'secondary'
    eventCardVariant?: 'light' | 'default'
    accentColor?: 'primary' | 'secondary'
    catalogLinkText?: string
  }>(),
  {
    bgSvg: '',
    bgVariant: 'wave',
    tagColor: 'primary',
    eventCardVariant: 'default',
    accentColor: 'primary',
    catalogLinkText: 'Ver Todos los Eventos'
  }
)

const emit = defineEmits<{
  (e: 'selectEvent', id: string): void
}>()
</script>

<template>
  <section class="relative w-full overflow-hidden py-10">
    <!-- Background SVG -->
    <div
      v-if="bgSvg && bgVariant === 'wave'"
      class="absolute inset-0 pointer-events-none z-0 overflow-hidden select-none flex justify-center items-start"
    >
      <img
        :src="bgSvg"
        alt=""
        aria-hidden="true"
        class="w-[125vw] min-w-[125%] max-w-none h-auto min-h-95 object-cover object-top opacity-35 select-none pointer-events-none shrink-0"
      />
    </div>
    <div
      v-else-if="bgSvg"
      class="absolute inset-0 pointer-events-none z-0 overflow-hidden select-none"
    >
      <img
        :src="bgSvg"
        alt=""
        aria-hidden="true"
        class="w-full h-full object-cover object-center opacity-25 select-none pointer-events-none"
      />
    </div>

    <div class="relative z-10 w-full max-w-[80vw] mx-auto px-4 sm:px-6 lg:px-8 flex flex-col gap-6">
      <!-- Header con Botón Desplegable para Ver Todos los Eventos Disponibles -->
      <div class="flex flex-col sm:flex-row sm:items-end justify-between gap-4 border-b border-border-subtle pb-4">
        <div class="flex flex-col gap-1">
          <span
            class="font-mono text-xs uppercase tracking-wider font-semibold"
            :class="tagColor === 'secondary' ? 'text-secondary' : 'text-primary'"
          >
            {{ tag }}
          </span>
          <h2 class="font-display text-2xl sm:text-3xl font-bold text-high-emphasis">
            {{ title }}
          </h2>
          <p class="font-body text-sm text-muted-light">
            {{ description }}
          </p>
        </div>

        <!-- Botón hacia Cartelera Completa -->
        <RouterLink
          to="/events"
          class="self-start sm:self-auto bg-surface-card hover:bg-surface-interactive font-mono text-xs font-semibold px-4 py-2.5 rounded-lg border border-border-subtle transition-all flex items-center gap-1.5 cursor-pointer shadow-sm group"
          :class="tagColor === 'secondary' ? 'text-secondary hover:text-white hover:border-secondary/50' : 'text-primary hover:text-white hover:border-primary/50'"
        >
          <span>{{ catalogLinkText }}</span>
          <span class="material-symbols-outlined text-[16px] group-hover:translate-x-0.5 transition-transform">arrow_forward</span>
        </RouterLink>
      </div>

      <!-- Grid de Shows usando EventCard -->
      <div class="grid grid-cols-1 md:grid-cols-3 gap-6">
        <EventCard
          v-for="event in events"
          :key="event.id"
          :event="event"
          :variant="eventCardVariant || 'default'"
          :accent-color="accentColor || 'primary'"
          show-venue-icon="location"
          @click="emit('selectEvent', $event)"
        />
      </div>
    </div>
  </section>
</template>
