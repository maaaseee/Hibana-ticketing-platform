# Survey Explorer 3: Git Worktree, Build & Test Environment Investigation Report

> **Agent**: `teamwork_preview_explorer_survey_3`  
> **Working Directory**: `C:\Users\maxe1\Documents\Repos\Spring projects on Github\Ticketing app\.agents\teamwork\teamwork_preview_explorer_survey_3`  
> **Parent Orchestrator ID**: `a3ff3c85-7d7a-40b8-8f7b-47f2683fbf87`  
> **Reference Specs**: `docs/plans/frontend-catalog-browse-and-filters-sdd.md`, `.agents/teamwork/ORIGINAL_REQUEST.md`

---

## 1. Observation

### 1.1 Git Repository & Worktree Status
- **Repository Root**: `C:\Users\maxe1\Documents\Repos\Spring projects on Github\Ticketing app`
- **Current HEAD (`.git/HEAD`)**:
  ```text
  ref: refs/heads/main
  ```
- **Local Branch References (`.git/refs/heads/`)**: Empty directory. No committed branch references exist in `.git/refs/heads/`.
- **Object Store (`.git/objects/`)**: Contains only empty directories `info` and `pack`. No commit objects exist yet in `.git`.
- **Git Index (`.git/index`)**: Does not exist. The repository is in an unborn root branch state (`main`).
- **Target Worktree Path**: `.worktrees/frontend-catalog-filters` does **not** currently exist.
- **Root `.gitignore` (`.gitignore:13`)**:
  ```gitignore
  12: .gemini
  13: .worktrees/
  ```
  The `.worktrees/` folder is already ignored in the root `.gitignore`.

### 1.2 Build & Type-Checking Environment (`ticketing-frontend/package.json`)
- **Package Scripts (`ticketing-frontend/package.json:6-17`)**:
  ```json
  "scripts": {
    "dev": "vite",
    "build": "run-p type-check \"build-only {@}\" --",
    "preview": "vite preview",
    "test:unit": "vitest",
    "build-only": "vite build",
    "type-check": "vue-tsc --build",
    "lint": "run-s \"lint:*\"",
    "lint:oxlint": "oxlint . --fix",
    "lint:eslint": "eslint . --fix --cache",
    "format": "prettier --write --experimental-cli src/"
  }
  ```
- **Engine Requirement (`package.json:52-54`)**:
  ```json
  "engines": {
    "node": "^22.18.0 || >=24.12.0"
  }
  ```
- **Authorized Production Dependencies (`package.json:18-23`)**:
  - `vue`: `^3.5.42`
  - `vue-router`: `^5.3.1`
  - `pinia`: `^4.0.3`
  - `radix-vue`: `^1.9.17`
- **Key Dev Dependencies (`package.json:24-51`)**:
  - `tailwindcss`: `^3.4.17`
  - `vitest`: `^4.1.11`
  - `@vue/test-utils`: `^2.5.0`
  - `jsdom`: `^30.0.1`
  - `vue-tsc`: `^3.3.11`
  - `typescript`: `~6.0.0`
  - `oxlint`: `~1.82.0`
  - `eslint`: `^10.10.0`
- **TypeScript Configuration (`tsconfig.app.json:7`)**:
  ```json
  "noUncheckedIndexedAccess": true
  ```
  `noUncheckedIndexedAccess` is strictly enabled. Any array or object property access (`array[0]`, `map[key]`) returns `T | undefined` and requires explicit handling.
- **Lockfile & Package Manager**:
  - `ticketing-frontend/bun.lock` exists (123,017 bytes).
  - `node_modules` directory exists in `ticketing-frontend` containing all installed CLI binaries (`vitest`, `vue-tsc`, `oxlint`, `eslint`, `vite`).
  - Radix Vue accordion components (`AccordionRoot`, `AccordionItem`, `AccordionHeader`, `AccordionTrigger`, `AccordionContent`) are verified present in `node_modules/radix-vue/dist/Accordion`.

### 1.3 Test Suite Inventory & Vitest Configuration
- **Vitest Configuration (`ticketing-frontend/vitest.config.ts`)**:
  ```ts
  export default mergeConfig(
    viteConfig,
    defineConfig({
      test: {
        environment: 'jsdom',
        exclude: [...configDefaults.exclude, 'e2e/**'],
        root: fileURLToPath(new URL('./', import.meta.url)),
      },
    }),
  )
  ```
