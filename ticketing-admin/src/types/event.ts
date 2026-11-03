export type EventCategory = 'CONCIERTOS' | 'FESTIVALES' | 'TEATRO' | 'DEPORTES' | 'ELECTRONICA' | 'OTRO'

export type EventStatus = 'DRAFT' | 'PUBLISHED' | 'CANCELLED'

export type SectorType = 'SEATED' | 'GENERAL_ADMISSION'

export interface EventSectorRequest {
  name: string
  type: SectorType
  totalCapacity: number
  basePrice: number
  serviceFee: number
  currency: string
}

export interface EventSectorResponse {
  id: string
  name: string
  type: SectorType
  totalCapacity: number
  basePrice: number
  serviceFee: number
  currency: string
}

export interface CreateEventRequest {
  organizerId: string
  venueId: string
  slug: string
  title: string
  category: string
  genres: string[]
  doorsOpenAt: string
  startsAt: string
  endsAt: string
  timezone: string
  media: string
  salesPolicy: string
  sectors: EventSectorRequest[]
}

export interface EventCreatedResponse {
  id: string
  slug: string
  status: EventStatus
  createdAt: string
  totalEstimatedCapacity: number
}

export interface EventResponse {
  id: string
  slug: string
  title: string
  status: EventStatus
  category: string
  genres: string[]
  doorsOpenAt: string
  startsAt: string
  endsAt: string
  timezone: string
  venueName: string
  venueCity: string
  venueAddress: string
  seatMapUrl?: string
  venueImageUrl?: string
  minPrice: number
  currency: string
  bannerUrl?: string
  posterUrl?: string
  description?: string
  salesPolicyJson?: string
  sectors?: EventSectorResponse[]
  serviceFeePercentage?: number
}
