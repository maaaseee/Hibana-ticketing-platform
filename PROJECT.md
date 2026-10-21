# Project: Amaryllis Tickets Frontend Catalog, Filters & Event Detail Refactoring

## Architecture
- **Framework**: Vue 3.5 (Composition API `<script setup>`), Pinia 4, Vue Router 5, Radix Vue 1.9, Tailwind CSS 3.4.
- **Data Flow**:
  - `src/types/index.ts`: Strongly typed interfaces (`EventFunction`, `SectorPricing`, `VenueParking`, `PaymentPromotion`, `VenueInfo`, `EventItem`).
  - `src/data/mockVenues.ts`: Authoritative specs for the 3 Buenos Aires venues (`Estadio Monumental (Buenos Aires)`, `Arena Buenos Aires (Buenos Aires)`, `Teatro Rex (Buenos Aires)`).
  - `src/data/mockEvents.ts`: 10 cohesive events situated exclusively in Buenos Aires with real sector breakdowns, dates/times, and parking/payment info.
  - `src/stores/catalog.ts`: Central state store providing reactive event queries, filtered sets, sorting, and price computations.
  - `src/views/HomeView.vue`: B2C Landing with carousel, recently added events, chronologically upcoming events, and "Entradas desde $X" badges.
  - `src/views/EventsCatalogView.vue`: Filterable catalog with debounced search, 8-month selector, 3 Buenos Aires venues filter, and bi-directional `route.query` sync (`?q=...&month=...&venue=...`).
  - `src/views/EventDetailView.vue`: Comprehensive event presentation with Sections A through E and no obsolete dropdown selects.
  - `src/__tests__/`: Vitest test suites covering store logic, view rendering, anti-pattern assertions, and URL reactivity.

## Feature Inventory
| # | Feature | Description | Milestone | Source |
|---|---------|-------------|-----------|--------|
| 1 | Git Worktree Isolation | Create `.worktrees/frontend-catalog-filters` on `feature/frontend-catalog-filters` with node_modules junction | M1 | SDD §2, R1 |
| 2 | Data Models & Types | Define `EventFunction`, `SectorPricing`, `VenueParking`, `PaymentPromotion`, `VenueInfo`, `EventItem` in `src/types/index.ts` | M2 | SDD §1, R5 |
| 3 | Buenos Aires Venues Fixture | Create `src/data/mockVenues.ts` with Estadio Monumental, Arena Buenos Aires, Teatro Rex | M2 | SDD §1, R3 |
| 4 | Buenos Aires Events Fixture | Create `src/data/mockEvents.ts` with 10 Buenos Aires events, functions, sectors, parking, payment info | M2 | SDD §1, R3 |
| 5 | Catalog Store Refactoring | Update `src/stores/catalog.ts` to consume mock fixtures, provide `getMinEventPrice`, sort getters | M2 | SDD §1 |
| 6 | Home View Badges & Ordering | Update `HomeView.vue` with "Entradas desde $X" badges and chronological upcoming events | M3 | SDD §3.1, R2 |
| 7 | Catalog Debounced Search | Implement 300ms debounced search matching title, subtitle, and artist/lineup | M3 | SDD §3.2, R3 |
| 8 | Catalog Month Selector | Dynamic selector covering up to 8 months into future | M3 | SDD §3.2, R3 |
| 9 | Catalog Venue Filter | Filter for the 3 Buenos Aires venues (`Estadio Monumental`, `Arena Buenos Aires`, `Teatro Rex`) | M3 | SDD §3.2, R3 |
| 10 | Catalog Anti-Pattern Removal | Omit genre pills and price range slider in `EventsCatalogView.vue` | M3 | SDD §3.2, R3 |
| 11 | Catalog URL Query Sync | Bidirectional reactive synchronization with `route.query` (`?q=...&month=...&venue=...`) | M3 | SDD §3.2, R3 |
| 12 | Catalog Min Price Badges | Prominently display "Entradas desde $X" badge on all catalog event cards | M3 | SDD §3.2 |
| 13 | Detail Remove Dropdowns | Remove obsolete date and ticket-type select dropdowns in `EventDetailView.vue` | M4 | SDD §3.3, R4 |
| 14 | Detail Section A Base Pricing | Display prominent "Entradas desde $X" badge in panoramic header banner | M4 | SDD §3.3.A, R4 |
| 15 | Detail Section B Dates List | Render table/list of available dates with doors open, show time, and direct "Comprar" buttons | M4 | SDD §3.3.B, R4 |
| 16 | Detail Section C Payment Accordion | Collapsible Radix Vue accordion for credit/debit cards and bank installment promos | M4 | SDD §3.3.C, R4 |
| 17 | Detail Section D Parking Accordion | Collapsible Radix Vue accordion for official venue parking and access routes | M4 | SDD §3.3.D, R4 |
| 18 | Detail Section E Sector Price Table | Sector pricing breakdown table mapping venue sectors, types, prices, and status | M4 | SDD §3.3.E, R4 |
| 19 | Unit Test Suites Creation | Create `catalogStore.spec.ts`, `HomeView.spec.ts`, `EventsCatalogView.spec.ts`, `EventDetailView.spec.ts` | M5 | SDD §4, AC |
| 20 | Quality & Build Gate Checks | Pass `npm run type-check`, `npm run lint`, `npm run build`, `npm run test:unit -- --run` | M5 | SDD §4, AC |

