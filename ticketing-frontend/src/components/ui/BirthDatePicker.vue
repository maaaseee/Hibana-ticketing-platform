<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import {
  SelectRoot,
  SelectTrigger,
  SelectValue,
  SelectPortal,
  SelectContent,
  SelectViewport,
  SelectItem,
  SelectItemText,
  SelectItemIndicator
} from 'radix-vue'

interface Props {
  modelValue?: string
  minAge?: number
  maxAge?: number
}

const props = withDefaults(defineProps<Props>(), {
  modelValue: '',
  minAge: 13,
  maxAge: 100
})

const emit = defineEmits<{
  (e: 'update:modelValue', value: string): void
}>()

// Extraer año, mes, día si viene en formato YYYY-MM-DD
const initialParts = props.modelValue ? props.modelValue.split('-') : []
const selectedYear = ref<string>(initialParts[0] || '')
const selectedMonth = ref<string>(initialParts[1] || '')
const selectedDay = ref<string>(initialParts[2] || '')

const currentYear = new Date().getFullYear()
const minYear = currentYear - props.maxAge
const maxYear = currentYear - props.minAge

// Lista de Años descendente (los más recientes primero)
const years = computed(() => {
  const list: string[] = []
  for (let y = maxYear; y >= minYear; y--) {
    list.push(y.toString())
  }
  return list
})

// Lista de Meses
const months = [
  { value: '01', label: 'Enero' },
  { value: '02', label: 'Febrero' },
  { value: '03', label: 'Marzo' },
  { value: '04', label: 'Abril' },
  { value: '05', label: 'Mayo' },
  { value: '06', label: 'Junio' },
  { value: '07', label: 'Julio' },
  { value: '08', label: 'Agosto' },
  { value: '09', label: 'Septiembre' },
  { value: '10', label: 'Octubre' },
  { value: '11', label: 'Noviembre' },
  { value: '12', label: 'Diciembre' }
]

// Cálculo dinámico de días en el mes seleccionado
const daysInMonth = computed(() => {
  const m = parseInt(selectedMonth.value, 10)
  const y = parseInt(selectedYear.value, 10)

  if (!m) return 31
  if (m === 2) {
    const isLeap = y ? (y % 4 === 0 && y % 100 !== 0) || y % 400 === 0 : false
    return isLeap ? 29 : 28
  }
  if ([4, 6, 9, 11].includes(m)) {
    return 30
  }
  return 31
})

const days = computed(() => {
  const list: string[] = []
  for (let d = 1; d <= daysInMonth.value; d++) {
    list.push(d < 10 ? `0${d}` : d.toString())
  }
  return list
})

// Ajustar el día si excede el nuevo mes seleccionado
watch([selectedMonth, selectedYear], () => {
  if (selectedDay.value && parseInt(selectedDay.value, 10) > daysInMonth.value) {
    selectedDay.value = daysInMonth.value.toString()
  }
  updateModel()
})

watch(selectedDay, () => {
  updateModel()
})

function updateModel() {
  if (selectedYear.value && selectedMonth.value && selectedDay.value) {
    emit('update:modelValue', `${selectedYear.value}-${selectedMonth.value}-${selectedDay.value}`)
  } else {
    emit('update:modelValue', '')
  }
}

const formattedPreview = computed(() => {
  if (selectedYear.value && selectedMonth.value && selectedDay.value) {
    const monthObj = months.find(m => m.value === selectedMonth.value)
    return `${parseInt(selectedDay.value, 10)} de ${monthObj?.label || selectedMonth.value} de ${selectedYear.value}`
  }
  return ''
})
</script>

