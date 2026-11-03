import { createRouter, createWebHistory } from 'vue-router'
import FrontOfficeLayout from '../layouts/FrontOfficeLayout.vue'
import HomeView from '../views/HomeView.vue'
import EventsCatalogView from '../views/EventsCatalogView.vue'
import EventDetailView from '../views/EventDetailView.vue'
import QueueView from '../views/QueueView.vue'
import SeatSelectionView from '../views/SeatSelectionView.vue'
import CheckoutView from '../views/CheckoutView.vue'
import DashboardTicketsView from '../views/DashboardTicketsView.vue'
import HelpCenterView from '../views/HelpCenterView.vue'
import SupportChatView from '../views/SupportChatView.vue'
import LoginView from '../views/LoginView.vue'
import RegisterView from '../views/RegisterView.vue'
import NotFoundView from '../views/NotFoundView.vue'
import ProfileSettingsView from '../views/ProfileSettingsView.vue'
import { useAuthStore } from '../stores/auth'
import { useLoadingBarStore } from '../stores/loadingBar'
import { setPageMeta } from '../composables/usePageMeta'

const ROUTE_TITLES: Record<string, string> = {
  home: 'Inicio',
  'events-catalog': 'Cartelera de Espectáculos',
  'event-detail': 'Detalle del Show',
  queue: 'Sala de Espera Virtual',
  'seat-selection': 'Selección de Asientos',
  checkout: 'Checkout Seguro',
  tickets: 'Mis Entradas Oficiales',
  'profile-settings': 'Configuración de Perfil',
  help: 'Centro de Ayuda y FAQ',
  'support-chat': 'Soporte en Vivo',
  login: 'Iniciar Sesión',
  register: 'Crear Cuenta',
  'not-found': 'Página No Encontrada (404)'
}

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      component: FrontOfficeLayout,
      children: [
        {
          path: '',
          name: 'home',
          component: HomeView
        },
        {
          path: 'events',
          name: 'events-catalog',
          component: EventsCatalogView
        },
        {
          path: 'event/:id',
          name: 'event-detail',
          component: EventDetailView
        },
        {
          path: 'event/:id/queue',
          name: 'queue',
          component: QueueView,
          meta: {
            hideNavbar: true,
            hideFooter: true,
            fullscreen: true,
            requiresAuth: true
          }
        },
        {
          path: 'event/:id/seats',
          name: 'seat-selection',
          component: SeatSelectionView,
          meta: {
            hideNavbar: true,
            hideFooter: true,
            fullscreen: true,
            requiresAuth: true
          }
        },
        {
          path: 'event/:id/checkout',
          name: 'checkout',
          component: CheckoutView,
          meta: {
            hideNavbar: true,
            hideFooter: true,
            requiresAuth: true
          }
        },
        {
          path: 'tickets',
          name: 'tickets',
          component: DashboardTicketsView,
          meta: {
            requiresAuth: true
          }
        },
        {
          path: 'profile',
          name: 'profile-settings',
          component: ProfileSettingsView,
          meta: {
            requiresAuth: true
          }
        },
        {
          path: 'help',
          name: 'help',
          component: HelpCenterView
        },
        {
          path: 'login',
          name: 'login',
          component: LoginView,
          meta: {
            hideNavbar: true,
            hideFooter: true
          }
        },
        {
          path: 'register',
          name: 'register',
          component: RegisterView,
          meta: {
            hideNavbar: true,
            hideFooter: true
          }
        },
        {
          path: 'support',
          name: 'support-chat',
          component: SupportChatView
        },
        {
          path: 'preview/invoice',
          name: 'preview-invoice',
          component: () => import('../views/preview/InvoicePreviewView.vue'),
          meta: {
            hideNavbar: true,
            hideFooter: true
          }
        },
        {
          path: 'preview/email',
          name: 'preview-email',
          component: () => import('../views/preview/EmailPreviewView.vue'),
          meta: {
            hideNavbar: true,
            hideFooter: true
          }
        }
      ]
    },
    {
      path: '/:pathMatch(.*)*',
      name: 'not-found',
      component: NotFoundView,
      meta: {
        hideNavbar: true,
        hideFooter: true
      }
    }
  ],
  scrollBehavior() {
    return { top: 0 }
  }
})

// Control de Acceso: Bloquear acceso a Queue, Mapa de Asientos y Checkout sin autenticación
router.beforeEach((to) => {
  const loadingBar = useLoadingBarStore()
  loadingBar.start()

  const authStore = useAuthStore()

  // Si intenta ir a login o register estando ya autenticado, redirigir al inicio o redirect
  if ((to.name === 'login' || to.name === 'register') && authStore.isAuthenticated) {
    const redirect = (to.query.redirect as string) || '/'
    return { path: redirect }
  }

  if (to.meta.requiresAuth) {
    if (!authStore.isAuthenticated) {
      authStore.redirectPathAfterLogin = to.fullPath
      return {
        name: 'login',
        query: {
          redirect: to.fullPath,
          notice: 'Debes iniciar sesión para acceder a la sala de espera y selección de asientos.'
        }
      }
    }
  }
})

router.afterEach((to) => {
  const loadingBar = useLoadingBarStore()
  loadingBar.finish()

  const routeName = (to.name as string) || ''
  if (routeName !== 'event-detail') {
    const title = ROUTE_TITLES[routeName]
    setPageMeta(title ? { title } : {})
  }
})

router.onError(() => {
  const loadingBar = useLoadingBarStore()
  loadingBar.fail()
})

export default router
