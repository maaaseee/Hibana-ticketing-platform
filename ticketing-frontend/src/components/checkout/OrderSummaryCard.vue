<script setup lang="ts">
import type { Seat } from '../../types'

defineProps<{
  eventTitle: string
  eventDateFormatted: string
  eventVenue: string
  eventThumbnail: string
  selectedSeats: Seat[]
  includeParkingPass: boolean
  subtotal: number
  serviceFee: number
  taxes: number
  grandTotal: number
}>()

const emit = defineEmits<{
  (e: 'update:includeParkingPass', val: boolean): void
}>()
</script>

<template>
  <div class="col-span-1 lg:col-span-5 flex flex-col gap-6">
    <h1 class="font-display font-bold text-2xl sm:text-3xl text-[#f1f5f9]">
      Resumen del Pedido
    </h1>

    <div class="card-level-1 rounded-2xl p-5 sm:p-6 flex flex-col gap-5 shadow-xl shadow-black/20">
      <!-- Event Details Header -->
      <div class="flex gap-4 pb-5 border-b border-[#23354d]">
        <img
          :src="eventThumbnail"
          :alt="eventTitle"
          class="w-20 h-20 sm:w-24 sm:h-24 object-cover rounded-xl border border-[#23354d] shrink-0 shadow-sm"
        />
        <div class="flex flex-col justify-center min-w-0">
          <h3 class="font-display font-bold text-[#f1f5f9] text-base sm:text-lg leading-tight mb-2 truncate">
            {{ eventTitle }}
          </h3>
          <div class="flex items-center gap-1.5 text-[#94a3b8] text-xs sm:text-sm">
            <span class="material-symbols-outlined text-[16px] text-[#86cffb]">calendar_today</span>
            <span>{{ eventDateFormatted }}</span>
          </div>
          <div class="flex items-center gap-1.5 text-[#94a3b8] text-xs sm:text-sm mt-1">
            <span class="material-symbols-outlined text-[16px] text-[#86cffb]">location_on</span>
            <span>{{ eventVenue }}</span>
          </div>
        </div>
      </div>

      <!-- Tickets List -->
      <div class="flex flex-col gap-4 pb-5 border-b border-[#23354d]">
        <!-- Dynamic selected seats from store if present -->
        <template v-if="selectedSeats.length > 0">
          <div
            v-for="seat in selectedSeats"
            :key="seat.id"
            class="flex flex-col gap-1"
          >
            <div class="flex justify-between items-center">
              <span class="text-[#f1f5f9] font-medium text-sm sm:text-base">
                1x {{ seat.sector }}
              </span>
              <span class="text-[#f1f5f9] font-medium font-mono text-sm sm:text-base">
                ${{ seat.price.toFixed(2) }}
              </span>
            </div>
            <div class="text-[#94a3b8] text-xs font-mono">
              Fila {{ seat.row }}, Asiento {{ seat.number }}
            </div>
          </div>
        </template>

        <!-- Fallback seats from Wireframe design if no live seats held -->
        <template v-else>
          <div class="flex flex-col gap-1">
            <div class="flex justify-between items-center">
              <span class="text-[#f1f5f9] font-medium text-sm sm:text-base">
                2x VIP Access
              </span>
              <span class="text-[#f1f5f9] font-medium font-mono text-sm sm:text-base">
                $300.00
              </span>
            </div>
            <div class="text-[#94a3b8] text-xs font-mono">
              Fila 12, Asientos A &amp; B
            </div>
          </div>
        </template>

        <!-- Optional Parking Pass Item -->
        <div class="flex flex-col gap-1 pt-2 border-t border-[#23354d]/60">
          <div class="flex justify-between items-center">
            <label class="flex items-center gap-2 cursor-pointer select-none">
              <input
                type="checkbox"
                :checked="includeParkingPass"
                @change="emit('update:includeParkingPass', ($event.target as HTMLInputElement).checked)"
                class="rounded text-[#00688f] focus:ring-0 focus:ring-offset-0 border-[#23354d] bg-[#1b2533] cursor-pointer"
              />
              <span class="text-[#f1f5f9] font-medium text-sm sm:text-base">
                1x Pase de Estacionamiento
              </span>
            </label>
            <span class="text-[#f1f5f9] font-medium font-mono text-sm sm:text-base" :class="{ 'line-through text-muted opacity-60': !includeParkingPass }">
              $45.00
            </span>
          </div>
          <div class="text-[#94a3b8] text-xs font-mono pl-6">
            Acceso exclusivo a Playón Norte Techado
          </div>
        </div>
      </div>

      <!-- Cost Breakdown Totals -->
      <div class="flex flex-col gap-2.5">
        <div class="flex justify-between items-center text-[#94a3b8] text-sm">
          <span>Subtotal</span>
          <span class="font-mono text-[#cbd5e1]">${{ subtotal.toFixed(2) }}</span>
        </div>
        <div class="flex justify-between items-center text-[#94a3b8] text-sm">
          <span>Cargo por Servicio (7%)</span>
          <span class="font-mono text-[#cbd5e1]">${{ serviceFee.toFixed(2) }}</span>
        </div>
        <div class="flex justify-between items-center text-[#94a3b8] text-sm">
          <span>Impuestos</span>
          <span class="font-mono text-[#cbd5e1]">${{ taxes.toFixed(2) }}</span>
        </div>
        <div class="flex justify-between items-center mt-3 pt-3 border-t border-[#23354d]">
          <span class="font-display font-bold text-[#f1f5f9] text-xl">Total</span>
          <span class="font-display font-bold text-[#86cffb] text-2xl font-mono">
            ${{ grandTotal.toFixed(2) }}
          </span>
        </div>
      </div>
    </div>

    <!-- 100% Secure Guarantee Card -->
    <div class="flex items-start gap-3 bg-[#101825] p-4 rounded-xl border border-[#23354d] shadow-sm">
      <span class="material-symbols-outlined text-[#86cffb] text-xl shrink-0 mt-0.5">lock</span>
      <div>
        <h4 class="font-mono text-sm text-[#f1f5f9] font-bold mb-1">Pago 100% Seguro</h4>
        <p class="text-xs text-[#94a3b8] leading-relaxed">
          Tu información de pago está cifrada con tecnología TLS de 256 bits y se procesa de forma segura. No almacenamos los datos completos de tu tarjeta.
        </p>
      </div>
    </div>
  </div>
</template>

<style scoped>
.card-level-1 {
  background-color: #141c27;
  border: 1px solid #23354d;
}
</style>
