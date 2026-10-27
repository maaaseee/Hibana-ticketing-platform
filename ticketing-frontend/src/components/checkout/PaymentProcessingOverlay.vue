<script setup lang="ts">
defineProps<{
  isProcessing: boolean
}>()
</script>

<template>
  <div
    v-if="isProcessing"
    class="fixed inset-0 z-50 bg-[#0c141f]/85 backdrop-blur-md flex flex-col items-center justify-center p-4 transition-all duration-300"
    id="processing-overlay"
  >
    <div
      class="bg-[#141c27] p-8 sm:p-10 rounded-2xl border border-[#23354d] flex flex-col items-center gap-5 max-w-sm w-[90%] mx-auto text-center shadow-2xl"
      id="processing-card"
    >
      <div class="relative w-16 h-16 flex items-center justify-center">
        <div class="spinner absolute inset-0"></div>
        <span class="material-symbols-outlined text-[#86cffb] text-2xl" style="font-variation-settings: 'FILL' 1;">
          security
        </span>
      </div>

      <div class="flex flex-col gap-1.5">
        <h3 class="font-display font-bold text-lg sm:text-xl text-[#f1f5f9]">
          Validando transacción segura...
        </h3>
        <p class="text-xs sm:text-sm text-[#94a3b8]">
          Por favor no cierres ni actualices esta página.
        </p>
      </div>

      <!-- Glowing Animated Progress Bar -->
      <div class="w-full bg-[#1b2533] h-1.5 rounded-full overflow-hidden mt-1 border border-[#23354d]/50">
        <div class="bg-[#00688f] h-full rounded-full animated-slide-bar"></div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.spinner {
  border: 3px solid #1e2c3d;
  border-top: 3px solid #00688f;
  border-radius: 50%;
  width: 48px;
  height: 48px;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  0% {
    transform: rotate(0deg);
  }
  100% {
    transform: rotate(360deg);
  }
}

.animated-slide-bar {
  animation: slideRight 1.8s ease-in-out infinite alternate;
}

@keyframes slideRight {
  0% {
    transform: translateX(0%);
    width: 25%;
  }
  100% {
    transform: translateX(260%);
    width: 35%;
  }
}
</style>
