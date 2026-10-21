<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useCatalogStore } from '../stores/catalog'
import { useQueueStore } from '../stores/queue'
import {
  SelectRoot,
  SelectTrigger,
  SelectValue,
  SelectIcon,
  SelectPortal,
  SelectContent,
  SelectViewport,
  SelectItem,
  SelectItemText,
  SelectItemIndicator,
  DialogRoot,
  DialogTrigger,
  DialogPortal,
  DialogOverlay,
  DialogContent,
  DialogClose,
  TooltipProvider,
  TooltipRoot,
  TooltipTrigger,
  TooltipContent
} from 'radix-vue'

const route = useRoute()
const router = useRouter()
const catalog = useCatalogStore()
const queue = useQueueStore()

const eventId = computed(() => (route.params.id as string) || 'symphony-of-lights')
const event = computed(() => catalog.getEventById(eventId.value) || catalog.events[0])

const selectedDate = ref(event.value?.availableDates?.[0] || 'Friday, Oct 15 - 20:00')
const selectedTicketTypeIndex = ref(0)

const availableTicketTypes = computed(() => {
  if (event.value?.ticketTypes && event.value.ticketTypes.length > 0) {
    return event.value.ticketTypes
  }
  return [
    { name: `General Admission ($${event.value?.price || 45}.00)`, price: event.value?.price || 45 },
    { name: `VIP Pit ($${(event.value?.price || 45) + 40}.00)`, price: (event.value?.price || 45) + 40 },
    { name: `Balcony Reserved ($${(event.value?.price || 45) + 15}.00)`, price: (event.value?.price || 45) + 15 }
  ]
})

const selectedTicketPrice = computed(() => {
  const current = availableTicketTypes.value[selectedTicketTypeIndex.value]
  return current ? current.price : (event.value?.price || 45)
})

function handleProceedToPurchase() {
  const ev = event.value
  if (!ev) return

  if (ev.status === 'QUEUE_ACTIVE') {
    queue.enterQueue(ev.id)
    router.push({ name: 'queue', params: { id: ev.id } })
  } else {
    router.push({ name: 'seat-selection', params: { id: ev.id } })
  }
}
</script>

