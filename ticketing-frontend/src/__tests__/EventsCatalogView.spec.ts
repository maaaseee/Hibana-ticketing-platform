import { describe, it, expect, beforeEach, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { createRouter, createMemoryHistory } from 'vue-router'
import { setActivePinia, createPinia } from 'pinia'
import EventsCatalogView from '../views/EventsCatalogView.vue'
import { useCatalogStore, getMinEventPrice } from '../stores/catalog'

function setupTestEnvironment() {
  const pinia = createPinia()
  setActivePinia(pinia)

  const router = createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/events', name: 'events-catalog', component: EventsCatalogView },
      { path: '/event/:id', name: 'event-detail', component: { template: '<div>Event Detail</div>' } }
    ]
  })

  return { pinia, router }
}

describe('EventsCatalogView.spec.ts - Catalog Shows & Filters Experience', () => {
  beforeEach(() => {
    vi.useFakeTimers()
  })

  describe('Dynamic 8-Month Selector', () => {
    it('generates upcoming months up to 8 months into the future with "Todos los meses" as default', async () => {
      const { pinia, router } = setupTestEnvironment()
      router.push('/events')
      await router.isReady()

      const wrapper = mount(EventsCatalogView, {
        global: {
          plugins: [pinia, router]
        }
      })

      const monthSelect = wrapper.findAll('select')[0]!
      expect(monthSelect.exists()).toBe(true)

      const options = monthSelect.findAll('option')
      // 1 option "Todos los meses" + 8 dynamic months = 9 options
      expect(options).toHaveLength(9)
      expect(options[0]!.text()).toBe('Todos los meses')
      expect(options[1]!.text()).toBe('Octubre 2026')
      expect(options[2]!.text()).toBe('Noviembre 2026')
      expect(options[3]!.text()).toBe('Diciembre 2026')
      expect(options[4]!.text()).toBe('Enero 2027')
      expect(options[5]!.text()).toBe('Febrero 2027')
      expect(options[6]!.text()).toBe('Marzo 2027')
      expect(options[7]!.text()).toBe('Abril 2027')
      expect(options[8]!.text()).toBe('Mayo 2027')
    })

    it('filters events accurately by selected month', async () => {
      const { pinia, router } = setupTestEnvironment()
      router.push('/events')
      await router.isReady()

      const wrapper = mount(EventsCatalogView, {
        global: {
          plugins: [pinia, router]
        }
      })

      const monthSelect = wrapper.findAll('select')[0]!
      await monthSelect.setValue('Noviembre 2026')
      await wrapper.vm.$nextTick()

      const cards = wrapper.findAll('article')
      expect(cards.length).toBe(3) // Coldplay, Global Tech Summit, Indie Rock Sessions
      cards.forEach(card => {
        expect(card.text().toLowerCase()).toContain('noviembre de 2026')
      })
    })
  })

  describe('Canonical Buenos Aires Venue Filter', () => {
    it('provides strictly the 3 canonical Buenos Aires venues in venue filter dropdown', async () => {
      const { pinia, router } = setupTestEnvironment()
      router.push('/events')
      await router.isReady()

      const wrapper = mount(EventsCatalogView, {
        global: {
          plugins: [pinia, router]
        }
      })

      const venueSelect = wrapper.findAll('select')[1]!
      expect(venueSelect.exists()).toBe(true)

      const venueOptions = venueSelect.findAll('option').map(o => o.text())
      expect(venueOptions).toEqual([
        'Todos los recintos',
        'Estadio Monumental (Buenos Aires)',
        'Arena Buenos Aires (Buenos Aires)',
        'Teatro Rex (Buenos Aires)'
      ])
    })

    it('filters events accurately by selected venue', async () => {
      const { pinia, router } = setupTestEnvironment()
      router.push('/events')
      await router.isReady()

      const wrapper = mount(EventsCatalogView, {
        global: {
          plugins: [pinia, router]
        }
      })

      const venueSelect = wrapper.findAll('select')[1]!
      await venueSelect.setValue('Estadio Monumental (Buenos Aires)')
      await wrapper.vm.$nextTick()

      const cards = wrapper.findAll('article')
      expect(cards.length).toBeGreaterThan(0)
      cards.forEach(card => {
        expect(card.text()).toContain('Estadio Monumental (Buenos Aires)')
      })
    })
  })

  describe('Anti-Pattern Assertions', () => {
    it('enforces anti-patterns: verifies input[type="range"] does NOT exist in the DOM', async () => {
      const { pinia, router } = setupTestEnvironment()
      router.push('/events')
      await router.isReady()

      const wrapper = mount(EventsCatalogView, {
        global: {
          plugins: [pinia, router]
        }
      })

      expect(wrapper.find('input[type="range"]').exists()).toBe(false)
    })

    it('enforces anti-patterns: verifies genre pills and category chips do NOT exist in the DOM', async () => {
      const { pinia, router } = setupTestEnvironment()
      router.push('/events')
      await router.isReady()

      const wrapper = mount(EventsCatalogView, {
        global: {
          plugins: [pinia, router]
        }
      })

      const text = wrapper.text().toLowerCase()
      expect(text).not.toContain('pop')
      expect(text).not.toContain('jazz')
      expect(text).not.toContain('hip hop')
      expect(wrapper.findAll('button.pill, .chip, [data-genre]').length).toBe(0)
    })
  })

  describe('300ms Debounced Search', () => {
    it('matches event by title with 300ms debounce', async () => {
      const { pinia, router } = setupTestEnvironment()
      router.push('/events')
      await router.isReady()

      const wrapper = mount(EventsCatalogView, {
        global: {
          plugins: [pinia, router]
        }
      })

      const searchInput = wrapper.find('input[type="text"]')
      await searchInput.setValue('Coldplay')

      // Before timer advances, all 10 events remain
      expect(wrapper.findAll('article').length).toBe(10)

      // Advance by 300ms
      vi.advanceTimersByTime(300)
      await wrapper.vm.$nextTick()

      const filteredCards = wrapper.findAll('article')
      expect(filteredCards.length).toBe(1)
      expect(filteredCards[0]!.text()).toContain('Coldplay')
    })

    it('matches event by subtitle with 300ms debounce', async () => {
      const { pinia, router } = setupTestEnvironment()
      router.push('/events')
      await router.isReady()

      const wrapper = mount(EventsCatalogView, {
        global: {
          plugins: [pinia, router]
        }
      })

      const searchInput = wrapper.find('input[type="text"]')
      // Subtitle of symphony-of-lights is "Concierto Sinfónico & Show Lumínico Inmersivo"
      await searchInput.setValue('Lumínico')

      vi.advanceTimersByTime(300)
      await wrapper.vm.$nextTick()

      const filteredCards = wrapper.findAll('article')
      expect(filteredCards.length).toBe(1)
      expect(filteredCards[0]!.text()).toContain('Symphony of Lights')
    })

    it('matches event by lineup/artist with 300ms debounce', async () => {
      const { pinia, router } = setupTestEnvironment()
      router.push('/events')
      await router.isReady()

      const wrapper = mount(EventsCatalogView, {
        global: {
          plugins: [pinia, router]
        }
      })

      const searchInput = wrapper.find('input[type="text"]')
      // Lineup of river-stadium-rockfest is "Foo Fighters, Queens of the Stone Age"
      await searchInput.setValue('Foo Fighters')

      vi.advanceTimersByTime(300)
      await wrapper.vm.$nextTick()

      const filteredCards = wrapper.findAll('article')
      expect(filteredCards.length).toBe(1)
      expect(filteredCards[0]!.text()).toContain('Monumental Rock Festival 2026')
    })

    it('clears search input and restores catalog when clicking clear icon', async () => {
      const { pinia, router } = setupTestEnvironment()
      router.push('/events')
      await router.isReady()

      const wrapper = mount(EventsCatalogView, {
        global: {
          plugins: [pinia, router]
        }
      })

      const searchInput = wrapper.find('input[type="text"]')
      await searchInput.setValue('Coldplay')
      vi.advanceTimersByTime(300)
      await wrapper.vm.$nextTick()

      expect(wrapper.findAll('article').length).toBe(1)

      const clearBtn = wrapper.find('button[title="Limpiar búsqueda"]')
      expect(clearBtn.exists()).toBe(true)

      await clearBtn.trigger('click')
      await wrapper.vm.$nextTick()

      expect(wrapper.findAll('article').length).toBe(10)
    })
  })

  describe('Prominent Min Price Badge on Cards', () => {
    it('displays prominent "Entradas desde $X" badge on all catalog cards matching getMinEventPrice', async () => {
      const { pinia, router } = setupTestEnvironment()
      router.push('/events')
      await router.isReady()

      const wrapper = mount(EventsCatalogView, {
        global: {
          plugins: [pinia, router]
        }
      })

      const catalog = useCatalogStore()
      const cards = wrapper.findAll('article')
      expect(cards.length).toBe(10)

      cards.forEach((card, idx) => {
        const correspondingEvent = catalog.upcomingEvents[idx]!
        const minPrice = getMinEventPrice(correspondingEvent)

        expect(card.text()).toContain('Entradas desde')
        expect(card.text()).toContain(`$${minPrice}`)
      })
    })
  })

  describe('Two-Way Reactive URL Synchronization', () => {
    it('initializes search, month, and venue filters from route.query', async () => {
      const { pinia, router } = setupTestEnvironment()
      router.push('/events?q=Rock&month=Diciembre%202026&venue=Estadio%20Monumental%20(Buenos%20Aires)')
      await router.isReady()

      const wrapper = mount(EventsCatalogView, {
        global: {
          plugins: [pinia, router]
        }
      })

      await wrapper.vm.$nextTick()

      const searchInput = wrapper.find('input[type="text"]')
      expect((searchInput.element as HTMLInputElement).value).toBe('Rock')

      const monthSelect = wrapper.findAll('select')[0]!
      expect((monthSelect.element as HTMLSelectElement).value).toBe('Diciembre 2026')

      const venueSelect = wrapper.findAll('select')[1]!
      expect((venueSelect.element as HTMLSelectElement).value).toBe('Estadio Monumental (Buenos Aires)')

      const cards = wrapper.findAll('article')
      expect(cards.length).toBe(1)
      expect(cards[0]!.text()).toContain('Monumental Rock Festival 2026')
    })

    it('updates route.query when filters change in the UI', async () => {
      const { pinia, router } = setupTestEnvironment()
      router.push('/events')
      await router.isReady()

      const replaceSpy = vi.spyOn(router, 'replace')

      const wrapper = mount(EventsCatalogView, {
        global: {
          plugins: [pinia, router]
        }
      })

      // Update venue select
      const venueSelect = wrapper.findAll('select')[1]!
      await venueSelect.setValue('Teatro Rex (Buenos Aires)')

      expect(replaceSpy).toHaveBeenCalledWith({
        query: {
          venue: 'Teatro Rex (Buenos Aires)'
        }
      })
    })

    it('reactively updates UI filters when route.query changes externally', async () => {
      const { pinia, router } = setupTestEnvironment()
      router.push('/events')
      await router.isReady()

      const wrapper = mount(EventsCatalogView, {
        global: {
          plugins: [pinia, router]
        }
      })

      expect((wrapper.find('input[type="text"]').element as HTMLInputElement).value).toBe('')

      // Simulate external route change (e.g. browser history navigation)
      await router.push('/events?q=Coldplay')
      await wrapper.vm.$nextTick()

      expect((wrapper.find('input[type="text"]').element as HTMLInputElement).value).toBe('Coldplay')
    })
  })

  describe('Empty State & Reset Filters Action', () => {
    it('shows clean empty state and resets all filters when clicking "Limpiar filtros"', async () => {
      const { pinia, router } = setupTestEnvironment()
      router.push('/events')
      await router.isReady()

      const wrapper = mount(EventsCatalogView, {
        global: {
          plugins: [pinia, router]
        }
      })

      const searchInput = wrapper.find('input[type="text"]')
      await searchInput.setValue('non-existent-artist-12345')
      vi.advanceTimersByTime(300)
      await wrapper.vm.$nextTick()

      expect(wrapper.findAll('article').length).toBe(0)
      expect(wrapper.text()).toContain('No se encontraron espectáculos')

      const clearFilterBtn = wrapper.findAll('button').find(b => b.text().includes('Limpiar filtros'))
      expect(clearFilterBtn).toBeDefined()

      await clearFilterBtn!.trigger('click')
      await wrapper.vm.$nextTick()

      // Filters reset, all 10 events returned
      expect(wrapper.findAll('article').length).toBe(10)
    })
  })
})
