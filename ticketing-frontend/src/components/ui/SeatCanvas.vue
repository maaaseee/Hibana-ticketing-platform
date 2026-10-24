<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import type { Seat } from '../../types'

const props = defineProps<{
  seats: Seat[]
  selectedSeats: Seat[]
}>()

const emit = defineEmits<{
  (e: 'select-seat', seatId: string): void
}>()

const canvasRef = ref<HTMLCanvasElement | null>(null)
const hoveredSeat = ref<Seat | null>(null)
const zoomLevel = ref<number>(1)
const panX = ref<number>(0)
const panY = ref<number>(0)

function render() {
  const canvas = canvasRef.value
  if (!canvas) return
  const ctx = canvas.getContext('2d')
  if (!ctx) return

  ctx.clearRect(0, 0, canvas.width, canvas.height)

  ctx.save()
  ctx.translate(panX.value, panY.value)
  ctx.scale(zoomLevel.value, zoomLevel.value)

  // Stage visualization
  ctx.fillStyle = '#141e2e'
  ctx.strokeStyle = '#23354d'
  ctx.lineWidth = 2
  ctx.beginPath()
  ctx.roundRect(120, 20, 480, 40, 8)
  ctx.fill()
  ctx.stroke()

  ctx.fillStyle = '#86cffb'
  ctx.font = '600 13px "Space Grotesk", sans-serif'
  ctx.textAlign = 'center'
  ctx.fillText('ESCENARIO PRINCIPAL / STAGE', 360, 45)

  // Sub-arc light
  ctx.strokeStyle = 'rgba(56, 189, 248, 0.25)'
  ctx.beginPath()
  ctx.arc(360, 60, 160, 0, Math.PI, false)
  ctx.stroke()

  // Render seats
  props.seats.forEach(seat => {
    const isSelected = props.selectedSeats.some(s => s.id === seat.id)
    const isHovered = hoveredSeat.value?.id === seat.id

    ctx.save()

    if (isSelected) {
      // Glow effect
      ctx.shadowColor = 'rgba(56, 189, 248, 0.6)'
      ctx.shadowBlur = 10
      ctx.fillStyle = '#38bdf8'
      ctx.strokeStyle = '#ffffff'
      ctx.lineWidth = 2
    } else if (seat.status === 'RESERVED') {
      ctx.fillStyle = '#101825'
      ctx.strokeStyle = '#1b2533'
      ctx.lineWidth = 1
    } else {
      ctx.fillStyle = isHovered ? '#1e3a5f' : '#141e2e'
      ctx.strokeStyle = isHovered ? '#86cffb' : '#23354d'
      ctx.lineWidth = 1.5
    }

    ctx.beginPath()
    ctx.arc(seat.x, seat.y + 40, 14, 0, Math.PI * 2)
    ctx.fill()
    ctx.stroke()

    // Seat number text
    ctx.fillStyle = isSelected ? '#00344a' : (seat.status === 'RESERVED' ? '#40484e' : '#cbd5e1')
    ctx.font = '600 10px "JetBrains Mono", monospace'
    ctx.textAlign = 'center'
    ctx.textBaseline = 'middle'
    ctx.fillText(`${seat.number}`, seat.x, seat.y + 40)

    ctx.restore()
  })

  // Row labels on left and right
  ctx.fillStyle = '#899299'
  ctx.font = '600 12px "JetBrains Mono", monospace'
  const rows = ['A', 'B', 'C', 'D', 'E', 'F']
  rows.forEach((row, idx) => {
    const y = 70 + idx * 48 + 40
    ctx.textAlign = 'right'
    ctx.fillText(`Fila ${row}`, 80, y)
    ctx.textAlign = 'left'
    ctx.fillText(`Fila ${row}`, 660, y)
  })

  ctx.restore()
}

function handleMouseMove(e: MouseEvent) {
  const canvas = canvasRef.value
  if (!canvas) return
  const rect = canvas.getBoundingClientRect()
  const mouseX = (e.clientX - rect.left - panX.value) / zoomLevel.value
  const mouseY = (e.clientY - rect.top - panY.value) / zoomLevel.value

  const found = props.seats.find(s => {
    const dx = mouseX - s.x
    const dy = mouseY - (s.y + 40)
    return Math.sqrt(dx * dx + dy * dy) <= 14
  })

  if (found !== hoveredSeat.value) {
    hoveredSeat.value = found || null
    canvas.style.cursor = found && found.status !== 'RESERVED' ? 'pointer' : 'default'
    render()
  }
}

