<template>
  <div v-if="isOpen" class="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/75 backdrop-blur-sm">
    <div
      class="w-full max-w-md bg-surface-container border border-border-subtle rounded-2xl p-6 shadow-2xl relative overflow-hidden"
    >
      <div class="flex items-center gap-3 mb-4">
        <div class="w-10 h-10 rounded-xl bg-error/10 border border-error/20 flex items-center justify-center text-error">
          <span class="material-symbols-outlined">warning</span>
        </div>
        <div>
          <h3 class="font-display font-bold text-lg text-high-emphasis">{{ title }}</h3>
          <p class="text-xs text-muted-light">{{ subtitle }}</p>
        </div>
      </div>

      <p class="text-sm text-on-surface mb-4 leading-relaxed">
        {{ message }}
      </p>

      <div v-if="requiresReason" class="mb-5">
        <label class="block text-xs font-mono uppercase tracking-wider text-muted-light mb-1.5">
          Motivo Obligatorio de Cancelación
        </label>
        <textarea
          v-model="reasonText"
          rows="3"
          placeholder="Ej: Suspensión por motivos de fuerza mayor y alerta meteorológica oficial..."
          class="w-full bg-surface-container-deep border border-border-subtle rounded-xl px-3.5 py-2.5 text-sm text-high-emphasis placeholder-muted focus:outline-none focus:border-error focus:ring-1 focus:ring-error transition"
        ></textarea>
      </div>

      <div class="flex items-center justify-end gap-3">
        <button
          type="button"
          class="px-4 py-2 rounded-xl text-sm font-medium text-muted-light hover:text-high-emphasis hover:bg-surface-elevated transition"
          @click="$emit('cancel')"
        >
          Cancelar
        </button>
        <button
          type="button"
          :disabled="requiresReason && !reasonText.trim()"
          class="px-4 py-2 rounded-xl text-sm font-semibold bg-error text-on-error hover:bg-error/90 disabled:opacity-50 disabled:cursor-not-allowed transition flex items-center gap-1.5"
          @click="onConfirm"
        >
          <span class="material-symbols-outlined text-base">check</span>
          {{ confirmText }}
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'

const props = withDefaults(
  defineProps<{
    isOpen: boolean
    title: string
    subtitle?: string
    message: string
    confirmText?: string
    requiresReason?: boolean
  }>(),
  {
    subtitle: 'Acción crítica de auditoría',
    confirmText: 'Confirmar',
    requiresReason: false,
  }
)

const emit = defineEmits<{
  (e: 'confirm', reason?: string): void
  (e: 'cancel'): void
}>()

const reasonText = ref('')

function onConfirm() {
  emit('confirm', props.requiresReason ? reasonText.value : undefined)
  reasonText.value = ''
}
</script>
