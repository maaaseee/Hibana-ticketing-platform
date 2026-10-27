<script setup lang="ts">
// Progress Stepper for 3-step checkout
</script>

<template>
  <div class="col-span-1 lg:col-span-12 mb-2">
    <div class="flex items-center justify-between max-w-2xl mx-auto">
      <!-- Step 1: Selección (Completed) -->
      <div class="flex flex-col items-center gap-1.5">
        <div class="w-8 h-8 rounded-full bg-[#1b293c] border border-[#23354d] text-[#86cffb] flex items-center justify-center font-mono text-xs font-bold shadow-xs">
          <span class="material-symbols-outlined text-[16px]">check</span>
        </div>
        <span class="font-mono text-xs text-[#94a3b8]">Selección</span>
      </div>

      <div class="flex-grow h-[1px] bg-[#23354d] mx-3 sm:mx-6"></div>

      <!-- Step 2: Pago (Active) -->
      <div class="flex flex-col items-center gap-1.5">
        <div class="w-8 h-8 rounded-full bg-[#00688f] text-white flex items-center justify-center font-mono text-xs font-bold ring-2 ring-[#86cffb]/30 shadow-md shadow-[#00688f]/30">
          2
        </div>
        <span class="font-mono text-xs text-[#86cffb] font-bold">Pago</span>
      </div>

      <div class="flex-grow h-[1px] bg-[#23354d] mx-3 sm:mx-6"></div>

      <!-- Step 3: Confirmación (Pending) -->
      <div class="flex flex-col items-center gap-1.5">
        <div class="w-8 h-8 rounded-full bg-[#141e2e] border border-[#23354d] text-[#94a3b8] flex items-center justify-center font-mono text-xs">
          3
        </div>
        <span class="font-mono text-xs text-[#94a3b8]">Confirmación</span>
      </div>
    </div>
  </div>
</template>
