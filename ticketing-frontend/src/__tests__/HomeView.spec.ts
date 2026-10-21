import { describe, it, expect, beforeEach, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { createRouter, createMemoryHistory } from 'vue-router'
import { setActivePinia, createPinia } from 'pinia'
import HomeView from '../views/HomeView.vue'
import { useCatalogStore, getMinEventPrice } from '../stores/catalog'

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

  it('renders the prominent "Entradas desde $X" badge on every event card in both sections', async () => {
    const { pinia, router } = setupTestEnvironment()
    router.push('/')
    await router.isReady()

    const wrapper = mount(HomeView, {
      global: {
        plugins: [pinia, router]
      }
    })

    const catalog = useCatalogStore()
    const articles = wrapper.findAll('article')
    // 3 cards in Recientemente Agregados + 3 cards in Próximos a Ocurrir = 6 cards
    expect(articles).toHaveLength(6)

    articles.forEach(card => {
      const badge = card.find('span.inline-flex')
      expect(badge.exists()).toBe(true)
      expect(badge.text()).toContain('Entradas desde')
      expect(badge.text()).toMatch(/Entradas desde\s*\$\d+/)
    })

    // Verify badge values match getMinEventPrice for recent cards (first 3)
    const expectedRecent = catalog.recentlyAddedEvents.slice(0, 3)
    expectedRecent.forEach((ev, idx) => {
      const card = articles[idx]!
      const minPrice = getMinEventPrice(ev)
      expect(card.text()).toContain(`$${minPrice}`)
    })

    // Verify badge values match getMinEventPrice for upcoming cards (last 3)
    const expectedUpcoming = catalog.upcomingEvents.slice(0, 3)
    expectedUpcoming.forEach((ev, idx) => {
      const card = articles[3 + idx]!
      const minPrice = getMinEventPrice(ev)
      expect(card.text()).toContain(`$${minPrice}`)
    })
  })

  it('navigates to /event/:id when clicking any event card', async () => {
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

    await firstCard.trigger('click')

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
})
