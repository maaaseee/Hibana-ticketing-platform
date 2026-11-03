<template>
  <div class="min-h-screen bg-canvas-base flex flex-col md:flex-row text-on-surface">
    <!-- Left Navigation Sidebar -->
    <aside
      class="w-full md:w-64 md:h-screen md:sticky md:top-0 bg-surface-container-low border-r border-border-subtle flex flex-col shrink-0 overflow-hidden z-40"
    >
      <!-- Brand & Title -->
      <div class="p-5 border-b border-border-subtle flex items-center justify-between">
        <div class="flex items-center gap-3">
          <div
            class="w-10 h-10 rounded-xl bg-gradient-to-br from-primary to-secondary flex items-center justify-center shadow-lg shadow-primary/20 text-on-primary font-black"
          >
            <span class="material-symbols-outlined text-xl">token</span>
          </div>
          <div>
            <div class="flex items-center gap-1.5">
              <span class="font-display font-black tracking-tight text-high-emphasis text-base">AMARYLLIS</span>
              <span class="text-[10px] font-mono px-1.5 py-0.5 rounded bg-primary/10 text-primary border border-primary/20 uppercase font-semibold">Ops</span>
            </div>
            <div class="text-[11px] font-mono text-muted tracking-wider">Hibana Back-Office</div>
          </div>
        </div>
      </div>

      <!-- Main Navigation Menu -->
      <nav class="flex-1 p-3 space-y-1 overflow-y-auto">
        <div class="px-3 pt-2 pb-1 text-[10px] font-mono uppercase tracking-wider text-muted">
          Gestión del Catálogo
        </div>

        <RouterLink
          to="/events"
          :class="[
            'flex items-center gap-3 px-3 py-2.5 rounded-xl text-sm font-medium transition group',
            $route.path.startsWith('/events')
              ? 'bg-surface-elevated text-primary font-semibold border border-primary/30 shadow-sm'
              : 'text-on-surface-variant hover:text-high-emphasis hover:bg-surface-container'
          ]"
        >
          <span
            :class="[
              'material-symbols-outlined transition',
              $route.path.startsWith('/events') ? 'text-primary' : 'text-muted group-hover:text-high-emphasis'
            ]"
          >
            festival
          </span>
          <span>Eventos</span>
          <span
            v-if="$route.path === '/events/new'"
            class="ml-auto text-[10px] font-mono bg-secondary/20 text-secondary px-1.5 py-0.5 rounded"
          >
            Nuevo
          </span>
        </RouterLink>

        <RouterLink
          to="/venues"
          :class="[
            'flex items-center gap-3 px-3 py-2.5 rounded-xl text-sm font-medium transition group',
            $route.path.startsWith('/venues')
              ? 'bg-surface-elevated text-primary font-semibold border border-primary/30 shadow-sm'
              : 'text-on-surface-variant hover:text-high-emphasis hover:bg-surface-container'
          ]"
        >
          <span
            :class="[
              'material-symbols-outlined transition',
              $route.path.startsWith('/venues') ? 'text-primary' : 'text-muted group-hover:text-high-emphasis'
            ]"
          >
            apartment
          </span>
          <span>Recintos</span>
        </RouterLink>

        <div class="px-3 pt-4 pb-1 text-[10px] font-mono uppercase tracking-wider text-muted">
          Operaciones & Auditoría
        </div>

        <div
          class="flex items-center justify-between px-3 py-2.5 rounded-xl text-sm text-muted opacity-60 cursor-not-allowed"
        >
          <div class="flex items-center gap-3">
            <span class="material-symbols-outlined">support_agent</span>
            <span>Mesa de Soporte</span>
          </div>
          <span class="text-[9px] font-mono bg-surface-card px-1.5 py-0.5 rounded text-muted-light">Pronto</span>
        </div>

        <div
          class="flex items-center justify-between px-3 py-2.5 rounded-xl text-sm text-muted opacity-60 cursor-not-allowed"
        >
          <div class="flex items-center gap-3">
            <span class="material-symbols-outlined">dns</span>
            <span>Telemetría</span>
          </div>
          <span class="text-[9px] font-mono bg-surface-card px-1.5 py-0.5 rounded text-muted-light">Pronto</span>
        </div>
      </nav>

      <!-- Bottom Platform Status Widget -->
      <div class="p-4 border-t border-border-subtle bg-surface-container-deep/50 space-y-3 shrink-0">
        <div class="flex items-center justify-between text-xs">
          <div class="flex items-center gap-2">
            <span class="w-2 h-2 rounded-full bg-secondary animate-ping"></span>
            <span class="text-high-emphasis font-medium text-[11px]">Plataforma Operativa</span>
          </div>
          <span class="font-mono text-[10px] text-secondary">99.98%</span>
        </div>

        <div class="text-[10px] font-mono text-muted space-y-1">
          <div class="flex justify-between">
            <span>Gateway:</span>
            <span class="text-on-surface">:8080 (Active)</span>
          </div>
          <div class="flex justify-between">
            <span>Auth Provider:</span>
            <span class="text-on-surface">Floci-AZ Entra</span>
          </div>
        </div>

        <a
          href="http://localhost:5173"
          target="_blank"
          rel="noopener noreferrer"
          class="flex items-center justify-center gap-2 w-full py-2 px-3 rounded-xl bg-surface-elevated hover:bg-surface-interactive text-primary text-xs font-semibold border border-border-subtle transition"
        >
          <span class="material-symbols-outlined text-sm">open_in_new</span>
          Ver Tienda B2C (:5173)
        </a>
      </div>
    </aside>

    <!-- Main Content Area with Header -->
    <div class="flex-1 flex flex-col min-w-0">
      <!-- Top Sticky Header -->
      <header
        class="sticky top-0 z-30 bg-surface/90 backdrop-blur-xl border-b border-border-subtle px-6 py-3.5 flex items-center justify-between gap-4"
      >
        <div class="flex items-center gap-4">
          <div class="flex items-center gap-2 px-3 py-1 rounded-full bg-surface-container border border-border-subtle text-xs font-mono text-muted-light">
            <span class="w-2 h-2 rounded-full bg-primary"></span>
            <span>Cluster:</span>
            <span class="text-high-emphasis font-semibold">local-floci-az</span>
          </div>
        </div>

        <div class="flex items-center gap-3">
          <!-- Dev Identity & Role Switcher -->
          <DevAuthSwitcher />
        </div>
      </header>

      <!-- Router Content Outlet -->
      <main class="flex-1 p-6 md:p-8 max-w-7xl w-full mx-auto">
        <RouterView />
      </main>
    </div>
  </div>
</template>

<script setup lang="ts">
import { RouterLink, RouterView } from 'vue-router'
import DevAuthSwitcher from '@/components/auth/DevAuthSwitcher.vue'
</script>