## Milestones
| # | Name | Scope | Dependencies | Status |
|---|------|-------|-------------|--------|
| 1 | M1: Worktree Setup & Git Isolation | Create baseline commit on main, branch `feature/frontend-catalog-filters`, worktree `.worktrees/frontend-catalog-filters`, junction `node_modules` | none | BLOCKED: Terminal permission prompt timed out on git commands |
| 2 | M2: Data Models & Buenos Aires Mock Data | Update `src/types/index.ts`, create `src/data/mockVenues.ts`, `src/data/mockEvents.ts`, update `src/stores/catalog.ts` | M1 | PLANNED |
| 3 | M3: Home View & Catalog Shows Refinements | Update `HomeView.vue` and `EventsCatalogView.vue` with debounced search, 8-month filter, 3 BA venues, URL sync, min price badges | M2 | PLANNED |
| 4 | M4: Event Detail View Overhaul | Overhaul `EventDetailView.vue` with Sections A-E, remove old dropdown selects | M2 | PLANNED |
| 5 | M5: Test Suites & Full Acceptance Verification | Implement 4 Vitest test suites, verify `type-check`, `lint`, `build`, and `test:unit` pass with 0 errors | M3, M4 | PLANNED |

## Interface Contracts
### Data Models (`src/types/index.ts`) ↔ Store (`src/stores/catalog.ts`)
- `EventItem` must contain:
  - `id: string`, `title: string`, `subtitle: string`, `date: string`, `dateIso: string`
  - `venue: string` (one of the 3 Buenos Aires venues), `venueId: string`
  - `location: string` ("Buenos Aires, Argentina")
  - `price: number`, `minPrice: number`
  - `functions: EventFunction[]` with `id`, `dateIso`, `formattedDate`, `doorsOpen`, `showStart`, `status`
  - `sectors: SectorPricing[]` with `id`, `name`, `price`, `currency`, `type`, `status`
  - `parking: VenueParking` with `available`, `title`, `description`, `accessRoutes`
  - `paymentPromotions: PaymentPromotion[]` with `id`, `bank`, `card`, `installments`, `description`
  - Backwards compatibility: `availableDates?: string[]`, `ticketTypes?: { name: string; price: number }[]`

### Store (`src/stores/catalog.ts`) ↔ Views (`HomeView.vue`, `EventsCatalogView.vue`, `EventDetailView.vue`)
- Helper: `getMinEventPrice(event: EventItem): number` returns minimum sector price or fallback price.
- Getters: `recentlyAddedEvents`, `upcomingEvents` (sorted chronologically by `dateIso`).

### Views ↔ Router
- `EventsCatalogView.vue` synchronizes filters with `route.query`:
  - `q`: search query string
  - `month`: selected month label string
  - `venue`: selected venue name string

## Code Layout
All changes operate strictly within the worktree:
`.worktrees/frontend-catalog-filters/ticketing-frontend/`
- `src/types/index.ts`: TypeScript interfaces and models
- `src/data/mockVenues.ts`: Venue definitions (Monumental, Arena, Rex)
- `src/data/mockEvents.ts`: 10 mock events with functions, sectors, parking, payment info
- `src/stores/catalog.ts`: Pinia store logic and getters
- `src/views/HomeView.vue`: Landing page with badges and sections
- `src/views/EventsCatalogView.vue`: Catalog with debounced search, month & venue filters, URL sync
- `src/views/EventDetailView.vue`: Event detail with Sections A-E
- `src/__tests__/catalogStore.spec.ts`: Store and data invariant tests
- `src/__tests__/HomeView.spec.ts`: Home view tests
- `src/__tests__/EventsCatalogView.spec.ts`: Catalog view and filter tests
- `src/__tests__/EventDetailView.spec.ts`: Event detail view tests
