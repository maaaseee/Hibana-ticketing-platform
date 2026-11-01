<script setup lang="ts">
import { computed } from 'vue'
import {
  CheckboxRoot,
  CheckboxIndicator,
  TooltipProvider,
  TooltipRoot,
  TooltipTrigger,
  TooltipPortal,
  TooltipContent,
  TooltipArrow,
} from 'radix-vue'
import {
  SUPPORTED_BANKS,
  calculateInstallmentsForBank,
} from '../../utils/bankPromotions'

const props = withDefaults(
  defineProps<{
    cardNumber: string
    cardHolder: string
    cardExpiry: string
    cardCvc: string
    saveCard: boolean
    issuingBank?: string
    installments?: number
    grandTotal?: number
  }>(),
  {
    issuingBank: 'GALICIA',
    installments: 1,
    grandTotal: 85000,
  }
)

const emit = defineEmits<{
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
}>()

// BIN detection for card issuers (Visa, Mastercard, Amex, Cabal)
const cardIssuer = computed(() => {
  const clean = props.cardNumber.replace(/\D/g, '')
  if (clean.startsWith('4')) return { name: 'Visa', label: 'VISA', colorClass: 'bg-blue-600 text-white font-sans' }
  if (/^(5[1-5]|2[2-7])/.test(clean)) return { name: 'Mastercard', label: 'Mastercard', colorClass: 'bg-amber-600 text-white font-sans' }
  if (/^3[47]/.test(clean)) return { name: 'Amex', label: 'AMEX', colorClass: 'bg-sky-600 text-white font-sans' }
  if (/^(6042|5896|60)/.test(clean)) return { name: 'Cabal', label: 'CABAL', colorClass: 'bg-emerald-600 text-white font-sans' }
  return null
})

// Dynamic installment calculation based on bank promotion
const availableInstallments = computed(() => {
  return calculateInstallmentsForBank(props.grandTotal, props.issuingBank)
})

function onCardNumberInput(event: Event) {
  const target = event.target as HTMLInputElement
  let raw = target.value.replace(/\D/g, '')
  const isAmex = /^3[47]/.test(raw)

  let formatted = ''
  if (isAmex) {
    raw = raw.slice(0, 15)
    if (raw.length > 10) {
      formatted = `${raw.slice(0, 4)} ${raw.slice(4, 10)} ${raw.slice(10)}`
    } else if (raw.length > 4) {
      formatted = `${raw.slice(0, 4)} ${raw.slice(4)}`
    } else {
      formatted = raw
    }
  } else {
    raw = raw.slice(0, 16)
    const parts = raw.match(/[\s\S]{1,4}/g) || []
    formatted = parts.join(' ')
  }

  target.value = formatted
  emit('update:cardNumber', formatted)
  emit('formatCardNumber', event)
}

function onExpiryInput(event: Event) {
  const target = event.target as HTMLInputElement
  let raw = target.value.replace(/\D/g, '').slice(0, 4)
  if (raw.length >= 2) {
    raw = `${raw.slice(0, 2)}/${raw.slice(2)}`
  }
  target.value = raw
  emit('update:cardExpiry', raw)
  emit('formatExpiry', event)
}

function onCvcInput(event: Event) {
  const target = event.target as HTMLInputElement
  const maxLen = cardIssuer.value?.name === 'Amex' ? 4 : 3
  const raw = target.value.replace(/\D/g, '').slice(0, maxLen)
  target.value = raw
  emit('update:cardCvc', raw)
  emit('formatCvc', event)
}
</script>

