<script setup lang="ts">
import { ref, computed } from 'vue'
import {
  DialogRoot,
  DialogPortal,
  DialogOverlay,
  DialogContent,
  DialogTitle,
  DialogDescription,
  DialogClose
} from 'radix-vue'
import { useToastStore } from '../../stores/toast'

const props = defineProps<{
  open: boolean
  initialEmail?: string
}>()

const emit = defineEmits<{
  (e: 'update:open', val: boolean): void
  (e: 'passwordResetSuccess', payload: { email: string; newPassword: string }): void
}>()

const toastStore = useToastStore()
const step = ref<1 | 2>(1)
const email = ref(props.initialEmail || '')
const verificationCode = ref('')
const newPassword = ref('')
const showPassword = ref(false)
const isLoading = ref(false)
const errorMessage = ref('')
const countdown = ref(0)
let timer: ReturnType<typeof setInterval> | null = null

// Password criteria
const passwordCriteria = computed(() => {
  const p = newPassword.value
  return [
    { id: 'length', label: 'Al menos 8 caracteres', met: p.length >= 8 },
    { id: 'uppercase', label: 'Una mayúscula (A-Z)', met: /[A-Z]/.test(p) },
    { id: 'lowercase', label: 'Una minúscula (a-z)', met: /[a-z]/.test(p) },
    { id: 'number', label: 'Al menos un número (0-9)', met: /\d/.test(p) },
    { id: 'special', label: 'Un carácter especial (@, #, !, etc.)', met: /[^A-Za-z0-9]/.test(p) }
  ]
})

const isPasswordValid = computed(() => {
  return passwordCriteria.value.every(c => c.met)
})

function startCountdown() {
  countdown.value = 60
  if (timer) clearInterval(timer)
  timer = setInterval(() => {
    if (countdown.value > 0) {
      countdown.value--
    } else {
      if (timer) clearInterval(timer)
    }
  }, 1000)
}

function handleSendCode() {
  errorMessage.value = ''
  if (!email.value.trim() || !email.value.includes('@')) {
    errorMessage.value = 'Por favor ingresá un correo electrónico válido.'
    return
  }

  isLoading.value = true
  setTimeout(() => {
    isLoading.value = false
    step.value = 2
    verificationCode.value = '849201' // Simulación de código para fluidez de testing
    startCountdown()
    toastStore.info('Código enviado', `Revisá la bandeja de entrada de ${email.value}`)
  }, 800)
}

function handleResetPassword() {
  errorMessage.value = ''
  if (!verificationCode.value.trim() || verificationCode.value.length < 6) {
    errorMessage.value = 'Ingresá el código de 6 dígitos que enviamos a tu correo.'
    return
  }
  if (!isPasswordValid.value) {
    errorMessage.value = 'La nueva contraseña debe cumplir con todos los requisitos en verde.'
    return
  }

  isLoading.value = true
  setTimeout(() => {
    isLoading.value = false
    toastStore.success('Contraseña restablecida', 'Ya podés ingresar con tu nueva contraseña')
    emit('passwordResetSuccess', { email: email.value, newPassword: newPassword.value })
    emit('update:open', false)
    step.value = 1
    newPassword.value = ''
  }, 800)
}
</script>

