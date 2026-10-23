<script setup lang="ts">
import { RouterView, useRoute } from 'vue-router'
import AppNavbar from '../components/ui/AppNavbar.vue'

const route = useRoute()
</script>

<template>
  <div class="min-h-screen flex flex-col bg-canvas-base text-high-emphasis font-body selection:bg-primary-container selection:text-white">
    <!-- Navbar global encapsulada en su propio componente con soporte de Sidebar móvil -->
    <AppNavbar />

    <!-- Main View Outlet -->
    <main class="flex-1 w-full pb-16 lg:pb-0">
      <RouterView />
      <slot />
    </main>

    <!-- Global Footer -->
    <footer class="bg-surface-container-lowest border-t border-border-subtle py-10 px-4 md:px-10 mt-auto">
      <div class="max-w-[1280px] mx-auto grid grid-cols-1 md:grid-cols-4 gap-8 text-sm">
        <div class="flex flex-col gap-2">
          <span class="font-display font-bold text-lg text-primary tracking-tight">Amaryllis</span>
          <p class="text-muted-light text-xs leading-relaxed">
            High-concurrency cloud ticketing platform. Microservices event-driven architecture, distributed Redlock seat reservations, and transactional outbox zero-oversell guarantee.
          </p>
        </div>

        <div class="flex flex-col gap-2">
          <span class="font-mono text-xs uppercase text-outline tracking-wider">Architecture & Standards</span>
          <ul class="text-xs text-muted-light space-y-1.5 font-mono">
            <li class="flex items-center gap-1.5">
              <span class="material-symbols-outlined text-[14px] text-secondary">verified</span>
              <span>Java 25 & Spring Boot 4</span>
            </li>
            <li class="flex items-center gap-1.5">
              <span class="material-symbols-outlined text-[14px] text-primary">lock</span>
              <span>Atomic Redlock 10m TTL</span>
            </li>
            <li class="flex items-center gap-1.5">
              <span class="material-symbols-outlined text-[14px] text-tertiary">account_balance_wallet</span>
              <span>Transactional Outbox Saga</span>
            </li>
          </ul>
        </div>

        <div class="flex flex-col gap-2">
          <span class="font-mono text-xs uppercase text-outline tracking-wider">Navigation</span>
          <ul class="text-xs text-muted-light space-y-1.5">
            <li><RouterLink to="/events" class="hover:text-primary transition-colors">Events Catalog</RouterLink></li>
            <li><RouterLink to="/tickets" class="hover:text-primary transition-colors">My Active Tickets</RouterLink></li>
            <li><RouterLink to="/help" class="hover:text-primary transition-colors">Help Center & Resolution</RouterLink></li>
            <li><RouterLink to="/support" class="hover:text-primary transition-colors">Amaryllis Support AI</RouterLink></li>
          </ul>
        </div>

        <div class="flex flex-col gap-2">
          <span class="font-mono text-xs uppercase text-outline tracking-wider">Gateway & Infrastructure</span>
          <div class="bg-surface-container-deep p-3 rounded-lg border border-border-subtle flex flex-col gap-1.5">
            <div class="flex items-center justify-between text-xs">
              <span class="text-outline">Rate Limiting:</span>
              <span class="text-secondary font-mono flex items-center gap-1">
                <span class="w-1.5 h-1.5 rounded-full bg-secondary"></span> 100 req/min
              </span>
            </div>
            <div class="flex items-center justify-between text-xs pt-1 border-t border-border-subtle/50">
              <span class="text-outline">Virtual Waiting Room:</span>
              <span class="text-primary font-mono">Redis FIFO Ready</span>
            </div>
          </div>
        </div>
      </div>
      <div class="max-w-[1280px] mx-auto mt-8 pt-4 border-t border-border-subtle/50 flex flex-col sm:flex-row items-center justify-between text-xs text-outline font-mono">
        <span>© 2026 Amaryllis Software. All rights reserved.</span>
        <span>B2C Public Client • www.amaryllis-tickets.com</span>
      </div>
    </footer>

    <!-- BottomNavBar (Mobile Only) -->
    <nav class="lg:hidden fixed bottom-0 left-0 w-full z-50 flex justify-around items-center px-4 py-2 pb-safe bg-surface-dim shadow-[0px_-4px_20px_rgba(0,0,0,0.5)] border-t border-border-subtle">
      <RouterLink
        to="/"
        class="flex flex-col items-center justify-center py-1 px-4 rounded-xl transition-all"
        :class="route.name === 'home' ? 'bg-primary-container text-white shadow-sm' : 'text-muted-light hover:text-primary'"
      >
        <span class="material-symbols-outlined text-[20px]">home</span>
        <span class="font-mono text-[10px] mt-0.5">Home</span>
      </RouterLink>

      <RouterLink
        to="/events"
        class="flex flex-col items-center justify-center py-1 px-4 rounded-xl transition-all"
        :class="route.name === 'events-catalog' ? 'bg-primary-container text-white shadow-sm' : 'text-muted-light hover:text-primary'"
      >
        <span class="material-symbols-outlined text-[20px]">theater_comedy</span>
        <span class="font-mono text-[10px] mt-0.5">Shows</span>
      </RouterLink>

      <RouterLink
        to="/tickets"
        class="flex flex-col items-center justify-center py-1 px-4 rounded-xl transition-all"
        :class="route.name === 'tickets' ? 'bg-primary-container text-white shadow-sm' : 'text-muted-light hover:text-primary'"
      >
        <span class="material-symbols-outlined text-[20px]">confirmation_number</span>
        <span class="font-mono text-[10px] mt-0.5">Tickets</span>
      </RouterLink>

      <RouterLink
        to="/help"
        class="flex flex-col items-center justify-center py-1 px-4 rounded-xl transition-all"
        :class="route.name === 'help' ? 'bg-primary-container text-white shadow-sm' : 'text-muted-light hover:text-primary'"
      >
        <span class="material-symbols-outlined text-[20px]">help</span>
        <span class="font-mono text-[10px] mt-0.5">Ayuda</span>
      </RouterLink>
    </nav>
  </div>
</template>