<template>
  <TooltipProvider>
    <div v-if="event" class="flex flex-col w-full min-h-[calc(100vh-64px)] bg-[#0c141f] text-[#f1f5f9]">
      <!-- Panoramic Cover Banner matching wireframe lines 141-145 -->
      <div class="w-full h-[46vh] min-h-[360px] max-h-[500px] relative bg-[#0c141f]">
        <div
          class="absolute inset-0 bg-cover bg-center"
          :style="{ backgroundImage: `url(${event.imageUrl})` }"
        ></div>
        <div class="absolute inset-0 bg-gradient-to-t from-[#0c141f] via-[#0c141f]/50 to-transparent"></div>
      </div>

      <!-- Content Container matching wireframe lines 146-253 (Without user sidebar as requested) -->
      <div class="max-w-[80vw] w-full mx-auto px-4 sm:px-6 lg:px-8 -mt-36 md:-mt-44 relative z-10 grid grid-cols-1 lg:grid-cols-12 gap-8 pb-16">
        <!-- Central Info Column (8 cols) -->
        <div class="lg:col-span-8 flex flex-col gap-6">
          <div>
            <span class="inline-block px-3 py-1 bg-[#141e2e] text-[#86cffb] border border-[#23354d] font-mono text-xs font-semibold rounded-md mb-3 tracking-wide uppercase">
              {{ event.category || 'Live Concert' }}
            </span>
            <h1 class="font-display text-3xl sm:text-4xl lg:text-5xl font-extrabold text-[#f1f5f9] tracking-tight mb-3">
              {{ event.title }}
            </h1>
            <p class="font-body text-base sm:text-lg text-[#94a3b8] max-w-2xl leading-relaxed">
              {{ event.subtitle }}
            </p>
          </div>

          <!-- Metadata Bento Grid (2 cards) matching wireframe lines 162-200 -->
          <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
            <!-- Venue Card with Radix Dialog for Zoomable Map -->
            <div class="bg-[#141e2e] rounded-xl p-5 border border-[#23354d] flex flex-col justify-between">
              <div class="flex items-center gap-3.5 mb-3">
                <div class="w-10 h-10 rounded-lg bg-[#1a293e] border border-[#23354d] flex items-center justify-center text-[#86cffb] shrink-0">
                  <span class="material-symbols-outlined text-[20px]">location_on</span>
                </div>
                <div>
                  <h3 class="font-display text-lg font-bold text-[#f1f5f9]">{{ event.venue }}</h3>
                  <p class="font-mono text-xs text-[#94a3b8]">{{ event.location }}</p>
                </div>
              </div>

              <!-- Seat Map Preview with Accessible Radix Dialog Modal -->
              <DialogRoot>
                <DialogTrigger as-child>
                  <button
                    class="w-full h-36 rounded-lg overflow-hidden border border-[#23354d] mt-2 bg-[#101825] relative flex items-center justify-center group hover:border-[#86cffb]/60 transition-all cursor-pointer"
                    aria-label="Ampliar plano del recinto"
                  >
                    <svg viewBox="0 0 400 180" class="w-full h-full p-2 text-primary opacity-80 group-hover:opacity-100 transition-opacity" xmlns="http://www.w3.org/2000/svg">
                      <rect x="140" y="10" width="120" height="24" rx="4" fill="#00688f" stroke="#86cffb" stroke-width="1.5" />
                      <text x="200" y="26" text-anchor="middle" fill="#ffffff" font-size="10" font-family="Space Grotesk" font-weight="bold">ESCENARIO PRINCIPAL</text>

                      <rect x="120" y="44" width="160" height="40" rx="6" fill="#162438" stroke="#4fdbc8" stroke-width="1.5" />
                      <text x="200" y="68" text-anchor="middle" fill="#4fdbc8" font-size="11" font-family="Space Grotesk">VIP FRONT STAGE</text>

                      <path d="M 60 95 Q 200 80 340 95 L 360 160 Q 200 140 40 160 Z" fill="#141e2e" stroke="#23354d" stroke-width="1.5" />
                      <text x="200" y="130" text-anchor="middle" fill="#94a3b8" font-size="10" font-family="Space Grotesk">SECTORES GENERAL & PLATEAS</text>
                    </svg>
                    <div class="absolute bottom-2 right-2 px-2 py-0.5 rounded bg-[#0b131e]/90 border border-[#23354d] text-[10px] font-mono text-[#86cffb] group-hover:bg-[#00688f] group-hover:text-white transition-colors flex items-center gap-1 shadow-sm">
                      <span class="material-symbols-outlined text-[13px]">zoom_in</span>
                      <span>Ampliar Plano</span>
                    </div>
                  </button>
                </DialogTrigger>

                <DialogPortal>
                  <DialogOverlay class="fixed inset-0 bg-black/80 backdrop-blur-sm z-50 animate-fadeIn" />
                  <DialogContent class="fixed top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 bg-[#141e2e] border border-[#23354d] rounded-2xl p-6 shadow-2xl z-50 w-full max-w-2xl max-h-[90vh] overflow-y-auto outline-none">
                    <div class="flex items-center justify-between pb-4 border-b border-[#23354d]">
                      <div class="flex items-center gap-2">
                        <span class="material-symbols-outlined text-[#86cffb]">map</span>
                        <h3 class="font-display text-lg font-bold text-[#f1f5f9]">Plano Oficial del Recinto: {{ event.venue }}</h3>
                      </div>
                      <DialogClose class="w-8 h-8 rounded-lg bg-[#1a293e] text-[#94a3b8] hover:text-white flex items-center justify-center transition-colors cursor-pointer">
                        <span class="material-symbols-outlined text-sm">close</span>
                      </DialogClose>
                    </div>

                    <div class="py-6 flex flex-col items-center justify-center">
                      <svg viewBox="0 0 500 240" class="w-full max-w-lg" xmlns="http://www.w3.org/2000/svg">
                        <rect x="175" y="10" width="150" height="32" rx="6" fill="#00688f" stroke="#86cffb" stroke-width="2" />
                        <text x="250" y="31" text-anchor="middle" fill="#ffffff" font-size="12" font-family="Space Grotesk" font-weight="bold">ESCENARIO PRINCIPAL</text>

                        <rect x="150" y="55" width="200" height="50" rx="8" fill="#162438" stroke="#4fdbc8" stroke-width="1.5" />
                        <text x="250" y="85" text-anchor="middle" fill="#4fdbc8" font-size="13" font-family="Space Grotesk">VIP FRONT STAGE ($85)</text>

                        <path d="M 50 120 Q 250 100 450 120 L 480 210 Q 250 180 20 210 Z" fill="#101825" stroke="#23354d" stroke-width="2" />
                        <text x="250" y="165" text-anchor="middle" fill="#cbd5e1" font-size="12" font-family="Space Grotesk">SECTOR GENERAL & PLATEAS ($45)</text>
                      </svg>
                      <p class="font-mono text-xs text-[#94a3b8] mt-4 text-center">
                        Distribución de sectores y accesos controlados por torniquetes de alta velocidad.
                      </p>
                    </div>
                  </DialogContent>
                </DialogPortal>
              </DialogRoot>
            </div>

            <!-- Schedule & Lineup Card -->
            <div class="bg-[#141e2e] rounded-xl p-5 border border-[#23354d] flex flex-col justify-between">
              <div>
                <div class="flex items-center gap-3.5 mb-4">
                  <div class="w-10 h-10 rounded-lg bg-[#1a293e] border border-[#23354d] flex items-center justify-center text-[#86cffb] shrink-0">
                    <span class="material-symbols-outlined text-[20px]">calendar_month</span>
                  </div>
                  <div>
                    <h3 class="font-display text-lg font-bold text-[#f1f5f9]">{{ event.date }}</h3>
                    <p class="font-mono text-xs text-[#94a3b8]">Doors open: {{ event.doorsOpen || '19:00 HRS' }}</p>
                  </div>
                </div>

                <div class="pt-3 border-t border-[#23354d]/60">
                  <p class="font-mono text-xs text-[#86cffb] uppercase tracking-wider font-semibold mb-1">Lineup</p>
                  <p class="font-body text-sm text-[#f1f5f9] font-medium leading-relaxed">
                    {{ event.lineup || 'The Lumineers, Of Monsters and Men' }}
                  </p>
                </div>
              </div>

              <TooltipRoot>
                <TooltipTrigger as-child>
                  <div class="pt-4 flex items-center gap-2 text-xs text-[#94a3b8] font-mono border-t border-[#23354d]/40 mt-3 cursor-help">
                    <span class="material-symbols-outlined text-sm text-[#86cffb]">verified</span>
                    <span>Evento oficial confirmado</span>
                  </div>
                </TooltipTrigger>
                <TooltipContent class="bg-[#101825] border border-[#23354d] text-xs font-mono text-[#cbd5e1] p-2 rounded-lg shadow-xl z-50">
                  Verificado criptográficamente por la productora y recinto
                </TooltipContent>
              </TooltipRoot>
            </div>
          </div>

          <!-- About Description Card matching wireframe lines 201-210 -->
          <div class="bg-[#141e2e] rounded-xl p-6 border border-[#23354d]">
            <h2 class="font-display text-xl font-bold text-[#f1f5f9] mb-4">About the Event</h2>
            <p class="font-body text-sm sm:text-base text-[#94a3b8] mb-4 leading-relaxed">
              {{ event.description || "The Symphony of Lights returns to the Movistar Arena for an unprecedented audiovisual spectacle. This year's production features state-of-the-art volumetric lighting and a 360-degree sound system, ensuring an immersive experience from every seat." }}
            </p>
            <p class="font-body text-sm sm:text-base text-[#94a3b8] leading-relaxed">
              Strict security measures will be in place. No large bags or professional recording equipment are permitted inside the venue. Please arrive early to ensure a smooth entry process. Entradas validadas mediante códigos QR criptográficos dinámicos para garantizar cero duplicaciones.
            </p>
          </div>
        </div>

        <!-- Ticket Purchase Sidebar (4 cols) matching wireframe lines 212-252 -->
        <div class="lg:col-span-4">
          <div class="sticky top-20 bg-[#141e2e] rounded-xl p-6 border border-[#23354d] shadow-xl flex flex-col">
            <!-- Price Header -->
            <div class="mb-6 pb-5 border-b border-[#23354d]">
              <p class="font-mono text-xs text-[#94a3b8] uppercase tracking-wider mb-1 font-medium">Tickets from</p>
              <div class="flex items-baseline gap-2">
                <span class="font-display text-4xl font-extrabold text-[#f1f5f9]">${{ selectedTicketPrice }}.00</span>
                <span class="font-mono text-xs text-[#86cffb] font-semibold">USD</span>
              </div>
            </div>

            <!-- Radix UI Select Form -->
            <div class="space-y-4 mb-6">
              <!-- Select Date via Radix Select -->
              <div>
                <label class="font-mono text-xs text-[#cbd5e1] mb-2 block uppercase tracking-wide font-semibold">
                  Select Date
                </label>
                <SelectRoot v-model="selectedDate">
                  <SelectTrigger
                    class="w-full bg-[#101825] border border-[#23354d] rounded-lg p-3 font-body text-sm text-[#f1f5f9] flex items-center justify-between focus:outline-none focus:border-[#86cffb] focus:ring-1 focus:ring-[#86cffb] transition-colors cursor-pointer select-none"
                    aria-label="Select Date"
                  >
                    <SelectValue :placeholder="selectedDate" />
                    <SelectIcon>
                      <span class="material-symbols-outlined text-[#94a3b8] text-xl">expand_more</span>
                    </SelectIcon>
                  </SelectTrigger>

                  <SelectPortal>
                    <SelectContent
                      class="bg-[#141e2e] border border-[#23354d] rounded-xl shadow-2xl p-1 z-50 min-w-[280px] max-h-60 overflow-y-auto"
                      position="popper"
                      :side-offset="4"
                    >
                      <SelectViewport class="p-1">
                        <SelectItem
                          v-for="d in (event.availableDates || ['Friday, Oct 15 - 20:00', 'Saturday, Oct 16 - 20:00'])"
                          :key="d"
                          :value="d"
                          class="relative flex items-center justify-between px-3 py-2 text-sm text-[#f1f5f9] rounded-lg cursor-pointer hover:bg-[#1a293e] hover:text-[#86cffb] focus:bg-[#1a293e] focus:text-[#86cffb] outline-none select-none transition-colors"
                        >
                          <SelectItemText>{{ d }}</SelectItemText>
                          <SelectItemIndicator class="text-[#86cffb]">
                            <span class="material-symbols-outlined text-sm">check</span>
                          </SelectItemIndicator>
                        </SelectItem>
                      </SelectViewport>
                    </SelectContent>
                  </SelectPortal>
                </SelectRoot>
              </div>

              <!-- Select Ticket Type via Radix Select -->
              <div>
                <label class="font-mono text-xs text-[#cbd5e1] mb-2 block uppercase tracking-wide font-semibold">
                  Ticket Type
                </label>
                <SelectRoot
                  :model-value="String(selectedTicketTypeIndex)"
                  @update:model-value="(val) => selectedTicketTypeIndex = Number(val)"
                >
                  <SelectTrigger
                    class="w-full bg-[#101825] border border-[#23354d] rounded-lg p-3 font-body text-sm text-[#f1f5f9] flex items-center justify-between focus:outline-none focus:border-[#86cffb] focus:ring-1 focus:ring-[#86cffb] transition-colors cursor-pointer select-none"
                    aria-label="Ticket Type"
                  >
                    <SelectValue>
                      {{ availableTicketTypes[selectedTicketTypeIndex]?.name }}
                    </SelectValue>
                    <SelectIcon>
                      <span class="material-symbols-outlined text-[#94a3b8] text-xl">expand_more</span>
                    </SelectIcon>
                  </SelectTrigger>

                  <SelectPortal>
                    <SelectContent
                      class="bg-[#141e2e] border border-[#23354d] rounded-xl shadow-2xl p-1 z-50 min-w-[280px] max-h-60 overflow-y-auto"
                      position="popper"
                      :side-offset="4"
                    >
                      <SelectViewport class="p-1">
                        <SelectItem
                          v-for="(type, idx) in availableTicketTypes"
                          :key="type.name"
                          :value="String(idx)"
                          class="relative flex items-center justify-between px-3 py-2 text-sm text-[#f1f5f9] rounded-lg cursor-pointer hover:bg-[#1a293e] hover:text-[#86cffb] focus:bg-[#1a293e] focus:text-[#86cffb] outline-none select-none transition-colors"
                        >
                          <SelectItemText>{{ type.name }}</SelectItemText>
                          <SelectItemIndicator class="text-[#86cffb]">
                            <span class="material-symbols-outlined text-sm">check</span>
                          </SelectItemIndicator>
                        </SelectItem>
                      </SelectViewport>
                    </SelectContent>
                  </SelectPortal>
                </SelectRoot>
              </div>
            </div>

            <!-- Solid CTA Button -->
            <button
              @click="handleProceedToPurchase"
              class="w-full bg-[#00688f] hover:bg-[#005a7d] text-white font-display text-base font-bold py-3.5 px-4 rounded-lg border border-[#00688f] transition-all flex items-center justify-center gap-2 cursor-pointer shadow-md hover:scale-[1.01] active:scale-[0.99]"
            >
              <span>{{ event.status === 'QUEUE_ACTIVE' ? 'Ingresar a Sala de Espera' : 'Seleccionar Asientos' }}</span>
              <span class="material-symbols-outlined text-[20px]">
                {{ event.status === 'QUEUE_ACTIVE' ? 'hourglass_top' : 'event_seat' }}
              </span>
            </button>

            <!-- Disclaimer / Microcopy -->
            <p class="text-center font-mono text-xs text-[#94a3b8] mt-3">
              Precios incluyen todos los cargos e impuestos.
            </p>

            <!-- High-concurrency assurance badge with Radix Tooltip -->
            <div class="mt-4 pt-4 border-t border-[#23354d]/50 flex flex-col gap-1.5 text-[11px] font-mono text-[#94a3b8]">
              <TooltipRoot>
                <TooltipTrigger as-child>
                  <div class="flex items-center gap-1.5 cursor-help hover:text-[#cbd5e1] transition-colors">
                    <span class="material-symbols-outlined text-[14px] text-[#4fdbc8]">lock_clock</span>
                    <span>Bloqueo Atómico Redlock 10m TTL</span>
                  </div>
                </TooltipTrigger>
                <TooltipContent class="bg-[#101825] border border-[#23354d] text-xs font-mono text-[#cbd5e1] p-2 rounded-lg shadow-xl z-50">
                  Reserva atómica distribuida en Redis durante el checkout sin riesgo de colisión
                </TooltipContent>
              </TooltipRoot>

              <TooltipRoot>
                <TooltipTrigger as-child>
                  <div class="flex items-center gap-1.5 cursor-help hover:text-[#cbd5e1] transition-colors">
                    <span class="material-symbols-outlined text-[14px] text-[#86cffb]">verified_user</span>
                    <span>Garantía de Asiento Sin Sobreventa</span>
                  </div>
                </TooltipTrigger>
                <TooltipContent class="bg-[#101825] border border-[#23354d] text-xs font-mono text-[#cbd5e1] p-2 rounded-lg shadow-xl z-50">
                  Verificado por el outbox transaccional y orquestación Saga
                </TooltipContent>
              </TooltipRoot>
            </div>
          </div>
        </div>
      </div>
    </div>
  </TooltipProvider>
</template>
