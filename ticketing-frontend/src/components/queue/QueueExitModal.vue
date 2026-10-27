<script setup lang="ts">
import {
  DialogRoot,
  DialogPortal,
  DialogOverlay,
  DialogContent,
  DialogTitle,
  DialogDescription
} from 'radix-vue'

defineProps<{
  currentPosition: number
}>()

const isOpen = defineModel<boolean>('open', { required: true })

const emit = defineEmits<{
  (e: 'confirm'): void
}>()
</script>

<template>
  <DialogRoot v-model:open="isOpen">
    <DialogPortal>
      <DialogOverlay class="fixed inset-0 z-50 bg-black/80 backdrop-blur-xs animate-fadeIn" />
      <DialogContent class="fixed top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 z-50 bg-surface-card border border-border-subtle rounded-2xl max-w-md w-[92vw] p-6 space-y-4 shadow-2xl outline-none">
        
        <div class="flex items-center gap-3 pb-2 border-b border-border-subtle">
          <div class="w-10 h-10 rounded-xl bg-amber-950/80 border border-amber-500/40 flex items-center justify-center text-amber-400 shrink-0">
            <span class="material-symbols-outlined text-2xl">warning</span>
          </div>
          <DialogTitle class="text-lg font-bold text-white font-display">
            ¿Deseas salir de la fila virtual?
          </DialogTitle>
        </div>

        <DialogDescription class="text-xs text-muted-light font-mono leading-relaxed">
          Actualmente te encuentras en la posición <strong class="text-white">#{{ currentPosition.toLocaleString('es-AR') }}</strong>. Si sales al home ahora, <span class="text-rose-400 font-bold">perderás tu lugar en la fila</span> y tendrás que volver a ingresar desde el final si decides volver.
        </DialogDescription>

        <div class="flex items-center justify-end gap-3 pt-3 font-mono text-xs">
          <button
            @click="isOpen = false"
            class="px-4 py-2.5 rounded-xl bg-surface-elevated hover:bg-surface-interactive text-white border border-border-subtle cursor-pointer transition-colors font-semibold"
          >
            Continuar en la fila
          </button>
          <button
            @click="emit('confirm')"
            class="px-4 py-2.5 rounded-xl bg-rose-700 hover:bg-rose-600 text-white font-bold cursor-pointer transition-colors shadow-md"
            data-testid="confirm-exit-queue-btn"
          >
            Salir y perder mi lugar
          </button>
        </div>
      </DialogContent>
    </DialogPortal>
  </DialogRoot>
</template>
