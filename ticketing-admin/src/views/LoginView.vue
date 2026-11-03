<template>
  <div class="min-h-screen bg-canvas-base flex flex-col justify-center items-center p-6 relative overflow-hidden">
    <!-- Background glow & topography -->
    <div class="absolute inset-0 bg-topography opacity-10 pointer-events-none"></div>
    <div class="absolute -top-40 -left-40 w-96 h-96 bg-primary/10 rounded-full blur-3xl pointer-events-none"></div>
    <div class="absolute -bottom-40 -right-40 w-96 h-96 bg-secondary/10 rounded-full blur-3xl pointer-events-none"></div>

    <div class="w-full max-w-md z-10">
      <!-- Brand Header -->
      <div class="text-center mb-8">
        <div class="inline-flex items-center justify-center w-16 h-16 rounded-2xl bg-gradient-to-br from-primary to-secondary shadow-xl shadow-primary/25 mb-4 text-on-primary">
          <span class="material-symbols-outlined text-3xl">token</span>
        </div>
        <h1 class="text-3xl font-display font-black tracking-tight text-high-emphasis">
          AMARYLLIS
        </h1>
        <p class="text-xs font-mono tracking-widest text-primary uppercase mt-1">
          Back-Office Operations Portal
        </p>
        <p class="text-xs text-muted-light mt-2">
          Acceso restringido a personal con roles <code class="text-primary font-mono font-bold">ROLE_ADMIN</code> y <code class="text-secondary font-mono font-bold">ROLE_OPERATOR</code>
        </p>
      </div>

      <!-- Dev Identity Selection Card -->
      <div class="bg-surface-container border border-border-subtle rounded-3xl p-6 shadow-2xl backdrop-blur-xl">
        <div class="flex items-center justify-between mb-4 pb-3 border-b border-border-subtle">
          <span class="text-xs font-mono uppercase tracking-wider text-muted-light">
            Selecciona Identidad para Acceder
          </span>
          <span class="inline-flex items-center gap-1 text-[11px] font-mono text-secondary">
            <span class="w-1.5 h-1.5 rounded-full bg-secondary animate-pulse"></span>
            Floci-AZ Entra ID
          </span>
        </div>
        <!-- Corporate MSAL PKCE Login Option -->
        <div class="mb-4">
          <button
            type="button"
            :disabled="authStore.isLoading"
            @click="handleMsalLogin"
            class="w-full flex items-center justify-center gap-2 p-3.5 rounded-2xl bg-primary text-on-primary font-bold hover:bg-primary-hover shadow-lg shadow-primary/20 transition disabled:opacity-50 cursor-pointer"
          >
            <span class="material-symbols-outlined text-xl">vpn_key</span>
            Iniciar Sesión con Entra ID (PKCE)
          </button>
        </div>

        <div class="relative flex py-2 items-center mb-3">
          <div class="flex-grow border-t border-border-subtle"></div>
          <span class="flex-shrink mx-3 text-[10px] font-mono uppercase text-muted-light">o identidades de desarrollo</span>
          <div class="flex-grow border-t border-border-subtle"></div>
        </div>

        <div class="space-y-3">
          <button
            v-for="idKey in (['admin', 'operator', 'user'] as const)"
            :key="idKey"
            type="button"
            :disabled="authStore.isLoading"
            @click="handleLogin(idKey)"
            class="w-full flex items-center justify-between p-3.5 rounded-2xl bg-surface-container-deep border border-border-subtle hover:border-primary hover:bg-surface-elevated transition group text-left disabled:opacity-50"
          >
            <div class="flex items-center gap-3">
              <div class="w-10 h-10 rounded-xl bg-surface-card border border-border-subtle flex items-center justify-center font-bold text-high-emphasis">
                {{ DEV_IDENTITIES[idKey].name.charAt(0) }}
              </div>
              <div>
                <div class="text-sm font-bold text-high-emphasis group-hover:text-primary transition flex items-center gap-2">
                  {{ DEV_IDENTITIES[idKey].name }}
                  <span
                    :class="[
                      'text-[9px] font-mono px-1.5 py-0.2 rounded border uppercase font-medium',
                      DEV_IDENTITIES[idKey].badgeColor
                    ]"
                  >
                    {{ idKey === 'admin' ? 'Super Admin' : idKey === 'operator' ? 'Operador' : 'Usuario B2C' }}
                  </span>
                </div>
                <div class="text-xs font-mono text-muted-light">
                  {{ DEV_IDENTITIES[idKey].email }}
                </div>
              </div>
            </div>

            <span class="material-symbols-outlined text-muted group-hover:text-primary transition">
              arrow_forward
            </span>
          </button>
        </div>

        <div class="mt-6 pt-4 border-t border-border-subtle text-center">
          <p class="text-[11px] text-muted">
            Los tokens son emitidos y firmados localmente por el emulador nativo de Microsoft Entra ID en <code class="text-on-surface font-mono">:4577</code>.
          </p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { DEV_IDENTITIES } from '@/services/authService'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

async function handleMsalLogin() {
  try {
    await authStore.loginWithMsalPkce()
    if (!authStore.isAdmin && !authStore.isOperator) {
      router.push('/unauthorized')
      return
    }
    const redirect = (route.query.redirect as string) || '/events'
    router.push(redirect)
  } catch (err) {
    console.error('[LOGIN] MSAL PKCE login error:', err)
  }
}

async function handleLogin(identityKey: 'admin' | 'operator' | 'user') {
  await authStore.switchIdentity(identityKey)

  if (identityKey === 'user') {
    router.push('/unauthorized')
    return
  }

  const redirect = (route.query.redirect as string) || '/events'
  router.push(redirect)
}
</script>
