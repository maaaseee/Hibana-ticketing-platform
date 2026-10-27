<script setup lang="ts">
import {
  TooltipRoot,
  TooltipTrigger,
  TooltipContent
} from 'radix-vue'
import type { EventItem } from '../../types'

defineProps<{
  event: EventItem
}>()
</script>

<template>
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
</template>
