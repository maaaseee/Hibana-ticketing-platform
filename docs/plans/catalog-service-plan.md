# Microservice Implementation Plan: `catalog-service`

> **Service:** `com.aieta.ticketing_app_catalog`
> **Stack:** Java 25 · Spring Boot 4 · Hexagonal Architecture (Ports & Adapters) · CQRS with Mediator
> **Build Tool:** Gradle (`gradlew.bat`) with Java 25 toolchain
> **Database:** PostgreSQL 17 + pgvector (`localhost:5432/ticketing_db`)
> **Goal:** Deliver a read-heavy B2C Event Catalog REST API that feeds the Stitch AI frontend wireframe.

---

## Pre-Flight Checklist (Before Writing Code)

- [x] **Infrastructure running:** `docker compose ps` shows `ticketing-postgres`, `ticketing-redis`, and `floci-az` as **healthy**.
- [x] **Schema applied:** [infra/local-dev/postgres-init/ex.sql](file:///infra/local-dev/postgres-init/ex.sql) executed → `events`, `venues`, `event_sectors`, `outbox_events` tables exist.
- [x] **Java 25 on PATH:** `java -version` returns `25.0.3`.
- [x] **Project directory created:** `ticketing-app-catalog/` exists (built with Gradle as requested).

---

## Milestone 1 — Scaffold & Runtime Baseline

**Goal:** A Spring Boot 4 application that starts, connects to PostgreSQL, and responds to `/actuator/health`.

### Tasks

- [x] **1.1 — `build.gradle`:** Create Gradle project file with Spring Boot 4 (`4.1.1`), Java 25 toolchain (`JavaLanguageVersion.of(25)`), dependencies: `spring-boot-starter-webmvc`, `spring-boot-starter-data-jpa`, `spring-boot-starter-actuator`, `spring-boot-starter-validation`, `postgresql`, and Gradle Wrapper (`gradlew.bat`).
- [x] **1.2 — `application.yml`:** Configure `spring.threads.virtual.enabled: true`, HikariCP targeting `${DB_HOST:localhost}:5432/ticketing_db`, `server.shutdown: graceful`, Actuator exposing `health,metrics,prometheus`. Exclude unconfigured vector auto-config until model provisioned.
- [x] **1.3 — `TicketingAppCatalogApplication.java`:** Create Spring Boot entry point at `com.aieta.ticketing_app_catalog`.

### ✅ Milestone 1 Checkpoint

```powershell
# C1: Gradle Wrapper works with Java 25
.\gradlew.bat -v
# Expected: Gradle 9.8.1 + JVM: 25.0.3

# C2: Project compiles cleanly
.\gradlew.bat clean compileJava
# Expected: BUILD SUCCESSFUL, exit code 0

# C3: Application starts and Actuator responds
curl http://localhost:8080/actuator/health
# Expected: {"status":"UP","components":{"db":{"status":"UP"},...}}
```

> [!IMPORTANT]
> The `db` component inside `actuator/health` **must** be `UP`. This confirms the HikariCP pool established a real connection to `ticketing-postgres`.

---

## Milestone 2 — Domain Core, CQRS Mediator & Use Case Folders

**Goal:** Pure Java 25 domain model, custom domain exceptions, in-memory Mediator bus, and isolated CQRS use-case packages. Zero framework dependencies inside `domain/`.

### Tasks

- [x] **2.1 — Domain Value Objects:** Create immutable Java records in `domain/entity/`:
  - `EventStatus.java` (enum: `DRAFT`, `PUBLISHED`, `ON_SALE`, `CANCELLED`)
  - `EventSectorSummary.java` (record: `sectorId`, `name`, `type`, `totalCapacity`, `basePrice`, `serviceFee`, `currency`)
  - `EventSummary.java` (record: `id`, `slug`, `title`, `status`, `category`, `genres`, `doorsOpenAt`, `startsAt`, `endsAt`, `timezone`, `venue`, `sectors`, `mediaJson`, `salesPolicyJson`)

- [x] **2.2 — Custom Domain Exceptions:** Create in `domain/exceptions/` (banning generic `RuntimeException` and `IllegalArgumentException`):
  - `EventNotFoundException.java` (status 404)
  - `InvalidEventScheduleException.java` (doors/starts/ends sequence violations)
  - `InvalidEventTitleException.java` (empty/blank event title)
  - `InvalidSectorPricingException.java` (non-positive base price, negative service fee)
  - `InvalidSectorCapacityException.java` (non-positive capacity)
  - `InvalidEventIdentifierException.java` (blank event identifier)
  
- [x] **2.3 — Outbound Port:** Create `EventRepositoryPort.java` in `domain/port/`:
  ```java
  public interface EventRepositoryPort {
      List<EventSummary> findUpcomingPublished();
      List<EventSummary> findByCategoryAndPublished(String category);
      Optional<EventSummary> findById(String id);
      Optional<EventSummary> findBySlug(String slug);
  }
  ```

- [x] **2.4 — In-Memory Mediator Bus:** Create generic mediator infrastructure in `common/mediator/` (per [CQRS_ARCHITECTURE.md](../CQRS_ARCHITECTURE.md)):
  - `Request<T>`: Generic request marker.
  - `RequestHandler<T, R>`: Generic handler interface.
  - `Mediator.java`: Central Spring `@Component` dispatcher.
  - `HandlerNotFoundException.java`: Typed exception for unregistered requests.

- [x] **2.5 — Isolated CQRS Query Packages:** Create under `application/query/[use_case]/`:
  - `application/query/get_upcoming_events/`:
    - `GetUpcomingEventsQuery.java` (record implementing `Request<List<EventSummary>>`)
    - `GetUpcomingEventsQueryHandler.java` (implementing `RequestHandler<GetUpcomingEventsQuery, List<EventSummary>>`)
  - `application/query/get_event_by_id/`:
    - `GetEventByIdQuery.java` (record implementing `Request<EventSummary>`)
    - `GetEventByIdQueryHandler.java` (implementing `RequestHandler<GetEventByIdQuery, EventSummary>`)

### ✅ Milestone 2 Checkpoint

```powershell
# C4: Compile confirms zero Spring/JPA imports in domain/
.\gradlew.bat clean compileJava
# Expected: BUILD SUCCESSFUL, exit code 0

# C5: Manual grep verifies domain layer purity
grep -r "springframework\|jakarta.persistence" src/main/java/com/aieta/ticketing_app_catalog/domain/
# Expected: NO OUTPUT (zero matches)

# C6: Unit tests for domain invariants with custom exceptions pass
.\gradlew.bat test --tests "*EventDomainUnitTest"
# Expected: BUILD SUCCESS, all tests GREEN in < 1s
```

> [!IMPORTANT]
> **C5 (domain purity grep) must return zero matches.** Any Spring or JPA import inside `domain/` is a Cardinal Rule violation per the [spring-boot-4-microservice skill](.gemini/skills/spring-boot4-microservices/SKILL.md).

---

## Milestone 3 — PostgreSQL Persistence Adapter

**Goal:** Wire the domain port to the live `ticketing-postgres` container via JPA adapters, seed realistic events.

### Tasks

- [x] **3.1 — JPA Entities:** Create in `infrastructure/database/`:
  - `VenueJpaEntity.java` (mapped to `venues`, `@Version`, coordinates as `BigDecimal`)
  - `EventJpaEntity.java` (mapped to `events`, `@Version`, `genres` as `String[]`, `media`/`sales_policy` as `String` JSONB, `embedding` deferred)
  - `EventSectorJpaEntity.java` (mapped to `event_sectors`, `@ManyToOne(fetch = LAZY)` to `EventJpaEntity`)

- [x] **3.2 — Spring Data Repositories:** Create internal-only interfaces:
  - `SpringDataEventRepository` extending `JpaRepository<EventJpaEntity, String>` with `findUpcomingPublished()` JPQL query.
  - `SpringDataVenueRepository` extending `JpaRepository<VenueJpaEntity, String>`.

- [x] **3.3 — Domain Mapper:** Create `EventMapper.java` in `infrastructure/database/mapper/` (converts `EventJpaEntity` → `EventSummary` domain record).

- [x] **3.4 — Port Adapter:** Create `JpaEventRepositoryAdapter.java` in `infrastructure/database/` implementing `EventRepositoryPort`. Annotated `@Component`.

- [x] **3.5 — Dev Seed Data:** Create `src/main/resources/data-dev.sql` with realistic events (Coldplay at Estadio Nacional Julio Martínez Prádanos) referencing the schema from [infra/local-dev/postgres-init/ex.sql](file:///infra/local-dev/postgres-init/ex.sql).

### ✅ Milestone 3 Checkpoint

```powershell
# C7: JPA schema validation passes (validates entity mappings against live DB)
.\gradlew.bat test --tests "*TicketingAppCatalogApplicationTests"
# Expected: App starts without HibernateException or column mismatch errors (ddl-auto: validate)

# C8: Verify seed data was inserted
docker exec ticketing-postgres psql -U ticketing_admin -d ticketing_db -c "SELECT id, title, status FROM events;"
# Expected: Rows with seeded events in status PUBLISHED

# C9: Integration test calls adapter against live DB
.\gradlew.bat test --tests "*EventJpaRepositoryIT"
# Expected: BUILD SUCCESS, events retrieved from real PostgreSQL container
```

---

## Milestone 4 — REST API, Native Versioning & RFC 7807

**Goal:** Expose HTTP endpoints matching the Stitch AI wireframe data contract using Spring Boot 4 native versioning, Mediator dispatching, and RFC 7807 error responses.

### Tasks

- [x] **4.1 — Response DTOs:** Create immutable records in `infrastructure/api/dto/`:
  - `SectorResponse.java` (sectorId, name, type, totalCapacity, basePrice, serviceFee, totalPrice, currency)
  - `EventResponse.java` (id, slug, title, status, category, genres, startsAt, venueName, venueCity, bannerUrl, sectors)

- [x] **4.2 — Native Spring Boot 4 API Versioning:** Create `WebVersioningConfig.java` in `infrastructure/configuration/`:
  - `configureApiVersioning(ApiVersionConfigurer)`: Configure `usePathSegment(1)`, `addSupportedVersions("1.0", "1")`, and `setDefaultVersion("1.0")`.
  - `configurePathMatch(PathMatchConfigurer)`: Register global prefix `addPathPrefix("/api/v{version}", HandlerTypePredicate.forAnnotation(RestController.class))`.

- [x] **4.3 — Controller with Mediator:** Create `EventController.java` in `infrastructure/api/`:
  - Map route `@RequestMapping("/events")` (never hardcode `"v1"` in controller path).
  - Declare version on endpoints: `@GetMapping(version = "1.0")` and `@GetMapping(path = "/{id}", version = "1.0")`.
  - Inject only `Mediator` and delegate queries to `mediator.dispatch(...)`.
  - Controllers accept only DTOs/primitives; never expose JPA entities or domain records directly.

- [x] **4.4 — Granular RFC 7807 Global Exception Handler:** Create `GlobalExceptionHandler.java` in `infrastructure/api/exception/`:
  - Dedicated `@ExceptionHandler` for each custom exception (`EventNotFoundException`, `InvalidEventScheduleException`, `InvalidEventTitleException`, `InvalidSectorPricingException`, `InvalidSectorCapacityException`, `InvalidEventIdentifierException`, `HandlerNotFoundException`, `MethodArgumentNotValidException`).
  - Return RFC 7807 `ProblemDetail` with distinct `errorCode`, URI `type`, `title`, and structured SLF4J logging (`log.warn` / `log.error`).

### ✅ Milestone 4 Checkpoint (End-to-End)

```powershell
# C10: List events returns seeded data via native versioning
curl -s http://localhost:8080/api/v1/events | python -m json.tool
# Expected: JSON array with events, all fields present (title, bannerUrl, venueName, startsAt, sectors)

# C11: Event by ID returns full detail
curl -s http://localhost:8080/api/v1/events/evt_coldplay_2026 | python -m json.tool
# Expected: Single event JSON object with all fields

# C12: Not found returns RFC 7807 ProblemDetail with errorCode
curl -s http://localhost:8080/api/v1/events/evt_nonexistent_999
# Expected: {"type":"https://amaryllis-tickets.com/errors/not-found","title":"Event Not Found","status":404,"errorCode":"EVENT_NOT_FOUND",...}
# Content-Type header: application/problem+json

# C13: Anti-pattern guard — JPA entities are NOT in controller return types
grep -r "JpaEntity" src/main/java/com/aieta/ticketing_app_catalog/infrastructure/api/
# Expected: NO OUTPUT
```

---

## Milestone 5 — Rigorous Testing & Concurrency Verification

**Goal:** Complete, production-grade test coverage verifying business specifications, isolated logic, real persistence, race condition resilience, and idempotency guarantees.

### Tasks

- [x] **5.1 — Gherkin Specifications (Fully Commented):**
  Create `src/test/resources/features/catalog_browsing.feature` describing domain behaviors with Cucumber/Gherkin syntax, fully commented:
  - Scenario: Public user browses upcoming published events.
  - Scenario: User queries event by non-existent identifier.
  - Wire step definitions with `Mediator`.

- [x] **5.2 — Unit Tests Without I/O:**
  Create `EventDomainUnitTest.java` in `src/test/java/.../domain/`:
  - Pure POJO domain entity invariant tests asserting custom domain exceptions (zero disk, zero network, zero Spring context).

- [x] **5.3 — Real Database Integration Tests:**
  Create `EventJpaRepositoryIT.java` running against the active PostgreSQL instance:
  - Verify mapping of JSONB columns (`media`, `sales_policy`) and native arrays (`genres`).
  - Verify query ordering and category filtering.

- [x] **5.4 — Parallel Concurrency Stress Tests:**
  Create `CatalogConcurrencyIT.java` simulating concurrent load on the exact same registry:
  - Dispatch 50 concurrent virtual threads targeting `evt_coldplay_2026` via `Mediator` and `CountDownLatch`.
  - Assert zero race conditions, thread safety, and identical data integrity.

- [x] **5.5 — Idempotency & Retry Resilience Tests:**
  Create `CatalogIdempotencyIT.java`:
  - Simulate repeated/duplicate requests (30 consecutive retries) with the same identifier via `Mediator`.
  - Verify that re-executing queries produces 100% deterministic, identical outcomes without side effects.

### ✅ Milestone 5 Checkpoint

```powershell
# C14: Unit tests run in-memory without I/O
.\gradlew.bat test --tests "*DomainUnitTest"
# Expected: Fast execution (< 1s), zero database or socket connections

# C15: Gherkin / BDD feature tests pass
.\gradlew.bat test --tests "*CucumberTestRunner"
# Expected: All scenarios and steps GREEN

# C16: Integration tests against live PostgreSQL pass
.\gradlew.bat test --tests "*EventJpaRepositoryIT"
# Expected: Real DB queries and mappings verified cleanly

# C17: Concurrency test confirms race condition safety
.\gradlew.bat test --tests "*ConcurrencyIT"
# Expected: 50+ concurrent virtual thread requests on identical record handled with zero data corruption

# C18: Idempotency test confirms retry immunity
.\gradlew.bat test --tests "*IdempotencyIT"
# Expected: Duplicate calls with identical key produce exact same state, 100% deterministic
```

---

## Definition of Done for `catalog-service` v1.0

All 18 checkpoints (C1–C18) have passed and are verified:

| # | Checkpoint | Status |
|---|---|---|
| C1 | Gradle Wrapper + Java 25 confirmed | PASS ✅ |
| C2 | `gradle compileJava` exits 0 | PASS ✅ |
| C3 | `actuator/health` → `db.status: UP` | PASS ✅ |
| C4 | Full compile after domain layer | PASS ✅ |
| C5 | Zero Spring/JPA imports in `domain/` | PASS ✅ |
| C6 | Domain invariants unit tests GREEN | PASS ✅ |
| C7 | JPA validate mode starts without errors | PASS ✅ |
| C8 | Seed data present in PostgreSQL | PASS ✅ |
| C9 | Integration test against live DB GREEN | PASS ✅ |
| C10 | `GET /api/v1/events` returns seeded list | PASS ✅ |
| C11 | `GET /api/v1/events/{id}` returns full detail | PASS ✅ |
| C12 | `GET /api/v1/events/nonexistent` returns RFC 7807 | PASS ✅ |
| C13 | Zero JpaEntity references in `api/` layer | PASS ✅ |
| C14 | Unit tests run without I/O | PASS ✅ |
| C15 | Fully commented Gherkin BDD scenarios GREEN | PASS ✅ |
| C16 | Integration tests against live PostgreSQL GREEN | PASS ✅ |
| C17 | Concurrency test (50 Virtual Threads on same record) GREEN | PASS ✅ |
| C18 | Idempotency test (30 retries with identical key) GREEN | PASS ✅ |

---

## Deferred to Later Milestones

| Item | Reason |
|---|---|
| `POST /api/v1/events` (admin create) | Requires B2B Back-Office UI wiring. |
| Transactional Outbox relay worker | `outbox_events` table exists; CDC relay belongs to messaging phase. |
| Cosmos DB dual-write sync | PostgreSQL-first; Cosmos integration is a separate milestone. |
| `pgvector` semantic search endpoint | Requires embedding generation via Azure OpenAI / Ollama. |
| Spring Security / Entra ID JWT filter | Deferred to API Gateway milestone. |

---

## Notes & Decisions

| Decision | Rationale |
|---|---|
| In-Memory Mediator Bus (`common.mediator`) | Follows [CQRS_ARCHITECTURE.md](../CQRS_ARCHITECTURE.md). Decouples controllers from use cases, simplifies unit testing, and provides seamless path to async brokers. |
| Group use cases in subfolders | `application/query/get_upcoming_events/` and `application/query/get_event_by_id/` keep query record and handler atomic. |
| Spring Boot 4 Native API Versioning | `WebVersioningConfig` configures `ApiVersionConfigurer` (`usePathSegment(1)`) + `PathMatchConfigurer` (`addPathPrefix("/api/v{version}", ...)`). Controllers declare `@RequestMapping("/events")` without hardcoded `"v1"` in URI. |
| Granular RFC 7807 Custom Exceptions | Generic `IllegalArgumentException` and `IllegalStateException` banned. Custom exceptions mapped with explicit `errorCode`, status, URI type, and structured logging. |
| Gradle build toolchain (`gradlew.bat`) | Adopted Gradle over Maven for faster builds and modern Java 25 compilation. |
| Jackson 3 imports | Spring Boot 4 uses Jackson 3 (`tools.jackson.databind.*`), replaced old `com.fasterxml.jackson` imports in controller. |
