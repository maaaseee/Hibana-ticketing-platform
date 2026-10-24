import { apiClient } from './apiClient'
import type {
  EventSeatsResponse,
  HoldSeatRequest,
  HoldSeatResponse,
  ConfirmReservationRequest,
  ConfirmReservationResponse,
  Seat,
} from '../types'

const BASE_URL = import.meta.env.VITE_INVENTORY_API_URL || '/api/inventory'

export function mapDtoToSeat(dto: any, index: number): Seat {
  // Generate deterministic grid position if coordinates are not stored in DB
  const col = (index % 12) + 1
  const rowIdx = Math.floor(index / 12)
  const rowChar = dto.rowIdentifier || String.fromCharCode(65 + (rowIdx % 26))
  const seatNum = typeof dto.seatNumber === 'number' ? dto.seatNumber : parseInt(dto.seatNumber, 10) || col

  let uiStatus: 'AVAILABLE' | 'SELECTED' | 'RESERVED' | 'LOCKED' = 'AVAILABLE'
  if (dto.status === 'HELD' || dto.status === 'BOOKED') {
    uiStatus = 'RESERVED'
  } else if (dto.status === 'BLOCKED') {
    uiStatus = 'LOCKED'
  }

  return {
    id: dto.seatId,
    row: rowChar,
    number: seatNum,
    sector: dto.sectorId,
    price: dto.price || 45000,
    status: uiStatus,
    x: 60 + col * 48,
    y: 70 + rowIdx * 48,
  }
}

export const inventoryService = {
  async getSeats(eventId: string, sectorId?: string): Promise<Seat[]> {
    try {
      const url = sectorId
        ? `${BASE_URL}/events/${eventId}/seats?sectorId=${encodeURIComponent(sectorId)}`
        : `${BASE_URL}/events/${eventId}/seats`

      const data = await apiClient.get<EventSeatsResponse>(url)
      if (data && Array.isArray(data.seats) && data.seats.length > 0) {
        return data.seats.map((s, idx) => mapDtoToSeat(s, idx))
      }
    } catch {
      // Graceful fallback to client seat grid
    }
    return []
  },

  async holdSeat(request: HoldSeatRequest): Promise<HoldSeatResponse> {
    return apiClient.post<HoldSeatResponse>(`${BASE_URL}/reservations`, request)
  },

  async confirmReservation(
    reservationId: string,
    request: ConfirmReservationRequest = {}
  ): Promise<ConfirmReservationResponse> {
    return apiClient.post<ConfirmReservationResponse>(
      `${BASE_URL}/reservations/${reservationId}/confirm`,
      request
    )
  },

  async releaseReservation(reservationId: string, reason = 'USER_CANCELLED'): Promise<void> {
    try {
      await apiClient.delete<void>(
        `${BASE_URL}/reservations/${reservationId}?reason=${encodeURIComponent(reason)}`
      )
    } catch {
      // Ignore if already deleted or expired
    }
  },
}
