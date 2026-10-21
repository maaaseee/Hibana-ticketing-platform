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
      expect(bannerBadge.text()).toContain('USD')

      // Also verify complementary inline badge in title area
      const inlineBadge = wrapper.findAll('span').find(s => s.text().includes(`Entradas desde $${minPrice} USD`))
      expect(inlineBadge).toBeDefined()
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

      const paymentAccordion = wrapper.find('[value="payment-methods"]')
      expect(paymentAccordion.exists()).toBe(true)

      // Trigger headline
      expect(paymentAccordion.text()).toContain('Medios de Pago y Financiación')
      expect(paymentAccordion.text()).toContain('Hasta 6 cuotas sin interés')

      // Promotions content
      expect(paymentAccordion.text()).toContain('Santander')
      expect(paymentAccordion.text()).toContain('Galicia')
      expect(paymentAccordion.text()).toContain('MODO')
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

      const parkingAccordion = wrapper.find('[value="venue-parking"]')
      expect(parkingAccordion.exists()).toBe(true)

      // Trigger headline
      expect(parkingAccordion.text()).toContain('Estacionamiento Oficial y Accesos')
      expect(parkingAccordion.text()).toContain('Estacionamiento disponible')

      // Parking content
      expect(parkingAccordion.text()).toContain('Estacionamiento Oficial')
      expect(parkingAccordion.text()).toContain('Rutas de Acceso Designadas')
      expect(parkingAccordion.text()).toContain('Tarifa oficial estimada')
    })
  })

  describe('Section E: Sector Price Breakdown Table', () => {
    it('renders sector price table mapping sectors, types, prices, and status', async () => {
      const { pinia, router } = setupTestEnvironment('/event/symphony-of-lights')
      await router.isReady()

      const wrapper = mount(EventDetailView, {
        global: {
          plugins: [pinia, router]
        }
      })

      const catalog = useCatalogStore()
      const event = catalog.getEventById('symphony-of-lights')!

      const table = wrapper.find('[data-testid="sector-price-table"]')
      expect(table.exists()).toBe(true)

      // Column headers
      expect(table.text()).toContain('Sector')
      expect(table.text()).toContain('Tipo / Ubicación')
      expect(table.text()).toContain('Precio Base')
      expect(table.text()).toContain('Disponibilidad')

      // Rows
      const rows = table.findAll('[data-testid="sector-row"]')
      expect(rows).toHaveLength(event.sectors.length)

      event.sectors.forEach((sec, idx) => {
        const row = rows[idx]!
        expect(row.text()).toContain(sec.name)
        expect(row.text()).toContain(`$${sec.price}`)
        expect(row.text()).toContain(sec.currency)
      })
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
})
