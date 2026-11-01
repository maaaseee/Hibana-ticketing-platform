import type { VenueParking, VenueTransit } from './venue'

export interface EventFunction {
  id: string
  dateIso: string
  formattedDate: string
  doorsOpen: string
  showStart: string
  status: 'AVAILABLE' | 'FEW_SEATS' | 'SOLD_OUT' | 'QUEUE_ACTIVE'
}

export interface SectorPricing {
  id: string
  name: string
  price: number
  basePrice?: number
  serviceFee?: number
  totalPrice?: number
  currency: 'USD' | 'ARS'
  capacity?: number
  type: 'GENERAL_ADMISSION' | 'NUMBERED_SEATS' | 'VIP'
  description?: string
  status?: 'AVAILABLE' | 'FEW_SEATS' | 'SOLD_OUT'
}

export interface PaymentPromotion {
  id: string
  bank: string
  card: string
  installments: string
  description: string
  badgeText?: string
}

export interface EventItem {
  id: string
  slug?: string
  title: string
  subtitle: string
  date: string
  dateIso: string
  time?: string
  venue: string
  venueId: string
  location: string
  category: string
  genre?: 'Music & Concerts' | 'Tech Conferences' | 'Sports' | 'Theater' | string
  dateFilter?: 'this_weekend' | 'next_30_days' | 'future'
  price: number
  minPrice: number
  imageUrl: string
  badgeText?: string
  badgeType?: 'primary' | 'secondary' | 'warning' | 'error'
  status: 'AVAILABLE' | 'FEW_SEATS' | 'SOLD_OUT' | 'QUEUE_ACTIVE'
  description?: string
  aiSimilarityScore?: number
  lineup?: string
  doorsOpen?: string
  venueMapUrl?: string
  seatMapUrl?: string
  venueImageUrl?: string
  functions: EventFunction[]
  sectors: SectorPricing[]
  parking: VenueParking
  transit?: VenueTransit
  paymentPromotions: PaymentPromotion[]
  availableDates?: string[]
  ticketTypes?: { name: string; price: number }[]
  maxTicketsPerUser?: number
  serviceFeePercentage?: number
  salesPolicy?: {
    maxTicketsPerUser?: number
    waitingRoomEnabled?: boolean
    serviceFeePercentage?: number
  }
}
