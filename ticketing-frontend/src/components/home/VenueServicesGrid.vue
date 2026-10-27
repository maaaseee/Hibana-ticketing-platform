<script setup lang="ts">
export interface VenueServiceItem {
  id: string
  title: string
  subtitle: string
  description: string
  imageUrl: string
  icon: string
}

defineProps<{
  services: VenueServiceItem[]
}>()
</script>

<template>
  <!-- 4. SERVICIOS DEL RECINTO (Comida, Bebidas, Merchandising) -->
  <section class="w-full max-w-[80vw] mx-auto px-4 sm:px-6 lg:px-8">
    <div class="flex flex-col gap-6">
      <div class="flex flex-col gap-1 border-b border-border-subtle pb-4">
        <span class="font-mono text-xs text-primary uppercase tracking-wider font-semibold">
          Servicios en el Recinto
        </span>
        <h2 class="font-display text-2xl sm:text-3xl font-bold text-high-emphasis">
          ¿Qué encontrarás en el lugar?
        </h2>
        <p class="font-body text-sm text-muted-light">
          Información sobre los servicios disponibles para tu comodidad durante el espectáculo.
        </p>
      </div>

      <!-- 3 Tarjetas Simples de Servicio -->
      <div class="grid grid-cols-1 md:grid-cols-3 gap-6">
        <article
          v-for="service in services"
          :key="service.id"
          class="bg-surface-card border border-border-subtle rounded-2xl overflow-hidden flex flex-col hover:border-primary/40 transition-colors"
        >
          <div class="h-44 w-full overflow-hidden relative">
            <img
              :src="service.imageUrl"
              :alt="service.title"
              class="w-full h-full object-cover"
            />
            <div class="absolute inset-0 bg-linear-to-t from-surface-card via-transparent to-transparent"></div>
            <div class="absolute bottom-3 left-4 flex items-center gap-2">
              <div class="w-8 h-8 rounded-lg bg-surface-dim/90 border border-border-subtle flex items-center justify-center text-primary">
                <span class="material-symbols-outlined text-[18px]">{{ service.icon }}</span>
              </div>
              <span class="font-mono text-xs font-bold text-high-emphasis bg-surface/80 px-2 py-0.5 rounded border border-border-subtle">
                {{ service.subtitle }}
              </span>
            </div>
          </div>

          <div class="p-5 flex flex-col gap-2 flex-1">
            <h3 class="font-display text-xl font-bold text-high-emphasis">
              {{ service.title }}
            </h3>
            <p class="font-body text-sm text-muted-light leading-relaxed">
              {{ service.description }}
            </p>
          </div>
        </article>
      </div>
    </div>
  </section>
</template>
