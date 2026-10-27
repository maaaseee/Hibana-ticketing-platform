<script setup lang="ts">
import {
  DialogRoot,
  DialogPortal,
  DialogOverlay,
  DialogContent,
  DialogTitle,
  DialogDescription,
  DialogClose
} from 'radix-vue'
import type { VenueInfo } from '@/types'

export interface VenueWithImage extends VenueInfo {
  heroImage: string
}

defineProps<{
  venue: VenueWithImage | null
}>()

const isOpen = defineModel<boolean>('open', { required: true })
</script>

<template>
  <DialogRoot v-model:open="isOpen">
    <DialogPortal>
      <DialogOverlay class="fixed inset-0 bg-black/80 backdrop-blur-sm z-50 transition-opacity" />
      <DialogContent
        v-if="venue"
        class="fixed left-1/2 top-1/2 -translate-x-1/2 -translate-y-1/2 w-[92vw] max-w-xl max-h-[85vh] overflow-y-auto bg-surface-card border border-border-subtle rounded-2xl p-6 sm:p-8 text-high-emphasis z-50 shadow-2xl focus:outline-none"
      >
        <!-- Header con Imagen del Recinto -->
        <div class="relative -mx-6 -mt-6 sm:-mx-8 sm:-mt-8 mb-6 h-48 overflow-hidden rounded-t-2xl">
          <img
            :src="venue.heroImage"
            :alt="venue.name"
            class="w-full h-full object-cover"
          />
          <div class="absolute inset-0 bg-linear-to-t from-surface-card via-surface-card/60 to-transparent"></div>

          <!-- Botón Cerrar (Radix DialogClose) -->
          <DialogClose
            class="absolute top-4 right-4 w-9 h-9 rounded-full bg-surface-card/80 hover:bg-surface-interactive border border-border-subtle text-high-emphasis flex items-center justify-center transition-all cursor-pointer backdrop-blur-sm focus:outline-none"
            aria-label="Cerrar modal"
          >
            <span class="material-symbols-outlined text-xl">close</span>
          </DialogClose>

          <div class="absolute bottom-4 left-6 sm:left-8">
            <span class="inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded bg-surface/90 border border-border-subtle text-secondary font-mono text-xs font-bold uppercase tracking-wider backdrop-blur-sm">
              <span class="material-symbols-outlined text-[14px]">stadium</span>
              <span>{{ venue.type.replace('_', ' ') }}</span>
            </span>
          </div>
        </div>

        <!-- Título y Subtítulo -->
        <DialogTitle class="font-display text-2xl sm:text-3xl font-extrabold text-high-emphasis mb-1">
          {{ venue.name }}
        </DialogTitle>
        <DialogDescription class="font-mono text-xs text-muted-light mb-6 flex flex-wrap items-center gap-2">
          <span>{{ venue.city }}, {{ venue.country }}</span>
          <span>•</span>
          <span class="text-primary font-semibold">Capacidad: {{ venue.capacity.toLocaleString('es-AR') }} espectadores</span>
        </DialogDescription>

        <div class="flex flex-col gap-5 text-sm">
          <!-- 1. DÓNDE QUEDA CON GOOGLE MAPS EMBED -->
          <div class="p-4 rounded-xl bg-surface-container-deep border border-border-subtle flex flex-col gap-3">
            <div class="flex items-center justify-between">
              <div class="flex items-center gap-2 text-primary font-mono text-xs uppercase font-bold tracking-wider">
                <span class="material-symbols-outlined text-[18px]">location_on</span>
                <span>¿Dónde queda?</span>
              </div>
              <a
                :href="venue.transit?.googleMapsUrl || `https://maps.google.com/?q=${encodeURIComponent(venue.address)}`"
                target="_blank"
                rel="noopener noreferrer"
                class="inline-flex items-center gap-1 text-[11px] font-mono font-semibold text-secondary hover:text-white transition-colors"
              >
                <span>Abrir en Google Maps</span>
                <span class="material-symbols-outlined text-[14px]">open_in_new</span>
              </a>
            </div>
            <p class="font-body text-base font-semibold text-high-emphasis">
              {{ venue.address }}
            </p>

            <!-- Iframe interactivo de Google Maps sin clave requerida -->
            <div class="relative w-full h-52 sm:h-60 rounded-xl overflow-hidden border border-border-subtle bg-surface">
              <iframe
                title="Ubicación en Google Maps"
                width="100%"
                height="100%"
                style="border:0;"
                loading="lazy"
                allowfullscreen
                referrerpolicy="no-referrer-when-downgrade"
                :src="`https://maps.google.com/maps?q=${encodeURIComponent(venue.transit?.embedQuery || venue.address)}&t=&z=15&ie=UTF8&iwloc=&output=embed`"
              ></iframe>
            </div>
          </div>

          <!-- 2. TRANSPORTE PÚBLICO: COLECTIVOS, TRENES Y SUBTE -->
          <div
            v-if="venue.transit"
            class="p-4 rounded-xl bg-surface-container-deep border border-border-subtle flex flex-col gap-3"
          >
            <div class="flex items-center gap-2 text-primary font-mono text-xs uppercase font-bold tracking-wider">
              <span class="material-symbols-outlined text-[18px]">directions_bus</span>
              <span>Transporte Público Cercano</span>
            </div>

            <!-- Colectivos / Buses -->
            <div v-if="venue.transit.busLines && venue.transit.busLines.length > 0" class="flex flex-col gap-1.5">
              <span class="font-mono text-xs text-medium-emphasis flex items-center gap-1.5 font-semibold">
                <span class="material-symbols-outlined text-[15px] text-secondary">directions_bus</span>
                <span>Líneas de Colectivo:</span>
              </span>
              <div class="flex flex-wrap gap-1.5 pt-0.5">
                <span
                  v-for="bus in venue.transit.busLines"
                  :key="bus"
                  class="px-2.5 py-0.5 rounded-md bg-surface-card border border-border-subtle text-high-emphasis font-mono text-xs font-bold shadow-sm"
                >
                  {{ bus }}
                </span>
              </div>
            </div>

            <!-- Trenes -->
            <div v-if="venue.transit.trains && venue.transit.trains.length > 0" class="flex flex-col gap-1.5 pt-2 border-t border-border-subtle/50">
              <span class="font-mono text-xs text-medium-emphasis flex items-center gap-1.5 font-semibold">
                <span class="material-symbols-outlined text-[15px] text-secondary">train</span>
                <span>Líneas de Tren:</span>
              </span>
              <ul class="space-y-1">
                <li
                  v-for="(train, tIdx) in venue.transit.trains"
                  :key="tIdx"
                  class="font-body text-xs text-high-emphasis flex items-start gap-1.5"
                >
                  <span class="material-symbols-outlined text-[14px] text-secondary mt-0.5 shrink-0">fiber_manual_record</span>
                  <span>{{ train }}</span>
                </li>
              </ul>
            </div>

            <!-- Subte / Metro -->
            <div v-if="venue.transit.subway && venue.transit.subway.length > 0" class="flex flex-col gap-1.5 pt-2 border-t border-border-subtle/50">
              <span class="font-mono text-xs text-medium-emphasis flex items-center gap-1.5 font-semibold">
                <span class="material-symbols-outlined text-[15px] text-secondary">subway</span>
                <span>Líneas de Subte / Conexiones:</span>
              </span>
              <ul class="space-y-1">
                <li
                  v-for="(sub, sIdx) in venue.transit.subway"
                  :key="sIdx"
                  class="font-body text-xs text-high-emphasis flex items-start gap-1.5"
                >
                  <span class="material-symbols-outlined text-[14px] text-secondary mt-0.5 shrink-0">fiber_manual_record</span>
                  <span>{{ sub }}</span>
                </li>
              </ul>
            </div>
          </div>

          <!-- 3. CÓMO LLEGAR Y ACCESOS PEATONALES / VEHICULARES -->
          <div
            v-if="venue.parking.accessRoutes && venue.parking.accessRoutes.length > 0"
            class="p-4 rounded-xl bg-surface-container-deep border border-border-subtle flex flex-col gap-2.5"
          >
            <div class="flex items-center gap-2 text-secondary font-mono text-xs uppercase font-bold tracking-wider">
              <span class="material-symbols-outlined text-[18px]">alt_route</span>
              <span>Accesos y Rutas Recomendadas</span>
            </div>
            <ul class="space-y-2 font-body text-xs text-medium-emphasis">
              <li
                v-for="(route, rIdx) in venue.parking.accessRoutes"
                :key="rIdx"
                class="flex items-start gap-2.5"
              >
                <span class="material-symbols-outlined text-[16px] text-secondary mt-0.5 shrink-0">turn_right</span>
                <span class="leading-relaxed">{{ route }}</span>
              </li>
            </ul>
          </div>

          <!-- 4. ESTACIONAMIENTO OFICIAL Y PRECIO -->
          <div class="p-4 rounded-xl bg-surface-container-deep border border-border-subtle flex flex-col gap-3">
            <div class="flex items-center justify-between">
              <div class="flex items-center gap-2 text-primary font-mono text-xs uppercase font-bold tracking-wider">
                <span class="material-symbols-outlined text-[18px]">local_parking</span>
                <span>Estacionamiento Oficial</span>
              </div>
              <span
                v-if="venue.parking.available"
                class="px-2.5 py-1 rounded bg-secondary/20 text-secondary border border-secondary/40 font-mono text-xs font-bold"
              >
                Disponible
              </span>
            </div>

            <h4 class="font-display text-sm font-bold text-high-emphasis">
              {{ venue.parking.title }}
            </h4>

            <p class="font-body text-xs text-muted-light leading-relaxed">
              {{ venue.parking.description }}
            </p>

            <!-- Tarifa destacada -->
            <div class="pt-2 border-t border-border-subtle/50 flex items-center justify-between bg-surface-card/60 -mx-4 -mb-4 p-3.5 rounded-b-xl">
              <span class="font-mono text-xs text-medium-emphasis">Tarifa estimada por vehículo:</span>
              <span class="font-mono text-base font-extrabold text-primary bg-primary-container/20 px-3 py-1 rounded-lg border border-primary/40">
                ${{ venue.parking.price?.toLocaleString('es-AR') }} ARS
              </span>
            </div>
          </div>
        </div>

        <!-- Botón de acción al pie -->
        <div class="mt-6 pt-4 border-t border-border-subtle flex justify-end">
          <DialogClose as-child>
            <button
              type="button"
              class="bg-surface-card hover:bg-surface-interactive text-high-emphasis font-display text-sm font-semibold px-5 py-2.5 rounded-xl border border-border-subtle transition-all cursor-pointer"
            >
              Cerrar
            </button>
          </DialogClose>
        </div>
      </DialogContent>
    </DialogPortal>
  </DialogRoot>
</template>
