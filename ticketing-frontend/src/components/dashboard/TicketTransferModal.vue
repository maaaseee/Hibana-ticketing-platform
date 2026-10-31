<script setup lang="ts">
import type { Ticket } from '../../types'

defineProps<{
  ticket: Ticket | null
  transferSuccess: boolean
}>()

const isOpen = defineModel<boolean>('open', { required: true })
const recipientEmail = defineModel<string>('recipientEmail', { required: true })

const emit = defineEmits<{
  (e: 'submit'): void
  (e: 'close'): void
}>()
</script>

<template>
  <div
    v-if="isOpen && ticket"
    class="fixed inset-0 z-50 bg-canvas-base/80 backdrop-blur-md flex items-center justify-center p-4"
  >
    <div class="w-full max-w-md bg-surface-card border border-border-subtle rounded-3xl p-6 sm:p-8 shadow-2xl space-y-5 animate-in fade-in zoom-in-95">
      <div>
        <span class="text-xs font-mono font-bold text-primary uppercase tracking-wider">Transferencia Segura</span>
        <h3 class="text-xl font-bold font-display text-high-emphasis mt-1">
          Enviar Entrada a otra Persona
        </h3>
        <p class="text-xs text-medium-emphasis mt-1">
          Reasigna la titularidad criptográfica. El destinatario recibirá un correo con el pase para activarlo.
        </p>
      </div>

      <div class="p-3 bg-canvas-base rounded-xl border border-border-subtle text-xs font-mono space-y-1">
        <p class="text-high-emphasis font-bold">{{ ticket.eventTitle }}</p>
        <p class="text-muted-light">{{ ticket.sector }} &bull; Fila {{ ticket.row }}, Butaca {{ ticket.seatNumber }}</p>
      </div>

      <div v-if="!transferSuccess" class="space-y-4">
        <div>
          <label class="block text-xs font-medium text-medium-emphasis mb-1">Correo Electrónico del Destinatario</label>
          <input
            v-model="recipientEmail"
            type="email"
            placeholder="contacto@hibana.tk"
            class="w-full px-3.5 py-2.5 rounded-xl bg-canvas-base border border-border-subtle text-high-emphasis text-sm focus:outline-none focus:border-primary"
          />
        </div>

        <div class="flex items-center justify-end gap-3 pt-2">
          <button
            @click="emit('close')"
            class="px-4 py-2.5 rounded-xl text-xs font-semibold text-medium-emphasis hover:text-high-emphasis cursor-pointer"
          >
            Cancelar
          </button>
          <button
            @click="emit('submit')"
            :disabled="!recipientEmail"
            class="px-5 py-2.5 rounded-xl text-xs font-bold bg-btn-buy text-white hover:bg-btn-buy-hover disabled:opacity-40 cursor-pointer transition-colors shadow-md"
          >
            Transferir Ahora
          </button>
        </div>
      </div>

      <div v-else class="text-center py-4 space-y-2">
        <div class="w-10 h-10 mx-auto rounded-full bg-secondary/20 text-secondary flex items-center justify-center">
          <svg class="w-6 h-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2.5" d="M5 13l4 4L19 7" />
          </svg>
        </div>
        <p class="text-sm font-bold text-high-emphasis">¡Pase Transferido!</p>
        <p class="text-xs text-muted-light font-mono">Notificación enviada a {{ recipientEmail }}</p>
      </div>
    </div>
  </div>
</template>
