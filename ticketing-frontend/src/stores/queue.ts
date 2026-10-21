import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export const useQueueStore = defineStore('queue', () => {
  const isInQueue = ref<boolean>(false)
  const currentEventId = ref<string>('')
  const initialPosition = ref<number>(1248)
  const currentPosition = ref<number>(1248)
  const estimatedSeconds = ref<number>(240) // 4 mins
  const checkoutToken = ref<string | null>(null)
  const tokenExpiresAt = ref<number | null>(null)
  const isConnected = ref<boolean>(true)
  let timerId: any = null

  const progressPercentage = computed(() => {
    if (currentPosition.value <= 0) return 100
    const progress = ((initialPosition.value - currentPosition.value) / initialPosition.value) * 100
    return Math.min(100, Math.max(0, Math.round(progress)))
  })

  const formattedEstimatedTime = computed(() => {
    if (currentPosition.value <= 0) return '¡Es tu turno!'
    const mins = Math.floor(estimatedSeconds.value / 60)
    const secs = estimatedSeconds.value % 60
    return `~${mins > 0 ? `${mins}m ` : ''}${secs}s`
  })

  function enterQueue(eventId: string) {
    isInQueue.value = true
    currentEventId.value = eventId
    initialPosition.value = Math.floor(Math.random() * 400) + 120
    currentPosition.value = initialPosition.value
    estimatedSeconds.value = Math.round(currentPosition.value * 0.4)
    checkoutToken.value = null
    tokenExpiresAt.value = null

    if (timerId) clearInterval(timerId)

    timerId = setInterval(() => {
      if (currentPosition.value > 0) {
        const step = Math.floor(Math.random() * 12) + 5
        currentPosition.value = Math.max(0, currentPosition.value - step)
        estimatedSeconds.value = Math.max(5, Math.round(currentPosition.value * 0.4))
      } else {
        grantCheckoutToken()
        clearInterval(timerId)
      }
    }, 1000)
  }

  function grantCheckoutToken() {
    currentPosition.value = 0
    estimatedSeconds.value = 0
    const mockJwt = `chk_tok_${Date.now()}_${Math.random().toString(36).substring(2, 10)}`
    checkoutToken.value = mockJwt
    tokenExpiresAt.value = Date.now() + 15 * 60 * 1000 // 15 mins
  }

  function skipQueueForDemo() {
    grantCheckoutToken()
    if (timerId) clearInterval(timerId)
  }

  function leaveQueue() {
    isInQueue.value = false
    currentPosition.value = 0
    if (timerId) clearInterval(timerId)
  }

  return {
    isInQueue,
    currentEventId,
    initialPosition,
    currentPosition,
    estimatedSeconds,
    checkoutToken,
    tokenExpiresAt,
    isConnected,
    progressPercentage,
    formattedEstimatedTime,
    enterQueue,
    grantCheckoutToken,
    skipQueueForDemo,
    leaveQueue,
  }
})
