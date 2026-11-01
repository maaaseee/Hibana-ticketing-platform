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

// BIN detection for debit cards (Visa Débito, Maestro, Cabal Débito)
const cardIssuer = computed(() => {
  const clean = props.cardNumber.replace(/\D/g, '')
  if (clean.startsWith('4')) return { name: 'Visa Débito', label: 'VISA DÉBITO', colorClass: 'bg-blue-600 text-white font-sans' }
  if (/^(5[08]|6)/.test(clean)) return { name: 'Maestro', label: 'MAESTRO', colorClass: 'bg-red-600 text-white font-sans' }
  if (/^(5[1-5]|2[2-7])/.test(clean)) return { name: 'Mastercard Débito', label: 'MASTERCARD DÉBITO', colorClass: 'bg-amber-600 text-white font-sans' }
  return null
})

function onCardNumberInput(event: Event) {
  const target = event.target as HTMLInputElement
  let raw = target.value.replace(/\D/g, '').slice(0, 16)
  const parts = raw.match(/[\s\S]{1,4}/g) || []
  const formatted = parts.join(' ')

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
  const raw = target.value.replace(/\D/g, '').slice(0, 3)
  target.value = raw
  emit('update:cardCvc', raw)
  emit('formatCvc', event)
}
</script>

<template>
  <div class="bg-surface-card border border-border-subtle rounded-2xl p-5 sm:p-7 flex flex-col gap-4 shadow-xl shadow-black/20">
    <!-- Notice badge -->
    <div class="flex items-center justify-between p-3 rounded-xl bg-surface-elevated border border-border-subtle text-xs">
      <div class="flex items-center gap-2 text-primary font-mono font-semibold">
        <span class="material-symbols-outlined text-sm">payment</span>
        <span>1 Pago Directo • Débito en Cuenta</span>
      </div>
      <span class="text-xs font-mono font-bold text-accent-cyan bg-accent-cyan/10 px-2 py-0.5 rounded border border-accent-cyan/30">
        Acreditación Inmediata
      </span>
    </div>

    <!-- Card Number Input -->
    <div class="flex flex-col gap-1.5">
      <label class="font-mono text-xs sm:text-sm text-medium-emphasis">Número de Tarjeta de Débito</label>
      <div class="relative">
        <input
          type="text"
          :value="cardNumber"
          @input="onCardNumberInput"
          placeholder="0000 0000 0000 0000"
          maxlength="19"
          class="w-full bg-surface-elevated border border-border-subtle rounded-lg py-3 px-4 font-mono text-sm text-high-emphasis placeholder:text-muted focus:border-primary-container focus:ring-1 focus:ring-primary/30 focus:outline-none transition-colors pr-24"
        />
        <div class="absolute right-3.5 top-1/2 -translate-y-1/2 flex items-center pointer-events-none">
          <span
            v-if="cardIssuer"
            :class="['px-2 py-0.5 rounded text-[10px] font-black tracking-wider uppercase shadow-sm transition-all', cardIssuer.colorClass]"
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
                  Código de 3 dígitos ubicado al dorso de tu tarjeta de débito.
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
          placeholder="123"
          maxlength="3"
          class="w-full bg-surface-elevated border border-border-subtle rounded-lg py-3 px-4 font-mono text-sm text-high-emphasis placeholder:text-muted focus:border-primary-container focus:ring-1 focus:ring-primary/30 focus:outline-none transition-colors text-center"
        />
      </div>
    </div>

    <!-- Save Card Checkbox -->
    <div class="flex items-center gap-2.5 pt-2">
      <CheckboxRoot
        id="save-card-debit"
        :checked="saveCard"
        @update:checked="emit('update:saveCard', $event as boolean)"
        class="w-5 h-5 rounded border border-border-subtle bg-surface-elevated data-[state=checked]:bg-primary-container data-[state=checked]:border-primary-container flex items-center justify-center transition-colors cursor-pointer"
      >
        <CheckboxIndicator>
          <span class="material-symbols-outlined text-[16px] text-white font-bold">check</span>
        </CheckboxIndicator>
      </CheckboxRoot>
      <label for="save-card-debit" class="font-mono text-xs text-muted-light cursor-pointer select-none">
        Guardar esta tarjeta de débito para futuras compras
      </label>
    </div>
  </div>
</template>
