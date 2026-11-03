<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter, useRoute, RouterLink } from 'vue-router'
import { useAuthStore, MOCK_USER } from '../stores/auth'
import AppLogo from '../components/ui/AppLogo.vue'
import { CheckboxRoot, CheckboxIndicator } from 'radix-vue'
import authBgUrl from '../assets/images/auth_bg.jpg'

const authStore = useAuthStore()
const router = useRouter()
const route = useRoute()

const email = ref('')
const password = ref('')
const showPassword = ref(false)
const rememberSession = ref(true)
const errorMessage = ref('')
const noticeMessage = ref<string | null>(null)
const forgotPasswordNotice = ref(false)

onMounted(() => {
  // Si ya está logueado, redirigir
  if (authStore.isAuthenticated) {
    const redirect = (route.query.redirect as string) || '/'
    router.replace(redirect)
    return
  }

  if (route.query.notice) {
    noticeMessage.value = route.query.notice as string
  } else if (route.query.redirect) {
    noticeMessage.value = 'Iniciá sesión para continuar con la compra de tus entradas.'
  }
})

function handleLogin() {
  errorMessage.value = ''
  if (!email.value.trim() || !email.value.includes('@')) {
    errorMessage.value = 'Por favor ingresá un correo electrónico válido.'
    return
  }
  if (!password.value.trim()) {
    errorMessage.value = 'Por favor ingresá tu contraseña.'
    return
  }

  authStore.login(email.value, password.value)

  const redirect = (route.query.redirect as string) || authStore.redirectPathAfterLogin || '/'
  authStore.redirectPathAfterLogin = null
  router.push(redirect)
}

