export interface VenueResponse {
  id: string
  name: string
  city: string
  country: string
  address: string
  latitude?: number
  longitude?: number
  seatMapUrl?: string
  imageUrl?: string
  totalCapacity?: number
  status?: string
}
