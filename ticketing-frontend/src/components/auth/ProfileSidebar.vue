<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import {
  DialogRoot,
  DialogPortal,
  DialogOverlay,
  DialogContent,
  DialogClose,
  DialogTitle,
  DialogDescription
} from 'radix-vue'
import { useAuthStore } from '../../stores/auth'
import { useTicketsStore } from '../../stores/tickets'

const authStore = useAuthStore()
const ticketsStore = useTicketsStore()
const router = useRouter()
const logoUrl = '/logo.png'

const activeTicketsCount = computed(() => {
  return ticketsStore.tickets.filter(t => t.status === 'ISSUED').length
})

function navigateTo(routeName: string) {
  authStore.closeProfileSidebar()
  router.push({ name: routeName })
}

function handleLogout() {
  authStore.logout(router)
}
</script>

<template>
  <DialogRoot
    :open="authStore.isProfileSidebarOpen"
    @update:open="val => { if (!val) authStore.closeProfileSidebar() }"
  >
    <DialogPortal>
      <!-- Overlay difuminado de fondo con fade in/out -->
      <DialogOverlay class="profile-overlay fixed inset-0 bg-black/80 backdrop-blur-md z-50" />

      <!-- Sidebar deslizante desde la derecha -->
      <DialogContent
        class="profile-sidebar fixed inset-y-0 right-0 w-[88vw] max-w-sm bg-[#101825] border-l border-[#23354d] shadow-2xl z-50 flex flex-col justify-between p-6 focus:outline-none font-body text-[#f1f5f9]"
      >
        <!-- Bloque Superior: Header + Perfil de Usuario + Navegación -->
        <div class="flex flex-col gap-6">
          <!-- Header de la Sidebar con Cierre -->
          <div class="flex items-center justify-between pb-4 border-b border-[#23354d]">
            <div class="flex items-center gap-2.5">
              <img
                :src="logoUrl"
                alt="Hibana.tk Logo"
                class="w-7 h-7 object-contain drop-shadow-[0_0_8px_rgba(134,207,251,0.5)]"
              />
              <span class="font-display text-xl font-black text-primary tracking-tight">Mi Perfil</span>
            </div>

            <DialogClose
              class="w-8 h-8 rounded-full bg-[#141c27] hover:bg-[#1b2533] border border-[#23354d] text-[#94a3b8] hover:text-white flex items-center justify-center transition-colors cursor-pointer"
              aria-label="Cerrar perfil"
            >
              <span class="material-symbols-outlined text-[18px]">close</span>
            </DialogClose>
          </div>

          <DialogTitle class="sr-only">Panel de Usuario Hibana.tk</DialogTitle>
          <DialogDescription class="sr-only">Detalles de cuenta, entradas activas y acciones de perfil.</DialogDescription>

          <!-- Tarjeta de Identidad del Usuario Logueado -->
          <div
            v-if="authStore.user"
            class="p-4 rounded-2xl bg-[#141c27] border border-[#23354d] flex items-center gap-3.5 shadow-sm relative overflow-hidden"
          >
            <div class="relative shrink-0">
              <img
                alt="Foto de perfil"
                class="w-13 h-13 rounded-full object-cover ring-2 ring-[#00688f]/80"
                :src="authStore.user.avatarUrl || 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&w=100&q=80'"
              />
              <span
                class="absolute bottom-0 right-0 w-3.5 h-3.5 rounded-full bg-emerald-500 ring-2 ring-[#141c27]"
                title="Sesión activa verificada"
              ></span>
            </div>

            <div class="flex flex-col min-w-0 flex-1">
              <span class="font-display font-bold text-base text-white truncate">{{ authStore.user.name }}</span>
              <span class="font-mono text-xs text-[#86cffb] truncate">{{ authStore.user.email }}</span>
              <div class="flex items-center gap-2 mt-1.5">
                <span class="px-2 py-0.5 rounded-full bg-[#0c141f] border border-[#23354d] font-mono text-[10px] text-[#94a3b8]">
                  DNI: {{ authStore.user.documentNumber || '38.452.190' }}
                </span>
                <span class="px-2 py-0.5 rounded-full bg-emerald-950/60 border border-emerald-500/40 font-mono text-[10px] text-emerald-300 font-semibold">
                  Verificado
                </span>
              </div>
            </div>
          </div>

          <!-- Opciones de Navegación del Usuario -->
          <nav class="flex flex-col gap-2 font-display text-sm font-semibold">
            <!-- Opción Principal: Mis Entradas -->
            <button
              type="button"
              @click="navigateTo('tickets')"
              class="w-full flex items-center justify-between px-4 py-3.5 rounded-xl bg-[#141c27] hover:bg-[#1b2533] border border-[#23354d] text-white transition-all cursor-pointer group text-left"
            >
              <div class="flex items-center gap-3">
                <div class="w-9 h-9 rounded-lg bg-[#00688f]/30 border border-[#38bdf8]/40 flex items-center justify-center text-[#38bdf8] group-hover:scale-105 transition-transform">
                  <span class="material-symbols-outlined text-[20px]">confirmation_number</span>
                </div>
                <div>
                  <span class="block font-bold">Mis Entradas</span>
                  <span class="block font-mono text-[11px] text-[#94a3b8]">QR dinámico y pases</span>
                </div>
              </div>
              <span class="px-2.5 py-1 rounded-full bg-[#00688f]/40 border border-[#38bdf8]/50 text-[#86cffb] font-mono text-xs font-bold">
                {{ activeTicketsCount || 3 }} activas
              </span>
            </button>

            <!-- Explorar Cartelera de Eventos -->
            <button
              type="button"
              @click="navigateTo('events-catalog')"
              class="w-full flex items-center gap-3 px-4 py-3 rounded-xl hover:bg-[#141c27] text-[#cbd5e1] hover:text-white transition-colors cursor-pointer text-left"
            >
              <span class="material-symbols-outlined text-xl text-[#86cffb]">theater_comedy</span>
              <span>Cartelera de Shows</span>
            </button>

            <!-- Centro de Ayuda & FAQ -->
            <button
              type="button"
              @click="navigateTo('help')"
              class="w-full flex items-center gap-3 px-4 py-3 rounded-xl hover:bg-[#141c27] text-[#cbd5e1] hover:text-white transition-colors cursor-pointer text-left"
            >
              <span class="material-symbols-outlined text-xl text-[#86cffb]">help</span>
              <span>Preguntas Frecuentes</span>
            </button>

            <!-- Soporte en Vivo -->
            <button
              type="button"
              @click="navigateTo('support-chat')"
              class="w-full flex items-center gap-3 px-4 py-3 rounded-xl hover:bg-[#141c27] text-[#cbd5e1] hover:text-white transition-colors cursor-pointer text-left"
            >
              <span class="material-symbols-outlined text-xl text-[#86cffb]">support_agent</span>
              <span>Asistencia y Soporte en Vivo</span>
            </button>
          </nav>
        </div>

        <!-- Bloque Inferior: Seguridad de la Sesión + Botón Cerrar Sesión -->
        <div class="pt-4 border-t border-[#23354d] flex flex-col gap-3">
          <div class="px-3 py-2 rounded-xl bg-[#0c141f] border border-[#23354d] text-[11px] font-mono text-[#94a3b8] flex items-center gap-2">
            <span class="material-symbols-outlined text-[15px] text-[#38bdf8]">security</span>
            <span>Sesión protegida • Hibana.tk Auth</span>
          </div>

          <button
            type="button"
            @click="handleLogout"
            class="w-full flex items-center justify-center gap-2.5 px-4 py-3 rounded-xl bg-rose-950/40 hover:bg-rose-950/70 border border-rose-500/50 text-rose-300 hover:text-rose-200 font-mono text-xs font-bold transition-all cursor-pointer shadow-sm active:scale-98"
          >
            <span class="material-symbols-outlined text-[18px]">logout</span>
            <span>Cerrar Sesión</span>
          </button>
        </div>
      </DialogContent>
    </DialogPortal>
  </DialogRoot>
</template>

<style scoped>
.profile-overlay[data-state='open'] {
  animation: overlayFadeIn 260ms cubic-bezier(0.16, 1, 0.3, 1) forwards;
}
.profile-overlay[data-state='closed'] {
  animation: overlayFadeOut 200ms cubic-bezier(0.16, 1, 0.3, 1) forwards;
}

.profile-sidebar[data-state='open'] {
  animation: sidebarSlideIn 300ms cubic-bezier(0.16, 1, 0.3, 1) forwards;
}
.profile-sidebar[data-state='closed'] {
  animation: sidebarSlideOut 220ms cubic-bezier(0.16, 1, 0.3, 1) forwards;
}

@keyframes overlayFadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

@keyframes overlayFadeOut {
  from { opacity: 1; }
  to { opacity: 0; }
}

@keyframes sidebarSlideIn {
  from {
    transform: translateX(100%);
    opacity: 0.6;
  }
  to {
    transform: translateX(0);
    opacity: 1;
  }
}

@keyframes sidebarSlideOut {
  from {
    transform: translateX(0);
    opacity: 1;
  }
  to {
    transform: translateX(100%);
    opacity: 0;
  }
}
</style>
