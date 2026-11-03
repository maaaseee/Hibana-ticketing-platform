import { ref, onMounted, onUnmounted } from 'vue'

const isOnline = ref(typeof navigator !== 'undefined' ? navigator.onLine : true)
const wasOffline = ref(false)

export function useNetworkStatus() {
  function handleOnline() {
    isOnline.value = true
    wasOffline.value = true
  }

  function handleOffline() {
    isOnline.value = false
    wasOffline.value = true
  }

  onMounted(() => {
    if (typeof window !== 'undefined') {
      window.addEventListener('online', handleOnline)
      window.addEventListener('offline', handleOffline)
      isOnline.value = navigator.onLine
    }
  })

  onUnmounted(() => {
    if (typeof window !== 'undefined') {
      window.removeEventListener('online', handleOnline)
      window.removeEventListener('offline', handleOffline)
    }
  })

  return {
    isOnline,
    wasOffline,
    resetWasOffline: () => { wasOffline.value = false }
  }
}
