# Handoff Report: Frontend Catalog Browse, Month Filtering & Event Detail Overhaul Survey

> **Agent:** `teamwork_preview_explorer_survey_2`  
> **Mission:** Read-only Investigation and Architectural Survey of Catalog, Filter, and Event Detail Views  
> **Working Directory:** `C:\Users\maxe1\Documents\Repos\Spring projects on Github\Ticketing app\.agents\teamwork\teamwork_preview_explorer_survey_2`  
> **Date:** `2026-10-21T05:35:00Z`  
> **Status:** `COMPLETE`  

---

## 1. Observation

### 1.1 Project Structure & Dependencies
Direct inspection of `ticketing-frontend/package.json` reveals the locked dependency set:
- **Vue**: `^3.5.42` (`vue`)
- **Router**: `^5.3.1` (`vue-router` using Vue 3 createRouter / createWebHistory)
- **Store**: `^4.0.3` (`pinia`)
- **Radix UI Primitives**: `^1.9.17` (`radix-vue`)
- **Styling**: `^3.4.17` (`tailwindcss`), `postcss` (`^8.5.31`), `autoprefixer` (`^10.6.1`)
- **Build & Quality Tooling**: `vite` (`^8.2.2`), `vue-tsc` (`^3.3.11`), `vitest` (`^4.1.11`), `oxlint` (`~1.82.0`), `eslint` (`^10.10.0`)

Validation command results:
- `npm run type-check` (`vue-tsc --build`): Exited with code 0 (zero errors).
- `npm run test:unit -- --run` (`vitest --run`): Exited with code 0 (`src/__tests__/App.spec.ts` passed 1 test in 52ms).

### 1.2 `src/views/HomeView.vue` Observations
- **Hero Carousel** (lines 10–48, 129–222): Displays `activeSlide` computed from `catalog.featuredEvents` (or first 3 events). Features auto-play timer (6000ms), left/right nav arrows, dot pagination, and CTA button "Ver Entradas" routing to `goToEvent(activeSlide.id)`.
- **Section 1: Recientemente Agregados** (lines 51–53, 224–290):
  - Line 51–53: `recentEvents = computed(() => catalog.recentlyAddedEvents.slice(0, 3))`
  - Line 233–235: Section title: `Recientemente Agregados`
  - Line 253–288: Grid of 3 event cards. Each card displays `badgeText` (e.g. `'Confirmado'`), event poster `imageUrl`, `event.date`, `event.title`, and `event.venue`.
  - **Defect/Gap**: Cards do **not** display the required `"Entradas desde $X"` badge.
- **Section 2: Próximos a Ocurrir** (lines 55–57, 292–358):
  - Line 55–57: `upcomingEvents = computed(() => catalog.upcomingEvents.slice(0, 3))`
  - Line 301–303: Section title: `Próximos a Ocurrir`
  - Line 321–356: Grid of 3 event cards displaying `badgeText`, `imageUrl`, `event.date`, `event.title`, and `event.venue`.
  - **Defect/Gap**: Cards do **not** display the required `"Entradas desde $X"` badge.
- **Ancillary Sections** (lines 360–496):
  - Services in venue (Comida, Bebidas, Merch)
  - Bank Promotions (Santander, Galicia, MODO)
  - FAQ Banner routing to `/help`.

### 1.3 `src/views/EventsCatalogView.vue` Observations
- **State Initialization** (lines 11–16):
  ```ts
  const searchName = ref('')
  const selectedMonth = ref<string>('all')
  const selectedVenue = ref<string>('all')
  ```
- **Text Search & Debounce** (lines 18–22, 52–60, 120–137):
  - `onMounted` copies `route.query.q` once.
  - `<input v-model="searchName">` triggers on every keystroke without debouncing.
  - Matches `title`, `subtitle`, and `lineup`.
- **Month Selector** (lines 25–34, 63–74, 140–156):
  - `availableMonths` extracts unique string splits (`parts[1] parts[2]` from `"15 de Octubre de 2026"`).
  - Only extracts months present in existing items, rather than generating a future 8-month timeline.
