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
