<script setup lang="ts">
import { ref, watch, onMounted, onUnmounted } from 'vue'
import { RouterLink, useRouter, useRoute } from 'vue-router'
import { useTicketingStore } from '../../stores/ticketing'
import { useAuthStore } from '../../stores/auth'
import ProfileSidebar from '../auth/ProfileSidebar.vue'
import AppLogo from './AppLogo.vue'
import GlobalSearchModal from '../search/GlobalSearchModal.vue'
import {
  DialogRoot,
  DialogPortal,
  DialogOverlay,
  DialogContent,
  DialogClose,
  DialogTitle,
  DialogDescription
} from 'radix-vue'

const ticketing = useTicketingStore()
const authStore = useAuthStore()
const router = useRouter()
const route = useRoute()
const logoUrl = '/logo.png'

// Estado de la Sidebar lateral móvil
const isMobileSidebarOpen = ref(false)
const isSearchOpen = ref(false)

// Cerrar automáticamente la sidebar al cambiar de ruta
watch(
  () => route.fullPath,
  () => {
    isMobileSidebarOpen.value = false
  }
)

// Cerrar automáticamente la sidebar al cambiar la ventana de tamaño a Desktop (>= 768px - md)
let mediaQueryList: MediaQueryList | null = null

function handleMediaQueryChange(e: MediaQueryListEvent | MediaQueryList) {
  if (e.matches) {
    isMobileSidebarOpen.value = false
  }
}

onMounted(() => {
  if (typeof window !== 'undefined' && typeof window.matchMedia === 'function') {
    mediaQueryList = window.matchMedia('(min-width: 768px)')
    if (mediaQueryList.matches) {
      isMobileSidebarOpen.value = false
    }
    mediaQueryList.addEventListener('change', handleMediaQueryChange)
  }
})

onUnmounted(() => {
  if (mediaQueryList) {
    mediaQueryList.removeEventListener('change', handleMediaQueryChange)
  }
})

function handleMobileProfileClick() {
  isMobileSidebarOpen.value = false
  authStore.openProfileSidebar()
}

function handleMobileLogout() {
  isMobileSidebarOpen.value = false
  authStore.logout(router)
}
</script>

