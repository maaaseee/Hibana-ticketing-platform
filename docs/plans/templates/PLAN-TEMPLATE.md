# Implementation Plan: `[Module Name]`

<!--
  HOW TO USE THIS TEMPLATE
  ─────────────────────────
  1. Copy this file to docs/plans/[module-name]-plan.md
  2. Fill in every [PLACEHOLDER] in the header and sections below.
  3. Delete any milestone that does not apply to your module type.
  4. Add module-specific checkpoints under each milestone.
  5. Keep "Deferred" and "Decisions" tables updated as you build.

  Reference implementations:
  - Spring Boot microservice  → docs/plans/catalog-service-plan.md
  - Microservice skeleton     → docs/plans/MICROSERVICE-PLAN-TEMPLATE.md
-->

---

## Module Header

> **Module:** `[module-name]`
> **Type:** `[Backend Service / Frontend App / Mobile / AI-ML Pipeline / CLI Tool]`
> **Tech Stack:** `[Language + Runtime] · [Framework] · [Build Tool]`
> **Primary Integration:** `[REST API / Database / Message Broker / AI Model / CDN]` at `[host:port or URL]`
> **Consumer:** `[Who or what consumes the output of this module]`
> **Goal:** _One sentence: what this module does and why it matters._

---

## Pre-Flight Checklist

> Verify the environment is ready before writing any code. Nothing moves forward until this is clean.

- [ ] **Runtime installed:** `[node --version / java -version / python --version]` returns expected version.
- [ ] **Dependencies resolved:** `[npm install / .\gradlew.bat build / pip install -r requirements.txt]` exits 0.
- [ ] **Local services running:** Required containers/services are healthy (`docker compose ps`).
- [ ] **Credentials & config available:** `.env.local` or `application-dev.yml` exists with required variables.
- [ ] **Target directory created:** `[apps/b2c-web / services/catalog-service / ml/recommender]` exists.

---

## Milestone 1 — Scaffold & Baseline

> **Goal:** The module exists, its entry point runs, and the pipeline (dev server / app boot / script execution) works end-to-end with zero features.

### Tasks

- [ ] **1.1 — Project initialization:** `[npm create vite@latest / gradle init / cookiecutter / poetry new]`
- [ ] **1.2 — Runtime configuration:** Create `[.env.local / application.yml / config.py / .env]` with all required environment variables.
- [ ] **1.3 — Entry point:** Create `[main.ts / Application.java / app.py / index.js]` — bare minimum to start.
- [ ] **1.4 — Linting & formatting:** Configure `[ESLint + Prettier / Checkstyle / ruff / gofmt]`.

### ✅ Milestone 1 Checkpoint

```bash
# C1: Runtime version
[node --version / java -version / python --version]
# Expected: [vX.Y.Z]

# C2: Dependency install / build succeeds
[npm install / .\gradlew.bat clean compileJava / pip install -r requirements.txt]
# Expected: EXIT CODE 0, no errors

# C3: Entry point starts / script runs
[npm run dev / .\gradlew.bat bootRun / python main.py]
# Expected: [Server running at http://localhost:PORT / App started / Script completed]
```

> [!IMPORTANT]
> All three commands must exit with code 0 and produce the expected output before moving forward.

---

## Milestone 2 — Core Logic, Use Case Grouping & Custom Exceptions

> **Goal:** The module's primary logic exists, use cases are grouped into dedicated subpackages/directories, domain invariants throw custom typed exceptions (zero generic exceptions), and logic is independently testable without external I/O.

### Tasks

- [ ] **2.1 — Core models / types:** Define data structures, interfaces, records, or domain classes that represent the module's primary entities and encapsulate business invariants.
- [ ] **2.2 — Use Case Subfolder Grouping:**
  > [!IMPORTANT]
  > **Atomic Feature / Use Case Folders:** Group each business use case, workflow, or state transition into its own isolated directory (e.g. `application/query/<use_case>/`, `application/command/<use_case>/`, `features/<feature_name>/`, `stores/<slice>/`). Never dump flat, unrelated handlers or actions into a single root directory.
  - Structure request/command, handler, and response types together within the use case package.
- [ ] **2.3 — Typed Custom Exceptions (Strictly Ban Generic Exceptions):**
  > [!CAUTION]
  > **Ban Generic Exceptions:** Do NOT throw generic `RuntimeException`, `Error`, `IllegalArgumentException`, or `IllegalStateException` in business logic.
  - Define explicit, domain-specific exception types (e.g., `[Entity]NotFoundException`, `Invalid[Entity]ScheduleException`, `Invalid[Entity]PriceException`).
  - Ensure every failure mode has an explicit reason, enabling granular tracing, distinct HTTP status codes, and accurate APM metrics.
- [ ] **2.4 — Pure In-Memory Unit Tests:** Write tests exercising the core logic and custom exception throws with zero external I/O (mocked/stubbed dependencies).

### ✅ Milestone 2 Checkpoint

