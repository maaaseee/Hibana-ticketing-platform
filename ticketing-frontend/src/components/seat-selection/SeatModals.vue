<script setup lang="ts">
import {
  DialogRoot,
  DialogPortal,
  DialogOverlay,
  DialogContent,
  DialogTitle,
  DialogDescription
} from 'radix-vue'

defineProps<{
  showExitModal: boolean
  showTimeExpiredModal: boolean
}>()

const emit = defineEmits<{
  (e: 'update:showExitModal', val: boolean): void
  (e: 'update:showTimeExpiredModal', val: boolean): void
  (e: 'confirmExit'): void
  (e: 'expiredReturnHome'): void
  (e: 'restartTimer'): void
}>()
</script>

<template>
  <!-- Exit Confirmation Modal with Radix Vue Dialog -->
  <DialogRoot
    :open="showExitModal"
    @update:open="emit('update:showExitModal', $event)"
  >
    <DialogPortal>
      <DialogOverlay class="fixed inset-0 z-50 bg-black/80 backdrop-blur-xs" />
      <DialogContent class="fixed top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 z-50 bg-[#141c27] border border-[#23354d] rounded-2xl max-w-md w-[92vw] p-6 space-y-4 shadow-2xl">
        <DialogTitle class="flex items-center gap-2 text-lg font-bold text-white font-display">
          <span class="material-symbols-outlined text-[#86cffb] text-2xl">warning</span>
          <span>¿Deseas salir de la reserva?</span>
        </DialogTitle>
        
        <DialogDescription class="text-xs text-[#94a3b8] font-mono leading-relaxed">
          Tienes asientos retenidos. Si sales al home ahora, se cancelará la sesión y se liberarán tus butacas para otros espectadores.
        </DialogDescription>

        <div class="flex items-center justify-end gap-3 pt-2 font-mono text-xs">
          <button
            @click="emit('update:showExitModal', false)"
            class="px-4 py-2 rounded-lg bg-[#1b2533] hover:bg-[#23354d] text-white border border-[#23354d] cursor-pointer transition-colors"
          >
            Continuar seleccionando
          </button>
          <button
            @click="emit('confirmExit')"
            class="px-4 py-2 rounded-lg bg-[#93000a] hover:bg-[#b00020] text-white border border-[#ffb4ab]/30 cursor-pointer transition-colors font-bold"
          >
            Salir y liberar asientos
          </button>
        </div>
      </DialogContent>
    </DialogPortal>
  </DialogRoot>

  <!-- Time Expired Dialog with Radix Vue -->
  <DialogRoot
    :open="showTimeExpiredModal"
    @update:open="emit('update:showTimeExpiredModal', $event)"
  >
    <DialogPortal>
      <DialogOverlay class="fixed inset-0 z-50 bg-black/80 backdrop-blur-xs" />
      <DialogContent class="fixed top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 z-50 bg-[#141c27] border border-rose-500/40 rounded-2xl max-w-md w-[92vw] p-6 space-y-4 shadow-2xl">
        <DialogTitle class="flex items-center gap-2 text-lg font-bold text-rose-400 font-display">
          <span class="material-symbols-outlined text-rose-400 text-2xl">timer_off</span>
          <span>Tiempo de Reserva Expirado</span>
        </DialogTitle>
        
        <DialogDescription class="text-xs text-[#94a3b8] font-mono leading-relaxed">
          Tus 10 minutos para concretar la compra han expirado. Por concurrencia y la política Redlock de alta demanda, los asientos reservados han sido liberados para otros usuarios.
        </DialogDescription>

        <div class="flex items-center justify-end gap-3 pt-2 font-mono text-xs">
          <button
            @click="emit('expiredReturnHome')"
            class="px-4 py-2 rounded-lg bg-[#1b2533] hover:bg-[#23354d] text-white border border-[#23354d] cursor-pointer transition-colors"
          >
            Volver al Evento
          </button>
          <button
            @click="emit('restartTimer')"
            class="px-4 py-2 rounded-lg bg-[#00688f] hover:bg-[#005a7d] text-white border border-[#00688f] cursor-pointer transition-colors font-bold"
          >
            Reiniciar Reserva (10 min)
          </button>
        </div>
      </DialogContent>
    </DialogPortal>
  </DialogRoot>
</template>
