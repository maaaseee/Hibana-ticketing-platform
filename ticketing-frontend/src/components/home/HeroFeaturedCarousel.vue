<script setup lang="ts">
import { computed } from 'vue'
import type { EventItem } from '../../types'

export interface FeaturedSlide extends EventItem {
  badgeText?: string
}

const props = withDefaults(
  defineProps<{
    slides?: FeaturedSlide[]
    currentSlide?: number
    isPaused?: boolean
  }>(),
  {
    slides: () => [],
    currentSlide: 0,
    isPaused: false
  }
)

const emit = defineEmits<{
  (e: 'prev'): void
  (e: 'next'): void
  (e: 'setSlide', idx: number): void
  (e: 'goToEvent', id: string): void
  (e: 'pauseAutoplay', val: boolean): void
  (e: 'update:currentSlide', idx: number): void
  (e: 'update:isPaused', val: boolean): void
}>()

const activeSlide = computed(() => {
  return props.slides[props.currentSlide] || props.slides[0] || null
})
</script>

<template>
  <section
    v-if="activeSlide"
    @mouseenter="emit('update:isPaused', true); emit('pauseAutoplay', true)"
    @mouseleave="emit('update:isPaused', false); emit('pauseAutoplay', false)"
    class="relative w-full overflow-hidden h-[55vh] min-h-110 max-h-150 bg-surface group border-b border-border-subtle"
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

    <!-- Flecha Izquierda -->
    <button
      @click="emit('prev')"
      class="absolute left-3 md:left-6 top-1/2 -translate-y-1/2 z-30 w-11 h-11 rounded-full bg-surface-card/80 hover:bg-primary-container border border-border-subtle text-white flex items-center justify-center transition-all shadow-lg cursor-pointer backdrop-blur-sm hover:scale-105 active:scale-95"
      aria-label="Evento anterior"
    >
      <span class="material-symbols-outlined text-2xl">chevron_left</span>
    </button>

    <!-- Flecha Derecha -->
    <button
      @click="emit('next')"
      class="absolute right-3 md:right-6 top-1/2 -translate-y-1/2 z-30 w-11 h-11 rounded-full bg-surface-card/80 hover:bg-primary-container border border-border-subtle text-white flex items-center justify-center transition-all shadow-lg cursor-pointer backdrop-blur-sm hover:scale-105 active:scale-95"
      aria-label="Evento siguiente"
    >
      <span class="material-symbols-outlined text-2xl">chevron_right</span>
    </button>

    <!-- Slide Content Overlay -->
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
        <h1 class="font-display text-3xl sm:text-4xl md:text-5xl lg:text-[54px] md:leading-14 lg:leading-15.5 font-extrabold text-high-emphasis tracking-tight drop-shadow-md">
          {{ activeSlide.title }}
        </h1>

        <!-- Subtítulo -->
        <p class="font-body text-sm sm:text-base md:text-lg text-medium-emphasis opacity-90 max-w-2xl line-clamp-2">
          {{ activeSlide.subtitle }}
        </p>

        <!-- Fecha en Español y Recinto -->
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
            @click="emit('goToEvent', activeSlide.id)"
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
        v-for="(_, idx) in slides"
        :key="idx"
        @click="emit('setSlide', idx); emit('update:currentSlide', idx)"
        class="w-3 h-3 rounded-full transition-all cursor-pointer"
        :class="currentSlide === idx ? 'bg-primary scale-125 ring-2 ring-primary/40' : 'bg-border-subtle hover:bg-primary/60'"
        :aria-label="`Ir al evento ${idx + 1}`"
      ></button>
    </div>
  </section>
</template>

<style scoped>
.carousel-fade-enter-active,
.carousel-fade-leave-active {
  transition: opacity 0.8s ease-in-out;
}
.carousel-fade-enter-from,
.carousel-fade-leave-to {
  opacity: 0;
}
</style>