function handleQuickDemoLogin() {
  authStore.loginWithMock()
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

    <!-- Top Navigation Bar matching wireframe (Logo left, Registrate right) -->
    <header class="relative z-10 w-full px-6 sm:px-12 lg:px-16 py-6 flex justify-between items-center">
      <AppLogo size="sm" variant="pill" />

      <RouterLink
        :to="{ name: 'register', query: route.query }"
        class="font-display text-sm sm:text-base font-semibold text-[#38bdf8] hover:text-white px-4 py-1.5 rounded-full hover:bg-white/10 transition-colors"
      >
        Registrate
      </RouterLink>
    </header>

    <!-- Main Content Section: Left Title + Right Login Card -->
    <main class="relative z-10 flex-1 w-full max-w-7xl mx-auto px-6 sm:px-12 lg:px-16 py-8 flex flex-col lg:flex-row items-center justify-between gap-12">
      <!-- Left Side: Massive Hero Display Headline (Pill deleted as requested) -->
      <div class="w-full lg:w-1/2 flex flex-col items-start justify-center">
        <h1 class="font-display font-black text-6xl sm:text-7xl lg:text-8xl xl:text-9xl text-white tracking-tight leading-[0.95] uppercase drop-shadow-[0_10px_30px_rgba(0,0,0,0.8)]">
          SE<br />
          SIENTE
        </h1>
        <p class="mt-6 text-sm sm:text-base font-body text-[#cbd5e1] max-w-md drop-shadow">
          Viví la emoción de tus shows favoritos con tus entradas oficiales y seguras al instante.
        </p>
      </div>

      <!-- Right Side: Dark Glass Login Card matching wireframe -->
      <div class="w-full lg:w-auto flex justify-center lg:justify-end">
        <div class="w-full max-w-md bg-black/85 backdrop-blur-2xl border border-white/15 rounded-3xl p-7 sm:p-9 shadow-[0_20px_60px_rgba(0,0,0,0.85)] flex flex-col">
          <h2 class="font-display font-bold text-2xl sm:text-3xl text-white mb-1">
            Ingresá a tu cuenta
          </h2>
          <p class="text-xs font-body text-[#94a3b8] mb-1">
            Ingresá con tu correo y contraseña para acceder a tus entradas.
          </p>
          <p class="text-[11px] font-body text-[#38bdf8] mb-5">
            Los campos marcados con <span class="text-rose-400 font-bold">(*)</span> son obligatorios.
          </p>

          <!-- Context Notice Banner (if redirected from queue/seatmap) -->
          <div
            v-if="noticeMessage"
            class="mb-5 p-3 rounded-xl bg-[#00688f]/25 border border-[#38bdf8]/50 flex items-start gap-2.5 text-xs text-[#86cffb] font-body"
          >
            <span class="material-symbols-outlined text-[18px] text-[#38bdf8] shrink-0 mt-0.5">lock</span>
            <span>{{ noticeMessage }}</span>
          </div>

          <!-- Error Alert Banner -->
          <div
            v-if="errorMessage"
            class="mb-4 p-3 rounded-xl bg-rose-950/70 border border-rose-500/60 text-xs font-body text-rose-300 flex items-center gap-2"
          >
            <span class="material-symbols-outlined text-[16px] text-rose-400">error</span>
            <span>{{ errorMessage }}</span>
          </div>

          <form @submit.prevent="handleLogin" class="space-y-4">
            <!-- Email Input with Mail Icon -->
            <div>
              <label class="block font-body text-[12px] text-[#cbd5e1] mb-1 font-semibold">
                Correo electrónico <span class="text-rose-400 font-bold">*</span>
              </label>
              <div class="relative">
                <span class="material-symbols-outlined absolute left-3.5 top-1/2 -translate-y-1/2 text-[19px] text-[#64748b]">
                  mail
                </span>
                <input
                  v-model="email"
                  type="email"
                  required
                  placeholder="ej: usuario@ejemplo.com"
                  class="w-full bg-[#0c1524]/90 border border-[#22354e] focus:border-[#0084ff] focus:ring-1 focus:ring-[#0084ff] rounded-xl py-3 pl-11 pr-4 text-xs sm:text-sm font-body text-white placeholder-[#64748b] outline-none transition-all shadow-inner"
                />
              </div>
            </div>

            <!-- Password Input with Key Icon and Eye Toggle -->
            <div>
              <label class="block font-body text-[12px] text-[#cbd5e1] mb-1 font-semibold">
                Contraseña <span class="text-rose-400 font-bold">*</span>
              </label>
              <div class="relative">
                <span class="material-symbols-outlined absolute left-3.5 top-1/2 -translate-y-1/2 text-[19px] text-[#64748b]">
                  vpn_key
                </span>
                <input
                  v-model="password"
                  :type="showPassword ? 'text' : 'password'"
                  required
                  placeholder="Ingresá tu contraseña"
                  class="w-full bg-[#0c1524]/90 border border-[#22354e] focus:border-[#0084ff] focus:ring-1 focus:ring-[#0084ff] rounded-xl py-3 pl-11 pr-11 text-xs sm:text-sm font-body text-white placeholder-[#64748b] outline-none transition-all shadow-inner"
                />
                <button
                  type="button"
                  @click="showPassword = !showPassword"
                  class="absolute right-3.5 top-1/2 -translate-y-1/2 text-[#64748b] hover:text-white transition-colors cursor-pointer"
                  tabindex="-1"
                >
                  <span class="material-symbols-outlined text-[19px]">
                    {{ showPassword ? 'visibility_off' : 'visibility' }}
                  </span>
                </button>
              </div>
            </div>

            <!-- Forgot password link -->
            <div class="pt-0.5">
              <button
                type="button"
                @click="forgotPasswordNotice = !forgotPasswordNotice"
                class="font-body text-xs text-[#00a3ff] hover:text-[#38bdf8] hover:underline cursor-pointer text-left"
              >
                Olvidé mi contraseña
              </button>
              <p v-if="forgotPasswordNotice" class="text-[11px] font-body text-[#86cffb] mt-1.5 p-2 rounded-lg bg-[#00688f]/20 border border-[#38bdf8]/30">
                Te enviaremos las instrucciones a tu correo para restablecer tu contraseña.
              </p>
            </div>

            <!-- Keep session active checkbox (Radix UI Checkbox) -->
            <div class="flex items-center gap-2.5 pt-1 select-none">
              <CheckboxRoot
                id="remember-session"
                v-model:checked="rememberSession"
                class="w-4 h-4 rounded border border-[#22354e] bg-[#0c1524] data-[state=checked]:bg-[#0084ff] data-[state=checked]:border-[#0084ff] flex items-center justify-center transition-colors focus:outline-none focus:ring-1 focus:ring-[#0084ff] cursor-pointer"
              >
                <CheckboxIndicator class="text-white flex items-center justify-center">
                  <span class="material-symbols-outlined text-[13px] font-bold">check</span>
                </CheckboxIndicator>
              </CheckboxRoot>
              <label for="remember-session" class="font-body text-xs text-[#94a3b8] cursor-pointer select-none">
                Mantener la sesión iniciada
              </label>
            </div>

            <!-- Submit Button (Vibrant Blue Pill matching wireframe) -->
            <button
              type="submit"
              class="w-full mt-4 py-3.5 rounded-full bg-[#0084ff] hover:bg-[#0070df] text-white font-display font-bold text-sm sm:text-base tracking-wide transition-all shadow-[0_0_25px_rgba(0,132,255,0.45)] hover:shadow-[0_0_35px_rgba(0,132,255,0.65)] cursor-pointer active:scale-98 flex items-center justify-center gap-2"
            >
              <span>Ingresar</span>
            </button>
          </form>

          <!-- Quick Demo Access 1-Click Button -->
          <div class="mt-6 pt-5 border-t border-[#1e293b] flex flex-col gap-2">
            <button
              type="button"
              @click="handleQuickDemoLogin"
              class="w-full py-2.5 px-3 rounded-xl bg-[#0e1726]/80 hover:bg-[#152338] border border-[#38bdf8]/40 hover:border-[#38bdf8] text-[#86cffb] font-body text-xs font-semibold flex items-center justify-center gap-2 transition-all cursor-pointer group"
              title="Iniciar sesión como Carlos Mendoza"
            >
              <span class="material-symbols-outlined text-amber-400 text-[17px] group-hover:rotate-12 transition-transform">bolt</span>
              <span>Ingreso rápido con Carlos Mendoza (Usuario demo)</span>
            </button>
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
