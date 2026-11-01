import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { Seat, Sector, ProblemDetail } from '../types'
import { inventoryService } from '../services/inventoryService'
import { ApiError } from '../services/apiClient'

export interface ActiveSeatHold {
  reservationId: string
  seatId: string
  eventId: string
  expiresAt: string
}

export const useTicketingStore = defineStore('ticketing', () => {
  const currentEventId = ref<string>('evt_coldplay_2026')
  const sectors = ref<Sector[]>([
    { id: 'platea-a', name: 'Platea A (Frente al Escenario)', price: 120000, color: '#38bdf8', availableCount: 42, totalCount: 60 },
    { id: 'platea-b', name: 'Platea B (Lateral)', price: 85000, color: '#14b8a6', availableCount: 58, totalCount: 80 },
    { id: 'vip-diamonds', name: 'VIP Lounge Lounge Bar', price: 180000, color: '#f59e0b', availableCount: 16, totalCount: 30 },
    { id: 'tribuna-alta', name: 'Tribuna Superior', price: 50000, color: '#818cf8', availableCount: 110, totalCount: 150 },
  ])

  const selectedSectorId = ref<string>('platea-a')
  const selectedSeats = ref<Seat[]>([])
  const activeHolds = ref<ActiveSeatHold[]>([])
  const isLoadingSeats = ref<boolean>(false)
  const lastError = ref<ProblemDetail | null>(null)
  const errorNotification = ref<string | null>(null)
  
  // Redlock 10-minute (600s) atomic lock timer
  const lockTimeRemaining = ref<number>(600)
  const isLockActive = ref<boolean>(false)
  const isTimerExpired = ref<boolean>(false)
  let lockTimerInterval: ReturnType<typeof setInterval> | null = null

  // Generated grid of seats
  const seats = ref<Seat[]>(generateMockSeats())

  function generateMockSeats(): Seat[] {
    const list: Seat[] = []
    const rows = ['A', 'B', 'C', 'D', 'E', 'F']
    const cols = 12

    rows.forEach((row, rIdx) => {
      for (let c = 1; c <= cols; c++) {
        const id = `${row}-${c}`
        const isUnavailable = (rIdx === 0 && (c === 4 || c === 5)) || (rIdx === 2 && (c === 8 || c === 9))
        list.push({
          id,
          row,
          number: c,
          sector: rIdx < 2 ? 'Platea A' : (rIdx < 4 ? 'Platea B' : 'Tribuna Alta'),
          price: rIdx < 2 ? 120000 : (rIdx < 4 ? 85000 : 50000),
          status: isUnavailable ? 'RESERVED' : 'AVAILABLE',
          x: 60 + c * 48,
          y: 70 + rIdx * 48,
        })
      }
    })
    return list
  }

  async function loadSeatsForEvent(eventId: string, sectorId?: string) {
    currentEventId.value = eventId
    isLoadingSeats.value = true
    errorNotification.value = null

    try {
      const remoteSeats = await inventoryService.getSeats(eventId, sectorId)
      if (remoteSeats && remoteSeats.length > 0) {
        seats.value = remoteSeats
      } else {
        seats.value = generateMockSeats()
      }
    } catch {
      seats.value = generateMockSeats()
    } finally {
      isLoadingSeats.value = false
    }
  }

  function startLockTimer(countdownSeconds = 600) {
    lockTimeRemaining.value = countdownSeconds
    isLockActive.value = true
    isTimerExpired.value = false

    if (lockTimerInterval) clearInterval(lockTimerInterval)
    lockTimerInterval = setInterval(() => {
      if (lockTimeRemaining.value > 0) {
        lockTimeRemaining.value--
      } else {
        isTimerExpired.value = true
        releaseExpiredLocks()
        if (lockTimerInterval) clearInterval(lockTimerInterval)
      }
    }, 1000)
  }

  function resetAndRestartTimer(countdownSeconds = 600) {
    isTimerExpired.value = false
    startLockTimer(countdownSeconds)
  }

  async function toggleSeat(seatId: string, userId = 'usr_guest_frontoffice') {
    errorNotification.value = null
    const seat = seats.value.find(s => s.id === seatId)
    if (!seat || seat.status === 'RESERVED') return

    if (seat.status === 'SELECTED') {
      // Release seat
      seat.status = 'AVAILABLE'
      selectedSeats.value = selectedSeats.value.filter(s => s.id !== seatId)
      const hold = activeHolds.value.find(h => h.seatId === seatId)
      if (hold) {
        activeHolds.value = activeHolds.value.filter(h => h.seatId !== seatId)
        inventoryService.releaseReservation(hold.reservationId).catch(() => {})
      }

      if (selectedSeats.value.length === 0) {
        resetLockTimer()
      }
    } else if (seat.status === 'AVAILABLE') {
      if (selectedSeats.value.length >= 6) {
        errorNotification.value = 'Límite máximo de 6 butacas por compra para prevenir acaparamiento.'
        return
      }

      // Optimistic select
      seat.status = 'SELECTED'
      selectedSeats.value.push(seat)

      // Remote Redlock reservation
      try {
        const result = await inventoryService.holdSeat({
          eventId: currentEventId.value,
          seatId: seat.id,
          userId,
          holdDurationSeconds: 600,
        })

        activeHolds.value.push({
          reservationId: result.reservationId,
          seatId: seat.id,
          eventId: result.eventId,
          expiresAt: result.expiresAt,
        })

        startLockTimer(result.countdownSeconds || 600)
      } catch (err: unknown) {
        // Rollback optimistic select on lock collision
        seat.status = 'RESERVED'
        selectedSeats.value = selectedSeats.value.filter(s => s.id !== seatId)
        if (err instanceof ApiError) {
          lastError.value = err.problem || null
          errorNotification.value = err.problem?.detail || err.message || 'La butaca fue bloqueada por otro usuario.'
        } else {
          // Fallback offline: keep local lock active
          startLockTimer(600)
        }
      }
    }
  }

  async function confirmAllActiveReservations(orderId: string): Promise<boolean> {
    if (activeHolds.value.length === 0) {
      return true
    }

    try {
      for (const hold of activeHolds.value) {
        await inventoryService.confirmReservation(hold.reservationId, { orderId })
      }
      activeHolds.value = []
      return true
    } catch (err: unknown) {
      if (err instanceof ApiError) {
        lastError.value = err.problem || null
        errorNotification.value = err.problem?.detail || 'Error confirmando reserva de asientos.'
      }
      return false
    }
  }

  function releaseExpiredLocks() {
    selectedSeats.value.forEach(s => {
      const target = seats.value.find(item => item.id === s.id)
      if (target) target.status = 'AVAILABLE'
    })
    activeHolds.value.forEach(hold => {
      inventoryService.releaseReservation(hold.reservationId, 'EXPIRED').catch(() => {})
    })
    activeHolds.value = []
    selectedSeats.value = []
    isLockActive.value = false
    lockTimeRemaining.value = 600
  }

  function resetLockTimer() {
    if (lockTimerInterval) clearInterval(lockTimerInterval)
    isLockActive.value = false
    lockTimeRemaining.value = 600
  }

  const formattedLockTime = computed(() => {
    const mins = Math.floor(lockTimeRemaining.value / 60)
    const secs = lockTimeRemaining.value % 60
    return `${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`
  })

  const subtotal = computed(() => {
    return selectedSeats.value.reduce((acc, s) => acc + s.price, 0)
  })

  // Service fees are calculated dynamically in Checkout based on admin configuration
  const serviceFee = computed(() => 0)

  const total = computed(() => {
    return subtotal.value
  })

  return {
    currentEventId,
    sectors,
    selectedSectorId,
    seats,
    selectedSeats,
    activeHolds,
    isLoadingSeats,
    lastError,
    errorNotification,
    lockTimeRemaining,
    isLockActive,
    isTimerExpired,
    formattedLockTime,
    subtotal,
    serviceFee,
    total,
    loadSeatsForEvent,
    toggleSeat,
    confirmAllActiveReservations,
    startLockTimer,
    resetLockTimer,
    resetAndRestartTimer,
    releaseExpiredLocks,
  }
})
