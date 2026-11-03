import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import AdminLayout from '@/layouts/AdminLayout.vue'

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'login',
    component: () => import('@/views/LoginView.vue'),
    meta: { public: true },
  },
  {
    path: '/unauthorized',
    name: 'unauthorized',
    component: () => import('@/views/UnauthorizedView.vue'),
    meta: { public: true },
  },
  {
    path: '/',
    component: AdminLayout,
    children: [
      {
        path: '',
        redirect: '/events',
      },
      {
        path: 'events',
        name: 'events-list',
        component: () => import('@/views/EventsListView.vue'),
        meta: { roles: ['ROLE_ADMIN', 'ROLE_OPERATOR'] },
      },
      {
        path: 'events/new',
        name: 'event-create',
        component: () => import('@/views/EventCreateWizardView.vue'),
        meta: { roles: ['ROLE_ADMIN', 'ROLE_OPERATOR'] },
      },
      {
        path: 'venues',
        name: 'venues-list',
        component: () => import('@/views/VenuesListView.vue'),
        meta: { roles: ['ROLE_ADMIN', 'ROLE_OPERATOR'] },
      },
    ],
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: '/events',
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

router.beforeEach(async (to, _from, next) => {
  const authStore = useAuthStore()

  // Initialize store if needed
  if (!authStore.isAuthenticated) {
    await authStore.initialize()
  }

  // Public route
  if (to.meta.public) {
    return next()
  }

  // Check authentication
  if (!authStore.isAuthenticated) {
    return next({ path: '/login', query: { redirect: to.fullPath } })
  }

  // Check RBAC permissions
  const requiredRoles = to.meta.roles as string[] | undefined
  if (requiredRoles && requiredRoles.length > 0) {
    const hasRequiredRole = requiredRoles.some((role) =>
      authStore.userRoles.includes(role as any)
    )
    if (!hasRequiredRole) {
      return next({ path: '/unauthorized' })
    }
  }

  next()
})

export default router