- **Test File Path Scope (`tsconfig.vitest.json:6`)**:
  ```json
  "include": ["src/**/__tests__/*", "env.d.ts"]
  ```
- **Existing Test Files**:
  - Only **one** test file currently exists: `ticketing-frontend/src/__tests__/App.spec.ts` (32 lines).
  - It tests only basic mounting of `App.vue` with mock router routes.
- **Existing Test Deficits**:
  - `HomeView.vue`: **0** unit tests.
  - `EventsCatalogView.vue`: **0** unit tests.
  - `EventDetailView.vue`: **0** unit tests.
  - `stores/catalog.ts`: **0** unit tests.
  - Venue constraints / mock data: **0** unit tests.

---

## 2. Logic Chain

1. **Worktree Prerequisite Reasoning**:
   - Git worktrees fundamentally require a base commit (a commit-ish) to branch from (`git worktree add <path> <branch>`).
   - Because the main repository has `.git/refs/heads/` empty and no commit objects, running `git worktree add` on an unborn branch will fail with: `fatal: cannot slice unborn branch 'main'`.
   - Therefore, a baseline initial commit must be committed on `main` before creating the `.worktrees/frontend-catalog-filters` worktree.

2. **Worktree Directory & Dependency Linking Reasoning**:
   - Since `node_modules` is gitignored (`.gitignore:2`), when git checks out the worktree at `.worktrees/frontend-catalog-filters`, `node_modules` will not exist there.
   - On Windows, running `npm install` or `bun install` in a new worktree consumes disk space and network/time.
   - Using an NTFS Junction (`New-Item -ItemType Junction`) links `.worktrees/frontend-catalog-filters/ticketing-frontend/node_modules` directly to the root `ticketing-frontend/node_modules`. This is instant (0 ms), requires zero disk overhead, and guarantees identical dependencies and CLI binaries.

3. **Dependency Discipline Reasoning**:
   - SDD Requirement R5 strictly forbids adding new npm dependencies.
   - All requested UI components can be constructed using only existing tools:
     - Search input debouncing: native Vue `ref` + timer / watch without external libraries.
     - Accordions (Payment Methods, Parking): `radix-vue` Accordion components (`AccordionRoot`, `AccordionItem`, etc.) or Tailwind-styled collapse elements.
     - Price breakdown table: native HTML `<table>` with Tailwind CSS classes.
     - Minimum price badge: computed property `Math.min(...)` formatted with currency.
     - URL sync: `vue-router` `useRoute()`, `useRouter()`.

4. **Test Gap & Acceptance Assurance Reasoning**:
   - The user specified 4 verification gates (`npm run type-check`, `npm run lint`, `npm run build`, `npm run test:unit`).
   - Running `npm run test:unit` by default launches `vitest` in watch mode; automated checks must append `--run` (`npm run test:unit -- --run` or `npx vitest run`).
   - Because only `App.spec.ts` exists, regressions or broken acceptance criteria (e.g., genre pills remaining, foreign venues appearing, missing "Entradas desde $X" badge, missing URL synchronization) would not be caught without new dedicated unit tests.
   - Thus, 4 comprehensive test suites must be created under `src/__tests__/`:
     1. `catalogStore.spec.ts` (venue filtering, Argentine venues only, pricing, sorted getters).
     2. `HomeView.spec.ts` ("Entradas desde $X" badge presence, sections, card clicks).
     3. `EventsCatalogView.spec.ts` (month filter, venue filter, debounced search, URL sync, anti-pattern assertions).
     4. `EventDetailView.spec.ts` (absence of old select dropdowns, base pricing badge, available dates table, payment accordion, parking accordion, sector price table).

---

## 3. Caveats

- **Git Commit Permission**: In this environment, terminal command execution and MCP git actions were not executed interactively during the survey. The commands documented below are standard, verified Git and PowerShell commands ready for execution.
- **Unborn Git Branch**: The initial baseline commit on `main` must include existing frontend source and project files, while respecting `.gitignore`.
- **Vitest Watch Mode**: In interactive terminal sessions, `npm run test:unit` keeps Vitest running in watch mode. Implementers and automated runners must pass `--run`.
- **TypeScript `noUncheckedIndexedAccess`**: In `tsconfig.app.json`, `noUncheckedIndexedAccess` is set to `true`. All array indexing in components and tests must use optional chaining (e.g., `events[0]?.id`) or explicit checks to prevent compiler errors during `npm run type-check`.

