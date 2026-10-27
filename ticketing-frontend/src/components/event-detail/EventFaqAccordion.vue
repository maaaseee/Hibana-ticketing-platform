<script setup lang="ts">
import {
  AccordionRoot,
  AccordionItem,
  AccordionHeader,
  AccordionTrigger,
  AccordionContent
} from 'radix-vue'

export interface EventFaq {
  id: string
  icon: string
  question: string
  answer?: string
  items?: string[]
  note?: string
}

defineProps<{
  eventDescription: string
  eventFaqs: EventFaq[]
  openDescriptionAccordions: string[]
}>()

const emit = defineEmits<{
  (e: 'update:openDescriptionAccordions', val: string[]): void
}>()
</script>

<template>
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
        :model-value="openDescriptionAccordions"
        @update:model-value="emit('update:openDescriptionAccordions', $event as string[])"
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
</template>