```bash
# C4: Compile / type-check passes
[npm run type-check / .\gradlew.bat compileJava / mypy src/]
# Expected: EXIT CODE 0, zero type errors

# C5: Architecture purity guard (adapt to your stack)
# Example: no framework imports in pure logic layer
grep -r "[FrameworkImport]" [path/to/core/logic/]
# Expected: NO OUTPUT

# C6: Unit tests pass
[npm test / .\gradlew.bat test --tests "*UnitTest" / pytest tests/unit/]
# Expected: All tests GREEN, zero failures (< 1s)
```

> [!IMPORTANT]
> **C5 is non-negotiable.** The core logic layer must be independently testable. If it can only run inside the framework, it is already coupled to infrastructure.

---

## Milestone 3 — Integration / Data Layer

> **Goal:** The module reads/writes real data from its actual datasource, external API, or message broker — connected to local emulated services.

### Tasks

- [ ] **3.1 — Data access / client layer:** Implement `[Repository / API client / Pinia store fetch / DB adapter / model loader]`.
- [ ] **3.2 — Connection configuration:** Wire credentials and connection strings from environment variables.
- [ ] **3.3 — Seed / fixture data:** Create realistic local data that matches the expected shape from the primary consumer (frontend wireframe, API contract, training data schema).
- [ ] **3.4 — Integration test:** Write at least one test that hits the real (local/emulated) datasource.

### ✅ Milestone 3 Checkpoint

```bash
# C7: Schema / data structure validation
# (Backend: JPA validate mode / Frontend: API contract shape check / ML: dataset schema assert)
[.\gradlew.bat test --tests "*ApplicationTests" / npm run validate-api / python -m pytest tests/data/]
# Expected: No structural mismatch errors

# C8: Seed data is accessible
[docker exec postgres psql -c "SELECT count(*) FROM table;" / curl http://localhost:4577/cosmos/... / ls data/fixtures/]
# Expected: Records/files present

# C9: Integration test against live local service
[.\gradlew.bat test --tests "*IT" / npm run test:integration / pytest tests/integration/]
# Expected: All tests GREEN
```

> [!IMPORTANT]
> **C7 is the structural contract test.** It must run against the real local service — not mocks — and confirm the data shape your code expects actually matches what the service provides.

---

## Milestone 4 — Public Contract, Native Versioning & Structured Errors

> **Goal:** The module exposes its functionality to its consumer (REST endpoint, rendered UI, exported model artifact, CLI output) with declarative API versioning, dumb controllers/mediators, and structured RFC 7807 error responses.

### Tasks

- [ ] **4.1 — Declarative API Versioning (No Hardcoded "v1" in Route Paths):**
  > [!IMPORTANT]
  > **Clean Route Paths:** Never hardcode version strings (e.g. `/v1/`) directly in controller `@RequestMapping` annotations or individual endpoint path strings.
  - In Spring Boot 4: Configure `WebMvcConfigurer` with `ApiVersionConfigurer` (`usePathSegment(1)`) and `PathMatchConfigurer` (`addPathPrefix("/api/v{version}", ...)`).
  - Declare version via framework annotations (e.g., `@GetMapping(version = "1.0")`).
  - In Frontend/Clients: Configure API base paths dynamically via environment variables or central API client version interceptors.
- [ ] **4.2 — Output Surface & Dispatching:** Create `[REST Controller / Vue view / React component / CLI command]`.
  - For Spring microservices: Controllers inject only `Mediator` and delegate to `mediator.dispatch(...)`.
  - Accept and return only dedicated, immutable DTOs; never leak internal entities or state models.
- [ ] **4.3 — Granular Error Handling & Machine-Readable Error Codes:**
  - Map each custom domain/CQRS exception to a structured error representation (e.g. RFC 7807 `ProblemDetail`, UI banner/toast).
  - Explicitly include a machine-readable `errorCode` property (e.g. `EVENT_NOT_FOUND`, `INVALID_SCHEDULE`), HTTP status code, and structured logging for observability and alerting.
- [ ] **4.4 — Response Shape:** Ensure output matches the exact contract the consumer expects.

### ✅ Milestone 4 Checkpoint (End-to-End)

```bash
# C10: Primary happy-path works end to end via versioned route
[curl -s http://localhost:PORT/api/v1/[resource] / open http://localhost:5173 in browser / python predict.py --input sample.json]
# Expected: [200 OK with JSON / UI renders correctly / Predictions output to stdout]

# C11: Primary read / display returns seeded/expected data
# Expected: Output matches data from Milestone 3 seed — titles, IDs, shapes align

# C12: Invalid input returns structured error with errorCode
[curl -s http://localhost:PORT/api/v1/[resource]/nonexistent / submit empty form / python predict.py --input bad.json]
# Expected: [RFC 7807 ProblemDetail with errorCode / user-visible error message / structured error log]

# C13: Boundary guard — internal types do not leak to the output surface
grep -r "[InternalType/JpaEntity/StoreState]" [path/to/output/layer/]
# Expected: NO OUTPUT
```

> [!CAUTION]
> **C13 must return zero matches.** Internal implementation types (DB entities, raw model weights, internal store state) must never appear in the public output surface.

---

## Milestone 5 — Full Testing Coverage & Reliability Verification

