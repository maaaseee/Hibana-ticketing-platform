<script setup lang="ts">
import { RouterLink } from 'vue-router'
</script>

<template>
  <section class="w-full max-w-[80vw] mx-auto px-4 sm:px-6 lg:px-8">
    <div class="relative overflow-hidden bg-linear-to-br from-surface-card via-surface-elevated to-surface-container-deep border border-border-subtle rounded-3xl p-8 sm:p-12 md:p-16 flex flex-col lg:flex-row items-center justify-between gap-8 sm:gap-10 shadow-2xl">
      <!-- Resplandores ambientales de fondo -->
      <div class="absolute -right-24 -bottom-24 w-96 h-96 bg-primary/10 rounded-full blur-3xl pointer-events-none"></div>
      <div class="absolute -left-20 -top-20 w-72 h-72 bg-secondary/10 rounded-full blur-3xl pointer-events-none"></div>

      <div class="relative z-10 flex flex-col sm:flex-row items-center sm:items-start text-center sm:text-left gap-6 sm:gap-8 max-w-3xl">
        <!-- Icono de Gran Tamaño -->
        <div class="w-20 h-20 sm:w-24 sm:h-24 rounded-3xl bg-primary-container/20 border border-primary/40 flex items-center justify-center text-primary shrink-0 shadow-lg backdrop-blur-md">
          <span class="material-symbols-outlined text-4xl sm:text-5xl">help_outline</span>
        </div>

        <div class="flex flex-col gap-3">
          <div class="inline-flex items-center gap-2 self-center sm:self-start">
            <span class="px-3 py-1 rounded-full bg-surface-module border border-border-subtle font-mono text-[11px] font-bold text-secondary uppercase tracking-widest">
              Centro de Ayuda & Asistencia Oficial
            </span>
          </div>

          <h2 class="font-display text-2xl sm:text-3xl md:text-4xl font-extrabold text-high-emphasis tracking-tight">
            ¿Tienes preguntas o dudas sobre tu próximo evento?
          </h2>

          <p class="font-body text-base sm:text-lg text-muted-light leading-relaxed">
            Encuentra respuestas rápidas sobre entradas digitales con código QR dinámico, políticas de cancelación, estacionamiento oficial y accesibilidad.
          </p>

          <!-- Tags rápidos de temas comunes -->
          <div class="flex flex-wrap items-center justify-center sm:justify-start gap-2 pt-2 font-mono text-xs text-medium-emphasis">
            <span class="px-2.5 py-1 rounded-md bg-surface-card border border-border-subtle/80 flex items-center gap-1">
              <span class="material-symbols-outlined text-sm text-primary">qr_code_2</span>
              Entradas Digitales
            </span>
            <span class="px-2.5 py-1 rounded-md bg-surface-card border border-border-subtle/80 flex items-center gap-1">
              <span class="material-symbols-outlined text-sm text-secondary">local_parking</span>
              Acceso a Cocheras
            </span>
            <span class="px-2.5 py-1 rounded-md bg-surface-card border border-border-subtle/80 flex items-center gap-1">
              <span class="material-symbols-outlined text-sm text-primary">sync_alt</span>
              Transferencia de Tickets
            </span>
          </div>
        </div>
      </div>

      <div class="relative z-10 w-full sm:w-auto shrink-0 flex justify-center">
        <RouterLink
          to="/help"
          class="w-full sm:w-auto bg-primary-container hover:bg-ocean-primary-hover text-white font-display text-base sm:text-lg font-bold px-8 py-5 rounded-2xl border border-primary/40 shadow-[0_0_30px_rgba(0,104,143,0.4)] transition-all flex items-center justify-center gap-3 cursor-pointer whitespace-nowrap hover:scale-105 active:scale-95"
        >
          <span>Ir a Preguntas Frecuentes</span>
          <span class="material-symbols-outlined text-2xl">arrow_forward</span>
        </RouterLink>
      </div>
    </div>
  </section>
</template>
