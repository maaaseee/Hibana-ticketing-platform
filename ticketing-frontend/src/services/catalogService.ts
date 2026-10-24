import { apiClient } from './apiClient'
import type { CatalogEventResponse, EventItem, SectorPricing } from '../types'
import { mockEvents } from '../data/mockEvents'
import { getVenueByName, mockVenues } from '../data/mockVenues'

const BASE_URL = import.meta.env.VITE_CATALOG_API_URL || '/api/catalog'

function mapCatalogToEventItem(backend: CatalogEventResponse): EventItem {
  const defaultVenue = mockVenues[0]!
  const matchedVenue = backend.venueName ? getVenueByName(backend.venueName) : defaultVenue
  const venueId = matchedVenue?.id || 'arena-buenos-aires'
  const venueName = backend.venueName || matchedVenue?.name || 'Buenos Aires Venue'

  const sectors: SectorPricing[] = backend.sectors?.map((s, idx) => {
    const cleanId = s.sectorId?.includes('__') ? s.sectorId.split('__')[1]! : (s.sectorId || `sec-${idx}`)
    return {
      id: cleanId,
      name: s.name,
      price: s.totalPrice || s.basePrice || 0,
      currency: (s.currency === 'USD' ? 'USD' : 'ARS'),
      capacity: s.totalCapacity,
      type: (s.type === 'VIP' ? 'VIP' : s.type === 'NUMBERED_SEATS' ? 'NUMBERED_SEATS' : 'GENERAL_ADMISSION'),
      status: 'AVAILABLE',
    }
  }) || matchedVenue?.sectors || []

  const startsAtDate = backend.startsAt ? new Date(backend.startsAt) : new Date()
  const formattedDate = startsAtDate.toLocaleDateString('es-AR', {
    weekday: 'long',
    day: '2-digit',
    month: 'long',
    year: 'numeric',
  })
  const timeFormatted = startsAtDate.toLocaleTimeString('es-AR', {
    hour: '2-digit',
    minute: '2-digit',
  }) + ' HRS'

  let functions: EventFunction[] = (backend as any).functions || []
  if (functions.length === 0 && backend.salesPolicyJson) {
    try {
      const sp = typeof backend.salesPolicyJson === 'string' ? JSON.parse(backend.salesPolicyJson) : backend.salesPolicyJson
      if (Array.isArray(sp.annexedDates) && sp.annexedDates.length > 0) {
        functions = sp.annexedDates
      }
    } catch {}
  }
  if (functions.length === 0) {
    functions = [
      {
        id: `fn-${backend.id}-1`,
        dateIso: backend.startsAt || new Date().toISOString(),
        formattedDate,
        doorsOpen: backend.doorsOpenAt ? new Date(backend.doorsOpenAt).toLocaleTimeString('es-AR', { hour: '2-digit', minute: '2-digit' }) + ' hs' : '18:00 hs',
        showStart: timeFormatted,
        status: 'AVAILABLE'
      }
    ]
  }

  const availableDates = functions.map(f => `${f.formattedDate} - ${f.showStart}`)

  return {
    id: backend.id || backend.slug,
    slug: backend.slug,
    title: backend.title,
    subtitle: `${backend.category} • ${backend.genres?.join(', ') || 'En vivo'}`,
    description: backend.description,
    lineup: backend.genres && backend.genres.length > 0 ? backend.genres.join(' • ') : undefined,
    date: formattedDate,
    dateIso: backend.startsAt || new Date().toISOString(),
    time: timeFormatted,
    doorsOpen: backend.doorsOpenAt ? new Date(backend.doorsOpenAt).toLocaleTimeString('es-AR', { hour: '2-digit', minute: '2-digit' }) + ' HRS' : '18:00 HRS',
    venue: venueName,
    venueId,
    location: backend.venueAddress ? `${backend.venueCity || 'Buenos Aires'}, ${backend.venueAddress}` : 'Buenos Aires, Argentina',
    category: backend.category === 'CONCERT' ? 'Música & Conciertos' : backend.category,
    genre: 'Music & Concerts',
    price: backend.minPrice || 45000,
    minPrice: backend.minPrice || 45000,
    imageUrl: backend.posterUrl || backend.bannerUrl || 'https://images.unsplash.com/photo-1465847899084-d164df4dedc6?auto=format&fit=crop&w=1200&q=80',
    badgeText: 'Confirmado',
    badgeType: 'secondary',
    status: 'AVAILABLE',
    seatMapUrl: backend.seatMapUrl || `/seat-maps/${venueId}.json`,
    venueMapUrl: backend.seatMapUrl || `/seat-maps/${venueId}.json`,
    sectors,
    functions,
    availableDates,
    parking: matchedVenue?.parking || defaultVenue.parking,
    transit: matchedVenue?.transit || defaultVenue.transit,
    paymentPromotions: matchedVenue?.paymentMethods || defaultVenue.paymentMethods,
  }
}

