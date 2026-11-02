<script setup lang="ts">
withDefaults(
  defineProps<{
    grandTotal: number
    enabled?: boolean
  }>(),
  {
    enabled: false,
  }
)
</script>

<template>
  <div class="card-level-1 rounded-2xl p-5 sm:p-7 flex flex-col gap-5 shadow-xl shadow-black/20 border border-[#23354d] bg-[#141c27]">
    <!-- Header with Mercado Pago branding -->
    <div class="flex items-start justify-between">
      <div class="flex items-center gap-3">
        <div class="w-11 h-11 rounded-xl bg-[#009ee3]/20 border border-[#009ee3]/50 flex items-center justify-center text-[#009ee3]">
          <span class="material-symbols-outlined text-2xl">account_balance_wallet</span>
        </div>
        <div>
          <div class="flex items-center gap-2">
            <h3 class="font-display font-bold text-[#f1f5f9] text-base">Mercado Pago</h3>
            <span
              v-if="enabled"
              class="px-2 py-0.5 rounded text-[10px] font-mono font-bold bg-[#009ee3]/20 text-[#009ee3] border border-[#009ee3]/40"
            >
              OFICIAL
            </span>
            <span
              v-else
              class="px-2 py-0.5 rounded text-[10px] font-mono font-bold bg-amber-500/20 text-amber-400 border border-amber-500/40"
            >
              PRÓXIMAMENTE
            </span>
          </div>
          <p class="font-mono text-xs text-[#94a3b8]">Billetera Digital • Checkout Transparente</p>
        </div>
      </div>
      <span class="px-2.5 py-1 rounded-md bg-[#101825] border border-[#23354d] font-mono text-xs text-[#86cffb] font-semibold">
        {{ enabled ? 'Acreditación Inmediata' : 'En Integración' }}
      </span>
    </div>

    <!-- Disabled Notice for MVP -->
    <div v-if="!enabled" class="p-4 rounded-xl bg-amber-500/10 border border-amber-500/30 flex items-start gap-3">
      <span class="material-symbols-outlined text-amber-400 text-xl shrink-0 mt-0.5">info</span>
      <div class="text-xs text-amber-200/90 leading-relaxed space-y-1">
        <p class="font-bold text-amber-300">Medio de pago no disponible para el MVP</p>
        <p>
          La pasarela de Mercado Pago se encuentra en fase de homologación de webhooks y se habilitará en la siguiente versión. Por favor selecciona <strong>Tarjeta de Débito</strong> o <strong>Tarjeta de Crédito</strong> para abonar tus entradas.
        </p>
      </div>
    </div>

    <!-- Amount to pay -->
    <div class="p-4 rounded-xl bg-[#101825] border border-[#23354d] flex items-center justify-between">
      <span class="font-mono text-sm text-[#94a3b8]">Total a pagar:</span>
      <span class="font-mono text-lg font-bold text-[#4fdbc8]">
        $ {{ grandTotal.toLocaleString('es-AR', { minimumFractionDigits: 2, maximumFractionDigits: 2 }) }}
      </span>
    </div>

    <!-- Payment options via Mercado Pago -->
    <div class="grid grid-cols-1 sm:grid-cols-3 gap-2.5 text-xs opacity-70">
      <div class="p-3 rounded-xl bg-[#0c141f] border border-[#23354d] flex items-center gap-2 text-[#f1f5f9]">
        <span class="material-symbols-outlined text-[#009ee3] text-lg">paid</span>
        <span class="font-medium">Dinero en cuenta</span>
      </div>
      <div class="p-3 rounded-xl bg-[#0c141f] border border-[#23354d] flex items-center gap-2 text-[#f1f5f9]">
        <span class="material-symbols-outlined text-[#009ee3] text-lg">credit_card</span>
        <span class="font-medium">Débito / Cuotas</span>
      </div>
      <div class="p-3 rounded-xl bg-[#0c141f] border border-[#23354d] flex items-center gap-2 text-[#f1f5f9]">
        <span class="material-symbols-outlined text-[#009ee3] text-lg">qr_code_scanner</span>
        <span class="font-medium">Código QR</span>
      </div>
    </div>

    <!-- Policy Notice -->
    <div class="p-3.5 rounded-xl bg-[#1b2533] border border-[#23354d] text-xs text-[#94a3b8] space-y-1.5 leading-relaxed">
      <div class="flex items-center gap-1.5 text-[#86cffb] font-mono font-bold">
        <span class="material-symbols-outlined text-sm">verified_user</span>
        <span>Compra Protegida & Sin Saldos Retenidos</span>
      </div>
      <p>
        El cobro se procesa por el importe exacto de tus entradas sin dejar saldos pendientes. Ante cualquier reprogramación o cancelación, se emitirán <strong>tickets de descuento</strong> válidos para tu próxima compra en Hibana.tk.
      </p>
    </div>
  </div>
</template>
