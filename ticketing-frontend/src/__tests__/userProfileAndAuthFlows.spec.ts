import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { createRouter, createMemoryHistory } from 'vue-router'
import ForgotPasswordModal from '../components/auth/ForgotPasswordModal.vue'
import ProfileSettingsView from '../views/ProfileSettingsView.vue'
import { useAuthStore } from '../stores/auth'

describe('Fase 3: Flujos de Usuario & Gestión', () => {
  let router: any

  beforeEach(() => {
    setActivePinia(createPinia())
    localStorage.clear()
    document.body.innerHTML = ''

    router = createRouter({
      history: createMemoryHistory(),
      routes: [
        { path: '/', name: 'home', component: { template: '<div>Home</div>' } },
        { path: '/login', name: 'login', component: { template: '<div>Login</div>' } },
        { path: '/tickets', name: 'tickets', component: { template: '<div>Tickets</div>' } },
        { path: '/preview/invoice', name: 'preview-invoice', component: { template: '<div>Invoice</div>' } },
        { path: '/profile', name: 'profile-settings', component: ProfileSettingsView }
      ]
    })
  })

  afterEach(() => {
    document.body.innerHTML = ''
    vi.restoreAllMocks()
    vi.useRealTimers()
  })

  describe('ForgotPasswordModal', () => {
    it('muestra el modal y permite enviar el correo para recuperación', async () => {
      vi.useFakeTimers()

      const wrapper = mount(ForgotPasswordModal, {
        props: {
          open: true,
          initialEmail: 'test@hibana.tk'
        },
        attachTo: document.body
      })
      await wrapper.vm.$nextTick()

      const emailInput = document.body.querySelector('input[type="email"]') as HTMLInputElement
      expect(emailInput).not.toBeNull()
      expect(emailInput.value).toBe('test@hibana.tk')

      // Avanzar al paso 2
      const form = document.body.querySelector('form') as HTMLFormElement
      expect(form).not.toBeNull()
      form.dispatchEvent(new Event('submit'))

      // Avanzar el timer de simulación de envío
      vi.advanceTimersByTime(900)
      await wrapper.vm.$nextTick()

      // Verificar que el paso 2 se activó
      expect(document.body.textContent).toContain('Código de verificación')
      expect(document.body.textContent).toContain('Nueva contraseña')
      wrapper.unmount()
    })

    it('emite evento passwordResetSuccess al restablecer la contraseña exitosamente', async () => {
      vi.useFakeTimers()

      const wrapper = mount(ForgotPasswordModal, {
        props: {
          open: true,
          initialEmail: 'test@hibana.tk'
        },
        attachTo: document.body
      })
      await wrapper.vm.$nextTick()

      // Paso 1: Enviar código
      const form1 = document.body.querySelector('form') as HTMLFormElement
      form1.dispatchEvent(new Event('submit'))
      vi.advanceTimersByTime(900)
      await wrapper.vm.$nextTick()

      // Llenar campos del paso 2
      const codeInput = document.body.querySelector('input[placeholder="000000"]') as HTMLInputElement
      const pwdInput = document.body.querySelector('input[placeholder="Ingresá tu nueva contraseña"]') as HTMLInputElement
      expect(codeInput).not.toBeNull()
      expect(pwdInput).not.toBeNull()

      codeInput.value = '849201'
      codeInput.dispatchEvent(new Event('input'))

      pwdInput.value = 'StadiuM2026!'
      pwdInput.dispatchEvent(new Event('input'))
      await wrapper.vm.$nextTick()

      const form2 = document.body.querySelector('form') as HTMLFormElement
      form2.dispatchEvent(new Event('submit'))
      vi.advanceTimersByTime(900)
      await wrapper.vm.$nextTick()

      expect(wrapper.emitted('passwordResetSuccess')).toBeTruthy()
      expect(wrapper.emitted('passwordResetSuccess')?.[0]).toEqual([
        { email: 'test@hibana.tk', newPassword: 'StadiuM2026!' }
      ])
      wrapper.unmount()
    })
  })

  describe('useAuthStore - Perfil y Tarjetas', () => {
    it('actualiza el perfil del usuario autenticado', () => {
      const auth = useAuthStore()
      auth.loginWithMock()

      expect(auth.user?.name).toBe('Carlos Mendoza')

      const updated = auth.updateProfile({
        name: 'Carlos Mendoza Jr.',
        phone: '+54 11 9999-8888'
      })

      expect(updated).toBe(true)
      expect(auth.user?.name).toBe('Carlos Mendoza Jr.')
      expect(auth.user?.phone).toBe('+54 11 9999-8888')
    })

    it('administra tarjetas guardadas (agregar y desvincular)', () => {
      const auth = useAuthStore()
      auth.loginWithMock()

      expect(auth.user?.savedCards?.length).toBe(1)
      const initialCardId = auth.user!.savedCards![0]!.id

      // Agregar tarjeta
      const newCard = auth.addSavedCard({
        brand: 'Mastercard',
        last4: '9876',
        expMonth: '11',
        expYear: '27'
      })
      expect(newCard).not.toBeNull()
      expect(auth.user?.savedCards?.length).toBe(2)

      // Eliminar tarjeta original
      auth.removeSavedCard(initialCardId)
      expect(auth.user?.savedCards?.length).toBe(1)
      expect(auth.user?.savedCards?.[0]?.brand).toBe('Mastercard')
    })
  })

  describe('ProfileSettingsView', () => {
    it('renderiza la información del usuario autenticado y permite actualizar datos', async () => {
      const auth = useAuthStore()
      auth.loginWithMock()

      const wrapper = mount(ProfileSettingsView, {
        global: {
          plugins: [router]
        }
      })

      expect(wrapper.text()).toContain('Carlos Mendoza')
      expect(wrapper.text()).toContain('carlos.m@hibana.tk')
      expect(wrapper.text()).toContain('DNI Argentino')
      expect(wrapper.text()).toContain('Datos Personales')
      expect(wrapper.text()).toContain('Seguridad & Contraseña')
      expect(wrapper.text()).toContain('Métodos de Pago & Facturación')
    })

    it('desvincula tarjeta guardada al presionar el botón de eliminar', async () => {
      const auth = useAuthStore()
      auth.loginWithMock()

      const wrapper = mount(ProfileSettingsView, {
        global: {
          plugins: [router]
        }
      })

      expect(wrapper.text()).toContain('Visa')
      expect(wrapper.text()).toContain('4242')

      const deleteBtn = wrapper.find('button[title="Desvincular tarjeta"]')
      expect(deleteBtn.exists()).toBe(true)
      await deleteBtn.trigger('click')

      expect(auth.user?.savedCards?.length).toBe(0)
    })
  })
})
