# Handoff Report: Survey of Frontend Catalog Browse, Filters & Event Detail View

> **Task ID:** `FRONTEND-CATALOG-AND-EVENT-DETAIL-01` (Survey Phase)  
> **Agent:** `teamwork_preview_explorer_survey_1`  
> **Recipient / Parent Agent:** `a3ff3c85-7d7a-40b8-8f7b-47f2683fbf87`  
> **Working Directory:** `C:\Users\maxe1\Documents\Repos\Spring projects on Github\Ticketing app\.agents\teamwork\teamwork_preview_explorer_survey_1`  
> **Date:** 2026-10-21  

---

## 1. Observation

Direct inspection of the codebase, specifications, and related artifacts revealed the following exact facts:

### 1.1 Specification Constraints & Anti-Patterns
- **Source:** `docs/plans/frontend-catalog-browse-and-filters-sdd.md` (lines 14–20, 39–74, 80–88) and `.agents/teamwork/ORIGINAL_REQUEST.md` (lines 17–39):
  - **Venue Exclusivity:** The system operates exclusively for **3 premier venues located in Buenos Aires, Argentina**:
    1. `Estadio Monumental (Buenos Aires)` (Estadio Masivo / River Plate style)
    2. `Arena Buenos Aires (Buenos Aires)` (Arena Cubierto / Movistar Arena style)
    3. `Teatro Rex (Buenos Aires)` (Teatro Histórico / Gran Rex style)
  - **Catalog Boundaries (Anti-Patterns):**
    - ❌ MUST NOT render genre pills or category tag filters in the catalog view.
    - ❌ MUST NOT include price range sliders or min/max price inputs in the catalog view.
    - ❌ MUST NOT present dropdowns for selecting Chilean or international cities (strictly Buenos Aires venues).
    - ❌ MUST NOT retain the old select dropdowns for selecting date and general ticket type in `EventDetailView.vue`.
    - ❌ MUST NOT install third-party UI/calendar libraries (`radix-vue` and `tailwindcss` are locked and sufficient).

### 1.2 Current State of Mock Data & Data Models
- **Source:** `ticketing-frontend/src/stores/catalog.ts` (lines 6–268) & `ticketing-frontend/src/types/index.ts` (lines 1–24):
  - **No Dedicated Mock Data File:** There is currently no `src/data/mockEvents.ts` or `src/data/mockVenues.ts`. All 10 mock events are hardcoded directly into the Pinia store `useCatalogStore` (`src/stores/catalog.ts:6-268`).
  - **City & Venue Violation:** 8 of the 10 mock events specify `location: 'Santiago, Chile'` with venues such as `Movistar Arena`, `Arena Central`, `Estadio Nacional`, `Teatro Caupolicán`, `Parque Bicentenario`, `Centro de Convenciones Titanium`, and `Espacio Riesco`. Only 2 specify Buenos Aires (`Tech Hub, Sala A`, `Grand Opera House`), neither of which matches the 3 required venue names.
  - **Flat Date & Missing Function Model:** `EventItem` in `src/types/index.ts` lines 5 and 22 only exposes `date: string` and `availableDates?: string[]` (flat string array, e.g. `'Viernes, 15 de Octubre - 20:00'`). It lacks structured metadata for doors open time (`doorsOpen`), show start time (`showStart`), unique function IDs, or ISO date stamps.
  - **Missing Sector Pricing Schema:** `EventItem` only contains `price: number` (flat scalar) and `ticketTypes?: { name: string; price: number }[]`. There is no link to the venue's sector configuration, capacities, seat tiers, or pricing breakdown.
  - **Missing Parking & Payment Promotion Types:** Neither `EventItem` nor any other model in `src/types/index.ts` contains structured fields for venue parking (availability, routes, passes) or accepted payment methods/bank installment promos.

