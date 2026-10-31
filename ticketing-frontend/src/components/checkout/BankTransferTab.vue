<script setup lang="ts">
defineProps<{
  copiedNotice: string | null
}>()

const emit = defineEmits<{
  (e: 'copy', text: string, label: string): void
}>()
</script>

<template>
  <div class="card-level-1 rounded-2xl p-5 sm:p-7 flex flex-col gap-4 shadow-xl shadow-black/20">
    <div class="flex items-center gap-3">
      <div class="w-10 h-10 rounded-xl bg-[#00688f]/20 border border-[#00688f]/40 flex items-center justify-center text-[#86cffb]">
        <span class="material-symbols-outlined text-2xl">payments</span>
      </div>
      <div>
        <h3 class="font-display font-bold text-[#f1f5f9] text-base">Transferencia Bancaria Inmediata</h3>
        <p class="font-mono text-xs text-[#94a3b8]">Coelsa • DEBIN 24/7 • Acreditación en 3 minutos</p>
      </div>
    </div>

    <div class="p-4 rounded-xl bg-[#101825] border border-[#23354d] space-y-3 font-mono text-xs">
      <div class="flex justify-between items-center pb-2 border-b border-[#23354d]">
        <span class="text-[#94a3b8]">Alias CBU:</span>
        <button
          type="button"
          @click="emit('copy', 'HIBANA.PAGOS.OFICIAL', 'Alias')"
          class="text-[#86cffb] font-bold hover:underline flex items-center gap-1 cursor-pointer bg-transparent border-0"
        >
          <span>HIBANA.PAGOS.OFICIAL</span>
          <span class="material-symbols-outlined text-[14px]">content_copy</span>
        </button>
      </div>

      <div class="flex justify-between items-center pb-2 border-b border-[#23354d]">
        <span class="text-[#94a3b8]">CBU:</span>
        <button
          type="button"
          @click="emit('copy', '0000003100084920194820', 'CBU')"
          class="text-[#cbd5e1] font-bold hover:underline flex items-center gap-1 cursor-pointer bg-transparent border-0"
        >
          <span>0000003100084920194820</span>
          <span class="material-symbols-outlined text-[14px]">content_copy</span>
        </button>
      </div>

      <div class="flex justify-between items-center">
        <span class="text-[#94a3b8]">Razón Social:</span>
        <span class="text-[#cbd5e1]">Hibana.tk Entertainment S.A. (30-71829104-9)</span>
      </div>
    </div>

    <div v-if="copiedNotice" class="text-xs font-mono text-[#4fdbc8] text-center font-bold">
      {{ copiedNotice }}
    </div>
  </div>
</template>

<style scoped>
.card-level-1 {
  background-color: #141c27;
  border: 1px solid #23354d;
}
</style>
