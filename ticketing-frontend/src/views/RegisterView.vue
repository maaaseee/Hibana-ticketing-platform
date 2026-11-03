<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute, RouterLink } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import AppLogo from '../components/ui/AppLogo.vue'
import BirthDatePicker from '../components/ui/BirthDatePicker.vue'
import {
  SelectRoot,
  SelectTrigger,
  SelectValue,
  SelectPortal,
  SelectContent,
  SelectViewport,
  SelectItem,
  SelectItemText,
  SelectItemIndicator,
  CheckboxRoot,
  CheckboxIndicator
} from 'radix-vue'
import authBgUrl from '../assets/images/auth_bg.jpg'

const authStore = useAuthStore()
const router = useRouter()
const route = useRoute()

// Paso actual del registro: 1 = Identificate (DNI), 2 = Validá, 3 = Creá tu cuenta
const step = ref(1)

// Datos de documentación (Paso 1: Identificate - DNI Argentino)
const documentType = ref('DNI')
const documentNumber = ref('')
const gender = ref('')

// Datos de validación (Paso 2: Validá)
const tramiteNumber = ref('')
const birthDate = ref('')

// Datos de cuenta (Paso 3: Creá tu cuenta)
const fullName = ref('')
const email = ref('')
const password = ref('')
const showPassword = ref(false)
const acceptedTerms = ref(false)

const errorMessage = ref('')

// Checklist reactivo de requerimientos de contraseña
const passwordCriteria = computed(() => [
  { id: 'min_length', label: 'Al menos 8 caracteres', met: password.value.length >= 8 },
  { id: 'uppercase', label: 'Una letra mayúscula (A-Z)', met: /[A-Z]/.test(password.value) },
  { id: 'lowercase', label: 'Una letra minúscula (a-z)', met: /[a-z]/.test(password.value) },
  { id: 'number', label: 'Al menos un número (0-9)', met: /[0-9]/.test(password.value) },
  { id: 'special', label: 'Un símbolo o carácter especial (!@#$%...)', met: /[^A-Za-z0-9]/.test(password.value) }
])

const isPasswordValid = computed(() => passwordCriteria.value.every(c => c.met))

onMounted(() => {
  if (authStore.isAuthenticated) {
    const redirect = (route.query.redirect as string) || '/'
    router.replace(redirect)
  }
})

function handleStep1Next() {
  errorMessage.value = ''

  if (!documentNumber.value.trim()) {
    errorMessage.value = 'Por favor ingresá tu número de documento.'
    return
  }
  if (!gender.value) {
    errorMessage.value = 'Por favor seleccioná tu género según figura en tu documento.'
    return
  }

  step.value = 2
}

function handleStep2Next() {
  errorMessage.value = ''

  if (!tramiteNumber.value.trim()) {
    errorMessage.value = 'Por favor ingresá el número de trámite de tu DNI.'
    return
  }
  if (!birthDate.value.trim()) {
    errorMessage.value = 'Por favor seleccioná tu fecha de nacimiento (campo obligatorio).'
    return
  }

  step.value = 3
}

function handleRegister() {
  errorMessage.value = ''

  if (!fullName.value.trim()) {
    errorMessage.value = 'Por favor ingresá tu nombre y apellido completo.'
    return
  }
  if (!email.value.trim() || !email.value.includes('@')) {
    errorMessage.value = 'Por favor ingresá un correo electrónico válido.'
    return
  }
  if (!isPasswordValid.value) {
    errorMessage.value = 'La contraseña debe cumplir con todos los requisitos en verde.'
    return
  }
  if (!acceptedTerms.value) {
    errorMessage.value = 'Debes aceptar las Políticas de privacidad y los Términos y condiciones.'
    return
  }

  authStore.register({
    name: fullName.value,
    email: email.value,
    documentNumber: `${documentType.value}: ${documentNumber.value}`,
    password: password.value
  })

  const redirect = (route.query.redirect as string) || authStore.redirectPathAfterLogin || '/'
  authStore.redirectPathAfterLogin = null
  router.push(redirect)
}
</script>

