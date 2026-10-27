<script setup lang="ts">
import { ref } from 'vue'
import { useTicketsStore } from '../stores/tickets'
import type { Ticket } from '../types'
import TicketPassCard from '../components/dashboard/TicketPassCard.vue'
import TicketTransferModal from '../components/dashboard/TicketTransferModal.vue'
import TicketsEmptyState from '../components/dashboard/TicketsEmptyState.vue'

const ticketsStore = useTicketsStore()

// Transfer Modal State
const showTransferModal = ref(false)
const selectedTicketForTransfer = ref<Ticket | null>(null)
const recipientEmail = ref('')
const transferSuccess = ref(false)

function openTransfer(ticket: Ticket) {
  selectedTicketForTransfer.value = ticket
  recipientEmail.value = ''
  transferSuccess.value = false
  showTransferModal.value = true
}

function handleTransferSubmit() {
  if (!recipientEmail.value || !selectedTicketForTransfer.value) return
  ticketsStore.transferTicket(selectedTicketForTransfer.value.id, recipientEmail.value)
  transferSuccess.value = true
  setTimeout(() => {
    showTransferModal.value = false
  }, 1600)
}
</script>

<template>
  <div class="py-8 px-4 sm:px-6 lg:px-8 max-w-7xl mx-auto space-y-8">
    <!-- Header -->
    <div class="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
      <div>
        <div class="flex items-center gap-2">
          <span class="inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full text-xs font-semibold bg-secondary/15 text-secondary border border-secondary/30">
            <span class="w-1.5 h-1.5 rounded-full bg-secondary"></span>
            Credenciales Cifradas
          </span>
        </div>
        <h1 class="text-2xl sm:text-3xl font-extrabold font-display text-high-emphasis mt-1">
          Mis Pases & Entradas Digitales
        </h1>
        <p class="text-sm text-medium-emphasis">
          Presenta estos pases en los molinetes de acceso. El código QR dinámico rota cada 15 segundos para evitar clonaciones.
        </p>
      </div>

      <div class="flex items-center gap-2 text-xs font-mono bg-surface-card px-4 py-2.5 rounded-xl border border-border-subtle text-medium-emphasis">
        <svg class="w-4 h-4 text-primary" fill="none" viewBox="0 0 24 24" stroke="currentColor">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z" />
        </svg>
        <span>Firma Ed25519 &bull; Sin Reventa Especulativa</span>
      </div>
    </div>

    <!-- Tickets List: Skewed Perforation Boarding-Pass Cards -->
    <div v-if="ticketsStore.tickets.length > 0" class="space-y-6">
      <TicketPassCard
        v-for="ticket in ticketsStore.tickets"
        :key="ticket.id"
        :ticket="ticket"
        @transfer="openTransfer"
      />
    </div>

    <!-- Empty State -->
    <TicketsEmptyState v-else />

    <!-- Transfer Pass Dialog Modal -->
    <TicketTransferModal
      v-model:open="showTransferModal"
      v-model:recipient-email="recipientEmail"
      :ticket="selectedTicketForTransfer"
      :transfer-success="transferSuccess"
      @submit="handleTransferSubmit"
      @close="showTransferModal = false"
    />
  </div>
</template>