- **Venue Filter** (lines 37–45, 76–81, 159–175):
  - Populated dynamically from all distinct `event.venue` entries in `catalog.events`.
  - **Defect/Gap**: Current mock catalog contains venues outside Buenos Aires (e.g. Chilean venues like `'Movistar Arena'`, `'Estadio Nacional'`, `'Teatro Caupolicán'`, etc.). The requirement mandates strictly 3 Buenos Aires venues (`Estadio Monumental (Buenos Aires)`, `Arena Buenos Aires (Buenos Aires)`, `Teatro Rex (Buenos Aires)`).
- **URL Synchronization** (lines 18–22):
  - Only performs a one-time read on mount: `if (route.query.q) searchName.value = route.query.q`.
  - Does **not** sync `selectedMonth` or `selectedVenue` from URL query.
  - Does **not** push/replace `route.query` when filters are changed.
- **Card Pricing Display** (line 218):
  - Displays plain text `Desde ${{ event.price }}` instead of a styled, prominent badge `"Entradas desde $X"`.

### 1.4 `src/views/EventDetailView.vue` Observations
- **Obsolete Select Dropdowns** (lines 6–16, 37–54, 228–313):
  - Imports Radix UI `SelectRoot`, `SelectTrigger`, `SelectValue`, `SelectItem`, etc.
  - Renders redundant dropdowns:
    1. Select Date: lines 230–267 (`SelectRoot v-model="selectedDate"`)
    2. Select Ticket Type: lines 270–313 (`SelectRoot v-model="selectedTicketTypeIndex"`)
  - Explicitly forbidden by SDD R4 / Anti-pattern 4: *"MUST NOT retain the two old select dropdowns for date and ticket type in EventDetailView.vue"*.
- **Section A: Base Pricing Headline** (lines 85–95, 219–225):
  - Price is only shown inside the sidebar card (`Tickets from ${{ selectedTicketPrice }}.00 USD`).
  - Missing prominent headline banner `"Entradas desde $X"` in the panoramic header banner.
- **Section B: Available Dates / Shows List**:
  - Missing explicit table/card row list where each date/function has full day-of-week date, doors open time, show start time, and a direct `"Comprar"` button.
- **Section C: Payment Methods Accordion**:
  - Missing from `EventDetailView.vue`.
- **Section D: Parking Accordion**:
  - Missing from `EventDetailView.vue`.
- **Section E: Event Description & Sector Price Table**:
  - Contains only an "About the Event" text block (lines 204–212).
  - Missing sector pricing breakdown table matching the venue schema.

### 1.5 Mock Data in `src/stores/catalog.ts` Observations
- Events (lines 6–268) currently reference venues in Chile:
  - `symphony-of-lights`: `venue: 'Movistar Arena'`, `location: 'Santiago, Chile'`
  - `chronos-fest-2026`: `venue: 'Arena Central'`, `location: 'Santiago, Chile'`
  - `global-tech-summit`: `venue: 'Tech Hub Arena, Grand Hall'`, `location: 'Santiago, Chile'`
  - `championship-finals`: `venue: 'Estadio Nacional'`, `location: 'Santiago, Chile'`
  - `indie-rock-sessions`: `venue: 'Teatro Caupolicán'`, `location: 'Santiago, Chile'`
  - `urban-trap-festival`: `venue: 'Parque Bicentenario'`, `location: 'Santiago, Chile'`
  - `cloud-native-devcon`: `venue: 'Centro de Convenciones Titanium'`, `location: 'Santiago, Chile'`
  - `electronica-open-air`: `venue: 'Espacio Riesco'`, `location: 'Santiago, Chile'`
- Only `design-systems-workshop` and `symphony-in-code` mention Buenos Aires.
- **Conflict with SDD**: SDD Section 1 & Section 3.2 explicitly restrict the platform to 3 venues in Buenos Aires:
  1. `Estadio Monumental (Buenos Aires)`
  2. `Arena Buenos Aires (Buenos Aires)`
  3. `Teatro Rex (Buenos Aires)`

---

## 2. Logic Chain

