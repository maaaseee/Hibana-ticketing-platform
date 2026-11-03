<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { useTicketsStore } from '../stores/tickets'
import { useToastStore } from '../stores/toast'

const router = useRouter()
const authStore = useAuthStore()
const ticketsStore = useTicketsStore()
const toastStore = useToastStore()

// Si no está logueado, redirigir a login
if (!authStore.isAuthenticated) {
  router.push({ name: 'login', query: { redirect: '/profile' } })
}

// 1. Datos personales
const fullName = ref(authStore.user?.name || '')
const phone = ref(authStore.user?.phone || '+54 11 4899-2310')
const isSavingProfile = ref(false)

function saveProfile() {
  if (!fullName.value.trim()) {
    toastStore.error('El nombre no puede estar vacío.')
    return
  }
  isSavingProfile.value = true
  setTimeout(() => {
    authStore.updateProfile({
      name: fullName.value.trim(),
      phone: phone.value.trim()
    })
    isSavingProfile.value = false
    toastStore.success('Datos personales actualizados correctamente.')
  }, 400)
}

// 2. Seguridad & Contraseña
const currentPassword = ref('')
const newPassword = ref('')
const confirmPassword = ref('')
const isChangingPassword = ref(false)

const passwordCriteria = computed(() => {
  const p = newPassword.value
  return {
    minLength: p.length >= 8,
    hasUpper: /[A-Z]/.test(p),
    hasNumber: /[0-9]/.test(p),
    hasSpecial: /[^A-Za-z0-9]/.test(p)
  }
})

const isNewPasswordValid = computed(() => {
  return (
    passwordCriteria.value.minLength &&
    passwordCriteria.value.hasUpper &&
    passwordCriteria.value.hasNumber &&
    passwordCriteria.value.hasSpecial
  )
})

function changePassword() {
  if (!currentPassword.value) {
    toastStore.error('Ingresa tu contraseña actual.')
    return
  }
  if (!isNewPasswordValid.value) {
    toastStore.error('La nueva contraseña no cumple con todos los requisitos.')
    return
  }
  if (newPassword.value !== confirmPassword.value) {
    toastStore.error('Las contraseñas no coinciden.')
    return
  }

  isChangingPassword.value = true
  setTimeout(() => {
    isChangingPassword.value = false
    currentPassword.value = ''
    newPassword.value = ''
    confirmPassword.value = ''
    toastStore.success('Contraseña actualizada con éxito.')
  }, 600)
}

// 3. Tarjetas guardadas
const savedCards = computed(() => authStore.user?.savedCards || [])

function removeCard(cardId: string) {
  authStore.removeSavedCard(cardId)
  toastStore.info('Tarjeta desvinculada de tu cuenta.')
}

// Stats
const activeTicketsCount = computed(() => {
  return ticketsStore.tickets.filter(t => t.status === 'ISSUED').length
})
</script>

