import { describe, it, expect, beforeEach, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { createRouter, createMemoryHistory } from 'vue-router'
import { setActivePinia, createPinia } from 'pinia'
import HomeView from '../views/HomeView.vue'
import { useCatalogStore } from '../stores/catalog'

function setupTestEnvironment() {
  const pinia = createPinia()
  setActivePinia(pinia)

  const router = createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/', name: 'home', component: HomeView },
      { path: '/events', name: 'events-catalog', component: { template: '<div>Events Catalog</div>' } },
      { path: '/event/:id', name: 'event-detail', component: { template: '<div>Event Detail</div>' } },
      { path: '/help', name: 'help', component: { template: '<div>Help</div>' } }
    ]
  })

  return { pinia, router }
}

describe('HomeView.spec.ts - Landing Page & Catalog Sections', () => {
  beforeEach(() => {
    vi.useFakeTimers()
  })

  it('renders "Recientemente Agregados" section with cards sourced from catalog.recentlyAddedEvents', async () => {
    const { pinia, router } = setupTestEnvironment()
    router.push('/')
    await router.isReady()

    const wrapper = mount(HomeView, {
      global: {
        plugins: [pinia, router]
      }
    })

    const catalog = useCatalogStore()
    expect(wrapper.text()).toContain('Recientemente Agregados')
    expect(wrapper.text()).toContain('Nuevos en Cartelera')

    // Find the section containing "Recientemente Agregados"
    const recentHeading = wrapper.findAll('h2').find(h => h.text().includes('Recientemente Agregados'))
    expect(recentHeading).toBeDefined()

    // The first 3 events of catalog.recentlyAddedEvents should be rendered
    const expectedRecent = catalog.recentlyAddedEvents.slice(0, 3)
    expectedRecent.forEach(event => {
      expect(wrapper.text()).toContain(event.title)
    })
  })

  it('renders "Próximos a Ocurrir" section with cards sourced from catalog.upcomingEvents', async () => {
    const { pinia, router } = setupTestEnvironment()
    router.push('/')
    await router.isReady()

    const wrapper = mount(HomeView, {
      global: {
        plugins: [pinia, router]
      }
    })

    const catalog = useCatalogStore()
    expect(wrapper.text()).toContain('Próximos a Ocurrir')
    expect(wrapper.text()).toContain('En Agenda Cercana')

    // Find heading for "Próximos a Ocurrir"
    const upcomingHeading = wrapper.findAll('h2').find(h => h.text().includes('Próximos a Ocurrir'))
    expect(upcomingHeading).toBeDefined()

    // The first 3 events of catalog.upcomingEvents should be rendered
    const expectedUpcoming = catalog.upcomingEvents.slice(0, 3)
    expectedUpcoming.forEach(event => {
      expect(wrapper.text()).toContain(event.title)
    })
  })

  it('renders every event card in both sections with its action buttons', async () => {
    const { pinia, router } = setupTestEnvironment()
    router.push('/')
    await router.isReady()

    const wrapper = mount(HomeView, {
      global: {
        plugins: [pinia, router]
      }
    })

    const catalog = useCatalogStore()
    const articles = wrapper.findAll('[data-testid="event-card"]')
    // 3 cards in Recientemente Agregados + 3 cards in Próximos a Ocurrir = 6 cards
    expect(articles).toHaveLength(6)

    articles.forEach(card => {
      const buyBtn = card.find('[data-testid="event-card-buy"]')
      expect(buyBtn.exists()).toBe(true)
      expect(buyBtn.text()).toContain('Comprar')

      const moreInfoBtn = card.find('[data-testid="event-card-more-info"]')
      expect(moreInfoBtn.exists()).toBe(true)
      expect(moreInfoBtn.text()).toContain('Más info')
    })

    // Verify titles match for recent cards (first 3)
    const expectedRecent = catalog.recentlyAddedEvents.slice(0, 3)
    expectedRecent.forEach((ev, idx) => {
      const card = articles[idx]!
      expect(card.text()).toContain(ev.title)
    })

    // Verify titles match for upcoming cards (last 3)
    const expectedUpcoming = catalog.upcomingEvents.slice(0, 3)
    expectedUpcoming.forEach((ev, idx) => {
      const card = articles[3 + idx]!
      expect(card.text()).toContain(ev.title)
    })
  })

  it('navigates to /event/:id when clicking buy button or image on any event card', async () => {
    const { pinia, router } = setupTestEnvironment()
    router.push('/')
    await router.isReady()

    const pushSpy = vi.spyOn(router, 'push')

    const wrapper = mount(HomeView, {
      global: {
        plugins: [pinia, router]
      }
    })

    const catalog = useCatalogStore()
    const firstRecentEvent = catalog.recentlyAddedEvents[0]!
    const cards = wrapper.findAll('article')
    const firstCard = cards[0]!

    const buyButton = firstCard.find('[data-testid="event-card-buy"]')
    await buyButton.trigger('click')

    expect(pushSpy).toHaveBeenCalledWith({
      name: 'event-detail',
      params: { id: firstRecentEvent.id }
    })
  })

  it('renders "Ver Todos los Eventos" navigation links pointing to /events', async () => {
    const { pinia, router } = setupTestEnvironment()
    router.push('/')
    await router.isReady()

    const wrapper = mount(HomeView, {
      global: {
        plugins: [pinia, router]
      }
    })

    const links = wrapper.findAll('a').filter(a => a.text().includes('Ver Todos los Eventos'))
    expect(links.length).toBeGreaterThanOrEqual(2)
    links.forEach(link => {
      expect(link.attributes('href')).toBe('/events')
    })
  })

  it('renders the featured carousel with slide controls and "Ver Entradas" action', async () => {
    const { pinia, router } = setupTestEnvironment()
    router.push('/')
    await router.isReady()

    const pushSpy = vi.spyOn(router, 'push')

    const wrapper = mount(HomeView, {
      global: {
        plugins: [pinia, router]
      }
    })

    const catalog = useCatalogStore()
    const activeSlide = catalog.featuredEvents[0]!

    expect(wrapper.text()).toContain(activeSlide.title)

    const verEntradasBtn = wrapper.findAll('button').find(b => b.text().includes('Ver Entradas'))
    expect(verEntradasBtn).toBeDefined()

    await verEntradasBtn!.trigger('click')
    expect(pushSpy).toHaveBeenCalledWith({
      name: 'event-detail',
      params: { id: activeSlide.id }
    })
  })

  it('renders "Conoce los Lugares y Recintos" section with venue cards and modal trigger info', async () => {
    const { pinia, router } = setupTestEnvironment()
    router.push('/')
    await router.isReady()

    const wrapper = mount(HomeView, {
      global: {
        plugins: [pinia, router]
      }
    })

    expect(wrapper.text()).toContain('Conoce los Lugares y Recintos')
    expect(wrapper.text()).toContain('Estadio Monumental')
    expect(wrapper.text()).toContain('Arena Buenos Aires')
    expect(wrapper.text()).toContain('Teatro Rex')
  })
})
