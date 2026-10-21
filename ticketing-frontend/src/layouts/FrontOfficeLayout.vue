<script setup lang="ts">
import { ref } from 'vue'
import { RouterLink, RouterView, useRouter, useRoute } from 'vue-router'
import { useTicketingStore } from '../stores/ticketing'
import { useCatalogStore } from '../stores/catalog'
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
  PopoverClose
} from 'radix-vue'

const ticketing = useTicketingStore()
const catalog = useCatalogStore()
const router = useRouter()
const route = useRoute()
const isMobileMenuOpen = ref(false)

function onSearchSubmit() {
  const q = catalog.searchQuery.trim()
  router.push({ name: 'events-catalog', query: q ? { q } : {} })
}
</script>

<template>
  <div class="min-h-screen flex flex-col bg-[#0b131e] text-[#f1f5f9] font-body selection:bg-[#00688f] selection:text-white">
    <!-- Top Global App Bar matching wireframe -->
    <header class="bg-[#0e1927]/90 backdrop-blur-md sticky top-0 z-50 border-b border-[#23354d]">
      <div class="flex justify-between items-center w-full px-4 md:px-10 max-w-[1400px] mx-auto h-16">
        <!-- Brand (Left) -->
        <div class="flex items-center gap-6">
          <RouterLink to="/" class="flex items-center gap-2 group">
            <span class="font-display text-2xl font-bold text-[#86cffb] tracking-tight group-hover:text-[#b7e2ff] transition-colors">Amaryllis</span>
          </RouterLink>
          <nav class="hidden md:flex items-center gap-6 font-display text-sm">
            <RouterLink
              to="/"
              class="text-[#cbd5e1] font-medium hover:text-[#86cffb] transition-colors duration-200 pb-0.5"
              active-class="text-[#86cffb] border-b-2 border-[#86cffb] font-bold"
            >
              Home
            </RouterLink>
            <RouterLink
              to="/events"
              class="text-[#cbd5e1] font-medium hover:text-[#86cffb] transition-colors duration-200 pb-0.5"
              active-class="text-[#86cffb] border-b-2 border-[#86cffb] font-bold"
            >
              Shows & Events
            </RouterLink>
            <RouterLink
              to="/tickets"
              class="text-[#cbd5e1] font-medium hover:text-[#86cffb] transition-colors duration-200 pb-0.5"
              active-class="text-[#86cffb] border-b-2 border-[#86cffb] font-bold"
            >
              My Tickets
            </RouterLink>
            <RouterLink
              to="/help"
              class="text-[#cbd5e1] font-medium hover:text-[#86cffb] transition-colors duration-200 pb-0.5"
              active-class="text-[#86cffb] border-b-2 border-[#86cffb] font-bold"
            >
              Venues & FAQ
            </RouterLink>
            <RouterLink
              to="/support"
              class="text-[#cbd5e1] font-medium hover:text-[#86cffb] transition-colors duration-200 flex items-center gap-1.5 pb-0.5"
              active-class="text-[#86cffb] border-b-2 border-[#86cffb] font-bold"
            >
              <span class="material-symbols-outlined text-[16px]">smart_toy</span>
              <span>Support AI</span>
            </RouterLink>
          </nav>
        </div>

        <!-- Global Search (Center Desktop) -->
        <div class="hidden md:flex max-w-md w-full mx-6 relative">
          <input
            v-model="catalog.searchQuery"
            @keydown.enter="onSearchSubmit"
            type="text"
            placeholder="Search events..."
            class="w-full bg-[#141e2e] border border-[#23354d] text-[#f1f5f9] placeholder-[#94a3b8] rounded-full py-2 pl-10 pr-4 font-mono text-xs focus:border-[#86cffb] focus:ring-1 focus:ring-[#86cffb] transition-colors outline-none"
          />
          <span class="material-symbols-outlined absolute left-3 top-2.5 text-[#94a3b8] text-sm pointer-events-none">search</span>
        </div>

        <!-- Trailing Actions (Right) -->
        <div class="flex items-center gap-3">
          <!-- Active Lock Countdown Pill -->
          <RouterLink
            v-if="ticketing.selectedSeats.length > 0"
            :to="{ name: 'checkout', params: { id: 'chronos-fest-2026' } }"
            class="hidden sm:flex items-center gap-2 bg-[#00688f]/30 border border-[#86cffb] text-[#86cffb] px-3 py-1.5 rounded-full text-xs font-mono font-semibold hover:bg-[#00688f]/50 transition-colors animate-pulse"
          >
            <span class="material-symbols-outlined text-[16px]">lock_clock</span>
            <span>{{ ticketing.selectedSeats.length }} Reservadas ({{ ticketing.formattedLockTime }})</span>
          </RouterLink>

          <!-- Notifications Radix Popover -->
          <PopoverRoot>
            <PopoverTrigger as-child>
              <button
                aria-label="Notificaciones"
                class="p-2 text-[#cbd5e1] hover:text-[#86cffb] transition-colors relative rounded-full hover:bg-[#1a293e] cursor-pointer"
              >
                <span class="material-symbols-outlined text-[20px]">notifications</span>
                <span class="absolute top-1.5 right-1.5 w-2 h-2 bg-[#86cffb] rounded-full ring-2 ring-[#0e1927]"></span>
              </button>
            </PopoverTrigger>

            <PopoverPortal>
              <PopoverContent
                class="bg-[#141e2e] border border-[#23354d] rounded-2xl p-4 shadow-2xl z-50 w-80 text-sm outline-none"
                :side-offset="8"
                align="end"
              >
                <div class="flex items-center justify-between pb-3 border-b border-[#23354d]">
                  <div class="flex items-center gap-2 font-display font-bold text-sm text-[#f1f5f9]">
                    <span class="material-symbols-outlined text-[#86cffb] text-base">notifications</span>
                    <span>Notificaciones</span>
                  </div>
                  <PopoverClose class="text-[#94a3b8] hover:text-white p-1 rounded transition-colors cursor-pointer">
                    <span class="material-symbols-outlined text-xs">close</span>
                  </PopoverClose>
                </div>

                <div class="py-2 flex flex-col gap-2.5">
                  <div class="p-2.5 rounded-xl bg-[#101825] border border-[#23354d] flex flex-col gap-1">
                    <div class="flex items-center gap-1.5 text-xs text-[#86cffb] font-mono font-semibold">
                      <span class="material-symbols-outlined text-[14px]">confirmation_number</span>
                      <span>Entradas Emitidas</span>
                    </div>
                    <p class="text-xs text-[#cbd5e1]">Tus tickets para Chronos Festival tienen QR dinámico activo.</p>
                  </div>

                  <div class="p-2.5 rounded-xl bg-[#101825] border border-[#23354d] flex flex-col gap-1">
                    <div class="flex items-center gap-1.5 text-xs text-[#4fdbc8] font-mono font-semibold">
                      <span class="material-symbols-outlined text-[14px]">lock_clock</span>
                      <span>Garantía Redlock</span>
                    </div>
                    <p class="text-xs text-[#cbd5e1]">10 minutos de reserva atómica garantizados durante la compra.</p>
                  </div>
                </div>
              </PopoverContent>
            </PopoverPortal>
          </PopoverRoot>

          <!-- User Profile Radix DropdownMenu -->
          <DropdownMenuRoot>
            <DropdownMenuTrigger as-child>
              <button
                class="w-8 h-8 rounded-full bg-[#1a293e] overflow-hidden border border-[#23354d] hover:border-[#86cffb] transition-colors flex items-center justify-center cursor-pointer outline-none"
                aria-label="Menú de Usuario"
              >
                <img
                  alt="User profile"
                  class="w-full h-full object-cover"
                  src="https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&w=100&q=80"
                />
              </button>
            </DropdownMenuTrigger>

            <DropdownMenuPortal>
              <DropdownMenuContent
                class="bg-[#141e2e] border border-[#23354d] rounded-2xl p-2 shadow-2xl z-50 min-w-[220px] text-sm outline-none"
                :side-offset="8"
                align="end"
              >
                <div class="px-3 py-2 border-b border-[#23354d] mb-1">
                  <p class="font-display font-bold text-sm text-[#f1f5f9]">Carlos Mendoza</p>
                  <p class="font-mono text-[11px] text-[#94a3b8]">carlos.m@amaryllis.com</p>
                </div>

                <DropdownMenuItem
                  @click="router.push({ name: 'tickets' })"
                  class="flex items-center gap-2.5 px-3 py-2 rounded-xl text-xs text-[#cbd5e1] hover:text-[#86cffb] hover:bg-[#1a293e] focus:bg-[#1a293e] focus:text-[#86cffb] cursor-pointer outline-none transition-colors"
                >
                  <span class="material-symbols-outlined text-sm">confirmation_number</span>
                  <span>Mis Entradas (3 activas)</span>
                </DropdownMenuItem>

                <DropdownMenuItem
                  @click="router.push({ name: 'help' })"
                  class="flex items-center gap-2.5 px-3 py-2 rounded-xl text-xs text-[#cbd5e1] hover:text-[#86cffb] hover:bg-[#1a293e] focus:bg-[#1a293e] focus:text-[#86cffb] cursor-pointer outline-none transition-colors"
                >
                  <span class="material-symbols-outlined text-sm">help</span>
                  <span>Centro de Ayuda</span>
                </DropdownMenuItem>

                <DropdownMenuItem
                  @click="router.push({ name: 'support-chat' })"
                  class="flex items-center gap-2.5 px-3 py-2 rounded-xl text-xs text-[#4fdbc8] hover:bg-[#1a293e] focus:bg-[#1a293e] cursor-pointer outline-none transition-colors"
                >
                  <span class="material-symbols-outlined text-sm">smart_toy</span>
                  <span>Asistente IA Concierge</span>
                </DropdownMenuItem>

                <DropdownMenuSeparator class="h-px bg-[#23354d] my-1" />

                <DropdownMenuItem
                  class="flex items-center gap-2.5 px-3 py-2 rounded-xl text-xs text-error hover:bg-[#1a293e] focus:bg-[#1a293e] cursor-pointer outline-none transition-colors"
                >
                  <span class="material-symbols-outlined text-sm">logout</span>
                  <span>Cerrar Sesión</span>
                </DropdownMenuItem>
              </DropdownMenuContent>
            </DropdownMenuPortal>
          </DropdownMenuRoot>

          <!-- Mobile Hamburger -->
          <button
            @click="isMobileMenuOpen = !isMobileMenuOpen"
            class="md:hidden p-2 text-on-surface-variant hover:text-on-surface"
            aria-label="Abrir menú"
          >
            <span class="material-symbols-outlined">{{ isMobileMenuOpen ? 'close' : 'menu' }}</span>
          </button>
        </div>
      </div>

      <!-- Mobile Nav Drawer -->
      <div v-if="isMobileMenuOpen" class="md:hidden bg-[#101825] border-b border-[#23354d] p-4 flex flex-col gap-2">
        <div class="relative w-full mb-2">
          <input
            v-model="catalog.searchQuery"
            @keydown.enter="isMobileMenuOpen = false; onSearchSubmit()"
            type="text"
            placeholder="Search events..."
            class="w-full bg-[#141e2e] border border-[#23354d] text-[#f1f5f9] placeholder-[#94a3b8] rounded-full py-2 pl-10 pr-4 text-xs font-mono outline-none"
          />
          <span class="material-symbols-outlined absolute left-3 top-2.5 text-[#94a3b8] text-sm">search</span>
        </div>
        <RouterLink to="/" @click="isMobileMenuOpen = false" class="py-2 px-3 rounded hover:bg-[#1a293e] text-[#f1f5f9]">Events</RouterLink>
        <RouterLink to="/tickets" @click="isMobileMenuOpen = false" class="py-2 px-3 rounded hover:bg-[#1a293e] text-[#f1f5f9]">My Tickets</RouterLink>
        <RouterLink to="/help" @click="isMobileMenuOpen = false" class="py-2 px-3 rounded hover:bg-[#1a293e] text-[#f1f5f9]">Venues & FAQ</RouterLink>
        <RouterLink to="/support" @click="isMobileMenuOpen = false" class="py-2 px-3 rounded hover:bg-[#1a293e] text-[#4fdbc8] flex items-center gap-2">
          <span class="material-symbols-outlined text-[16px]">smart_toy</span>
          <span>Support AI</span>
        </RouterLink>
      </div>
    </header>

    <!-- Main View Outlet -->
    <main class="flex-1 w-full pb-16 lg:pb-0">
      <RouterView />
      <slot />
    </main>

    <!-- Global Footer -->
    <footer class="bg-[#070f19] border-t border-[#23354d] py-10 px-4 md:px-10 mt-auto">
      <div class="max-w-[1280px] mx-auto grid grid-cols-1 md:grid-cols-4 gap-8 text-sm">
        <div class="flex flex-col gap-2">
          <span class="font-display font-bold text-lg text-[#86cffb] tracking-tight">Amaryllis</span>
          <p class="text-[#94a3b8] text-xs leading-relaxed">
            High-concurrency cloud ticketing platform. Microservices event-driven architecture, distributed Redlock seat reservations, and transactional outbox zero-oversell guarantee.
          </p>
        </div>

        <div class="flex flex-col gap-2">
          <span class="font-mono text-xs uppercase text-[#899299] tracking-wider">Architecture & Standards</span>
          <ul class="text-xs text-[#94a3b8] space-y-1.5 font-mono">
            <li class="flex items-center gap-1.5">
              <span class="material-symbols-outlined text-[14px] text-[#4fdbc8]">verified</span>
              <span>Java 25 & Spring Boot 4</span>
            </li>
            <li class="flex items-center gap-1.5">
              <span class="material-symbols-outlined text-[14px] text-[#86cffb]">lock</span>
              <span>Atomic Redlock 10m TTL</span>
            </li>
            <li class="flex items-center gap-1.5">
              <span class="material-symbols-outlined text-[14px] text-[#7bd0ff]">account_balance_wallet</span>
              <span>Transactional Outbox Saga</span>
            </li>
          </ul>
        </div>

        <div class="flex flex-col gap-2">
          <span class="font-mono text-xs uppercase text-[#899299] tracking-wider">Navigation</span>
          <ul class="text-xs text-[#94a3b8] space-y-1.5">
            <li><RouterLink to="/events" class="hover:text-[#86cffb] transition-colors">Events Catalog</RouterLink></li>
            <li><RouterLink to="/tickets" class="hover:text-[#86cffb] transition-colors">My Active Tickets</RouterLink></li>
            <li><RouterLink to="/help" class="hover:text-[#86cffb] transition-colors">Help Center & Resolution</RouterLink></li>
            <li><RouterLink to="/support" class="hover:text-[#86cffb] transition-colors">Amaryllis Support AI</RouterLink></li>
          </ul>
        </div>

        <div class="flex flex-col gap-2">
          <span class="font-mono text-xs uppercase text-[#899299] tracking-wider">Gateway & Infrastructure</span>
          <div class="bg-[#101825] p-3 rounded-lg border border-[#23354d] flex flex-col gap-1.5">
            <div class="flex items-center justify-between text-xs">
              <span class="text-[#899299]">Rate Limiting:</span>
              <span class="text-[#4fdbc8] font-mono flex items-center gap-1">
                <span class="w-1.5 h-1.5 rounded-full bg-[#4fdbc8]"></span> 100 req/min
              </span>
            </div>
            <div class="flex items-center justify-between text-xs pt-1 border-t border-[#23354d]/50">
              <span class="text-[#899299]">Virtual Waiting Room:</span>
              <span class="text-[#86cffb] font-mono">Redis FIFO Ready</span>
            </div>
          </div>
        </div>
      </div>
      <div class="max-w-[1280px] mx-auto mt-8 pt-4 border-t border-[#23354d]/50 flex flex-col sm:flex-row items-center justify-between text-xs text-[#899299] font-mono">
        <span>© 2026 Amaryllis Software. All rights reserved.</span>
        <span>B2C Public Client • www.amaryllis-tickets.com</span>
      </div>
    </footer>

    <!-- BottomNavBar (Mobile Only) matching wireframe lines 400-417 -->
    <nav class="lg:hidden fixed bottom-0 left-0 w-full z-50 flex justify-around items-center px-4 py-2 pb-safe bg-[#0e1927] shadow-[0px_-4px_20px_rgba(0,0,0,0.5)] border-t border-[#23354d]">
      <RouterLink
        to="/"
        class="flex flex-col items-center justify-center py-1 px-4 rounded-xl transition-all"
        :class="route.name === 'home' ? 'bg-[#00688f] text-white shadow-sm' : 'text-[#94a3b8] hover:text-[#86cffb]'"
      >
        <span class="material-symbols-outlined text-[20px]">home</span>
        <span class="font-mono text-[10px] mt-0.5">Home</span>
      </RouterLink>

      <button
        @click="catalog.searchQuery = ''; router.push({ name: 'home' })"
        class="flex flex-col items-center justify-center py-1 px-4 rounded-xl text-[#94a3b8] hover:text-[#86cffb] transition-all"
      >
        <span class="material-symbols-outlined text-[20px]">search</span>
        <span class="font-mono text-[10px] mt-0.5">Search</span>
      </button>

      <RouterLink
        to="/tickets"
        class="flex flex-col items-center justify-center py-1 px-4 rounded-xl transition-all"
        :class="route.name === 'tickets' ? 'bg-[#00688f] text-white shadow-sm' : 'text-[#94a3b8] hover:text-[#86cffb]'"
      >
        <span class="material-symbols-outlined text-[20px]">confirmation_number</span>
        <span class="font-mono text-[10px] mt-0.5">Tickets</span>
      </RouterLink>

      <RouterLink
        to="/support"
        class="flex flex-col items-center justify-center py-1 px-4 rounded-xl transition-all"
        :class="route.name === 'support-chat' ? 'bg-[#00688f] text-white shadow-sm' : 'text-[#94a3b8] hover:text-[#86cffb]'"
      >
        <span class="material-symbols-outlined text-[20px]">smart_toy</span>
        <span class="font-mono text-[10px] mt-0.5">Support</span>
      </RouterLink>
    </nav>
  </div>
</template>