### 1.3 Current State of Views
- **Home View (`ticketing-frontend/src/views/HomeView.vue`):**
  - Lines 51–57 compute `recentEvents` and `upcomingEvents` (slicing 3 events each).
  - Lines 253–288 and 321–355 render event cards displaying badge, image, date, title, and venue.
  - **Deficiency:** Cards do NOT render the required **"Entradas desde $X"** badge using minimum sector pricing (SDD 3.1).
- **Catalog View (`ticketing-frontend/src/views/EventsCatalogView.vue`):**
  - Lines 25–34 extract `availableMonths` using a brittle string split: `event.date.split(' de ')`.
  - Lines 37–45 extract `availableVenues` dynamically from the current events, currently populating Chilean venues.
  - Lines 18–22: Only reads `route.query.q` on initial mount.
  - **Deficiency:** Does NOT read `month` or `venue` from `route.query`.
  - **Deficiency:** Does NOT synchronize active filter changes back to `route.query` (no `router.replace` / watcher).
  - **Deficiency:** Month filter is not bounded up to 8 months into the future.
  - **Deficiency:** Venue filter shows `Todos los estadios` and Chilean venues rather than the 3 designated Buenos Aires venues.
- **Event Detail View (`ticketing-frontend/src/views/EventDetailView.vue`):**
  - Lines 228–313: Uses two Radix UI `SelectRoot` dropdowns (`Select Date` and `Ticket Type`) inside the purchase sidebar.
  - **Deficiency:** SDD 3.3 and MUST NOT rule explicitly demand eliminating these two dropdown selects.
  - **Deficiency:** Section A (prominent "Entradas desde $X" banner badge) is missing from the header.
  - **Deficiency:** Section B (explicit list/table of available dates/functions with doors open, show start time, and direct "Comprar" button) is missing.
  - **Deficiency:** Section C (Payment Methods accordion) is missing.
  - **Deficiency:** Section D (Parking accordion) is missing.
  - **Deficiency:** Section E (Sector price table mapping venue JSON sectors) is missing.

### 1.4 Radix-Vue Capabilities Verification
- Inspection of `ticketing-frontend/node_modules/radix-vue/dist/Accordion/index.d.ts` confirmed the presence of:
  - `AccordionRoot`, `AccordionItem`, `AccordionHeader`, `AccordionTrigger`, `AccordionContent`.
- No new packages are needed to fulfill the collapsible accordions for Payment Methods and Parking.

### 1.5 Test Suite Status
- Command `npm run test:unit` executed in `ticketing-frontend`:
  - Result: 1 test file passed (`src/__tests__/App.spec.ts`), 1 test passed in 1.60s.

---

## 2. Logic Chain

1. **Premise 1 (Domain Alignment):** The platform is strictly tailored for 3 Buenos Aires venues (`Estadio Monumental (Buenos Aires)`, `Arena Buenos Aires (Buenos Aires)`, `Teatro Rex (Buenos Aires)`).
   - *Inference:* All mock events in `ticketing-frontend` must be updated or replaced to reference these 3 venues exclusively, with valid Buenos Aires addresses, geographic details, and appropriate capacities. All Chilean references must be removed.

2. **Premise 2 (Data Architecture & Maintainability):** Hardcoding 10 complex event objects inside `src/stores/catalog.ts` bloats store logic and makes testing, typing, and future API integration fragile.
   - *Inference:* Mock data should be cleanly separated into `src/data/mockVenues.ts` and `src/data/mockEvents.ts`, leaving `catalog.ts` as a pure Pinia state store that consumes typed mock fixtures or APIs.

3. **Premise 3 (Event Function Schema):** SDD Section 3.3.B requires each date to be rendered as an explicit row with doors open time, show start time, and a direct "Comprar" action.
   - *Inference:* Flat string dates (`availableDates: ['Viernes, 15 de Octubre - 20:00']`) are inadequate. A dedicated `EventFunction` interface is required:
     ```typescript
     interface EventFunction {
       id: string
       dateIso: string // e.g. "2026-11-15T21:00:00"
       formattedDate: string // e.g. "Viernes 15 de Noviembre de 2026"
       doorsOpen: string // e.g. "18:00 hs"
       showStart: string // e.g. "21:00 hs"
       status: 'AVAILABLE' | 'FEW_SEATS' | 'SOLD_OUT' | 'QUEUE_ACTIVE'
     }
     ```

