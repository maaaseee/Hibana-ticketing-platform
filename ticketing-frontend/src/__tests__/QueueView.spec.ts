import { describe, it, expect, beforeEach, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { createRouter, createMemoryHistory } from 'vue-router'
import { setActivePinia, createPinia } from 'pinia'
import QueueView from '../views/QueueView.vue'
import { useQueueStore } from '../stores/queue'

function setupTestEnvironment(initialRoute = '/event/chronos-fest-2026/queue') {
  const pinia = createPinia()
  setActivePinia(pinia)

  const router = createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/', name: 'home', component: { template: '<div>Home</div>' } },
      { path: '/event/:id/queue', name: 'queue', component: QueueView },
      { path: '/event/:id/seats', name: 'seat-selection', component: { template: '<div>Seat Selection</div>' } }
    ]
  })

  router.push(initialRoute)

  return { pinia, router }
}

describe('QueueView.spec.ts - Virtual Waiting Room Wireframe Implementation', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('renders the waiting room wireframe structure with position and estimated time', async () => {
    const { pinia, router } = setupTestEnvironment('/event/chronos-fest-2026/queue')
    await router.isReady()

    const wrapper = mount(QueueView, {
      global: {
        plugins: [pinia, router]
      }
    })
    await wrapper.vm.$nextTick()

    const queueStore = useQueueStore(pinia)
    expect(wrapper.text()).toContain('Estás en la fila')
    expect(wrapper.text()).toContain('Tu posición')
    expect(wrapper.text()).toContain(queueStore.currentPosition.toLocaleString('es-AR'))
    expect(wrapper.text()).toContain('Tiempo estimado:')
    expect(wrapper.find('[data-testid="queue-logo-home-btn"]').exists()).toBe(true)
  })

  it('triggers exit warning dialog when clicking logo, and leaves queue when confirmed', async () => {
    const { pinia, router } = setupTestEnvironment('/event/chronos-fest-2026/queue')
    await router.isReady()

    const pushSpy = vi.spyOn(router, 'push')

    const wrapper = mount(QueueView, {
      global: {
        plugins: [pinia, router]
      }
    })

    const queueStore = useQueueStore()
    const leaveSpy = vi.spyOn(queueStore, 'leaveQueue')

    const logoBtn = wrapper.find('[data-testid="queue-logo-home-btn"]')
    await logoBtn.trigger('click')

    // Radix dialog opens in body or content
    expect(document.body.innerHTML).toContain('¿Deseas salir de la fila virtual?')
    expect(document.body.innerHTML).toContain('perderás tu lugar en la fila')

    // Find and click the confirmation button in the dialog portal
    const confirmBtn = document.querySelector('[data-testid="confirm-exit-queue-btn"]') as HTMLButtonElement
    expect(confirmBtn).toBeTruthy()
    confirmBtn.click()

    expect(leaveSpy).toHaveBeenCalled()
    expect(pushSpy).toHaveBeenCalledWith({ name: 'home' })
  })

  it('displays admission state and button to seat-selection when checkoutToken is issued', async () => {
    const { pinia, router } = setupTestEnvironment('/event/chronos-fest-2026/queue')
    await router.isReady()

    const pushSpy = vi.spyOn(router, 'push')

    const wrapper = mount(QueueView, {
      global: {
        plugins: [pinia, router]
      }
    })

    const queueStore = useQueueStore()
    queueStore.grantCheckoutToken()
    await wrapper.vm.$nextTick()

    expect(wrapper.text()).toContain('¡Es tu turno!')
    expect(wrapper.text()).toContain('Token Emitido')
    expect(wrapper.text()).toContain('Seleccionar mis butacas')

    const proceedBtn = wrapper.find('button.bg-btn-buy')
    expect(proceedBtn.exists()).toBe(true)
    await proceedBtn.trigger('click')

    expect(pushSpy).toHaveBeenCalledWith({
      name: 'seat-selection',
      params: { id: 'chronos-fest-2026' }
    })
  })
})
