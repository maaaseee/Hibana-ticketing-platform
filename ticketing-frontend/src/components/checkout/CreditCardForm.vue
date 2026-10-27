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

const props = defineProps<{
  cardNumber: string
  cardHolder: string
  cardExpiry: string
  cardCvc: string
  saveCard: boolean
}>()

const emit = defineEmits<{
  (e: 'update:cardNumber', val: string): void
  (e: 'update:cardHolder', val: string): void
  (e: 'update:cardExpiry', val: string): void
  (e: 'update:cardCvc', val: string): void
  (e: 'update:saveCard', val: boolean): void
  (e: 'formatCardNumber', event: Event): void
  (e: 'formatExpiry', event: Event): void
  (e: 'formatCvc', event: Event): void
}>()

// BIN detection for card issuers (Visa, Mastercard, Amex, Cabal)
const cardIssuer = computed(() => {
  const clean = props.cardNumber.replace(/\D/g, '')
  if (/^4/.test(clean)) return { name: 'Visa', label: 'VISA', colorClass: 'bg-blue-600 text-white font-sans' }
  if (/^(5[1-5]|2[2-7])/.test(clean)) return { name: 'Mastercard', label: 'Mastercard', colorClass: 'bg-amber-600 text-white font-sans' }
  if (/^3[47]/.test(clean)) return { name: 'Amex', label: 'AMEX', colorClass: 'bg-sky-600 text-white font-sans' }
  if (/^(6042|5896|60)/.test(clean)) return { name: 'Cabal', label: 'CABAL', colorClass: 'bg-emerald-600 text-white font-sans' }
  return null
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
  <div class="bg-surface-card border border-border-subtle rounded-2xl p-5 sm:p-7 flex flex-col gap-4 shadow-xl shadow-black/20">
    <!-- Card Number Input with Dynamic Issuer Detection -->
    <div class="flex flex-col gap-1.5">
      <label class="font-mono text-xs sm:text-sm text-medium-emphasis">Número de Tarjeta</label>
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
        @input="emit('update:cardHolder', ($event.target as HTMLInputElement).value)"
        placeholder="NOMBRE Y APELLIDO"
        class="w-full bg-surface-elevated border border-border-subtle rounded-lg py-3 px-4 font-mono text-sm text-high-emphasis placeholder:text-muted focus:border-primary-container focus:ring-1 focus:ring-primary/30 focus:outline-none transition-colors uppercase"
      />
    </div>

    <!-- Expiry & CVC Grid -->
    <div class="grid grid-cols-2 gap-4">
      <div class="flex flex-col gap-1.5">
        <label class="font-mono text-xs sm:text-sm text-medium-emphasis">Fecha de Expiración</label>
        <input
          type="text"
          :value="cardExpiry"
          @input="onExpiryInput"
          placeholder="MM/AA"
          maxlength="5"
          class="w-full bg-surface-elevated border border-border-subtle rounded-lg py-3 px-4 font-mono text-sm text-high-emphasis placeholder:text-muted focus:border-primary-container focus:ring-1 focus:ring-primary/30 focus:outline-none transition-colors"
        />
      </div>

      <div class="flex flex-col gap-1.5">
        <div class="flex justify-between items-center">
          <label class="font-mono text-xs sm:text-sm text-medium-emphasis">CVC</label>
          
          <!-- Radix UI Tooltip for CVC Help -->
          <TooltipProvider :delay-duration="150">
            <TooltipRoot>
              <TooltipTrigger as-child>
                <button
                  type="button"
                  class="text-muted-light hover:text-primary transition-colors p-0 border-0 bg-transparent cursor-help flex items-center"
                  aria-label="Ayuda sobre CVC"
                >
                  <span class="material-symbols-outlined text-[16px]">help</span>
                </button>
              </TooltipTrigger>
              <TooltipPortal>
                <TooltipContent
                  side="top"
                  :side-offset="6"
                  class="bg-surface-container-deep border border-border-subtle text-high-emphasis px-3 py-2 rounded-lg text-xs font-mono max-w-xs shadow-2xl z-50 animate-in fade-in-50 zoom-in-95"
                >
                  3 dígitos al dorso de la tarjeta (o 4 dígitos en el frente para American Express).
                  <TooltipArrow class="fill-border-subtle" />
                </TooltipContent>
              </TooltipPortal>
            </TooltipRoot>
          </TooltipProvider>
        </div>
        <input
          type="text"
          :value="cardCvc"
          @input="onCvcInput"
          :placeholder="cardIssuer?.name === 'Amex' ? '1234' : '123'"
          :maxlength="cardIssuer?.name === 'Amex' ? 4 : 3"
          class="w-full bg-surface-elevated border border-border-subtle rounded-lg py-3 px-4 font-mono text-sm text-high-emphasis placeholder:text-muted focus:border-primary-container focus:ring-1 focus:ring-primary/30 focus:outline-none transition-colors"
        />
      </div>
    </div>

    <!-- Radix UI Checkbox: Save Card -->
    <div class="flex items-center gap-2.5 mt-2">
      <CheckboxRoot
        id="save-card-checkbox"
        :checked="saveCard"
        @update:checked="emit('update:saveCard', $event as boolean)"
        class="w-5 h-5 rounded bg-surface-elevated border border-border-subtle flex items-center justify-center transition-colors cursor-pointer data-[state=checked]:bg-primary-container data-[state=checked]:border-primary"
      >
        <CheckboxIndicator class="text-white flex items-center justify-center">
          <span class="material-symbols-outlined text-[16px] font-bold">check</span>
        </CheckboxIndicator>
      </CheckboxRoot>
      <label for="save-card-checkbox" class="font-body text-xs sm:text-sm text-muted-light cursor-pointer select-none">
        Guardar esta tarjeta de forma segura para compras futuras
      </label>
    </div>
  </div>
</template>