4. **Premise 4 (Venue Sectors & Price Breakdown):** SDD Section 3.3.E requires a price table mapping every sector from the venue schema, and SDD 3.1–3.3 requires displaying "Entradas desde $X" using the minimum sector price.
   - *Inference:* Each venue must define its canonical sectors (e.g. Campo Delantero, Platea Baja, General), and each event must contain a `sectors: SectorPricing[]` list with `minPrice` calculated dynamically as `Math.min(...sectors.map(s => s.price))` or stored explicitly.

5. **Premise 5 (Month Filter Robustness):** SDD Section 3.2 specifies a month filter displaying upcoming months up to 8 months into the future, dynamically bounded by the latest event date.
   - *Inference:* Splitting strings by `' de '` (`catalog.events.forEach(...)`) fails if any date format deviates. Using standard `dateIso` timestamps (e.g., `2026-10-15`) allows generating a sequence of future months in Spanish (e.g., `Octubre 2026`, `Noviembre 2026`, ..., `Mayo 2027`) and filtering events deterministically.

6. **Premise 6 (URL Synchronization):** SDD Section 3.2 mandates that search queries, selected month, and selected venue synchronize with `route.query` (`?q=...&month=...&venue=...`).
   - *Inference:* `EventsCatalogView.vue` must initialize its state from `route.query` and use a debounced watcher to execute `router.replace({ query: { ... } })` whenever filters change.

7. **Premise 7 (EventDetailView Overhaul):** SDD Section 3.3 and Section 4 require eliminating the two dropdown selects and replacing the layout with Sections A through E.
   - *Inference:* Remove lines 228–313 of `EventDetailView.vue`. Replace them with:
     - Section A: Prominent "Entradas desde $X" banner badge in the hero header.
     - Section B: Available Dates & Functions list with individual "Comprar" buttons.
     - Section C: Radix `AccordionRoot` for Payment Methods & Bank Installments.
     - Section D: Radix `AccordionRoot` for Official Venue Parking & Access Routes.
     - Section E: Event description alongside the Sector Pricing Table.

---

## 3. Detailed Venue & Data Specification

### 3.1 The 3 Proprietary Buenos Aires Venues

| Venue Name | Category & Scale | Location / Address | Capacity | Canonical Sectors & Typical Pricing (USD / ARS base) | Parking Infrastructure | Key Payment Promotions |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **Estadio Monumental (Buenos Aires)** | Estadio Masivo (Outdoor Stadium) | Av. Figueroa Alcorta 7597, Núñez, CABA | 84,567 | 1. **Campo Delantero VIP:** $140<br>2. **Platea San Martín / Belgrano Baja:** $110<br>3. **Platea San Martín / Belgrano Alta:** $75<br>4. **Campo General:** $50<br>5. **Tribuna Sívori / Centenario Alta:** $40<br>*Min Price: $40* | Estacionamiento oficial Tiro Federal y Ciudad Universitaria (3,500 vehículos). Acceso exclusivo por Av. Cantilo / Av. del Libertador y Puente Labruna. Pase de parking con reserva anticipada ($20). | **Santander:** Hasta 6 cuotas sin interés.<br>**Galicia:** 3 cuotas sin interés.<br>**MODO:** 15% reintegro los viernes. |
| **Arena Buenos Aires (Buenos Aires)** | Arena Cubierto (Indoor Precision Acoustics) | Humboldt 450, Villa Crespo, CABA | 15,000 | 1. **Campo VIP Frontal:** $95<br>2. **Campo General:** $65<br>3. **Platea Baja Preferencial:** $85<br>4. **Platea Alta Central:** $55<br>5. **Platea Alta Lateral:** $35<br>*Min Price: $35* | Estacionamiento subterráneo techado (800 cocheras). Ingreso por Humboldt y Juan B. Justo. Celdas de recarga para vehículos eléctricos. | **Santander:** 6 cuotas sin interés.<br>**BBVA:** 3 cuotas sin interés.<br>**Mercado Pago:** Débito y dinero en cuenta. |
| **Teatro Rex (Buenos Aires)** | Teatro Histórico (Acoustic Hall & Theatre) | Av. Corrientes 857, San Nicolás, CABA | 3,262 | 1. **Platea Platino (Filas 1–12):** $80<br>2. **Platea Oro (Filas 13–25):** $65<br>3. **Super Pullman:** $50<br>4. **Pullman:** $38<br>5. **Tertulia Superior:** $25<br>*Min Price: $25* | Sin cochera propia (edificio patrimonial). Convenio exclusivo con garajes comerciales sobre Av. Corrientes 880 y Sarmiento 940 (20% descuento con ticket digital Amaryllis). | **Banco Ciudad / Santander:** 3 cuotas sin interés.<br>**Cuenta DNI / MODO:** Beneficios bancarios semanales. |

