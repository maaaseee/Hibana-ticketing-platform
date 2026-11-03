<script setup lang="ts">
import { ref, watch } from 'vue'
import { useNetworkStatus } from '../../composables/useNetworkStatus'

const { isOnline, wasOffline, resetWasOffline } = useNetworkStatus()
const showReconnectedNotice = ref(false)

watch(isOnline, (online) => {
  if (online && wasOffline.value) {
    showReconnectedNotice.value = true
    setTimeout(() => {
      showReconnectedNotice.value = false
      resetWasOffline()
    }, 4000)
  }
})
</script>

<template>
  <!-- Offline Warning Banner -->
  <Transition
    enter-active-class="transition-all duration-300 ease-out"
    enter-from-class="-translate-y-full opacity-0"
    enter-to-class="translate-y-0 opacity-100"
    leave-active-class="transition-all duration-200 ease-in"
    leave-from-class="translate-y-0 opacity-100"
    leave-to-class="-translate-y-full opacity-0"
  >
    <div
      v-if="!isOnline"
      class="fixed top-0 inset-x-0 z-[99998] bg-rose-600/95 text-white py-2 px-4 shadow-[0_4px_25px_rgba(225,29,72,0.4)] backdrop-blur-md flex items-center justify-center gap-2.5 text-xs sm:text-sm font-body font-semibold"
      role="alert"
    >
      <span class="material-symbols-outlined text-[18px] animate-pulse">wifi_off</span>
      <span>Sin conexión a Internet. Tus selecciones y reservas se mantendrán en espera hasta reconectar.</span>
    </div>
  </Transition>

  <!-- Reconnected Brief Success Banner -->
  <Transition
    enter-active-class="transition-all duration-300 ease-out"
    enter-from-class="-translate-y-full opacity-0"
    enter-to-class="translate-y-0 opacity-100"
    leave-active-class="transition-all duration-200 ease-in"
    leave-from-class="translate-y-0 opacity-100"
    leave-to-class="-translate-y-full opacity-0"
  >
    <div
      v-if="showReconnectedNotice"
      class="fixed top-0 inset-x-0 z-[99998] bg-emerald-600/95 text-white py-2 px-4 shadow-[0_4px_25px_rgba(16,185,129,0.4)] backdrop-blur-md flex items-center justify-center gap-2.5 text-xs sm:text-sm font-body font-semibold"
      role="status"
    >
      <span class="material-symbols-outlined text-[18px]">wifi</span>
      <span>¡Conexión a Internet restablecida con éxito!</span>
    </div>
  </Transition>
</template>