<template>
  <div class="bg-surface-card border border-border-subtle rounded-2xl p-5 sm:p-7 flex flex-col gap-5 shadow-xl shadow-black/20">
    <!-- Bank Promotion Selector -->
    <div class="flex flex-col gap-2">
      <div class="flex items-center justify-between">
        <label class="font-mono text-xs sm:text-sm text-medium-emphasis flex items-center gap-1.5">
          <span class="material-symbols-outlined text-sm text-primary">account_balance</span>
          <span>Banco Emisor (Promociones & Cuotas)</span>
        </label>
        <span class="text-[11px] font-mono text-accent-cyan font-semibold">
          Cuotas sin interés disponibles
        </span>
      </div>

      <div class="grid grid-cols-2 sm:grid-cols-3 gap-2">
        <button
          v-for="bank in SUPPORTED_BANKS"
          :key="bank.id"
          type="button"
          @click="emit('update:issuingBank', bank.id)"
          :class="[
            'p-2.5 rounded-xl border text-left flex flex-col gap-0.5 transition-all cursor-pointer',
            issuingBank === bank.id
              ? 'bg-primary-container/20 border-primary-container text-white ring-1 ring-primary/40'
              : 'bg-surface-elevated/70 border-border-subtle text-medium-emphasis hover:border-primary/30 hover:text-white'
          ]"
        >
          <div class="flex items-center justify-between w-full">
            <span class="font-display text-xs font-bold">{{ bank.shortName }}</span>
            <span v-if="issuingBank === bank.id" class="w-2 h-2 rounded-full bg-accent-cyan animate-pulse"></span>
          </div>
          <span class="font-mono text-[10px] text-accent-cyan truncate">
            {{ bank.popularPromo }}
          </span>
        </button>
      </div>
    </div>

    <!-- Card Number Input with Dynamic Issuer Detection -->
    <div class="flex flex-col gap-1.5">
      <label class="font-mono text-xs sm:text-sm text-medium-emphasis">Número de Tarjeta de Crédito</label>
      <div class="relative">
        <input
          type="text"
          :value="cardNumber"
          @input="onCardNumberInput"
          placeholder="0000 0000 0000 0000"
          :maxlength="cardIssuer?.name === 'Amex' ? 17 : 19"
          class="w-full bg-surface-elevated border border-border-subtle rounded-lg py-3 px-4 font-mono text-sm text-high-emphasis placeholder:text-muted focus:border-primary-container focus:ring-1 focus:ring-primary/30 focus:outline-none transition-colors pr-24"
        />
        <div class="absolute right-3.5 top-1/2 -translate-y-1/2 flex items-center pointer-events-none">
          <span
            v-if="cardIssuer"
            :class="['px-2 py-0.5 rounded text-[10px] font-black tracking-wider uppercase shadow-sm transition-all animate-fadeIn', cardIssuer.colorClass]"
            data-testid="card-issuer-badge"
          >
            {{ cardIssuer.label }}
          </span>
          <span v-else class="material-symbols-outlined text-muted-light">
            credit_card
          </span>
        </div>
      </div>
    </div>

    <!-- Card Holder Input -->
    <div class="flex flex-col gap-1.5">
      <label class="font-mono text-xs sm:text-sm text-medium-emphasis">Titular de la Tarjeta</label>
      <input
        type="text"
        :value="cardHolder"
        @input="emit('update:cardHolder', ($event.target as HTMLInputElement).value.toUpperCase())"
        placeholder="NOMBRE Y APELLIDO"
        class="w-full bg-surface-elevated border border-border-subtle rounded-lg py-3 px-4 font-mono text-sm text-high-emphasis placeholder:text-muted focus:border-primary-container focus:ring-1 focus:ring-primary/30 focus:outline-none transition-colors uppercase"
      />
    </div>

    <!-- Expiry and CVC Row -->
    <div class="grid grid-cols-2 gap-4">
      <div class="flex flex-col gap-1.5">
        <label class="font-mono text-xs sm:text-sm text-medium-emphasis">Fecha de Expiración</label>
        <input
          type="text"
          :value="cardExpiry"
          @input="onExpiryInput"
          placeholder="MM/AA"
          maxlength="5"
          class="w-full bg-surface-elevated border border-border-subtle rounded-lg py-3 px-4 font-mono text-sm text-high-emphasis placeholder:text-muted focus:border-primary-container focus:ring-1 focus:ring-primary/30 focus:outline-none transition-colors text-center"
        />
      </div>

      <div class="flex flex-col gap-1.5">
        <div class="flex items-center justify-between">
          <label class="font-mono text-xs sm:text-sm text-medium-emphasis">CVC / CVV</label>
          <TooltipProvider>
            <TooltipRoot>
              <TooltipTrigger as-child>
                <button type="button" class="text-muted-light hover:text-white transition-colors cursor-pointer flex items-center">
                  <span class="material-symbols-outlined text-[16px]">help</span>
                </button>
              </TooltipTrigger>
              <TooltipPortal>
                <TooltipContent class="bg-surface-elevated border border-border-subtle text-high-emphasis text-xs p-2.5 rounded-lg shadow-xl font-mono max-w-xs z-50">
                  Código de seguridad de 3 o 4 dígitos ubicado en el reverso (o frente en Amex).
                  <TooltipArrow class="fill-surface-elevated" />
                </TooltipContent>
              </TooltipPortal>
            </TooltipRoot>
          </TooltipProvider>
        </div>
        <input
          type="password"
          :value="cardCvc"
          @input="onCvcInput"
          :placeholder="cardIssuer?.name === 'Amex' ? '1234' : '123'"
          :maxlength="cardIssuer?.name === 'Amex' ? 4 : 3"
          class="w-full bg-surface-elevated border border-border-subtle rounded-lg py-3 px-4 font-mono text-sm text-high-emphasis placeholder:text-muted focus:border-primary-container focus:ring-1 focus:ring-primary/30 focus:outline-none transition-colors text-center"
        />
      </div>
    </div>

    <!-- Installments Selector -->
    <div class="flex flex-col gap-2 pt-1">
      <label class="font-mono text-xs sm:text-sm text-medium-emphasis flex items-center justify-between">
        <span>Plan de Cuotas:</span>
        <span class="text-xs text-primary font-bold">
          {{ issuingBank }}
        </span>
      </label>

      <div class="flex flex-col gap-2">
        <label
          v-for="plan in availableInstallments"
          :key="plan.installments"
          :class="[
            'p-3 rounded-xl border flex items-center justify-between cursor-pointer transition-all',
            installments === plan.installments
              ? 'bg-primary-container/20 border-primary-container text-white'
              : 'bg-surface-elevated/70 border-border-subtle text-medium-emphasis hover:border-border-subtle/80 hover:text-white'
          ]"
        >
          <div class="flex items-center gap-3">
            <input
              type="radio"
              name="installments"
              :value="plan.installments"
              :checked="installments === plan.installments"
              @change="emit('update:installments', plan.installments)"
              class="w-4 h-4 text-primary-container focus:ring-0 cursor-pointer"
            />
            <div class="flex flex-col">
              <span class="font-mono text-xs sm:text-sm font-bold text-white">
                {{ plan.installments }}x $ {{ plan.installmentAmount.toLocaleString('es-AR', { minimumFractionDigits: 2, maximumFractionDigits: 2 }) }}
              </span>
              <span class="font-mono text-[11px] text-muted-light">
                Total: $ {{ plan.totalAmount.toLocaleString('es-AR', { minimumFractionDigits: 2, maximumFractionDigits: 2 }) }}
              </span>
            </div>
          </div>

          <span
            v-if="plan.isInterestFree"
            class="px-2 py-0.5 rounded text-[10px] font-mono font-bold bg-accent-cyan/15 text-accent-cyan border border-accent-cyan/30"
          >
            SIN INTERÉS
          </span>
          <span
            v-else
            class="px-2 py-0.5 rounded text-[10px] font-mono font-semibold bg-surface-card text-muted-light border border-border-subtle"
          >
            CFT: {{ plan.interestRatePercent }}%
          </span>
        </label>
      </div>
    </div>

    <!-- Save Card Checkbox -->
    <div class="flex items-center gap-2.5 pt-1">
      <CheckboxRoot
        id="save-card-credit"
        :checked="saveCard"
        @update:checked="emit('update:saveCard', $event as boolean)"
        class="w-5 h-5 rounded border border-border-subtle bg-surface-elevated data-[state=checked]:bg-primary-container data-[state=checked]:border-primary-container flex items-center justify-center transition-colors cursor-pointer"
      >
        <CheckboxIndicator>
          <span class="material-symbols-outlined text-[16px] text-white font-bold">check</span>
        </CheckboxIndicator>
      </CheckboxRoot>
      <label for="save-card-credit" class="font-mono text-xs text-muted-light cursor-pointer select-none">
        Guardar esta tarjeta de crédito de forma segura para compras futuras
      </label>
    </div>
  </div>
</template>
