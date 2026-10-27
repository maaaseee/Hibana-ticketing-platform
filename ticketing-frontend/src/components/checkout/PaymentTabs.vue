<script setup lang="ts">
import {
  TabsRoot,
  TabsList,
  TabsTrigger,
  TabsContent,
} from 'radix-vue'
import CreditCardForm from './CreditCardForm.vue'
import PocketWalletTab from './PocketWalletTab.vue'
import BankTransferTab from './BankTransferTab.vue'

defineProps<{
  selectedPaymentTab: string
  cardNumber: string
  cardHolder: string
  cardExpiry: string
  cardCvc: string
  saveCard: boolean
  pocketBalance: number
  grandTotal: number
  copiedNotice: string | null
  isProcessing: boolean
}>()

const emit = defineEmits<{
  (e: 'update:selectedPaymentTab', val: string): void
  (e: 'update:cardNumber', val: string): void
  (e: 'update:cardHolder', val: string): void
  (e: 'update:cardExpiry', val: string): void
  (e: 'update:cardCvc', val: string): void
  (e: 'update:saveCard', val: boolean): void
  (e: 'formatCardNumber', event: Event): void
  (e: 'formatExpiry', event: Event): void
  (e: 'formatCvc', event: Event): void
  (e: 'copy', text: string, label: string): void
  (e: 'completeOrder'): void
}>()
</script>

<template>
  <div class="col-span-1 lg:col-span-7 flex flex-col gap-6 lg:pl-6">
    <div class="flex flex-col gap-4">
      <h2 class="font-display font-bold text-2xl text-[#f1f5f9]">
        Método de Pago
      </h2>

      <!-- Payment Options Tabs using Radix UI Tabs -->
      <TabsRoot
        :model-value="selectedPaymentTab"
        @update:model-value="emit('update:selectedPaymentTab', $event as string)"
        class="w-full flex flex-col gap-4"
      >
        <TabsList class="flex gap-2.5 overflow-x-auto pb-1.5 -mx-4 px-4 md:mx-0 md:px-0">
          <!-- Tab 1: Credit Card -->
          <TabsTrigger
            value="credit_card"
            class="flex items-center gap-2 border rounded-xl px-4 py-2.5 font-mono text-xs sm:text-sm font-semibold transition-all whitespace-nowrap cursor-pointer data-[state=active]:bg-[#00688f] data-[state=active]:text-white data-[state=active]:border-[#00688f] data-[state=inactive]:bg-[#141c27] data-[state=inactive]:text-[#94a3b8] data-[state=inactive]:border-[#23354d] hover:border-[#86cffb]/40 shadow-xs"
          >
            <span class="material-symbols-outlined text-[20px]">credit_card</span>
            <span>Tarjeta de Crédito</span>
          </TabsTrigger>

          <!-- Tab 2: Digital Wallet (Pocket) -->
          <TabsTrigger
            value="pocket_wallet"
            class="flex items-center gap-2 border rounded-xl px-4 py-2.5 font-mono text-xs sm:text-sm font-semibold transition-all whitespace-nowrap cursor-pointer data-[state=active]:bg-[#00688f] data-[state=active]:text-white data-[state=active]:border-[#00688f] data-[state=inactive]:bg-[#141c27] data-[state=inactive]:text-[#94a3b8] data-[state=inactive]:border-[#23354d] hover:border-[#86cffb]/40 shadow-xs"
          >
            <span class="material-symbols-outlined text-[20px]">account_balance_wallet</span>
            <span>Billetera Digital</span>
          </TabsTrigger>

          <!-- Tab 3: Bank Transfer -->
          <TabsTrigger
            value="bank_transfer"
            class="flex items-center gap-2 border rounded-xl px-4 py-2.5 font-mono text-xs sm:text-sm font-semibold transition-all whitespace-nowrap cursor-pointer data-[state=active]:bg-[#00688f] data-[state=active]:text-white data-[state=active]:border-[#00688f] data-[state=inactive]:bg-[#141c27] data-[state=inactive]:text-[#94a3b8] data-[state=inactive]:border-[#23354d] hover:border-[#86cffb]/40 shadow-xs"
          >
            <span class="material-symbols-outlined text-[20px]">payments</span>
            <span>Transferencia Bancaria</span>
          </TabsTrigger>
        </TabsList>

        <!-- Credit Card Form -->
        <TabsContent value="credit_card" class="focus:outline-none">
          <CreditCardForm
            :card-number="cardNumber"
            :card-holder="cardHolder"
            :card-expiry="cardExpiry"
            :card-cvc="cardCvc"
            :save-card="saveCard"
            @update:card-number="emit('update:cardNumber', $event)"
            @update:card-holder="emit('update:cardHolder', $event)"
            @update:card-expiry="emit('update:cardExpiry', $event)"
            @update:card-cvc="emit('update:cardCvc', $event)"
            @update:save-card="emit('update:saveCard', $event)"
            @format-card-number="emit('formatCardNumber', $event)"
            @format-expiry="emit('formatExpiry', $event)"
            @format-cvc="emit('formatCvc', $event)"
          />
        </TabsContent>

        <!-- Digital Wallet Tab -->
        <TabsContent value="pocket_wallet" class="focus:outline-none">
          <PocketWalletTab
            :pocket-balance="pocketBalance"
            :grand-total="grandTotal"
          />
        </TabsContent>

        <!-- Bank Transfer Tab -->
        <TabsContent value="bank_transfer" class="focus:outline-none">
          <BankTransferTab
            :copied-notice="copiedNotice"
            @copy="(t, l) => emit('copy', t, l)"
          />
        </TabsContent>
      </TabsRoot>

      <!-- Main Action Pay Button -->
      <div class="mt-2 flex flex-col gap-3">
        <button
          type="button"
          id="btn-pay"
          @click="emit('completeOrder')"
          :disabled="isProcessing"
          class="w-full bg-[#00688f] hover:bg-[#005a7d] text-white font-mono text-sm sm:text-base font-bold py-4 rounded-xl transition-all active:scale-[0.99] flex justify-center items-center gap-2 cursor-pointer shadow-lg shadow-[#00688f]/20 disabled:opacity-50 disabled:cursor-not-allowed"
        >
          <span class="material-symbols-outlined text-[20px]" style="font-variation-settings: 'FILL' 1;">lock</span>
          <span>
            {{
              selectedPaymentTab === 'credit_card'
                ? `Pagar $${grandTotal.toFixed(2)}`
                : selectedPaymentTab === 'pocket_wallet'
                ? `Pagar con Billetera Pocket ($${grandTotal.toFixed(2)})`
                : `Generar Orden de Transferencia ($${grandTotal.toFixed(2)})`
            }}
          </span>
        </button>

        <p class="text-center text-xs text-[#94a3b8] leading-relaxed">
          Al presionar "Pagar", aceptas nuestros
          <a href="#" class="text-[#86cffb] hover:underline">Términos del Servicio</a>
          y
          <a href="#" class="text-[#86cffb] hover:underline">Política de Privacidad</a>.
        </p>
      </div>
    </div>
  </div>
</template>
