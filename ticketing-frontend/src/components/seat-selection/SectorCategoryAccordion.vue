<script setup lang="ts">
import {
  AccordionRoot,
  AccordionItem,
  AccordionHeader,
  AccordionTrigger,
  AccordionContent
} from 'radix-vue'
import type { GeneralSector, SectorPricingInfo } from './MacroVenueMap.vue'

export interface CategoryDefinition {
  id: 'campo' | 'platea_baja' | 'platea_alta'
  name: 'Campo' | 'Platea Baja' | 'Platea Alta'
  badgeText: string
  colorBar: string
  description: string
}

defineProps<{
  activeAccordion: string
  categoryDefinitions: CategoryDefinition[]
  selectedSector: GeneralSector
  generalSectors: GeneralSector[]
  maxTicketsAllowed: number
  campoTicketCount: number
  isCampoMode: boolean
  getCategoryPriceRangeFormatted: (catName: CategoryDefinition['name']) => string
  getBackendSectorInfo: (secId: string, secName: string, fallbackPrice?: number) => SectorPricingInfo
}>()

const emit = defineEmits<{
  (e: 'update:activeAccordion', val: string): void
  (e: 'selectSector', sec: GeneralSector): void
  (e: 'incrementCampoTickets'): void
  (e: 'decrementCampoTickets'): void
  (e: 'continue'): void
}>()
</script>

