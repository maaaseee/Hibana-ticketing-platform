<script setup lang="ts">
import {
  DialogRoot,
  DialogPortal,
  DialogOverlay,
  DialogContent,
  DialogTitle,
  DialogDescription,
} from 'radix-vue'

defineProps<{
  orderCompletedOpen: boolean
  showTimeExpiredModal: boolean
  orderId: string
  grandTotal: number
}>()

const emit = defineEmits<{
  (e: 'update:orderCompletedOpen', val: boolean): void
  (e: 'update:showTimeExpiredModal', val: boolean): void
  (e: 'navigateToTickets'): void
  (e: 'returnToSeats'): void
}>()
</script>

<template>
  <!-- Order Completed Modal Dialog (Radix UI) -->
  <DialogRoot
    :open="orderCompletedOpen"
    @update:open="emit('update:orderCompletedOpen', $event)"
  >
    <DialogPortal>
      <DialogOverlay class="fixed inset-0 z-50 bg-black/80 backdrop-blur-md animate-in fade-in duration-200" />
      <DialogContent
        class="fixed left-1/2 top-1/2 -translate-x-1/2 -translate-y-1/2 z-50 w-[92%] max-w-lg bg-[#141c27] border border-[#23354d] rounded-3xl p-6 sm:p-8 shadow-2xl text-center space-y-6 focus:outline-none animate-in zoom-in-95 fade-in duration-200"
      >
        <div class="w-20 h-20 mx-auto rounded-full bg-[#14b8a6]/20 border-2 border-[#14b8a6] text-[#4fdbc8] flex items-center justify-center shadow-lg shadow-[#14b8a6]/20">
          <span class="material-symbols-outlined text-4xl font-bold">check_circle</span>
        </div>

        <div class="space-y-2">
          <span class="text-xs font-mono font-bold text-[#4fdbc8] uppercase tracking-widest block">
            Saga Completada con Éxito
          </span>
          <DialogTitle class="text-2xl sm:text-3xl font-bold font-display text-[#f1f5f9]">
            ¡Entradas Emitidas Correctamente!
          </DialogTitle>
          <DialogDescription class="text-sm text-[#94a3b8] max-w-sm mx-auto leading-relaxed">
            El pago fue aprobado y tus pases digitales con QR rotativo ya se encuentran registrados en tu billetera.
          </DialogDescription>
        </div>

        <div class="p-4 rounded-xl bg-[#101825] border border-[#23354d] text-left font-mono text-xs space-y-2">
          <div class="flex justify-between text-[#94a3b8]">
            <span>Referencia de Orden:</span>
            <span class="text-[#f1f5f9] font-bold">{{ orderId }}</span>
          </div>
          <div class="flex justify-between text-[#94a3b8]">
            <span>Total Abonado:</span>
            <span class="text-[#86cffb] font-bold">${{ grandTotal.toFixed(2) }}</span>
          </div>
          <div class="flex justify-between text-[#94a3b8]">
            <span>Formato de Pase:</span>
            <span class="text-[#4fdbc8] font-bold">QR Dinámico Anti-Clonación (15s)</span>
          </div>
        </div>

        <div class="pt-2">
          <button
            type="button"
            @click="emit('navigateToTickets')"
            class="w-full py-4 px-6 rounded-xl font-mono text-sm font-bold bg-[#00688f] hover:bg-[#005a7d] text-white shadow-lg shadow-[#00688f]/30 cursor-pointer transition-all flex items-center justify-center gap-2"
          >
            <span>Ver Mis Entradas Digitales</span>
            <span class="material-symbols-outlined text-sm">arrow_forward</span>
          </button>
        </div>
      </DialogContent>
    </DialogPortal>
  </DialogRoot>

  <!-- Time Expired Modal Dialog (Radix UI) -->
  <DialogRoot
    :open="showTimeExpiredModal"
    @update:open="emit('update:showTimeExpiredModal', $event)"
  >
    <DialogPortal>
      <DialogOverlay class="fixed inset-0 z-50 bg-black/85 backdrop-blur-md animate-in fade-in duration-200" />
      <DialogContent
        class="fixed left-1/2 top-1/2 -translate-x-1/2 -translate-y-1/2 z-50 w-[92%] max-w-lg bg-[#141c27] border border-rose-500/40 rounded-3xl p-6 sm:p-8 shadow-2xl text-center space-y-6 focus:outline-none animate-in zoom-in-95 fade-in duration-200"
      >
        <div class="w-20 h-20 mx-auto rounded-full bg-rose-500/20 border-2 border-rose-400 text-rose-400 flex items-center justify-center shadow-lg shadow-rose-500/20">
          <span class="material-symbols-outlined text-4xl">timer_off</span>
        </div>

        <div class="space-y-2">
          <span class="text-xs font-mono font-bold text-rose-400 uppercase tracking-widest block">
            Tiempo de Reserva Expirado
          </span>
          <DialogTitle class="text-2xl sm:text-3xl font-bold font-display text-[#f1f5f9]">
            Tus Asientos Fueron Liberados
          </DialogTitle>
          <DialogDescription class="text-sm text-[#94a3b8] max-w-sm mx-auto leading-relaxed">
            El tiempo límite de 10 minutos para completar el pago ha expirado. Para permitir que otros compradores accedan equitativamente, las butacas fueron devueltas al inventario general.
          </DialogDescription>
        </div>

        <div class="pt-2">
          <button
            type="button"
            @click="emit('returnToSeats')"
            class="w-full py-3.5 px-6 rounded-xl font-mono text-sm font-bold bg-[#00688f] hover:bg-[#005a7d] text-white shadow-lg cursor-pointer transition-all flex items-center justify-center gap-2"
          >
            <span>Volver a Seleccionar Butacas</span>
            <span class="material-symbols-outlined text-sm">arrow_forward</span>
          </button>
        </div>
      </DialogContent>
    </DialogPortal>
  </DialogRoot>
</template>