<template>
  <!-- Top Global App Bar ocupando el 100% del width con fondo degradado que se desvanece suavemente hacia abajo -->
  <header class="sticky top-0 z-40 w-full transition-all">
    <!-- Capa de fondo con degradado y desenfoque progresivo (fade-out hacia abajo sin corte abrupto) -->
    <div
      class="absolute inset-0 -z-10 pointer-events-none select-none backdrop-blur-xl"
      style="
        background: linear-gradient(to bottom, rgba(11, 19, 30, 0.98) 0%, rgba(11, 19, 30, 0.88) 45%, rgba(11, 19, 30, 0.40) 78%, rgba(11, 19, 30, 0) 100%);
        -webkit-mask-image: linear-gradient(to bottom, rgba(0, 0, 0, 1) 0%, rgba(0, 0, 0, 1) 45%, rgba(0, 0, 0, 0.35) 78%, rgba(0, 0, 0, 0) 100%);
        mask-image: linear-gradient(to bottom, rgba(0, 0, 0, 1) 0%, rgba(0, 0, 0, 1) 45%, rgba(0, 0, 0, 0.35) 78%, rgba(0, 0, 0, 0) 100%);
      "
    />
    <div class="flex justify-between items-center w-full px-4 sm:px-6 lg:px-10 h-16">
      <!-- Brand (Left) con componente unificado AppLogo -->
      <AppLogo size="lg" />

      <!-- Right Side: Links de Navegación + Acciones de Usuario (Todo agrupado a la derecha) -->
      <div class="flex items-center gap-4 sm:gap-6 lg:gap-8">
        <!-- Desktop Navigation Links desplazados al lado de las acciones de usuario -->
        <nav class="hidden md:flex items-center gap-5 lg:gap-7 font-display text-[15px] lg:text-base font-semibold">
          <!-- Botón Home representado por el icono de una casa -->
          <RouterLink
            to="/"
            class="text-medium-emphasis hover:text-primary transition-colors duration-200 py-1.5 px-1 relative flex items-center justify-center group"
            :class="route.name === 'home' ? 'text-primary font-bold after:absolute after:bottom-0 after:left-0 after:right-0 after:h-0.5 after:bg-primary after:rounded-full' : ''"
            aria-label="Inicio"
          >
            <span class="material-symbols-outlined text-[24px] group-hover:scale-105 transition-transform">home</span>
          </RouterLink>

          <!-- Shows & Events -->
          <RouterLink
            to="/events"
            class="text-medium-emphasis hover:text-primary transition-colors duration-200 py-1.5 px-1 relative"
            :class="route.path.startsWith('/events') || route.path.startsWith('/event/') ? 'text-primary font-bold after:absolute after:bottom-0 after:left-0 after:right-0 after:h-0.5 after:bg-primary after:rounded-full' : ''"
          >
            Eventos
          </RouterLink>

          <!-- Venues & FAQ -->
          <RouterLink
            to="/help"
            class="text-medium-emphasis hover:text-primary transition-colors duration-200 py-1.5 px-1 relative"
            :class="route.path.startsWith('/help') ? 'text-primary font-bold after:absolute after:bottom-0 after:left-0 after:right-0 after:h-0.5 after:bg-primary after:rounded-full' : ''"
          >
            Preguntas frecuentes
          </RouterLink>
        </nav>

        <!-- Separador sutil entre links y controles de usuario -->
        <div class="hidden md:block h-6 w-px bg-border-subtle/80"></div>

        <!-- Trailing Actions: Buscador, Reserva Activa, Login/Registro O Perfil Deslizante (Right) -->
        <div class="flex items-center gap-2.5 sm:gap-3 lg:gap-4 shrink-0">
          <!-- Buscador Global rápido (Ctrl+K / Cmd+K) -->
          <button
            type="button"
            @click="isSearchOpen = true"
            class="flex items-center gap-1.5 sm:gap-2 px-2.5 sm:px-3 py-1.5 rounded-full bg-surface-card hover:bg-surface-interactive border border-border-subtle hover:border-[#38bdf8]/50 text-medium-emphasis hover:text-white transition-all text-xs font-body cursor-pointer group"
            title="Buscar shows y artistas (Ctrl+K)"
            aria-label="Buscar shows y artistas"
          >
            <span class="material-symbols-outlined text-[18px] text-[#38bdf8] group-hover:scale-110 transition-transform">search</span>
            <span class="hidden sm:inline-block">Buscar</span>
            <kbd class="hidden lg:inline-block text-[10px] font-mono px-1.5 py-0.5 bg-black/40 border border-border-subtle rounded text-muted-light">⌘K</kbd>
          </button>

          <!-- Active Lock Countdown Pill -->
          <RouterLink
            v-if="ticketing.selectedSeats.length > 0"
            :to="{ name: 'checkout', params: { id: 'chronos-fest-2026' } }"
            class="hidden sm:flex items-center gap-2 bg-primary-container/30 border border-primary text-primary px-3.5 py-1.5 rounded-full text-xs font-mono font-semibold hover:bg-primary-container/50 transition-colors animate-pulse"
          >
            <span class="material-symbols-outlined text-[16px]">lock_clock</span>
            <span>{{ ticketing.selectedSeats.length }} Reservadas ({{ ticketing.formattedLockTime }})</span>
          </RouterLink>

          <!-- CASO 1: USUARIO AUTENTICADO -> Botón que despliega la SIDEBAR de perfil (no dropdown) -->
          <button
            v-if="authStore.isAuthenticated"
            @click="authStore.openProfileSidebar()"
            class="flex items-center gap-2 p-1 pl-1 pr-2.5 rounded-full bg-surface-interactive/80 hover:bg-surface-interactive border border-border-subtle hover:border-primary transition-all cursor-pointer outline-none group"
            aria-label="Abrir perfil de usuario"
          >
            <img
              alt="User profile"
              class="w-7 h-7 sm:w-8 sm:h-8 rounded-full object-cover ring-1 ring-border-subtle group-hover:ring-primary transition-all"
              :src="authStore.user?.avatarUrl || 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&w=100&q=80'"
            />
            <span class="hidden sm:inline-block font-display text-xs font-semibold text-high-emphasis">
              {{ authStore.user?.name.split(' ')[0] }}
            </span>
            <span class="material-symbols-outlined text-[18px] text-muted-light group-hover:text-primary transition-colors">
              manage_accounts
            </span>
          </button>

          <!-- CASO 2: USUARIO NO AUTENTICADO -> Enlaces a vistas dedicadas de Login y Registro -->
          <div v-else class="hidden sm:flex items-center gap-2.5">
            <RouterLink
              to="/login"
              class="px-3.5 py-1.5 rounded-full text-xs font-display font-semibold text-[#cbd5e1] hover:text-white hover:bg-surface-interactive transition-colors cursor-pointer"
            >
              Iniciar Sesión
            </RouterLink>
            <RouterLink
              to="/register"
              class="px-4 py-1.5 rounded-full text-xs font-display font-bold bg-[#00688f] hover:bg-[#005a7d] text-white border border-[#38bdf8]/40 shadow-sm transition-all cursor-pointer active:scale-95"
            >
              Crear Cuenta
            </RouterLink>
          </div>

          <!-- Mobile Hamburger para desplegar la Sidebar lateral -->
          <button
            @click="isMobileSidebarOpen = true"
            class="md:hidden p-2 text-on-surface-variant hover:text-on-surface rounded-lg hover:bg-surface-interactive cursor-pointer transition-colors"
            aria-label="Abrir menú de navegación"
          >
            <span class="material-symbols-outlined text-2xl">menu</span>
          </button>
        </div>
      </div>
    </div>
  </header>

  <!-- SIDEBAR MÓVIL DESLIZABLE (Radix Dialog con animaciones de entrada y salida fluidas) -->
  <DialogRoot v-model:open="isMobileSidebarOpen">
    <DialogPortal>
      <!-- Overlay difuminado de fondo con fade in/out -->
      <DialogOverlay class="sidebar-overlay fixed inset-0 bg-black/75 backdrop-blur-md z-50" />

      <!-- Sidebar deslizante desde la derecha con slide in/out -->
      <DialogContent
        class="sidebar-content fixed inset-y-0 right-0 w-[84vw] max-w-sm bg-surface-dim border-l border-border-subtle shadow-2xl z-50 flex flex-col justify-between p-6 focus:outline-none"
      >
        <!-- Header de la Sidebar -->
        <div class="flex flex-col gap-6">
          <div class="flex items-center justify-between pb-4 border-b border-border-subtle">
            <AppLogo size="md" :to="null" />
            <DialogClose
              class="w-9 h-9 rounded-full bg-surface-interactive/80 hover:bg-surface-interactive border border-border-subtle text-high-emphasis flex items-center justify-center transition-all cursor-pointer focus:outline-none hover:scale-105 active:scale-95"
              aria-label="Cerrar navegación"
            >
              <span class="material-symbols-outlined text-xl">close</span>
            </DialogClose>
          </div>

          <DialogTitle class="sr-only">Menú de Navegación Móvil</DialogTitle>
          <DialogDescription class="sr-only">Navega a través de las diferentes secciones del portal Hibana.tk</DialogDescription>

          <!-- Perfil rápido de usuario O Bloque de Acceso Móvil -->
          <div
            v-if="authStore.isAuthenticated && authStore.user"
            @click="handleMobileProfileClick"
            class="p-3.5 rounded-2xl bg-surface-card border border-border-subtle flex items-center justify-between gap-3 shadow-sm cursor-pointer hover:border-primary transition-colors"
          >
            <div class="flex items-center gap-3 min-w-0">
              <img
                alt="User profile"
                class="w-10 h-10 rounded-full object-cover ring-2 ring-primary/40 shrink-0"
                :src="authStore.user.avatarUrl || 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&w=100&q=80'"
              />
              <div class="flex flex-col min-w-0">
                <span class="font-display font-bold text-sm text-high-emphasis truncate">{{ authStore.user.name }}</span>
                <span class="font-mono text-[11px] text-muted-light truncate">{{ authStore.user.email }}</span>
              </div>
            </div>
            <span class="material-symbols-outlined text-sm text-[#86cffb]">chevron_right</span>
          </div>

          <!-- Si no está autenticado, botones en mobile -->
          <div
            v-else
            class="p-4 rounded-2xl bg-surface-card border border-border-subtle flex flex-col gap-2.5"
          >
            <span class="font-display font-bold text-sm text-white">Mi Cuenta</span>
            <p class="text-xs text-[#94a3b8]">Inicia sesión o regístrate para comprar entradas y acceder a las salas de espera.</p>
            <div class="grid grid-cols-2 gap-2 mt-1">
              <RouterLink
                to="/login"
                @click="isMobileSidebarOpen = false"
                class="py-2 px-3 rounded-xl bg-[#141c27] hover:bg-[#1b2533] border border-[#23354d] text-white text-xs font-semibold text-center transition-colors"
              >
                Iniciar Sesión
              </RouterLink>
              <RouterLink
                to="/register"
                @click="isMobileSidebarOpen = false"
                class="py-2 px-3 rounded-xl bg-[#00688f] hover:bg-[#005a7d] text-white text-xs font-bold text-center transition-colors"
              >
                Crear Cuenta
              </RouterLink>
            </div>
          </div>

          <!-- Enlaces Principales de Navegación con Íconos y Estado Activo -->
          <nav class="flex flex-col gap-1.5 font-display text-base font-semibold">
            <button
              type="button"
              @click="isMobileSidebarOpen = false; isSearchOpen = true"
              class="flex items-center gap-3.5 px-4 py-3 rounded-xl text-medium-emphasis hover:text-white hover:bg-surface-interactive transition-all cursor-pointer text-left"
            >
              <span class="material-symbols-outlined text-2xl text-[#38bdf8]">search</span>
              <span>Buscar Shows</span>
            </button>

            <RouterLink
              to="/"
              @click="isMobileSidebarOpen = false"
              class="flex items-center gap-3.5 px-4 py-3 rounded-xl transition-all"
              :class="route.name === 'home' ? 'bg-primary-container text-white shadow-md font-bold' : 'text-medium-emphasis hover:text-white hover:bg-surface-interactive'"
            >
              <span class="material-symbols-outlined text-2xl">home</span>
              <span>Home</span>
            </RouterLink>

            <RouterLink
              to="/events"
              @click="isMobileSidebarOpen = false"
              class="flex items-center gap-3.5 px-4 py-3 rounded-xl transition-all"
              :class="route.path.startsWith('/events') || route.path.startsWith('/event/') ? 'bg-primary-container text-white shadow-md font-bold' : 'text-medium-emphasis hover:text-white hover:bg-surface-interactive'"
            >
              <span class="material-symbols-outlined text-2xl">theater_comedy</span>
              <span>Shows & Events</span>
            </RouterLink>

            <RouterLink
              to="/tickets"
              @click="isMobileSidebarOpen = false"
              class="flex items-center justify-between px-4 py-3 rounded-xl transition-all"
              :class="route.path.startsWith('/tickets') ? 'bg-primary-container text-white shadow-md font-bold' : 'text-medium-emphasis hover:text-white hover:bg-surface-interactive'"
            >
              <div class="flex items-center gap-3.5">
                <span class="material-symbols-outlined text-2xl">confirmation_number</span>
                <span>Mis Entradas</span>
              </div>
              <span class="px-2 py-0.5 rounded-full bg-surface-container-deep text-primary border border-primary/30 font-mono text-[11px] font-bold">
                Pases Activos
              </span>
            </RouterLink>

            <RouterLink
              to="/help"
              @click="isMobileSidebarOpen = false"
              class="flex items-center gap-3.5 px-4 py-3 rounded-xl transition-all"
              :class="route.path.startsWith('/help') ? 'bg-primary-container text-white shadow-md font-bold' : 'text-medium-emphasis hover:text-white hover:bg-surface-interactive'"
            >
              <span class="material-symbols-outlined text-2xl">help</span>
              <span>Venues & FAQ</span>
            </RouterLink>
          </nav>
        </div>

        <!-- Footer de la Sidebar (Soporte & Cierre de Sesión si autenticado) -->
        <div class="pt-4 border-t border-border-subtle flex flex-col gap-2">
          <RouterLink
            to="/help"
            @click="isMobileSidebarOpen = false"
            class="flex items-center gap-2.5 px-3 py-2 text-xs font-mono text-muted-light hover:text-primary transition-colors"
          >
            <span class="material-symbols-outlined text-sm">support_agent</span>
            <span>Centro de Ayuda y Asistencia</span>
          </RouterLink>

          <button
            v-if="authStore.isAuthenticated"
            type="button"
            @click="handleMobileLogout"
            class="flex items-center gap-2.5 px-3 py-2 rounded-xl text-xs font-mono text-error hover:bg-surface-interactive transition-colors cursor-pointer text-left"
          >
            <span class="material-symbols-outlined text-sm">logout</span>
            <span>Cerrar Sesión</span>
          </button>
        </div>
      </DialogContent>
    </DialogPortal>
  </DialogRoot>

  <!-- Componente Perfil Deslizante (Sidebar) -->
  <ProfileSidebar />

  <!-- Buscador Global Cmd+K con Backend Search -->
  <GlobalSearchModal v-model:open="isSearchOpen" />
</template>

<style scoped>
/* Animaciones de Entrada y Salida para la Sidebar Móvil */
.sidebar-overlay[data-state='open'] {
  animation: overlayFadeIn 280ms cubic-bezier(0.16, 1, 0.3, 1) forwards;
}
.sidebar-overlay[data-state='closed'] {
  animation: overlayFadeOut 220ms cubic-bezier(0.16, 1, 0.3, 1) forwards;
}

.sidebar-content[data-state='open'] {
  animation: sidebarSlideIn 320ms cubic-bezier(0.16, 1, 0.3, 1) forwards;
}
.sidebar-content[data-state='closed'] {
  animation: sidebarSlideOut 240ms cubic-bezier(0.16, 1, 0.3, 1) forwards;
}

@keyframes overlayFadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

@keyframes overlayFadeOut {
  from { opacity: 1; }
  to { opacity: 0; }
}

@keyframes sidebarSlideIn {
  from {
    transform: translateX(100%);
    opacity: 0.5;
  }
  to {
    transform: translateX(0);
    opacity: 1;
  }
}

@keyframes sidebarSlideOut {
  from {
    transform: translateX(0);
    opacity: 1;
  }
  to {
    transform: translateX(100%);
    opacity: 0;
  }
}
</style>
