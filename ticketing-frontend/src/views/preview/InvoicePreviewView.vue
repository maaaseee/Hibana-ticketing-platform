<script setup lang="ts">
import { ref } from 'vue'
import { orderService } from '../../services/orderService'

const previewUrl = ref<string>(orderService.getPreviewInvoicePdfUrl())
const timestamp = ref<number>(Date.now())

function refreshPreview() {
  timestamp.value = Date.now()
  previewUrl.value = `${orderService.getPreviewInvoicePdfUrl()}?t=${timestamp.value}`
}

function openInNewTab() {
  window.open(previewUrl.value, '_blank')
}

async function downloadSamplePdf() {
  await orderService.downloadInvoicePdf('PREVIEW-001')
}
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
    <main class="flex-1 w-full bg-[#101825] border border-[#23354d] rounded-2xl overflow-hidden shadow-2xl relative">
      <iframe
        :src="previewUrl"
        class="w-full h-[calc(100vh-170px)] border-0 rounded-2xl bg-white/5"
        title="Hibana Invoice PDF Preview"
      />
    </main>
  </div>
</template>
