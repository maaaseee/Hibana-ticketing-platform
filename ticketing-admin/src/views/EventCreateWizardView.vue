<template>
  <div class="space-y-6">
    <!-- Breadcrumb & Top Bar -->
    <div class="flex items-center justify-between pb-2 border-b border-border-subtle">
      <div class="flex items-center gap-2 text-xs font-mono text-muted-light">
        <RouterLink to="/events" class="hover:text-high-emphasis transition flex items-center gap-1">
          <span class="material-symbols-outlined text-sm">arrow_back</span>
          Eventos
        </RouterLink>
        <span>/</span>
        <span class="text-primary font-semibold">Nuevo Espectáculo</span>
      </div>

      <button
        type="button"
        @click="wizardStore.reset"
        class="text-xs font-mono text-muted hover:text-error transition flex items-center gap-1"
      >
        <span class="material-symbols-outlined text-sm">restart_alt</span>
        Reiniciar Formulario
      </button>
    </div>

    <!-- Stepper Navigation Bar -->
    <div class="bg-surface-container border border-border-subtle rounded-2xl p-3 overflow-x-auto">
      <div class="flex items-center justify-between min-w-[600px] gap-2">
        <button
          v-for="step in steps"
          :key="step.number"
          type="button"
          @click="goToStep(step.number)"
          :class="[
            'flex items-center gap-2.5 px-3.5 py-2 rounded-xl text-xs font-mono transition text-left shrink-0',
            wizardStore.currentStep === step.number
              ? 'bg-surface-elevated text-primary border border-primary/30 font-bold shadow-sm'
              : wizardStore.currentStep > step.number
                ? 'text-secondary hover:bg-surface-container-deep'
                : 'text-muted hover:text-on-surface-variant hover:bg-surface-container-deep'
          ]"
        >
          <span
            :class="[
              'w-6 h-6 rounded-lg text-[11px] font-bold flex items-center justify-center shrink-0',
              wizardStore.currentStep === step.number
                ? 'bg-primary text-on-primary'
                : wizardStore.currentStep > step.number
                  ? 'bg-secondary/20 text-secondary'
                  : 'bg-surface-card text-muted'
            ]"
          >
            <span v-if="wizardStore.currentStep > step.number" class="material-symbols-outlined text-sm">check</span>
            <span v-else>{{ step.number }}</span>
          </span>
          <span>{{ step.title }}</span>
        </button>
      </div>
    </div>

    <!-- 2-Column Responsive Layout -->
    <div class="grid grid-cols-1 lg:grid-cols-12 gap-8 items-start">
      <!-- Left Column: Current Wizard Step (8 cols) -->
      <div class="lg:col-span-8 bg-surface-container border border-border-subtle rounded-3xl p-6 md:p-8 shadow-sm">
        <Step1GeneralInfo v-if="wizardStore.currentStep === 1" :venues="venues" />
        <Step2DatesSchedule v-else-if="wizardStore.currentStep === 2" />
        <Step3PricingSectors v-else-if="wizardStore.currentStep === 3" :venues="venues" />
        <Step4PoliciesAccess v-else-if="wizardStore.currentStep === 4" />
        <Step5ReviewPublish v-else-if="wizardStore.currentStep === 5" />

        <!-- Navigation Buttons (Steps 1 to 4) -->
        <div
          v-if="wizardStore.currentStep < 5"
          class="mt-8 pt-6 border-t border-border-subtle flex items-center justify-between"
        >
          <button
            type="button"
            :disabled="wizardStore.currentStep === 1"
            @click="prevStep"
            class="px-4 py-2.5 rounded-xl bg-surface-card hover:bg-surface-elevated text-xs font-semibold text-muted-light hover:text-high-emphasis disabled:opacity-40 disabled:cursor-not-allowed transition flex items-center gap-1.5"
          >
            <span class="material-symbols-outlined text-sm">arrow_back</span>
            Paso Anterior
          </button>

          <button
            type="button"
            @click="nextStep"
            class="px-5 py-2.5 rounded-xl bg-primary text-on-primary hover:bg-primary-hover text-xs font-bold transition flex items-center gap-1.5 shadow-md shadow-primary/20"
          >
            <span>Continuar al Paso {{ wizardStore.currentStep + 1 }}</span>
            <span class="material-symbols-outlined text-sm">arrow_forward</span>
          </button>
        </div>
      </div>

      <!-- Right Column: Sticky Live B2C Catalog Preview (4 cols) -->
      <div class="lg:col-span-4">
        <LiveCatalogPreview :venues="venues" />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { RouterLink } from 'vue-router'
import { useEventWizardStore } from '@/stores/eventWizard'
import { venueService } from '@/services/venueService'
import type { VenueResponse } from '@/types/venue'

import Step1GeneralInfo from '@/components/wizard/Step1GeneralInfo.vue'
import Step2DatesSchedule from '@/components/wizard/Step2DatesSchedule.vue'
import Step3PricingSectors from '@/components/wizard/Step3PricingSectors.vue'
import Step4PoliciesAccess from '@/components/wizard/Step4PoliciesAccess.vue'
import Step5ReviewPublish from '@/components/wizard/Step5ReviewPublish.vue'
import LiveCatalogPreview from '@/components/wizard/LiveCatalogPreview.vue'

const wizardStore = useEventWizardStore()
const venues = ref<VenueResponse[]>([])

const steps = [
  { number: 1, title: 'Información & Recinto' },
  { number: 2, title: 'Fechas & Horarios' },
  { number: 3, title: 'Sectores & Precios' },
  { number: 4, title: 'Políticas & Acceso' },
  { number: 5, title: 'Resumen & Publicación' },
]

function nextStep() {
  if (wizardStore.currentStep < 5) {
    wizardStore.currentStep++
    window.scrollTo({ top: 0, behavior: 'smooth' })
  }
}

function prevStep() {
  if (wizardStore.currentStep > 1) {
    wizardStore.currentStep--
    window.scrollTo({ top: 0, behavior: 'smooth' })
  }
}

function goToStep(num: number) {
  wizardStore.currentStep = num
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

onMounted(async () => {
  venues.value = await venueService.getVenues()
  if (venues.value.length > 0 && !wizardStore.venueId) {
    wizardStore.venueId = venues.value[0].id
  }
})
</script>