### 3.2 Proposed TypeScript Models (`src/types/index.ts`)

```typescript
export interface EventFunction {
  id: string
  dateIso: string // e.g., "2026-11-15T21:00:00"
  formattedDate: string // e.g., "Viernes 15 de Noviembre de 2026"
  doorsOpen: string // e.g., "18:00 hs"
  showStart: string // e.g., "21:00 hs"
  status: 'AVAILABLE' | 'FEW_SEATS' | 'SOLD_OUT' | 'QUEUE_ACTIVE'
}

export interface SectorPricing {
  id: string
  name: string
  price: number
  currency: 'USD' | 'ARS'
  capacity?: number
  type: 'GENERAL_ADMISSION' | 'NUMBERED_SEATS' | 'VIP'
  description?: string
  status?: 'AVAILABLE' | 'FEW_SEATS' | 'SOLD_OUT'
}

export interface VenueParking {
  available: boolean
  title: string
  description: string
  price?: number
  accessRoutes: string[]
  bookingPassUrl?: string
}

export interface PaymentPromotion {
  id: string
  bank: string
  card: string
  installments: string
  description: string
  badgeText?: string
}

export interface VenueInfo {
  id: string
  name: string // "Estadio Monumental (Buenos Aires)" | "Arena Buenos Aires (Buenos Aires)" | "Teatro Rex (Buenos Aires)"
  shortName: string
  city: 'Buenos Aires'
  country: 'Argentina'
  address: string
  capacity: number
  type: 'ESTADIO_MASIVO' | 'ARENA_CUBIERTO' | 'TEATRO_HISTORICO'
  parking: VenueParking
  paymentMethods: PaymentPromotion[]
  sectors: SectorPricing[]
}

export interface EventItem {
  id: string
  title: string
  subtitle: string
  date: string
  dateIso: string
  time?: string
  venue: string
  venueId: 'estadio-monumental' | 'arena-buenos-aires' | 'teatro-rex'
  location: string // "Buenos Aires, Argentina"
  category: string
  genre?: string
  price: number // Minimum entry price for backwards-compat
  minPrice: number
  imageUrl: string
  badgeText?: string
  badgeType?: 'primary' | 'secondary' | 'warning' | 'error'
  status: 'AVAILABLE' | 'FEW_SEATS' | 'SOLD_OUT' | 'QUEUE_ACTIVE'
  description?: string
  aiSimilarityScore?: number
  lineup?: string
  doorsOpen?: string
  venueMapUrl?: string
  functions: EventFunction[]
  sectors: SectorPricing[]
  parking: VenueParking
  paymentPromotions: PaymentPromotion[]
  // Deprecated backwards-compat fields
  availableDates?: string[]
  ticketTypes?: { name: string; price: number }[]
}
```

