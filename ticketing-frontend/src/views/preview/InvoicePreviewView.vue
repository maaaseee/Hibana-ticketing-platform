<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { orderService } from '../../services/orderService'

const previewUrl = ref<string>(orderService.getPreviewInvoicePdfUrl())
const pdfBlobUrl = ref<string>('')
const isLoading = ref<boolean>(true)
const loadError = ref<string | null>(null)
const timestamp = ref<number>(Date.now())

async function loadPdfBlob() {
  isLoading.value = true
  loadError.value = null
  timestamp.value = Date.now()
  previewUrl.value = `${orderService.getPreviewInvoicePdfUrl()}?t=${timestamp.value}`
  try {
    const res = await fetch(previewUrl.value)
    if (!res.ok) throw new Error(`Error HTTP: ${res.status}`)
    const blob = await res.blob()
    if (pdfBlobUrl.value) {
      URL.revokeObjectURL(pdfBlobUrl.value)
    }
    pdfBlobUrl.value = URL.createObjectURL(blob)
  } catch (err: any) {
    console.error('[INVOICE-PREVIEW] Error loading PDF blob:', err)
    loadError.value = err.message || 'No se pudo generar el documento PDF.'
  } finally {
    isLoading.value = false
  }
}

function refreshPreview() {
  loadPdfBlob()
}

function openInNewTab() {
  if (pdfBlobUrl.value) {
    window.open(pdfBlobUrl.value, '_blank')
  } else {
    window.open(previewUrl.value, '_blank')
  }
}

async function downloadSamplePdf() {
  await orderService.downloadInvoicePdf('PREVIEW-001')
}

onMounted(() => {
  loadPdfBlob()
})

onUnmounted(() => {
  if (pdfBlobUrl.value) {
    URL.revokeObjectURL(pdfBlobUrl.value)
  }
})
</script>

<template>
  <div class="min-h-screen bg-[#0c141f] text-[#f1f5f9] p-4 sm:p-6 lg:p-8 flex flex-col space-y-4">
    <!-- Top Bar Controls -->
    <header class="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4 p-4 rounded-2xl bg-[#141c27] border border-[#23354d]">
      <div class="space-y-1">
        <div class="flex items-center gap-2.5">
          <span class="text-xs font-mono font-bold text-[#86cffb] uppercase tracking-wider bg-[#86cffb]/10 px-2 py-0.5 rounded border border-[#86cffb]/30">
            Preview Mode • OpenPDF
          </span>
          <span class="text-xs font-mono font-bold text-[#4fdbc8] uppercase tracking-wider bg-[#4fdbc8]/10 px-2 py-0.5 rounded border border-[#4fdbc8]/30">
            Validez Fiscal AFIP (CAE)
          </span>
        </div>
        <h1 class="text-xl font-bold font-display text-white">
          Visor de Factura Oficial Hibana.tk
        </h1>
        <p class="text-xs text-[#94a3b8]">
          Motor: OpenPDF 2.0.3 + ZXing QR • Paleta Oficial: #0c141f, #141c27, #86cffb, #4fdbc8
        </p>
      </div>

      <div class="flex flex-wrap items-center gap-2">
        <button
          type="button"
          @click="refreshPreview"
          class="px-3.5 py-2 rounded-xl text-xs font-mono font-medium text-[#94a3b8] hover:text-[#f1f5f9] hover:bg-[#1c2738] border border-[#23354d] transition-colors flex items-center gap-1.5 cursor-pointer"
        >
          <span class="material-symbols-outlined text-base">refresh</span>
          <span>Recargar</span>
        </button>

        <button
          type="button"
          @click="openInNewTab"
          class="px-3.5 py-2 rounded-xl text-xs font-mono font-medium text-[#86cffb] hover:bg-[#86cffb]/10 border border-[#86cffb]/40 transition-colors flex items-center gap-1.5 cursor-pointer"
        >
          <span class="material-symbols-outlined text-base">open_in_new</span>
          <span>Abrir Pestaña</span>
        </button>

        <button
          type="button"
          @click="downloadSamplePdf"
          class="px-4 py-2 rounded-xl text-xs font-mono font-bold bg-[#00688f] hover:bg-[#005a7d] text-white shadow-lg shadow-[#00688f]/30 transition-all flex items-center gap-1.5 cursor-pointer"
        >
          <span class="material-symbols-outlined text-base">download</span>
          <span>Descargar PDF</span>
        </button>
      </div>
    </header>

    <!-- PDF IFrame Container -->
    <main class="flex-1 w-full bg-[#101825] border border-[#23354d] rounded-2xl overflow-hidden shadow-2xl relative flex flex-col justify-center items-center min-h-[500px]">
      <!-- Spinner -->
      <div v-if="isLoading" class="flex flex-col items-center gap-3">
        <div class="w-8 h-8 border-3 border-[#86cffb] border-t-transparent rounded-full animate-spin"></div>
        <span class="text-xs font-mono text-[#94a3b8]">Generando documento con OpenPDF...</span>
      </div>

      <!-- Error State -->
      <div v-else-if="loadError" class="p-8 text-center text-[#94a3b8] max-w-md">
        <span class="material-symbols-outlined text-4xl text-rose-400 mb-2">warning</span>
        <h3 class="text-base font-bold text-white mb-1">No se pudo cargar la vista previa</h3>
        <p class="text-xs text-[#94a3b8] mb-4">{{ loadError }}</p>
        <button
          @click="refreshPreview"
          class="px-4 py-2 bg-[#141c27] hover:bg-[#1c2738] text-[#86cffb] border border-[#23354d] rounded-xl text-xs font-mono font-bold inline-flex items-center gap-1.5 cursor-pointer"
        >
          <span class="material-symbols-outlined text-sm">refresh</span>
          Reintentar
        </button>
      </div>

      <!-- PDF Embedded via Object & IFrame with Blob URL -->
      <object
        v-else-if="pdfBlobUrl"
        :data="pdfBlobUrl"
        type="application/pdf"
        class="w-full h-[calc(100vh-170px)] rounded-2xl bg-white/5 border-0"
      >
        <iframe
          :src="pdfBlobUrl"
          class="w-full h-full border-0 rounded-2xl bg-white/5"
          title="Hibana Invoice PDF Preview"
        >
          <div class="p-8 text-center text-[#94a3b8] flex flex-col items-center justify-center h-full">
            <span class="material-symbols-outlined text-4xl text-[#86cffb] mb-2">description</span>
            <p class="text-sm text-white font-bold mb-1">Visualizador de PDF en Firefox</p>
            <p class="text-xs text-[#94a3b8] mb-4 max-w-sm">
              Tu navegador requiere abrir los documentos PDF en una pestaña externa o descargarlos.
            </p>
            <div class="flex items-center gap-3">
              <button
                @click="openInNewTab"
                class="px-4 py-2 bg-[#00688f] hover:bg-[#005a7d] text-white rounded-xl font-mono text-xs font-bold inline-flex items-center gap-1.5 cursor-pointer"
              >
                <span class="material-symbols-outlined text-sm">open_in_new</span>
                Abrir en nueva pestaña
              </button>
              <button
                @click="downloadSamplePdf"
                class="px-4 py-2 bg-[#141c27] hover:bg-[#1c2738] text-[#86cffb] border border-[#23354d] rounded-xl font-mono text-xs font-bold inline-flex items-center gap-1.5 cursor-pointer"
              >
                <span class="material-symbols-outlined text-sm">download</span>
                Descargar
              </button>
            </div>
          </div>
        </iframe>
      </object>
    </main>
  </div>
</template>
