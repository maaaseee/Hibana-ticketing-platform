<script setup lang="ts">
import {
  ToastProvider,
  ToastRoot,
  ToastTitle,
  ToastDescription,
  ToastClose,
  ToastViewport
} from 'radix-vue'
import { useToastStore, type ToastType } from '../../stores/toast'

const toastStore = useToastStore()

function getIcon(type: ToastType): string {
  switch (type) {
    case 'success':
      return 'check_circle'
    case 'error':
      return 'error'
    case 'warning':
      return 'warning'
    case 'info':
    default:
      return 'info'
  }
}

function getStyles(type: ToastType): { border: string; glow: string; text: string; icon: string } {
  switch (type) {
    case 'success':
      return {
        border: 'border-emerald-500/50',
        glow: 'shadow-[0_10px_30px_rgba(16,185,129,0.25)]',
        text: 'text-emerald-300',
        icon: 'text-emerald-400'
      }
    case 'error':
      return {
        border: 'border-rose-500/60',
        glow: 'shadow-[0_10px_30px_rgba(225,29,72,0.3)]',
        text: 'text-rose-300',
        icon: 'text-rose-400'
      }
    case 'warning':
      return {
        border: 'border-amber-500/60',
        glow: 'shadow-[0_10px_30px_rgba(245,158,11,0.25)]',
        text: 'text-amber-300',
        icon: 'text-amber-400'
      }
    case 'info':
    default:
      return {
        border: 'border-[#0084ff]/50',
        glow: 'shadow-[0_10px_30px_rgba(0,132,255,0.25)]',
        text: 'text-[#38bdf8]',
        icon: 'text-[#38bdf8]'
      }
  }
}
</script>

<template>
  <ToastProvider swipe-direction="right" :duration="5000">
    <ToastRoot
      v-for="toast in toastStore.toasts"
      :key="toast.id"
      :open="true"
      @update:open="(val: boolean) => !val && toastStore.removeToast(toast.id)"
      class="pointer-events-auto relative flex items-start gap-3 w-full max-w-sm p-4 rounded-2xl bg-[#0c1524]/95 backdrop-blur-xl border text-white transition-all duration-300 shadow-2xl"
      :class="[getStyles(toast.type).border, getStyles(toast.type).glow]"
    >
      <!-- Severity Icon -->
      <span
        class="material-symbols-outlined text-[20px] shrink-0 mt-0.5"
        :class="getStyles(toast.type).icon"
      >
        {{ getIcon(toast.type) }}
      </span>

      <!-- Content -->
      <div class="flex-1 min-w-0 pr-2">
        <ToastTitle class="font-display font-bold text-sm tracking-wide text-white leading-snug">
          {{ toast.title }}
        </ToastTitle>
        <ToastDescription
          v-if="toast.description"
          class="font-body text-xs text-[#94a3b8] mt-1 leading-relaxed"
        >
          {{ toast.description }}
        </ToastDescription>

        <button
          v-if="toast.action"
          type="button"
          @click="toast.action.onClick"
          class="mt-2 text-xs font-display font-bold text-[#38bdf8] hover:underline"
        >
          {{ toast.action.label }}
        </button>
      </div>

      <!-- Close Button -->
      <ToastClose
        class="text-[#64748b] hover:text-white transition-colors cursor-pointer p-0.5 rounded-lg"
        aria-label="Cerrar notificación"
      >
        <span class="material-symbols-outlined text-[18px]">close</span>
      </ToastClose>
    </ToastRoot>

    <!-- Fixed Viewport in Bottom-Right -->
    <ToastViewport
      class="fixed bottom-0 right-0 z-[99999] p-4 sm:p-6 flex flex-col gap-2.5 w-full max-w-md pointer-events-none outline-none"
    />
  </ToastProvider>
</template>
