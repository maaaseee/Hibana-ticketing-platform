<script setup lang="ts">
import {
  TabsRoot,
  TabsList,
  TabsTrigger,
  TabsContent,
} from 'radix-vue'
import DebitCardForm from './DebitCardForm.vue'
import CreditCardForm from './CreditCardForm.vue'
import MercadoPagoTab from './MercadoPagoTab.vue'

defineProps<{
  selectedPaymentTab: string
  cardNumber: string
  cardHolder: string
  cardExpiry: string
  cardCvc: string
  saveCard: boolean
  issuingBank: string
  installments: number
  grandTotal: number
  isProcessing: boolean
}>()

const emit = defineEmits<{
  (e: 'update:selectedPaymentTab', val: string): void
  (e: 'update:cardNumber', val: string): void
  (e: 'update:cardHolder', val: string): void
  (e: 'update:cardExpiry', val: string): void
  (e: 'update:cardCvc', val: string): void
  (e: 'update:saveCard', val: boolean): void
  (e: 'update:issuingBank', val: string): void
  (e: 'update:installments', val: number): void
  (e: 'formatCardNumber', event: Event): void
  (e: 'formatExpiry', event: Event): void
  (e: 'formatCvc', event: Event): void
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
          <!-- Tab 1: Débito -->
          <TabsTrigger
            value="debit_card"
            class="flex items-center gap-2 border rounded-xl px-4 py-2.5 font-mono text-xs sm:text-sm font-semibold transition-all whitespace-nowrap cursor-pointer data-[state=active]:bg-[#00688f] data-[state=active]:text-white data-[state=active]:border-[#00688f] data-[state=inactive]:bg-[#141c27] data-[state=inactive]:text-[#94a3b8] data-[state=inactive]:border-[#23354d] hover:border-[#86cffb]/40 shadow-xs"
          >
            <span class="material-symbols-outlined text-[20px]">payment</span>
            <span>Tarjeta de Débito</span>
          </TabsTrigger>

          <!-- Tab 2: Crédito -->
          <TabsTrigger
            value="credit_card"
            class="flex items-center gap-2 border rounded-xl px-4 py-2.5 font-mono text-xs sm:text-sm font-semibold transition-all whitespace-nowrap cursor-pointer data-[state=active]:bg-[#00688f] data-[state=active]:text-white data-[state=active]:border-[#00688f] data-[state=inactive]:bg-[#141c27] data-[state=inactive]:text-[#94a3b8] data-[state=inactive]:border-[#23354d] hover:border-[#86cffb]/40 shadow-xs"
          >
            <span class="material-symbols-outlined text-[20px]">credit_card</span>
            <span>Tarjeta de Crédito</span>
          </TabsTrigger>

          <!-- Tab 3: Mercado Pago -->
          <TabsTrigger
            value="mercado_pago"
            class="flex items-center gap-2 border rounded-xl px-4 py-2.5 font-mono text-xs sm:text-sm font-semibold transition-all whitespace-nowrap cursor-pointer data-[state=active]:bg-[#009ee3] data-[state=active]:text-white data-[state=active]:border-[#009ee3] data-[state=inactive]:bg-[#141c27] data-[state=inactive]:text-[#94a3b8] data-[state=inactive]:border-[#23354d] hover:border-[#009ee3]/50 shadow-xs"
          >
            <span class="material-symbols-outlined text-[20px]">account_balance_wallet</span>
            <span>Mercado Pago</span>
          </TabsTrigger>
        </TabsList>

        <!-- Debit Card Form Content -->
        <TabsContent value="debit_card" class="focus:outline-none">
          <DebitCardForm
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

        <!-- Credit Card Form Content -->
        <TabsContent value="credit_card" class="focus:outline-none">
          <CreditCardForm
            :card-number="cardNumber"
            :card-holder="cardHolder"
            :card-expiry="cardExpiry"
            :card-cvc="cardCvc"
            :save-card="saveCard"
            :issuing-bank="issuingBank"
            :installments="installments"
            :grand-total="grandTotal"
            @update:card-number="emit('update:cardNumber', $event)"
            @update:card-holder="emit('update:cardHolder', $event)"
            @update:card-expiry="emit('update:cardExpiry', $event)"
            @update:card-cvc="emit('update:cardCvc', $event)"
            @update:save-card="emit('update:saveCard', $event)"
            @update:issuing-bank="emit('update:issuingBank', $event)"
            @update:installments="emit('update:installments', $event)"
            @format-card-number="emit('formatCardNumber', $event)"
            @format-expiry="emit('formatExpiry', $event)"
            @format-cvc="emit('formatCvc', $event)"
          />
        </TabsContent>

        <!-- Mercado Pago Tab Content -->
        <TabsContent value="mercado_pago" class="focus:outline-none">
          <MercadoPagoTab
            :grand-total="grandTotal"
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
          :class="[
            'w-full text-white font-mono text-sm sm:text-base font-bold py-4 rounded-xl transition-all active:scale-[0.99] flex justify-center items-center gap-2 cursor-pointer shadow-lg disabled:opacity-50 disabled:cursor-not-allowed',
            selectedPaymentTab === 'mercado_pago'
              ? 'bg-[#009ee3] hover:bg-[#0087c2] shadow-[#009ee3]/25'
              : 'bg-[#00688f] hover:bg-[#005a7d] shadow-[#00688f]/20'
          ]"
        >
          <span class="material-symbols-outlined text-[20px]" style="font-variation-settings: 'FILL' 1;">lock</span>
          <span>
            {{
              selectedPaymentTab === 'debit_card'
                ? `Pagar con Débito ($${grandTotal.toFixed(2)})`
                : selectedPaymentTab === 'credit_card'
                ? `Pagar con Crédito en ${installments} cuota${installments > 1 ? 's' : ''}`
                : `Pagar con Mercado Pago ($${grandTotal.toFixed(2)})`
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
