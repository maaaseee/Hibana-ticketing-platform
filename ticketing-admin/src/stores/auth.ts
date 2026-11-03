import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { UserProfile, UserRole, DevIdentity } from '@/types/auth'
import {
  DEV_IDENTITIES,
  getStoredIdentityKey,
  getStoredProfile,
  loginWithDevIdentity,
  logout as serviceLogout,
} from '@/services/authService'
import { getStoredToken } from '@/services/api'

export const useAuthStore = defineStore('auth', () => {
  const token = ref<string | null>(getStoredToken())
  const profile = ref<UserProfile | null>(getStoredProfile())
  const activeIdentityKey = ref<'admin' | 'operator' | 'user'>(getStoredIdentityKey())
  const isLoading = ref<boolean>(false)

  const isAuthenticated = computed(() => !!token.value && !!profile.value)

  const userRoles = computed<UserRole[]>(() => profile.value?.roles || [])

  const isAdmin = computed(() => userRoles.value.includes('ROLE_ADMIN'))

  const isOperator = computed(() =>
    userRoles.value.includes('ROLE_OPERATOR') || userRoles.value.includes('ROLE_ADMIN')
  )

  const currentIdentity = computed<DevIdentity>(() => {
    return DEV_IDENTITIES[activeIdentityKey.value] || DEV_IDENTITIES.admin
  })

  function hasRole(role: UserRole): boolean {
    return userRoles.value.includes(role)
  }

  async function switchIdentity(identityKey: 'admin' | 'operator' | 'user'): Promise<void> {
    isLoading.value = true
    try {
      const result = await loginWithDevIdentity(identityKey)
      token.value = result.token
      profile.value = result.profile
      activeIdentityKey.value = identityKey
    } finally {
      isLoading.value = false
    }
  }

  async function initialize(): Promise<void> {
    if (!token.value || !profile.value) {
      await switchIdentity(activeIdentityKey.value || 'admin')
    }
  }

  function logout(): void {
    serviceLogout()
    token.value = null
    profile.value = null
  }

  return {
    token,
    profile,
    activeIdentityKey,
    isLoading,
    isAuthenticated,
    userRoles,
    isAdmin,
    isOperator,
    currentIdentity,
    hasRole,
    switchIdentity,
    initialize,
    logout,
  }
})
