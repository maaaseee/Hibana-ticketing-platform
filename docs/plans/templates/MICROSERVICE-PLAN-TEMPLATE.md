# Microservice Implementation Plan Template

> **How to use this document:**
> Copy this file, rename it `docs/plans/[service-name]-plan.md`, and fill in every section.
> Each milestone must be completed and all checkpoints (C#) verified before proceeding to the next one.
> See [catalog-service-plan.md](./catalog-service-plan.md) as a complete reference implementation.

---

## Service Header

> **Service:** `com.aieta.[service_name]`
> **Stack:** Java 25 · Spring Boot 4 · Hexagonal Architecture (Ports & Adapters) · CQRS with Mediator
> **Build Tool:** Gradle (`gradlew.bat`) with Java 25 toolchain
> **Primary Datastore:** `[PostgreSQL 17 / Cosmos DB NoSQL / Redis]` on `[host:port/db]`
> **Goal:** _One sentence describing what this service does and who consumes it._

---

## Pre-Flight Checklist (Before Writing Code)

> Verify infrastructure readiness before any code is written.

- [ ] **Infrastructure running:** `docker compose ps` shows all required containers as **healthy**.
- [ ] **Schema applied:** `[path/to/schema.sql]` executed → `[table_a]`, `[table_b]`, `[outbox_events]` tables exist.
- [ ] **Java 25 on PATH:** `java -version` returns `25.x.x`.
- [ ] **Project directory created:** `[service-name]/` exists with Gradle Wrapper.

---

## Milestone 1 — Scaffold & Runtime Baseline

> **Goal:** A Spring Boot 4 app that starts, connects to its datastore, and responds to `/actuator/health`.

### Tasks

- [ ] **1.1 — `build.gradle`:** Spring Boot 4 plugin, Java 25 toolchain (`JavaLanguageVersion.of(25)`), core dependencies (`spring-boot-starter-webmvc`, `spring-boot-starter-data-jpa`, `spring-boot-starter-actuator`, `spring-boot-starter-validation`, `postgresql`), and Gradle Wrapper (`gradlew.bat`).
- [ ] **1.2 — `application.yml`:** Virtual Threads (`spring.threads.virtual.enabled: true`), datastore connection, Actuator endpoints (`health,metrics,prometheus`).
- [ ] **1.3 — `[ServiceName]Application.java`:** Spring Boot entry point at `com.aieta.[service_name]`.

### ✅ Milestone 1 Checkpoint

```powershell
# C1: Gradle Wrapper works with Java 25
.\gradlew.bat -v
# Expected: Gradle x.x + JVM: 25.x.x

# C2: Project compiles cleanly
.\gradlew.bat clean compileJava
# Expected: BUILD SUCCESSFUL, exit code 0

# C3: Application starts and Actuator confirms datastore connection
curl http://localhost:[PORT]/actuator/health
# Expected: {"status":"UP","components":{"db":{"status":"UP"},...}}
```

> [!IMPORTANT]
> The `db` (or equivalent) component inside `actuator/health` **must** be `UP` before moving forward.

---

## Milestone 2 — Domain Core, CQRS Mediator & Use Case Folders

> **Goal:** Pure Java 25 domain model, custom domain exceptions, in-memory Mediator bus, and isolated CQRS use-case packages. Zero framework dependencies inside `domain/`.

### Tasks

- [ ] **2.1 — Enums & Value Objects:** `domain/entity/` — immutable Java records and status enums encapsulating business invariants.
- [ ] **2.2 — Custom Domain Exceptions:** `domain/exceptions/` — typed, business-specific exceptions.
  > [!CAUTION]
  > **Ban generic exceptions:** Do NOT use `RuntimeException`, `IllegalArgumentException`, or `IllegalStateException` in domain or application logic. Create custom domain exceptions (e.g., `[Entity]NotFoundException`, `Invalid[Entity]ScheduleException`, `Invalid[Entity]PricingException`) so error traces and metrics are granular.
- [ ] **2.3 — Outbound Port:** `domain/port/[Entity]RepositoryPort.java` — pure Java interface with domain operations.
- [ ] **2.4 — In-Memory Mediator Bus:** `common/mediator/` — core CQRS infrastructure (per [CQRS_ARCHITECTURE.md](../CQRS_ARCHITECTURE.md)):
  - `Request<T>`: Generic request marker strongly typed to return type `T`.
  - `RequestHandler<T extends Request<R>, R>`: Generic handler contract.
  - `Mediator.java`: Central $O(1)$ dispatching component.
  - `HandlerNotFoundException.java`: Typed exception for unregistered requests.
- [ ] **2.5 — Isolated CQRS Use Case Packages:** Under `application/query/[use_case_name]/` and `application/command/[use_case_name]/`:
  > [!IMPORTANT]
  > **Use Case Subfolder Isolation:** Every use case must be grouped into its own dedicated subpackage (e.g. `application/query/get_upcoming_events/`, `application/command/create_event/`). Never keep flat files directly in `application/query/` or `application/command/`.
  - `[Action]Query.java` (record implementing `Request<R>`)
  - `[Action]QueryHandler.java` (service implementing `RequestHandler<[Action]Query, R>`, `@Transactional(readOnly = true)`)

### ✅ Milestone 2 Checkpoint

```powershell
# C4: Full compile after domain & mediator layer
.\gradlew.bat clean compileJava
# Expected: BUILD SUCCESSFUL, exit code 0

# C5: Domain purity guard — zero Spring/JPA imports in domain/
grep -r "springframework\|jakarta.persistence" src/main/java/com/aieta/[service_name]/domain/
# Expected: NO OUTPUT (zero matches — Cardinal Rule violation if any found)

# C6: Pure domain unit tests pass with zero I/O
.\gradlew.bat test --tests "*DomainUnitTest"
# Expected: All domain invariant tests GREEN in < 1s
```

> [!IMPORTANT]
> **C5 must return zero matches.** Any Spring or JPA annotation in `domain/` violates the Agnostic Domain Core rule from the [spring-boot-4-microservice skill](.gemini/skills/spring-boot4-microservices/SKILL.md).

---

## Milestone 3 — Persistence Adapter

> **Goal:** Wire the domain port to the live datastore container via JPA adapters. Seed realistic data.

### Tasks

- [ ] **3.1 — JPA Entities:** `infrastructure/database/[Entity]JpaEntity.java` — `@Entity`, `@Table`, `@Version` for optimistic locking. Types match the SQL DDL exactly.
- [ ] **3.2 — Spring Data Repository:** `infrastructure/database/SpringData[Entity]Repository.java` — internal, never exposed outside `infrastructure/`.
- [ ] **3.3 — Mapper:** `infrastructure/database/mapper/[Entity]Mapper.java` — converts JPA entity ↔ domain record.
- [ ] **3.4 — Port Adapter:** `infrastructure/database/Jpa[Entity]RepositoryAdapter.java` — `@Component`, implements `[Entity]RepositoryPort`.
- [ ] **3.5 — Dev Seed Data:** `src/main/resources/data-dev.sql` — realistic rows matching the frontend wireframe data contract.

### ✅ Milestone 3 Checkpoint

```powershell
# C7: JPA schema validation — entities match live DDL
.\gradlew.bat test --tests "*ApplicationTests"
# Expected: App starts without HibernateException or MappingException (ddl-auto: validate)

# C8: Seed data is present in the database
docker exec [postgres-container] psql -U [user] -d [db] -c "SELECT id FROM [table] LIMIT 5;"
# Expected: Rows returned matching seeded data

# C9: Integration test exercises adapter against live container
.\gradlew.bat test --tests "*RepositoryIT"
# Expected: BUILD SUCCESSFUL, live DB queries pass
```

> [!IMPORTANT]
> **C7 (JPA validate mode) is mandatory.** It is the only reliable guard against silent column-type drift between your Java `@Column` annotations and the SQL DDL.

---

## Milestone 4 — REST API, Native Versioning & RFC 7807

> **Goal:** HTTP endpoints matching frontend contracts using Spring Boot 4 native versioning, Mediator dispatching, and RFC 7807 error responses.

### Tasks

- [ ] **4.1 — Response DTOs:** `infrastructure/api/dto/[Entity]Response.java` — immutable Java records mapping domain projections to the wire format.
- [ ] **4.2 — Native Spring Boot 4 API Versioning:** `infrastructure/configuration/WebVersioningConfig.java` implementing `WebMvcConfigurer`:
  - `configureApiVersioning(ApiVersionConfigurer configurer)`: Configure `usePathSegment(1)`, `addSupportedVersions("1.0", "1")`, and `setDefaultVersion("1.0")`.
  - `configurePathMatch(PathMatchConfigurer configurer)`: Register dynamic prefix `addPathPrefix("/api/v{version}", HandlerTypePredicate.forAnnotation(RestController.class))`.
- [ ] **4.3 — Controller with Mediator:** `infrastructure/api/[Resource]Controller.java`:
  > [!IMPORTANT]
  > **No Hardcoded "v1" in Controller Paths:** Use `@RequestMapping("/[resources]")`. Declare version on methods using the Spring Boot 4 attribute (e.g., `@GetMapping(version = "1.0")`).
  > **Dumb Controllers:** Controllers must inject only `Mediator` and delegate execution exclusively to `mediator.dispatch(...)`.
- [ ] **4.4 — Granular RFC 7807 Global Exception Handler:** `infrastructure/api/exception/GlobalExceptionHandler.java`:
  - Dedicated `@ExceptionHandler` for each custom domain and CQRS exception.
  - Return RFC 7807 `ProblemDetail` with dedicated URI `type`, `title`, HTTP status, `timestamp`, and distinct `errorCode` property.
  - Structured SLF4J logging (`log.warn` / `log.error`) to support APM metrics and log filtering.

### ✅ Milestone 4 Checkpoint (End-to-End)

```powershell
# C10: Primary read endpoint returns seeded data via native versioning
curl -s http://localhost:[PORT]/api/v1/[resources] | python -m json.tool
# Expected: JSON array/object with all expected frontend fields

# C11: Single entity retrieval returns full detail
curl -s http://localhost:[PORT]/api/v1/[resources]/[seeded-id] | python -m json.tool
# Expected: Single JSON object

# C12: Missing or invalid entity returns RFC 7807 Problem Detail with errorCode
curl -s http://localhost:[PORT]/api/v1/[resources]/nonexistent
# Expected: {"type":".../errors/not-found","title":"... Not Found","status":404,"errorCode":"[ENTITY]_NOT_FOUND",...}
# Content-Type: application/problem+json

# C13: Anti-pattern guard — JPA entities absent from API layer
grep -r "JpaEntity" src/main/java/com/aieta/[service_name]/infrastructure/api/
# Expected: NO OUTPUT
```

> [!CAUTION]
> **C13 must return zero matches.** Leaking `JpaEntity` objects into the `api/` layer breaks the Explicit Infrastructure Boundary rule and causes Hibernate lazy-loading `SerializationException` in production.

---

## Milestone 5 — Full Testing Coverage & Reliability Verification

> **Goal:** Exhaustive verification covering behavioral specifications, pure unit logic, database integrity, race condition resilience, and network retry idempotency.

### Tasks

- [ ] **5.1 — Gherkin Scenarios (Fully Commented):**
  Create `src/test/resources/features/[module]_behavior.feature` using Cucumber/Gherkin syntax with complete explanatory comments:
  - Explicit *Given-When-Then* structure for happy paths, negative flows, and domain edge cases.
  - Explanatory header comments describing the business requirement behind each scenario.
  - Wire step definitions with `Mediator`.

- [ ] **5.2 — Unit Tests Without I/O:**
  Create in `src/test/java/.../domain/`:
  - Pure in-memory unit tests (zero disk, zero network, zero database, zero Spring context).
  - Test pure entity invariants asserting custom domain exceptions.

- [ ] **5.3 — Integration Tests with Real Database:**
  Create `[Entity]RepositoryIT.java` executing against the live containerized database:
  - Verify complex SQL mappings, JSONB columns, arrays, and transactional semantics.
  - Verify pagination, filters, and custom queries against real indexes.

- [ ] **5.4 — Concurrent Tests on Identical Registry:**
  Create `[Entity]ConcurrencyIT.java` simulating concurrent load on the exact same record:
  - Spin up 50+ parallel requests targeting the identical entity simultaneously via `Mediator` (`CountDownLatch` + Java 25 Virtual Threads).
  - Assert optimistic locking (`@Version`) behavior and absence of lost updates or deadlocks.

- [ ] **5.5 — Idempotency Against Retries:**
  Create `[Entity]IdempotencyIT.java`:
  - Send duplicate query/mutation requests or network retries (20-30 iterations) with identical identifiers via `Mediator`.
  - Assert that repeated executions produce 100% deterministic, identical state without side effects.

### ✅ Milestone 5 Checkpoint

```powershell
# C14: Unit tests run in-memory without I/O
.\gradlew.bat test --tests "*DomainUnitTest"
# Expected: Blazing fast execution (< 1s), zero database or socket connections

# C15: Gherkin / BDD feature scenarios pass
.\gradlew.bat test --tests "*CucumberTestRunner"
# Expected: All scenarios and steps GREEN

# C16: Integration tests against live PostgreSQL pass
.\gradlew.bat test --tests "*IT"
# Expected: Real DB queries, mappings, and index paths verified cleanly

# C17: Concurrency test confirms race condition safety
.\gradlew.bat test --tests "*ConcurrencyIT"
# Expected: 50+ concurrent requests on identical record handled with zero data corruption

# C18: Idempotency test confirms retry immunity
.\gradlew.bat test --tests "*IdempotencyIT"
# Expected: Duplicate calls with identical key produce exact same state, no duplicate entries
```

---

## Definition of Done

All checkpoints must be green before this milestone is considered closed.

| # | Checkpoint | Status |
|---|---|---|
| C1 | Gradle Wrapper + Java 25 confirmed | ⬜ |
| C2 | `gradle compileJava` exits 0 | ⬜ |
| C3 | `actuator/health` → datastore `UP` | ⬜ |
| C4 | Full compile after domain layer | ⬜ |
| C5 | Zero Spring/JPA imports in `domain/` | ⬜ |
| C6 | Domain invariants unit tests GREEN | ⬜ |
| C7 | JPA validate mode starts without errors | ⬜ |
| C8 | Seed data present in datastore | ⬜ |
| C9 | Integration test against live container GREEN | ⬜ |
| C10 | Primary read endpoint returns seeded list | ⬜ |
| C11 | Single entity retrieval returns full detail | ⬜ |
| C12 | Non-existent entity returns RFC 7807 response | ⬜ |
| C13 | Zero JpaEntity references in `api/` layer | ⬜ |
| C14 | Unit tests run in-memory without I/O | ⬜ |
| C15 | Fully commented Gherkin BDD scenarios GREEN | ⬜ |
| C16 | Integration tests against live database GREEN | ⬜ |
| C17 | Concurrency test (parallel load on same record) GREEN | ⬜ |
| C18 | Idempotency test (retries with identical key) GREEN | ⬜ |

---

## Deferred to Later Milestones

> Use this section to park scope that is explicitly out of this plan. Make trade-offs visible.

| Item | Reason |
|---|---|
| Write/admin endpoints (`POST`, `PUT`, `DELETE`) | B2B Back-Office UI must be ready to validate the contract. |
| Transactional Outbox relay worker | `outbox_events` table is created; CDC relay worker is a separate messaging milestone. |
| Security / Entra ID JWT validation | Delegated to the Spring Cloud Gateway filter. |
| Inter-service OpenFeign clients | Only needed when a dependent service exists and its contract is stable. |

---

## Notes & Decisions

> Use this section to record any architectural decisions made during implementation.

| Decision | Rationale |
|---|---|
| Use in-memory Mediator bus | Adopts [CQRS_ARCHITECTURE.md](../CQRS_ARCHITECTURE.md); decouples controllers from use cases, prepares handlers for microservice decoupling. |
| Group use cases in subfolders | Keeps Query/Command record and Handler atomic, cleanly organized under `application/query/<use_case>/`. |
| Spring Boot 4 native versioning | `ApiVersionConfigurer` + `PathMatchConfigurer` handles routing; eliminates hardcoding `"v1"` in `@RequestMapping`. |
| Custom domain & CQRS exceptions | Eliminates generic `IllegalArgumentException`; enables fine-grained logging, metrics, and RFC 7807 `errorCode` mappings. |
| Gradle build toolchain | Modern Java 25 compilation speed and dependency resolution over Maven. |
