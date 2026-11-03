<script setup lang="ts">
import { ref } from 'vue'
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
import { useAuthStore, MOCK_USER } from '../../stores/auth'

const authStore = useAuthStore()
const router = useRouter()
const logoUrl = '/logo.png'

// Login form state
const loginEmail = ref('carlos.m@hibana.tk')
const loginPassword = ref('password123')
const loginError = ref('')

// Register form state
const regName = ref('')
const regEmail = ref('')
const regDni = ref('')
const regPassword = ref('')
const regError = ref('')

function handleLoginSubmit() {
  loginError.value = ''
  if (!loginEmail.value.trim() || !loginEmail.value.includes('@')) {
    loginError.value = 'Por favor ingresa un correo electrónico válido.'
    return
  }
  if (!loginPassword.value.trim()) {
    loginError.value = 'Por favor ingresa tu contraseña.'
    return
  }

  authStore.login(loginEmail.value, loginPassword.value, router)
}

function handleQuickDemoLogin() {
  authStore.loginWithMock(router)
}

function handleRegisterSubmit() {
  regError.value = ''
  if (!regName.value.trim()) {
    regError.value = 'Por favor ingresa tu nombre y apellido.'
    return
  }
  if (!regEmail.value.trim() || !regEmail.value.includes('@')) {
    regError.value = 'Por favor ingresa un correo electrónico válido.'
    return
  }
  if (!regPassword.value || regPassword.value.length < 6) {
    regError.value = 'La contraseña debe contener al menos 6 caracteres.'
    return
  }

  authStore.register(
    {
      name: regName.value,
      email: regEmail.value,
      documentNumber: regDni.value,
      password: regPassword.value
    },
    router
  )
}
</script>