<template>
  <div class="min-h-screen bg-[#070e1a] text-[#f1f5f9] py-10 px-4 sm:px-6 lg:px-8">
    <div class="max-w-4xl mx-auto space-y-8">
      <!-- Breadcrumb & Back -->
      <div class="flex items-center justify-between">
        <button
          type="button"
          @click="router.back()"
          class="inline-flex items-center gap-2 text-sm text-[#94a3b8] hover:text-[#38bdf8] transition-colors cursor-pointer"
        >
          <span class="material-symbols-outlined text-[18px]">arrow_back</span>
          <span>Volver</span>
        </button>
        <span class="font-mono text-xs text-[#64748b] tracking-wider uppercase">
          Configuración de cuenta • Hibana.tk
        </span>
      </div>

      <!-- Tarjeta Principal de Cabecera del Usuario -->
      <div class="p-6 sm:p-8 rounded-3xl bg-[#0c1524] border border-[#23354d] shadow-2xl relative overflow-hidden backdrop-blur-xl">
        <div class="absolute -right-16 -top-16 w-64 h-64 bg-[#00688f]/20 rounded-full blur-3xl pointer-events-none"></div>

        <div class="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-6 relative z-10">
          <div class="flex items-center gap-5">
            <div class="relative">
              <img
                :src="authStore.user?.avatarUrl || 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&w=120&q=80'"
                alt="Avatar"
                class="w-20 h-20 rounded-2xl object-cover ring-2 ring-[#38bdf8]/60 shadow-lg"
              />
              <span
                class="absolute -bottom-1 -right-1 w-5 h-5 rounded-full bg-emerald-500 ring-4 ring-[#0c1524] flex items-center justify-center text-[10px] text-white font-bold"
                title="Usuario Verificado"
              >
                ✓
              </span>
            </div>

            <div>
              <div class="flex items-center gap-3">
                <h1 class="font-display text-2xl sm:text-3xl font-extrabold text-white">
                  {{ authStore.user?.name || 'Usuario Hibana' }}
                </h1>
                <span class="px-2.5 py-0.5 rounded-full bg-emerald-950/70 border border-emerald-500/40 text-emerald-300 font-mono text-[11px] font-semibold">
                  DNI Verificado
                </span>
              </div>
              <p class="font-mono text-sm text-[#86cffb] mt-0.5">{{ authStore.user?.email }}</p>
              <p class="font-mono text-xs text-[#94a3b8] mt-1">
                Miembro oficial de Hibana Fans Club • ID: {{ authStore.user?.id || 'usr_demo' }}
              </p>
            </div>
          </div>

          <!-- Badges de Estado / Acceso Rápido -->
          <div class="flex items-center gap-3 w-full sm:w-auto">
            <button
              type="button"
              @click="router.push({ name: 'tickets' })"
              class="flex-1 sm:flex-initial px-4 py-3 rounded-2xl bg-[#142033] hover:bg-[#1a2b45] border border-[#23354d] transition-all flex items-center gap-3 text-left cursor-pointer group"
            >
              <div class="w-10 h-10 rounded-xl bg-[#00688f]/30 border border-[#38bdf8]/40 flex items-center justify-center text-[#38bdf8] group-hover:scale-105 transition-transform">
                <span class="material-symbols-outlined text-[20px]">confirmation_number</span>
              </div>
              <div>
                <span class="block font-display text-xs text-[#94a3b8]">Entradas Activas</span>
                <span class="block font-display font-black text-white text-base">
                  {{ activeTicketsCount || 3 }} pases
                </span>
              </div>
            </button>
          </div>
        </div>
      </div>

      <!-- Grid de 2 Columnas: Datos Personales y Seguridad -->
      <div class="grid grid-cols-1 lg:grid-cols-2 gap-8">
        <!-- 1. Datos Personales -->
        <div class="p-6 sm:p-7 rounded-3xl bg-[#0c1524] border border-[#23354d] shadow-xl flex flex-col justify-between">
          <div>
            <div class="flex items-center gap-3 pb-4 border-b border-[#23354d] mb-6">
              <div class="w-9 h-9 rounded-xl bg-[#00688f]/30 border border-[#38bdf8]/40 flex items-center justify-center text-[#38bdf8]">
                <span class="material-symbols-outlined text-[20px]">person</span>
              </div>
              <div>
                <h2 class="font-display font-bold text-lg text-white">Datos Personales</h2>
                <p class="font-sans text-xs text-[#94a3b8]">Información registrada para nominación de entradas.</p>
              </div>
            </div>

            <form @submit.prevent="saveProfile" class="space-y-4">
              <div>
                <label class="block font-mono text-xs uppercase tracking-wider text-[#94a3b8] mb-1.5">
                  Nombre Completo
                </label>
                <input
                  v-model="fullName"
                  type="text"
                  required
                  class="w-full px-4 py-3 rounded-xl bg-[#142033] border border-[#23354d] text-white focus:outline-none focus:border-[#38bdf8] focus:ring-1 focus:ring-[#38bdf8] transition-colors font-sans text-sm"
                  placeholder="Tu nombre completo"
                />
              </div>

              <div>
                <label class="block font-mono text-xs uppercase tracking-wider text-[#94a3b8] mb-1.5">
                  Correo Electrónico (Solo Lectura)
                </label>
                <input
                  :value="authStore.user?.email"
                  type="email"
                  disabled
                  class="w-full px-4 py-3 rounded-xl bg-[#08101c] border border-[#1b2b3f] text-[#64748b] font-mono text-sm cursor-not-allowed"
                />
              </div>

              <div class="grid grid-cols-1 sm:grid-cols-2 gap-4">
                <div>
                  <label class="block font-mono text-xs uppercase tracking-wider text-[#94a3b8] mb-1.5">
                    DNI Argentino (Inmutable)
                  </label>
                  <input
                    :value="authStore.user?.documentNumber || '38.452.190'"
                    type="text"
                    disabled
                    class="w-full px-4 py-3 rounded-xl bg-[#08101c] border border-[#1b2b3f] text-[#64748b] font-mono text-sm cursor-not-allowed"
                  />
                </div>

                <div>
                  <label class="block font-mono text-xs uppercase tracking-wider text-[#94a3b8] mb-1.5">
                    Teléfono Celular
                  </label>
                  <input
                    v-model="phone"
                    type="tel"
                    class="w-full px-4 py-3 rounded-xl bg-[#142033] border border-[#23354d] text-white focus:outline-none focus:border-[#38bdf8] focus:ring-1 focus:ring-[#38bdf8] transition-colors font-sans text-sm"
                    placeholder="+54 11 1234-5678"
                  />
                </div>
              </div>

              <div class="pt-4">
                <button
                  type="submit"
                  :disabled="isSavingProfile"
                  class="w-full py-3 px-4 rounded-xl bg-gradient-to-r from-[#00688f] to-[#0284c7] hover:from-[#0284c7] hover:to-[#38bdf8] text-white font-display font-bold text-sm tracking-wide transition-all shadow-lg shadow-[#00688f]/30 cursor-pointer disabled:opacity-50 active:scale-98"
                >
                  <span v-if="isSavingProfile">Guardando cambios...</span>
                  <span v-else>Guardar Información Personal</span>
                </button>
              </div>
            </form>
          </div>
        </div>

        <!-- 2. Seguridad & Contraseña -->
        <div class="p-6 sm:p-7 rounded-3xl bg-[#0c1524] border border-[#23354d] shadow-xl flex flex-col justify-between">
          <div>
            <div class="flex items-center gap-3 pb-4 border-b border-[#23354d] mb-6">
              <div class="w-9 h-9 rounded-xl bg-[#00688f]/30 border border-[#38bdf8]/40 flex items-center justify-center text-[#38bdf8]">
                <span class="material-symbols-outlined text-[20px]">lock_reset</span>
              </div>
              <div>
                <h2 class="font-display font-bold text-lg text-white">Seguridad & Contraseña</h2>
                <p class="font-sans text-xs text-[#94a3b8]">Modifica tu clave de acceso con validación estricta.</p>
              </div>
            </div>

            <form @submit.prevent="changePassword" class="space-y-4">
              <div>
                <label class="block font-mono text-xs uppercase tracking-wider text-[#94a3b8] mb-1.5">
                  Contraseña Actual
                </label>
                <input
                  v-model="currentPassword"
                  type="password"
                  placeholder="••••••••••••"
                  class="w-full px-4 py-3 rounded-xl bg-[#142033] border border-[#23354d] text-white focus:outline-none focus:border-[#38bdf8] focus:ring-1 focus:ring-[#38bdf8] transition-colors font-mono text-sm"
                />
              </div>

              <div>
                <label class="block font-mono text-xs uppercase tracking-wider text-[#94a3b8] mb-1.5">
                  Nueva Contraseña
                </label>
                <input
                  v-model="newPassword"
                  type="password"
                  placeholder="••••••••••••"
                  class="w-full px-4 py-3 rounded-xl bg-[#142033] border border-[#23354d] text-white focus:outline-none focus:border-[#38bdf8] focus:ring-1 focus:ring-[#38bdf8] transition-colors font-mono text-sm"
                />
              </div>

              <!-- Checklist dinámico de la contraseña -->
              <div class="p-3 rounded-xl bg-[#08101c] border border-[#1b2b3f] space-y-1.5">
                <p class="font-mono text-[10px] uppercase text-[#64748b] tracking-wider mb-1">
                  Requisitos de seguridad:
                </p>
                <div class="grid grid-cols-2 gap-1.5 text-xs font-mono">
                  <div
                    class="flex items-center gap-1.5"
                    :class="passwordCriteria.minLength ? 'text-emerald-400' : 'text-[#64748b]'"
                  >
                    <span class="material-symbols-outlined text-[14px]">
                      {{ passwordCriteria.minLength ? 'check_circle' : 'radio_button_unchecked' }}
                    </span>
                    <span>Mínimo 8 caracteres</span>
                  </div>
                  <div
                    class="flex items-center gap-1.5"
                    :class="passwordCriteria.hasUpper ? 'text-emerald-400' : 'text-[#64748b]'"
                  >
                    <span class="material-symbols-outlined text-[14px]">
                      {{ passwordCriteria.hasUpper ? 'check_circle' : 'radio_button_unchecked' }}
                    </span>
                    <span>Una mayúscula</span>
                  </div>
                  <div
                    class="flex items-center gap-1.5"
                    :class="passwordCriteria.hasNumber ? 'text-emerald-400' : 'text-[#64748b]'"
                  >
                    <span class="material-symbols-outlined text-[14px]">
                      {{ passwordCriteria.hasNumber ? 'check_circle' : 'radio_button_unchecked' }}
                    </span>
                    <span>Un número</span>
                  </div>
                  <div
                    class="flex items-center gap-1.5"
                    :class="passwordCriteria.hasSpecial ? 'text-emerald-400' : 'text-[#64748b]'"
                  >
                    <span class="material-symbols-outlined text-[14px]">
                      {{ passwordCriteria.hasSpecial ? 'check_circle' : 'radio_button_unchecked' }}
                    </span>
                    <span>Un símbolo especial</span>
                  </div>
                </div>
              </div>

              <div>
                <label class="block font-mono text-xs uppercase tracking-wider text-[#94a3b8] mb-1.5">
                  Confirmar Nueva Contraseña
                </label>
                <input
                  v-model="confirmPassword"
                  type="password"
                  placeholder="••••••••••••"
                  class="w-full px-4 py-3 rounded-xl bg-[#142033] border border-[#23354d] text-white focus:outline-none focus:border-[#38bdf8] focus:ring-1 focus:ring-[#38bdf8] transition-colors font-mono text-sm"
                />
              </div>

              <div class="pt-4">
                <button
                  type="submit"
                  :disabled="isChangingPassword || !isNewPasswordValid || newPassword !== confirmPassword"
                  class="w-full py-3 px-4 rounded-xl bg-[#142033] hover:bg-[#1a2b45] border border-[#38bdf8]/50 text-[#86cffb] hover:text-white font-display font-bold text-sm tracking-wide transition-all shadow-md cursor-pointer disabled:opacity-50 disabled:cursor-not-allowed"
                >
                  <span v-if="isChangingPassword">Actualizando clave...</span>
                  <span v-else>Actualizar Contraseña</span>
                </button>
              </div>
            </form>
          </div>
        </div>
      </div>

      <!-- 3. Métodos de Pago Guardados & Facturas -->
      <div class="p-6 sm:p-7 rounded-3xl bg-[#0c1524] border border-[#23354d] shadow-xl">
        <div class="flex items-center justify-between pb-4 border-b border-[#23354d] mb-6">
          <div class="flex items-center gap-3">
            <div class="w-9 h-9 rounded-xl bg-[#00688f]/30 border border-[#38bdf8]/40 flex items-center justify-center text-[#38bdf8]">
              <span class="material-symbols-outlined text-[20px]">credit_card</span>
            </div>
            <div>
              <h2 class="font-display font-bold text-lg text-white">Métodos de Pago & Facturación</h2>
              <p class="font-sans text-xs text-[#94a3b8]">Tarjetas guardadas para reservas rápidas y descarga de comprobantes.</p>
            </div>
          </div>

          <button
            type="button"
            @click="router.push({ name: 'preview-invoice' })"
            class="hidden sm:inline-flex items-center gap-2 px-3.5 py-2 rounded-xl bg-[#142033] hover:bg-[#1a2b45] border border-[#23354d] text-xs font-mono text-[#38bdf8] transition-colors cursor-pointer"
          >
            <span class="material-symbols-outlined text-[16px]">receipt_long</span>
            <span>Ver Factura Oficial AFIP</span>
          </button>
        </div>

        <div v-if="savedCards.length > 0" class="grid grid-cols-1 sm:grid-cols-2 gap-4">
          <div
            v-for="card in savedCards"
            :key="card.id"
            class="p-5 rounded-2xl bg-gradient-to-br from-[#142033] to-[#0a1220] border border-[#23354d] shadow-md flex items-center justify-between relative group hover:border-[#38bdf8]/40 transition-colors"
          >
            <div class="flex items-center gap-4">
              <div class="w-12 h-8 rounded-lg bg-emerald-950/40 border border-emerald-500/40 flex items-center justify-center text-emerald-300 font-mono text-xs font-black">
                {{ card.brand }}
              </div>
              <div>
                <p class="font-mono text-sm text-white font-bold tracking-wider">
                  •••• •••• •••• {{ card.last4 }}
                </p>
                <p class="font-mono text-xs text-[#94a3b8]">
                  Vence: {{ card.expMonth }}/{{ card.expYear }}
                </p>
              </div>
            </div>

            <button
              type="button"
              @click="removeCard(card.id)"
              class="w-8 h-8 rounded-full bg-[#1b2b3f]/60 hover:bg-rose-950/70 border border-transparent hover:border-rose-500/50 text-[#94a3b8] hover:text-rose-300 flex items-center justify-center transition-all cursor-pointer"
              title="Desvincular tarjeta"
            >
              <span class="material-symbols-outlined text-[16px]">delete</span>
            </button>
          </div>
        </div>

        <div
          v-else
          class="p-6 rounded-2xl bg-[#08101c] border border-dashed border-[#23354d] text-center"
        >
          <span class="material-symbols-outlined text-3xl text-[#64748b] mb-2">credit_card_off</span>
          <p class="font-mono text-sm text-[#94a3b8]">No tienes métodos de pago guardados.</p>
          <p class="font-sans text-xs text-[#64748b] mt-1">
            Puedes guardar tu tarjeta durante el checkout para compras en un clic.
          </p>
        </div>
      </div>

      <!-- Zona de Sesión & Salida -->
      <div class="p-6 rounded-3xl bg-[#0c1524] border border-[#23354d] flex flex-col sm:flex-row items-center justify-between gap-4">
        <div class="flex items-center gap-3">
          <span class="material-symbols-outlined text-2xl text-[#38bdf8]">shield</span>
          <div>
            <h3 class="font-display font-bold text-sm text-white">Sesión Protegida y Cifrada</h3>
            <p class="font-sans text-xs text-[#94a3b8]">Tus datos viajan encriptados bajo protocolo TLS 1.3.</p>
          </div>
        </div>

        <button
          type="button"
          @click="authStore.logout(router)"
          class="w-full sm:w-auto px-5 py-2.5 rounded-xl bg-rose-950/40 hover:bg-rose-950/70 border border-rose-500/50 text-rose-300 font-mono text-xs font-bold transition-all cursor-pointer shadow-sm active:scale-98 flex items-center justify-center gap-2"
        >
          <span class="material-symbols-outlined text-[16px]">logout</span>
          <span>Cerrar Sesión</span>
        </button>
      </div>
    </div>
  </div>
</template>
