import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { EventItem, VenueInfo } from '../types'
import { mockEvents } from '../data/mockEvents'
import { mockVenues, getVenueById as findVenueById, getVenueByName as findVenueByName } from '../data/mockVenues'

/**
 * Helper to compute the minimum price for an event based on its sector pricing.
 * Falls back to minPrice or price if sectors are empty.
 */
export function getMinEventPrice(event: EventItem): number {
  if (event.sectors && event.sectors.length > 0) {
    const validPrices = event.sectors
      .map(s => s.price)
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
      'coldplay-music-spheres',
      'river-stadium-rockfest',
      'chronos-fest-2026',
      'symphony-of-lights',
      'urban-trap-festival',
      'global-tech-summit',
      'indie-rock-sessions',
      'opera-clasica-verano',
      'cloud-native-devcon',
      'electronica-sunset'
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
    const direct = events.value.find(e => e.id === id)
    if (direct) return direct

    // Aliases for backwards compatibility with legacy routes/ids
    const aliasMap: Record<string, string> = {
      'championship-finals': 'river-stadium-rockfest',
      'symphony-in-code': 'opera-clasica-verano',
      'electronica-open-air': 'electronica-sunset',
      'neon-nights-techno': 'chronos-fest-2026',
      'design-systems-workshop': 'global-tech-summit'
    }

    const mappedId = aliasMap[id]
    if (mappedId) {
      return events.value.find(e => e.id === mappedId)
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
    featuredEvents,
    recentlyAddedEvents,
    upcomingEvents,
    getEventById,
    getVenueById,
    getVenueByName,
    getMinEventPrice
  }
})
