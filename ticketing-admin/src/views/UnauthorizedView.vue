<template>
  <div class="min-h-screen bg-canvas-base flex flex-col justify-center items-center p-6 relative overflow-hidden">
    <div class="absolute inset-0 bg-topography opacity-10 pointer-events-none"></div>

    <div class="w-full max-w-lg z-10 text-center">
      <div class="inline-flex items-center justify-center w-20 h-20 rounded-3xl bg-error/10 border border-error/20 text-error shadow-2xl mb-6">
        <span class="material-symbols-outlined text-4xl">gpp_maybe</span>
      </div>

      <div class="inline-flex items-center gap-1.5 px-3 py-1 rounded-full bg-error/10 text-error font-mono text-xs font-semibold mb-3 border border-error/20">
        HTTP 403 • FORBIDDEN
      </div>

      <h1 class="text-3xl font-display font-black tracking-tight text-high-emphasis mb-3">
        Acceso Restringido por Política RBAC
      </h1>

      <p class="text-sm text-on-surface-variant max-w-md mx-auto mb-6 leading-relaxed">
        Tu identidad actual (<strong class="text-high-emphasis font-mono">{{ authStore.profile?.email || 'user@hibana.tk' }}</strong>) posee exclusivamente el rol <span class="font-mono text-xs px-1.5 py-0.5 rounded bg-muted/20 text-muted-light border border-muted/30">ROLE_USER</span>. Se requiere autorización de tipo <span class="font-mono text-xs px-1.5 py-0.5 rounded bg-primary/20 text-primary border border-primary/30">ROLE_ADMIN</span> o <span class="font-mono text-xs px-1.5 py-0.5 rounded bg-secondary/20 text-secondary border border-secondary/30">ROLE_OPERATOR</span> para administrar el portal.
      </p>

      <div class="bg-surface-container border border-border-subtle rounded-2xl p-5 mb-6 text-left max-w-md mx-auto">
        <div class="text-xs font-mono uppercase tracking-wider text-muted-light mb-2">
          Resolución para Desarrollo Local:
        </div>
        <p class="text-xs text-muted mb-4">
          Puedes conmutar inmediatamente a la identidad de Alex Thorne (Super Admin) para continuar evaluando las funcionalidades del portal:
        </p>
        <button
          type="button"
          @click="elevateToAdmin"
          class="w-full py-2.5 px-4 rounded-xl bg-primary text-on-primary hover:bg-primary-hover font-semibold text-xs transition flex items-center justify-center gap-2 shadow-lg shadow-primary/20"
        >
          <span class="material-symbols-outlined text-sm">admin_panel_settings</span>
          Conmutar a Alex Thorne (Admin)
        </button>
      </div>

      <div class="flex items-center justify-center gap-4">
        <button
          type="button"
          @click="goToLogin"
          class="text-xs font-mono text-muted-light hover:text-high-emphasis transition flex items-center gap-1"
        >
          <span class="material-symbols-outlined text-sm">arrow_back</span>
          Volver a Selección de Identidad
        </button>
        <span class="text-muted">•</span>
        <a
          href="http://localhost:5173"
          class="text-xs font-mono text-secondary hover:underline flex items-center gap-1"
        >
          <span class="material-symbols-outlined text-sm">storefront</span>
          Ir al Portal Público B2C (:5173)
        </a>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const authStore = useAuthStore()

async function elevateToAdmin() {
  await authStore.switchIdentity('admin')
  router.push('/events')
}

function goToLogin() {
  authStore.logout()
  router.push('/login')
}
</script>
