<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { orderService } from '../../services/orderService'

const previewUrl = ref<string>(orderService.getPreviewEmailHtmlUrl())
const timestamp = ref<number>(Date.now())
const viewportMode = ref<'desktop' | 'mobile' | 'full'>('desktop')
const isSendingTest = ref(false)
const testSentStatus = ref<string | null>(null)
const emailHtml = ref<string>('')
const isLoading = ref<boolean>(true)

async function fetchEmailHtml() {
  isLoading.value = true
  timestamp.value = Date.now()
  previewUrl.value = `${orderService.getPreviewEmailHtmlUrl()}?t=${timestamp.value}`
  try {
    const res = await fetch(previewUrl.value)
    if (res.ok) {
      emailHtml.value = await res.text()
    }
  } catch (err) {
    console.error('Error fetching email template:', err)
  } finally {
    isLoading.value = false
  }
}

function refreshPreview() {
  fetchEmailHtml()
}

onMounted(() => {
  fetchEmailHtml()
})

function openMailpit() {
  window.open('http://localhost:8025', '_blank')
}

async function triggerTestEmail() {
  if (isSendingTest.value) return
  isSendingTest.value = true
  testSentStatus.value = null
  try {
    const res = await orderService.resendConfirmationEmail('TEST-SAMPLE-001', 'preview-tester@hibana.tk')
    testSentStatus.value = '✓ Correo de prueba enviado a Mailpit (puerto 8025)'
  } catch (err: any) {
    testSentStatus.value = 'Error al despachar el correo de prueba.'
  } finally {
    isSendingTest.value = false
    setTimeout(() => {
      testSentStatus.value = null
    }, 4500)
  }
}
</script>

<template>
  <div class="min-h-screen bg-[#0c141f] text-[#f1f5f9] p-4 sm:p-6 lg:p-8 flex flex-col space-y-4">
    <!-- Top Bar Controls -->
    <header class="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4 p-4 rounded-2xl bg-[#141c27] border border-[#23354d]">
      <div class="space-y-1">
        <div class="flex items-center gap-2.5">
          <span class="text-xs font-mono font-bold text-[#86cffb] uppercase tracking-wider bg-[#86cffb]/10 px-2 py-0.5 rounded border border-[#86cffb]/30">
            Email Template Preview
          </span>
          <span class="text-xs font-mono font-bold text-[#4fdbc8] uppercase tracking-wider bg-[#4fdbc8]/10 px-2 py-0.5 rounded border border-[#4fdbc8]/30">
            Hot Reload Active
          </span>
        </div>
        <h1 class="text-xl font-bold font-display text-white">
          Plantilla HTML de Confirmación y Factura
        </h1>
        <p class="text-xs text-[#94a3b8]">
          Diseño Responsivo Dark Blue con Botón CTA y Adjunto PDF • Mailpit Local :8025 / Azure ACS
        </p>
      </div>

      <!-- Controls: Viewport toggles + Actions -->
      <div class="flex flex-wrap items-center gap-2">
        <!-- Viewport Mode Buttons -->
        <div class="flex items-center bg-[#101825] border border-[#23354d] rounded-xl p-1">
          <button
            type="button"
            @click="viewportMode = 'desktop'"
            :class="[
              'px-2.5 py-1.5 rounded-lg text-xs font-mono transition-colors flex items-center gap-1 cursor-pointer',
              viewportMode === 'desktop' ? 'bg-[#00688f] text-white font-bold' : 'text-[#94a3b8] hover:text-white',
            ]"
            title="Desktop 600px standard"
          >
            <span class="material-symbols-outlined text-sm">desktop_windows</span>
            <span class="hidden sm:inline">600px</span>
          </button>
          <button
            type="button"
            @click="viewportMode = 'mobile'"
            :class="[
              'px-2.5 py-1.5 rounded-lg text-xs font-mono transition-colors flex items-center gap-1 cursor-pointer',
              viewportMode === 'mobile' ? 'bg-[#00688f] text-white font-bold' : 'text-[#94a3b8] hover:text-white',
            ]"
            title="Mobile 375px"
          >
            <span class="material-symbols-outlined text-sm">smartphone</span>
            <span class="hidden sm:inline">375px</span>
          </button>
          <button
            type="button"
            @click="viewportMode = 'full'"
            :class="[
              'px-2.5 py-1.5 rounded-lg text-xs font-mono transition-colors flex items-center gap-1 cursor-pointer',
              viewportMode === 'full' ? 'bg-[#00688f] text-white font-bold' : 'text-[#94a3b8] hover:text-white',
            ]"
            title="Full Width"
          >
            <span class="material-symbols-outlined text-sm">fullscreen</span>
            <span class="hidden sm:inline">100%</span>
          </button>
        </div>

        <button
          type="button"
          @click="refreshPreview"
          class="px-3 py-2 rounded-xl text-xs font-mono font-medium text-[#94a3b8] hover:text-[#f1f5f9] hover:bg-[#1c2738] border border-[#23354d] transition-colors flex items-center gap-1 cursor-pointer"
        >
          <span class="material-symbols-outlined text-base">refresh</span>
          <span>Recargar</span>
        </button>

        <button
          type="button"
          @click="openMailpit"
          class="px-3 py-2 rounded-xl text-xs font-mono font-medium text-[#4fdbc8] hover:bg-[#4fdbc8]/10 border border-[#4fdbc8]/40 transition-colors flex items-center gap-1 cursor-pointer"
          title="Abrir Mailpit Web UI en localhost:8025"
        >
          <span class="material-symbols-outlined text-base">mail</span>
          <span>Mailpit :8025</span>
        </button>

        <button
          type="button"
          @click="triggerTestEmail"
          :disabled="isSendingTest"
          class="px-3.5 py-2 rounded-xl text-xs font-mono font-bold bg-[#00688f] hover:bg-[#005a7d] text-white shadow-lg shadow-[#00688f]/30 transition-all flex items-center gap-1.5 cursor-pointer"
        >
          <span class="material-symbols-outlined text-base">send</span>
          <span>{{ isSendingTest ? 'Despachando...' : 'Enviar Prueba' }}</span>
        </button>
      </div>
    </header>

    <!-- Feedback Banner -->
    <div
      v-if="testSentStatus"
      class="p-2.5 rounded-xl bg-[#4fdbc8]/10 border border-[#4fdbc8]/30 text-[#4fdbc8] text-xs font-mono flex items-center justify-center gap-2 animate-in fade-in"
    >
      <span class="material-symbols-outlined text-base">check_circle</span>
      <span>{{ testSentStatus }}</span>
    </div>

    <!-- Email Preview Frame Container -->
    <main class="flex-1 w-full bg-[#080d14] border border-[#23354d] rounded-2xl p-4 flex items-center justify-center overflow-auto">
      <div
        :class="[
          'h-[calc(100vh-210px)] bg-[#0c141f] rounded-2xl overflow-hidden border border-[#23354d] shadow-2xl transition-all duration-300',
          viewportMode === 'desktop' ? 'w-[620px]' : viewportMode === 'mobile' ? 'w-[390px]' : 'w-full',
        ]"
      >
        <iframe
          :srcdoc="emailHtml"
          :src="previewUrl"
          class="w-full h-full border-0 bg-[#0c141f]"
          title="Hibana Email HTML Preview"
        />
      </div>
    </main>
  </div>
</template>