<template>
  <DialogRoot
    :open="authStore.isAuthModalOpen"
    @update:open="val => { if (!val) authStore.closeAuthModal() }"
  >
    <DialogPortal>
      <DialogOverlay class="fixed inset-0 bg-black/80 backdrop-blur-md z-50 animate-in fade-in-0 duration-200" />
      <DialogContent
        class="fixed top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 z-50 w-[94vw] max-w-md bg-[#101825] border border-[#23354d] rounded-2xl p-6 sm:p-8 shadow-2xl focus:outline-none text-[#f1f5f9] font-body animate-in zoom-in-95 duration-200"
      >
        <!-- Header con Logo y Botón de Cierre -->
        <div class="flex items-center justify-between pb-4 border-b border-[#23354d]">
          <div class="flex items-center gap-2.5">
            <img
              :src="logoUrl"
              alt="Hibana.tk"
              class="w-7 h-7 object-contain drop-shadow-[0_0_8px_rgba(134,207,251,0.5)]"
            />
            <span class="font-display font-black text-xl text-primary tracking-tight">Hibana.tk</span>
          </div>

          <DialogClose
            class="w-8 h-8 rounded-full bg-[#141c27] hover:bg-[#1b2533] border border-[#23354d] text-[#94a3b8] hover:text-white flex items-center justify-center transition-colors cursor-pointer"
            aria-label="Cerrar modal"
          >
            <span class="material-symbols-outlined text-[18px]">close</span>
          </DialogClose>
        </div>

        <DialogTitle class="sr-only">Acceso y Autenticación Hibana.tk</DialogTitle>
        <DialogDescription class="sr-only">Inicia sesión o regístrate para comprar entradas y acceder a salas de espera.</DialogDescription>

        <!-- Mensaje de bloqueo contextual (si intentó acceder a queue/seatmap) -->
        <div
          v-if="authStore.authNoticeMessage"
          class="mt-4 p-3 rounded-xl bg-[#00688f]/20 border border-[#38bdf8]/40 flex items-start gap-2.5 text-xs text-[#86cffb] font-mono"
        >
          <span class="material-symbols-outlined text-[18px] text-[#38bdf8] shrink-0 mt-0.5">lock</span>
          <span>{{ authStore.authNoticeMessage }}</span>
        </div>

        <!-- Selector de Pestañas (Iniciar Sesión vs Registrarse) -->
        <div class="mt-5 grid grid-cols-2 p-1 bg-[#0c141f] rounded-xl border border-[#23354d] font-mono text-xs">
          <button
            type="button"
            @click="authStore.authModalTab = 'login'"
            class="py-2.5 rounded-lg font-bold transition-all cursor-pointer text-center"
            :class="authStore.authModalTab === 'login' ? 'bg-[#00688f] text-white shadow-md' : 'text-[#94a3b8] hover:text-white'"
          >
            Iniciar Sesión
          </button>
          <button
            type="button"
            @click="authStore.authModalTab = 'register'"
            class="py-2.5 rounded-lg font-bold transition-all cursor-pointer text-center"
            :class="authStore.authModalTab === 'register' ? 'bg-[#00688f] text-white shadow-md' : 'text-[#94a3b8] hover:text-white'"
          >
            Crear Cuenta
          </button>
        </div>

        <!-- FORMULARIO 1: INICIAR SESIÓN -->
        <div v-if="authStore.authModalTab === 'login'" class="mt-5 space-y-4">
          <div
            v-if="loginError"
            class="p-2.5 rounded-lg bg-rose-950/60 border border-rose-500/50 text-rose-300 text-xs font-mono"
          >
            {{ loginError }}
          </div>

          <div>
            <label class="block font-mono text-xs text-[#94a3b8] mb-1.5">Correo electrónico</label>
            <div class="relative">
              <span class="material-symbols-outlined absolute left-3 top-1/2 -translate-y-1/2 text-[18px] text-[#64748b]">mail</span>
              <input
                v-model="loginEmail"
                type="email"
                placeholder="ej: carlos.m@hibana.tk"
                @keyup.enter="handleLoginSubmit"
                class="w-full bg-[#0c141f] border border-[#23354d] focus:border-[#38bdf8] rounded-xl py-2.5 pl-10 pr-4 text-xs font-mono text-white placeholder-[#475569] outline-none transition-colors"
              />
            </div>
          </div>

          <div>
            <div class="flex items-center justify-between mb-1.5">
              <label class="font-mono text-xs text-[#94a3b8]">Contraseña</label>
              <span class="font-mono text-[11px] text-[#86cffb] hover:underline cursor-pointer">¿Olvidaste tu clave?</span>
            </div>
            <div class="relative">
              <span class="material-symbols-outlined absolute left-3 top-1/2 -translate-y-1/2 text-[18px] text-[#64748b]">lock</span>
              <input
                v-model="loginPassword"
                type="password"
                placeholder="••••••••"
                @keyup.enter="handleLoginSubmit"
                class="w-full bg-[#0c141f] border border-[#23354d] focus:border-[#38bdf8] rounded-xl py-2.5 pl-10 pr-4 text-xs font-mono text-white placeholder-[#475569] outline-none transition-colors"
              />
            </div>
          </div>

          <!-- Botón Principal Iniciar Sesión -->
          <button
            type="button"
            @click="handleLoginSubmit"
            class="w-full bg-[#00688f] hover:bg-[#005a7d] text-white font-mono text-sm font-bold py-3 rounded-xl border border-[#00688f] transition-all flex items-center justify-center gap-2 cursor-pointer shadow-lg active:scale-98"
          >
            <span>Ingresar a mi cuenta</span>
            <span class="material-symbols-outlined text-[18px]">arrow_forward</span>
          </button>

          <!-- Botón de Acceso Rápido Demo (1 clic) -->
          <div class="pt-2">
            <button
              type="button"
              @click="handleQuickDemoLogin"
              class="w-full bg-[#141c27] hover:bg-[#1b2533] border border-[#38bdf8]/40 hover:border-[#38bdf8] text-[#86cffb] font-mono text-xs font-semibold py-2.5 rounded-xl transition-all flex items-center justify-center gap-2 cursor-pointer group"
              title="Iniciar sesión inmediatamente con el usuario demo de prueba"
            >
              <span class="material-symbols-outlined text-[16px] text-amber-400 group-hover:rotate-12 transition-transform">bolt</span>
              <span>Entrar rápido como {{ MOCK_USER.name }} (Demo)</span>
            </button>
          </div>
        </div>

        <!-- FORMULARIO 2: CREAR CUENTA -->
        <div v-else class="mt-5 space-y-3.5">
          <div
            v-if="regError"
            class="p-2.5 rounded-lg bg-rose-950/60 border border-rose-500/50 text-rose-300 text-xs font-mono"
          >
            {{ regError }}
          </div>

          <div>
            <label class="block font-mono text-xs text-[#94a3b8] mb-1">Nombre y Apellido</label>
            <input
              v-model="regName"
              type="text"
              placeholder="ej: Lucía Fernández"
              class="w-full bg-[#0c141f] border border-[#23354d] focus:border-[#38bdf8] rounded-xl py-2 px-3 text-xs font-mono text-white placeholder-[#475569] outline-none transition-colors"
            />
          </div>

          <div>
            <label class="block font-mono text-xs text-[#94a3b8] mb-1">Correo electrónico</label>
            <input
              v-model="regEmail"
              type="email"
              placeholder="ej: lucia@ejemplo.com"
              class="w-full bg-[#0c141f] border border-[#23354d] focus:border-[#38bdf8] rounded-xl py-2 px-3 text-xs font-mono text-white placeholder-[#475569] outline-none transition-colors"
            />
          </div>

          <div class="grid grid-cols-2 gap-3">
            <div>
              <label class="block font-mono text-xs text-[#94a3b8] mb-1">DNI / Documento</label>
              <input
                v-model="regDni"
                type="text"
                placeholder="ej: 39.120.400"
                class="w-full bg-[#0c141f] border border-[#23354d] focus:border-[#38bdf8] rounded-xl py-2 px-3 text-xs font-mono text-white placeholder-[#475569] outline-none transition-colors"
              />
            </div>
            <div>
              <label class="block font-mono text-xs text-[#94a3b8] mb-1">Contraseña</label>
              <input
                v-model="regPassword"
                type="password"
                placeholder="Mín. 6 caracteres"
                class="w-full bg-[#0c141f] border border-[#23354d] focus:border-[#38bdf8] rounded-xl py-2 px-3 text-xs font-mono text-white placeholder-[#475569] outline-none transition-colors"
              />
            </div>
          </div>

          <p class="text-[11px] text-[#64748b] leading-tight">
            Al registrarte aceptas las Condiciones de Compra y la Política de Privacidad de Hibana.tk.
          </p>

          <button
            type="button"
            @click="handleRegisterSubmit"
            class="w-full bg-[#00688f] hover:bg-[#005a7d] text-white font-mono text-sm font-bold py-3 rounded-xl border border-[#00688f] transition-all flex items-center justify-center gap-2 cursor-pointer shadow-lg active:scale-98 mt-2"
          >
            <span>Crear cuenta y continuar</span>
            <span class="material-symbols-outlined text-[18px]">how_to_reg</span>
          </button>
        </div>
      </DialogContent>
    </DialogPortal>
  </DialogRoot>
</template>
