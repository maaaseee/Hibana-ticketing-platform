<script setup lang="ts">
import { ref, watch, onMounted, onUnmounted } from 'vue'
import { RouterLink, useRouter, useRoute } from 'vue-router'
import { useTicketingStore } from '../../stores/ticketing'
import {
  DropdownMenuRoot,
  DropdownMenuTrigger,
  DropdownMenuPortal,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  PopoverRoot,
  PopoverTrigger,
  PopoverPortal,
  PopoverContent,
  PopoverClose,
  DialogRoot,
  DialogPortal,
  DialogOverlay,
  DialogContent,
  DialogClose,
  DialogTitle,
  DialogDescription
} from 'radix-vue'

const ticketing = useTicketingStore()
const router = useRouter()
const route = useRoute()

// Estado de la Sidebar lateral móvil
const isMobileSidebarOpen = ref(false)

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
  if (typeof window !== 'undefined') {
    mediaQueryList = window.matchMedia('(min-width: 768px)')
    // Cerrar si ya está en pantalla grande
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

function navigateTo(routeName: string) {
  isMobileSidebarOpen.value = false
  router.push({ name: routeName })
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
      <!-- Brand (Left) -->
      <RouterLink to="/" class="flex items-center gap-2.5 sm:gap-3 group shrink-0">
        <img
          src="/logo.png"
          alt="Hibana.tk Tickets"
          class="w-8 h-8 sm:w-9 sm:h-9 object-contain drop-shadow-[0_0_12px_rgba(134,207,251,0.45)] group-hover:scale-105 transition-transform duration-300"
        />
        <span class="font-display text-2xl lg:text-3xl font-black text-primary tracking-tight group-hover:text-on-primary-container transition-colors">
          Hibana.tk
        </span>
      </RouterLink>

      <!-- Right Side: Links de Navegación + Acciones de Usuario (Todo agrupado a la derecha) -->
      <div class="flex items-center gap-4 sm:gap-6 lg:gap-8">
        <!-- Desktop Navigation Links desplazados al lado de las acciones de usuario -->
        <nav class="hidden md:flex items-center gap-5 lg:gap-7 font-display text-[15px] lg:text-base font-semibold">
          <!-- Botón Home representado por el icono de una casa, limpio sin bordes ni background como los demás -->
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

        <!-- Trailing Actions: Reserva Activa, Notificaciones y Menú de Perfil (Right) -->
        <div class="flex items-center gap-3 sm:gap-4 shrink-0">
          <!-- Active Lock Countdown Pill -->
          <RouterLink
            v-if="ticketing.selectedSeats.length > 0"
            :to="{ name: 'checkout', params: { id: 'chronos-fest-2026' } }"
            class="hidden sm:flex items-center gap-2 bg-primary-container/30 border border-primary text-primary px-3.5 py-1.5 rounded-full text-xs font-mono font-semibold hover:bg-primary-container/50 transition-colors animate-pulse"
          >
            <span class="material-symbols-outlined text-[16px]">lock_clock</span>
            <span>{{ ticketing.selectedSeats.length }} Reservadas ({{ ticketing.formattedLockTime }})</span>
          </RouterLink>

          <!-- Notifications Radix Popover -->
          <PopoverRoot>
            <PopoverTrigger as-child>
              <button
                aria-label="Notificaciones"
                class="p-2 text-medium-emphasis hover:text-primary transition-colors relative rounded-full hover:bg-surface-interactive cursor-pointer outline-none"
              >
                <span class="material-symbols-outlined text-[20px]">notifications</span>
                <span class="absolute top-1.5 right-1.5 w-2 h-2 bg-primary rounded-full ring-2 ring-surface-dim"></span>
              </button>
            </PopoverTrigger>

            <PopoverPortal>
              <PopoverContent
                class="bg-surface-card border border-border-subtle rounded-2xl p-4 shadow-2xl z-50 w-80 text-sm outline-none"
                :side-offset="8"
                align="end"
              >
                <div class="flex items-center justify-between pb-3 border-b border-border-subtle">
                  <div class="flex items-center gap-2 font-display font-bold text-sm text-high-emphasis">
                    <span class="material-symbols-outlined text-primary text-base">notifications</span>
                    <span>Notificaciones</span>
                  </div>
                  <PopoverClose class="text-muted-light hover:text-white p-1 rounded transition-colors cursor-pointer">
                    <span class="material-symbols-outlined text-xs">close</span>
                  </PopoverClose>
                </div>

                <div class="py-2 flex flex-col gap-2.5">
                  <div class="p-2.5 rounded-xl bg-surface-container-deep border border-border-subtle flex flex-col gap-1">
                    <div class="flex items-center gap-1.5 text-xs text-primary font-mono font-semibold">
                      <span class="material-symbols-outlined text-[14px]">confirmation_number</span>
                      <span>Entradas Emitidas</span>
                    </div>
                    <p class="text-xs text-medium-emphasis">Tus tickets para Chronos Festival tienen QR dinámico activo.</p>
                  </div>

                  <div class="p-2.5 rounded-xl bg-surface-container-deep border border-border-subtle flex flex-col gap-1">
                    <div class="flex items-center gap-1.5 text-xs text-secondary font-mono font-semibold">
                      <span class="material-symbols-outlined text-[14px]">lock_clock</span>
                      <span>Reserva Garantizada</span>
                    </div>
                    <p class="text-xs text-medium-emphasis">Tus asientos están guardados durante el proceso de compra.</p>
                  </div>
                </div>
              </PopoverContent>
            </PopoverPortal>
          </PopoverRoot>

          <!-- User Profile Radix DropdownMenu (Agrupa My Tickets, Centro de Ayuda y Acciones) -->
          <DropdownMenuRoot>
            <DropdownMenuTrigger as-child>
              <button
                class="flex items-center gap-2 p-1 pl-1 pr-2.5 rounded-full bg-surface-interactive/80 hover:bg-surface-interactive border border-border-subtle hover:border-primary transition-all cursor-pointer outline-none group"
                aria-label="Menú de Usuario"
              >
                <img
                  alt="User profile"
                  class="w-7 h-7 sm:w-8 sm:h-8 rounded-full object-cover ring-1 ring-border-subtle group-hover:ring-primary transition-all"
                  src="https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&w=100&q=80"
                />
                <span class="hidden sm:inline-block font-display text-xs font-semibold text-high-emphasis">Carlos</span>
                <span class="material-symbols-outlined text-[18px] text-muted-light group-hover:text-primary transition-colors">expand_more</span>
              </button>
            </DropdownMenuTrigger>

            <DropdownMenuPortal>
              <DropdownMenuContent
                class="bg-surface-card border border-border-subtle rounded-2xl p-2 shadow-2xl z-50 min-w-60 text-sm outline-none animate-in fade-in-50 zoom-in-95"
                :side-offset="8"
                align="end"
              >
                <div class="px-3 py-2.5 border-b border-border-subtle mb-1">
                  <p class="font-display font-bold text-sm text-high-emphasis">Carlos Mendoza</p>
                  <p class="font-mono text-[11px] text-muted-light">carlos.m@hibana.tk</p>
                </div>

                <!-- My Tickets / Mis Entradas como opción principal destacada -->
                <DropdownMenuItem
                  @click="router.push({ name: 'tickets' })"
                  class="flex items-center justify-between px-3 py-2.5 rounded-xl text-xs font-medium text-high-emphasis hover:text-primary hover:bg-surface-interactive focus:bg-surface-interactive focus:text-primary cursor-pointer outline-none transition-colors"
                >
                  <div class="flex items-center gap-2.5">
                    <span class="material-symbols-outlined text-[18px] text-primary">confirmation_number</span>
                    <span class="font-semibold">Mis Entradas</span>
                  </div>
                  <span class="px-2 py-0.5 rounded-full bg-primary-container/30 text-primary border border-primary/30 font-mono text-[10px] font-bold">
                    3 activas
                  </span>
                </DropdownMenuItem>

                <DropdownMenuItem
                  @click="router.push({ name: 'help' })"
                  class="flex items-center gap-2.5 px-3 py-2 rounded-xl text-xs text-medium-emphasis hover:text-primary hover:bg-surface-interactive focus:bg-surface-interactive focus:text-primary cursor-pointer outline-none transition-colors"
                >
                  <span class="material-symbols-outlined text-sm">help</span>
                  <span>Centro de Ayuda</span>
                </DropdownMenuItem>

                <DropdownMenuSeparator class="h-px bg-border-subtle my-1" />

                <DropdownMenuItem
                  class="flex items-center gap-2.5 px-3 py-2 rounded-xl text-xs text-error hover:bg-surface-interactive focus:bg-surface-interactive cursor-pointer outline-none transition-colors"
                >
                  <span class="material-symbols-outlined text-sm">logout</span>
                  <span>Cerrar Sesión</span>
                </DropdownMenuItem>
              </DropdownMenuContent>
            </DropdownMenuPortal>
          </DropdownMenuRoot>

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
            <div class="flex items-center gap-2.5">
              <img
                src="/logo.png"
                alt="Hibana.tk Logo"
                class="w-7 h-7 object-contain drop-shadow-[0_0_8px_rgba(134,207,251,0.4)]"
              />
              <span class="font-display text-2xl font-black text-primary tracking-tight">Hibana.tk</span>
            </div>
            <DialogClose
              class="w-9 h-9 rounded-full bg-surface-interactive/80 hover:bg-surface-interactive border border-border-subtle text-high-emphasis flex items-center justify-center transition-all cursor-pointer focus:outline-none hover:scale-105 active:scale-95"
              aria-label="Cerrar navegación"
            >
              <span class="material-symbols-outlined text-xl">close</span>
            </DialogClose>
          </div>

          <DialogTitle class="sr-only">Menú de Navegación Móvil</DialogTitle>
          <DialogDescription class="sr-only">Navega a través de las diferentes secciones del portal Hibana.tk</DialogDescription>

          <!-- Perfil rápido de usuario en la Sidebar -->
          <div class="p-3.5 rounded-2xl bg-surface-card border border-border-subtle flex items-center gap-3 shadow-sm">
            <img
              alt="User profile"
              class="w-10 h-10 rounded-full object-cover ring-2 ring-primary/40 shrink-0"
              src="https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&w=100&q=80"
            />
            <div class="flex flex-col min-w-0">
              <span class="font-display font-bold text-sm text-high-emphasis truncate">Carlos Mendoza</span>
              <span class="font-mono text-[11px] text-muted-light truncate">carlos.m@hibana.tk</span>
            </div>
          </div>

          <!-- Enlaces Principales de Navegación con Íconos y Estado Activo -->
          <nav class="flex flex-col gap-1.5 font-display text-base font-semibold">
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
                3 activas
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

        <!-- Footer de la Sidebar (Soporte & Cierre de Sesión) -->
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
            type="button"
            @click="isMobileSidebarOpen = false"
            class="flex items-center gap-2.5 px-3 py-2 rounded-xl text-xs font-mono text-error hover:bg-surface-interactive transition-colors cursor-pointer text-left"
          >
            <span class="material-symbols-outlined text-sm">logout</span>
            <span>Cerrar Sesión</span>
          </button>
        </div>
      </DialogContent>
    </DialogPortal>
  </DialogRoot>
</template>

<style scoped>
/* Animaciones de Entrada y Salida para la Sidebar Móvil (Radix data-[state=open/closed]) */
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
  from {
    opacity: 0;
  }
  to {
    opacity: 1;
  }
}

@keyframes overlayFadeOut {
  from {
    opacity: 1;
  }
  to {
    opacity: 0;
  }
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