export interface HomeHighlightsData {
  recentlyAdded: EventItem[]
  upcoming: EventItem[]
}

export interface CatalogMetadataData {
  furthestEventDate: string | null
  venues: { id: string; name: string }[]
}

export const catalogService = {
  async getEvents(): Promise<EventItem[]> {
    try {
      const data = await apiClient.get<CatalogEventResponse[]>(`${BASE_URL}/events`)
      if (Array.isArray(data) && data.length > 0) {
        const mapped = data.map(mapCatalogToEventItem)
        // Combine with mock events if backend only has partial seed events
        const backendIds = new Set(mapped.map(e => e.id))
        const nonDuplicateMocks = mockEvents.filter(e => !backendIds.has(e.id))
        return [...mapped, ...nonDuplicateMocks]
      }
      return mockEvents
    } catch {
      // Graceful fallback to mockEvents if catalog backend is offline
      return mockEvents
    }
  },

  async getHomeHighlights(referenceDate?: string): Promise<HomeHighlightsData> {
    try {
      const url = referenceDate
        ? `${BASE_URL}/events/highlights?referenceDate=${encodeURIComponent(referenceDate)}`
        : `${BASE_URL}/events/highlights`
      const data = await apiClient.get<{ recentlyAdded: CatalogEventResponse[]; upcoming: CatalogEventResponse[] }>(url)
      if (data && (data.recentlyAdded?.length > 0 || data.upcoming?.length > 0)) {
        return {
          recentlyAdded: data.recentlyAdded.map(mapCatalogToEventItem),
          upcoming: data.upcoming.map(mapCatalogToEventItem)
        }
      }
    } catch {
      // Fallback below
    }

    // Local fallback: top 3 recently added & top 3 upcoming
    const sortedUpcoming = [...mockEvents].sort(
      (a, b) => new Date(a.dateIso).getTime() - new Date(b.dateIso).getTime()
    )
    return {
      recentlyAdded: mockEvents.slice(0, 3),
      upcoming: sortedUpcoming.slice(0, 3)
    }
  },

  async getCatalogMetadata(): Promise<CatalogMetadataData> {
    try {
      const data = await apiClient.get<{ furthestEventDate: string; venues: { id: string; name: string }[] }>(`${BASE_URL}/events/meta`)
      if (data) {
        return {
          furthestEventDate: data.furthestEventDate || null,
          venues: data.venues || []
        }
      }
    } catch {
      // Fallback below
    }

    // Local fallback: calculate furthest date from mockEvents
    const maxTime = Math.max(...mockEvents.map(e => new Date(e.dateIso).getTime()))
    return {
      furthestEventDate: isNaN(maxTime) ? null : new Date(maxTime).toISOString(),
      venues: mockVenues.map(v => ({ id: v.id, name: v.name }))
    }
  },

  async searchEvents(query?: string, venue?: string): Promise<EventItem[]> {
    try {
      const params = new URLSearchParams()
      if (query && query.trim()) params.append('q', query.trim())
      if (venue && venue.trim() && venue !== 'all') params.append('venue', venue.trim())

      const url = params.toString() ? `${BASE_URL}/events/search?${params.toString()}` : `${BASE_URL}/events`
      const data = await apiClient.get<CatalogEventResponse[]>(url)
      if (Array.isArray(data) && data.length > 0) {
        return data.map(mapCatalogToEventItem)
      }
    } catch {
      // Fallback to local filter
    }

    const q = (query || '').toLowerCase().trim()
    const v = (venue || '').toLowerCase().trim()

    return mockEvents.filter(e => {
      if (v && v !== 'all') {
        const venueMatch = e.venue.toLowerCase().includes(v) || e.venueId.toLowerCase().includes(v)
        if (!venueMatch) return false
      }
      if (q) {
        const titleMatch = e.title.toLowerCase().includes(q)
        const subtitleMatch = e.subtitle ? e.subtitle.toLowerCase().includes(q) : false
        const lineupMatch = e.lineup ? e.lineup.toLowerCase().includes(q) : false
        if (!titleMatch && !subtitleMatch && !lineupMatch) return false
      }
      return true
    })
  },

  async getEventById(id: string): Promise<EventItem | undefined> {
    try {
      const data = await apiClient.get<CatalogEventResponse>(`${BASE_URL}/events/${id}`)
      if (data && data.id) {
        return mapCatalogToEventItem(data)
      }
    } catch {
      // Fallback
    }
    return mockEvents.find(e => e.id === id)
  },
}
