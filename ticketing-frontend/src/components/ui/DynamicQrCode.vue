<script setup lang="ts">
defineProps<{
  ticketId?: string
  payload?: {
    tid?: string
    eid?: string
    vid?: string
    sec?: string
    sig?: string
  }
}>()
</script>

<template>
  <div class="flex flex-col items-center p-3 bg-white rounded-2xl shadow-inner border border-border-subtle">
    <svg
      class="w-44 h-44 object-contain text-slate-950"
      viewBox="0 0 160 160"
      fill="currentColor"
      xmlns="http://www.w3.org/2000/svg"
      role="img"
      aria-label="Código QR de Acceso"
    >
      <!-- Background -->
      <rect width="160" height="160" fill="white" />

      <!-- Position Detection Pattern: Top-Left (7x7 modules) -->
      <rect x="10" y="10" width="42" height="42" fill="#070f19" />
      <rect x="16" y="16" width="30" height="30" fill="white" />
      <rect x="22" y="22" width="18" height="18" fill="#070f19" />

      <!-- Position Detection Pattern: Top-Right -->
      <rect x="108" y="10" width="42" height="42" fill="#070f19" />
      <rect x="114" y="16" width="30" height="30" fill="white" />
      <rect x="120" y="22" width="18" height="18" fill="#070f19" />

      <!-- Position Detection Pattern: Bottom-Left -->
      <rect x="10" y="108" width="42" height="42" fill="#070f19" />
      <rect x="16" y="114" width="30" height="30" fill="white" />
      <rect x="22" y="120" width="18" height="18" fill="#070f19" />

      <!-- Timing patterns and alignment -->
      <g fill="#070f19">
        <!-- Horizontal timing -->
        <rect x="58" y="28" width="6" height="6" />
        <rect x="70" y="28" width="6" height="6" />
        <rect x="82" y="28" width="6" height="6" />
        <rect x="94" y="28" width="6" height="6" />

        <!-- Vertical timing -->
        <rect x="28" y="58" width="6" height="6" />
        <rect x="28" y="70" width="6" height="6" />
        <rect x="28" y="82" width="6" height="6" />
        <rect x="28" y="94" width="6" height="6" />

        <!-- Alignment pattern bottom-right -->
        <rect x="108" y="108" width="30" height="30" />
        <rect x="114" y="114" width="18" height="18" fill="white" />
        <rect x="120" y="120" width="6" height="6" fill="#070f19" />

        <!-- Standard QR data pattern modules -->
        <rect x="58" y="10" width="6" height="12" />
        <rect x="70" y="10" width="18" height="6" />
        <rect x="94" y="10" width="6" height="12" />
        <rect x="58" y="40" width="12" height="6" />
        <rect x="76" y="34" width="6" height="18" />
        <rect x="88" y="40" width="12" height="6" />

        <rect x="10" y="58" width="12" height="6" />
        <rect x="10" y="70" width="6" height="18" />
        <rect x="10" y="94" width="12" height="6" />
        <rect x="40" y="58" width="6" height="12" />
        <rect x="34" y="76" width="18" height="6" />
        <rect x="40" y="88" width="6" height="12" />

        <!-- Central payload grid -->
        <rect x="58" y="58" width="12" height="12" />
        <rect x="76" y="58" width="6" height="6" />
        <rect x="88" y="58" width="12" height="6" />
        <rect x="106" y="58" width="6" height="18" />
        <rect x="118" y="58" width="12" height="6" />
        <rect x="136" y="58" width="14" height="6" />

        <rect x="58" y="76" width="6" height="18" />
        <rect x="70" y="70" width="12" height="6" />
        <rect x="70" y="82" width="6" height="12" />
        <rect x="82" y="76" width="18" height="6" />
        <rect x="88" y="88" width="6" height="12" />
        <rect x="100" y="82" width="12" height="6" />
        <rect x="118" y="70" width="6" height="18" />
        <rect x="130" y="76" width="12" height="6" />

        <rect x="58" y="100" width="18" height="6" />
        <rect x="70" y="112" width="6" height="12" />
        <rect x="82" y="106" width="12" height="6" />
        <rect x="82" y="118" width="6" height="12" />
        <rect x="94" y="112" width="18" height="6" />
        <rect x="94" y="124" width="6" height="12" />

        <!-- Bottom edge modules -->
        <rect x="58" y="130" width="12" height="6" />
        <rect x="58" y="142" width="6" height="8" />
        <rect x="76" y="136" width="18" height="6" />
        <rect x="82" y="148" width="12" height="6" />
        <rect x="100" y="142" width="18" height="8" />
        <rect x="124" y="142" width="6" height="8" />
        <rect x="136" y="142" width="14" height="8" />

        <!-- Right edge modules -->
        <rect x="144" y="10" width="6" height="18" />
        <rect x="144" y="34" width="6" height="18" />
        <rect x="144" y="76" width="6" height="24" />
        <rect x="144" y="106" width="6" height="12" />
        <rect x="144" y="124" width="6" height="12" />
      </g>
    </svg>
  </div>
</template>
