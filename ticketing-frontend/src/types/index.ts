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
  parking: VenueParking
  transit?: VenueTransit
  paymentMethods: PaymentPromotion[]
  sectors: SectorPricing[]
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
  functions: EventFunction[]
  sectors: SectorPricing[]
  parking: VenueParking
  transit?: VenueTransit
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

// Parametric Seat Map Layout Interfaces (from /seat-maps/*.json)
export type SectorShapeType = 'GRID' | 'ARC' | 'POLYGON' | 'PATH'

export interface BaseSectorGeometry {
  id: string
  name: string
  type: 'VIP' | 'NUMBERED_SEATS' | 'GENERAL_ADMISSION' | 'TECHNICAL'
  shapeType: SectorShapeType
  color: string
  strokeColor: string
  capacity: number
  labelPosition?: [number, number]
  vendible?: boolean
  selectable?: boolean
}

export interface GridSectorLayout extends BaseSectorGeometry {
  shapeType: 'GRID'
  x: number
  y: number
  width: number
  height: number
  rows: number
  seatsPerRow: number
  rowLabels?: string[]
}

export interface ArcSectorLayout extends BaseSectorGeometry {
  shapeType: 'ARC'
  centerX: number
  centerY: number
  innerRadius: number
  outerRadius: number
  startAngleDeg: number
  endAngleDeg: number
  rows?: number
  seatsPerRow?: number
  rowLabels?: string[]
}

export interface PolygonSectorLayout extends BaseSectorGeometry {
  shapeType: 'POLYGON'
  points: [number, number][]
  labelPosition: [number, number]
  standingArea?: boolean
}

export interface PathSectorLayout extends BaseSectorGeometry {
  shapeType: 'PATH'
  pathData: string
  labelPosition: [number, number]
  standingArea?: boolean
}

export type SectorGeometry = GridSectorLayout | ArcSectorLayout | PolygonSectorLayout | PathSectorLayout

export interface StageGeometry {
  x: number
  y: number
  width: number
  height: number
  label: string
  shape: 'RECTANGLE' | 'THRUST'
}

export interface VenueSeatMapLayout {
  venueId: string
  venueName: string
  venueType: 'ESTADIO_MASIVO' | 'ARENA_CUBIERTO' | 'TEATRO_HISTORICO'
  canvasWidth: number
  canvasHeight: number
  viewBox: { minX: number; minY: number; maxX: number; maxY: number }
  stage: StageGeometry
  sectors: SectorGeometry[]
}

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

