<script setup lang="ts">
import {
  AccordionRoot,
  AccordionItem,
  AccordionHeader,
  AccordionTrigger,
  AccordionContent
} from 'radix-vue'
import type { EventFunction } from '../../types'

export interface PaymentPromotion {
  id: string
  bank: string
  card: string
  installments: string
  badgeText?: string
}

export interface PaymentBrand {
  name: string
  logo: string
}

export interface ParkingInfo {
  available: boolean
  title: string
  description: string
  price?: number
  accessRoutes?: string[]
}

defineProps<{
  formattedMinPrice: string
  minPrice: number
  primaryCurrency: string
  functions: EventFunction[]
  openSidebarAccordions: string[]
  paymentPromotions: PaymentPromotion[]
  acceptedPaymentBrands: PaymentBrand[]
  parkingInfo: ParkingInfo
  parseFunctionDate: (fn: EventFunction) => { day: string; month: string }
  cleanTime: (t?: string) => string
  getFunctionStatusLabel: (status: EventFunction['status']) => string
  getPaymentLogo: (bank: string) => string
}>()

const emit = defineEmits<{
  (e: 'update:openSidebarAccordions', val: string[]): void
  (e: 'purchase', fn: EventFunction): void
}>()
</script>

<template>
  <div class="rounded-2xl overflow-hidden border border-[#18395b] bg-[#0f263e] shadow-2xl flex flex-col">
    <!-- Top Vibrant Blue Header: Entradas desde $ 75.000 + cargo por servicio -->
    <div
      class="bg-[#0084d1] px-6 py-4 sm:py-5 flex items-center justify-between text-white"
      data-testid="banner-min-price-badge"
    >
      <span class="font-display font-extrabold text-xl sm:text-2xl tracking-tight">Entradas desde</span>
      <div class="flex flex-col items-end">
        <div class="flex items-baseline gap-1.5">
          <span class="font-display font-black text-3xl sm:text-4xl tracking-tight">$ {{ formattedMinPrice }}</span>
          <span class="sr-only">${{ minPrice }} {{ primaryCurrency }}</span>
        </div>
        <span class="font-mono text-xs text-white/90 font-medium tracking-wide">+ cargo por servicio</span>
      </div>
    </div>

    <!-- Available Functions List -->
    <div class="divide-y divide-[#18395b] bg-[#0f263e]">
      <div
        v-for="fn in functions"
        :key="fn.id"
        class="px-5 py-5 sm:px-6 sm:py-5 flex items-center justify-between gap-4 transition-colors hover:bg-[#132f4c]"
        data-testid="function-row"
      >
        <div class="flex items-center gap-4 sm:gap-5 min-w-0">
          <!-- Date Box matching Movistar Arena design -->
          <div class="w-18 h-18 sm:w-20 sm:h-20 rounded-xl border-2 border-[#0092e6] bg-[#0f263e] flex flex-col items-center justify-center shrink-0 shadow-sm">
            <span class="text-2xl sm:text-3xl font-black text-white leading-none">
              {{ parseFunctionDate(fn).day }}
            </span>
            <span class="text-xs sm:text-sm font-semibold text-white mt-1 leading-none capitalize">
              {{ parseFunctionDate(fn).month }}
            </span>
            <span class="sr-only">{{ fn.formattedDate }}</span>
          </div>

          <!-- Horarios Movistar Arena Style -->
          <div class="flex flex-col">
            <span class="text-[#ff2b56] text-xs font-bold font-mono tracking-wider uppercase">
              HORARIOS
            </span>
            <div class="flex items-center gap-5 sm:gap-7 mt-1">
              <div>
                <span class="block text-white font-bold text-sm sm:text-base leading-tight">
                  {{ cleanTime(fn.doorsOpen) }}
                </span>
                <span class="block text-slate-400 font-sans text-xs mt-0.5">Puertas</span>
                <span class="sr-only">Apertura de puertas: {{ fn.doorsOpen }}</span>
              </div>
              <div>
                <span class="block text-white font-bold text-sm sm:text-base leading-tight">
                  {{ cleanTime(fn.showStart) }}
                </span>
                <span class="block text-slate-400 font-sans text-xs mt-0.5">Show</span>
                <span class="sr-only">Inicio del show: {{ fn.showStart }}</span>
              </div>
            </div>
            <span class="sr-only">{{ getFunctionStatusLabel(fn.status) }}</span>
          </div>
        </div>

        <!-- Botón Comprar: Exactamente el estilo Movistar Arena -->
        <div class="shrink-0">
          <button
            @click="emit('purchase', fn)"
            :disabled="fn.status === 'SOLD_OUT'"
            :class="[
              'px-7 sm:px-8 py-3 rounded-full font-display font-black text-sm sm:text-base transition-all flex items-center justify-center shadow-md',
              fn.status === 'SOLD_OUT'
                ? 'bg-slate-700/90 text-slate-400 border border-slate-600/60 cursor-not-allowed shadow-none'
                : 'bg-[#0099ff] hover:bg-[#0086e6] active:scale-95 text-black hover:shadow-lg cursor-pointer'
            ]"
          >
            <span>{{ fn.status === 'SOLD_OUT' ? 'Agotado' : 'Comprar' }}</span>
          </button>
        </div>
      </div>
    </div>

    <!-- Accordions: Medios de pago & Estacionamiento -->
    <AccordionRoot
      type="multiple"
      :model-value="openSidebarAccordions"
      @update:model-value="emit('update:openSidebarAccordions', $event as string[])"
      class="w-full divide-y divide-[#18395b] border-t border-[#18395b]"
    >
      <!-- Section C: Medios de pago -->
      <AccordionItem
        value="payment-methods"
        data-testid="payment-methods-accordion"
        class="overflow-hidden"
      >
        <AccordionHeader class="flex">
          <AccordionTrigger
            class="w-full flex items-center justify-between p-4 sm:p-5 font-display text-sm sm:text-base font-bold text-white hover:bg-[#132f4c] transition-colors group cursor-pointer outline-none"
          >
            <div class="flex items-center gap-3">
              <span class="material-symbols-outlined text-[#52d92e] text-2xl font-bold">add_card</span>
              <span class="text-white font-bold text-base sm:text-lg">Medios de pago</span>
              <span class="sr-only">Medios de Pago y Financiación - Hasta 6 cuotas sin interés</span>
            </div>
            <span class="material-symbols-outlined text-white text-xl transition-transform duration-200 group-data-[state=open]:rotate-180">
              expand_more
            </span>
          </AccordionTrigger>
        </AccordionHeader>

        <AccordionContent class="overflow-hidden data-[state=closed]:hidden p-4 sm:p-5 pt-1 border-t border-[#18395b]/60 bg-[#0a1c30]/90 text-sm">
          <div class="space-y-3 pt-2">
            <!-- Tarjetas de promociones bancarias con imágenes oficiales -->
            <div
              v-for="promo in paymentPromotions"
              :key="promo.id"
              class="p-3.5 rounded-xl bg-surface-card border border-border-subtle/70 flex items-center justify-between gap-3 shadow-sm hover:border-[#0099ff]/40 transition-colors"
            >
              <div class="flex items-center gap-3 min-w-0">
                <img
                  :src="getPaymentLogo(promo.bank)"
                  :alt="promo.bank"
                  class="w-10 h-7 object-contain rounded bg-white/95 p-0.5 shrink-0 shadow-xs"
                />
                <div class="flex flex-col min-w-0">
                  <span class="font-display font-bold text-sm text-high-emphasis truncate">{{ promo.bank }}</span>
                  <span class="font-mono text-[11px] text-muted-light truncate">{{ promo.card }}</span>
                </div>
              </div>
              <span class="px-2.5 py-1 rounded-full bg-emerald-950/80 border border-emerald-500/40 font-mono text-xs text-emerald-400 font-bold whitespace-nowrap shrink-0">
                {{ promo.badgeText || promo.installments }}
              </span>
            </div>

            <!-- Galería de logos de medios aceptados -->
            <div class="pt-3 border-t border-border-subtle/30 flex flex-col gap-2">
              <p class="font-semibold text-medium-emphasis text-xs">Tarjetas y Billeteras Aceptadas:</p>
              <div class="flex flex-wrap items-center gap-2 pt-1">
                <img
                  v-for="brand in acceptedPaymentBrands"
                  :key="brand.name"
                  :src="brand.logo"
                  :alt="brand.name"
                  class="h-6 w-auto rounded shadow-xs"
                />
              </div>
              <p class="sr-only">Santander, BBVA, Galicia, Mercado Pago, Visa, Mastercard, American Express, Cabal, MODO.</p>
            </div>
          </div>
        </AccordionContent>
      </AccordionItem>

      <!-- Section D: Estacionamiento -->
      <AccordionItem
        value="venue-parking"
        data-testid="venue-parking-accordion"
        class="overflow-hidden"
      >
        <AccordionHeader class="flex">
          <AccordionTrigger
            class="w-full flex items-center justify-between p-4 sm:p-5 font-display text-sm sm:text-base font-bold text-white hover:bg-[#132f4c] transition-colors group cursor-pointer outline-none"
          >
            <div class="flex items-center gap-3">
              <span class="material-symbols-outlined text-[#52d92e] text-2xl font-bold">directions_car</span>
              <span class="text-white font-bold text-base sm:text-lg">Estacionamiento</span>
              <span class="sr-only">Estacionamiento Oficial y Accesos - Estacionamiento disponible</span>
            </div>
            <span class="material-symbols-outlined text-white text-xl transition-transform duration-200 group-data-[state=open]:rotate-180">
              expand_more
            </span>
          </AccordionTrigger>
        </AccordionHeader>

        <AccordionContent class="overflow-hidden data-[state=closed]:hidden p-4 sm:p-5 pt-1 border-t border-[#18395b]/60 bg-[#0a1c30]/90 text-sm">
          <div class="space-y-3 pt-2">
            <div class="flex flex-col gap-1">
              <h4 class="font-display text-sm font-bold text-high-emphasis">{{ parkingInfo.title }}</h4>
              <p class="font-body text-xs text-medium-emphasis leading-relaxed">
                {{ parkingInfo.description }}
              </p>
              <p v-if="parkingInfo.available && parkingInfo.price" class="font-mono text-xs text-primary font-semibold mt-1">
                Tarifa oficial estimada: ${{ parkingInfo.price.toLocaleString('es-AR') }} ARS por vehículo
              </p>
            </div>

            <div v-if="parkingInfo.accessRoutes && parkingInfo.accessRoutes.length > 0" class="pt-2 border-t border-border-subtle/30">
              <p class="font-mono text-xs font-semibold text-primary uppercase tracking-wider mb-2">
                Rutas de Acceso Designadas:
              </p>
              <ul class="space-y-1.5 font-body text-xs text-muted-light">
                <li
                  v-for="(route, rIdx) in parkingInfo.accessRoutes"
                  :key="rIdx"
                  class="flex items-start gap-2"
                >
                  <span class="material-symbols-outlined text-[14px] text-primary mt-0.5 shrink-0">turn_right</span>
                  <span>{{ route }}</span>
                </li>
              </ul>
            </div>
          </div>
        </AccordionContent>
      </AccordionItem>
    </AccordionRoot>
  </div>
</template>
