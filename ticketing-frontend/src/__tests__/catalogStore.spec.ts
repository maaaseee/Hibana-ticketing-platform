import { describe, it, expect, beforeEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useCatalogStore, getMinEventPrice } from '../stores/catalog'
import { mockVenues, getVenueById, getVenueByName } from '../data/mockVenues'
import { mockEvents } from '../data/mockEvents'

describe('catalogStore.spec.ts - Store & Data Invariants', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  describe('Canonical Venues (mockVenues.ts)', () => {
    it('defines exactly the 3 designated Buenos Aires venues', () => {
      expect(mockVenues).toHaveLength(3)
      const venueNames = mockVenues.map(v => v.name)
      expect(venueNames).toContain('Estadio Monumental (Buenos Aires)')
      expect(venueNames).toContain('Arena Buenos Aires (Buenos Aires)')
      expect(venueNames).toContain('Teatro Rex (Buenos Aires)')
    })

    it('ensures 100% of venues are situated exclusively in Buenos Aires, Argentina', () => {
      mockVenues.forEach(venue => {
        expect(venue.city).toBe('Buenos Aires')
        expect(venue.country).toBe('Argentina')
        expect(venue.address).toBeTruthy()
        expect(venue.capacity).toBeGreaterThan(0)
      })
    })

    it('ensures zero Chilean cities, venues, or references exist in mockVenues', () => {
      mockVenues.forEach(venue => {
        const fullVenueStr = JSON.stringify(venue).toLowerCase()
        expect(fullVenueStr).not.toContain('santiago')
        expect(fullVenueStr).not.toContain('chile')
        expect(fullVenueStr).not.toContain('caupolicán')
        expect(fullVenueStr).not.toContain('bicentenario')
      })
    })

    it('ensures each venue has parking, payment methods, and sector structures', () => {
      mockVenues.forEach(venue => {
        expect(venue.parking).toBeDefined()
        expect(venue.parking.title).toBeTruthy()
        expect(venue.parking.accessRoutes.length).toBeGreaterThan(0)
        expect(venue.paymentMethods.length).toBeGreaterThan(0)
        expect(venue.sectors.length).toBeGreaterThan(0)
      })
    })

    it('finds venues by ID and Name', () => {
      const monumental = getVenueById('estadio-monumental')
      expect(monumental).toBeDefined()
      expect(monumental?.name).toBe('Estadio Monumental (Buenos Aires)')

      const arena = getVenueByName('Arena Buenos Aires (Buenos Aires)')
      expect(arena).toBeDefined()
      expect(arena?.id).toBe('arena-buenos-aires')

      const rex = getVenueByName('Teatro Rex')
      expect(rex).toBeDefined()
      expect(rex?.id).toBe('teatro-rex')
    })
  })

  describe('Events Fixture (mockEvents.ts)', () => {
    it('contains exactly 10 cohesive events situated exclusively in Buenos Aires', () => {
      expect(mockEvents).toHaveLength(10)
      mockEvents.forEach(event => {
        expect(event.location).toBe('Buenos Aires, Argentina')
        expect(['estadio-monumental', 'arena-buenos-aires', 'teatro-rex']).toContain(event.venueId)
        expect([
          'Estadio Monumental (Buenos Aires)',
          'Arena Buenos Aires (Buenos Aires)',
          'Teatro Rex (Buenos Aires)'
        ]).toContain(event.venue)
      })
    })

    it('contains zero Chilean cities, venues, or locations in mockEvents', () => {
      mockEvents.forEach(event => {
        const eventStr = JSON.stringify(event).toLowerCase()
        expect(eventStr).not.toContain('santiago')
        expect(eventStr).not.toContain('chile')
        expect(eventStr).not.toContain('movistar arena')
        expect(eventStr).not.toContain('caupolicán')
        expect(eventStr).not.toContain('bicentenario')
      })
    })

    it('ensures every event has structured functions with doorsOpen, showStart, and dateIso', () => {
      mockEvents.forEach(event => {
        expect(event.functions.length).toBeGreaterThan(0)
        event.functions.forEach(fn => {
          expect(fn.id).toBeTruthy()
          expect(fn.dateIso).toMatch(/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}/)
          expect(fn.formattedDate).toBeTruthy()
          expect(fn.doorsOpen).toBeTruthy()
          expect(fn.showStart).toBeTruthy()
          expect(['AVAILABLE', 'FEW_SEATS', 'SOLD_OUT', 'QUEUE_ACTIVE']).toContain(fn.status)
        })
      })
    })

    it('ensures every event has sectors and minPrice matching sector minimum', () => {
      mockEvents.forEach(event => {
        expect(event.sectors.length).toBeGreaterThan(0)
        const expectedMin = Math.min(...event.sectors.map(s => s.price))
        expect(event.minPrice).toBe(expectedMin)
        expect(event.price).toBe(expectedMin)
      })
    })

    it('maintains backwards-compatible fields', () => {
      mockEvents.forEach(event => {
        expect(Array.isArray(event.availableDates)).toBe(true)
        expect(event.availableDates!.length).toBeGreaterThan(0)
        expect(Array.isArray(event.ticketTypes)).toBe(true)
        expect(event.ticketTypes!.length).toBeGreaterThan(0)
      })
    })
  })

  describe('Catalog Store (catalog.ts)', () => {
    it('getMinEventPrice calculates the minimum sector price', () => {
      const sampleEvent = mockEvents[0]!
      const minPrice = getMinEventPrice(sampleEvent)
      expect(minPrice).toBe(35)
    })

    it('getMinEventPrice falls back gracefully to minPrice if sectors are empty', () => {
      const fallbackEvent = {
        ...mockEvents[0]!,
        sectors: [],
        minPrice: 55,
        price: 60
      }
      expect(getMinEventPrice(fallbackEvent)).toBe(55)
    })

    it('getMinEventPrice falls back to price if sectors and minPrice are not available', () => {
      const fallbackEvent = {
        ...mockEvents[0]!,
        sectors: [],
        minPrice: 0,
        price: 42
      }
      expect(getMinEventPrice(fallbackEvent)).toBe(42)
    })

    it('sorts upcomingEvents chronologically by dateIso ascending', () => {
      const store = useCatalogStore()
      const upcoming = store.upcomingEvents
      expect(upcoming).toHaveLength(10)

      for (let i = 0; i < upcoming.length - 1; i++) {
        const current = new Date(upcoming[i]!.dateIso).getTime()
        const next = new Date(upcoming[i + 1]!.dateIso).getTime()
        expect(current).toBeLessThanOrEqual(next)
      }
    })

    it('provides recentlyAddedEvents containing newly added events with priority shows at top', () => {
      const store = useCatalogStore()
      const recent = store.recentlyAddedEvents
      expect(recent).toHaveLength(10)
      expect(recent[0]!.id).toBe('coldplay-music-spheres')
      expect(recent[1]!.id).toBe('river-stadium-rockfest')
      expect(recent[2]!.id).toBe('chronos-fest-2026')
    })

    it('resolves events by primary canonical id and backwards-compatible aliases', () => {
      const store = useCatalogStore()
      expect(store.getEventById('coldplay-music-spheres')).toBeDefined()
      expect(store.getEventById('chronos-fest-2026')).toBeDefined()

      // Backwards-compatible aliases
      expect(store.getEventById('championship-finals')?.id).toBe('river-stadium-rockfest')
      expect(store.getEventById('symphony-in-code')?.id).toBe('opera-clasica-verano')
      expect(store.getEventById('electronica-open-air')?.id).toBe('electronica-sunset')
      expect(store.getEventById('neon-nights-techno')?.id).toBe('chronos-fest-2026')
      expect(store.getEventById('design-systems-workshop')?.id).toBe('global-tech-summit')

      // Non-existent IDs
      expect(store.getEventById('non-existent-event-999')).toBeUndefined()
      expect(store.getEventById('')).toBeUndefined()
    })
  })
})