```
[Observation 1.5: catalog.ts mock data has Chilean venues]
       │
       ▼
[SDD Rule 1.0 & 4.0: Platform operates EXCLUSIVELY for 3 proprietary venues in Buenos Aires]
       │
       ▼
(Step 1: Mock data in catalog.ts must be updated so all events belong to the 3 Buenos Aires venues with location 'Buenos Aires, Argentina')
       │
       ├──────────────────────────────────────────────────────────────────┐
       ▼                                                                  ▼
[Observation 1.2: HomeView.vue lacks min-price badges]       [Observation 1.3: EventsCatalogView.vue lacks debouncing,
       │                                                      URL query sync, and 8-month selector]
       ▼                                                                  ▼
(Step 2: Add computeMinPrice helper;                         (Step 3: Implement debounced search [title, lineup, subtitle];
render "Entradas desde $X" badge in both                      generate up to 8 months into future; enforce 3 BA venues;
"Recién salidos a la venta" and "Próximos a ocurrir")        two-way reactive sync with route.query [?q=&month=&venue=];
                                                              render "Entradas desde $X" badge on all catalog cards)
       │
       └──────────────────────────────────┬───────────────────────────────┘
                                          │
                                          ▼
[Observation 1.4: EventDetailView.vue has obsolete selects; lacks sections A, B, C, D, E]
                                          │
                                          ▼
[SDD Rule 3.3: Delete date/ticket selects; Section A Base Pricing; Section B Dates List with direct "Comprar";
Section C Medios de Pago accordion; Section D Parking accordion; Section E Narrative + Sector Price Table]
                                          │
                                          ▼
(Step 4: Refactor EventDetailView.vue utilizing Radix Vue AccordionRoot/AccordionItem,
render explicit date rows with direct navigation, and display sector breakdown table)
```

---

## 3. Component Architecture Breakdown & UI State Patterns

### 3.1 Minimum Price Computation Pattern
To support `"Entradas desde $X"` consistently across `HomeView.vue`, `EventsCatalogView.vue`, and `EventDetailView.vue`:
```ts
export function getMinEventPrice(event: EventItem): number {
  if (event.ticketTypes && event.ticketTypes.length > 0) {
    return Math.min(...event.ticketTypes.map(t => t.price))
  }
  return event.price || 0
}
```
**Badge Markup Pattern (Tailwind)**:
```html
<span class="inline-flex items-center gap-1 px-2.5 py-1 rounded-md bg-[#00688f]/90 text-[#86cffb] border border-[#86cffb]/30 font-mono text-xs font-bold shadow-sm backdrop-blur-md">
  <span>Entradas desde</span>
  <span class="text-white">${{ getMinEventPrice(event) }}</span>
</span>
```

### 3.2 `EventsCatalogView.vue` Reactive State & URL Sync Pattern
1. **State Definition**:
   ```ts
   const searchQuery = ref('')
   const debouncedQuery = ref('')
   const selectedMonth = ref('all')
   const selectedVenue = ref('all')
   let debounceTimer: ReturnType<typeof setTimeout> | null = null
   ```
2. **Debounce Handler**:
   ```ts
   function onSearchInput(val: string) {
     searchQuery.value = val
     if (debounceTimer) clearTimeout(debounceTimer)
     debounceTimer = setTimeout(() => {
       debouncedQuery.value = val.trim()
       updateRouteQuery()
     }, 300)
   }
   ```
3. **Two-Way URL Sync**:
   ```ts
   // Initialize from route.query
   function syncFromRoute() {
     const q = (route.query.q as string) || ''
     const month = (route.query.month as string) || 'all'
     const venue = (route.query.venue as string) || 'all'
     searchQuery.value = q
     debouncedQuery.value = q
     selectedMonth.value = month
     selectedVenue.value = venue
   }

   // Push state to route.query
   function updateRouteQuery() {
     const query: Record<string, string> = {}
     if (debouncedQuery.value) query.q = debouncedQuery.value
     if (selectedMonth.value !== 'all') query.month = selectedMonth.value
     if (selectedVenue.value !== 'all') query.venue = selectedVenue.value

     router.replace({ query })
   }

   watch([selectedMonth, selectedVenue], () => {
     updateRouteQuery()
   })

   watch(() => route.query, () => {
     syncFromRoute()
   }, { deep: true })
   ```
