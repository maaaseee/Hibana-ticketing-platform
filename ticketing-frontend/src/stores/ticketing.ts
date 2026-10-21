import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { Seat, Sector } from '../types'

export const useTicketingStore = defineStore('ticketing', () => {
  const sectors = ref<Sector[]>([
    { id: 'platea-a', name: 'Platea A (Frente al Escenario)', price: 120, color: '#38bdf8', availableCount: 42, totalCount: 60 },
    { id: 'platea-b', name: 'Platea B (Lateral)', price: 85, color: '#14b8a6', availableCount: 58, totalCount: 80 },
    { id: 'vip-diamonds', name: 'VIP Lounge Lounge Bar', price: 180, color: '#f59e0b', availableCount: 16, totalCount: 30 },
    { id: 'tribuna-alta', name: 'Tribuna Superior', price: 50, color: '#818cf8', availableCount: 110, totalCount: 150 },
  ])

  const selectedSectorId = ref<string>('platea-a')
  const selectedSeats = ref<Seat[]>([])
  
  // Redlock 10-minute (600s) atomic lock timer
  const lockTimeRemaining = ref<number>(600)
  const isLockActive = ref<boolean>(false)
  let lockTimerInterval: any = null

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
          price: rIdx < 2 ? 120 : (rIdx < 4 ? 85 : 50),
          status: isUnavailable ? 'RESERVED' : 'AVAILABLE',
          x: 60 + c * 48,
          y: 70 + rIdx * 48,
        })
      }
    })
    return list
  }

  function startLockTimer() {
    if (isLockActive.value) return
    isLockActive.value = true
    lockTimeRemaining.value = 600

    if (lockTimerInterval) clearInterval(lockTimerInterval)
    lockTimerInterval = setInterval(() => {
      if (lockTimeRemaining.value > 0) {
        lockTimeRemaining.value--
      } else {
        releaseExpiredLocks()
        clearInterval(lockTimerInterval)
      }
    }, 1000)
  }

  function toggleSeat(seatId: string) {
    const seat = seats.value.find(s => s.id === seatId)
    if (!seat || seat.status === 'RESERVED') return

    if (seat.status === 'SELECTED') {
      seat.status = 'AVAILABLE'
      selectedSeats.value = selectedSeats.value.filter(s => s.id !== seatId)
      if (selectedSeats.value.length === 0) {
        resetLockTimer()
      }
    } else if (seat.status === 'AVAILABLE') {
      if (selectedSeats.value.length >= 6) {
        alert('Límite máximo de 6 butacas por compra para prevenir acaparamiento.')
        return
      }
      seat.status = 'SELECTED'
      selectedSeats.value.push(seat)
      startLockTimer()
    }
  }

  function releaseExpiredLocks() {
    selectedSeats.value.forEach(s => {
      const target = seats.value.find(item => item.id === s.id)
      if (target) target.status = 'AVAILABLE'
    })
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

  const serviceFee = computed(() => {
    return Math.round(subtotal.value * 0.12)
  })

  const total = computed(() => {
    return subtotal.value + serviceFee.value
  })

  return {
    sectors,
    selectedSectorId,
    seats,
    selectedSeats,
    lockTimeRemaining,
    isLockActive,
    formattedLockTime,
    subtotal,
    serviceFee,
    total,
    toggleSeat,
    startLockTimer,
    resetLockTimer,
    releaseExpiredLocks,
  }
})
