import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { apiClient } from '../services/apiClient'

export interface QueueStatusDto {
  eventId: string
  userId: string
  status: 'WAITING' | 'ADMITTED' | 'NOT_IN_QUEUE'
  position?: number
  totalInQueue?: number
  estimatedWaitSeconds?: number
  checkoutToken?: string | null
  tokenExpiresAt?: number | null
}

const STORAGE_KEY = 'waiting_room_checkout_token'
const USER_KEY = 'waiting_room_user_id'

function getOrCreateUserId(): string {
  if (typeof sessionStorage !== 'undefined') {
    const existing = sessionStorage.getItem(USER_KEY)
    if (existing) return existing
    const generated = `usr-${Date.now().toString(36)}-${Math.random().toString(36).substring(2, 7)}`
    sessionStorage.setItem(USER_KEY, generated)
    return generated
  }
  return `usr-${Date.now()}`
}

export const useQueueStore = defineStore('queue', () => {
  const isInQueue = ref<boolean>(false)
  const currentEventId = ref<string>('')
  const currentUserId = ref<string>(getOrCreateUserId())
  const initialPosition = ref<number>(1248)
  const currentPosition = ref<number>(1248)
  const totalInQueue = ref<number>(1248)
  const estimatedSeconds = ref<number>(240) // 4 mins default
  const checkoutToken = ref<string | null>(
    typeof sessionStorage !== 'undefined' ? sessionStorage.getItem(STORAGE_KEY) : null
  )
  const tokenExpiresAt = ref<number | null>(null)
  const isConnected = ref<boolean>(true)

  let timerId: any = null
  let eventSource: EventSource | null = null

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

  function storeToken(token: string, expiresAt?: number | null) {
    checkoutToken.value = token
    tokenExpiresAt.value = expiresAt || Date.now() + 15 * 60 * 1000
    if (typeof sessionStorage !== 'undefined') {
      sessionStorage.setItem(STORAGE_KEY, token)
    }
  }

  function clearToken() {
    checkoutToken.value = null
    tokenExpiresAt.value = null
    if (typeof sessionStorage !== 'undefined') {
      sessionStorage.removeItem(STORAGE_KEY)
    }
  }

  function grantCheckoutToken(mockToken?: string) {
    currentPosition.value = 0
    estimatedSeconds.value = 0
    const tok = mockToken || `chk_tok_${Date.now()}_${Math.random().toString(36).substring(2, 10)}`
    storeToken(tok)
  }

  async function enterQueue(eventId: string) {
    isInQueue.value = true
    currentEventId.value = eventId
    const userId = currentUserId.value

    // If we already have a valid checkout token for this session, admit immediately
    if (checkoutToken.value) {
      currentPosition.value = 0
      estimatedSeconds.value = 0
      return
    }

    // Default optimistic state
    initialPosition.value = Math.floor(Math.random() * 400) + 120
    currentPosition.value = initialPosition.value
    estimatedSeconds.value = Math.round(currentPosition.value * 0.4)

    // Attempt real backend join
    try {
      const response = await apiClient.post<QueueStatusDto>('/api/v1/queue/join', {
        eventId,
        userId,
      })

      if (response && response.status === 'ADMITTED' && response.checkoutToken) {
        currentPosition.value = 0
        estimatedSeconds.value = 0
        storeToken(response.checkoutToken, response.tokenExpiresAt)
        return
      }

      if (response && response.status === 'WAITING' && response.position) {
        initialPosition.value = Math.max(initialPosition.value, response.position)
        currentPosition.value = response.position
        if (response.totalInQueue) totalInQueue.value = response.totalInQueue
        if (response.estimatedWaitSeconds) estimatedSeconds.value = response.estimatedWaitSeconds
        startListening(eventId, userId)
        return
      }
    } catch {
      // Backend unreachable or offline: fallback to client-side progress simulation
    }

    startLocalSimulation()
  }

  function startListening(eventId: string, userId: string) {
    stopListening()

    // Try EventSource / SSE if supported in browser
    if (typeof EventSource !== 'undefined') {
      try {
        const streamUrl = `/api/v1/queue/stream?eventId=${encodeURIComponent(eventId)}&userId=${encodeURIComponent(userId)}`
        eventSource = new EventSource(streamUrl)

        eventSource.addEventListener('queue-status', (e: MessageEvent) => {
          try {
            const data: QueueStatusDto = JSON.parse(e.data)
            handleStatusUpdate(data)
          } catch {
            // Malformed event payload
          }
        })

        eventSource.onerror = () => {
          stopListening()
          startPolling(eventId, userId)
        }
        return
      } catch {
        // Fallback to polling
      }
    }

    startPolling(eventId, userId)
  }

  function startPolling(eventId: string, userId: string) {
    if (timerId) clearInterval(timerId)
    timerId = setInterval(async () => {
      try {
        const status = await apiClient.get<QueueStatusDto>(
          `/api/v1/queue/status?eventId=${encodeURIComponent(eventId)}&userId=${encodeURIComponent(userId)}`
        )
        if (status) {
          handleStatusUpdate(status)
        }
      } catch {
        // Fallback to local decrement if polling fails
        decrementLocalPosition()
      }
    }, 2000)
  }

  function handleStatusUpdate(data: QueueStatusDto) {
    if (data.status === 'ADMITTED' && data.checkoutToken) {
      currentPosition.value = 0
      estimatedSeconds.value = 0
      storeToken(data.checkoutToken, data.tokenExpiresAt)
      stopListening()
    } else if (data.status === 'WAITING' && typeof data.position === 'number') {
      currentPosition.value = data.position
      if (data.totalInQueue) totalInQueue.value = data.totalInQueue
      if (data.estimatedWaitSeconds) estimatedSeconds.value = data.estimatedWaitSeconds
    }
  }

  function startLocalSimulation() {
    if (timerId) clearInterval(timerId)
    timerId = setInterval(() => {
      decrementLocalPosition()
    }, 1000)
  }

  function decrementLocalPosition() {
    if (currentPosition.value > 0) {
      const step = Math.floor(Math.random() * 12) + 5
      currentPosition.value = Math.max(0, currentPosition.value - step)
      estimatedSeconds.value = Math.max(5, Math.round(currentPosition.value * 0.4))
    } else {
      grantCheckoutToken()
      stopListening()
    }
  }

  function stopListening() {
    if (timerId) {
      clearInterval(timerId)
      timerId = null
    }
    if (eventSource) {
      eventSource.close()
      eventSource = null
    }
  }

  async function skipQueueForDemo() {
    stopListening()
    const eventId = currentEventId.value || 'chronos-fest-2026'
    const userId = currentUserId.value

    try {
      const response = await apiClient.post<QueueStatusDto>('/api/v1/queue/skip', {
        eventId,
        userId,
      })

      if (response && response.status === 'ADMITTED' && response.checkoutToken) {
        currentPosition.value = 0
        estimatedSeconds.value = 0
        storeToken(response.checkoutToken, response.tokenExpiresAt)
        return
      }
    } catch {
      // Fallback
    }

    grantCheckoutToken()
  }

  async function leaveQueue() {
    isInQueue.value = false
    currentPosition.value = 0
    stopListening()
    clearToken()

    const eventId = currentEventId.value
    const userId = currentUserId.value
    if (eventId) {
      try {
        await apiClient.post('/api/v1/queue/leave', { eventId, userId })
      } catch {
        // Ignore network errors on leave
      }
    }
  }

  return {
    isInQueue,
    currentEventId,
    currentUserId,
    initialPosition,
    currentPosition,
    totalInQueue,
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
    stopListening,
  }
})
