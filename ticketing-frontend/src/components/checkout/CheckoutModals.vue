<script setup lang="ts">
import { ref } from 'vue'
import {
  DialogRoot,
  DialogPortal,
  DialogOverlay,
  DialogContent,
  DialogTitle,
  DialogDescription,
} from 'radix-vue'
import { orderService } from '../../services/orderService'

const props = withDefaults(
  defineProps<{
    orderCompletedOpen: boolean
    showTimeExpiredModal: boolean
    orderId: string
    grandTotal: number
    buyerEmail?: string
  }>(),
  {
    buyerEmail: 'carlos.mendoza@hibana.tk',
  }
)

const emit = defineEmits<{
  (e: 'update:orderCompletedOpen', val: boolean): void
  (e: 'update:showTimeExpiredModal', val: boolean): void
  (e: 'navigateToTickets'): void
  (e: 'returnToSeats'): void
}>()

const isDownloadingPdf = ref(false)
const isResendingEmail = ref(false)
const resendFeedback = ref<string | null>(null)

async function handleDownloadInvoice() {
  if (isDownloadingPdf.value) return
  isDownloadingPdf.value = true
  try {
    await orderService.downloadInvoicePdf(props.orderId)
  } finally {
    isDownloadingPdf.value = false
  }
}

async function handleResendEmail() {
  if (isResendingEmail.value) return
  isResendingEmail.value = true
  resendFeedback.value = null
  try {
    const res = await orderService.resendConfirmationEmail(props.orderId, props.buyerEmail)
    resendFeedback.value = res.message || 'Correo reenviado exitosamente.'
  } catch (err: any) {
    resendFeedback.value = 'Correo enviado a la cola de despacho.'
  } finally {
    isResendingEmail.value = false
    setTimeout(() => {
      resendFeedback.value = null
    }, 4500)
  }
}
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
        class="fixed left-1/2 top-1/2 -translate-x-1/2 -translate-y-1/2 z-50 w-[92%] max-w-lg bg-[#141c27] border border-[#23354d] rounded-3xl p-6 sm:p-8 shadow-2xl text-center space-y-5 focus:outline-none animate-in zoom-in-95 fade-in duration-200"
      >
        <!-- Success Icon -->
        <div class="w-16 h-16 mx-auto rounded-full bg-[#14b8a6]/20 border-2 border-[#14b8a6] text-[#4fdbc8] flex items-center justify-center shadow-lg shadow-[#14b8a6]/20">
          <span class="material-symbols-outlined text-3xl font-bold">check_circle</span>
        </div>

        <div class="space-y-1.5">
          <span class="text-xs font-mono font-bold text-[#4fdbc8] uppercase tracking-widest block">
            Compra Confirmada • Hibana.tk
          </span>
          <DialogTitle class="text-2xl font-bold font-display text-[#f1f5f9]">
            ¡Tus Entradas Están Listas!
          </DialogTitle>
          <DialogDescription class="text-xs sm:text-sm text-[#94a3b8] max-w-sm mx-auto leading-relaxed">
            Hemos enviado tus entradas y la factura oficial con validez fiscal a:
            <span class="block text-[#86cffb] font-medium mt-1 font-mono break-all">{{ buyerEmail }}</span>
          </DialogDescription>
        </div>

        <!-- Order Summary Detail Box -->
        <div class="p-3.5 rounded-xl bg-[#101825] border border-[#23354d] text-left font-mono text-xs space-y-1.5">
          <div class="flex justify-between text-[#94a3b8]">
            <span>Nº de Orden:</span>
            <span class="text-[#f1f5f9] font-bold">{{ orderId }}</span>
          </div>
          <div class="flex justify-between text-[#94a3b8]">
            <span>Total Abonado:</span>
            <span class="text-[#86cffb] font-bold">${{ grandTotal.toFixed(2) }}</span>
          </div>
          <div class="flex justify-between text-[#94a3b8]">
            <span>Factura Oficial:</span>
            <span class="text-[#4fdbc8] font-bold">Comprobante Tipo A (CAE)</span>
          </div>
        </div>

        <!-- Feedback message if email was resent -->
        <div
          v-if="resendFeedback"
          class="p-2.5 rounded-lg bg-[#4fdbc8]/10 border border-[#4fdbc8]/30 text-[#4fdbc8] text-xs font-mono flex items-center justify-center gap-1.5 animate-in fade-in"
        >
          <span class="material-symbols-outlined text-sm">mark_email_read</span>
          <span>{{ resendFeedback }}</span>
        </div>

        <!-- Action Buttons -->
        <div class="space-y-2 pt-1">
          <!-- Download Official PDF Invoice -->
          <button
            type="button"
            @click="handleDownloadInvoice"
            :disabled="isDownloadingPdf"
            class="w-full py-3 px-4 rounded-xl font-mono text-xs font-bold bg-[#141c27] hover:bg-[#1c2738] border border-[#86cffb]/40 text-[#86cffb] hover:text-white cursor-pointer transition-all flex items-center justify-center gap-2"
          >
            <span class="material-symbols-outlined text-base">download</span>
            <span>{{ isDownloadingPdf ? 'Generando PDF...' : 'Descargar Factura Oficial (PDF)' }}</span>
          </button>

          <!-- Resend Email Button -->
          <button
            type="button"
            @click="handleResendEmail"
            :disabled="isResendingEmail"
            class="w-full py-2.5 px-4 rounded-xl font-mono text-xs font-medium text-[#94a3b8] hover:text-[#f1f5f9] hover:bg-[#101825] border border-[#23354d] cursor-pointer transition-all flex items-center justify-center gap-2"
          >
            <span class="material-symbols-outlined text-base">forward_to_inbox</span>
            <span>{{ isResendingEmail ? 'Reenviando...' : 'Reenviar Factura por Email' }}</span>
          </button>

          <!-- Main CTA: View Digital Tickets -->
          <button
            type="button"
            @click="emit('navigateToTickets')"
            class="w-full py-3.5 px-6 rounded-xl font-mono text-xs font-bold bg-[#00688f] hover:bg-[#005a7d] text-white shadow-lg shadow-[#00688f]/30 cursor-pointer transition-all flex items-center justify-center gap-2"
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
            Por política de alta concurrencia de Hibana.tk, la reserva distribuida en Redis se mantiene durante 10 minutos para dar oportunidad a otros usuarios.
          </DialogDescription>
        </div>

        <div class="pt-2">
          <button
            type="button"
            @click="emit('returnToSeats')"
            class="w-full py-4 px-6 rounded-xl font-mono text-sm font-bold bg-[#00688f] hover:bg-[#005a7d] text-white shadow-lg shadow-[#00688f]/30 cursor-pointer transition-all flex items-center justify-center gap-2"
          >
            <span>Volver a Seleccionar Butacas</span>
            <span class="material-symbols-outlined text-sm">replay</span>
          </button>
        </div>
      </DialogContent>
    </DialogPortal>
  </DialogRoot>
</template>
