<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'

const props = defineProps<{
  ticketId: string
  payload: {
    tid: string
    eid: string
    vid: string
    sec: string
    sig: string
  }
}>()

const canvasRef = ref<HTMLCanvasElement | null>(null)
const secondsLeft = ref<number>(15)
let timer: any = null

function drawQr() {
  const canvas = canvasRef.value
  if (!canvas) return
  const ctx = canvas.getContext('2d')
  if (!ctx) return

  const size = 180
  ctx.clearRect(0, 0, size, size)

  // Draw background
  ctx.fillStyle = '#ffffff'
  ctx.fillRect(0, 0, size, size)

  // Draw simulated QR matrix with corner targets
  ctx.fillStyle = '#070f19'
  
  // Outer corner squares
  const drawTarget = (x: number, y: number) => {
    ctx.fillRect(x, y, 42, 42)
    ctx.fillStyle = '#ffffff'
    ctx.fillRect(x + 6, y + 6, 30, 30)
    ctx.fillStyle = '#070f19'
    ctx.fillRect(x + 12, y + 12, 18, 18)
  }

  drawTarget(12, 12)
  drawTarget(126, 12)
  drawTarget(12, 126)

  // Data dots based on hash of props.payload.tid + secondsLeft
  const seed = props.payload.tid.charCodeAt(props.payload.tid.length - 1) + secondsLeft.value
  for (let r = 0; r < 14; r++) {
    for (let c = 0; c < 14; c++) {
      const isCorner = (r < 4 && c < 4) || (r < 4 && c > 9) || (r > 9 && c < 4)
      if (!isCorner) {
        if ((r * c + seed + r + c) % 3 === 0 || (r + c + seed) % 5 === 0) {
          ctx.fillRect(20 + c * 10, 20 + r * 10, 8, 8)
        }
      }
    }
  }
}

onMounted(() => {
  drawQr()
  timer = setInterval(() => {
    if (secondsLeft.value > 1) {
      secondsLeft.value--
    } else {
      secondsLeft.value = 15
    }
    drawQr()
  }, 1000)
})

onUnmounted(() => {
  if (timer) clearInterval(timer)
})
</script>

<template>
  <div class="flex flex-col items-center gap-space-sm p-space-md bg-surface-container-low rounded-xl border border-border-subtle">
    <div class="relative p-2 bg-white rounded-lg shadow-inner flex items-center justify-center">
      <canvas ref="canvasRef" width="180" height="180" class="rounded"></canvas>
    </div>

    <!-- Dynamic TOTP Rotating Seed indicator -->
    <div class="flex items-center gap-2 font-mono text-xs text-on-surface-variant">
      <div class="relative w-4 h-4 flex items-center justify-center">
        <svg class="w-4 h-4 transform -rotate-90" viewBox="0 0 36 36">
          <path
            class="text-surface-container-highest stroke-current"
            stroke-width="4"
            fill="none"
            d="M18 2.0845 a 15.9155 15.9155 0 0 1 0 31.831 a 15.9155 15.9155 0 0 1 0 -31.831"
          />
          <path
            class="text-secondary stroke-current"
            :stroke-dasharray="`${(secondsLeft / 15) * 100}, 100`"
            stroke-width="4"
            stroke-linecap="round"
            fill="none"
            d="M18 2.0845 a 15.9155 15.9155 0 0 1 0 31.831 a 15.9155 15.9155 0 0 1 0 -31.831"
          />
        </svg>
      </div>
      <span>Rotación anti-captura en: <strong class="text-secondary font-bold">{{ secondsLeft }}s</strong></span>
    </div>

    <div class="text-[11px] font-mono text-outline uppercase tracking-wider flex items-center gap-1">
      <span class="material-symbols-outlined text-[14px] text-secondary">verified</span>
      <span>Token Criptográfico Ed25519</span>
    </div>
  </div>
</template>