### 3.3 Mock Events Catalog Proposal (10 Cohesive Buenos Aires Events)

1. **`symphony-of-lights`**
   - Venue: `Arena Buenos Aires (Buenos Aires)`
   - Title: `Symphony of Lights 2026`
   - Subtitle: `Fusión de orquesta sinfónica con visuales inmersivos y audio 360.`
   - Dates: 15 y 16 de Octubre de 2026 (2 funciones: 20:00 hs)
   - Min Price: $35.00
2. **`neon-nights-techno`** (formerly `chronos-fest-2026`)
   - Venue: `Arena Buenos Aires (Buenos Aires)`
   - Title: `Neon Nights: Electronic Festival`
   - Subtitle: `Convergencia de techno melódico con Tale of Us y Boris Brejcha.`
   - Dates: 24 y 25 de Octubre de 2026 (2 funciones: 21:00 hs)
   - Min Price: $35.00
3. **`coldplay-music-spheres`**
   - Venue: `Estadio Monumental (Buenos Aires)`
   - Title: `Coldplay: Music of the Spheres Tour 2026`
   - Subtitle: `El espectáculo en estadio más imponente del planeta con pulseras LED y energía cinética.`
   - Dates: 06, 07 y 08 de Noviembre de 2026 (3 funciones: 21:00 hs, puertas 17:00 hs)
   - Min Price: $40.00
4. **`global-tech-summit`**
   - Venue: `Teatro Rex (Buenos Aires)`
   - Title: `Global Tech Summit Buenos Aires 2026`
   - Subtitle: `Arquitecturas distribuidas, IA generativa y computación de alto desempeño.`
   - Dates: 12 y 13 de Noviembre de 2026 (2 funciones: 09:00 hs, puertas 08:00 hs)
   - Min Price: $25.00
5. **`indie-rock-sessions`**
   - Venue: `Teatro Rex (Buenos Aires)`
   - Title: `Indie Rock Sessions 2026`
   - Subtitle: `Guitarras eléctricas, sintetizadores analógicos y las bandas emergentes de la escena porteña.`
   - Dates: 28 de Noviembre de 2026 (1 función: 20:30 hs)
   - Min Price: $25.00
6. **`river-stadium-rockfest`** (formerly `championship-finals`)
   - Venue: `Estadio Monumental (Buenos Aires)`
   - Title: `Monumental Rock Festival 2026`
   - Subtitle: `Más de 80.000 personas celebrando las leyendas del rock en el estadio más grande de Sudamérica.`
   - Dates: 05 de Diciembre de 2026 (1 función: 16:00 hs, puertas 14:00 hs)
   - Status: `QUEUE_ACTIVE` (con sala de espera habilitada)
   - Min Price: $40.00
7. **`urban-trap-festival`**
   - Venue: `Estadio Monumental (Buenos Aires)`
   - Title: `Urban Flow: Trap & Hip-Hop Fest`
   - Subtitle: `Duki, Bizarrap, Nicki Nicole y Trueno en una noche histórica en Núñez.`
   - Dates: 12 de Diciembre de 2026 (1 función: 18:00 hs, puertas 15:00 hs)
   - Min Price: $40.00
8. **`opera-clasica-verano`** (formerly `symphony-in-code`)
   - Venue: `Teatro Rex (Buenos Aires)`
   - Title: `Noche Lírica: Grandes Obras Maestras`
   - Subtitle: `Solistas internacionales interpretando arias inmortales en la acústica histórica de la calle Corrientes.`
   - Dates: 18 de Enero de 2027 (1 función: 20:00 hs)
   - Min Price: $25.00