4. **8-Month Selector Generation**:
   Generates upcoming months up to 8 months into the future:
   ```ts
   const availableMonths = computed(() => {
     const months: { label: string; value: string }[] = []
     const date = new Date(2026, 9, 1) // Base reference date Oct 2026
     for (let i = 0; i < 8; i++) {
       const d = new Date(date.getFullYear(), date.getMonth() + i, 1)
       const raw = d.toLocaleDateString('es-AR', { month: 'long', year: 'numeric' })
       const label = raw.charAt(0).toUpperCase() + raw.slice(1) // e.g. "Octubre 2026"
       months.push({ label, value: label })
     }
     return months
   })
   ```
5. **Venue Filter Options**:
   ```ts
   const venueOptions = [
     { label: 'Todos los recintos', value: 'all' },
     { label: 'Estadio Monumental (Buenos Aires)', value: 'Estadio Monumental (Buenos Aires)' },
     { label: 'Arena Buenos Aires (Buenos Aires)', value: 'Arena Buenos Aires (Buenos Aires)' },
     { label: 'Teatro Rex (Buenos Aires)', value: 'Teatro Rex (Buenos Aires)' }
   ]
   ```

### 3.3 `EventDetailView.vue` Structure
```
EventDetailView.vue
├── Header Panoramic Banner (h-[46vh])
│   └── Section A: Prominent Base Pricing Headline ("Entradas desde $X")
│
├── Content Grid (lg:grid-cols-12, -mt-36)
│   ├── Left Column (lg:col-span-8)
│   │   ├── Category badge, Title, Subtitle
│   │   ├── Bento Grid (Venue Card with Zoomable Map modal + Lineup Card)
│   │   ├── Section E: Event Description & Synopsis
│   │   └── Section E: Sector Price Breakdown Table
│   │       (Sector Name, Ubicación/Tipo, Precio Base, Disponibilidad)
│   │
│   └── Right Column (lg:col-span-4)
│       └── Sticky Information & Action Panel
│           ├── Section B: Available Dates List (Funciones disponibles)
│           │   ├── Date Row 1: Full Day/Date + Doors Open + Show Start + [Comprar]
│           │   └── Date Row 2: Full Day/Date + Doors Open + Show Start + [Comprar]
│           │
│           ├── Section C: Payment Methods Accordion (Radix AccordionRoot)
│           │   ├── Accepted Cards (Visa, Mastercard, Amex, Cabal)
│           │   ├── Installment Promos (Santander 6 cuotas, Galicia 3 cuotas)
│           │   └── Digital Wallets (MODO 15% reintegro, Mercado Pago)
│           │
│           ├── Section D: Parking Accordion (Radix AccordionRoot)
│           │   ├── Venue Parking Availability
│           │   ├── Vehicular Access Routes (Avenidas principales)
│           │   └── Parking Passes acquisition info
│           │
│           └── Redlock & Zero-overselling Guarantee Trust Badges
```

### 3.4 Direct "Comprar" Navigation Flow
When user clicks "Comprar" on a specific date row in Section B:
```ts
function handleProceedToPurchase(functionDate?: string) {
  const ev = event.value
  if (!ev) return

  if (ev.status === 'QUEUE_ACTIVE') {
    queue.enterQueue(ev.id)
    router.push({ name: 'queue', params: { id: ev.id }, query: functionDate ? { date: functionDate } : {} })
  } else {
    router.push({ name: 'seat-selection', params: { id: ev.id }, query: functionDate ? { date: functionDate } : {} })
  }
}
```

---

## 4. Concrete Step-by-Step Refactoring Plan

