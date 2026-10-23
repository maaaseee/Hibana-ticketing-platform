import { describe, it, expect, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { createRouter, createMemoryHistory } from 'vue-router'
import { setActivePinia, createPinia } from 'pinia'
import HelpCenterView from '../views/HelpCenterView.vue'

function setupTestEnvironment() {
  const pinia = createPinia()
  setActivePinia(pinia)

  const router = createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/help', name: 'help', component: HelpCenterView },
      { path: '/support', name: 'support-chat', component: { template: '<div>Support Chat</div>' } },
      { path: '/tickets', name: 'tickets', component: { template: '<div>My Tickets</div>' } }
    ]
  })

  return { pinia, router }
}

describe('HelpCenterView.spec.ts - Refined User-Centric FAQ & Clean UI', () => {
  let router: ReturnType<typeof setupTestEnvironment>['router']
  let pinia: ReturnType<typeof setupTestEnvironment>['pinia']

  beforeEach(async () => {
    const env = setupTestEnvironment()
    router = env.router
    pinia = env.pinia
    router.push('/help')
    await router.isReady()
  })

  it('renders clean hero title and search bar, omitting removed clutter', () => {
    const wrapper = mount(HelpCenterView, {
      global: { plugins: [pinia, router] }
    })

    // Main title must be present
    expect(wrapper.text()).toContain('¿En qué podemos ayudarte hoy?')
    expect(wrapper.find('input[placeholder*="Buscar por término"]').exists()).toBe(true)

    // Elements explicitly removed per user request:
    expect(wrapper.text()).not.toContain('Portal de Auto-Servicio Amaryllis')
    expect(wrapper.text()).not.toContain('Encuentra respuestas inmediatas sobre tus entradas')
    expect(wrapper.text()).not.toContain('Consultas habituales:')
    expect(wrapper.text()).not.toContain('Garantía Oficial Anti-Fraude')
    expect(wrapper.text()).not.toContain('Soporte Prioritario en Eventos')
    expect(wrapper.text()).not.toContain('Acceso Garantizado Offline')
  })

  it('renders rich set of friendly, attendee-oriented questions without programmer jargon', () => {
    const wrapper = mount(HelpCenterView, {
      global: { plugins: [pinia, router] }
    })

    // Check humanized questions
    expect(wrapper.text()).toContain('¿Cómo y cuándo recibo mis entradas después de comprar?')
    expect(wrapper.text()).toContain('¿Por qué mi código QR cambia continuamente en la pantalla?')
    expect(wrapper.text()).toContain('¿Cómo puedo transferir una entrada a un amigo o familiar?')
    expect(wrapper.text()).toContain('¿Qué significa el tiempo límite de 10 minutos al elegir asientos o pagar?')
    expect(wrapper.text()).toContain('¿Cómo funciona la fila virtual cuando hay mucha demanda en un evento?')
    expect(wrapper.text()).toContain('¿Puedo ingresar al evento si me quedo sin internet o sin señal en el móvil?')
    expect(wrapper.text()).toContain('¿Qué ocurre si un evento se cancela o cambia de fecha?')
    expect(wrapper.text()).toContain('¿Cuáles son los métodos de pago aceptados?')
    expect(wrapper.text()).toContain('¿Cuántas entradas puedo comprar por persona?')
    expect(wrapper.text()).toContain('¿Los niños o menores de edad pagan entrada?')
    expect(wrapper.text()).toContain('¿Qué hago si veo el cobro en mi banco pero no aparecen mis entradas?')

    // Verify programmer jargon is replaced with plain human language
    expect(wrapper.text()).not.toContain('algoritmo Redlock')
    expect(wrapper.text()).not.toContain('Sorted Sets FIFO')
    expect(wrapper.text()).not.toContain('Saga workflow')
    expect(wrapper.text()).not.toContain('X-Checkout-Token')
  })

  it('animates and toggles accordion items open and closed', async () => {
    const wrapper = mount(HelpCenterView, {
      global: { plugins: [pinia, router] }
    })

    const firstFaqItem = wrapper.find('#faq-tickets-delivery')
    expect(firstFaqItem.exists()).toBe(true)

    // First item is open by default
    const gridContainer = firstFaqItem.find('.grid')
    expect(gridContainer.classes()).toContain('grid-rows-[1fr]')

    // Click button to collapse
    const toggleButton = firstFaqItem.find('button')
    await toggleButton.trigger('click')
    expect(firstFaqItem.find('.grid').classes()).toContain('grid-rows-[0fr]')

    // Click button again to expand
    await toggleButton.trigger('click')
    expect(firstFaqItem.find('.grid').classes()).toContain('grid-rows-[1fr]')
  })

  it('filters questions dynamically by search input and handles empty results', async () => {
    const wrapper = mount(HelpCenterView, {
      global: { plugins: [pinia, router] }
    })

    const searchInput = wrapper.find('input[type="text"]')
    await searchInput.setValue('niños')

    expect(wrapper.text()).toContain('¿Los niños o menores de edad pagan entrada?')
    expect(wrapper.text()).not.toContain('¿Cómo funciona la fila virtual')

    // Search query with no match
    await searchInput.setValue('busqueda_sin_coincidencias_xyz')
    expect(wrapper.text()).toContain('No encontramos respuestas para "busqueda_sin_coincidencias_xyz"')

    // Clear search
    const resetBtn = wrapper.find('button.font-label-sm')
    if (resetBtn.exists()) {
      await resetBtn.trigger('click')
      expect(wrapper.text()).toContain('¿Cómo y cuándo recibo mis entradas después de comprar?')
    }
  })

  it('renders the support escalation banner linking to support chat and tickets', () => {
    const wrapper = mount(HelpCenterView, {
      global: { plugins: [pinia, router] }
    })

    expect(wrapper.text()).toContain('¿No encontraste lo que buscabas o necesitas más ayuda?')
    expect(wrapper.text()).toContain('Iniciar chat de soporte')
    expect(wrapper.text()).toContain('Ver mis tickets (3)')
  })
})
