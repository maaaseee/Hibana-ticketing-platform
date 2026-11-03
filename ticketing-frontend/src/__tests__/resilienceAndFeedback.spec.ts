import { describe, it, expect, beforeEach, vi, afterEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { setActivePinia, createPinia } from 'pinia'
import { useToastStore } from '../stores/toast'
import { useLoadingBarStore } from '../stores/loadingBar'
import AppToastContainer from '../components/ui/AppToastContainer.vue'
import OfflineAlertBanner from '../components/ui/OfflineAlertBanner.vue'
import GlobalErrorBoundary from '../components/ui/GlobalErrorBoundary.vue'
import TopProgressBar from '../components/ui/TopProgressBar.vue'
import { defineComponent, h } from 'vue'

describe('Fase 1: Resiliencia & Feedback Inmediato', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  describe('Sistema Global de Toasts (useToastStore & AppToastContainer)', () => {
    it('stores and dismisses toasts of various severities', () => {
      const toastStore = useToastStore()
      expect(toastStore.toasts.length).toBe(0)

      const id1 = toastStore.success('Operación exitosa', 'Entrada reservada')
      const id2 = toastStore.error('Error de red', 'No se pudo conectar')
      const id3 = toastStore.warning('Atención', 'Tiempo crítico')
      const id4 = toastStore.info('Información', 'Nueva fecha disponible')

      expect(toastStore.toasts.length).toBe(4)
      expect(toastStore.toasts[0]?.title).toBe('Operación exitosa')
      expect(toastStore.toasts[1]?.type).toBe('error')

      toastStore.removeToast(id1)
      expect(toastStore.toasts.length).toBe(3)
      expect(toastStore.toasts.some(t => t.id === id1)).toBe(false)

      toastStore.clearAll()
      expect(toastStore.toasts.length).toBe(0)
    })

    it('renders AppToastContainer with Radix UI viewport and displays active toasts', async () => {
      const pinia = createPinia()
      setActivePinia(pinia)
      const toastStore = useToastStore()

      const wrapper = mount(AppToastContainer, {
        global: {
          plugins: [pinia]
        }
      })

      expect(wrapper.text()).toBe('')

      toastStore.success('Pago Aprobado', 'Se emitieron 2 entradas')
      await wrapper.vm.$nextTick()

      expect(wrapper.text()).toContain('Pago Aprobado')
      expect(wrapper.text()).toContain('Se emitieron 2 entradas')
    })
  })

  describe('TopProgressBar & useLoadingBarStore', () => {
    it('progresses from initial percentage to 100 on finish', () => {
      const loadingBar = useLoadingBarStore()
      expect(loadingBar.isLoading).toBe(false)
      expect(loadingBar.progress).toBe(0)

      loadingBar.start()
      expect(loadingBar.isLoading).toBe(true)
      expect(loadingBar.progress).toBeGreaterThanOrEqual(15)

      loadingBar.finish()
      expect(loadingBar.progress).toBe(100)
    })

    it('renders TopProgressBar with glowing gradient', async () => {
      const pinia = createPinia()
      setActivePinia(pinia)
      const loadingBar = useLoadingBarStore()

      const wrapper = mount(TopProgressBar, {
        global: { plugins: [pinia] }
      })

      expect(wrapper.find('div').exists()).toBe(false)

      loadingBar.start()
      await wrapper.vm.$nextTick()

      expect(wrapper.find('.fixed.top-0').exists()).toBe(true)
      expect(wrapper.html()).toContain('bg-gradient-to-r')
    })
  })

  describe('GlobalErrorBoundary', () => {
    it('renders normal slot when no error occurs', () => {
      const wrapper = mount(GlobalErrorBoundary, {
        slots: {
          default: '<div id="safe-content">Contenido Seguro</div>'
        }
      })
      expect(wrapper.find('#safe-content').text()).toBe('Contenido Seguro')
      expect(wrapper.text()).not.toContain('Interferencia Técnica')
    })

    it('catches child render error and presents stadium contingency fallback', async () => {
      const CrashingComponent = defineComponent({
        setup() {
          throw new Error('Fallo crítico simulado en el canvas')
        },
        render() {
          return h('div', 'Crash')
        }
      })

      const wrapper = mount(GlobalErrorBoundary, {
        slots: {
          default: () => h(CrashingComponent)
        }
      })

      await wrapper.vm.$nextTick()

      expect(wrapper.text()).toContain('Interferencia Técnica')
      expect(wrapper.text()).toContain('Fallo crítico simulado en el canvas')
      expect(wrapper.text()).toContain('Reintentar')
      expect(wrapper.text()).toContain('Volver a Cartelera')
    })
  })

  describe('OfflineAlertBanner', () => {
    it('mounts without crashing', () => {
      const wrapper = mount(OfflineAlertBanner)
      expect(wrapper.exists()).toBe(true)
    })
  })
})
