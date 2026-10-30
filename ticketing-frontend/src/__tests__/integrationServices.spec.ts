import { describe, it, expect, beforeEach, vi } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { apiClient, ApiError } from '../services/apiClient'
import { catalogService } from '../services/catalogService'
import { inventoryService } from '../services/inventoryService'
import { useTicketingStore } from '../stores/ticketing'
import { useCatalogStore } from '../stores/catalog'
import type { CatalogEventResponse, EventSeatsResponse } from '../types'

describe('Frontend <-> Backend Microservices Integration Test Suite', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.restoreAllMocks()
  })

  describe('API Client & HTTP Invariants', () => {
    it('propagates Trace-ID and X-Trace-Id headers across all requests', async () => {
      const fetchSpy = vi.spyOn(globalThis, 'fetch').mockResolvedValueOnce(
        new Response(JSON.stringify({ status: 'OK' }), {
          status: 200,
          headers: { 'Content-Type': 'application/json' },
        })
      )

      await apiClient.get('/api/test')

      expect(fetchSpy).toHaveBeenCalledTimes(1)
      const callArgs = fetchSpy.mock.calls[0]!
      const headers = (callArgs[1]?.headers as Record<string, string>) || {}
      expect(headers['Trace-ID']).toMatch(/^trc-[0-9a-f]+-[0-9a-f]+$/)
      expect(headers['X-Trace-Id']).toBe(headers['Trace-ID'])
      expect(headers['Content-Type']).toBe('application/json')
    })

    it('parses RFC 7807 ProblemDetail on 409 Conflict', async () => {
      const problem = {
        type: 'urn:problem:seat-already-held',
        title: 'Seat Unavailable',
        status: 409,
        detail: 'Seat A-1 is currently held by another customer',
        code: 'SEAT_ALREADY_HELD',
      }

      vi.spyOn(globalThis, 'fetch').mockResolvedValueOnce(
        new Response(JSON.stringify(problem), {
          status: 409,
          headers: { 'Content-Type': 'application/problem+json' },
        })
      )

      try {
        await apiClient.post('/api/v1/reservations', { seatId: 'A-1' })
        expect.fail('Should have thrown an ApiError')
      } catch (err: any) {
        expect(err).toBeInstanceOf(ApiError)
        expect(err.status).toBe(409)
        expect(err.message).toBe('Seat A-1 is currently held by another customer')
        expect(err.problem?.code).toBe('SEAT_ALREADY_HELD')
      }
    })
  })

  describe('Catalog Service Integration', () => {
    it('hydrates and maps live CatalogEventResponse into EventItem', async () => {
      const mockBackendEvent: CatalogEventResponse = {
        id: 'evt_coldplay_2026',
        slug: 'coldplay-music-of-the-spheres-2026',
        title: 'Coldplay: Music of the Spheres',
        status: 'PUBLISHED',
        category: 'CONCERT',
        genres: ['Rock', 'Pop'],
        doorsOpenAt: '2026-11-06T18:00:00Z',
        startsAt: '2026-11-06T21:00:00Z',
        endsAt: '2026-11-06T23:30:00Z',
        timezone: 'America/Argentina/Buenos_Aires',
        venueName: 'Estadio Monumental (Buenos Aires)',
        minPrice: 38000,
        currency: 'ARS',
        bannerUrl: 'https://cdn.amaryllis.test/banner.jpg',
        posterUrl: 'https://cdn.amaryllis.test/poster.jpg',
        sectors: [
          {
            sectorId: 'sec_vip',
            name: 'Campo VIP',
            type: 'VIP',
            totalCapacity: 10000,
            basePrice: 140000,
            serviceFee: 16800,
            totalPrice: 156800,
            currency: 'ARS',
          },
        ],
      }

      vi.spyOn(globalThis, 'fetch').mockResolvedValueOnce(
        new Response(JSON.stringify([mockBackendEvent]), {
          status: 200,
          headers: { 'Content-Type': 'application/json' },
        })
      )

      const events = await catalogService.getEvents()
      expect(events.length).toBeGreaterThan(0)
      const coldplay = events.find(e => e.id === 'evt_coldplay_2026')
      expect(coldplay).toBeDefined()
      expect(coldplay?.title).toBe('Coldplay: Music of the Spheres')
      expect(coldplay?.category).toBe('Música & Conciertos')
      expect(coldplay?.minPrice).toBe(38000)
      expect(coldplay?.sectors?.[0]?.name).toBe('Campo VIP')
    })

    it('catalogStore.fetchEvents updates store state with live events', async () => {
      const catalogStore = useCatalogStore()

      vi.spyOn(catalogService, 'getEvents').mockResolvedValueOnce([
        {
          id: 'live_event_1',
          title: 'Live Festival 2026',
          subtitle: 'Live festival',
          date: '10 de Noviembre de 2026',
          dateIso: '2026-11-10T21:00:00',
          venue: 'Arena Buenos Aires (Buenos Aires)',
          venueId: 'arena-buenos-aires',
          location: 'Buenos Aires, Argentina',
          category: 'Música & Conciertos',
          price: 45000,
          minPrice: 45000,
          imageUrl: 'https://test.jpg',
          status: 'AVAILABLE',
          sectors: [],
          functions: [],
          parking: {} as any,
          paymentPromotions: [],
        },
      ])

      await catalogStore.fetchEvents()
      expect(catalogStore.isLoading).toBe(false)
      expect(catalogStore.events.some(e => e.id === 'live_event_1')).toBe(true)
    })
  })

  describe('Inventory Service & Redlock Integration', () => {
    it('fetches remote physical seats from inventory service', async () => {
      const mockSeatsPayload: EventSeatsResponse = {
        eventId: 'evt_coldplay_2026',
        totalSeats: 2,
        availableSeats: 1,
        heldSeats: 1,
        bookedSeats: 0,
        blockedSeats: 0,
        seats: [
          {
            seatId: 'seat_A1',
            eventId: 'evt_coldplay_2026',
            sectorId: 'platea-a',
            rowIdentifier: 'A',
            seatNumber: 1,
            price: 120000,
            currency: 'ARS',
            status: 'AVAILABLE',
          },
          {
            seatId: 'seat_A2',
            eventId: 'evt_coldplay_2026',
            sectorId: 'platea-a',
            rowIdentifier: 'A',
            seatNumber: 2,
            price: 120000,
            currency: 'ARS',
            status: 'HELD',
          },
        ],
      }

      vi.spyOn(globalThis, 'fetch').mockResolvedValueOnce(
        new Response(JSON.stringify(mockSeatsPayload), {
          status: 200,
          headers: { 'Content-Type': 'application/json' },
        })
      )

      const seats = await inventoryService.getSeats('evt_coldplay_2026')
      expect(seats).toHaveLength(2)
      expect(seats[0]?.id).toBe('seat_A1')
      expect(seats[0]?.status).toBe('AVAILABLE')
      expect(seats[1]?.id).toBe('seat_A2')
      expect(seats[1]?.status).toBe('RESERVED')
    })

    it('ticketingStore.toggleSeat holds seat via Redlock and starts 10-minute timer', async () => {
      const ticketingStore = useTicketingStore()
      ticketingStore.seats = [
        {
          id: 'seat_101',
          row: 'A',
          number: 1,
          sector: 'platea-a',
          price: 120000,
          status: 'AVAILABLE',
          x: 100,
          y: 100,
        },
      ]

      vi.spyOn(inventoryService, 'holdSeat').mockResolvedValueOnce({
        reservationId: 'res_001',
        eventId: 'evt_coldplay_2026',
        seatId: 'seat_101',
        userId: 'usr_guest_frontoffice',
        status: 'PENDING',
        expiresAt: new Date(Date.now() + 600000).toISOString(),
        countdownSeconds: 600,
      })

      await ticketingStore.toggleSeat('seat_101')

      expect(ticketingStore.selectedSeats).toHaveLength(1)
      expect(ticketingStore.selectedSeats[0]?.id).toBe('seat_101')
      expect(ticketingStore.isLockActive).toBe(true)
      expect(ticketingStore.lockTimeRemaining).toBe(600)
      expect(ticketingStore.activeHolds).toHaveLength(1)
      expect(ticketingStore.activeHolds[0]?.reservationId).toBe('res_001')
    })

    it('ticketingStore rolls back optimistic selection on 409 Conflict', async () => {
      const ticketingStore = useTicketingStore()
      ticketingStore.seats = [
        {
          id: 'seat_collision',
          row: 'A',
          number: 2,
          sector: 'platea-a',
          price: 120000,
          status: 'AVAILABLE',
          x: 100,
          y: 100,
        },
      ]

      vi.spyOn(inventoryService, 'holdSeat').mockRejectedValueOnce(
        new ApiError(409, 'La butaca fue bloqueada por otro usuario.', {
          status: 409,
          detail: 'El asiento seat_collision se encuentra reservado.',
          code: 'SEAT_ALREADY_HELD',
        })
      )

      await ticketingStore.toggleSeat('seat_collision')

      // Should be unselected and marked RESERVED
      expect(ticketingStore.selectedSeats).toHaveLength(0)
      expect(ticketingStore.seats[0]?.status).toBe('RESERVED')
      expect(ticketingStore.errorNotification).toBe('El asiento seat_collision se encuentra reservado.')
    })

    it('ticketingStore.confirmAllActiveReservations confirms with orderId', async () => {
      const ticketingStore = useTicketingStore()
      ticketingStore.activeHolds = [
        {
          reservationId: 'res_999',
          seatId: 'seat_1',
          eventId: 'evt_1',
          expiresAt: new Date().toISOString(),
        },
      ]

      const confirmSpy = vi.spyOn(inventoryService, 'confirmReservation').mockResolvedValueOnce({
        reservationId: 'res_999',
        eventId: 'evt_1',
        seatId: 'seat_1',
        status: 'CONFIRMED',
        orderId: 'ORD-12345',
        confirmedAt: new Date().toISOString(),
      })

      const ok = await ticketingStore.confirmAllActiveReservations('ORD-12345')
      expect(ok).toBe(true)
      expect(confirmSpy).toHaveBeenCalledWith('res_999', { orderId: 'ORD-12345' })
      expect(ticketingStore.activeHolds).toHaveLength(0)
    })
  })
})
