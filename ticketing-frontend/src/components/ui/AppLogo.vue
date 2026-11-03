<script setup lang="ts">
import { computed } from 'vue'
import { RouterLink } from 'vue-router'

interface Props {
  /** Ruta de destino. Si es vacía o null, no envuelve en enlace */
  to?: string | object | null
  /** Tamaño del logo: 'sm' | 'md' | 'lg' | 'xl' */
  size?: 'sm' | 'md' | 'lg' | 'xl'
  /** Estilo visual: 'standard' (sin borde) o 'pill' (con cápsula traslúcida) */
  variant?: 'standard' | 'pill'
  /** Renderizar como botón para capturar eventos de confirmación (ej: salas de espera) */
  asButton?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  to: '/',
  size: 'md',
  variant: 'standard',
  asButton: false
})

const emit = defineEmits<{
  (e: 'click', event: MouseEvent): void
}>()

const logoUrl = '/logo.png'

const sizeClasses = computed(() => {
  switch (props.size) {
    case 'sm':
      return {
        img: 'w-6 h-6',
        text: 'text-lg sm:text-xl',
        gap: 'gap-2',
        pill: 'px-2.5 py-1'
      }
    case 'lg':
      return {
        img: 'w-9 h-9',
        text: 'text-2xl sm:text-3xl',
        gap: 'gap-3',
        pill: 'px-4 py-1.5'
      }
    case 'xl':
      return {
        img: 'w-10 h-10',
        text: 'text-3xl sm:text-4xl',
        gap: 'gap-3.5',
        pill: 'px-5 py-2'
      }
    case 'md':
    default:
      return {
        img: 'w-7 h-7 sm:w-8 sm:h-8',
        text: 'text-xl sm:text-2xl',
        gap: 'gap-2.5 sm:gap-3',
        pill: 'px-3 py-1.5'
      }
  }
})

function handleClick(e: MouseEvent) {
  emit('click', e)
}
</script>

<template>
  <!-- Modo Botón (ej: QueueHeader con advertencia de salida) -->
  <button
    v-if="asButton"
    type="button"
    @click="handleClick"
    class="flex items-center group cursor-pointer outline-none transition-transform active:scale-95 text-left"
    :class="[
      sizeClasses.gap,
      variant === 'pill'
        ? `${sizeClasses.pill} rounded-full border border-white/20 bg-black/50 backdrop-blur-md hover:border-[#38bdf8]`
        : ''
    ]"
    aria-label="Hibana.tk Inicio"
  >
    <img
      :src="logoUrl"
      alt="Hibana.tk Logo"
      class="object-contain drop-shadow-[0_0_12px_rgba(134,207,251,0.5)] group-hover:scale-105 transition-transform duration-300 shrink-0"
      :class="sizeClasses.img"
    />
    <span
      class="font-display font-black text-primary tracking-tight group-hover:text-on-primary-container transition-colors"
      :class="sizeClasses.text"
    >
      Hibana.tk
    </span>
  </button>

  <!-- Modo Enlace a Home (Default) -->
  <RouterLink
    v-else-if="to"
    :to="to"
    @click="handleClick"
    class="flex items-center group cursor-pointer outline-none shrink-0"
    :class="[
      sizeClasses.gap,
      variant === 'pill'
        ? `${sizeClasses.pill} rounded-full border border-white/20 bg-black/50 backdrop-blur-md hover:border-[#38bdf8] transition-colors`
        : ''
    ]"
    aria-label="Hibana.tk Inicio"
  >
    <img
      :src="logoUrl"
      alt="Hibana.tk Logo"
      class="object-contain drop-shadow-[0_0_12px_rgba(134,207,251,0.5)] group-hover:scale-105 transition-transform duration-300 shrink-0"
      :class="sizeClasses.img"
    />
    <span
      class="font-display font-black text-primary tracking-tight group-hover:text-on-primary-container transition-colors"
      :class="sizeClasses.text"
    >
      Hibana.tk
    </span>
  </RouterLink>

  <!-- Modo Estático (Div) -->
  <div
    v-else
    class="flex items-center shrink-0"
    :class="[
      sizeClasses.gap,
      variant === 'pill'
        ? `${sizeClasses.pill} rounded-full border border-white/20 bg-black/50 backdrop-blur-md`
        : ''
    ]"
  >
    <img
      :src="logoUrl"
      alt="Hibana.tk Logo"
      class="object-contain drop-shadow-[0_0_12px_rgba(134,207,251,0.5)] shrink-0"
      :class="sizeClasses.img"
    />
    <span
      class="font-display font-black text-primary tracking-tight"
      :class="sizeClasses.text"
    >
      Hibana.tk
    </span>
  </div>
</template>
