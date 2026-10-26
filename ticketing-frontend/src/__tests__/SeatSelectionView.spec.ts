import { describe, it, expect, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { createRouter, createWebHistory } from 'vue-router'
import SeatSelectionView from '../views/SeatSelectionView.vue'
import { useTicketingStore } from '../stores/ticketing'

const routes = [
  { path: '/', name: 'home', component: { template: '<div>Home</div>' } },
  { path: '/event/:id', name: 'event-detail', component: { template: '<div>Event</div>' } },
  { path: '/event/:id/seats', name: 'seat-selection', component: SeatSelectionView },
  { path: '/event/:id/checkout', name: 'checkout', component: { template: '<div>Checkout</div>' } },
  { path: '/event/:id/queue', name: 'queue', component: { template: '<div>Queue</div>' } }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

describe('SeatSelectionView.vue (3-Step Hierarchical Purchase Flow)', () => {
  beforeEach(async () => {
    setActivePinia(createPinia())
    router.push('/event/evt_ado_2026/seats')
    await router.isReady()
  })

  it('mounts properly and displays the top urgency countdown banner & exit button', async () => {
    const wrapper = mount(SeatSelectionView, {
      global: {
        plugins: [router]
      }
    })

    expect(wrapper.text()).toContain('Asientos reservados:')
    expect(wrapper.text()).toContain('Salir al Home')
    expect(wrapper.text()).toContain('Stress Test Fila')
  })

  it('renders Paso 1: Seleccionar ubicación with clean accordion sidebar (Campo, Platea Baja, Platea Alta)', async () => {
    const wrapper = mount(SeatSelectionView, {
      global: {
        plugins: [router]
      }
    })

    expect(wrapper.text()).toContain('Seleccionar ubicación')
    expect(wrapper.text()).toContain('Para continuar elegí tu ubicación desde el listado')
    expect(wrapper.text()).toContain('Campo')
    expect(wrapper.text()).toContain('Platea Baja')
    expect(wrapper.text()).toContain('Platea Alta')
    expect(wrapper.text()).toContain('Continuar')
    // Sourced from backend event sector pricing with price range (min price - max price)
    expect(wrapper.text()).toMatch(/\$\s?[0-9.,]+\s*-\s*\$\s?[0-9.,]+/)
    // Floating tooltip container with "Numbered Seats" has been removed
    expect(wrapper.text()).not.toContain('Numbered Seats')
  })

  it('supports double clicking a sector to advance to Paso 2: Selección de Zona', async () => {
    const wrapper = mount(SeatSelectionView, {
      global: {
        plugins: [router]
      }
    })

    // Double-click on sector
    const sectorElement = wrapper.find('svg g.cursor-pointer')
    expect(sectorElement.exists()).toBe(true)
    await sectorElement.trigger('dblclick')

    // Verify Step 2 indicators
    expect(wrapper.text()).toContain('Paso 2: Selección de Zona')
    expect(wrapper.text()).toContain('DIRECCIÓN HACIA EL ESCENARIO PRINCIPAL')
    expect(wrapper.text()).toContain('Radar Estadio')
    expect(wrapper.text()).toContain('ZONA A1')
    expect(wrapper.text()).toContain('ZONA A2')
    expect(wrapper.text()).toContain('ZONA A3')
    expect(wrapper.text()).toContain('Puerta Sur 3')
    expect(wrapper.text()).toContain('Continuar a Selección de Butacas')
    // Radar preview marks the selected sector in bright cyan
    expect(wrapper.html()).toContain('#00b4d8')
  })

  it('transitions to Paso 3: Selección de Butacas with 0 pre-selected seats by default and 100% available seats for normal functions', async () => {
    const wrapper = mount(SeatSelectionView, {
      global: {
        plugins: [router]
      }
    })

    // Advance: Step 1 -> Step 2
    const step1Btn = wrapper.findAll('aside button').find(b => b.text().includes('Continuar'))
    await step1Btn!.trigger('click')

    // Advance: Step 2 -> Step 3
    const step2Btn = wrapper.findAll('aside button').find(b => b.text().includes('Continuar a Selección de Butacas'))
    await step2Btn!.trigger('click')

    // Verify Step 3 layout and cart
    expect(wrapper.text()).toContain('Tu Selección')
    expect(wrapper.text()).toContain('DIRECCIÓN HACIA EL ESCENARIO PRINCIPAL')
    expect(wrapper.text()).toContain('No hay asientos seleccionados')
    expect(wrapper.text()).toContain('Confirmar Asientos y Proceder al Pago')

    const ticketingStore = useTicketingStore()
    // Invariants: ZERO pre-selected seats on initial mount
    expect(ticketingStore.selectedSeats.length).toBe(0)

    // For normal/current functions, seats are available (no occupied seats except wheelchair accessible)
    const occupiedSeats = wrapper.findAll('.cursor-not-allowed.opacity-70')
    expect(occupiedSeats.length).toBe(0)
  })

  it('allows selecting seats, updating total, and removing them from cart', async () => {
    const wrapper = mount(SeatSelectionView, {
      global: {
        plugins: [router]
      }
    })

    // Go to Step 3
    const step1Btn = wrapper.findAll('aside button').find(b => b.text().includes('Continuar'))
    await step1Btn!.trigger('click')

    const step2Btn = wrapper.findAll('aside button').find(b => b.text().includes('Continuar a Selección de Butacas'))
    await step2Btn!.trigger('click')

    const ticketingStore = useTicketingStore()
    expect(ticketingStore.selectedSeats.length).toBe(0)

    // Select an available seat in row S
    const availableSeat = wrapper.find('[title*="Fila S, Butaca 10"]')
    expect(availableSeat.exists()).toBe(true)
    await availableSeat.trigger('click')

    expect(ticketingStore.selectedSeats.length).toBe(1)
    expect(wrapper.text()).toContain('Subtotal (1 asientos)')

    // Deselect one seat via remove button
    const removeBtn = wrapper.find('button[title="Eliminar asiento"]')
    expect(removeBtn.exists()).toBe(true)
    await removeBtn.trigger('click')

    expect(ticketingStore.selectedSeats.length).toBe(0)
  })

  it('populates occupied seats ONLY on designated test function (?testOccupied=true or ?functionId=fn-symphony-2)', async () => {
    await router.push('/event/symphony-of-lights/seats?functionId=fn-symphony-2')
    await router.isReady()

    const wrapper = mount(SeatSelectionView, {
      global: {
        plugins: [router]
      }
    })

    // Advance to Step 3
    const step1Btn = wrapper.findAll('aside button').find(b => b.text().includes('Continuar'))
    await step1Btn!.trigger('click')

    const step2Btn = wrapper.findAll('aside button').find(b => b.text().includes('Continuar a Selección de Butacas'))
    await step2Btn!.trigger('click')

    // Should have occupied seats in this designated test function
    const occupiedSeats = wrapper.findAll('.cursor-not-allowed.opacity-70')
    expect(occupiedSeats.length).toBeGreaterThan(0)
  })

  it('shows confirmation modal when clicking exit while having selected seats', async () => {
    const wrapper = mount(SeatSelectionView, {
      global: {
        plugins: [router]
      }
    })

    // Advance to Step 3 and select a seat
    const step1Btn = wrapper.findAll('aside button').find(b => b.text().includes('Continuar'))
    await step1Btn!.trigger('click')

    const step2Btn = wrapper.findAll('aside button').find(b => b.text().includes('Continuar a Selección de Butacas'))
    await step2Btn!.trigger('click')

    const seat = wrapper.find('[title*="Fila S, Butaca 10"]')
    await seat.trigger('click')

    const ticketingStore = useTicketingStore()
    expect(ticketingStore.selectedSeats.length).toBe(1)

    // Click exit button in header
    const exitBtn = wrapper.find('header button')
    expect(exitBtn.text()).toContain('Salir al Home')
    await exitBtn.trigger('click')

    // Expect modal to appear in document body via Radix DialogPortal
    expect(document.body.textContent).toContain('¿Deseas salir de la reserva?')
    expect(document.body.textContent).toContain('Salir y liberar asientos')
  })
})
