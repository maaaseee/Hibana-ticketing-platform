import { describe, it, expect, beforeEach, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { createRouter, createMemoryHistory } from 'vue-router'
import { SelectRoot, CheckboxRoot } from 'radix-vue'
import { useAuthStore, MOCK_USER } from '../stores/auth'
import AppNavbar from '../components/ui/AppNavbar.vue'
import AppLogo from '../components/ui/AppLogo.vue'
import BirthDatePicker from '../components/ui/BirthDatePicker.vue'
import LoginView from '../views/LoginView.vue'
import RegisterView from '../views/RegisterView.vue'

function createTestNavbarRouter() {
  return createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/', name: 'home', component: { template: '<div>Home</div>' } },
      { path: '/events', name: 'events-catalog', component: { template: '<div>Events</div>' } },
      { path: '/help', name: 'help', component: { template: '<div>Help</div>' } },
      { path: '/tickets', name: 'tickets', component: { template: '<div>Tickets</div>' } },
      { path: '/login', name: 'login', component: LoginView },
      { path: '/register', name: 'register', component: RegisterView }
    ]
  })
}

describe('Authentication & Dedicated Auth Pages (AppNavbar, AppLogo, LoginView, RegisterView, BirthDatePicker)', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    localStorage.clear()
    sessionStorage.clear()
    vi.clearAllMocks()
  })

  describe('useAuthStore functionality', () => {
    it('initializes with unauthenticated user when localStorage is empty', () => {
      const auth = useAuthStore()
      expect(auth.isAuthenticated).toBe(false)
      expect(auth.user).toBeNull()
      expect(auth.isProfileSidebarOpen).toBe(false)
    })

    it('logs in with mock credentials via loginWithMock()', () => {
      const auth = useAuthStore()
      auth.loginWithMock()

      expect(auth.isAuthenticated).toBe(true)
      expect(auth.user?.name).toBe('Carlos Mendoza')
      expect(auth.user?.email).toBe('carlos.m@hibana.tk')
      expect(localStorage.getItem('hibana_auth_user')).toContain('Carlos Mendoza')
    })

    it('allows registering a new user', () => {
      const auth = useAuthStore()
      auth.register({
        name: 'Ana Valenzuela',
        email: 'ana@hibana.tk',
        documentNumber: '40.123.456',
        password: 'password123'
      })

      expect(auth.isAuthenticated).toBe(true)
      expect(auth.user?.name).toBe('Ana Valenzuela')
      expect(auth.user?.email).toBe('ana@hibana.tk')
      expect(auth.user?.documentNumber).toBe('40.123.456')
    })

    it('clears user and storage on logout', () => {
      const auth = useAuthStore()
      auth.loginWithMock()
      expect(auth.isAuthenticated).toBe(true)

      auth.logout()
      expect(auth.isAuthenticated).toBe(false)
      expect(auth.user).toBeNull()
      expect(localStorage.getItem('hibana_auth_user')).toBeNull()
    })
  })

  describe('Unified AppLogo Component', () => {
    it('renders unified AppLogo component with flame spark and brand typography', () => {
      const router = createTestNavbarRouter()
      const wrapper = mount(AppLogo, {
        props: { size: 'md', variant: 'standard' },
        global: { plugins: [router] }
      })

      expect(wrapper.text()).toContain('Hibana.tk')
      const img = wrapper.find('img[alt="Hibana.tk Logo"]')
      expect(img.exists()).toBe(true)
      expect(img.attributes('src')).toBe('/logo.png')
    })

    it('renders AppLogo as button when asButton is true', async () => {
      const router = createTestNavbarRouter()
      let clicked = false
      const wrapper = mount(AppLogo, {
        props: { asButton: true },
        attrs: { onClick: () => { clicked = true } },
        global: { plugins: [router] }
      })

      const btn = wrapper.find('button')
      expect(btn.exists()).toBe(true)
      await btn.trigger('click')
      expect(clicked).toBe(true)
    })
  })

  describe('AppNavbar Navigation & Layout', () => {
    it('deletes the notifications button completely and uses unified AppLogo', () => {
      const localRouter = createTestNavbarRouter()

      const wrapper = mount(AppNavbar, {
        global: {
          plugins: [localRouter]
        }
      })

      // The notifications button must be completely absent
      const notifBtn = wrapper.find('[aria-label="Notificaciones"]')
      expect(notifBtn.exists()).toBe(false)
      expect(wrapper.text()).not.toContain('Notificaciones')

      // AppLogo is used
      expect(wrapper.findComponent(AppLogo).exists()).toBe(true)
    })

    it('displays Iniciar Sesión and Crear Cuenta links pointing to dedicated views when user is not logged in', () => {
      const localRouter = createTestNavbarRouter()

      const wrapper = mount(AppNavbar, {
        global: {
          plugins: [localRouter]
        }
      })

      const auth = useAuthStore()
      expect(auth.isAuthenticated).toBe(false)

      expect(wrapper.text()).toContain('Iniciar Sesión')
      expect(wrapper.text()).toContain('Crear Cuenta')

      // Verify RouterLinks exist
      const loginLink = wrapper.findAll('a').find(a => a.text().includes('Iniciar Sesión'))
      const registerLink = wrapper.findAll('a').find(a => a.text().includes('Crear Cuenta'))
      expect(loginLink?.attributes('href')).toBe('/login')
      expect(registerLink?.attributes('href')).toBe('/register')

      // No user profile avatar should be displayed
      expect(wrapper.find('[aria-label="Abrir perfil de usuario"]').exists()).toBe(false)
    })

    it('opens Profile Sidebar (and not a dropdown) when clicking the user profile button', async () => {
      const localRouter = createTestNavbarRouter()

      const auth = useAuthStore()
      auth.loginWithMock()
      expect(auth.isAuthenticated).toBe(true)

      const wrapper = mount(AppNavbar, {
        global: {
          plugins: [localRouter]
        }
      })

      // Profile avatar button must be visible
      const profileBtn = wrapper.find('[aria-label="Abrir perfil de usuario"]')
      expect(profileBtn.exists()).toBe(true)
      expect(profileBtn.text()).toContain('Carlos')

      // Clicking it opens the sidebar, not a dropdown
      expect(auth.isProfileSidebarOpen).toBe(false)
      await profileBtn.trigger('click')
      expect(auth.isProfileSidebarOpen).toBe(true)
    })
  })

  describe('Route Access Control (Blocking unauthenticated users)', () => {
    it('blocks access to queue and seat-selection when not authenticated and redirects to /login page', async () => {
      const auth = useAuthStore()
      expect(auth.isAuthenticated).toBe(false)

      const testRouter = createRouter({
        history: createMemoryHistory(),
        routes: [
          { path: '/', name: 'home', component: { template: '<div>Home</div>' } },
          { path: '/event/:id/seats', name: 'seat-selection', component: { template: '<div>Seats</div>' }, meta: { requiresAuth: true } },
          { path: '/event/:id/queue', name: 'queue', component: { template: '<div>Queue</div>' }, meta: { requiresAuth: true } },
          { path: '/login', name: 'login', component: LoginView }
        ]
      })

      testRouter.beforeEach((to) => {
        if (to.meta.requiresAuth && !auth.isAuthenticated) {
          auth.redirectPathAfterLogin = to.fullPath
          return {
            name: 'login',
            query: {
              redirect: to.fullPath,
              notice: 'Debes iniciar sesión para acceder a la sala de espera y selección de asientos.'
            }
          }
        }
      })

      // Attempt to access seat-selection directly
      await testRouter.push('/event/ado-hibana-world-tour/seats')

      // Router guard blocks navigation and redirects to login view
      expect(testRouter.currentRoute.value.name).toBe('login')
      expect(testRouter.currentRoute.value.query.redirect).toBe('/event/ado-hibana-world-tour/seats')
      expect(testRouter.currentRoute.value.query.notice).toContain('Debes iniciar sesión')
    })

    it('allows access to queue and seat-selection once user logs in', async () => {
      const auth = useAuthStore()
      auth.loginWithMock()
      expect(auth.isAuthenticated).toBe(true)

      const testRouter = createRouter({
        history: createMemoryHistory(),
        routes: [
          { path: '/', name: 'home', component: { template: '<div>Home</div>' } },
          { path: '/event/:id/seats', name: 'seat-selection', component: { template: '<div>Seats</div>' }, meta: { requiresAuth: true } }
        ]
      })

      testRouter.beforeEach((to) => {
        if (to.meta.requiresAuth && !auth.isAuthenticated) {
          return false
        }
      })

      await testRouter.push('/event/ado-hibana-world-tour/seats')
      expect(testRouter.currentRoute.value.name).toBe('seat-selection')
    })
  })

  describe('Dedicated Auth Views (LoginView & RegisterView)', () => {
    it('renders LoginView matching wireframe with unified AppLogo, no pill, credentials form, Radix Checkbox, and quick demo login', async () => {
      const router = createTestNavbarRouter()
      await router.push('/login')

      const wrapper = mount(LoginView, {
        global: {
          plugins: [router]
        }
      })

      // AppLogo is used
      expect(wrapper.findComponent(AppLogo).exists()).toBe(true)

      // Pill on top of big title must be deleted
      expect(wrapper.text()).not.toContain('Portal Oficial • Shows en Vivo')

      // Big bold headline from wireframe
      expect(wrapper.text()).toContain('SE')
      expect(wrapper.text()).toContain('SIENTE')
      expect(wrapper.text()).toContain('Ingresá a tu cuenta')
      expect(wrapper.text()).toContain('Registrate')
      expect(wrapper.text()).toContain('Olvidé mi contraseña')
      expect(wrapper.text()).toContain('Mantener la sesión iniciada')

      // Background image element with auth_bg.jpg
      const bgImg = wrapper.find('img[alt="Concierto de fondo"]')
      expect(bgImg.exists()).toBe(true)

      // Radix UI Checkbox for remember session
      expect(wrapper.findComponent(CheckboxRoot).exists()).toBe(true)

      // Inputs for email and password
      const emailInput = wrapper.find('input[type="email"]')
      const passwordInput = wrapper.find('input[placeholder*="contraseña"]')
      expect(emailInput.exists()).toBe(true)
      expect(passwordInput.exists()).toBe(true)

      // Quick Demo button
      const demoBtn = wrapper.find('button[title*="Carlos Mendoza"]')
      expect(demoBtn.exists()).toBe(true)
      await demoBtn.trigger('click')

      const auth = useAuthStore()
      expect(auth.isAuthenticated).toBe(true)
      expect(auth.user?.name).toBe('Carlos Mendoza')
    })

    it('renders RegisterView with 3 steps without size change, Radix Selects, BirthDatePicker, and Step 3 live password checklist', async () => {
      const router = createTestNavbarRouter()
      await router.push('/register')

      const wrapper = mount(RegisterView, {
        global: {
          plugins: [router]
        }
      })

      // AppLogo is used
      expect(wrapper.findComponent(AppLogo).exists()).toBe(true)

      // Pill on top of big title must be deleted
      expect(wrapper.text()).not.toContain('Registro Oficial de Espectadores')

      // Headline from wireframe
      expect(wrapper.text()).toContain('ESTÁS A UN PASO DE TUS SHOWS FAVORITOS')
      expect(wrapper.text()).toContain('REGISTRATE')

      // Stepper from wireframe with all 3 steps
      expect(wrapper.text()).toContain('1. Identificate')
      expect(wrapper.text()).toContain('2. Validá')
      expect(wrapper.text()).toContain('3. Creá tu cuenta')

      // Background image element with auth_bg.jpg
      const bgImg = wrapper.find('img[alt="Concierto de fondo"]')
      expect(bgImg.exists()).toBe(true)

      // Container has fixed height class so it doesn't change size between steps
      const cardContainer = wrapper.find('.max-w-lg')
      expect(cardContainer.classes()).toContain('h-[690px]')

      // ================= PASO 1: Identificate (Solo documentación DNI con Radix Selects) =================
      const dniInput = wrapper.find('input[placeholder*="DNI"]')
      expect(dniInput.exists()).toBe(true)

      // Radix UI Selects for document type and gender
      const radixSelects = wrapper.findAllComponents(SelectRoot)
      expect(radixSelects.length).toBe(2)

      // Account fields (email, password) must NOT be present in Step 1
      expect(wrapper.find('input[type="email"]').exists()).toBe(false)
      expect(wrapper.find('input[placeholder*="contraseña"]').exists()).toBe(false)

      // Fill DNI and Gender
      await dniInput.setValue('42987654')
      await radixSelects[1]!.vm.$emit('update:modelValue', 'Femenino')

      // Click "Siguiente" -> Moves to Step 2 (Validá)
      await wrapper.find('form').trigger('submit.prevent')

      // ================= PASO 2: Validá (Validación de DNI + BirthDatePicker) =================
      expect(wrapper.text()).toContain('VALIDÁ TU IDENTIDAD')
      expect(wrapper.text()).toContain('Número de trámite del DNI')
      const tramiteInput = wrapper.find('input[placeholder*="11 dígitos"]')
      expect(tramiteInput.exists()).toBe(true)

      // BirthDatePicker component exists
      const datePicker = wrapper.findComponent(BirthDatePicker)
      expect(datePicker.exists()).toBe(true)

      // Mandatory validation check: Attempting to advance without birthDate triggers error
      await tramiteInput.setValue('00123456789')
      await wrapper.find('form').trigger('submit.prevent')
      expect(wrapper.text()).toContain('Por favor seleccioná tu fecha de nacimiento (campo obligatorio).')

      // Now set birth date and advance successfully to Step 3
      await datePicker.vm.$emit('update:modelValue', '1998-05-14')
      await wrapper.find('form').trigger('submit.prevent')

      // ================= PASO 3: Creá tu cuenta (Datos + Password Checklist + Radix Checkbox) =================
      expect(wrapper.text()).toContain('CREÁ TU CUENTA')
      expect(wrapper.find('input[placeholder*="María Florencia Rossi"]').exists()).toBe(true)
      expect(wrapper.find('input[type="email"]').exists()).toBe(true)

      const passwordInput = wrapper.find('input[placeholder*="contraseña"]')
      expect(passwordInput.exists()).toBe(true)

      // Password checklist is visible
      expect(wrapper.text()).toContain('Tu contraseña debe contener:')
      expect(wrapper.text()).toContain('Al menos 8 caracteres')
      expect(wrapper.text()).toContain('Una letra mayúscula (A-Z)')
      expect(wrapper.text()).toContain('Una letra minúscula (a-z)')
      expect(wrapper.text()).toContain('Al menos un número (0-9)')
      expect(wrapper.text()).toContain('Un símbolo o carácter especial')

      // Initially with empty password, criteria are NOT met
      expect(wrapper.findAll('.criterion-item.is-met').length).toBe(0)

      // Typing a partial password (only lowercase): only lowercase turns green
      await passwordInput.setValue('lucia')
      expect((passwordInput.element as HTMLInputElement).value).toBe('lucia')
      const greenChecks1 = wrapper.findAll('.criterion-item.is-met')
      expect(greenChecks1.length).toBe(1) // only lowercase

      // Typing more: lowercase + numbers
      await passwordInput.setValue('lucia2026')
      const greenChecks2 = wrapper.findAll('.criterion-item.is-met')
      expect(greenChecks2.length).toBe(3) // length (9 >= 8) + lowercase + number

      // Typing complete valid password (uppercase + special character)
      await passwordInput.setValue('Lucia2026!')
      const greenChecks3 = wrapper.findAll('.criterion-item.is-met')
      // All 5 criteria marked in green
      expect(greenChecks3.length).toBe(5)

      // Radix Checkbox for terms
      const checkbox = wrapper.findComponent(CheckboxRoot)
      expect(checkbox.exists()).toBe(true)
      await checkbox.vm.$emit('update:checked', true)

      // Fill name and email
      await wrapper.find('input[placeholder*="María Florencia Rossi"]').setValue('Lucía Fontana')
      await wrapper.find('input[type="email"]').setValue('lucia@hibana.tk')

      // Submit registration
      await wrapper.find('form').trigger('submit.prevent')

      const auth = useAuthStore()
      expect(auth.isAuthenticated).toBe(true)
      expect(auth.user?.name).toBe('Lucía Fontana')
      expect(auth.user?.email).toBe('lucia@hibana.tk')
    })
  })
})
