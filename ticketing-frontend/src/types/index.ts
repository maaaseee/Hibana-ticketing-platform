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
  currency: 'USD' | 'ARS'
  capacity?: number
  type: 'GENERAL_ADMISSION' | 'NUMBERED_SEATS' | 'VIP'
  description?: string
  status?: 'AVAILABLE' | 'FEW_SEATS' | 'SOLD_OUT'
}

export interface VenueParking {
  available: boolean
  title: string
  description: string
  price?: number
  accessRoutes: string[]
  bookingPassUrl?: string
}

export interface PaymentPromotion {
  id: string
  bank: string
  card: string
  installments: string
  description: string
  badgeText?: string
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
  parking: VenueParking
  paymentMethods: PaymentPromotion[]
  sectors: SectorPricing[]
}

export interface EventItem {
  id: string
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
  functions: EventFunction[]
  sectors: SectorPricing[]
  parking: VenueParking
  paymentPromotions: PaymentPromotion[]
  availableDates?: string[]
  ticketTypes?: { name: string; price: number }[]
}

export interface Seat {
  id: string
  row: string
  number: number
  sector: string
  price: number
  status: 'AVAILABLE' | 'SELECTED' | 'RESERVED' | 'LOCKED'
  x: number
  y: number
}

export interface Sector {
  id: string
  name: string
  price: number
  color: string
  availableCount: number
  totalCount: number
}

export interface Ticket {
  id: string
  orderId: string
  eventId: string
  eventTitle: string
  eventDate: string
  venue: string
  sector: string
  row: string
  seatNumber: number | string
  customerName: string
  customerEmail: string
  status: 'ISSUED' | 'REDEEMED' | 'REFUNDED' | 'TRANSFERRED'
  qrPayload: {
    tid: string
    eid: string
    vid: string
    sec: string
    sig: string
  }
  totpRemainingSeconds: number
  barcode: string
}

export interface ChatMessage {
  id: string
  sender: 'ai' | 'user' | 'operator' | 'system'
  senderName: string
  text: string
  timestamp: string
  isInternalNote?: boolean
  quickActions?: { label: string; action: string; icon?: string }[]
}
