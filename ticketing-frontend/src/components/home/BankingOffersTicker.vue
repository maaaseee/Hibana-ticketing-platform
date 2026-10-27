<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'

export interface BankPromotion {
  id: string
  bank: string
  card: string
  title: string
  subtitle: string
  tag: string
  imageUrl: string
  accentColor: string
}

const defaultPromotions: BankPromotion[] = [
  {
    id: 'promo-galicia',
    bank: 'Banco Galicia',
    card: 'Visa & Mastercard Galicia',
    title: 'Hasta 6 Cuotas Sin Interés',
    subtitle: 'Exclusivo en espectáculos seleccionados y festivales internacionales.',
    tag: '6 Cuotas Sin Interés',
    imageUrl: 'https://images.unsplash.com/photo-1559526324-4b87b5e36e44?auto=format&fit=crop&w=1000&q=80',
    accentColor: '#e11d48'
  },
  {
    id: 'promo-santander',
    bank: 'Santander',
    card: 'Visa Santander & American Express',
    title: '3 Cuotas Sin Interés + 15% Ahorro',
    subtitle: 'Válido para clientes Select en todos los conciertos del estadio.',
    tag: 'Select 15% OFF',
    imageUrl: 'https://images.unsplash.com/photo-1563986768609-322da13575f3?auto=format&fit=crop&w=1000&q=80',
    accentColor: '#dc2626'
  },
  {
    id: 'promo-bbva',
    bank: 'BBVA',
    card: 'Visa & Mastercard BBVA',
    title: 'Preventa Exclusiva & 6 Cuotas',
    subtitle: 'Acceso prioritario a tickets y financiación en cuotas fijas.',
    tag: 'Preventa Exclusiva',
    imageUrl: 'https://images.unsplash.com/photo-1556740738-b6a63e27c4df?auto=format&fit=crop&w=1000&q=80',
    accentColor: '#0284c7'
  },
  {
    id: 'promo-modo',
    bank: 'MODO',
    card: 'Billetera Virtual & Débito',
    title: '20% de Reintegro Inmediato',
    subtitle: 'Tope de ahorro pagando con QR desde tu app bancaria o MODO.',
    tag: '20% Reintegro QR',
    imageUrl: 'https://images.unsplash.com/photo-1556742049-0a67c5574f73?auto=format&fit=crop&w=1000&q=80',
    accentColor: '#14b8a6'
  },
  {
    id: 'promo-macro',
    bank: 'Banco Macro',
    card: 'Visa Macro & Macro BMA',
    title: 'Hasta 6 Cuotas Sin Interés',
    subtitle: 'Financiación sin recargo en entradas para grandes estadios.',
    tag: '6 Cuotas Macro',
    imageUrl: 'https://images.unsplash.com/photo-1556742111-a301076d9d18?auto=format&fit=crop&w=1000&q=80',
    accentColor: '#0055b3'
  },
  {
    id: 'promo-naranja',
    bank: 'Naranja X',
    card: 'Tarjeta Naranja X',
    title: 'Plan Z: 3 Cuotas Cero Interés',
    subtitle: 'Elegí cómo pagar tu resumen con el clásico beneficio de Plan Z.',
    tag: 'Plan Z Cero Interés',
    imageUrl: 'https://images.unsplash.com/photo-1607604276583-eef5d076aa5f?auto=format&fit=crop&w=1000&q=80',
    accentColor: '#f97316'
  },
  {
    id: 'promo-nacion',
    bank: 'Banco Nación',
    card: 'BNA+ & Visa Nación',
    title: '15% de Ahorro + 3 Cuotas',
    subtitle: 'Comprando tus entradas a través de la billetera virtual BNA+.',
    tag: '15% Ahorro BNA+',
    imageUrl: 'https://images.unsplash.com/photo-1589758438368-0ad531db3366?auto=format&fit=crop&w=1000&q=80',
    accentColor: '#38bdf8'
  },
  {
    id: 'promo-amex',
    bank: 'American Express',
    card: 'The Platinum & Gold Card',
    title: 'Beneficios VIP & Cuotas Fijas',
    subtitle: 'Acceso a salas Lounge en recintos seleccionados y cuotas fijas.',
    tag: 'VIP Lounge & Cuotas',
    imageUrl: 'https://images.unsplash.com/photo-1563013544-b03a8d11634b?auto=format&fit=crop&w=1000&q=80',
    accentColor: '#94a3b8'
  },
  {
    id: 'promo-ciudad',
    bank: 'Banco Ciudad',
    card: 'Visa & Mastercard Ciudad',
    title: 'Hasta 25% de Ahorro en Teatro',
    subtitle: 'Descuento especial en funciones teatrales y espectáculos clásicos.',
    tag: '25% Ahorro Teatro',
    imageUrl: 'https://images.unsplash.com/photo-1526304640581-d334cdbbf45e?auto=format&fit=crop&w=1000&q=80',
    accentColor: '#10b981'
  }
]

const props = withDefaults(
  defineProps<{
    promotions?: BankPromotion[]
  }>(),
  {
    promotions: () => []
  }
)