<template>
  <DialogRoot :open="open" @update:open="emit('update:open', $event)">
    <DialogPortal>
      <DialogOverlay class="fixed inset-0 z-50 bg-black/80 backdrop-blur-md transition-opacity" />
      <DialogContent
        class="fixed top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 z-50 w-full max-w-md bg-[#0c1524]/95 border border-[#22354e] rounded-3xl p-6 sm:p-7 shadow-[0_25px_70px_rgba(0,0,0,0.85)] text-white outline-none select-none"
      >
        <div class="flex items-center justify-between pb-3 border-b border-[#22354e]">
          <div class="flex items-center gap-2">
            <span class="material-symbols-outlined text-[#38bdf8] text-xl">lock_reset</span>
            <DialogTitle class="font-display font-black text-lg tracking-wide uppercase text-white">
              Recuperar Contraseña
            </DialogTitle>
          </div>
          <DialogClose class="text-[#64748b] hover:text-white p-1 rounded-lg cursor-pointer">
            <span class="material-symbols-outlined text-[18px]">close</span>
          </DialogClose>
        </div>

        <DialogDescription class="font-body text-xs text-[#94a3b8] mt-3 mb-4 leading-relaxed">
          {{
            step === 1
              ? 'Ingresá el correo electrónico asociado a tu cuenta de Hibana para recibir tu código de seguridad.'
              : 'Ingresá el código de 6 dígitos enviado y definí tu nueva contraseña segura.'
          }}
        </DialogDescription>

        <!-- Error alert -->
        <div v-if="errorMessage" class="mb-4 p-2.5 rounded-xl bg-rose-950/70 border border-rose-500/60 text-xs font-body text-rose-300 flex items-center gap-2">
          <span class="material-symbols-outlined text-[16px] text-rose-400">error</span>
          <span>{{ errorMessage }}</span>
        </div>

        <!-- PASO 1: Ingreso de Correo -->
        <form v-if="step === 1" @submit.prevent="handleSendCode" class="space-y-4">
          <div>
            <label class="block font-body text-[12px] text-[#cbd5e1] mb-1 font-semibold">Correo electrónico</label>
            <input
              v-model="email"
              type="email"
              required
              placeholder="ej: usuario@ejemplo.com"
              class="w-full bg-[#141f30] border border-[#22354e] focus:border-[#0084ff] focus:ring-1 focus:ring-[#0084ff] rounded-xl py-2.5 px-3.5 text-xs sm:text-sm font-body text-white placeholder-[#64748b] outline-none transition-all shadow-inner"
            />
          </div>

          <button
            type="submit"
            :disabled="isLoading"
            class="w-full py-3 rounded-full bg-[#0084ff] hover:bg-[#0070df] text-white font-display font-bold text-sm tracking-wide transition-all shadow-[0_0_20px_rgba(0,132,255,0.45)] cursor-pointer flex items-center justify-center gap-2 disabled:opacity-50"
          >
            <span v-if="isLoading" class="material-symbols-outlined text-[18px] animate-spin">progress_activity</span>
            <span>{{ isLoading ? 'Enviando código...' : 'Enviar Código de Seguridad' }}</span>
          </button>
        </form>

        <!-- PASO 2: Código + Nueva Contraseña -->
        <form v-else @submit.prevent="handleResetPassword" class="space-y-3.5">
          <div>
            <div class="flex items-center justify-between mb-1">
              <label class="block font-body text-[12px] text-[#cbd5e1] font-semibold">Código de verificación (6 dígitos)</label>
              <button
                type="button"
                :disabled="countdown > 0"
                @click="handleSendCode"
                class="text-[11px] font-body text-[#38bdf8] hover:underline disabled:opacity-50 disabled:no-underline cursor-pointer"
              >
                {{ countdown > 0 ? `Reenviar en ${countdown}s` : 'Reenviar código' }}
              </button>
            </div>
            <input
              v-model="verificationCode"
              type="text"
              maxlength="6"
              placeholder="000000"
              class="w-full bg-[#141f30] border border-[#22354e] focus:border-[#0084ff] rounded-xl py-2.5 px-3.5 text-center tracking-[0.25em] text-base font-mono font-bold text-white outline-none"
            />
          </div>

          <div>
            <label class="block font-body text-[12px] text-[#cbd5e1] mb-1 font-semibold">Nueva contraseña</label>
            <div class="relative">
              <input
                v-model="newPassword"
                :type="showPassword ? 'text' : 'password'"
                placeholder="Ingresá tu nueva contraseña"
                class="w-full bg-[#141f30] border rounded-xl py-2 px-3 pr-10 text-xs sm:text-sm font-body text-white outline-none transition-all shadow-inner"
                :class="isPasswordValid ? 'border-emerald-500' : 'border-[#22354e] focus:border-[#0084ff]'"
              />
              <button
                type="button"
                @click="showPassword = !showPassword"
                class="absolute right-3 top-1/2 -translate-y-1/2 text-[#64748b] hover:text-white"
                tabindex="-1"
              >
                <span class="material-symbols-outlined text-[18px]">{{ showPassword ? 'visibility_off' : 'visibility' }}</span>
              </button>
            </div>

            <!-- Requisitos en vivo -->
            <div class="mt-2 p-2 rounded-xl bg-black/40 border border-[#22354e] space-y-1">
              <div
                v-for="criterion in passwordCriteria"
                :key="criterion.id"
                class="flex items-center gap-1.5 text-[11px] font-body"
                :class="criterion.met ? 'text-emerald-400 font-semibold' : 'text-[#64748b]'"
              >
                <span class="material-symbols-outlined text-[14px]">
                  {{ criterion.met ? 'check_circle' : 'radio_button_unchecked' }}
                </span>
                <span>{{ criterion.label }}</span>
              </div>
            </div>
          </div>

          <button
            type="submit"
            :disabled="isLoading || !isPasswordValid"
            class="w-full py-3 rounded-full font-display font-bold text-sm tracking-wide transition-all shadow-md flex items-center justify-center gap-2"
            :class="isPasswordValid ? 'bg-[#0084ff] hover:bg-[#0070df] text-white shadow-[0_0_20px_rgba(0,132,255,0.45)] cursor-pointer' : 'bg-[#1e293b] text-[#64748b] cursor-not-allowed'"
          >
            <span>{{ isLoading ? 'Restableciendo...' : 'Guardar Nueva Contraseña' }}</span>
          </button>
        </form>
      </DialogContent>
    </DialogPortal>
  </DialogRoot>
</template>