<template>
  <div class="birth-date-picker flex flex-col gap-1.5 w-full select-none">
    <div class="grid grid-cols-3 gap-2">
      <!-- 1. DÍA (Radix Select) -->
      <div class="flex flex-col">
        <SelectRoot v-model="selectedDay">
          <SelectTrigger
            class="w-full bg-[#0c1524]/90 border border-[#22354e] focus:border-[#0084ff] hover:border-[#38bdf8]/50 rounded-xl py-2 px-2.5 text-xs sm:text-sm font-body text-white flex items-center justify-between outline-none transition-colors cursor-pointer data-[placeholder]:text-[#64748b]"
            aria-label="Seleccionar Día"
          >
            <SelectValue placeholder="Día" />
            <span class="material-symbols-outlined text-[16px] text-[#64748b]">expand_more</span>
          </SelectTrigger>
          <SelectPortal>
            <SelectContent
              class="z-50 bg-[#0e1726] border border-[#22354e] rounded-xl shadow-2xl p-1 max-h-56 overflow-y-auto text-xs text-white w-[var(--radix-select-trigger-width)] min-w-[var(--radix-select-trigger-width)]"
              position="popper"
              :side-offset="4"
            >
              <SelectViewport class="p-1 w-full">
                <SelectItem
                  v-for="d in days"
                  :key="d"
                  :value="d"
                  class="w-full flex items-center justify-between px-3 py-1.5 rounded-lg hover:bg-[#0084ff] hover:text-white cursor-pointer outline-none transition-colors data-[highlighted]:bg-[#0084ff] data-[highlighted]:text-white"
                >
                  <SelectItemText>{{ d }}</SelectItemText>
                  <SelectItemIndicator>
                    <span class="material-symbols-outlined text-[14px]">check</span>
                  </SelectItemIndicator>
                </SelectItem>
              </SelectViewport>
            </SelectContent>
          </SelectPortal>
        </SelectRoot>
      </div>

      <!-- 2. MES (Radix Select) -->
      <div class="flex flex-col">
        <SelectRoot v-model="selectedMonth">
          <SelectTrigger
            class="w-full bg-[#0c1524]/90 border border-[#22354e] focus:border-[#0084ff] hover:border-[#38bdf8]/50 rounded-xl py-2 px-2.5 text-xs sm:text-sm font-body text-white flex items-center justify-between outline-none transition-colors cursor-pointer data-[placeholder]:text-[#64748b]"
            aria-label="Seleccionar Mes"
          >
            <SelectValue placeholder="Mes" />
            <span class="material-symbols-outlined text-[16px] text-[#64748b]">expand_more</span>
          </SelectTrigger>
          <SelectPortal>
            <SelectContent
              class="z-50 bg-[#0e1726] border border-[#22354e] rounded-xl shadow-2xl p-1 max-h-56 overflow-y-auto text-xs text-white w-[var(--radix-select-trigger-width)] min-w-[var(--radix-select-trigger-width)]"
              position="popper"
              :side-offset="4"
            >
              <SelectViewport class="p-1 w-full">
                <SelectItem
                  v-for="m in months"
                  :key="m.value"
                  :value="m.value"
                  class="w-full flex items-center justify-between px-3 py-1.5 rounded-lg hover:bg-[#0084ff] hover:text-white cursor-pointer outline-none transition-colors data-[highlighted]:bg-[#0084ff] data-[highlighted]:text-white"
                >
                  <SelectItemText>{{ m.label }}</SelectItemText>
                  <SelectItemIndicator>
                    <span class="material-symbols-outlined text-[14px]">check</span>
                  </SelectItemIndicator>
                </SelectItem>
              </SelectViewport>
            </SelectContent>
          </SelectPortal>
        </SelectRoot>
      </div>

      <!-- 3. AÑO (Radix Select) -->
      <div class="flex flex-col">
        <SelectRoot v-model="selectedYear">
          <SelectTrigger
            class="w-full bg-[#0c1524]/90 border border-[#22354e] focus:border-[#0084ff] hover:border-[#38bdf8]/50 rounded-xl py-2 px-2.5 text-xs sm:text-sm font-body text-white flex items-center justify-between outline-none transition-colors cursor-pointer data-[placeholder]:text-[#64748b]"
            aria-label="Seleccionar Año"
          >
            <SelectValue placeholder="Año" />
            <span class="material-symbols-outlined text-[16px] text-[#64748b]">expand_more</span>
          </SelectTrigger>
          <SelectPortal>
            <SelectContent
              class="z-50 bg-[#0e1726] border border-[#22354e] rounded-xl shadow-2xl p-1 max-h-56 overflow-y-auto text-xs text-white w-[var(--radix-select-trigger-width)] min-w-[var(--radix-select-trigger-width)]"
              position="popper"
              :side-offset="4"
            >
              <SelectViewport class="p-1 w-full">
                <SelectItem
                  v-for="y in years"
                  :key="y"
                  :value="y"
                  class="w-full flex items-center justify-between px-3 py-1.5 rounded-lg hover:bg-[#0084ff] hover:text-white cursor-pointer outline-none transition-colors data-[highlighted]:bg-[#0084ff] data-[highlighted]:text-white"
                >
                  <SelectItemText>{{ y }}</SelectItemText>
                  <SelectItemIndicator>
                    <span class="material-symbols-outlined text-[14px]">check</span>
                  </SelectItemIndicator>
                </SelectItem>
              </SelectViewport>
            </SelectContent>
          </SelectPortal>
        </SelectRoot>
      </div>
    </div>

    <!-- Confirmación visual amigable si la fecha está completa -->
    <div v-if="formattedPreview" class="flex items-center gap-1.5 text-[11px] font-body text-[#38bdf8] pt-0.5">
      <span class="material-symbols-outlined text-[13px]">calendar_today</span>
      <span>Fecha elegida: {{ formattedPreview }}</span>
    </div>
  </div>
</template>
