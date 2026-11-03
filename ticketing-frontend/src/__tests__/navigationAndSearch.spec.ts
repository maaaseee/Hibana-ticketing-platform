import { describe, it, expect, beforeEach, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { createRouter, createMemoryHistory } from 'vue-router'
import { setActivePinia, createPinia } from 'pinia'
import NotFoundView from '../views/NotFoundView.vue'
import GlobalSearchModal from '../components/search/GlobalSearchModal.vue'
import SkipToContent from '../components/ui/SkipToContent.vue'
import AppNavbar from '../components/ui/AppNavbar.vue'
import { catalogService } from '../services/catalogService'
import { mockEvents } from '../data/mockEvents'

function createTestRouter() {
  return createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/', name: 'home', component: { template: '<div>Home</div>' } },
      { path: '/events', name: 'events-catalog', component: { template: '<div>Events</div>' } },
      { path: '/event/:id', name: 'event-detail', component: { template: '<div>Event Detail</div>' } },
      { path: '/help', name: 'help', component: { template: '<div>Help</div>' } },
      { path: '/tickets', name: 'tickets', component: { template: '<div>Tickets</div>' } },
      { path: '/:pathMatch(.*)*', name: 'not-found', component: NotFoundView }
    ]
  })
}

describe('Fase 2: Navegación, Búsqueda Global & Accesibilidad', () => {
  let pinia: ReturnType<typeof createPinia>
  let router: ReturnType<typeof createTestRouter>

  beforeEach(() => {
    pinia = createPinia()
    setActivePinia(pinia)
    router = createTestRouter()
    vi.clearAllMocks()
  })

  describe('Página 404 Dedicada (NotFoundView)', () => {
    it('renders 404 stage message and direct recovery actions', async () => {
      const wrapper = mount(NotFoundView, {
        global: { plugins: [pinia, router] }
      })

      expect(wrapper.text()).toContain('404')
      expect(wrapper.text()).toContain('ESCENARIO')
      expect(wrapper.text()).toContain('APAGADO')
      expect(wrapper.text()).toContain('El espectáculo, fecha o butaca que estás buscando no se encuentra en cartelera')

      // Action links exist
      expect(wrapper.find('a[href="/events"]').exists()).toBe(true)
      expect(wrapper.text()).toContain('Ver Cartelera')
      expect(wrapper.text()).toContain('Buscar Show (⌘K)')
    })
  })

  describe('Buscador Global con Autocompletado (GlobalSearchModal)', () => {
    it('executes debounced query against catalogService and displays results', async () => {
      vi.spyOn(catalogService, 'searchEvents').mockResolvedValue([mockEvents[0]!])

      const wrapper = mount(GlobalSearchModal, {
        props: { open: true },
        attachTo: document.body,
        global: { plugins: [pinia, router] }
      })
      await wrapper.vm.$nextTick()

      // Radix DialogPortal renders content into document.body
      const searchInput = document.body.querySelector('input[type="text"]') as HTMLInputElement
      expect(searchInput).not.toBeNull()

      searchInput.value = 'Neon'
      searchInput.dispatchEvent(new Event('input'))

      // Wait for debounce
      await new Promise(resolve => setTimeout(resolve, 260))
      await wrapper.vm.$nextTick()

      expect(catalogService.searchEvents).toHaveBeenCalledWith('Neon')
      expect(document.body.textContent).toContain(mockEvents[0]!.title)
      expect(document.body.textContent).toContain(mockEvents[0]!.venue)

      wrapper.unmount()
    })
  })

  describe('Accesibilidad (SkipToContent)', () => {
    it('renders an accessible skip link with proper href and focus classes', () => {
      const wrapper = mount(SkipToContent, {
        props: { targetId: 'main-content' }
      })

      const link = wrapper.find('a')
      expect(link.exists()).toBe(true)
      expect(link.attributes('href')).toBe('#main-content')
      expect(link.classes()).toContain('sr-only')
      expect(link.classes()).toContain('focus:not-sr-only')
      expect(link.text()).toBe('Saltar al contenido principal')
    })
  })

  describe('AppNavbar Search Button', () => {
    it('includes the search button with ⌘K shortcut badge in navbar', () => {
      const wrapper = mount(AppNavbar, {
        global: { plugins: [pinia, router] }
      })

      const searchBtn = wrapper.find('button[aria-label="Buscar shows y artistas"]')
      expect(searchBtn.exists()).toBe(true)
      expect(searchBtn.text()).toContain('Buscar')
      expect(searchBtn.text()).toContain('⌘K')
    })
  })
})