function handleClick(e: MouseEvent) {
  const canvas = canvasRef.value
  if (!canvas) return
  const rect = canvas.getBoundingClientRect()
  const mouseX = (e.clientX - rect.left - panX.value) / zoomLevel.value
  const mouseY = (e.clientY - rect.top - panY.value) / zoomLevel.value

  const found = props.seats.find(s => {
    const dx = mouseX - s.x
    const dy = mouseY - (s.y + 40)
    return Math.sqrt(dx * dx + dy * dy) <= 14
  })

  if (found && found.status !== 'RESERVED') {
    emit('select-seat', found.id)
  }
}

function zoomIn() {
  zoomLevel.value = Math.min(2, zoomLevel.value + 0.15)
  render()
}

function zoomOut() {
  zoomLevel.value = Math.max(0.6, zoomLevel.value - 0.15)
  render()
}

function resetView() {
  zoomLevel.value = 1
  panX.value = 0
  panY.value = 0
  render()
}

onMounted(() => {
  render()
})

watch(() => [props.seats, props.selectedSeats], () => {
  render()
}, { deep: true })
</script>

<template>
  <div class="relative w-full h-130 bg-surface rounded-xl overflow-hidden border border-border-subtle flex items-center justify-center">
    <canvas
      ref="canvasRef"
      width="740"
      height="480"
      class="w-full h-full max-w-185 max-h-120"
      role="img"
      aria-label="Mapa interactivo de butacas del recinto"
      @mousemove="handleMouseMove"
      @click="handleClick"
    ></canvas>

    <!-- Controls Toolbar -->
    <div class="absolute bottom-4 right-4 flex flex-col bg-surface-container rounded-lg p-1 border border-border-subtle z-10 shadow-lg">
      <button
        @click="zoomIn"
        class="w-9 h-9 flex items-center justify-center text-on-surface-variant hover:text-on-surface hover:bg-surface-container-high rounded transition-colors"
        title="Acercar mapa"
        aria-label="Acercar mapa"
      >
        <span class="material-symbols-outlined text-[20px]">zoom_in</span>
      </button>
      <button
        @click="zoomOut"
        class="w-9 h-9 flex items-center justify-center text-on-surface-variant hover:text-on-surface hover:bg-surface-container-high rounded transition-colors"
        title="Alejar mapa"
        aria-label="Alejar mapa"
      >
        <span class="material-symbols-outlined text-[20px]">zoom_out</span>
      </button>
      <button
        @click="resetView"
        class="w-9 h-9 flex items-center justify-center text-on-surface-variant hover:text-on-surface hover:bg-surface-container-high rounded transition-colors"
        title="Restablecer vista"
        aria-label="Restablecer vista"
      >
        <span class="material-symbols-outlined text-[18px]">center_focus_strong</span>
      </button>
    </div>

    <!-- Seat Tooltip overlay -->
    <div
      v-if="hoveredSeat"
      class="absolute top-4 left-4 bg-surface-container-high/90 backdrop-blur-md px-space-md py-2 rounded-lg border border-border-subtle text-xs pointer-events-none shadow-xl flex items-center gap-space-md"
    >
      <div>
        <span class="text-outline uppercase font-mono tracking-wider text-[10px]">Butaca</span>
        <p class="font-headline font-bold text-on-surface text-sm">Fila {{ hoveredSeat.row }} • Asiento {{ hoveredSeat.number }}</p>
      </div>
      <div class="border-l border-border-subtle pl-space-md">
        <span class="text-outline uppercase font-mono tracking-wider text-[10px]">Precio</span>
        <p class="font-mono font-bold text-primary text-sm">${{ hoveredSeat.price.toLocaleString('es-AR') }} ARS</p>
      </div>
      <div class="border-l border-border-subtle pl-space-md">
        <span class="text-outline uppercase font-mono tracking-wider text-[10px]">Estado</span>
        <p class="font-mono text-xs" :class="hoveredSeat.status === 'RESERVED' ? 'text-error' : 'text-secondary'">
          {{ hoveredSeat.status === 'RESERVED' ? 'Ocupado' : 'Disponible' }}
        </p>
      </div>
    </div>
  </div>
</template>
