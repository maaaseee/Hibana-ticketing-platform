import { describe, it, expect, beforeEach, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { createRouter, createMemoryHistory } from 'vue-router'
import { setActivePinia, createPinia } from 'pinia'
import EventDetailView from '../views/EventDetailView.vue'
import { useCatalogStore, getMinEventPrice } from '../stores/catalog'
import { useQueueStore } from '../stores/queue'

function setupTestEnvironment(initialRoute = '/event/symphony-of-lights') {
  const pinia = createPinia()
  setActivePinia(pinia)

  const router = createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/event/:id', name: 'event-detail', component: EventDetailView },
      { path: '/seat-selection/:id', name: 'seat-selection', component: { template: '<div>Seat Selection</div>' } },
      { path: '/queue/:id', name: 'queue', component: { template: '<div>Queue</div>' } }
    ]
  })

  router.push(initialRoute)

  return { pinia, router }
}

describe('EventDetailView.spec.ts - Event Detail View Overhaul', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  describe('Anti-Pattern Assertions', () => {
    it('verifies 0 occurrences of select dropdowns (<select> or SelectRoot) across the view', async () => {
      const { pinia, router } = setupTestEnvironment('/event/symphony-of-lights')
      await router.isReady()

      const wrapper = mount(EventDetailView, {
        global: {
          plugins: [pinia, router]
        }
      })

      // Strict anti-pattern check: zero <select> elements in the rendered DOM
      const selects = wrapper.findAll('select')
      expect(selects).toHaveLength(0)

      // Ensure no SelectRoot, SelectTrigger or legacy dropdown markup
      expect(wrapper.find('[data-radix-select-trigger]').exists()).toBe(false)
      expect(wrapper.find('.select-trigger').exists()).toBe(false)
    })
  })

  describe('Section A: Base Pricing Headline Banner', () => {
    it('renders the prominent min price badge in the banner displaying "Entradas desde" with numeric min price and currency', async () => {
      const { pinia, router } = setupTestEnvironment('/event/symphony-of-lights')
      await router.isReady()

      const wrapper = mount(EventDetailView, {
        global: {
          plugins: [pinia, router]
        }
      })

      const catalog = useCatalogStore()
      const event = catalog.getEventById('symphony-of-lights')!
      const minPrice = getMinEventPrice(event)

      const bannerBadge = wrapper.find('[data-testid="banner-min-price-badge"]')
      expect(bannerBadge.exists()).toBe(true)
      expect(bannerBadge.text()).toContain('Entradas desde')
      expect(bannerBadge.text()).toContain(`$${minPrice}`)
      expect(bannerBadge.text()).toContain('ARS')

      // Verify inline badge in title area has been eliminated as requested
      const inlineBadge = wrapper.find('[data-testid="inline-min-price-badge"]')
      expect(inlineBadge.exists()).toBe(false)
    })

    it('renders exterior venue photo for Arena Buenos Aires instead of schematic map thumbnail', async () => {
      const { pinia, router } = setupTestEnvironment('/event/symphony-of-lights')
      await router.isReady()

      const wrapper = mount(EventDetailView, {
        global: {
          plugins: [pinia, router]
        }
      })

      const exteriorImage = wrapper.find('[data-testid="venue-exterior-image"]')
      expect(exteriorImage.exists()).toBe(true)
      const img = exteriorImage.find('img')
      expect(img.attributes('src')).toContain('arena_buenos_aires.jpg')
    })
  })

  describe('Section B: Available Dates & Functions List', () => {
    it('renders structured function rows with doorsOpen, showStart, status badge, and direct Comprar button', async () => {
      const { pinia, router } = setupTestEnvironment('/event/symphony-of-lights')
      await router.isReady()

      const wrapper = mount(EventDetailView, {
        global: {
          plugins: [pinia, router]
        }
      })

      const catalog = useCatalogStore()
      const event = catalog.getEventById('symphony-of-lights')!
      const functionRows = wrapper.findAll('[data-testid="function-row"]')

      expect(functionRows).toHaveLength(event.functions.length)

      event.functions.forEach((fn, idx) => {
        const row = functionRows[idx]!
        expect(row.text()).toContain(fn.formattedDate)
        expect(row.text()).toContain(`Apertura de puertas: ${fn.doorsOpen}`)
        expect(row.text()).toContain(`Inicio del show: ${fn.showStart}`)
        expect(row.text()).toContain('Disponible')

        const buyButton = row.find('button')
        expect(buyButton.exists()).toBe(true)
        expect(buyButton.text()).toContain('Comprar')
      })
    })

    it('navigates directly to seat-selection when clicking Comprar on an AVAILABLE function', async () => {
      const { pinia, router } = setupTestEnvironment('/event/symphony-of-lights')
      await router.isReady()

      const pushSpy = vi.spyOn(router, 'push')

      const wrapper = mount(EventDetailView, {
        global: {
          plugins: [pinia, router]
        }
      })

      const catalog = useCatalogStore()
      const event = catalog.getEventById('symphony-of-lights')!
      const targetFunction = event.functions[0]!

      const firstFunctionRow = wrapper.findAll('[data-testid="function-row"]')[0]!
      const buyButton = firstFunctionRow.find('button')

      await buyButton.trigger('click')

      expect(pushSpy).toHaveBeenCalledWith({
        name: 'seat-selection',
        params: { id: event.id },
        query: {
          functionId: targetFunction.id,
          date: targetFunction.dateIso
        }
      })
    })

    it('navigates to virtual queue when clicking Comprar on a QUEUE_ACTIVE event/function', async () => {
      const { pinia, router } = setupTestEnvironment('/event/river-stadium-rockfest')
      await router.isReady()

      const pushSpy = vi.spyOn(router, 'push')

      const wrapper = mount(EventDetailView, {
        global: {
          plugins: [pinia, router]
        }
      })

      const catalog = useCatalogStore()
      const queue = useQueueStore()
      const enterQueueSpy = vi.spyOn(queue, 'enterQueue')

      const event = catalog.getEventById('river-stadium-rockfest')!
      const targetFunction = event.functions[0]!

      const row = wrapper.find('[data-testid="function-row"]')
      expect(row.text()).toContain('Fila Virtual')

      const buyButton = row.find('button')
      await buyButton.trigger('click')

      expect(enterQueueSpy).toHaveBeenCalledWith(event.id)
      expect(pushSpy).toHaveBeenCalledWith({
        name: 'queue',
        params: { id: event.id },
        query: {
          functionId: targetFunction.id,
          date: targetFunction.dateIso
        }
      })
    })

    it('disables button with "Agotado" label when a function is SOLD_OUT', async () => {
      const { pinia, router } = setupTestEnvironment('/event/symphony-of-lights')
      await router.isReady()

      const catalog = useCatalogStore()
      const event = catalog.getEventById('symphony-of-lights')!
      const originalStatus = event.functions[1]!.status
      event.functions[1]!.status = 'SOLD_OUT'

      try {
        const pushSpy = vi.spyOn(router, 'push')

        const wrapper = mount(EventDetailView, {
          global: {
            plugins: [pinia, router]
          }
        })

        const functionRows = wrapper.findAll('[data-testid="function-row"]')
        const soldOutRow = functionRows[1]!
        expect(soldOutRow.text()).toContain('Agotado')

        const soldOutBtn = soldOutRow.find('button')
        expect(soldOutBtn.attributes('disabled')).toBeDefined()
        expect(soldOutBtn.text()).toContain('Agotado')

        await soldOutBtn.trigger('click')
        expect(pushSpy).not.toHaveBeenCalled()
      } finally {
        event.functions[1]!.status = originalStatus
      }
    })
  })

  describe('Section C: Payment Methods Accordion', () => {
    it('renders collapsible Radix accordion for payment methods detailing promotions and installments', async () => {
      const { pinia, router } = setupTestEnvironment('/event/symphony-of-lights')
      await router.isReady()

      const wrapper = mount(EventDetailView, {
        global: {
          plugins: [pinia, router]
        }
      })

      const paymentAccordion = wrapper.find('[data-testid="payment-methods-accordion"]')
      expect(paymentAccordion.exists()).toBe(true)

      // Trigger headline
      expect(paymentAccordion.text()).toContain('Medios de Pago y Financiación')
      expect(paymentAccordion.text()).toContain('Hasta 6 cuotas sin interés')

      const trigger = paymentAccordion.find('button')
      await trigger.trigger('click')
      await wrapper.vm.$nextTick()

      // Promotions content
      expect(paymentAccordion.text()).toContain('Santander')
      expect(paymentAccordion.text()).toContain('BBVA')
      expect(paymentAccordion.text()).toContain('Mercado Pago')
      expect(paymentAccordion.text()).toContain('Tarjetas y Billeteras Aceptadas')
    })
  })

  describe('Section D: Parking Accordion', () => {
    it('renders collapsible Radix accordion for official venue parking and access routes', async () => {
      const { pinia, router } = setupTestEnvironment('/event/symphony-of-lights')
      await router.isReady()

      const wrapper = mount(EventDetailView, {
        global: {
          plugins: [pinia, router]
        }
      })

      const parkingAccordion = wrapper.find('[data-testid="venue-parking-accordion"]')
      expect(parkingAccordion.exists()).toBe(true)

      // Trigger headline
      expect(parkingAccordion.text()).toContain('Estacionamiento Oficial y Accesos')
      expect(parkingAccordion.text()).toContain('Estacionamiento disponible')

      const trigger = parkingAccordion.find('button')
      await trigger.trigger('click')
      await wrapper.vm.$nextTick()

      // Parking content
      expect(parkingAccordion.text()).toContain('Estacionamiento Oficial')
      expect(parkingAccordion.text()).toContain('Rutas de Acceso Designadas')
      expect(parkingAccordion.text()).toContain('Tarifa oficial estimada')
    })
  })

  describe('Section E: Interactive Venue Sector Map & Pricing Exploration', () => {
    it('renders interactive venue map section guiding the user that prices are viewed on the stage map and removes legacy static table', async () => {
      const { pinia, router } = setupTestEnvironment('/event/symphony-of-lights')
      await router.isReady()

      const wrapper = mount(EventDetailView, {
        global: {
          plugins: [pinia, router]
        }
      })

      // Strict check: legacy static table is completely deleted
      expect(wrapper.find('[data-testid="sector-price-table"]').exists()).toBe(false)

      // Interactive venue map section is present with clear price exploration guidance
      const mapSection = wrapper.find('[data-testid="interactive-venue-map-section"]')
      expect(mapSection.exists()).toBe(true)
      expect(mapSection.text()).toContain('Plano Interactivo & Precios por Sector')
      expect(mapSection.text()).toContain('Precios visibles al interactuar en el mapa')
      expect(mapSection.text()).toContain('Exploración interactiva estilo Google Maps')
    })
  })

  describe('Trust Badges & Security Guarantees', () => {
    it('renders Redlock 10m TTL and zero-overselling guarantee badges', async () => {
      const { pinia, router } = setupTestEnvironment('/event/symphony-of-lights')
      await router.isReady()

      const wrapper = mount(EventDetailView, {
        global: {
          plugins: [pinia, router]
        }
      })

      expect(wrapper.text()).toContain('Bloqueo Atómico Redlock 10m TTL')
      expect(wrapper.text()).toContain('Garantía de Asiento Sin Sobreventa')
    })
  })

  describe('Section F: Real-time Countdown, FAQs and Payment Logos', () => {
    it('renders the real-time countdown banner formatted as "Faltan dd, hh, mm, ss" for upcoming concerts', async () => {
      const { pinia, router } = setupTestEnvironment('/event/coldplay-music-spheres')
      await router.isReady()

      const wrapper = mount(EventDetailView, {
        global: {
          plugins: [pinia, router]
        }
      })

      const countdownBar = wrapper.find('[data-testid="concert-countdown-bar"]')
      expect(countdownBar.exists()).toBe(true)
      expect(countdownBar.text()).toMatch(/Faltan \d+d, \d+h, \d+m, \d+s/)
    })

    it('renders "Concierto finalizado" when event date is in the past', async () => {
      const { pinia, router } = setupTestEnvironment('/event/symphony-of-lights')
      await router.isReady()

      const wrapper = mount(EventDetailView, {
        global: {
          plugins: [pinia, router]
        }
      })

      const countdownBar = wrapper.find('[data-testid="concert-countdown-bar"]')
      expect(countdownBar.exists()).toBe(true)
      expect(countdownBar.text()).toContain('Concierto finalizado')
    })

    it('renders the description FAQ accordion covering policies, setlist and max tickets', async () => {
      const { pinia, router } = setupTestEnvironment('/event/symphony-of-lights')
      await router.isReady()

      const wrapper = mount(EventDetailView, {
        global: {
          plugins: [pinia, router]
        }
      })

      expect(wrapper.text()).toContain('Preguntas Frecuentes y Políticas del Evento')
      expect(wrapper.text()).toContain('Políticas de Acceso y Elementos Prohibidos')
      expect(wrapper.text()).toContain('Cancionero y Repertorio del Show')
      expect(wrapper.text()).toContain('Cantidad Máxima de Entradas por Persona')
    })

    it('renders payment logos for accepted payment methods', async () => {
      const { pinia, router } = setupTestEnvironment('/event/symphony-of-lights')
      await router.isReady()

      const wrapper = mount(EventDetailView, {
        global: {
          plugins: [pinia, router]
        }
      })

      const paymentAccordion = wrapper.find('[data-testid="payment-methods-accordion"]')
      const trigger = paymentAccordion.find('button')
      await trigger.trigger('click')
      await wrapper.vm.$nextTick()

      const paymentImages = paymentAccordion.findAll('img')
      expect(paymentImages.length).toBeGreaterThan(0)
      const srcs = paymentImages.map(img => img.attributes('src'))
      expect(srcs.some(src => src?.includes('visa'))).toBe(true)
      expect(srcs.some(src => src?.includes('mastercard'))).toBe(true)
    })
  })
})
