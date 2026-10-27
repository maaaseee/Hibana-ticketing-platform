<script setup lang="ts">
defineProps<{
  pocketBalance: number
  grandTotal: number
}>()
</script>

<template>
  <div class="card-level-1 rounded-2xl p-5 sm:p-7 flex flex-col gap-5 shadow-xl shadow-black/20">
    <div class="flex items-start justify-between">
      <div class="flex items-center gap-3">
        <div class="w-10 h-10 rounded-xl bg-[#00688f]/20 border border-[#00688f]/40 flex items-center justify-center text-[#86cffb]">
          <span class="material-symbols-outlined text-2xl">account_balance_wallet</span>
        </div>
        <div>
          <h3 class="font-display font-bold text-[#f1f5f9] text-base">Billetera Virtual Pocket</h3>
          <p class="font-mono text-xs text-[#94a3b8]">Partida Doble Inmutable (com.aieta.pocket)</p>
        </div>
      </div>
      <span class="px-2.5 py-1 rounded-md bg-[#101825] border border-[#23354d] font-mono text-xs text-[#86cffb] font-semibold">
        1-Click Pay
      </span>
    </div>

    <div class="p-4 rounded-xl bg-[#101825] border border-[#23354d] flex items-center justify-between">
      <span class="font-mono text-sm text-[#94a3b8]">Saldo Disponible:</span>
      <span
        class="font-mono text-base font-bold"
        :class="pocketBalance >= grandTotal ? 'text-[#4fdbc8]' : 'text-rose-400'"
      >
        ${{ pocketBalance.toFixed(2) }} USD / ARS
      </span>
    </div>

    <div class="p-3.5 rounded-xl bg-[#1b2533] border border-[#23354d] text-xs text-[#94a3b8] space-y-1 leading-relaxed">
      <span class="font-mono font-bold text-[#86cffb] block">Asiento Contable Inmutable (Pocket Ledger):</span>
      <p>
        Al confirmar, el servicio financiero generará un débito atómico en tu cuenta y crédito en cuenta escrow en una sola transacción ACID con Transactional Outbox hacia Azure Service Bus.
      </p>
    </div>
  </div>
</template>

<style scoped>
.card-level-1 {
  background-color: #141c27;
  border: 1px solid #23354d;
}
</style>
