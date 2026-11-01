import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { EventItem, VenueInfo } from '../types'
import { mockEvents } from '../data/mockEvents'
import { mockVenues, getVenueById as findVenueById, getVenueByName as findVenueByName } from '../data/mockVenues'
import { catalogService } from '../services/catalogService'

/**
 * Helper to compute the minimum price for an event based on its sector pricing.
 * Falls back to minPrice or price if sectors are empty.
 */
export function getMinEventPrice(event: EventItem): number {
  if (event.sectors && event.sectors.length > 0) {
    const validPrices = event.sectors
      .map(s => (s.basePrice !== undefined && s.basePrice !== null ? s.basePrice : s.price))
      .filter(p => typeof p === 'number' && !isNaN(p) && p > 0)
    if (validPrices.length > 0) {
      return Math.min(...validPrices)
    }
  }
  if (typeof event.minPrice === 'number' && !isNaN(event.minPrice) && event.minPrice > 0) {
    return event.minPrice
  }
  return event.price || 0
}

export const useCatalogStore = defineStore('catalog', () => {
  const events = ref<EventItem[]>(mockEvents)
  const venues = ref<VenueInfo[]>(mockVenues)
  const searchQuery = ref<string>('')
  const isLoading = ref<boolean>(false)
  const error = ref<string | null>(null)

  // Home Highlights Cache in SessionStorage
  const HIGHLIGHTS_STORAGE_KEY = 'hibana_highlights_cache'
  const HIGHLIGHTS_TTL_MS = import.meta.env.DEV ? 5000 : 10 * 60 * 1000 // 5s in dev, 10 min in prod

  const homeHighlights = ref<{ recentlyAdded: EventItem[]; upcoming: EventItem[] }>({
    recentlyAdded: [],
    upcoming: []
  })

  const furthestEventDate = ref<string | null>(null)

  async function fetchHomeHighlights(force = false): Promise<void> {
    if (!force) {
      try {
        const cached = sessionStorage.getItem(HIGHLIGHTS_STORAGE_KEY)
        if (cached) {
          const parsed = JSON.parse(cached)
          if (Date.now() - parsed.timestamp < HIGHLIGHTS_TTL_MS && parsed.recentlyAdded?.length && parsed.upcoming?.length) {
            homeHighlights.value = {
              recentlyAdded: parsed.recentlyAdded,
              upcoming: parsed.upcoming
            }
            return
          }
        }
      } catch {
        // Fallback to fetch
      }
    }

    try {
      const data = await catalogService.getHomeHighlights()
      homeHighlights.value = data
      try {
        sessionStorage.setItem(HIGHLIGHTS_STORAGE_KEY, JSON.stringify({
          recentlyAdded: data.recentlyAdded,
          upcoming: data.upcoming,
          timestamp: Date.now()
        }))
      } catch {
        // Ignore quota
      }
    } catch {
      // Keep fallbacks
    }
  }

  async function fetchCatalogMetadata(): Promise<void> {
    try {
      const meta = await catalogService.getCatalogMetadata()
      furthestEventDate.value = meta.furthestEventDate
    } catch {
      // Ignore
    }
  }

  async function searchCatalog(query?: string, venue?: string): Promise<EventItem[]> {
    isLoading.value = true
    try {
      const results = await catalogService.searchEvents(query, venue)
      if (results && results.length > 0) {
        events.value = results
      }
      return results
    } catch (err: unknown) {
      error.value = err instanceof Error ? err.message : 'Error en la búsqueda'
      return []
    } finally {
      isLoading.value = false
    }
  }

  async function fetchEvents(): Promise<void> {
    isLoading.value = true
    error.value = null
    try {
      const data = await catalogService.getEvents()
      if (data && data.length > 0) {
        events.value = data
      }
    } catch (err: unknown) {
      error.value = err instanceof Error ? err.message : 'Error al sincronizar catálogo'
    } finally {
      isLoading.value = false
    }
  }

  // Dynamic available months from current month up to furthest event date
  const SPANISH_MONTHS = [
    'Enero', 'Febrero', 'Marzo', 'Abril', 'Mayo', 'Junio',
    'Julio', 'Agosto', 'Septiembre', 'Octubre', 'Noviembre', 'Diciembre'
  ]

  const availableMonths = computed(() => {
    // Current reference navigation date (e.g. Octubre 2026)
    const baseYear = 2026
    const baseMonth = 9 // October (0-indexed)

    // Default up to Mayo 2027 (concierto más lejano en catálogo base)
    let endYear = 2027
    let endMonth = 4 // Mayo 2027 (0-indexed 4)

    // Dynamically adjust if furthestEventDate is provided by backend
    const targetIso = furthestEventDate.value
    if (targetIso) {
      const d = new Date(targetIso)
      if (!isNaN(d.getTime())) {
        endYear = d.getFullYear()
        endMonth = d.getMonth()
      }
    }

    const totalDiff = Math.max(1, (endYear - baseYear) * 12 + (endMonth - baseMonth) + 1)
    const list: { label: string; value: string }[] = []

    for (let i = 0; i < totalDiff; i++) {
      const totalMonths = baseMonth + i
      const year = baseYear + Math.floor(totalMonths / 12)
      const monthIndex = totalMonths % 12
      const monthName = SPANISH_MONTHS[monthIndex]
      const label = `${monthName} ${year}`
      list.push({ label, value: label })
    }

    return list
  })

  async function fetchEventById(id: string): Promise<EventItem | undefined> {
    try {
      const liveEvent = await catalogService.getEventById(id)
      if (liveEvent) {
        const existingIdx = events.value.findIndex(e => e.id === liveEvent.id)
        if (existingIdx !== -1) {
          events.value[existingIdx] = liveEvent
        } else {
          events.value.push(liveEvent)
        }
        return liveEvent
      }
    } catch {
      // Fallback
    }
    return getEventById(id)
  }

  // Featured Slides for Carousel
  const featuredEvents = computed(() => {
    const featuredIds = [
      'coldplay-music-spheres',
      'chronos-fest-2026',
      'symphony-of-lights',
      'global-tech-summit'
    ]
    return featuredIds
      .map(id => getEventById(id))
      .filter(Boolean) as EventItem[]
  })

  // 1. Shows agregados recientemente (Ordenados por fecha de publicación/novedad)
  const recentlyAddedEvents = computed(() => {
    const priorityIds = [
      'cloud-native-devcon',
      'opera-clasica-verano',
      'chronos-fest-2026',
      'urban-trap-festival',
      'global-tech-summit',
      'indie-rock-sessions',
      'electronica-sunset',
      'symphony-of-lights',
      'river-stadium-rockfest',
      'coldplay-music-spheres'
    ]
    const prioritized = priorityIds
      .map(id => getEventById(id))
      .filter(Boolean) as EventItem[]

    const remaining = events.value.filter(e => !priorityIds.includes(e.id))
    return [...prioritized, ...remaining]
  })

  // 2. Shows próximos a ocurrir (Orden cronológico estricto por dateIso)
  const upcomingEvents = computed(() => {
    return [...events.value].sort(
      (a, b) => new Date(a.dateIso).getTime() - new Date(b.dateIso).getTime()
    )
  })

  function getEventById(id: string): EventItem | undefined {
    if (!id) return undefined
    const direct = events.value.find(e => e.id === id || e.slug === id)
    if (direct) return direct

    // Aliases for backwards compatibility with legacy routes/ids
    const aliasMap: Record<string, string> = {
      'championship-finals': 'river-stadium-rockfest',
      'symphony-in-code': 'opera-clasica-verano',
      'electronica-open-air': 'electronica-sunset',
      'neon-nights-techno': 'chronos-fest-2026',
      'design-systems-workshop': 'global-tech-summit',
      'evt_ado_2026': events.value.some(e => e.id === 'ado-hibana-world-tour') ? 'ado-hibana-world-tour' : 'symphony-of-lights',
      'evt-stellar-2026': events.value.some(e => e.id === 'ado-hibana-world-tour') ? 'ado-hibana-world-tour' : 'symphony-of-lights'
    }

    const mappedId = aliasMap[id]
    if (mappedId) {
      return events.value.find(e => e.id === mappedId || e.slug === mappedId)
    }

    return undefined
  }

  function getVenueById(id: string): VenueInfo | undefined {
    return venues.value.find(v => v.id === id) || findVenueById(id)
  }

  function getVenueByName(name: string): VenueInfo | undefined {
    return venues.value.find(v => v.name === name || v.shortName === name) || findVenueByName(name)
  }

  return {
    events,
    venues,
    searchQuery,
    isLoading,
    error,
    featuredEvents,
    recentlyAddedEvents,
    upcomingEvents,
    homeHighlights,
    furthestEventDate,
    availableMonths,
    fetchHomeHighlights,
    fetchCatalogMetadata,
    searchCatalog,
    fetchEvents,
    fetchEventById,
    getEventById,
    getVenueById,
    getVenueByName,
    getMinEventPrice
  }
})
