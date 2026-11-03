import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import EventCardSkeleton from '../components/ui/EventCardSkeleton.vue'
import EventsCatalogView from '../views/EventsCatalogView.vue'
import CookieConsentBanner from '../components/ui/CookieConsentBanner.vue'
import { setPageMeta, usePageMeta } from '../composables/usePageMeta'
import { useCatalogStore } from '../stores/catalog'
import { createRouter, createMemoryHistory } from 'vue-router'
import { ref } from 'vue'

describe('Fase 4: Rendimiento, SEO & Legal', () => {
  let router: any

  beforeEach(() => {
    setActivePinia(createPinia())
    localStorage.clear()
    document.title = ''
    // Limpiar meta tags previos
    document.head.querySelectorAll('meta').forEach(el => el.remove())

    router = createRouter({
      history: createMemoryHistory(),
      routes: [
        { path: '/events', name: 'events-catalog', component: EventsCatalogView },
        { path: '/event/:id', name: 'event-detail', component: { template: '<div>Event Detail</div>' } }
      ]
    })
  })

  afterEach(() => {
    vi.restoreAllMocks()
    vi.useRealTimers()
  })

  describe('EventCardSkeleton (Shimmer Loader)', () => {
    it('renderiza la tarjeta esqueleto con animaciones de carga neón para prevenir CLS', () => {
      const wrapper = mount(EventCardSkeleton)

      expect(wrapper.find('[data-testid="event-card-skeleton"]').exists()).toBe(true)
      expect(wrapper.html()).toContain('after:animate-[shimmer_1.8s_infinite]')
      expect(wrapper.html()).toContain('bg-[#142033]')
    })

    it('renderiza el grid de skeletons en EventsCatalogView cuando está cargando sin eventos iniciales', async () => {
      const catalog = useCatalogStore()
      catalog.events = []
      catalog.isLoading = true

      router.push('/events')
      await router.isReady()

      const wrapper = mount(EventsCatalogView, {
        global: {
          plugins: [router]
        }
      })

      expect(wrapper.find('[data-testid="catalog-skeleton-grid"]').exists()).toBe(true)
      const skeletons = wrapper.findAll('[data-testid="event-card-skeleton"]')
      expect(skeletons.length).toBe(6)
    })
  })

  describe('usePageMeta & setPageMeta (SEO & Open Graph Dinámico)', () => {
    it('inyecta title, meta description y tags Open Graph y Twitter Cards correctamente', () => {
      setPageMeta({
        title: 'Roger Waters - Us + Them',
        description: 'Concierto histórico en Estadio Monumental de River Plate.',
        image: 'https://hibana.tk/images/waters.jpg',
        type: 'event'
      })

      expect(document.title).toBe('Roger Waters - Us + Them | Hibana.tk')

      const descMeta = document.querySelector('meta[name="description"]')
      expect(descMeta?.getAttribute('content')).toBe('Concierto histórico en Estadio Monumental de River Plate.')

      const ogTitle = document.querySelector('meta[property="og:title"]')
      expect(ogTitle?.getAttribute('content')).toBe('Roger Waters - Us + Them | Hibana.tk')

      const ogImage = document.querySelector('meta[property="og:image"]')
      expect(ogImage?.getAttribute('content')).toBe('https://hibana.tk/images/waters.jpg')

      const ogType = document.querySelector('meta[property="og:type"]')
      expect(ogType?.getAttribute('content')).toBe('event')

      const twitterCard = document.querySelector('meta[name="twitter:card"]')
      expect(twitterCard?.getAttribute('content')).toBe('summary_large_image')
    })

    it('actualiza reactivamente los metadatos cuando cambia una propiedad', async () => {
      const eventTitle = ref('Show Inicial')

      const TestComponent = {
        setup() {
          usePageMeta(() => ({
            title: eventTitle.value,
            description: `Descripción de ${eventTitle.value}`
          }))
          return {}
        },
        template: '<div>Test Meta</div>'
      }

      const wrapper = mount(TestComponent)
      await wrapper.vm.$nextTick()

      expect(document.title).toBe('Show Inicial | Hibana.tk')

      eventTitle.value = 'Show Final Sold Out'
      await wrapper.vm.$nextTick()

      expect(document.title).toBe('Show Final Sold Out | Hibana.tk')
      wrapper.unmount()
    })
  })

  describe('CookieConsentBanner', () => {
    it('muestra el banner de cookies si no hay consentimiento guardado', async () => {
      vi.useFakeTimers()

      const wrapper = mount(CookieConsentBanner)
      expect(wrapper.find('[data-testid="cookie-consent-banner"]').exists()).toBe(false)

      // Avanzar retardo de apertura amable (700ms)
      vi.advanceTimersByTime(750)
      await wrapper.vm.$nextTick()

      expect(wrapper.find('[data-testid="cookie-consent-banner"]').exists()).toBe(true)
      expect(wrapper.text()).toContain('Tu Privacidad en Hibana')
      expect(wrapper.text()).toContain('Solo Necesarias')
      expect(wrapper.text()).toContain('Aceptar Todas')
    })

    it('guarda solo cookies necesarias al hacer click en Solo Necesarias', async () => {
      vi.useFakeTimers()

      const wrapper = mount(CookieConsentBanner)
      vi.advanceTimersByTime(750)
      await wrapper.vm.$nextTick()

      const btnNecessary = wrapper.find('[data-testid="cookie-accept-necessary"]')
      await btnNecessary.trigger('click')

      const savedConsent = JSON.parse(localStorage.getItem('hibana_cookie_consent') || '{}')
      expect(savedConsent.necessary).toBe(true)
      expect(savedConsent.analytics).toBe(false)
      expect(savedConsent.timestamp).toBeGreaterThan(0)

      await wrapper.vm.$nextTick()
      expect(wrapper.find('[data-testid="cookie-consent-banner"]').exists()).toBe(false)
    })

    it('guarda todas las cookies al hacer click en Aceptar Todas', async () => {
      vi.useFakeTimers()

      const wrapper = mount(CookieConsentBanner)
      vi.advanceTimersByTime(750)
      await wrapper.vm.$nextTick()

      const btnAll = wrapper.find('[data-testid="cookie-accept-all"]')
      await btnAll.trigger('click')

      const savedConsent = JSON.parse(localStorage.getItem('hibana_cookie_consent') || '{}')
      expect(savedConsent.necessary).toBe(true)
      expect(savedConsent.analytics).toBe(true)
    })

    it('no muestra el banner si el usuario ya aceptó previamente', async () => {
      vi.useFakeTimers()
      localStorage.setItem('hibana_cookie_consent', JSON.stringify({ necessary: true, analytics: true }))

      const wrapper = mount(CookieConsentBanner)
      vi.advanceTimersByTime(1000)
      await wrapper.vm.$nextTick()

      expect(wrapper.find('[data-testid="cookie-consent-banner"]').exists()).toBe(false)
    })
  })
})
