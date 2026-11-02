<script setup lang="ts">
import { ref } from 'vue'
import DynamicQrCode from '../ui/DynamicQrCode.vue'
import type { Ticket } from '../../types'
import { orderService } from '../../services/orderService'

const props = defineProps<{
  ticket: Ticket
}>()

defineEmits<{
  (e: 'transfer', ticket: Ticket): void
}>()

const isDownloadingInvoice = ref(false)

async function handleDownloadInvoice() {
  if (!props.ticket.orderId) return
  isDownloadingInvoice.value = true
  try {
    await orderService.downloadInvoicePdf(props.ticket.orderId)
  } catch (err) {
    console.error('Error al descargar factura:', err)
  } finally {
    isDownloadingInvoice.value = false
  }
}
</script>

<template>
  <div class="relative bg-surface-card border border-border-subtle rounded-3xl overflow-hidden shadow-2xl hover:border-primary/50 transition-all flex flex-col md:flex-row group">
    <!-- Top & Bottom Perforation Notches (Physical Ticket Cutouts) -->
    <div class="hidden md:block absolute -top-4 right-72 w-8 h-8 rounded-full bg-canvas-base border border-border-subtle translate-x-1/2 z-30 shadow-inner"></div>
    <div class="hidden md:block absolute -bottom-4 right-72 w-8 h-8 rounded-full bg-canvas-base border border-border-subtle translate-x-1/2 z-30 shadow-inner"></div>

    <!-- Left Side: Event Details & Metadata (Main Pass Body) -->
    <div class="p-6 sm:p-8 flex-1 flex flex-col justify-between space-y-6 border-b md:border-b-0 md:border-r border-dashed border-border-subtle/80 relative bg-surface-card/90">
      <div>
        <div class="flex items-center justify-between gap-4 mb-2">
          <span class="font-mono text-xs uppercase tracking-widest text-primary font-bold flex items-center gap-1.5">
            <span class="w-2 h-2 rounded-full bg-secondary animate-pulse"></span>
            <span>{{ ticket.sector }}</span>
          </span>
          <span
            :class="[
              'px-3 py-1 rounded-full text-xs font-mono font-bold uppercase tracking-wider',
              ticket.status === 'ISSUED' ? 'bg-secondary/15 text-secondary border border-secondary/30' : (ticket.status === 'REDEEMED' ? 'bg-slate-800 text-slate-400 border border-slate-700' : 'bg-rose-500/15 text-rose-400 border border-rose-500/30')
            ]"
          >
            {{ ticket.status === 'ISSUED' ? 'Válido • Emitido' : ticket.status }}
          </span>
        </div>

        <h2 class="text-xl sm:text-2xl font-bold font-display text-high-emphasis">
          {{ ticket.eventTitle }}
        </h2>
        <p class="text-sm text-muted-light mt-1 flex items-center gap-1.5">
          <span class="material-symbols-outlined text-[17px] text-secondary">location_on</span>
          <span>{{ ticket.venue }}</span>
        </p>
      </div>

      <!-- Seat & Schedule Details Grid -->
      <div class="grid grid-cols-2 sm:grid-cols-4 gap-4 p-4 rounded-2xl bg-surface-container-deep border border-border-subtle font-mono text-xs shadow-inner">
        <div>
          <span class="text-muted-light block text-[10px] uppercase font-semibold">Fecha y Hora</span>
          <span class="font-bold text-high-emphasis">{{ ticket.eventDate }}</span>
        </div>
        <div>
          <span class="text-muted-light block text-[10px] uppercase font-semibold">Sector</span>
          <span class="font-bold text-primary">{{ ticket.sector }}</span>
        </div>
        <div>
          <span class="text-muted-light block text-[10px] uppercase font-semibold">Fila</span>
          <span class="font-bold text-high-emphasis">{{ ticket.row }}</span>
        </div>
        <div>
          <span class="text-muted-light block text-[10px] uppercase font-semibold">Butaca #</span>
          <span class="font-bold text-secondary">{{ ticket.seatNumber }}</span>
        </div>
      </div>

      <!-- Footer Actions & Metadata -->
      <div class="flex flex-wrap items-center justify-between gap-4 pt-2">
        <div class="font-mono text-xs text-muted-light">
          <span>Pass ID: </span>
          <span class="text-high-emphasis select-all font-semibold">{{ ticket.id }}</span>
          <span class="mx-2 text-border-subtle">&bull;</span>
          <span>Orden: </span>
          <span class="text-high-emphasis select-all font-semibold">{{ ticket.orderId }}</span>
        </div>

        <div class="flex items-center gap-3">
          <button
            @click="handleDownloadInvoice"
            :disabled="isDownloadingInvoice"
            class="px-4 py-2 rounded-xl text-xs font-semibold bg-surface-elevated hover:bg-surface-interactive border border-border-subtle text-high-emphasis cursor-pointer transition-colors flex items-center gap-1.5 disabled:opacity-50"
            title="Descargar Factura Oficial PDF"
          >
            <span class="material-symbols-outlined text-[16px] text-secondary">receipt_long</span>
            <span>{{ isDownloadingInvoice ? 'Descargando...' : 'Factura PDF' }}</span>
          </button>

          <button
            v-if="ticket.status === 'ISSUED'"
            @click="$emit('transfer', ticket)"
            class="px-4 py-2 rounded-xl text-xs font-semibold bg-surface-elevated hover:bg-surface-interactive border border-border-subtle text-high-emphasis cursor-pointer transition-colors flex items-center gap-1.5"
          >
            <span class="material-symbols-outlined text-[16px] text-primary">sync_alt</span>
            <span>Transferir Pase</span>
          </button>

          <button
            class="px-4 py-2 rounded-xl text-xs font-semibold bg-primary-container/20 hover:bg-primary-container/40 text-primary border border-primary/30 cursor-pointer transition-colors flex items-center gap-1.5"
            title="Apple Wallet / Google Wallet"
          >
            <span class="material-symbols-outlined text-[16px]">account_balance_wallet</span>
            <span>Billetera Móvil</span>
          </button>
        </div>
      </div>
    </div>

    <!-- Right Side: Rotating Dynamic QR Code (Perforated ticket stub with Holographic Sheen) -->
    <div class="p-6 sm:p-8 md:w-72 bg-surface-container-deep flex flex-col items-center justify-center space-y-4 text-center relative overflow-hidden select-none">
      <!-- Holographic Iridescent Shimmer Overlay -->
      <div
        class="absolute inset-0 pointer-events-none opacity-25 group-hover:opacity-40 transition-opacity duration-700 mix-blend-overlay"
        style="background: linear-gradient(135deg, rgba(56, 189, 248, 0.25) 0%, rgba(244, 63, 94, 0.2) 35%, rgba(79, 219, 200, 0.28) 70%, rgba(134, 207, 251, 0.3) 100%);"
      ></div>

      <!-- Holographic Watermark Badge -->
      <div class="relative z-10 flex flex-col items-center">
        <span class="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full bg-surface-card border border-border-subtle font-mono text-[9px] font-bold text-secondary uppercase tracking-widest shadow-sm">
          <span class="w-1.5 h-1.5 rounded-full bg-emerald-400"></span>
          <span>Ed25519 Verified</span>
        </span>
        <div class="text-[11px] font-mono uppercase tracking-widest text-primary font-bold mt-1.5">
          Molinete de Acceso
        </div>
        <p class="text-[11px] text-muted-light">Acercar al lector óptico</p>
      </div>

      <!-- Dynamic QR Canvas Wrapper with subtle frame -->
      <div class="relative z-10 p-2 rounded-2xl bg-white shadow-lg border border-white/20">
        <DynamicQrCode
          :ticket-id="ticket.id"
          :payload="ticket.qrPayload"
        />
      </div>

      <div class="relative z-10 text-[10px] font-mono text-muted-light text-center leading-tight">
        Cifrado con firma biométrica<br/>
        Capturas de pantalla quedan anuladas
      </div>
    </div>
  </div>
</template>
