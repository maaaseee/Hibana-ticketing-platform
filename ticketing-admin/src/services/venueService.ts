import type { VenueResponse } from '@/types/venue'
import { apiClient } from './api'

interface CatalogMetadataResponse {
  furthestEventDate: string
  venues: VenueResponse[]
}

export const KNOWN_VENUE_CAPACITIES: Record<string, number> = {
  'estadio-monumental': 84567,
  'ven_estadio_nacional': 48665,
  'arena-buenos-aires': 15000,
  'teatro-rex': 3262,
  '00000000-0000-0000-0000-000000000001': 15000,
  '00000000-0000-0000-0000-000000000002': 84567,
  '00000000-0000-0000-0000-000000000003': 3262,
  '00000000-0000-0000-0000-000000000004': 9290,
}

export function resolveVenueCapacity(venue: Partial<VenueResponse>): number {
  if (venue.totalCapacity && venue.totalCapacity > 0) return venue.totalCapacity
  if (venue.id && KNOWN_VENUE_CAPACITIES[venue.id]) return KNOWN_VENUE_CAPACITIES[venue.id]
  const idOrName = `${venue.id || ''} ${venue.name || ''}`.toLowerCase()
  if (idOrName.includes('monumental') || idOrName.includes('river')) return 84567
  if (idOrName.includes('nacional')) return 48665
  if (idOrName.includes('rex')) return 3262
  if (idOrName.includes('luna')) return 9290
  if (idOrName.includes('arena')) return 15000
  return 15000
}

const DEFAULT_VENUES: VenueResponse[] = [
  {
    id: 'arena-buenos-aires',
    name: 'Arena Buenos Aires (Movistar Arena)',
    city: 'Buenos Aires',
    country: 'Argentina',
    address: 'Humboldt 450, Villa Crespo',
    totalCapacity: 15000,
    status: 'ACTIVE',
  },
  {
    id: 'estadio-monumental',
    name: 'Estadio River Plate (Mâs Monumental)',
    city: 'Buenos Aires',
    country: 'Argentina',
    address: 'Av. Pres. Figueroa Alcorta 7597, Nuñez',
    totalCapacity: 84567,
    status: 'ACTIVE',
  },
  {
    id: 'teatro-rex',
    name: 'Teatro Rex (Gran Rex)',
    city: 'Buenos Aires',
    country: 'Argentina',
    address: 'Av. Corrientes 857, San Nicolás',
    totalCapacity: 3262,
    status: 'ACTIVE',
  },
  {
    id: 'ven_estadio_nacional',
    name: 'Estadio Nacional Julio Martínez Prádanos',
    city: 'Santiago',
    country: 'Chile',
    address: 'Av. Grecia 2001, Ñuñoa',
    totalCapacity: 48665,
    status: 'ACTIVE',
  },
]

export const venueService = {
  async getVenues(): Promise<VenueResponse[]> {
    try {
      const meta = await apiClient<CatalogMetadataResponse>('/api/v1/events/meta')
      if (meta && meta.venues && meta.venues.length > 0) {
        return meta.venues.map((v) => ({
          ...v,
          totalCapacity: resolveVenueCapacity(v),
        }))
      }
    } catch (err) {
      console.warn('Could not load venues from /api/v1/events/meta, using fallback list:', err)
    }
    return DEFAULT_VENUES
  },
}
