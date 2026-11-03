<template>
  <div class="relative" ref="dropdownRef">
    <button
      type="button"
      @click="isOpen = !isOpen"
      class="flex items-center gap-3 px-3 py-1.5 rounded-xl bg-surface-container-deep border border-border-subtle hover:border-primary/50 transition group"
    >
      <!-- Avatar -->
      <div class="relative">
        <img
          :src="authStore.profile?.avatarUrl || authStore.currentIdentity.avatarUrl || 'https://api.dicebear.com/7.x/bottts/svg?seed=admin'"
          alt="Avatar"
          class="w-8 h-8 rounded-lg bg-surface-card border border-border-subtle object-cover"
        />
        <span
          :class="[
            'absolute -bottom-0.5 -right-0.5 w-2.5 h-2.5 rounded-full border-2 border-surface',
            authStore.isAdmin ? 'bg-primary' : authStore.isOperator ? 'bg-secondary' : 'bg-muted'
          ]"
        />
      </div>

      <!-- Identity text -->
      <div class="text-left hidden md:block">
        <div class="text-xs font-semibold text-high-emphasis flex items-center gap-1.5">
          {{ authStore.profile?.name || authStore.currentIdentity.name }}
          <span
            :class="[
              'text-[10px] font-mono px-1.5 py-0.2 rounded border font-medium uppercase',
              authStore.currentIdentity.badgeColor
            ]"
          >
            {{ authStore.isAdmin ? 'Admin' : authStore.isOperator ? 'Operator' : 'User' }}
          </span>
        </div>
        <div class="text-[11px] font-mono text-muted-light truncate max-w-[150px]">
          {{ authStore.profile?.email || authStore.currentIdentity.email }}
        </div>
      </div>

      <span class="material-symbols-outlined text-muted text-base group-hover:text-high-emphasis transition">
        expand_more
      </span>
    </button>

    <!-- Dropdown Menu -->
    <div
      v-if="isOpen"
      class="absolute right-0 mt-2 w-72 rounded-2xl bg-surface-container border border-border-subtle shadow-2xl p-2 z-50 animate-in fade-in zoom-in-95 duration-150"
    >
      <div class="px-3 py-2 border-b border-border-subtle mb-1">
        <div class="text-[11px] font-mono uppercase tracking-wider text-muted-light">
          Simulador Entra ID (Floci-AZ)
        </div>
        <div class="text-xs text-on-surface-variant mt-0.5">
          Conmuta entre identidades seeded para probar RBAC
        </div>
      </div>

      <div class="space-y-1">
        <button
          v-for="idKey in (['admin', 'operator', 'user'] as const)"
          :key="idKey"
          type="button"
          @click="selectIdentity(idKey)"
          :class="[
            'w-full flex items-start gap-2.5 px-3 py-2 rounded-xl text-left transition',
            authStore.activeIdentityKey === idKey
              ? 'bg-surface-elevated border border-border-focus/40'
              : 'hover:bg-surface-container-deep border border-transparent'
          ]"
        >
          <span
            :class="[
              'w-2 h-2 rounded-full mt-1.5 shrink-0',
              idKey === 'admin' ? 'bg-primary' : idKey === 'operator' ? 'bg-secondary' : 'bg-muted'
            ]"
          />
          <div class="min-w-0 flex-1">
            <div class="flex items-center justify-between">
              <span class="text-xs font-bold text-high-emphasis">
                {{ DEV_IDENTITIES[idKey].name }}
              </span>
              <span
                :class="[
                  'text-[9px] font-mono px-1 py-0.2 rounded border uppercase font-medium',
                  DEV_IDENTITIES[idKey].badgeColor
                ]"
              >
                {{ idKey }}
              </span>
            </div>
            <div class="text-[11px] text-muted-light font-mono truncate">
              {{ DEV_IDENTITIES[idKey].email }}
            </div>
            <div class="text-[10px] text-muted truncate mt-0.5">
              {{ DEV_IDENTITIES[idKey].roleTitle }}
            </div>
          </div>
        </button>
      </div>

      <div class="pt-2 mt-1 border-t border-border-subtle flex items-center justify-between px-2">
        <span class="text-[10px] font-mono text-muted flex items-center gap-1">
          <span class="w-1.5 h-1.5 rounded-full bg-secondary"></span>
          Floci-AZ :4577
        </span>
        <button
          type="button"
          @click="logout"
          class="text-xs font-mono text-error hover:underline flex items-center gap-1"
        >
          <span class="material-symbols-outlined text-sm">logout</span>
          Cerrar Sesión
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { DEV_IDENTITIES } from '@/services/authService'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

const isOpen = ref(false)
const dropdownRef = ref<HTMLElement | null>(null)

async function selectIdentity(key: 'admin' | 'operator' | 'user') {
  isOpen.value = false
  await authStore.switchIdentity(key)

  // If user role is selected and we are on a protected route, router will guard
  if (key === 'user' && route.meta.roles) {
    router.push('/unauthorized')
  }
}

function logout() {
  isOpen.value = false
  authStore.logout()
  router.push('/login')
}

function handleClickOutside(e: MouseEvent) {
  if (dropdownRef.value && !dropdownRef.value.contains(e.target as Node)) {
    isOpen.value = false
  }
}

onMounted(() => {
  document.addEventListener('click', handleClickOutside)
})

onUnmounted(() => {
  document.removeEventListener('click', handleClickOutside)
})
</script>
