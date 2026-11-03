<script setup lang="ts">
import type { GeneralSector } from './MacroVenueMap.vue'
import type { SectorZone } from './ZoneSelectorCanvas.vue'
export interface CartSeatItem {
  id: string
  rowLetter: string
  seatNum: number
  price: number
}

defineProps<{
  maxTicketsAllowed: number
  selectedZone: SectorZone
  currentZonePriceFormatted: string
  selectedSeatsCount: number
  selectedSeatsList: CartSeatItem[]
  selectedSector: GeneralSector
  subtotalPrice: number
  serviceFeePrice: number
  grandTotalPrice: number
  formatMoney: (val: number) => string
}>()

const emit = defineEmits<{
  (e: 'removeSeat', id: string): void
  (e: 'proceed'): void
}>()
</script>

<template>
  <aside class="w-full lg:w-96 bg-[#141c27] flex flex-col h-full border-l border-[#23354d] z-20 shrink-0">
    <div class="p-4 border-b border-[#23354d]">
      <h2 class="font-display text-xl font-bold text-[#f1f5f9]">Tu Selección</h2>
    </div>

    <!-- Tier Breakdown -->
    <div class="px-4 py-2.5 bg-[#101825] border-b border-[#23354d] flex items-center justify-between font-mono text-xs">
      <span class="text-[#94a3b8]">Categoría seleccionada</span>
      <span class="text-[#86cffb] font-medium">{{ selectedZone.name }} ({{ currentZonePriceFormatted }} c/u)</span>
    </div>

    <!-- Carrito Desplegado -->
    <div class="flex-1 p-4 overflow-y-auto space-y-2.5">
      <div
        v-if="selectedSeatsCount === 0"
        class="h-40 flex flex-col items-center justify-center text-center p-4 border border-dashed border-[#23354d] rounded-xl text-[#94a3b8]"
      >
        <span class="material-symbols-outlined text-3xl text-[#00688f] mb-1">event_seat</span>
        <p class="text-xs">No hay asientos seleccionados</p>
      </div>

      <!-- Selected Seat Cards -->
      <div
        v-for="seat in selectedSeatsList"
        :key="seat.id"
        class="bg-[#1b2533] border border-[#23354d] rounded-xl p-3 flex justify-between items-center"
      >
        <div class="flex items-center gap-3">
          <div class="w-10 h-10 rounded-lg bg-[#00688f] flex items-center justify-center text-white font-bold font-mono text-sm border border-[#23354d]">
            {{ seat.id }}
          </div>
          <div>
            <p class="font-body text-sm font-semibold text-[#f1f5f9]">Entrada Estándar</p>
            <p class="font-mono text-xs text-[#94a3b8]">Fila {{ seat.rowLetter.toUpperCase() }}, Butaca {{ seat.seatNum }}</p>
          </div>
        </div>

        <div class="flex flex-col items-end">
          <p class="font-mono text-sm font-bold text-[#f1f5f9]">{{ formatMoney(seat.price) }}</p>
          <button
            @click="emit('removeSeat', seat.id)"
            class="text-[#94a3b8] hover:text-[#ffb4ab] p-1 transition-colors mt-0.5 cursor-pointer"
            title="Eliminar asiento"
          >
            <span class="material-symbols-outlined text-[16px]">close</span>
          </button>
        </div>
      </div>

      <!-- Summary Price Tier Breakdown -->
      <div v-if="selectedSeatsCount > 0" class="mt-4 p-3 bg-[#101825] border border-[#23354d] rounded-xl space-y-1.5 font-mono text-xs">
        <div class="flex justify-between text-[#94a3b8]">
          <span>{{ selectedSector.name }} (x{{ selectedSeatsCount }})</span>
          <span class="text-[#f1f5f9]">{{ formatMoney(subtotalPrice) }}</span>
        </div>
        <div class="text-[11px] text-[#86cffb] italic pt-1 border-t border-[#23354d]/50">
          * El costo por servicio de la página se desglosará en el siguiente paso (Checkout).
        </div>
      </div>


    </div>

    <!-- Checkout Summary Sticky Bottom -->
    <div class="p-4 bg-[#141c27] border-t border-[#23354d]">
      <div class="flex justify-between items-center mb-3">
        <span class="text-[#94a3b8] font-mono text-sm">Subtotal ({{ selectedSeatsCount }} asientos)</span>
        <span class="text-white font-mono text-xl font-bold">{{ formatMoney(subtotalPrice) }}</span>
      </div>

      <button
        @click="emit('proceed')"
        :disabled="selectedSeatsCount === 0"
        class="w-full bg-[#00688f] text-white hover:bg-[#005a7d] disabled:opacity-40 disabled:cursor-not-allowed font-mono text-sm font-bold py-3.5 rounded-xl border border-[#00688f] transition-colors flex items-center justify-center gap-2 cursor-pointer shadow-lg"
      >
        <span>Confirmar Asientos y Proceder al Pago</span>
        <span class="material-symbols-outlined text-[18px]">arrow_forward</span>
      </button>
    </div>
  </aside>
</template>