9. **`cloud-native-devcon`**
   - Venue: `Arena Buenos Aires (Buenos Aires)`
   - Title: `Cloud Native & Kubernetes Summit 2027`
   - Subtitle: `Conferencia técnica con deep-dives en plataformas distribuidas e infraestructura resiliente.`
   - Dates: 25 y 26 de Enero de 2027 (2 funciones: 09:00 hs)
   - Min Price: $35.00
10. **`electronica-sunset`** (formerly `electronica-open-air`)
    - Venue: `Arena Buenos Aires (Buenos Aires)`
    - Title: `Open Air Sun & Melodic Beats`
    - Subtitle: `Experiencia audiovisual con Ben Böhmer y Lane 8.`
    - Dates: 08 de Febrero de 2027 (1 función: 17:00 hs)
    - Min Price: $35.00

---

## 4. Caveats & Assumptions

1. **Read-Only Scope:** This survey was conducted in read-only mode in accordance with agent boundaries. No application code, test files, or mock data files were modified in `ticketing-frontend`.
2. **Worktree Isolation:** The worktree `.worktrees/frontend-catalog-filters` will be provisioned by the orchestrator/implementer. All edits must take place exclusively within that worktree.
3. **Queue / Checkout Route Parity:** When users click "Comprar" on an individual function row in `EventDetailView.vue`, the navigation target remains:
   - If event status is `QUEUE_ACTIVE`: `router.push({ name: 'queue', params: { id: event.id } })`
   - Otherwise: `router.push({ name: 'seat-selection', params: { id: event.id } })` (optionally passing `functionId` as a query param).
4. **Radix UI Imports:** Radix Vue components (`AccordionRoot`, `AccordionItem`, `AccordionTrigger`, `AccordionContent`) are verified to exist in `node_modules/radix-vue`. No additional NPM dependencies are permitted or needed.

---

## 5. Conclusion

1. **Data Model Deficiencies:** The current mock data violates domain invariants (Chilean locations, missing sector tables, flat string dates). Implementing `mockVenues.ts` and `mockEvents.ts` with structured `EventFunction`, `SectorPricing`, `VenueParking`, and `PaymentPromotion` will immediately satisfy SDD requirements and future-proof the architecture.
2. **Catalog Filters Refactor:** In `EventsCatalogView.vue`, replacing the dynamic venue list with the fixed 3 Buenos Aires venues, adding dynamic future month calculation, and implementing bi-directional `route.query` sync will fully satisfy R3 / SDD 3.2.
3. **Event Detail View Overhaul:** In `EventDetailView.vue`, replacing the two obsolete dropdown selects with Section A ("Entradas desde $X" badge), Section B (explicit function list with "Comprar" buttons), Sections C & D (Radix accordions for payment and parking), and Section E (sector price table) will complete R4 / SDD 3.3.
4. **Home View Badge:** In `HomeView.vue`, adding the **"Entradas desde $X"** badge on event cards in both "Recientemente Agregados" and "Próximos a Ocurrir" sections will fulfill R2 / SDD 3.1.

---

## 6. Verification Method

To verify these findings and validate the subsequent implementation:

1. **Verify Files and Code Locations:**
   - Inspect `ticketing-frontend/src/stores/catalog.ts:6-268` for current Chilean venue strings.
   - Inspect `ticketing-frontend/src/views/EventDetailView.vue:228-313` for the existing `SelectRoot` dropdowns.
   - Inspect `ticketing-frontend/src/views/EventsCatalogView.vue:18-45` for query parsing and month split logic.
2. **Implementation Verification Commands (in `.worktrees/frontend-catalog-filters/ticketing-frontend`):**
   ```powershell
   # 1. Type check verification
   npm run type-check

   # 2. Linter verification
   npm run lint

   # 3. Unit test execution
   npm run test:unit

   # 4. Production build check
   npm run build
   ```
3. **Invalidation Condition:** If `ticketing-frontend` fails `npm run type-check` or `npm run build`, or if any Chilean venue appears in the catalog, the implementation fails the SDD acceptance gate.
