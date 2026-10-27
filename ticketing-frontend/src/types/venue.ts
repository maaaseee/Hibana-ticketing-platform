import type { PaymentPromotion, SectorPricing } from './event'

export interface VenueParking {
  available: boolean
  title: string
  description: string
  price?: number
  accessRoutes: string[]
  bookingPassUrl?: string
}

export interface VenueTransit {
  busLines: string[]
  trains: string[]
  subway?: string[]
  googleMapsUrl?: string
  embedQuery?: string
}

export interface VenueInfo {
  id: string
  name: string
  shortName: string
  city: 'Buenos Aires'
  country: 'Argentina'
  address: string
  capacity: number
  type: 'ESTADIO_MASIVO' | 'ARENA_CUBIERTO' | 'TEATRO_HISTORICO'
  imageUrl?: string
  parking: VenueParking
  transit?: VenueTransit
  paymentMethods: PaymentPromotion[]
  sectors: SectorPricing[]
}
