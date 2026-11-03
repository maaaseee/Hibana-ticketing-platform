<template>
  <div class="space-y-6">
    <div class="border-b border-border-subtle pb-4">
      <h2 class="text-xl font-display font-bold text-high-emphasis">
        Paso 5: Resumen, Proyección & Publicación
      </h2>
      <p class="text-xs text-muted-light mt-1">
        Auditoría final de parámetros antes de consolidar el evento en el Catálogo general.
      </p>
    </div>

    <!-- Financial Projection Card -->
    <div class="p-6 rounded-3xl bg-gradient-to-br from-surface-card to-surface-container-deep border border-border-subtle shadow-xl space-y-4">
      <div class="flex items-center justify-between">
        <span class="text-xs font-mono uppercase tracking-wider text-muted-light">
          Proyección de Recaudación Bruta Estimada
        </span>
        <span class="text-xs font-mono text-secondary px-2.5 py-0.5 rounded-full bg-secondary/10 border border-secondary/20">
          Sold Out (100% Ocupación)
        </span>
      </div>

      <div class="grid grid-cols-1 md:grid-cols-3 gap-6 pt-2">
        <div>
          <span class="text-[11px] font-mono text-muted block uppercase">Taquilla Neta Total</span>
          <div class="text-2xl font-black font-display text-high-emphasis mt-0.5">
            ${{ wizardStore.grossRevenueProjection.toLocaleString() }}
            <span class="text-xs font-mono text-muted font-normal">ARS</span>
          </div>
        </div>

        <div>
          <span class="text-[11px] font-mono text-muted block uppercase">Tarifas de Servicio (Service Fee)</span>
          <div class="text-2xl font-black font-display text-primary mt-0.5">
            ${{ wizardStore.estimatedServiceFeeTotal.toLocaleString() }}
            <span class="text-xs font-mono text-muted font-normal">ARS</span>
          </div>
        </div>

        <div>
          <span class="text-[11px] font-mono text-muted block uppercase">Capacidad Puesta a la Venta</span>
          <div class="text-2xl font-black font-display text-secondary mt-0.5">
            {{ wizardStore.totalCapacity.toLocaleString() }}
            <span class="text-xs font-mono text-muted font-normal">tickets</span>
          </div>
        </div>
      </div>
    </div>

    <!-- Pre-launch Checklist -->
    <div class="p-5 rounded-2xl bg-surface-container-deep border border-border-subtle space-y-3">
      <h3 class="text-xs font-mono uppercase tracking-wider text-muted-light">
        Checklist de Validación Operativa
      </h3>

      <div class="grid grid-cols-1 md:grid-cols-2 gap-3 text-xs font-mono">
        <div class="flex items-center gap-2">
          <span
            class="material-symbols-outlined text-base"
            :class="wizardStore.isStep1Valid ? 'text-secondary' : 'text-error'"
          >
            {{ wizardStore.isStep1Valid ? 'check_circle' : 'cancel' }}
          </span>
          <span :class="wizardStore.isStep1Valid ? 'text-on-surface' : 'text-error'">
            Datos generales y recinto asignado
          </span>
        </div>

        <div class="flex items-center gap-2">
          <span
            class="material-symbols-outlined text-base"
            :class="wizardStore.isStep2Valid ? 'text-secondary' : 'text-error'"
          >
            {{ wizardStore.isStep2Valid ? 'check_circle' : 'cancel' }}
          </span>
          <span :class="wizardStore.isStep2Valid ? 'text-on-surface' : 'text-error'">
            Cronograma de horarios coherente
          </span>
        </div>

        <div class="flex items-center gap-2">
          <span
            class="material-symbols-outlined text-base"
            :class="wizardStore.isStep3Valid ? 'text-secondary' : 'text-error'"
          >
            {{ wizardStore.isStep3Valid ? 'check_circle' : 'cancel' }}
          </span>
          <span :class="wizardStore.isStep3Valid ? 'text-on-surface' : 'text-error'">
            Sectores y precios parametrizados ({{ wizardStore.sectors.length }})
          </span>
        </div>

        <div class="flex items-center gap-2">
          <span
            class="material-symbols-outlined text-base"
            :class="wizardStore.isStep4Valid ? 'text-secondary' : 'text-error'"
          >
            {{ wizardStore.isStep4Valid ? 'check_circle' : 'cancel' }}
          </span>
          <span :class="wizardStore.isStep4Valid ? 'text-on-surface' : 'text-error'">
            Políticas de compra y QR dinámico
          </span>
        </div>
      </div>
    </div>

    <!-- Error Banner if any -->
    <div v-if="errorMessage" class="p-4 rounded-xl bg-error/10 border border-error/30 text-error text-xs font-mono flex items-center gap-2">
      <span class="material-symbols-outlined text-base shrink-0">error</span>
      <span>{{ errorMessage }}</span>
    </div>

    <!-- Action Buttons -->
    <div class="pt-4 border-t border-border-subtle flex flex-col sm:flex-row items-center justify-between gap-4">
      <button
        type="button"
        :disabled="wizardStore.isSubmitting"
        @click="submitAsDraft"
        class="w-full sm:w-auto px-5 py-3 rounded-xl bg-surface-card hover:bg-surface-elevated text-on-surface font-semibold text-xs border border-border-subtle transition flex items-center justify-center gap-2"
      >
        <span class="material-symbols-outlined text-sm">save</span>
        Guardar como Borrador (DRAFT)
      </button>

      <button
        type="button"
        :disabled="!wizardStore.canPublish || wizardStore.isSubmitting"
        @click="submitAndPublish"
        class="w-full sm:w-auto px-6 py-3 rounded-xl bg-primary text-on-primary hover:bg-primary-hover font-bold text-xs shadow-lg shadow-primary/25 disabled:opacity-50 disabled:cursor-not-allowed transition flex items-center justify-center gap-2"
      >
        <span v-if="wizardStore.isSubmitting" class="material-symbols-outlined text-sm animate-spin">refresh</span>
        <span v-else class="material-symbols-outlined text-sm">rocket_launch</span>
        Publicar a Venta General
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useEventWizardStore } from '@/stores/eventWizard'
import { eventAdminService } from '@/services/eventAdminService'

const router = useRouter()
const wizardStore = useEventWizardStore()
const errorMessage = ref<string | null>(null)

async function submitAsDraft() {
  wizardStore.isSubmitting = true
  errorMessage.value = null
  try {
    const payload = wizardStore.buildPayload()
    await eventAdminService.createEvent(payload)
    wizardStore.reset()
    router.push('/events')
  } catch (err: any) {
    errorMessage.value = err.problem?.detail || err.message || 'Error al guardar el borrador del evento.'
  } finally {
    wizardStore.isSubmitting = false
  }
}

async function submitAndPublish() {
  wizardStore.isSubmitting = true
  errorMessage.value = null
  try {
    const payload = wizardStore.buildPayload()
    const created = await eventAdminService.createEvent(payload)
    await eventAdminService.publishEvent(created.id)
    wizardStore.reset()
    router.push('/events')
  } catch (err: any) {
    errorMessage.value = err.problem?.detail || err.message || 'Error al crear y publicar el evento.'
  } finally {
    wizardStore.isSubmitting = false
  }
}
</script>
