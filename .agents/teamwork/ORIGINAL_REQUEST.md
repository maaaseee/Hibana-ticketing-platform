# Original User Request

## Initial Request — 2026-10-21T05:23:13Z

Execute the frontend catalog browse, month filtering, and event detail view refactoring for Amaryllis Tickets in an isolated Git worktree, strictly adhering to the Spec-Driven Development (SDD) specification.

Working directory: `C:\Users\maxe1\Documents\Repos\Spring projects on Github\Ticketing app` (operating inside `.worktrees/frontend-catalog-filters`)  
Integrity mode: development  

Specification Reference: docs/plans/frontend-catalog-browse-and-filters-sdd.md

## Requirements

### R1. Git Worktree Isolation
Create and operate exclusively inside `.worktrees/frontend-catalog-filters` on branch `feature/frontend-catalog-filters`. No edits are allowed in the main working tree or backend code.

### R2. Home View Refinements (`HomeView.vue`)
- Showcase **Eventos recién salidos a la venta** (*Recently added / On sale now*).
- Showcase **Eventos próximos a ocurrir** (*Upcoming events* ordered chronologically).
- Display a prominent **"Entradas desde $X"** badge on each event card using the minimum sector price.

### R3. Catalog Shows & Filter Experience (`EventsCatalogView.vue`)
- **Text Search:** Debounced search query matching title, artist, or subtitle.
- **Month Selector:** Filter events by month up to 8 months into the future.
- **Venue Filter:** Filter by the 3 proprietary Buenos Aires venues (`Estadio Monumental`, `Arena Buenos Aires`, `Teatro Rex`).
- **No Genre Pills & No Price Slider:** Omit genre pills and price range sliders in the catalog view.
- **URL Synchronization:** Synchronize active search query, month, and venue with `route.query`.

### R4. Event Detail View Overhaul (`EventDetailView.vue`)
- **Remove Redundant Selects:** Remove the old date and ticket-type select dropdowns.
- **Base Pricing:** Prominently display **"Entradas desde $X"** in the event banner.
- **Available Dates List:** Render each date/function as an explicit row with doors open time, show start time, and a direct "Comprar" button navigating to the purchase/waiting room flow.
- **Payment Methods Accordion:** Dropdown detailing accepted credit/debit cards and installment promotions.
- **Parking Accordion:** Dropdown detailing official venue parking availability and access routes.
- **Sector Price Table:** Render description alongside a pricing breakdown table for all sectors defined in the venue JSON.

### R5. Locked Dependencies
Use only pre-existing dependencies in `package.json` (`vue`, `vue-router`, `pinia`, `radix-vue`, `tailwindcss`). Do not add new npm dependencies.

## Acceptance Criteria

### Build & Type Safety
- [ ] `npm run type-check` succeeds with 0 TypeScript compilation errors in `ticketing-frontend`.
- [ ] `npm run lint` passes without ESLint/Oxlint errors.
- [ ] `npm run build` generates a production Vite bundle without warnings.
- [ ] `npm run test:unit` passes all existing and updated test cases.

### Functional Verification
- [ ] Catalog filtering by month and venue reactively updates displayed events.
- [ ] Navigating to `/events/:id` displays the new structured layout without old dropdown selects.
- [ ] All 3 Buenos Aires venues are correctly represented in mock data and venue filters.
