<script setup lang="ts">
// Skeleton loader con efecto Shimmer Neón Stadium para evitar layout shift durante la carga
</script>

<template>
  <div
    data-testid="event-card-skeleton"
    class="rounded-2xl overflow-hidden flex flex-col bg-[#0c1524] border border-[#23354d]/60 shadow-md relative after:absolute after:inset-0 after:-translate-x-full after:animate-[shimmer_1.8s_infinite] after:bg-gradient-to-r after:from-transparent after:via-white/[0.07] after:to-transparent overflow-hidden"
  >
    <!-- Imagen placeholder -->
    <div class="w-full h-56 sm:h-64 bg-[#142033]/90 relative">
      <div class="absolute top-3 right-3 w-16 h-5 rounded-bl-md bg-[#1e2f47]/80"></div>
    </div>

    <!-- Contenido placeholder -->
    <div class="p-5 flex flex-col flex-1 justify-between gap-4">
      <div class="flex flex-col gap-2.5">
        <!-- Fecha -->
        <div class="w-24 h-3.5 rounded-full bg-[#1e2f47]/80"></div>

        <!-- Título (2 líneas simuladas) -->
        <div class="space-y-1.5 py-1">
          <div class="w-11/12 h-5 rounded-md bg-[#1e2f47]"></div>
          <div class="w-3/5 h-5 rounded-md bg-[#1e2f47]/70"></div>
        </div>

        <!-- Recinto -->
        <div class="flex items-center gap-2 pt-1">
          <div class="w-4 h-4 rounded-full bg-[#1e2f47]"></div>
          <div class="w-36 h-3 rounded-full bg-[#1e2f47]/60"></div>
        </div>
      </div>

      <!-- Footer con botones -->
      <div class="pt-3 border-t border-[#23354d]/50 flex items-center justify-between gap-3 mt-auto">
        <div class="w-16 h-6 rounded-lg bg-[#142033]"></div>
        <div class="w-24 h-8 rounded-full bg-[#00688f]/30 border border-[#38bdf8]/20"></div>
      </div>
    </div>
  </div>
</template>

<style scoped>
@keyframes shimmer {
  100% {
    transform: translateX(100%);
  }
}
</style>