| Phase | Target File | Action Required |
|---|---|---|
| **P1** | `src/stores/catalog.ts` | Normalize all 10 mock events to the 3 Buenos Aires venues (`Estadio Monumental (Buenos Aires)`, `Arena Buenos Aires (Buenos Aires)`, `Teatro Rex (Buenos Aires)`) with location `'Buenos Aires, Argentina'`. Ensure each event has multi-sector ticket types and structured available date entries. |
| **P2** | `src/views/HomeView.vue` | 1. Add `getMinEventPrice(event)` helper.<br>2. Label Section 1 clearly as "Eventos recién salidos a la venta".<br>3. Render prominent `"Entradas desde $X"` badge on cards in both sections.<br>4. Retain carousel, services, bank promos, and FAQ components. |
| **P3** | `src/views/EventsCatalogView.vue` | 1. Add debounced text search (300ms) matching title, lineup/artist, subtitle.<br>2. Generate 8-month selector (default "Todos los meses").<br>3. Enforce the 3 Buenos Aires venue options.<br>4. Wire two-way URL reactive sync with `route.query` (`?q=...&month=...&venue=...`).<br>5. Display `"Entradas desde $X"` badge on every catalog card. Zero genre pills or price sliders. |
| **P4** | `src/views/EventDetailView.vue` | 1. Delete redundant Radix `SelectRoot` dropdowns for date and ticket type.<br>2. Add Section A prominent headline `"Entradas desde $X"` in banner.<br>3. Implement Section B available dates list with date, doors open, show time, and direct "Comprar" button.<br>4. Implement Section C Medios de Pago using `AccordionRoot` from `radix-vue`.<br>5. Implement Section D Estacionamiento using `AccordionRoot`.<br>6. Implement Section E rich narrative and sector price breakdown table. |
| **P5** | `src/__tests__/App.spec.ts` & verification | Verify `npm run type-check` (0 errors), `npm run test:unit` (passes), and `npm run build` succeed. |

---

## 5. Caveats
1. **Mock Data Scope**: The frontend currently operates in standalone mock mode via Pinia stores (`src/stores/catalog.ts`). Backend PostgreSQL database tables exist but the B2C frontend client does not fetch via HTTP yet; updating `catalog.ts` is the intended design for this client worktree.
2. **Worktree Isolation**: In accordance with rule R1 and SDD 3.4, actual implementation edits must occur inside `.worktrees/frontend-catalog-filters` on branch `feature/frontend-catalog-filters`. As an explorer agent, no source modifications have been made during this survey turn.
3. **No New Dependencies**: All necessary UI primitives (`AccordionRoot`, `AccordionItem`, `DialogRoot`, `TooltipRoot`) are confirmed present in `radix-vue` (v1.9.17) in `node_modules`.

---

## 6. Conclusion
The codebase is in a clean, stable baseline state: `vue-tsc` type-checks cleanly and unit tests pass. However, both the catalog and event detail views require targeted refactoring to align with the SDD specification:
1. Updating `src/stores/catalog.ts` mock data to strictly reflect the 3 Buenos Aires venues.
2. Enhancing `HomeView.vue` with `"Entradas desde $X"` badges.
3. Upgrading `EventsCatalogView.vue` with debounced search, 8-month calendar selector, Buenos Aires venue filter, two-way `route.query` sync, and min-price badges.
4. Overhauling `EventDetailView.vue` by deleting redundant selects, adding the banner pricing headline, date list with direct "Comprar" buttons, Radix accordions for payment and parking, and sector price breakdown table.

---

## 7. Verification Method

To independently verify the baseline and subsequent implementation in `ticketing-frontend`:

1. **TypeScript Type Checking**:
   ```powershell
   cd "ticketing-frontend"
   npm run type-check
   ```
   *Expected*: `vue-tsc --build` finishes with 0 errors.

2. **Unit Test Suite**:
   ```powershell
   cd "ticketing-frontend"
   npm run test:unit -- --run
   ```
   *Expected*: Vitest executes and passes all test suites.

3. **Vite Production Build**:
   ```powershell
   cd "ticketing-frontend"
   npm run build-only
   ```
   *Expected*: Production bundle is generated in `dist/` without compilation warnings or errors.

4. **Visual & Behavioral Verification**:
   - Verify `/` displays "Entradas desde $X" on recent and upcoming event cards.
   - Verify `/events` reactive filter with URL query: e.g. `/events?q=rock&month=Octubre%202026&venue=Arena%20Buenos%20Aires%20(Buenos%20Aires)`.
   - Verify `/event/:id` displays base pricing headline, available dates list with "Comprar", payment accordion, parking accordion, and sector price table without old dropdowns.