<template>
  <aside class="w-full lg:w-96 bg-[#141c27] border-l border-[#23354d] flex flex-col h-full z-20 shrink-0">
    <!-- Header con paleta Dark Blue y degradado corporativo -->
    <div class="p-6 border-b border-[#23354d]">
      <h2 class="font-display text-xl font-bold text-white tracking-tight">Seleccionar ubicación</h2>
      <div class="w-12 h-0.5 bg-gradient-to-r from-[#00688f] to-[#38bdf8] mt-1.5 mb-2 rounded-full"></div>
      <p class="font-body text-xs text-[#94a3b8]">Para continuar elegí tu ubicación desde el listado</p>
    </div>

    <!-- Radix Vue Accordion List de Categorías -->
    <div class="flex-1 p-5 overflow-y-auto">
      <AccordionRoot
        :model-value="activeAccordion"
        @update:model-value="emit('update:activeAccordion', $event as string)"
        type="single"
        collapsible
        class="space-y-3.5 font-body"
      >
        <AccordionItem
          v-for="cat in categoryDefinitions"
          :key="cat.id"
          :value="cat.id"
          class="bg-[#101825] border border-[#23354d] rounded-xl overflow-hidden transition-all duration-200"
          :class="selectedSector.category === cat.name ? 'border-[#00688f] ring-1 ring-[#00688f]/60' : 'hover:border-[#334b6b]'"
        >
          <AccordionHeader class="flex">
            <AccordionTrigger
              class="flex-1 p-4 flex items-center justify-between text-left cursor-pointer group"
            >
              <div class="flex items-center gap-3">
                <div class="w-1.5 h-8 rounded-full" :class="cat.colorBar"></div>
                <div>
                  <h3 class="font-display font-bold text-white text-base leading-tight">{{ cat.name }}</h3>
                  <p class="font-mono text-xs text-[#86cffb] font-medium mt-0.5">
                    {{ getCategoryPriceRangeFormatted(cat.name) }}
                  </p>
                </div>
              </div>
              <span class="material-symbols-outlined text-[#86cffb] transition-transform duration-300 group-data-[state=open]:rotate-180 text-xl">
                expand_more
              </span>
            </AccordionTrigger>
          </AccordionHeader>

          <AccordionContent class="px-4 pb-4 pt-1 border-t border-[#23354d] bg-[#0c141f]/70 text-xs space-y-3">
            <!-- Caso 1: Campo (General y VIP) con selector dinámico de cantidad según política del organizador -->
            <template v-if="cat.name === 'Campo'">
              <div class="space-y-2">
                <button
                  v-for="sec in generalSectors.filter(s => s.category === 'Campo')"
                  :key="sec.id"
                  @click="emit('selectSector', sec)"
                  class="w-full text-left p-2.5 rounded-lg border text-xs flex justify-between items-center transition-colors cursor-pointer"
                  :class="selectedSector.id === sec.id ? 'bg-[#00688f] text-white border-[#86cffb] font-bold shadow-sm' : 'bg-[#141c27] text-[#cbd5e1] border-[#23354d] hover:bg-[#1c2738]'"
                >
                  <div>
                    <span class="block">{{ sec.name }}</span>
                    <span class="text-[10px] opacity-80 font-mono">{{ sec.type }} • Cap: {{ getBackendSectorInfo(sec.id, sec.name, sec.fallbackPrice).capacity.toLocaleString() }}</span>
                  </div>
                  <span class="font-mono font-bold">{{ getBackendSectorInfo(sec.id, sec.name, sec.fallbackPrice).priceRangeFormatted }}</span>
                </button>
              </div>

              <!-- Selector de Cantidad Dinámico según Política del Evento -->
              <div class="pt-2 border-t border-[#23354d] flex items-center justify-between">
                <div>
                  <span class="block font-semibold text-white">Cantidad de entradas:</span>
                  <span class="text-[10px] text-[#94a3b8] font-mono">Máx. {{ maxTicketsAllowed }} por política del evento</span>
                </div>
                <div class="flex items-center gap-2">
                  <button
                    @click.stop="emit('decrementCampoTickets')"
                    class="w-7 h-7 rounded bg-[#1b2533] hover:bg-[#23354d] text-white font-bold flex items-center justify-center transition-colors cursor-pointer border border-[#23354d]"
                  >
                    -
                  </button>
                  <span class="font-mono font-bold text-white text-sm w-5 text-center">{{ campoTicketCount }}</span>
                  <button
                    @click.stop="emit('incrementCampoTickets')"
                    class="w-7 h-7 rounded bg-[#1b2533] hover:bg-[#23354d] text-white font-bold flex items-center justify-center transition-colors cursor-pointer border border-[#23354d]"
                  >
                    +
                  </button>
                </div>
              </div>
            </template>

            <!-- Caso 2: Platea Baja o Platea Alta (Listado de sectores cardinales con precio mínimo - máximo) -->
            <template v-else>
              <p class="text-[11px] text-[#94a3b8] mb-1.5">{{ cat.description }}</p>
              <div class="space-y-1.5">
                <button
                  v-for="sec in generalSectors.filter(s => s.category === cat.name)"
                  :key="sec.id"
                  @click="emit('selectSector', sec)"
                  @dblclick="emit('continue')"
                  class="w-full text-left p-2.5 rounded-lg border text-xs flex justify-between items-center transition-colors cursor-pointer"
                  :class="selectedSector.id === sec.id ? 'bg-[#00688f] text-white border-[#86cffb] font-bold shadow-sm' : 'bg-[#141c27] text-[#cbd5e1] border-[#23354d] hover:bg-[#1c2738]'"
                >
                  <div>
                    <span class="block">{{ sec.name }}</span>
                    <span class="text-[10px] opacity-80 font-mono">Cap: {{ getBackendSectorInfo(sec.id, sec.name, sec.fallbackPrice).capacity.toLocaleString() }}</span>
                  </div>
                  <span class="font-mono font-bold">{{ getBackendSectorInfo(sec.id, sec.name, sec.fallbackPrice).priceRangeFormatted }}</span>
                </button>
              </div>
            </template>
          </AccordionContent>
        </AccordionItem>
      </AccordionRoot>
    </div>

    <!-- Bottom Sticky Bar con Botón Continuar -->
    <div class="p-6 bg-[#141c27] border-t border-[#23354d]">
      <button
        @click="emit('continue')"
        class="w-full bg-[#00688f] hover:bg-[#005a7d] text-white font-mono text-sm font-bold py-3.5 rounded-xl border border-[#00688f] transition-colors flex items-center justify-center gap-2 cursor-pointer shadow-lg"
      >
        <span>{{ isCampoMode ? 'Continuar al Pago' : 'Continuar a Selección de Zona' }}</span>
        <span class="material-symbols-outlined text-[18px]">arrow_forward</span>
      </button>
    </div>
  </aside>
</template>
