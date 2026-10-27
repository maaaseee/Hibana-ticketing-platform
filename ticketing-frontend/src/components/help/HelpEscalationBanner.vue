<script setup lang="ts">
import { RouterLink } from 'vue-router'
</script>

<template>
  <section class="bg-surface-card border border-border-subtle rounded-2xl p-6 sm:p-8 flex flex-col md:flex-row items-center justify-between gap-6 shadow-xl">
    <div class="flex items-start gap-4">
      <div class="w-12 h-12 rounded-xl bg-primary/10 border border-primary/20 text-primary flex items-center justify-center shrink-0 shadow-inner">
        <span class="material-symbols-outlined text-[28px]">support_agent</span>
      </div>
      <div class="flex flex-col gap-1 text-left">
        <h3 class="font-display text-lg sm:text-xl text-white font-bold tracking-tight">
          ¿No encontraste lo que buscabas o necesitas más ayuda?
        </h3>
        <p class="font-body text-sm sm:text-base text-muted-light max-w-xl leading-relaxed">
          Conéctate con nuestro asistente virtual inteligente para resolver incidentes con tus compras o transferir tu caso a un especialista en minutos.
        </p>
      </div>
    </div>

    <div class="flex flex-col sm:flex-row items-center gap-3 w-full md:w-auto shrink-0">
      <RouterLink
        :to="{ name: 'support-chat' }"
        class="w-full sm:w-auto bg-primary hover:bg-btn-buy-hover text-on-primary-fixed font-body text-sm font-bold px-6 py-3 rounded-xl flex items-center justify-center gap-2 transition-all shadow-md cursor-pointer"
      >
        <span class="material-symbols-outlined text-[20px]">chat</span>
        <span>Iniciar chat de soporte</span>
      </RouterLink>
      <RouterLink
        :to="{ name: 'tickets' }"
        class="w-full sm:w-auto bg-surface-elevated hover:bg-surface-interactive text-white px-5 py-3 rounded-xl font-body text-sm border border-border-subtle transition-all flex items-center justify-center gap-2 cursor-pointer"
      >
        <span class="material-symbols-outlined text-[18px]">receipt_long</span>
        <span>Ver mis tickets (3)</span>
      </RouterLink>
    </div>
  </section>
</template>
