<script setup lang="ts">
import { RouterView, useRoute } from 'vue-router'
import AppNavbar from '../components/ui/AppNavbar.vue'

const route = useRoute()
</script>

<template>
  <div
    class="min-h-screen flex flex-col bg-canvas-base text-high-emphasis font-body selection:bg-primary-container selection:text-white"
    :class="{ 'h-screen overflow-hidden': route.meta.fullscreen }"
  >
    <!-- Navbar global encapsulada en su propio componente con soporte de Sidebar móvil -->
    <AppNavbar v-if="!route.meta.hideNavbar" />

    <!-- Main View Outlet -->
    <main
      class="flex-1 w-full"
      :class="{
        'pb-16 lg:pb-0': !route.meta.fullscreen,
        'h-full overflow-hidden flex flex-col': route.meta.fullscreen
      }"
    >
      <RouterView />
      <slot />
    </main>

    <!-- Global Commercial B2C Footer (Option 1: Official Entertainment & Trust Platform) -->
    <footer
      v-if="!route.meta.hideFooter"
      class="bg-surface-container-lowest border-t border-border-subtle pt-14 pb-12 px-4 sm:px-6 lg:px-10 mt-auto text-high-emphasis"
    >
      <div class="max-w-7xl mx-auto">
        <!-- Main 4-Column Grid -->
        <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-10 lg:gap-8 pb-12 border-b border-border-subtle/60 text-sm">
          
          <!-- Column 1: Brand & Purpose -->
          <div class="flex flex-col gap-4">
            <RouterLink to="/" class="flex items-center gap-2.5 group shrink-0">
              <img
                src="/logo.png"
                alt="Amaryllis Logo"
                class="w-8 h-8 object-contain drop-shadow-[0_0_10px_rgba(134,207,251,0.4)]"
              />
              <span class="font-display font-black text-2xl text-primary tracking-tight group-hover:text-on-primary-container transition-colors">
                Amaryllis
              </span>
            </RouterLink>
            <p class="text-muted-light text-xs leading-relaxed max-w-xs">
              Plataforma oficial de venta de entradas para los mayores recitales, festivales en vivo y espectáculos teatrales de Buenos Aires. Entradas 100% oficiales y transferencias seguras.
            </p>
            <div class="flex items-center gap-3 pt-1">
              <a
                href="#"
                class="w-8 h-8 rounded-full bg-surface-card hover:bg-surface-interactive border border-border-subtle flex items-center justify-center text-medium-emphasis hover:text-primary transition-colors"
                aria-label="Instagram"
              >
                <span class="material-symbols-outlined text-sm">photo_camera</span>
              </a>
              <a
                href="#"
                class="w-8 h-8 rounded-full bg-surface-card hover:bg-surface-interactive border border-border-subtle flex items-center justify-center text-medium-emphasis hover:text-primary transition-colors"
                aria-label="X / Twitter"
              >
                <span class="material-symbols-outlined text-sm">tag</span>
              </a>
              <a
                href="#"
                class="w-8 h-8 rounded-full bg-surface-card hover:bg-surface-interactive border border-border-subtle flex items-center justify-center text-medium-emphasis hover:text-primary transition-colors"
                aria-label="YouTube"
              >
                <span class="material-symbols-outlined text-sm">play_circle</span>
              </a>
              <a
                href="#"
                class="w-8 h-8 rounded-full bg-surface-card hover:bg-surface-interactive border border-border-subtle flex items-center justify-center text-medium-emphasis hover:text-primary transition-colors"
                aria-label="Spotify"
              >
                <span class="material-symbols-outlined text-sm">headphones</span>
              </a>
            </div>
          </div>

          <!-- Column 2: Cartelera & Recintos -->
          <div class="flex flex-col gap-3">
            <span class="font-display text-xs uppercase font-bold text-white tracking-wider">Cartelera & Shows</span>
            <ul class="text-xs text-muted-light space-y-2.5">
              <li>
                <RouterLink :to="{ name: 'events-catalog' }" class="hover:text-primary transition-colors flex items-center gap-1.5">
                  <span class="w-1.5 h-1.5 rounded-full bg-primary"></span>
                  <span>Recitales en Estadio Monumental</span>
                </RouterLink>
              </li>
              <li>
                <RouterLink :to="{ name: 'events-catalog' }" class="hover:text-primary transition-colors flex items-center gap-1.5">
                  <span class="w-1.5 h-1.5 rounded-full bg-secondary"></span>
                  <span>Shows en Arena Buenos Aires</span>
                </RouterLink>
              </li>
              <li>
                <RouterLink :to="{ name: 'events-catalog' }" class="hover:text-primary transition-colors flex items-center gap-1.5">
                  <span class="w-1.5 h-1.5 rounded-full bg-tertiary"></span>
                  <span>Temporada Teatro Rex</span>
                </RouterLink>
              </li>
              <li>
                <RouterLink :to="{ name: 'events-catalog' }" class="hover:text-primary transition-colors">
                  Festivales & Música Urbana
                </RouterLink>
              </li>
              <li>
                <RouterLink :to="{ name: 'events-catalog' }" class="hover:text-primary transition-colors">
                  Conferencias & Congresos
                </RouterLink>
              </li>
            </ul>
          </div>

          <!-- Column 3: Atención al Espectador -->
          <div class="flex flex-col gap-3">
            <span class="font-display text-xs uppercase font-bold text-white tracking-wider">Atención al Espectador</span>
            <ul class="text-xs text-muted-light space-y-2.5">
              <li>
                <RouterLink :to="{ name: 'help' }" class="hover:text-primary transition-colors">
                  Centro de Ayuda & Preguntas Frecuentes
                </RouterLink>
              </li>
              <li>
                <RouterLink :to="{ name: 'tickets' }" class="hover:text-primary transition-colors">
                  Mis Entradas & Canje Digital
                </RouterLink>
              </li>
              <li>
                <RouterLink :to="{ name: 'help' }" class="hover:text-primary transition-colors">
                  Promociones Bancarias & Cuotas
                </RouterLink>
              </li>
              <li>
                <RouterLink :to="{ name: 'help' }" class="hover:text-primary transition-colors">
                  Accesibilidad en Recintos (PMR)
                </RouterLink>
              </li>
              <li>
                <RouterLink :to="{ name: 'support-chat' }" class="hover:text-primary transition-colors flex items-center gap-1.5">
                  <span class="material-symbols-outlined text-sm text-primary">chat</span>
                  <span>Asistente de Soporte en Vivo</span>
                </RouterLink>
              </li>
            </ul>
          </div>

          <!-- Column 4: Transparencia & Consumidor -->
          <div class="flex flex-col gap-3">
            <span class="font-display text-xs uppercase font-bold text-white tracking-wider">Transparencia & Legal</span>
            <ul class="text-xs text-muted-light space-y-2.5">
              <li>
                <RouterLink :to="{ name: 'help' }" class="text-primary hover:underline font-semibold flex items-center gap-1">
                  <span class="material-symbols-outlined text-sm">undo</span>
                  <span>Botón de Arrepentimiento</span>
                </RouterLink>
              </li>
              <li>
                <RouterLink :to="{ name: 'help' }" class="hover:text-primary transition-colors">
                  Defensa de las y los Consumidores
                </RouterLink>
              </li>
              <li>
                <RouterLink :to="{ name: 'help' }" class="hover:text-primary transition-colors">
                  Términos y Condiciones de Compra
                </RouterLink>
              </li>
              <li>
                <RouterLink :to="{ name: 'help' }" class="hover:text-primary transition-colors">
                  Políticas de Privacidad & Protección de Datos
                </RouterLink>
              </li>
              <li>
                <a href="mailto:contacto@amaryllis-tickets.com" class="hover:text-primary transition-colors">
                  Contacto Comercial & Productores
                </a>
              </li>
            </ul>
          </div>

        </div>

        <!-- Middle Trust Badges & Payment Methods Strip -->
        <div class="py-6 border-b border-border-subtle/40 flex flex-col md:flex-row items-center justify-between gap-4 text-xs text-muted-light">
          <!-- Medios de Pago -->
          <div class="flex flex-wrap items-center gap-2 text-medium-emphasis">
            <span class="font-mono text-[11px] uppercase tracking-wider text-muted-light mr-1">Medios de Pago:</span>
            <span class="px-2.5 py-1 rounded-md bg-surface-card border border-border-subtle font-mono text-[11px] font-bold text-white">VISA</span>
            <span class="px-2.5 py-1 rounded-md bg-surface-card border border-border-subtle font-mono text-[11px] font-bold text-white">Mastercard</span>
            <span class="px-2.5 py-1 rounded-md bg-surface-card border border-border-subtle font-mono text-[11px] font-bold text-white">AMEX</span>
            <span class="px-2.5 py-1 rounded-md bg-surface-card border border-border-subtle font-mono text-[11px] font-semibold text-primary">Santander</span>
            <span class="px-2.5 py-1 rounded-md bg-surface-card border border-border-subtle font-mono text-[11px] font-semibold text-primary">Galicia</span>
            <span class="px-2.5 py-1 rounded-md bg-surface-card border border-border-subtle font-mono text-[11px] font-semibold text-secondary">MODO</span>
          </div>

          <!-- Security Badges -->
          <div class="flex items-center gap-4 text-xs font-mono text-medium-emphasis">
            <div class="flex items-center gap-1.5 text-secondary">
              <span class="material-symbols-outlined text-[16px]">verified_user</span>
              <span>Compra 100% Oficial</span>
            </div>
            <span class="text-border-subtle">•</span>
            <div class="flex items-center gap-1.5 text-primary">
              <span class="material-symbols-outlined text-[16px]">lock</span>
              <span>Encriptación SSL 256-bit</span>
            </div>
          </div>
        </div>

        <!-- Bottom Copyright Line -->
        <div class="pt-6 flex flex-col sm:flex-row items-center justify-between gap-3 text-xs text-outline font-body">
          <p>© 2026 Amaryllis Tickets S.A. Todos los derechos reservados.</p>
          <p class="font-mono text-[11px]">Av. Figueroa Alcorta 7597, Núñez, Ciudad Autónoma de Buenos Aires, Argentina</p>
        </div>
      </div>
    </footer>

    <!-- BottomNavBar (Mobile Only) -->
    <nav
      v-if="!route.meta.hideNavbar"
      class="lg:hidden fixed bottom-0 left-0 w-full z-50 flex justify-around items-center px-4 py-2 pb-safe bg-surface-dim shadow-[0px_-4px_20px_rgba(0,0,0,0.5)] border-t border-border-subtle"
    >
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
