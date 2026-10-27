// ---------------------------------------------------------------------------
// Backend Microservices Contracts (Catalog & Inventory)
// ---------------------------------------------------------------------------

export interface ProblemDetail {
  type?: string
  title?: string
  status?: number
  detail?: string
  instance?: string
  code?: string
  invalidParams?: { name: string; reason: string }[]
}

export interface CatalogSectorResponse {
  sectorId: string
  name: string
  type: string
  totalCapacity: number
  basePrice: number
  serviceFee: number
  totalPrice: number
  currency: string
}

export interface CatalogEventResponse {
  id: string
  slug: string
  title: string
  status: string
  category: string
  genres: string[]
  doorsOpenAt: string
  startsAt: string
  endsAt: string
  timezone: string
  venueName?: string
  venueCity?: string
  venueAddress?: string
  seatMapUrl?: string
  venueImageUrl?: string
  minPrice: number
  currency: string
  bannerUrl?: string
  posterUrl?: string
  description?: string
  salesPolicyJson?: string
  sectors: CatalogSectorResponse[]
}

export interface SeatResponseDto {
  seatId: string
  eventId: string
  sectorId: string
  rowIdentifier: string
  seatNumber: string | number
  price: number
  currency: string
  status: 'AVAILABLE' | 'HELD' | 'BOOKED' | 'BLOCKED'
}

export interface EventSeatsResponse {
  eventId: string
  totalSeats: number
  availableSeats: number
  heldSeats: number
  bookedSeats: number
  blockedSeats: number
  seats: SeatResponseDto[]
}

export interface HoldSeatRequest {
  eventId: string
  seatId: string
  userId: string
  holdDurationSeconds?: number
}

export interface HoldSeatResponse {
  reservationId: string
  eventId: string
  seatId: string
  userId: string
  status: string
  expiresAt: string
  countdownSeconds: number
}

export interface ConfirmReservationRequest {
  orderId?: string
}

export interface ConfirmReservationResponse {
  reservationId: string
  eventId: string
  seatId: string
  status: string
  orderId: string
  confirmedAt: string
}
