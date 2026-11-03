<script setup lang="ts">
defineProps<{
  targetId?: string
}>()
</script>

<template>
  <a
    :href="`#${targetId || 'main-content'}`"
    class="sr-only focus:not-sr-only focus:fixed focus:top-4 focus:left-4 focus:z-[99999] focus:px-4 focus:py-2.5 focus:rounded-xl focus:bg-[#0084ff] focus:text-white focus:font-display focus:font-bold focus:text-sm focus:shadow-[0_0_25px_rgba(0,132,255,0.7)] focus:outline-none focus:ring-2 focus:ring-white transition-all"
  >
    Saltar al contenido principal
  </a>
</template>
