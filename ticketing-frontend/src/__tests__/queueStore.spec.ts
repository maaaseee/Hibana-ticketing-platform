import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useQueueStore } from '../stores/queue'

describe('queueStore.spec.ts - Virtual Waiting Room Pinia Store', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    sessionStorage.clear()
    vi.restoreAllMocks()
  })

  afterEach(() => {
    sessionStorage.clear()
  })

  it('initializes with default waiting values', () => {
    const store = useQueueStore()
    expect(store.isInQueue).toBe(false)
    expect(store.checkoutToken).toBeNull()
    expect(store.progressPercentage).toBeGreaterThanOrEqual(0)
  })

  it('stores checkout token in state and sessionStorage when granted', () => {
    const store = useQueueStore()
    store.grantCheckoutToken('chk_jwt_test_token_123')

    expect(store.checkoutToken).toBe('chk_jwt_test_token_123')
    expect(store.currentPosition).toBe(0)
    expect(store.progressPercentage).toBe(100)
    expect(store.formattedEstimatedTime).toBe('¡Es tu turno!')
    expect(sessionStorage.getItem('waiting_room_checkout_token')).toBe('chk_jwt_test_token_123')
  })

  it('skipQueueForDemo issues token and sets state to admitted', async () => {
    const store = useQueueStore()
    await store.skipQueueForDemo()

    expect(store.checkoutToken).toBeTruthy()
    expect(store.currentPosition).toBe(0)
    expect(sessionStorage.getItem('waiting_room_checkout_token')).toBeTruthy()
  })

  it('leaveQueue clears state and deletes sessionStorage token', async () => {
    const store = useQueueStore()
    store.grantCheckoutToken('dummy-token')
    expect(sessionStorage.getItem('waiting_room_checkout_token')).toBe('dummy-token')

    await store.leaveQueue()
    expect(store.isInQueue).toBe(false)
    expect(store.checkoutToken).toBeNull()
    expect(sessionStorage.getItem('waiting_room_checkout_token')).toBeNull()
  })

  it('enterQueue immediately admits if valid token already in storage', async () => {
    sessionStorage.setItem('waiting_room_checkout_token', 'cached-valid-jwt')
    const store = useQueueStore()

    await store.enterQueue('event-vip-2026')
    expect(store.checkoutToken).toBe('cached-valid-jwt')
    expect(store.currentPosition).toBe(0)
    expect(store.progressPercentage).toBe(100)
  })
})