> **Goal:** Complete test suite covering behavior specifications, pure isolated logic, real persistence integrations, concurrent race condition safety, and retry idempotency.

### Tasks

- [ ] **5.1 — Gherkin Scenarios (Fully Commented):**
  Create behavioral test specifications (`[module].feature` / BDD specs) with detailed comments explaining the business intent behind each rule:
  - Formulate *Given-When-Then* scenarios covering standard paths, error flows, and boundary edge cases.
  - Add explanatory comments above each scenario detailing the business rule being validated.

- [ ] **5.2 — Unit Tests Without I/O:**
  Create in `[test/unit / tests/unit / src/**/*.spec.ts]`:
  - Pure in-memory unit tests with zero disk access, zero network calls, zero external DB connections, and zero heavy framework contexts.
  - Verify deterministic logic, state mutations, and pure calculation rules asserting custom exceptions.

- [ ] **5.3 — Integration Tests with Real Services / Database:**
  Create integration tests executing against actual running local containers (Postgres, Redis, Cosmos emulator, or live mock server):
  - Verify real wire protocols, schema queries, data persistence, and query execution paths.

- [ ] **5.4 — Parallel Concurrency Stress Tests:**
  Create concurrency tests simulating simultaneous operations on the exact same record or state:
  - Trigger parallel threads or asynchronous coroutines competing for the same resource (e.g. 50 parallel Virtual Threads / `CountDownLatch`).
  - Assert that atomic locks, versioning (`@Version` / ETags), or mutexes prevent race conditions, dirty reads, or state corruption.

- [ ] **5.5 — Idempotency Against Retries:**
  Create retry resilience tests:
  - Dispatch identical commands, requests, or events multiple times (20-30 iterations) using the same correlation / idempotency key.
  - Assert that duplicate deliveries produce identical end states and generate zero duplicate records or unwanted side effects.

### ✅ Milestone 5 Checkpoint

```bash
# C14: In-memory unit tests run with zero I/O
[npm run test:unit / .\gradlew.bat test --tests "*UnitTest" / pytest -m "not io"]
# Expected: Sub-second execution, zero network or database dependencies

# C15: Gherkin / BDD feature scenarios pass
[npx cucumber-js / .\gradlew.bat test --tests "*CucumberTestRunner" / pytest-bdd]
# Expected: All behavioral steps GREEN

# C16: Integration tests against live datastore/services pass
[.\gradlew.bat test --tests "*IT" / npm run test:integration / pytest tests/integration/]
# Expected: Real DB queries and mappings verified cleanly

# C17: Concurrency test confirms race condition resilience
[.\gradlew.bat test --tests "*ConcurrencyIT" / npm run test:concurrency / k6 run concurrency-test.js]
# Expected: Parallel hits on the same record complete without data corruption or deadlocks

# C18: Idempotency test confirms retry safety
[.\gradlew.bat test --tests "*IdempotencyIT" / npm run test:idempotency / pytest tests/test_idempotency.py]
# Expected: Duplicate calls with identical key produce identical result and no duplicate rows
```

---

## Definition of Done

> Every checkpoint must be green. Update status as you go.

| # | Checkpoint | Status |
|---|---|---|
| C1 | Runtime version confirmed | ⬜ |
| C2 | Dependency install / build exits 0 | ⬜ |
| C3 | Entry point starts or script executes | ⬜ |
| C4 | Compile / type-check passes | ⬜ |
| C5 | Architecture purity guard (zero forbidden imports) | ⬜ |
| C6 | Core logic unit tests GREEN | ⬜ |
| C7 | Schema / data structural contract validated | ⬜ |
| C8 | Seed data accessible in local service | ⬜ |
| C9 | Integration test against live local service GREEN | ⬜ |
| C10 | Primary happy-path works end to end | ⬜ |
| C11 | Primary read / display returns seeded data | ⬜ |
| C12 | Structured error response on invalid input (`errorCode`) | ⬜ |
| C13 | Output surface boundary guard (no internal leakage) | ⬜ |
| C14 | Unit tests run in-memory without I/O | ⬜ |
| C15 | Fully commented Gherkin BDD scenarios GREEN | ⬜ |
| C16 | Integration tests against live datastore GREEN | ⬜ |
| C17 | Concurrency test (parallel load on same record) GREEN | ⬜ |
| C18 | Idempotency test (retries with identical key) GREEN | ⬜ |

---

## Deferred Scope

| Item | Reason |
|---|---|
| _[Feature X]_ | _[Explicit reason why it is deferred — e.g., depends on module Y contract]_ |

---

## Notes & Decisions

| Decision | Rationale |
|---|---|
| _[e.g., Declarative API Versioning]_ | _[e.g., Centralized in WebMvcConfigurer / gateway; eliminates path string coupling]_ |
| _[e.g., Subfolder Use Case Grouping]_ | _[e.g., Keeps command/query, handler, and DTO cohesive in an atomic directory slice]_ |
| _[e.g., Custom Domain Exceptions]_ | _[e.g., Banned generic exceptions to provide distinct errorCodes and clear APM metrics]_ |
