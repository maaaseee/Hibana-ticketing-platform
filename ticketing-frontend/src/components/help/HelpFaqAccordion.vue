<script setup lang="ts">
import DOMPurify from 'dompurify'

export interface FaqItem {
  id: string
  question: string
  answer: string
  tags: string[]
}

defineProps<{
  faqs: FaqItem[]
  searchQuery: string
  openItem: string | null
}>()

const emit = defineEmits<{
  (e: 'toggle', id: string): void
  (e: 'clearSearch'): void
}>()

function sanitizeHtml(dirty: string): string {
  return DOMPurify.sanitize(dirty, {
    ALLOWED_TAGS: ['b', 'i', 'em', 'strong', 'a', 'p', 'span', 'code', 'ul', 'ol', 'li', 'br'],
    ALLOWED_ATTR: ['href', 'target', 'rel', 'class'],
  })
}
</script>

<template>
  <section class="bg-surface-card/70 backdrop-blur-xs rounded-2xl p-5 sm:p-8 border border-border-subtle flex flex-col gap-6 shadow-xl">
    <!-- Accordion Header -->
    <div class="flex flex-col sm:flex-row sm:items-center justify-between border-b border-border-subtle/70 pb-4 gap-3">
      <div class="flex items-center gap-2.5">
        <div class="w-8 h-8 rounded-lg bg-secondary/10 border border-secondary/20 text-secondary flex items-center justify-center">
          <span class="material-symbols-outlined text-[20px]">quiz</span>
        </div>
        <h2 class="font-display text-xl sm:text-2xl text-white font-bold tracking-tight">
          Preguntas Frecuentes
        </h2>
        <span class="ml-1.5 px-2.5 py-0.5 rounded-full bg-surface-elevated text-primary font-mono text-xs border border-border-subtle">
          {{ faqs.length }}
        </span>
      </div>

      <div class="flex items-center gap-4 text-xs font-mono">
        <button
          v-if="searchQuery"
          @click="emit('clearSearch')"
          class="text-primary hover:text-accent-sky underline transition-colors cursor-pointer"
        >
          Ver todas las preguntas
        </button>
        <span class="text-muted-light flex items-center gap-1">
          <span class="w-2 h-2 rounded-full bg-emerald-400 animate-pulse"></span>
          Actualizado hoy
        </span>
      </div>
    </div>

    <!-- FAQ Accordion Items List -->
    <div class="flex flex-col gap-3">
      <div
        v-for="faq in faqs"
        :key="faq.id"
        :id="faq.id"
        :class="[
          'rounded-xl p-4 sm:p-5 flex flex-col gap-2 border transition-all duration-300 ease-in-out',
          openItem === faq.id
            ? 'bg-surface-elevated/80 border-primary/50 shadow-lg ring-1 ring-primary/20'
            : 'bg-surface-container-deep/80 border-border-subtle hover:border-border-structural hover:bg-surface-card'
        ]"
      >
        <!-- Accordion Toggle Button -->
        <button
          @click="emit('toggle', faq.id)"
          class="w-full flex items-center justify-between cursor-pointer text-left select-none focus:outline-none group gap-3 py-1"
          :aria-expanded="openItem === faq.id"
        >
          <span
            :class="[
              'font-display font-semibold transition-colors duration-200 text-sm sm:text-base leading-snug',
              openItem === faq.id ? 'text-primary' : 'text-high-emphasis group-hover:text-primary'
            ]"
          >
            {{ faq.question }}
          </span>
          <span
            :class="[
              'material-symbols-outlined text-[22px] transition-transform duration-300 ease-in-out shrink-0',
              openItem === faq.id ? 'text-primary rotate-180' : 'text-muted-light group-hover:text-white'
            ]"
          >
            expand_more
          </span>
        </button>

        <!-- Collapsible Content Animated with CSS Grid -->
        <div
          class="grid transition-all duration-300 ease-in-out"
          :class="openItem === faq.id ? 'grid-rows-[1fr] opacity-100' : 'grid-rows-[0fr] opacity-0'"
        >
          <div class="overflow-hidden">
            <div class="font-body text-medium-emphasis text-sm sm:text-base leading-relaxed pt-2 pb-1 border-t border-border-subtle/50 mt-2">
              <p v-html="sanitizeHtml(faq.answer)" class="text-medium-emphasis"></p>
            </div>
          </div>
        </div>
      </div>

      <!-- Empty Search State -->
      <div
        v-if="faqs.length === 0"
        class="py-12 px-4 text-center flex flex-col items-center gap-3 bg-surface-container-deep/50 rounded-xl border border-dashed border-border-subtle"
      >
        <div class="w-12 h-12 rounded-full bg-surface-elevated flex items-center justify-center text-muted-light">
          <span class="material-symbols-outlined text-[28px]">search_off</span>
        </div>
        <div class="flex flex-col gap-1 max-w-md">
          <h3 class="font-display text-base font-semibold text-white">
            No encontramos respuestas para "{{ searchQuery }}"
          </h3>
          <p class="font-body text-xs sm:text-sm text-muted-light">
            Intenta buscar con palabras más simples como "entradas", "devolución", "pago" o consulta a nuestro equipo mediante el chat de soporte.
          </p>
        </div>
        <button
          @click="emit('clearSearch')"
          class="mt-2 bg-surface-elevated hover:bg-surface-interactive text-primary px-4 py-2 rounded-lg font-label-sm font-mono text-xs border border-border-subtle transition-colors cursor-pointer"
        >
          Borrar búsqueda
        </button>
      </div>
    </div>
  </section>
</template>
