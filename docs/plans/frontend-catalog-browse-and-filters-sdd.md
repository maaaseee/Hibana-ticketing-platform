# Spec-Driven Development (SDD) Task Specification

> **Task ID & Name:** `FRONTEND-CATALOG-AND-EVENT-DETAIL-01`  
> **Bounded Context:** `Event & Venue Catalog (B2C Front-Office Client)`  
> **Target Git Worktree:** `.worktrees/frontend-catalog-filters`  
> **Assigned Agent:** `Frontend Software Engineer`  
> **Author & Reviewer:** `Planner & System Architect`  
> **Status:** `APPROVED & READY FOR IMPLEMENTATION`  

---

## 1. Context & Business Intent

Amaryllis Tickets is a high-concurrency ticketing platform operating exclusively for **3 premier venues located in Buenos Aires, Argentina**:
1. **Estadio Masivo** (Estadio tipo Monumental / River Plate) — Gran escala / recitales masivos al aire libre.
2. **Arena Cubierto** (Arena tipo Movistar Arena) — Conciertos y shows indoor con acústica de precisión.
3. **Teatro Histórico** (Teatro tipo Gran Rex / Ópera) — Obras, musicales y recitales acústicos íntimos.

This specification completely redesigns the catalog browsing, date filters, and the individual event detail view (`EventDetailView.vue`), removing bloated generic e-commerce patterns (no genre pills, no price sliders, no multi-country search) and enforcing the exact domain requirements.

---

## 2. Locked Dependencies Specification

> **CRITICAL RULE:** No agent may introduce dependencies not listed below.

### 2.1 Frontend Dependencies (`package.json`)
Authorized libraries already present in the workspace:
- `vue` (`^3.5.42`)
- `vue-router` (`^5.3.1`)
- `pinia` (`^4.0.3`)
- `radix-vue` (`^1.9.17`)
- `tailwindcss` (`^3.4.17`)

---

## 3. Explicit MUST DO (Requirements & Invariants)

### 3.1 Home View (`HomeView.vue`)
- [ ] Display **Eventos recién salidos a la venta** (*Recently added / On sale now*).
- [ ] Display **Eventos próximos a ocurrir** (*Upcoming events* ordered chronologically).
- [ ] Each event card clearly displays **"Entradas desde $X"** (minimum price extracted from sectors).

### 3.2 Catalog / Shows List View (`EventsCatalogView.vue`)
- [ ] **Search Input:** Debounced text search matching event title, artist, or subtitle.
- [ ] **Month Filter (Selector de Meses):**
  - Dropdown or tab selector displaying upcoming months (up to 8 months into the future, dynamically bounded by the latest event date).
  - Default option: `Todos los meses`.
- [ ] **Venue Filter (Recinto & Localidad Unificados):**
  - Filter by the 3 proprietary venues in Buenos Aires:
    - `Todos los recintos`
    - `Estadio Monumental (Buenos Aires)`
    - `Arena Buenos Aires (Buenos Aires)`
    - `Teatro Rex (Buenos Aires)`
- [ ] **Event Cards:** Show poster, title, venue, date, and **"Entradas desde $X"** badge.
- [ ] **URL Sync:** Synchronize active filters with `route.query` (`?q=...&month=...&venue=...`).

### 3.3 Event Detail View (`EventDetailView.vue`)
- [ ] **Eliminate previous selects:** Delete the redundant/confusing dropdowns for selecting date and general ticket type.
- [ ] **Section A — Base Pricing Headline:** Prominently display **"Entradas desde $X"** in the header banner.
- [ ] **Section B — List of Available Dates/Shows:**
  - Render an explicit list/table where each row represents an available date/function:
    - Full date and day of the week (e.g. `Viernes 15 de Noviembre de 2026`).
    - Exact doors open time (e.g. `Apertura de puertas: 18:00 hs`).
    - Exact show start time (e.g. `Inicio del show: 21:00 hs`).
    - Direct **"Comprar"** button for that specific date that navigates to the checkout / waiting room / seat selection flow.
- [ ] **Section C — Payment Methods Dropdown (Medios de Pago):**
  - Collapsible Radix/Tailwind accordion or dropdown showing accepted credit/debit cards, bank promo installments (e.g. 3 o 6 cuotas sin interés), and digital wallets.
- [ ] **Section D — Parking Dropdown (Estacionamiento):**
  - Collapsible Radix/Tailwind accordion or dropdown detailing official venue parking availability, vehicular access routes, and parking passes.
- [ ] **Section E — Event Description & Sector Price Table:**
  - Rich show narrative and synopsis.
  - Price table mapping every sector from the venue's JSON schema (e.g., *Campo Delantero: $X*, *Platea Baja: $Y*, *Platea Alta: $Z*, *General: $W*).

### 3.4 Worktree Isolation
- [ ] Execute all modifications inside `.worktrees/frontend-catalog-filters` on branch `feature/frontend-catalog-filters`.

---

## 4. Explicit MUST NOT DO (Strict Anti-Patterns & Boundaries)

- ❌ **MUST NOT** render genre pills or category tag filters in the catalog.
- ❌ **MUST NOT** include price range sliders or min/max price inputs in the catalog list.
- ❌ **MUST NOT** present dropdowns for selecting Chilean or international cities (venues are strictly in Buenos Aires, Argentina).
- ❌ **MUST NOT** retain the two old select dropdowns for date and ticket type in `EventDetailView.vue`.
- ❌ **MUST NOT** install third-party UI/calendar libraries (`radix-vue` and `tailwindcss` are sufficient).
- ❌ **MUST NOT** touch backend Java or SQL files within this frontend worktree.

---

## 5. Verification & Acceptance Gate (Executable Checkpoints)

```powershell
# C1: TypeScript type checking passes without errors
npm run type-check

# C2: Linter passes
npm run lint

# C3: Vite production build succeeds
npm run build

# C4: Vitest unit tests pass
npm run test:unit
```
