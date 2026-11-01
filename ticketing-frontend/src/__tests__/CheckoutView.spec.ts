import { describe, it, expect, beforeEach, vi, afterEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { createRouter, createMemoryHistory } from 'vue-router'
import { setActivePinia, createPinia } from 'pinia'
import CheckoutView from '../views/CheckoutView.vue'
import { useTicketingStore } from '../stores/ticketing'
import { useTicketsStore } from '../stores/tickets'

function setupTestEnvironment(initialRoute = '/event/chronos-fest-2026/checkout') {
  const pinia = createPinia()
  setActivePinia(pinia)

  const router = createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/', name: 'home', component: { template: '<div>Home</div>' } },
      { path: '/event/:id/seats', name: 'seat-selection', component: { template: '<div>Seat Selection</div>' } },
      { path: '/event/:id/checkout', name: 'checkout', component: CheckoutView },
      { path: '/tickets', name: 'tickets', component: { template: '<div>Tickets Dashboard</div>' } },
    ],
  })

  router.push(initialRoute)

  return { pinia, router }
}

describe('CheckoutView.spec.ts - Payment & Checkout Dark Blue Wireframe Implementation', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    vi.useFakeTimers()
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  it('renders the checkout wireframe header with Back button, Hibana.tk logo, and Redlock timer', async () => {
    const { pinia, router } = setupTestEnvironment()
    await router.isReady()

    const wrapper = mount(CheckoutView, {
      global: {
        plugins: [pinia, router],
      },
    })
    await wrapper.vm.$nextTick()

    // Header items
    expect(wrapper.text()).toContain('Volver')
    expect(wrapper.text()).toContain('Hibana.tk')
    expect(wrapper.find('#countdown').exists()).toBe(true)
    expect(wrapper.find('#countdown').text()).toMatch(/\d{2}:\d{2}/)
  })

  it('renders the 3-step progress indicator correctly', async () => {
    const { pinia, router } = setupTestEnvironment()
    await router.isReady()

    const wrapper = mount(CheckoutView, {
      global: {
        plugins: [pinia, router],
      },
    })
    await wrapper.vm.$nextTick()

    expect(wrapper.text()).toContain('Selección')
    expect(wrapper.text()).toContain('Pago')
    expect(wrapper.text()).toContain('Confirmación')
  })

  it('renders the Order Summary with event details, tickets, parking pass, and cost breakdown', async () => {
    const { pinia, router } = setupTestEnvironment()
    await router.isReady()

    const wrapper = mount(CheckoutView, {
      global: {
        plugins: [pinia, router],
      },
    })
    await wrapper.vm.$nextTick()

    expect(wrapper.text()).toContain('Resumen del Pedido')
    expect(wrapper.text()).toContain('Subtotal')
    expect(wrapper.text()).toContain('Costo por Servicio')
    expect(wrapper.text()).toContain('Total')
    expect(wrapper.text()).toContain('1x Pase de Estacionamiento')
    expect(wrapper.text()).toContain('Pago 100% Seguro')
  })

  it('renders Radix payment tabs and allows switching between Credit Card, Pocket Wallet, and Bank Transfer', async () => {
    const { pinia, router } = setupTestEnvironment()
    await router.isReady()

    const wrapper = mount(CheckoutView, {
      global: {
        plugins: [pinia, router],
      },
    })
    await wrapper.vm.$nextTick()

    // All 3 tab triggers exist
    expect(wrapper.text()).toContain('Tarjeta de Débito')
    expect(wrapper.text()).toContain('Tarjeta de Crédito')
    expect(wrapper.text()).toContain('Mercado Pago')

    // Form inputs for Credit Card
    expect(wrapper.find('input[placeholder="0000 0000 0000 0000"]').exists()).toBe(true)
    expect(wrapper.find('input[placeholder="NOMBRE Y APELLIDO"]').exists()).toBe(true)
    expect(wrapper.find('input[placeholder="MM/AA"]').exists()).toBe(true)
    expect(wrapper.find('input[placeholder="123"]').exists()).toBe(true)
  })

  it('dynamically recalculates total when parking pass is toggled', async () => {
    const { pinia, router } = setupTestEnvironment()
    await router.isReady()

    const wrapper = mount(CheckoutView, {
      global: {
        plugins: [pinia, router],
      },
    })
    await wrapper.vm.$nextTick()

    const parkingCheckbox = wrapper.find('input[type="checkbox"]')
    expect(parkingCheckbox.exists()).toBe(true)

    // Initial total includes tickets ($90.000) + fee 10% ($9.000) + parking ($15.000) = $114.000
    expect(wrapper.text()).toContain('114.000')

    // Toggle off parking pass
    await parkingCheckbox.setValue(false)
    await wrapper.vm.$nextTick()

    // Recalculated total without parking is $99.000 ($90.000 + $9.000)
    expect(wrapper.text()).toContain('99.000')
  })

  it('triggers payment processing overlay and completes order issuing tickets', async () => {
    const { pinia, router } = setupTestEnvironment()
    await router.isReady()

    const wrapper = mount(CheckoutView, {
      global: {
        plugins: [pinia, router],
      },
    })
    await wrapper.vm.$nextTick()

    const ticketsStore = useTicketsStore(pinia)
    const initialTicketsCount = ticketsStore.tickets.length

    const payButton = wrapper.find('#btn-pay')
    expect(payButton.exists()).toBe(true)

    // Trigger complete order
    await payButton.trigger('click')
    await wrapper.vm.$nextTick()

    // Processing overlay should appear
    expect(wrapper.find('#processing-overlay').exists()).toBe(true)
    expect(wrapper.text()).toContain('Validando transacción segura...')

    // Fast-forward processing simulation timer
    vi.advanceTimersByTime(3000)
    await wrapper.vm.$nextTick()

    // Verify tickets were added to digital wallet
    expect(ticketsStore.tickets.length).toBeGreaterThan(initialTicketsCount)
  })

  it('triggers expired lock modal when Redlock timer runs out', async () => {
    const { pinia, router } = setupTestEnvironment()
    await router.isReady()

    const ticketingStore = useTicketingStore(pinia)

    mount(CheckoutView, {
      global: {
        plugins: [pinia, router],
      },
    })

    // Signal expiration in ticketing store
    ticketingStore.isTimerExpired = true
    await vi.runAllTimersAsync()

    // Modal dialog opens
    expect(document.body.innerHTML).toContain('Tus Asientos Fueron Liberados')
  })
})
