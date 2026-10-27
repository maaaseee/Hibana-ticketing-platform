<script setup lang="ts">
import type { EventItem } from '../../types'

defineProps<{
  event: EventItem
  countdownFormatted: string
}>()
</script>

<template>
  <!-- Panoramic Cover Banner matching wireframe lines 141-145 -->
  <div class="w-full h-[46vh] min-h-90 max-h-125 relative bg-canvas-base">
    <div
      class="absolute inset-0 bg-cover bg-center"
      :style="{ backgroundImage: `url(${event.imageUrl})` }"
    ></div>
    <div class="absolute inset-0 bg-linear-to-t from-canvas-base via-canvas-base/50 to-transparent"></div>
  </div>

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
</template>
