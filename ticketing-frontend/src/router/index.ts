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
          component: QueueView
        },
        {
          path: 'event/:id/seats',
          name: 'seat-selection',
          component: SeatSelectionView
        },
        {
          path: 'event/:id/checkout',
          name: 'checkout',
          component: CheckoutView
        },
        {
          path: 'tickets',
          name: 'tickets',
          component: DashboardTicketsView
        },
        {
          path: 'help',
          name: 'help',
          component: HelpCenterView
        },
        {
          path: 'support',
          name: 'support-chat',
          component: SupportChatView
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

export default router