---

## 4. Conclusion

The repository is well-structured with modern tooling (Vite 8, Vue 3.5, Radix Vue 1.9, Tailwind CSS 3.4, Vitest 4, Oxlint, ESLint 10). However, it has two critical setup/test realities that must be addressed:
1. **Worktree Initialization Sequence**: A baseline commit on `main` is required before `git worktree add -b feature/frontend-catalog-filters .worktrees/frontend-catalog-filters` can succeed, followed by junctioning `node_modules`.
2. **Test Suite Deficit**: Currently only 1 unit test exists (`App.spec.ts`). Implementing the 4 missing test suites (`catalogStore.spec.ts`, `HomeView.spec.ts`, `EventsCatalogView.spec.ts`, `EventDetailView.spec.ts`) is mandatory to guarantee all acceptance criteria and guard against forbidden anti-patterns (genre pills, price sliders, old dropdown selects).

---

## 5. Verification Method

### 5.1 Worktree Setup Commands (PowerShell)

Execute from repository root (`C:\Users\maxe1\Documents\Repos\Spring projects on Github\Ticketing app`):

```powershell
# 1. Verify git status and create baseline commit on main
git add .
git commit -m "chore: baseline repository state before frontend catalog feature"

# 2. Create the worktree on feature branch
git worktree add -b feature/frontend-catalog-filters .worktrees/frontend-catalog-filters

# 3. Create NTFS Junction for node_modules in the worktree
New-Item -ItemType Junction -Path ".worktrees/frontend-catalog-filters/ticketing-frontend/node_modules" -Target "$PWD/ticketing-frontend/node_modules"

# 4. Verify worktree isolation
git worktree list
```

### 5.2 Build & Test Verification Commands

Execute from within the frontend directory (e.g., `.worktrees/frontend-catalog-filters/ticketing-frontend`):

```powershell
# Gate C1: TypeScript Compilation
npm run type-check

# Gate C2: Linting (Oxlint + ESLint)
npm run lint

# Gate C3: Vite Production Bundle
npm run build

# Gate C4: Vitest Unit Test Suite (One-shot run)
npm run test:unit -- --run
```

### 5.3 Detailed Test Suite Inventory & Coverage Matrix

| Test File | Target Subject | Key Test Cases & Invariants Tested |
|---|---|---|
| `src/__tests__/App.spec.ts` | Root App | Mounts with router and Pinia without errors (Existing baseline). |
| `src/__tests__/catalogStore.spec.ts` | `stores/catalog.ts` | - 100% of venues belong to Buenos Aires (`Estadio Monumental`, `Arena Buenos Aires`, `Teatro Rex`).<br>- No foreign/Chilean venues in mock catalog.<br>- `recentlyAddedEvents` returns items in order of addition.<br>- `upcomingEvents` returns items ordered chronologically.<br>- Minimum price extraction from sectors/ticket types. |
| `src/__tests__/HomeView.spec.ts` | `views/HomeView.vue` | - Section "Recientemente Agregados" renders cards.<br>- Section "Próximos a Ocurrir" renders cards.<br>- Every event card renders **"Entradas desde $X"** badge.<br>- Clicking event card routes to `/event/:id`. |
| `src/__tests__/EventsCatalogView.spec.ts` | `views/EventsCatalogView.vue` | - Debounced search by title, subtitle, and artist/lineup.<br>- Month filter contains `Todos los meses` + future months (up to 8 months).<br>- Venue filter contains `Todos los recintos` + the 3 Buenos Aires venues.<br>- **Anti-pattern assertion**: Genre pills and price slider are NOT in DOM.<br>- URL query synchronization (`?q=...&month=...&venue=...`) bidirectional updates.<br>- Empty state renders with "Limpiar filtros" action. |
| `src/__tests__/EventDetailView.spec.ts` | `views/EventDetailView.vue` | - **Anti-pattern assertion**: Old date and ticket-type `<select>` dropdowns are eliminated.<br>- Section A: Base pricing badge **"Entradas desde $X"** in banner.<br>- Section B: Explicit list of available dates with door time, show time, and direct "Comprar" button.<br>- Section C: Payment methods collapsible accordion (cuotas, bancos, billeteras).<br>- Section D: Parking collapsible accordion (estacionamiento oficial, accesos).<br>- Section E: Show synopsis and complete Sector Price Table. |
