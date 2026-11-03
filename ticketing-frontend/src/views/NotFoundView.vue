<script setup lang="ts">
import { ref } from 'vue'
import { RouterLink } from 'vue-router'
import AppLogo from '../components/ui/AppLogo.vue'
import GlobalSearchModal from '../components/search/GlobalSearchModal.vue'

const isSearchOpen = ref(false)
</script>

<template>
  <div class="min-h-screen bg-[#020b1f] text-white flex flex-col justify-between select-none relative overflow-hidden font-body">
    <!-- Ambient Stage Floodlights -->
    <div class="absolute -top-40 left-1/2 -translate-x-1/2 w-[700px] h-[400px] bg-radial from-[#0084ff]/20 via-transparent to-transparent pointer-events-none blur-3xl"></div>
    <div class="absolute -bottom-40 left-1/4 w-[500px] h-[350px] bg-radial from-rose-900/15 via-transparent to-transparent pointer-events-none blur-3xl"></div>

    <!-- Header bar -->
    <header class="relative z-10 w-full max-w-7xl mx-auto px-6 py-6 flex items-center justify-between">
      <AppLogo size="lg" />
      <RouterLink
        to="/"
        class="text-xs sm:text-sm font-display font-semibold text-[#38bdf8] hover:text-white transition-colors flex items-center gap-1.5"
      >
        <span class="material-symbols-outlined text-[18px]">arrow_back</span>
        <span>Volver a Inicio</span>
      </RouterLink>
    </header>

    <!-- Center Stage 404 Hero -->
    <main class="relative z-10 flex-1 flex flex-col items-center justify-center px-6 text-center max-w-2xl mx-auto py-12">
      <!-- Stadium Spotlight Icon -->
      <div class="relative mb-6">
        <div class="w-24 h-24 rounded-3xl bg-[#0c1524]/90 border border-[#22354e] shadow-[0_0_40px_rgba(0,132,255,0.25)] flex items-center justify-center text-[#38bdf8]">
          <span class="material-symbols-outlined text-5xl animate-pulse">theater_comedy</span>
        </div>
        <span class="absolute -top-2 -right-2 px-2.5 py-0.5 rounded-full bg-rose-600 text-white font-mono text-[10px] font-bold uppercase tracking-widest shadow-md">
          404
        </span>
      </div>

      <!-- Monumental Headline -->
      <h1 class="font-display font-black text-4xl sm:text-6xl text-white tracking-tight uppercase leading-none mb-3 drop-shadow-[0_10px_30px_rgba(0,0,0,0.8)]">
        ESCENARIO<br />
        <span class="text-transparent bg-clip-text bg-gradient-to-r from-[#38bdf8] to-[#0084ff]">
          APAGADO
        </span>
      </h1>

      <p class="font-body text-sm sm:text-base text-[#94a3b8] max-w-md mx-auto mb-8 leading-relaxed">
        El espectáculo, fecha o butaca que estás buscando no se encuentra en cartelera o cambió de recinto.
      </p>

      <!-- Action Buttons -->
      <div class="flex flex-col sm:flex-row items-center gap-3 w-full max-w-md">
        <RouterLink
          to="/events"
          class="w-full sm:w-1/2 py-3.5 rounded-full bg-[#0084ff] hover:bg-[#0070df] text-white font-display font-bold text-sm tracking-wide transition-all shadow-[0_0_25px_rgba(0,132,255,0.45)] flex items-center justify-center gap-2 cursor-pointer active:scale-98"
        >
          <span class="material-symbols-outlined text-[18px]">confirmation_number</span>
          <span>Ver Cartelera</span>
        </RouterLink>

        <button
          type="button"
          @click="isSearchOpen = true"
          class="w-full sm:w-1/2 py-3.5 rounded-full bg-[#0c1524] hover:bg-[#141f30] text-[#cbd5e1] hover:text-white font-display font-bold text-sm tracking-wide border border-[#22354e] hover:border-[#38bdf8]/50 transition-all flex items-center justify-center gap-2 cursor-pointer"
        >
          <span class="material-symbols-outlined text-[18px]">search</span>
          <span>Buscar Show (⌘K)</span>
        </button>
      </div>
    </main>

    <!-- Footer Note -->
    <footer class="relative z-10 w-full py-6 text-center text-xs font-mono text-[#64748b]">
      Hibana.tk • Sistema Oficial de Entradas y Recintos
    </footer>

    <!-- Global Search Modal instance -->
    <GlobalSearchModal v-model:open="isSearchOpen" />
  </div>
</template>