<template>
  <div class="relative w-full min-h-screen flex flex-col justify-between overflow-x-hidden select-none text-[#f1f5f9] font-body bg-[#020b1f]">
    <!-- Fixed Concert Background Image Layer with Vibrant Blue Mask -->
    <div class="fixed inset-0 pointer-events-none overflow-hidden z-0 bg-[#020b1f]">
      <img
        :src="authBgUrl"
        alt="Concierto de fondo"
        class="w-full h-full object-cover object-center scale-105"
      />
      <!-- Primary Blue Color Wash Filter (transforms black/grey tones into electric cobalt blue) -->
      <div class="absolute inset-0 bg-[#0052cc]/45 mix-blend-color"></div>
      <!-- Deep Blue Gradient Mask (ensures text readability while bathing the live concert scene in cobalt and navy blue) -->
      <div class="absolute inset-0 bg-gradient-to-tr from-[#021844]/90 via-[#003882]/75 to-[#005fb8]/65 mix-blend-multiply"></div>
      <!-- Soft ambient blue vignette -->
      <div class="absolute inset-0 bg-[radial-gradient(ellipse_at_center,_transparent_20%,_#021233_95%)] opacity-80"></div>
    </div>

    <!-- Top Navigation Bar matching wireframe (Unified Logo left, ¿Ya tenés cuenta? right) -->
    <header class="relative z-10 w-full px-6 sm:px-12 lg:px-16 py-6 flex justify-between items-center">
      <AppLogo size="sm" variant="pill" />

      <div class="text-xs sm:text-sm font-body text-[#cbd5e1]">
        <span>¿Ya tenés cuenta? </span>
        <RouterLink
          :to="{ name: 'login', query: route.query }"
          class="font-display font-bold text-[#38bdf8] hover:text-white transition-colors underline ml-1"
        >
          Ingresá acá
        </RouterLink>
      </div>
    </header>

    <!-- Main Content Section: Left Title + Right Register Card -->
    <main class="relative z-10 flex-1 w-full max-w-7xl mx-auto px-6 sm:px-12 lg:px-16 py-6 flex flex-col lg:flex-row items-center justify-between gap-12">
      <!-- Left Side: Massive Hero Display Headline (No pill on top) -->
      <div class="w-full lg:w-1/2 flex flex-col items-start justify-center">
        <h1 class="font-display font-black text-5xl sm:text-6xl lg:text-7xl xl:text-8xl text-white tracking-tight leading-[1.05] uppercase drop-shadow-[0_10px_30px_rgba(0,0,0,0.8)]">
          ESTÁS A UN<br />
          PASO DE TUS<br />
          SHOWS<br />
          FAVORITOS
        </h1>
        <p class="mt-6 text-sm sm:text-base font-body text-[#cbd5e1] max-w-md drop-shadow">
          Completá tu registro para comprar tus entradas, ver tus fechas favoritas y asegurar tu lugar en los mejores espectáculos.
        </p>
      </div>

      <!-- Right Side: Dark Glass Register Card with FIXED STABLE HEIGHT across all steps -->
      <div class="w-full lg:w-auto flex justify-center lg:justify-end">
        <div class="w-full max-w-lg min-h-[690px] h-[690px] bg-black/85 backdrop-blur-2xl border border-white/15 rounded-3xl p-6 sm:p-8 shadow-[0_20px_60px_rgba(0,0,0,0.85)] flex flex-col justify-between">
          
          <!-- Top Area: Stepper, Dynamic Step Title & Subtitle, Error Alert -->
          <div>
            <!-- Stepper Breadcrumb matching wireframe (1. Identificate > 2. Validá > 3. Creá tu cuenta) -->
            <div class="flex items-center gap-1.5 sm:gap-2 font-body text-[11px] overflow-x-auto pb-1 mb-4">
              <!-- Step 1: Identificate -->
              <div
                class="px-3 py-1 rounded-full font-bold flex items-center gap-1.5 shrink-0 transition-colors"
                :class="step === 1 ? 'bg-[#e11d48] text-white shadow-sm' : step > 1 ? 'border border-emerald-500/50 bg-emerald-950/40 text-emerald-300 cursor-pointer' : 'border border-[#2e3e57] text-[#94a3b8]'"
                @click="step > 1 ? step = 1 : null"
              >
                <span class="material-symbols-outlined text-[14px]">
                  {{ step > 1 ? 'check_circle' : 'badge' }}
                </span>
                <span>1. Identificate</span>
              </div>
              <span class="text-[#64748b]">&gt;</span>

              <!-- Step 2: Validá -->
              <div
                class="px-3 py-1 rounded-full font-medium flex items-center gap-1.5 shrink-0 transition-colors"
                :class="step === 2 ? 'bg-[#e11d48] text-white font-bold shadow-sm' : step > 2 ? 'border border-emerald-500/50 bg-emerald-950/40 text-emerald-300 cursor-pointer' : 'border border-[#2e3e57] text-[#94a3b8]'"
                @click="step > 2 ? step = 2 : null"
              >
                <span class="material-symbols-outlined text-[14px]">
                  {{ step > 2 ? 'check_circle' : 'verified_user' }}
                </span>
                <span>2. Validá</span>
              </div>
              <span class="text-[#64748b]">&gt;</span>

              <!-- Step 3: Creá tu cuenta -->
              <div
                class="px-3 py-1 rounded-full font-medium flex items-center gap-1.5 shrink-0 transition-colors"
                :class="step === 3 ? 'bg-[#0084ff] text-white font-bold shadow-sm' : 'border border-[#2e3e57] text-[#94a3b8]'"
              >
                <span class="material-symbols-outlined text-[14px]">person_add</span>
                <span>3. Creá tu cuenta</span>
              </div>
            </div>

            <!-- Title & Instructions -->
            <h2 class="font-display font-black text-2xl tracking-wider text-white uppercase mb-1">
              {{ step === 1 ? 'REGISTRATE' : step === 2 ? 'VALIDÁ TU IDENTIDAD' : 'CREÁ TU CUENTA' }}
            </h2>
            <p class="text-xs font-body text-[#94a3b8] mb-1">
              {{
                step === 1
                  ? 'Para empezar, necesitamos conocer tu identidad. Por favor, completá los siguientes datos:'
                  : step === 2
                  ? 'Ingresá los datos de tu DNI para verificar tu titularidad oficial de entradas:'
                  : 'Completá tus datos de contacto para acceder a tus entradas:'
              }}
            </p>
            <p class="text-[11px] font-body text-[#38bdf8] mb-3">
              Los campos marcados con <span class="text-rose-400 font-bold">(*)</span> son obligatorios.
            </p>

            <!-- Error Alert Banner -->
            <div
              v-if="errorMessage"
              class="mb-3 p-2.5 rounded-xl bg-rose-950/70 border border-rose-500/60 text-xs font-body text-rose-300 flex items-center gap-2"
            >
              <span class="material-symbols-outlined text-[16px] text-rose-400">error</span>
              <span>{{ errorMessage }}</span>
            </div>
          </div>

          <!-- Middle Area: Active Step Forms (Contained within stable layout) -->
          <div class="flex-1 flex flex-col justify-between py-1">
            <!-- ================= PASO 1: IDENTIFICATE (DOCUMENTACIÓN DNI ARGENTINO CON RADIX UI) ================= -->
            <form v-if="step === 1" @submit.prevent="handleStep1Next" class="flex-1 flex flex-col justify-between">
              <div class="space-y-3.5">
                <!-- Tipo de documento con Radix UI Select (Ancho completo del contenedor) -->
                <div>
                  <label class="block font-body text-[12px] text-[#cbd5e1] mb-1 font-semibold">
                    Tipo de documento <span class="text-rose-400 font-bold">*</span>
                  </label>
                  <SelectRoot v-model="documentType">
                    <SelectTrigger
                      class="w-full bg-[#0c1524]/90 border border-[#22354e] focus:border-[#0084ff] hover:border-[#38bdf8]/50 rounded-xl py-2.5 px-3.5 text-xs sm:text-sm font-body text-white flex items-center justify-between outline-none transition-colors cursor-pointer"
                      aria-label="Tipo de documento"
                    >
                      <SelectValue placeholder="Seleccionar tipo de documento" />
                      <span class="material-symbols-outlined text-[18px] text-[#64748b]">expand_more</span>
                    </SelectTrigger>
                    <SelectPortal>
                      <SelectContent
                        class="z-50 bg-[#0e1726] border border-[#22354e] rounded-xl shadow-2xl p-1 text-xs sm:text-sm text-white w-[var(--radix-select-trigger-width)] min-w-[var(--radix-select-trigger-width)]"
                        position="popper"
                        :side-offset="4"
                      >
                        <SelectViewport class="p-1 w-full">
                          <SelectItem
                            value="DNI"
                            class="w-full flex items-center justify-between px-3 py-2 rounded-lg hover:bg-[#0084ff] hover:text-white cursor-pointer outline-none transition-colors data-[highlighted]:bg-[#0084ff] data-[highlighted]:text-white"
                          >
                            <SelectItemText>DNI - Documento Nacional de Identidad</SelectItemText>
                            <SelectItemIndicator>
                              <span class="material-symbols-outlined text-[14px]">check</span>
                            </SelectItemIndicator>
                          </SelectItem>
                          <SelectItem
                            value="Pasaporte"
                            class="w-full flex items-center justify-between px-3 py-2 rounded-lg hover:bg-[#0084ff] hover:text-white cursor-pointer outline-none transition-colors data-[highlighted]:bg-[#0084ff] data-[highlighted]:text-white"
                          >
                            <SelectItemText>Pasaporte Internacional</SelectItemText>
                            <SelectItemIndicator>
                              <span class="material-symbols-outlined text-[14px]">check</span>
                            </SelectItemIndicator>
                          </SelectItem>
                          <SelectItem
                            value="Cédula"
                            class="w-full flex items-center justify-between px-3 py-2 rounded-lg hover:bg-[#0084ff] hover:text-white cursor-pointer outline-none transition-colors data-[highlighted]:bg-[#0084ff] data-[highlighted]:text-white"
                          >
                            <SelectItemText>Cédula de Identidad Mercosur</SelectItemText>
                            <SelectItemIndicator>
                              <span class="material-symbols-outlined text-[14px]">check</span>
                            </SelectItemIndicator>
                          </SelectItem>
                        </SelectViewport>
                      </SelectContent>
                    </SelectPortal>
                  </SelectRoot>
                </div>

                <!-- Número de documento -->
                <div>
                  <label class="block font-body text-[12px] text-[#cbd5e1] mb-1 font-semibold">
                    Número de documento <span class="text-rose-400 font-bold">*</span>
                  </label>
                  <input
                    v-model="documentNumber"
                    type="text"
                    placeholder="Ingresá tu DNI (sin puntos ni espacios)"
                    class="w-full bg-[#0c1524]/90 border border-[#22354e] focus:border-[#0084ff] focus:ring-1 focus:ring-[#0084ff] rounded-xl py-2.5 px-3.5 text-xs sm:text-sm font-body text-white placeholder-[#64748b] outline-none transition-all shadow-inner"
                  />
                </div>

                <!-- Género según documento con Radix UI Select (Ancho completo del contenedor) -->
                <div>
                  <label class="block font-body text-[12px] text-[#cbd5e1] mb-1 font-semibold">
                    Género (según figura en tu DNI) <span class="text-rose-400 font-bold">*</span>
                  </label>
                  <SelectRoot v-model="gender">
                    <SelectTrigger
                      class="w-full bg-[#0c1524]/90 border border-[#22354e] focus:border-[#0084ff] hover:border-[#38bdf8]/50 rounded-xl py-2.5 px-3.5 text-xs sm:text-sm font-body text-white flex items-center justify-between outline-none transition-colors cursor-pointer data-[placeholder]:text-[#64748b]"
                      aria-label="Género según DNI"
                    >
                      <SelectValue placeholder="Seleccionar género" />
                      <span class="material-symbols-outlined text-[18px] text-[#64748b]">expand_more</span>
                    </SelectTrigger>
                    <SelectPortal>
                      <SelectContent
                        class="z-50 bg-[#0e1726] border border-[#22354e] rounded-xl shadow-2xl p-1 text-xs sm:text-sm text-white w-[var(--radix-select-trigger-width)] min-w-[var(--radix-select-trigger-width)]"
                        position="popper"
                        :side-offset="4"
                      >
                        <SelectViewport class="p-1 w-full">
                          <SelectItem
                            value="Femenino"
                            class="w-full flex items-center justify-between px-3 py-2 rounded-lg hover:bg-[#0084ff] hover:text-white cursor-pointer outline-none transition-colors data-[highlighted]:bg-[#0084ff] data-[highlighted]:text-white"
                          >
                            <SelectItemText>Femenino</SelectItemText>
                            <SelectItemIndicator>
                              <span class="material-symbols-outlined text-[14px]">check</span>
                            </SelectItemIndicator>
                          </SelectItem>
                          <SelectItem
                            value="Masculino"
                            class="w-full flex items-center justify-between px-3 py-2 rounded-lg hover:bg-[#0084ff] hover:text-white cursor-pointer outline-none transition-colors data-[highlighted]:bg-[#0084ff] data-[highlighted]:text-white"
                          >
                            <SelectItemText>Masculino</SelectItemText>
                            <SelectItemIndicator>
                              <span class="material-symbols-outlined text-[14px]">check</span>
                            </SelectItemIndicator>
                          </SelectItem>
                          <SelectItem
                            value="No binario"
                            class="w-full flex items-center justify-between px-3 py-2 rounded-lg hover:bg-[#0084ff] hover:text-white cursor-pointer outline-none transition-colors data-[highlighted]:bg-[#0084ff] data-[highlighted]:text-white"
                          >
                            <SelectItemText>No binario</SelectItemText>
                            <SelectItemIndicator>
                              <span class="material-symbols-outlined text-[14px]">check</span>
                            </SelectItemIndicator>
                          </SelectItem>
                          <SelectItem
                            value="Prefiero no decir"
                            class="w-full flex items-center justify-between px-3 py-2 rounded-lg hover:bg-[#0084ff] hover:text-white cursor-pointer outline-none transition-colors data-[highlighted]:bg-[#0084ff] data-[highlighted]:text-white"
                          >
                            <SelectItemText>Prefiero no decir</SelectItemText>
                            <SelectItemIndicator>
                              <span class="material-symbols-outlined text-[14px]">check</span>
                            </SelectItemIndicator>
                          </SelectItem>
                        </SelectViewport>
                      </SelectContent>
                    </SelectPortal>
                  </SelectRoot>
                </div>

                <!-- Helper Card no técnico de seguridad -->
                <div class="p-3 rounded-xl bg-[#0c1524]/60 border border-[#22354e]/70 flex items-start gap-2.5">
                  <span class="material-symbols-outlined text-[18px] text-[#38bdf8] shrink-0 mt-0.5">verified</span>
                  <p class="text-[11px] font-body text-[#94a3b8] leading-relaxed">
                    Tus entradas quedarán nominadas a tu DNI para garantizar tu ingreso y prevenir fraudes o reventas.
                  </p>
                </div>
              </div>

              <!-- Botón Siguiente -->
              <div class="pt-4">
                <button
                  type="submit"
                  class="w-full py-3.5 rounded-full bg-[#0084ff] hover:bg-[#0070df] text-white font-display font-bold text-sm sm:text-base tracking-wide transition-all shadow-[0_0_25px_rgba(0,132,255,0.45)] hover:shadow-[0_0_35px_rgba(0,132,255,0.65)] cursor-pointer active:scale-98 flex items-center justify-center gap-2"
                >
                  <span>Siguiente</span>
                  <span class="material-symbols-outlined text-[18px]">arrow_forward</span>
                </button>
              </div>
            </form>

            <!-- ================= PASO 2: VALIDÁ (VALIDACIÓN DE TITULARIDAD DNI CON BIRTH DATE PICKER) ================= -->
            <form v-else-if="step === 2" @submit.prevent="handleStep2Next" class="flex-1 flex flex-col justify-between">
              <div class="space-y-3.5">
                <!-- Resumen DNI verificado en paso 1 -->
                <div class="p-3 rounded-xl bg-[#0c1524]/90 border border-[#22354e] flex items-center justify-between">
                  <div class="flex items-center gap-2.5">
                    <span class="material-symbols-outlined text-[18px] text-[#38bdf8]">badge</span>
                    <span class="text-xs font-body text-white font-semibold">{{ documentType }}: {{ documentNumber }} ({{ gender }})</span>
                  </div>
                  <button
                    type="button"
                    @click="step = 1"
                    class="text-[11px] text-[#38bdf8] hover:underline font-body cursor-pointer"
                  >
                    Modificar
                  </button>
                </div>

                <!-- Número de trámite del DNI -->
                <div>
                  <label class="block font-body text-[12px] text-[#cbd5e1] mb-1 font-semibold">
                    Número de trámite del DNI <span class="text-rose-400 font-bold">*</span>
                  </label>
                  <input
                    v-model="tramiteNumber"
                    type="text"
                    maxlength="11"
                    placeholder="Ej: 00123456789 (11 dígitos)"
                    class="w-full bg-[#0c1524]/90 border border-[#22354e] focus:border-[#0084ff] focus:ring-1 focus:ring-[#0084ff] rounded-xl py-2.5 px-3.5 text-xs sm:text-sm font-body text-white placeholder-[#64748b] outline-none transition-all shadow-inner"
                  />
                  <p class="text-[11px] font-body text-[#94a3b8] mt-1">
                    Son los 11 dígitos que figuran en el frente de tu DNI (debajo de la foto) o en el dorso según tu ejemplar.
                  </p>
                </div>

                <!-- Fecha de nacimiento con BirthDatePicker (Radix UI Segmented Selects) -->
                <div>
                  <label class="block font-body text-[12px] text-[#cbd5e1] mb-1 font-semibold">
                    Fecha de nacimiento <span class="text-rose-400 font-bold">*</span>
                  </label>
                  <BirthDatePicker v-model="birthDate" />
                </div>

                <!-- Helper visual de ubicación del trámite -->
                <div class="p-3 rounded-xl bg-emerald-950/20 border border-emerald-500/30 flex items-center gap-3">
                  <span class="material-symbols-outlined text-emerald-400 text-xl shrink-0">id_card</span>
                  <div class="text-[11px] font-body text-emerald-200">
                    Validación instantánea para vincular de forma segura tus entradas a tu identidad real.
                  </div>
                </div>
              </div>

              <!-- Botones de Acción Paso 2 -->
              <div class="flex items-center gap-3 pt-4">
                <button
                  type="button"
                  @click="step = 1"
                  class="w-1/3 py-3 rounded-full bg-[#141d2b] hover:bg-[#1c293d] text-[#cbd5e1] font-display font-semibold text-xs sm:text-sm transition-colors cursor-pointer"
                >
                  Volver
                </button>
                <button
                  type="submit"
                  class="w-2/3 py-3.5 rounded-full bg-[#0084ff] hover:bg-[#0070df] text-white font-display font-bold text-sm sm:text-base tracking-wide transition-all shadow-[0_0_25px_rgba(0,132,255,0.45)] hover:shadow-[0_0_35px_rgba(0,132,255,0.65)] cursor-pointer active:scale-98 flex items-center justify-center gap-2"
                >
                  <span>Validar y Continuar</span>
                  <span class="material-symbols-outlined text-[18px]">arrow_forward</span>
                </button>
              </div>
            </form>

            <!-- ================= PASO 3: CREÁ TU CUENTA (DATOS DE ACCESO + CHECKLIST EN VIVO + RADIX CHECKBOX) ================= -->
            <form v-else-if="step === 3" @submit.prevent="handleRegister" class="flex-1 flex flex-col justify-between">
              <div class="space-y-3">
                <!-- Nombre y Apellido -->
                <div>
                  <label class="block font-body text-[12px] text-[#cbd5e1] mb-1 font-semibold">
                    Nombre y Apellido completo <span class="text-rose-400 font-bold">*</span>
                  </label>
                  <input
                    v-model="fullName"
                    type="text"
                    placeholder="Ej: María Florencia Rossi"
                    class="w-full bg-[#0c1524]/90 border border-[#22354e] focus:border-[#0084ff] focus:ring-1 focus:ring-[#0084ff] rounded-xl py-2 px-3 text-xs sm:text-sm font-body text-white placeholder-[#64748b] outline-none transition-all shadow-inner"
                  />
                </div>

                <!-- Email -->
                <div>
                  <label class="block font-body text-[12px] text-[#cbd5e1] mb-1 font-semibold">
                    Correo electrónico <span class="text-rose-400 font-bold">*</span>
                  </label>
                  <input
                    v-model="email"
                    type="email"
                    placeholder="Ej: tucorreo@ejemplo.com"
                    class="w-full bg-[#0c1524]/90 border border-[#22354e] focus:border-[#0084ff] focus:ring-1 focus:ring-[#0084ff] rounded-xl py-2 px-3 text-xs sm:text-sm font-body text-white placeholder-[#64748b] outline-none transition-all shadow-inner"
                  />
                </div>

                <!-- Contraseña con toggle de visibilidad -->
                <div>
                  <label class="block font-body text-[12px] text-[#cbd5e1] mb-1 font-semibold">
                    Contraseña <span class="text-rose-400 font-bold">*</span>
                  </label>
                  <div class="relative">
                    <input
                      v-model="password"
                      :type="showPassword ? 'text' : 'password'"
                      placeholder="Ingresá una contraseña segura"
                      class="w-full bg-[#0c1524]/90 border rounded-xl py-2 pl-3 pr-10 text-xs sm:text-sm font-body text-white placeholder-[#64748b] outline-none transition-all shadow-inner"
                      :class="password.length === 0 ? 'border-[#22354e] focus:border-[#0084ff]' : isPasswordValid ? 'border-emerald-500 focus:border-emerald-500 focus:ring-1 focus:ring-emerald-500 shadow-[0_0_10px_rgba(16,185,129,0.2)]' : 'border-amber-500/80 focus:border-amber-500 focus:ring-1 focus:ring-amber-500'"
                    />
                    <button
                      type="button"
                      @click="showPassword = !showPassword"
                      class="absolute right-3 top-1/2 -translate-y-1/2 text-[#64748b] hover:text-white transition-colors cursor-pointer"
                      tabindex="-1"
                    >
                      <span class="material-symbols-outlined text-[18px]">
                        {{ showPassword ? 'visibility_off' : 'visibility' }}
                      </span>
                    </button>
                  </div>

                  <!-- Checklist de Requisitos de Contraseña en Vivo (Se marcan en verde a medida que se escriben) -->
                  <div class="mt-2 p-2.5 rounded-xl bg-[#0c1524]/70 border border-[#22354e]/80 space-y-1">
                    <p class="text-[11px] font-body text-[#94a3b8] font-semibold mb-1">
                      Tu contraseña debe contener:
                    </p>
                    <div class="grid grid-cols-1 sm:grid-cols-2 gap-x-2 gap-y-1">
                      <div
                        v-for="criterion in passwordCriteria"
                        :key="criterion.id"
                        class="criterion-item flex items-center gap-1.5 text-[11px] font-body transition-colors duration-200"
                        :class="criterion.met ? 'is-met text-emerald-400 font-semibold' : 'text-[#64748b]'"
                      >
                        <span
                          class="material-symbols-outlined text-[15px] transition-transform duration-200"
                          :class="criterion.met ? 'text-emerald-400 scale-110' : 'text-[#475569]'"
                        >
                          {{ criterion.met ? 'check_circle' : 'radio_button_unchecked' }}
                        </span>
                        <span>{{ criterion.label }}</span>
                      </div>
                    </div>
                  </div>
                </div>

                <!-- Términos y Condiciones con Radix UI Checkbox -->
                <div class="flex items-start gap-2.5 pt-1 select-none">
                  <CheckboxRoot
                    id="register-terms"
                    v-model:checked="acceptedTerms"
                    class="w-4 h-4 mt-0.5 rounded border border-[#22354e] bg-[#0c1524] data-[state=checked]:bg-[#0084ff] data-[state=checked]:border-[#0084ff] flex items-center justify-center transition-colors focus:outline-none focus:ring-1 focus:ring-[#0084ff] cursor-pointer shrink-0"
                  >
                    <CheckboxIndicator class="text-white flex items-center justify-center">
                      <span class="material-symbols-outlined text-[13px] font-bold">check</span>
                    </CheckboxIndicator>
                  </CheckboxRoot>
                  <label for="register-terms" class="font-body text-xs text-[#94a3b8] leading-tight cursor-pointer">
                    Leí y acepto las
                    <RouterLink to="/help" class="text-[#00a3ff] hover:underline">Políticas de privacidad</RouterLink>
                    y los
                    <RouterLink to="/help" class="text-[#00a3ff] hover:underline">Términos y condiciones</RouterLink>
                    <span class="text-rose-400 font-bold">*</span>.
                  </label>
                </div>
              </div>

              <!-- Botones de Acción Paso 3 -->
              <div class="flex items-center gap-3 pt-3">
                <button
                  type="button"
                  @click="step = 2"
                  class="w-1/3 py-3 rounded-full bg-[#141d2b] hover:bg-[#1c293d] text-[#cbd5e1] font-display font-semibold text-xs sm:text-sm transition-colors cursor-pointer"
                >
                  Volver
                </button>
                <button
                  type="submit"
                  :disabled="!isPasswordValid || !acceptedTerms"
                  class="w-2/3 py-3.5 rounded-full font-display font-bold text-sm tracking-wide transition-all flex items-center justify-center gap-2 shadow-sm"
                  :class="isPasswordValid && acceptedTerms ? 'bg-[#0084ff] hover:bg-[#0070df] text-white shadow-[0_0_25px_rgba(0,132,255,0.45)] cursor-pointer active:scale-98' : 'bg-[#1e293b] text-[#64748b] cursor-not-allowed'"
                >
                  <span>Registrarme</span>
                  <span class="material-symbols-outlined text-[18px]">check</span>
                </button>
              </div>
            </form>
          </div>
        </div>
      </div>
    </main>

    <!-- Subtle Footer with Security & Terms -->
    <footer class="relative z-10 w-full px-6 sm:px-12 py-4 flex flex-col sm:flex-row items-center justify-between text-xs font-body text-[#94a3b8] border-t border-white/10">
      <span>Hibana.tk © 2026 • Todos los derechos reservados.</span>
      <div class="flex items-center gap-4 mt-2 sm:mt-0">
        <RouterLink to="/help" class="hover:text-white transition-colors">Términos y Condiciones</RouterLink>
        <span>•</span>
        <RouterLink to="/help" class="hover:text-white transition-colors">Centro de Ayuda</RouterLink>
      </div>
    </footer>
  </div>
</template>
