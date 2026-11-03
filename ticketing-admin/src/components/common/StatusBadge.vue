<template>
  <span
    :class="[
      'inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full text-xs font-mono font-medium tracking-wide uppercase border',
      badgeStyles
    ]"
  >
    <span :class="['w-1.5 h-1.5 rounded-full', dotStyles]" />
    {{ label }}
  </span>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{
  status: string
}>()

const statusUpper = computed(() => (props.status || 'DRAFT').toUpperCase())

const label = computed(() => {
  switch (statusUpper.value) {
    case 'PUBLISHED':
      return 'Publicado'
    case 'DRAFT':
      return 'Borrador'
    case 'CANCELLED':
      return 'Cancelado'
    default:
      return statusUpper.value
  }
})

const badgeStyles = computed(() => {
  switch (statusUpper.value) {
    case 'PUBLISHED':
      return 'bg-secondary/10 text-secondary border-secondary/30'
    case 'CANCELLED':
      return 'bg-error/10 text-error border-error/30'
    case 'DRAFT':
    default:
      return 'bg-tertiary/10 text-tertiary border-tertiary/30'
  }
})

const dotStyles = computed(() => {
  switch (statusUpper.value) {
    case 'PUBLISHED':
      return 'bg-secondary animate-pulse'
    case 'CANCELLED':
      return 'bg-error'
    case 'DRAFT':
    default:
      return 'bg-tertiary'
  }
})
</script>
