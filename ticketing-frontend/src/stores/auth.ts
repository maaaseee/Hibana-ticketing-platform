import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { Router } from 'vue-router'

export interface SavedCard {
  id: string
  last4: string
  brand: string
  expMonth: string
  expYear: string
}

export interface AuthUser {
  id: string
  name: string
  email: string
  documentNumber?: string
  phone?: string
  avatarUrl?: string
  role?: 'ROLE_USER' | 'ROLE_ADMIN' | 'ROLE_OPERATOR'
  savedCards?: SavedCard[]
}

export const MOCK_USER: AuthUser = {
  id: 'usr_carlos_mendoza',
  name: 'Carlos Mendoza',
  email: 'carlos.m@hibana.tk',
  documentNumber: '38.452.190',
  phone: '+54 11 4899-2310',
  avatarUrl: 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&w=100&q=80',
  role: 'ROLE_USER',
  savedCards: [
    {
      id: 'card_demo_1',
      brand: 'Visa',
      last4: '4242',
      expMonth: '08',
      expYear: '29'
    }
  ]
}

const STORAGE_KEY = 'hibana_auth_user'

function getSavedUser(): AuthUser | null {
  if (typeof window === 'undefined') return null
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    if (!raw) return null
    return JSON.parse(raw) as AuthUser
  } catch {
    return null
  }
}

export const useAuthStore = defineStore('auth', () => {
  const user = ref<AuthUser | null>(getSavedUser())
  const isAuthenticated = computed(() => user.value !== null)

  const isAuthModalOpen = ref<boolean>(false)
  const authModalTab = ref<'login' | 'register'>('login')
  const redirectPathAfterLogin = ref<string | null>(null)
  const authNoticeMessage = ref<string | null>(null)

  const isProfileSidebarOpen = ref<boolean>(false)

  function login(email: string, _password?: string, routerInstance?: Router): boolean {
    // Normalizar email o aceptar credenciales de demo
    let loggedUser: AuthUser
    if (email.toLowerCase().includes('carlos') || email === MOCK_USER.email) {
      loggedUser = { ...MOCK_USER }
    } else {
      const derivedName = email.split('@')[0]?.replace(/[._]/g, ' ') || 'Usuario Hibana'
      const capitalized = derivedName
        .split(' ')
        .map(w => w.charAt(0).toUpperCase() + w.slice(1))
        .join(' ')
      loggedUser = {
        id: `usr_${Date.now().toString(36)}`,
        name: capitalized,
        email,
        documentNumber: '35.120.456',
        avatarUrl: 'https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?auto=format&fit=crop&w=100&q=80',
        role: 'ROLE_USER'
      }
    }

    user.value = loggedUser
    if (typeof window !== 'undefined') {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(loggedUser))
    }

    isAuthModalOpen.value = false
    authNoticeMessage.value = null

    // Redirigir si había una ruta pendiente
    if (redirectPathAfterLogin.value && routerInstance) {
      const target = redirectPathAfterLogin.value
      redirectPathAfterLogin.value = null
      routerInstance.push(target)
    }

    return true
  }

  function loginWithMock(routerInstance?: Router): boolean {
    return login(MOCK_USER.email, 'password123', routerInstance)
  }

  function register(
    data: { name: string; email: string; documentNumber?: string; password?: string },
    routerInstance?: Router
  ): boolean {
    const newUser: AuthUser = {
      id: `usr_${Date.now().toString(36)}`,
      name: data.name.trim() || 'Nuevo Usuario',
      email: data.email.trim(),
      documentNumber: data.documentNumber?.trim() || '39.812.900',
      avatarUrl: 'https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?auto=format&fit=crop&w=100&q=80',
      role: 'ROLE_USER'
    }

    user.value = newUser
    if (typeof window !== 'undefined') {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(newUser))
    }

    isAuthModalOpen.value = false
    authNoticeMessage.value = null

    if (redirectPathAfterLogin.value && routerInstance) {
      const target = redirectPathAfterLogin.value
      redirectPathAfterLogin.value = null
      routerInstance.push(target)
    }

    return true
  }

  function logout(routerInstance?: Router) {
    user.value = null
    if (typeof window !== 'undefined') {
      localStorage.removeItem(STORAGE_KEY)
    }
    isProfileSidebarOpen.value = false

    if (routerInstance) {
      const currentRoute = routerInstance.currentRoute.value
      const protectedRoutes = ['queue', 'seat-selection', 'checkout', 'tickets']
      if (protectedRoutes.includes(currentRoute.name as string)) {
        routerInstance.push('/')
      }
    }
  }

  function openAuthModal(options?: {
    tab?: 'login' | 'register'
    redirectPath?: string
    notice?: string
  }) {
    if (options?.tab) {
      authModalTab.value = options.tab
    }
    if (options?.redirectPath) {
      redirectPathAfterLogin.value = options.redirectPath
    }
    if (options?.notice) {
      authNoticeMessage.value = options.notice
    } else if (options?.redirectPath) {
      authNoticeMessage.value = 'Inicia sesión o regístrate para acceder a la sala de espera y reserva de asientos.'
    }
    isAuthModalOpen.value = true
  }

  function closeAuthModal() {
    isAuthModalOpen.value = false
    authNoticeMessage.value = null
  }

  function openProfileSidebar() {
    isProfileSidebarOpen.value = true
  }

  function closeProfileSidebar() {
    isProfileSidebarOpen.value = false
  }

  function updateProfile(data: Partial<AuthUser>): boolean {
    if (!user.value) return false
    user.value = {
      ...user.value,
      ...data
    }
    if (typeof window !== 'undefined') {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(user.value))
    }
    return true
  }

  function removeSavedCard(cardId: string): boolean {
    if (!user.value) return false
    const cards = (user.value.savedCards || []).filter(c => c.id !== cardId)
    user.value = {
      ...user.value,
      savedCards: cards
    }
    if (typeof window !== 'undefined') {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(user.value))
    }
    return true
  }

  function addSavedCard(card: Omit<SavedCard, 'id'>): SavedCard | null {
    if (!user.value) return null
    const newCard: SavedCard = {
      ...card,
      id: `card_${Date.now().toString(36)}`
    }
    const currentCards = user.value.savedCards || []
    user.value = {
      ...user.value,
      savedCards: [...currentCards, newCard]
    }
    if (typeof window !== 'undefined') {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(user.value))
    }
    return newCard
  }

  return {
    user,
    isAuthenticated,
    isAuthModalOpen,
    authModalTab,
    redirectPathAfterLogin,
    authNoticeMessage,
    isProfileSidebarOpen,
    login,
    loginWithMock,
    register,
    logout,
    updateProfile,
    removeSavedCard,
    addSavedCard,
    openAuthModal,
    closeAuthModal,
    openProfileSidebar,
    closeProfileSidebar
  }
})
