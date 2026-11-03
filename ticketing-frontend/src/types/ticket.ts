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
  customerDocument?: string
  documentNumber?: string
  dni?: string
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
  price?: number
  serviceCharge?: number
  doorsOpen?: string
  showTime?: string
  imageUrl?: string
  redeemedDate?: string
}
