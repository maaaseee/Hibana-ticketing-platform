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
import { useAuthStore } from '../stores/auth'

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
      redirect: '/'
    }
  ],
  scrollBehavior() {
    return { top: 0 }
  }
})

// Control de Acceso: Bloquear acceso a Queue, Mapa de Asientos y Checkout sin autenticación
router.beforeEach((to) => {
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

export default router
