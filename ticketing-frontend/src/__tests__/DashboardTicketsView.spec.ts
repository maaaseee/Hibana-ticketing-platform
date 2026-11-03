import { describe, it, expect, beforeEach, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { createRouter, createMemoryHistory } from 'vue-router'
import DashboardTicketsView from '../views/DashboardTicketsView.vue'
import { useTicketsStore } from '../stores/tickets'

function setupTest() {
  const pinia = createPinia()
  setActivePinia(pinia)

  const router = createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/tickets', name: 'tickets', component: DashboardTicketsView },
      { path: '/events', name: 'events-catalog', component: { template: '<div>Events</div>' } },
    ],
  })

  return { pinia, router }
}

describe('DashboardTicketsView.vue - Hybrid Wireframe & Multi-Ticket Viewer', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    window.scrollTo = vi.fn()
  })

  it('renders the initial list view with "Mis compras" title and 3 segmented filter tabs', async () => {
    const { pinia, router } = setupTest()
    await router.push('/tickets')
    await router.isReady()

    const wrapper = mount(DashboardTicketsView, {
      global: { plugins: [pinia, router] },
    })

    expect(wrapper.text()).toContain('Mis compras')
    expect(wrapper.text()).toContain('Credenciales Cifradas')
    expect(wrapper.text()).toContain('PRÓXIMOS')
    expect(wrapper.text()).toContain('ANTERIORES')
    expect(wrapper.text()).toContain('RESERVAS')
    expect(wrapper.text()).toContain('Order History')
  })

  it('displays order cards with pinned quantity badge and "Ver entrada" action', async () => {
    const { pinia, router } = setupTest()
    await router.push('/tickets')
    await router.isReady()

    const wrapper = mount(DashboardTicketsView, {
      global: { plugins: [pinia, router] },
    })

    // Upcoming tab is active by default
    expect(wrapper.text()).toContain('Neon Nights Festival')
    expect(wrapper.text()).toContain('CHRONOS FESTIVAL 2026')
    expect(wrapper.text()).toContain('x2 entradas')

    const verEntradaBtns = wrapper.findAll('button').filter(b => b.text().includes('Ver entrada'))
    expect(verEntradaBtns.length).toBeGreaterThan(0)
  })

  it('switches between tabs and shows past orders under ANTERIORES', async () => {
    const { pinia, router } = setupTest()
    await router.push('/tickets')
    await router.isReady()

    const wrapper = mount(DashboardTicketsView, {
      global: { plugins: [pinia, router] },
    })

    // Click on "ANTERIORES"
    const pastTab = wrapper.findAll('button').find(b => b.text().includes('ANTERIORES'))
    expect(pastTab).toBeDefined()
    await pastTab!.trigger('click')

    // Should display Andrés Calamaro 2025 and Cybersymphony
    expect(wrapper.text()).toContain('ANDRES CALAMARO 2025')
    expect(wrapper.text()).toContain('x5 entradas')
    expect(wrapper.text()).toContain('CYBERSYMPHONY TOUR 2025')
  })

  it('switches between tabs and shows cancelled / reservations under RESERVAS', async () => {
    const { pinia, router } = setupTest()
    await router.push('/tickets')
    await router.isReady()

    const wrapper = mount(DashboardTicketsView, {
      global: { plugins: [pinia, router] },
    })

    const reservasTab = wrapper.findAll('button').find(b => b.text().includes('RESERVAS'))
    expect(reservasTab).toBeDefined()
    await reservasTab!.trigger('click')

    expect(wrapper.text()).toContain('CITY DERBY MATCH')
    expect(wrapper.text()).toContain('Cancelado')
  })

  it('transitions to detail inspection mode when clicking "Ver entrada"', async () => {
    const { pinia, router } = setupTest()
    await router.push('/tickets')
    await router.isReady()

    const wrapper = mount(DashboardTicketsView, {
      global: { plugins: [pinia, router] },
    })

    // Click on first "Ver entrada"
    const verEntradaBtn = wrapper.findAll('button').find(b => b.text().includes('Ver entrada'))!
    await verEntradaBtn.trigger('click')

    // Detail mode view rendered
    expect(wrapper.text()).toContain('Volver a mis compras')
    expect(wrapper.text()).toMatch(/Entrada 1 de \d+/)
    expect(wrapper.text()).toContain('Descargar')
    expect(wrapper.text()).toContain('Cómo llegar')
    expect(wrapper.text()).toContain('Apertura de puertas')
    expect(wrapper.text()).toContain('Hora show')
    expect(wrapper.text()).toContain('Fila')
    expect(wrapper.text()).toContain('Asiento')

    // Document comes strictly from backend customerDocument
    expect(wrapper.text()).toContain('Documento:')
    expect(wrapper.text()).toContain('38.921.402')

    // Verifies 'Firma xxx valida' and text under QR are eliminated
    expect(wrapper.text()).not.toContain('Firma Ed25519 Válida')
    expect(wrapper.text()).not.toContain('Escanear al ingresar en molinete')
    expect(wrapper.text()).not.toContain('Escanear en el ingreso')
  })

  it('allows ticket pagination (< Entrada X de Y >) inside detail mode', async () => {
    const { pinia, router } = setupTest()
    await router.push('/tickets')
    await router.isReady()

    const wrapper = mount(DashboardTicketsView, {
      global: { plugins: [pinia, router] },
    })

    // Switch to ANTERIORES and open Calamaro (which has 5 tickets)
    const pastTab = wrapper.findAll('button').find(b => b.text().includes('ANTERIORES'))!
    await pastTab.trigger('click')

    const verEntradaBtn = wrapper.findAll('button').find(b => b.text().includes('Ver entrada'))!
    await verEntradaBtn.trigger('click')

    expect(wrapper.text()).toContain('Entrada 1 de 5')

    // Click next ticket
    const nextBtn = wrapper.find('button[title="Entrada siguiente"]')
    expect(nextBtn.exists()).toBe(true)
    await nextBtn.trigger('click')

    expect(wrapper.text()).toContain('Entrada 2 de 5')

    // Click previous ticket
    const prevBtn = wrapper.find('button[title="Entrada anterior"]')
    expect(prevBtn.exists()).toBe(true)
    await prevBtn.trigger('click')

    expect(wrapper.text()).toContain('Entrada 1 de 5')
  })

  it('returns to list view when clicking "Volver a mis compras"', async () => {
    const { pinia, router } = setupTest()
    await router.push('/tickets')
    await router.isReady()

    const wrapper = mount(DashboardTicketsView, {
      global: { plugins: [pinia, router] },
    })

    const verEntradaBtn = wrapper.findAll('button').find(b => b.text().includes('Ver entrada'))!
    await verEntradaBtn.trigger('click')
    expect(wrapper.text()).toContain('Volver a mis compras')

    const backBtn = wrapper.findAll('button').find(b => b.text().includes('Volver a mis compras'))!
    await backBtn.trigger('click')

    expect(wrapper.text()).toContain('Order History')
    expect(wrapper.text()).toContain('Mis compras')
  })

  it('opens transfer modal when clicking "Transferir" on an issued ticket', async () => {
    const { pinia, router } = setupTest()
    await router.push('/tickets')
    await router.isReady()

    const wrapper = mount(DashboardTicketsView, {
      global: { plugins: [pinia, router] },
    })

    const verEntradaBtn = wrapper.findAll('button').find(b => b.text().includes('Ver entrada'))!
    await verEntradaBtn.trigger('click')

    const transferBtn = wrapper.findAll('button').find(b => b.text().includes('Transferir'))
    expect(transferBtn).toBeDefined()
    await transferBtn!.trigger('click')

    expect(wrapper.text()).toContain('Enviar Entrada a otra Persona')
    expect(wrapper.text()).toContain('Correo Electrónico del Destinatario')
  })
})
