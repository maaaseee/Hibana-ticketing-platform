import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { CreateEventRequest, EventSectorRequest } from '@/types/event'

export const useEventWizardStore = defineStore('eventWizard', () => {
  const currentStep = ref<number>(1)
  const isSubmitting = ref<boolean>(false)

  // Step 1: Info & Venue
  const title = ref<string>('')
  const slug = ref<string>('')
  const category = ref<string>('CONCIERTOS')
  const genres = ref<string[]>(['Rock', 'Indie'])
  const description = ref<string>('')
  const heroBannerUrl = ref<string>('https://images.unsplash.com/photo-1470225620780-dba8ba36b745?auto=format&fit=crop&w=1200&q=80')
  const posterUrl = ref<string>('https://images.unsplash.com/photo-1514525253161-7a46d19cd819?auto=format&fit=crop&w=600&q=80')
  const venueId = ref<string>('')

  // Step 2: Dates & Schedule
  const doorsOpenAt = ref<string>('2026-04-18T18:00')
  const startsAt = ref<string>('2026-04-18T21:00')
  const endsAt = ref<string>('2026-04-19T01:00')
  const timezone = ref<string>('America/Argentina/Buenos_Aires')
  const waitingRoomEnabled = ref<boolean>(true)
  const waitingRoomQueueThreshold = ref<number>(2500)

  // Step 3: Pricing & Sectors
  const sectors = ref<EventSectorRequest[]>([
    {
      name: 'Campo Delantero VIP',
      type: 'GENERAL_ADMISSION',
      totalCapacity: 4000,
      basePrice: 65000,
      serviceFee: 6500,
      currency: 'ARS',
    },
    {
      name: 'Campo General',
      type: 'GENERAL_ADMISSION',
      totalCapacity: 6000,
      basePrice: 40000,
      serviceFee: 4000,
      currency: 'ARS',
    },
    {
      name: 'Platea Baja Numerada',
      type: 'SEATED',
      totalCapacity: 2500,
      basePrice: 85000,
      serviceFee: 8500,
      currency: 'ARS',
    },
  ])

  // Step 4: Policies & Access
  const maxTicketsPerUser = ref<number>(4)
  const transferable = ref<boolean>(true)
  const minAge = ref<number>(16)
  const dynamicQr = ref<boolean>(true)

  // Auto-generate slug when title changes
  function updateTitle(newTitle: string): void {
    title.value = newTitle
    slug.value = newTitle
      .toLowerCase()
      .normalize('NFD')
      .replace(/[\u0300-\u036f]/g, '')
      .replace(/[^a-z0-9]+/g, '-')
      .replace(/^-+|-+$/g, '')
  }

  // Sector mutations
  function addSector(sector: EventSectorRequest): void {
    sectors.value.push({ ...sector })
  }

  function removeSector(index: number): void {
    sectors.value.splice(index, 1)
  }

  function updateSector(index: number, sector: EventSectorRequest): void {
    if (sectors.value[index]) {
      sectors.value[index] = { ...sector }
    }
  }

  // Calculations
  const totalCapacity = computed(() => {
    return sectors.value.reduce((acc, s) => acc + (Number(s.totalCapacity) || 0), 0)
  })

  const grossRevenueProjection = computed(() => {
    return sectors.value.reduce((acc, s) => acc + (Number(s.totalCapacity) || 0) * (Number(s.basePrice) || 0), 0)
  })

  const estimatedServiceFeeTotal = computed(() => {
    return sectors.value.reduce((acc, s) => acc + (Number(s.totalCapacity) || 0) * (Number(s.serviceFee) || 0), 0)
  })

  // Validation
  const isStep1Valid = computed(() => {
    return title.value.trim().length >= 3 && slug.value.trim().length >= 3 && !!venueId.value
  })

  const isStep2Valid = computed(() => {
    return !!doorsOpenAt.value && !!startsAt.value && !!endsAt.value && new Date(startsAt.value) >= new Date(doorsOpenAt.value)
  })

  const isStep3Valid = computed(() => {
    return sectors.value.length > 0 && sectors.value.every((s) => s.name && s.totalCapacity > 0 && s.basePrice >= 0)
  })

  const isStep4Valid = computed(() => {
    return maxTicketsPerUser.value >= 1 && minAge.value >= 0
  })

  const canPublish = computed(() => {
    return isStep1Valid.value && isStep2Valid.value && isStep3Valid.value && isStep4Valid.value
  })

  function buildPayload(organizerId = 'org-hibana-live'): CreateEventRequest {
    const toIso = (dt: string) => {
      try {
        return new Date(dt).toISOString()
      } catch {
        return dt
      }
    }

    const mediaObj = {
      heroBannerUrl: heroBannerUrl.value,
      posterUrl: posterUrl.value,
      description: description.value,
    }

    const salesPolicyObj = {
      maxTicketsPerUser: maxTicketsPerUser.value,
      transferable: transferable.value,
      minAge: minAge.value,
      dynamicQr: dynamicQr.value,
      waitingRoomEnabled: waitingRoomEnabled.value,
      waitingRoomQueueThreshold: waitingRoomQueueThreshold.value,
    }

    return {
      organizerId,
      venueId: venueId.value,
      slug: slug.value,
      title: title.value,
      category: category.value,
      genres: genres.value,
      doorsOpenAt: toIso(doorsOpenAt.value),
      startsAt: toIso(startsAt.value),
      endsAt: toIso(endsAt.value),
      timezone: timezone.value,
      media: JSON.stringify(mediaObj),
      salesPolicy: JSON.stringify(salesPolicyObj),
      sectors: sectors.value.map((s) => ({
        name: s.name,
        type: s.type,
        totalCapacity: Number(s.totalCapacity),
        basePrice: Number(s.basePrice),
        serviceFee: Number(s.serviceFee),
        currency: s.currency,
      })),
    }
  }

  function reset(): void {
    currentStep.value = 1
    title.value = ''
    slug.value = ''
    category.value = 'CONCIERTOS'
    genres.value = ['Rock', 'Indie']
    description.value = ''
    heroBannerUrl.value = 'https://images.unsplash.com/photo-1470225620780-dba8ba36b745?auto=format&fit=crop&w=1200&q=80'
    posterUrl.value = 'https://images.unsplash.com/photo-1514525253161-7a46d19cd819?auto=format&fit=crop&w=600&q=80'
    venueId.value = ''
    doorsOpenAt.value = '2026-04-18T18:00'
    startsAt.value = '2026-04-18T21:00'
    endsAt.value = '2026-04-19T01:00'
    timezone.value = 'America/Argentina/Buenos_Aires'
    waitingRoomEnabled.value = true
    waitingRoomQueueThreshold.value = 2500
    sectors.value = [
      {
        name: 'Campo General',
        type: 'GENERAL_ADMISSION',
        totalCapacity: 5000,
        basePrice: 45000,
        serviceFee: 4500,
        currency: 'ARS',
      },
    ]
    maxTicketsPerUser.value = 4
    transferable.value = true
    minAge.value = 16
    dynamicQr.value = true
  }

  return {
    currentStep,
    isSubmitting,
    title,
    slug,
    category,
    genres,
    description,
    heroBannerUrl,
    posterUrl,
    venueId,
    doorsOpenAt,
    startsAt,
    endsAt,
    timezone,
    waitingRoomEnabled,
    waitingRoomQueueThreshold,
    sectors,
    maxTicketsPerUser,
    transferable,
    minAge,
    dynamicQr,
    totalCapacity,
    grossRevenueProjection,
    estimatedServiceFeeTotal,
    isStep1Valid,
    isStep2Valid,
    isStep3Valid,
    isStep4Valid,
    canPublish,
    updateTitle,
    addSector,
    removeSector,
    updateSector,
    buildPayload,
    reset,
  }
})
