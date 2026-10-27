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
