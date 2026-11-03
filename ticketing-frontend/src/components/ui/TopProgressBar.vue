<script setup lang="ts">
import { useLoadingBarStore } from '../../stores/loadingBar'

const loadingBar = useLoadingBarStore()
</script>

<template>
  <div
    v-if="loadingBar.isLoading"
    class="fixed top-0 left-0 right-0 h-[3px] z-[99999] pointer-events-none overflow-hidden transition-opacity duration-200"
    :class="loadingBar.isLoading ? 'opacity-100' : 'opacity-0'"
  >
    <div
      class="h-full transition-all duration-200 ease-out shadow-[0_0_12px_rgba(56,189,248,0.8)]"
      :class="loadingBar.isFailed ? 'bg-rose-500' : 'bg-gradient-to-r from-[#0084ff] via-[#38bdf8] to-[#4fdbc8]'"
      :style="{ width: `${loadingBar.progress}%` }"
    />
  </div>
</template>
