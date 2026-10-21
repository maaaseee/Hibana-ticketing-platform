<script setup lang="ts">
import { ref } from 'vue'
import { useTicketsStore } from '../stores/tickets'
import DynamicQrCode from '../components/ui/DynamicQrCode.vue'
import type { Ticket } from '../types'

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
          <span class="inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full text-xs font-semibold bg-secondary-500/10 text-secondary-300 border border-secondary-500/20">
            <span class="w-1.5 h-1.5 rounded-full bg-secondary-400"></span>
            Credenciales Cifradas
          </span>
        </div>
        <h1 class="text-2xl sm:text-3xl font-extrabold font-display text-text-primary mt-1">
          Mis Pases & Entradas Digitales
        </h1>
        <p class="text-sm text-text-secondary">
          Presenta estos pases en los molinetes de acceso. El código QR dinámico rota cada 15 segundos para evitar clonaciones.
        </p>
      </div>

      <div class="flex items-center gap-2 text-xs font-mono bg-surface-card px-4 py-2.5 rounded-xl border border-border-default text-text-secondary">
        <svg class="w-4 h-4 text-primary-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z" />
        </svg>
        <span>Firma Ed25519 &bull; Sin Reventa Especulativa</span>
      </div>
    </div>

    <!-- Tickets List: Skewed Perforation Boarding-Pass Cards -->
    <div v-if="ticketsStore.tickets.length > 0" class="space-y-6">
      
      <div
        v-for="ticket in ticketsStore.tickets"
        :key="ticket.id"
        class="relative bg-surface-card border border-border-default rounded-3xl overflow-hidden shadow-xl hover:border-primary-500/40 transition-all flex flex-col md:flex-row"
      >
        <!-- Left Side: Event Details & Metadata (2/3 width) -->
        <div class="p-6 sm:p-8 flex-1 flex flex-col justify-between space-y-6 border-b md:border-b-0 md:border-r border-border-default/80 relative">
          
          <div>
            <div class="flex items-center justify-between gap-4 mb-2">
              <span class="font-mono text-xs uppercase tracking-widest text-primary-400 font-semibold">
                {{ ticket.sector }}
              </span>
              <span
                :class="[
                  'px-3 py-1 rounded-full text-xs font-mono font-bold uppercase',
                  ticket.status === 'ISSUED' ? 'bg-secondary-500/10 text-secondary-300 border border-secondary-500/30' : (ticket.status === 'REDEEMED' ? 'bg-slate-700 text-slate-300' : 'bg-red-500/10 text-red-400 border border-red-500/30')
                ]"
              >
                {{ ticket.status }}
              </span>
            </div>

            <h2 class="text-xl sm:text-2xl font-bold font-display text-text-primary">
              {{ ticket.eventTitle }}
            </h2>
            <p class="text-sm text-text-secondary mt-1 flex items-center gap-2">
              <svg class="w-4 h-4 text-text-muted" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17.657 16.657L13.414 20.9a1.998 1.998 0 01-2.827 0l-4.244-4.243a8 8 0 1111.314 0z" />
              </svg>
              {{ ticket.venue }}
            </p>
          </div>

          <!-- Seat & Schedule Details Grid -->
          <div class="grid grid-cols-2 sm:grid-cols-4 gap-4 p-4 rounded-2xl bg-surface-base border border-border-default font-mono text-xs">
            <div>
              <span class="text-text-muted block text-[10px] uppercase">Fecha y Hora</span>
              <span class="font-bold text-text-primary">{{ ticket.eventDate }}</span>
            </div>
            <div>
              <span class="text-text-muted block text-[10px] uppercase">Sector</span>
              <span class="font-bold text-primary-300">{{ ticket.sector }}</span>
            </div>
            <div>
              <span class="text-text-muted block text-[10px] uppercase">Fila</span>
              <span class="font-bold text-text-primary">{{ ticket.row }}</span>
            </div>
            <div>
              <span class="text-text-muted block text-[10px] uppercase">Butaca #</span>
              <span class="font-bold text-secondary-400">{{ ticket.seatNumber }}</span>
            </div>
          </div>

          <!-- Footer Actions & Metadata -->
          <div class="flex flex-wrap items-center justify-between gap-4 pt-2">
            <div class="font-mono text-xs text-text-muted">
              <span>Pass ID: </span>
              <span class="text-text-secondary select-all font-semibold">{{ ticket.id }}</span>
              <span class="mx-2">&bull;</span>
              <span>Orden: </span>
              <span class="text-text-secondary select-all">{{ ticket.orderId }}</span>
            </div>

            <div class="flex items-center gap-3">
              <button
                v-if="ticket.status === 'ISSUED'"
                @click="openTransfer(ticket)"
                class="px-4 py-2 rounded-xl text-xs font-semibold bg-surface-elevated hover:bg-surface-elevated/80 border border-border-default text-text-primary cursor-pointer transition-colors flex items-center gap-1.5"
              >
                <svg class="w-3.5 h-3.5 text-primary-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 7h12m0 0l-4-4m4 4l-4 4m0 6H4m0 0l4 4m-4-4l4-4" />
                </svg>
                <span>Transferir Pase</span>
              </button>

              <button
                class="px-4 py-2 rounded-xl text-xs font-semibold bg-primary-500/10 hover:bg-primary-500/20 text-primary-300 border border-primary-500/20 cursor-pointer transition-colors flex items-center gap-1.5"
                title="Apple Wallet / Google Wallet"
              >
                <svg class="w-3.5 h-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4" />
                </svg>
                <span>Billetera Móvil</span>
              </button>
            </div>
          </div>

        </div>

        <!-- Right Side: Rotating Dynamic QR Code (Perforated ticket stub) -->
        <div class="p-6 sm:p-8 md:w-72 bg-surface-base/90 flex flex-col items-center justify-center space-y-4 text-center relative">
          
          <div class="text-center">
            <div class="text-[11px] font-mono uppercase tracking-widest text-secondary-400 font-bold">
              Molinete de Acceso
            </div>
            <p class="text-[11px] text-text-muted mt-0.5">Acercar al lector óptico</p>
          </div>

          <!-- Dynamic QR Canvas -->
          <DynamicQrCode
            :ticket-id="ticket.id"
            :payload="ticket.qrPayload"
          />

          <div class="text-[10px] font-mono text-text-muted text-center leading-tight">
            Cifrado con firma Ed25519<br/>
            Capturas de pantalla quedan anuladas
          </div>

        </div>

      </div>

    </div>

    <!-- Empty State -->
    <div v-else class="text-center py-20 px-4 rounded-3xl bg-surface-card border border-border-default space-y-4">
      <div class="w-16 h-16 mx-auto rounded-full bg-surface-base flex items-center justify-center text-text-muted">
        <svg class="w-8 h-8" fill="none" viewBox="0 0 24 24" stroke="currentColor">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M15 5v2m0 4v2m0 4v2M5 5a2 2 0 00-2 2v3a2 2 0 110 4v3a2 2 0 002 2h14a2 2 0 002-2v-3a2 2 0 110-4V7a2 2 0 00-2-2H5z" />
        </svg>
      </div>
      <h2 class="text-xl font-bold font-display text-text-primary">No tienes pases emitidos</h2>
      <p class="text-sm text-text-secondary max-w-sm mx-auto">
        Aún no has adquirido entradas para próximos espectáculos.
      </p>
      <div class="pt-2">
        <RouterLink
          :to="{ name: 'home' }"
          class="inline-flex items-center gap-2 px-6 py-3 rounded-xl font-bold bg-primary-400 text-surface-base hover:bg-primary-300 transition-colors"
        >
          Explorar Cartelera &rarr;
        </RouterLink>
      </div>
    </div>

    <!-- Transfer Pass Dialog Modal -->
    <div
      v-if="showTransferModal && selectedTicketForTransfer"
      class="fixed inset-0 z-50 bg-surface-base/80 backdrop-blur-md flex items-center justify-center p-4"
    >
      <div class="w-full max-w-md bg-surface-card border border-border-default rounded-3xl p-6 sm:p-8 shadow-2xl space-y-5 animate-in fade-in zoom-in-95">
        <div>
          <span class="text-xs font-mono font-bold text-primary-400 uppercase tracking-wider">Transferencia Segura</span>
          <h3 class="text-xl font-bold font-display text-text-primary mt-1">
            Enviar Entrada a otra Persona
          </h3>
          <p class="text-xs text-text-secondary mt-1">
            Reasigna la titularidad criptográfica. El destinatario recibirá un correo con el pase para activarlo.
          </p>
        </div>

        <div class="p-3 bg-surface-base rounded-xl border border-border-default text-xs font-mono space-y-1">
          <p class="text-text-primary font-bold">{{ selectedTicketForTransfer.eventTitle }}</p>
          <p class="text-text-muted">{{ selectedTicketForTransfer.sector }} &bull; Fila {{ selectedTicketForTransfer.row }}, Butaca {{ selectedTicketForTransfer.seatNumber }}</p>
        </div>

        <div v-if="!transferSuccess" class="space-y-4">
          <div>
            <label class="block text-xs font-medium text-text-secondary mb-1">Correo Electrónico del Destinatario</label>
            <input
              v-model="recipientEmail"
              type="email"
              placeholder="contacto@amaryllis.live"
              class="w-full px-3.5 py-2.5 rounded-xl bg-surface-base border border-border-default text-text-primary text-sm focus:outline-none focus:border-primary-400"
            />
          </div>

          <div class="flex items-center justify-end gap-3 pt-2">
            <button
              @click="showTransferModal = false"
              class="px-4 py-2.5 rounded-xl text-xs font-semibold text-text-secondary hover:text-text-primary cursor-pointer"
            >
              Cancelar
            </button>
            <button
              @click="handleTransferSubmit"
              :disabled="!recipientEmail"
              class="px-5 py-2.5 rounded-xl text-xs font-bold bg-primary-400 text-surface-base hover:bg-primary-300 disabled:opacity-40 cursor-pointer transition-colors shadow-md"
            >
              Transferir Ahora
            </button>
          </div>
        </div>

        <div v-else class="text-center py-4 space-y-2">
          <div class="w-10 h-10 mx-auto rounded-full bg-secondary-500/20 text-secondary-400 flex items-center justify-center">
            <svg class="w-6 h-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2.5" d="M5 13l4 4L19 7" />
            </svg>
          </div>
          <p class="text-sm font-bold text-text-primary">¡Pase Transferido!</p>
          <p class="text-xs text-text-muted font-mono">Notificación enviada a {{ recipientEmail }}</p>
        </div>

      </div>
    </div>

  </div>
</template>