const activePromos = computed(() => {
  return props.promotions && props.promotions.length > 0 ? props.promotions : defaultPromotions
})

const promoCurrentIndex = ref(0)
let promoTimer: ReturnType<typeof setInterval> | null = null

const visiblePromotions = computed(() => {
  const len = activePromos.value.length
  if (len === 0) return []
  const idx = promoCurrentIndex.value
  return [
    activePromos.value[idx % len]!,
    activePromos.value[(idx + 1) % len]!,
    activePromos.value[(idx + 2) % len]!
  ]
})

function nextPromo() {
  promoCurrentIndex.value = (promoCurrentIndex.value + 1) % activePromos.value.length
}

onMounted(() => {
  promoTimer = setInterval(nextPromo, 5000)
})

onUnmounted(() => {
  if (promoTimer) clearInterval(promoTimer)
})
</script>

<template>
  <section class="w-full max-w-[80vw] mx-auto px-4 sm:px-6 lg:px-8">
    <div class="flex flex-col gap-6">
      <div class="flex flex-col gap-1 border-b border-border-subtle pb-4">
        <span class="font-mono text-xs text-secondary uppercase tracking-wider font-semibold">
          Financiación y Beneficios
        </span>
        <h2 class="font-display text-2xl sm:text-3xl font-bold text-high-emphasis">
          Promociones Bancarias
        </h2>
        <p class="font-body text-sm text-muted-light">
          Aprovecha cuotas sin interés y reintegros con tarjetas seleccionadas en el checkout oficial.
        </p>
      </div>

      <!-- Grid de 3 Contenedores: Misma altura uniforme, el del medio más ancho horizontalmente (1fr / 1.6fr / 1fr) -->
      <div class="grid grid-cols-1 md:grid-cols-[1fr_1.6fr_1fr] gap-6 items-stretch">
        <div
          v-for="(promo, slotIdx) in visiblePromotions"
          :key="slotIdx"
          class="relative h-64 sm:h-72 md:h-80 rounded-3xl overflow-hidden border border-border-subtle hover:border-primary/50 transition-colors shadow-lg bg-surface-card"
        >
          <!-- Transición suave de la imagen al cambiar automáticamente con el tiempo -->
          <transition name="promo-fade" mode="out-in">
            <div
              :key="promo.id"
              class="w-full h-full relative flex flex-col justify-between p-6 group cursor-pointer"
            >
              <!-- Imagen Fotográfica de la Promoción de Fondo -->
              <img
                :src="promo.imageUrl"
                :alt="promo.title"
                class="absolute inset-0 w-full h-full object-cover group-hover:scale-105 transition-transform duration-700 ease-out"
              />

              <!-- Overlay de Gradiente para Legibilidad Óptima -->
              <div
                class="absolute inset-0 pointer-events-none"
                style="background: linear-gradient(to top, rgba(8, 14, 23, 0.96) 0%, rgba(8, 14, 23, 0.6) 50%, rgba(8, 14, 23, 0.25) 100%);"
              ></div>

              <!-- Cabecera: Nombre de la Entidad Bancaria y Tag -->
              <div class="relative z-10 flex items-center justify-between gap-2">
                <span class="px-3 py-1 rounded-full font-mono text-xs font-bold shadow-md backdrop-blur-md border bg-slate-950/80 text-white border-white/10">
                  {{ promo.bank }}
                </span>

                <span class="font-mono text-xs font-bold text-secondary bg-surface-card/90 px-2.5 py-0.5 rounded-full border border-secondary/30 backdrop-blur-sm">
                  {{ promo.tag }}
                </span>
              </div>

              <!-- Bloque Inferior con Información de la Promoción -->
              <div class="relative z-10 flex flex-col gap-1.5">
                <h3
                  class="font-display font-black text-white group-hover:text-primary transition-colors leading-tight"
                  :class="slotIdx === 1 ? 'text-2xl sm:text-3xl' : 'text-xl'"
                >
                  {{ promo.title }}
                </h3>

                <p
                  class="font-body text-xs text-muted-light line-clamp-1"
                  :class="slotIdx === 1 ? 'sm:text-sm sm:line-clamp-2' : ''"
                >
                  {{ promo.subtitle }}
                </p>

                <div class="pt-2 border-t border-white/10 flex items-center justify-between text-[11px] font-mono text-medium-emphasis">
                  <span class="truncate max-w-50">{{ promo.card }}</span>
                  <span class="text-primary font-semibold flex items-center gap-1 shrink-0">
                    <span class="material-symbols-outlined text-sm">credit_card</span>
                    <span>En Checkout</span>
                  </span>
                </div>
              </div>
            </div>
          </transition>
        </div>
      </div>
    </div>
  </section>
</template>

<style scoped>
.promo-fade-enter-active,
.promo-fade-leave-active {
  transition: opacity 0.5s ease-in-out, transform 0.5s ease-in-out;
}
.promo-fade-enter-from {
  opacity: 0;
  transform: scale(0.96);
}
.promo-fade-leave-to {
  opacity: 0;
  transform: scale(1.04);
}
</style>
