# Microservice Implementation Plan: `catalog-service` (Part 2 — Write / Command Operations)

> **Service:** `com.aieta.ticketing_app_catalog`
> **Stack:** Java 25 · Spring Boot 4 (`4.1.1`) · Hexagonal Architecture (Ports & Adapters) · CQRS with Mediator
> **Build Tool:** Gradle (`gradlew.bat`) with Java 25 toolchain
> **Primary Datastore:** PostgreSQL 17 (`localhost:5432/ticketing_db`) + Transactional Outbox (`outbox_events`)
> **Messaging Backbone:** Azure Service Bus (local emulation via `floci-az`) on Topic `event.lifecycle.v1`
> **Goal:** Deliver the B2B administrative write/command side of the Catalog Service: Event creation (`POST /api/v1/events`), lifecycle management (publish, cancel), Transactional Outbox persistence with domain events (`EventCreated`, `EventCancelled`), and asynchronous relay to Azure Service Bus.

---

## Pre-Flight Checklist (Before Writing Code)

- [x] **Infrastructure running:** `docker compose ps` shows `ticketing-postgres`, `ticketing-redis`, and `floci-az` as **healthy**.
- [x] **Catalog Read Baseline operational:** All 16 existing tests in `ticketing-app-catalog` pass cleanly (`.\gradlew.bat test`).
- [x] **Database Schema verified:** `events`, `event_sectors`, `venues`, and `outbox_events` tables exist in `ticketing_db`.
- [x] **Local Azure Service Bus Emulator reachable:** `floci-az` is running on ports 4577 / 5673.

---

## Milestone 1 — Scaffold & Runtime Baseline (Write Side Extensions)

> **Goal:** Add messaging client and scheduling capabilities to the existing Spring Boot 4 application for Transactional Outbox processing and event relay without breaking existing read APIs.

### Tasks

- [x] **1.1 — Dependencies Update in `build.gradle`:**
  - Add Jackson Databind modules for domain event serialization (`tools.jackson.datatype:jackson-datatype-jsr310`).
  - Configure Azure Service Bus messaging dependencies or lightweight HTTP/AMQP client targeting local `floci-az` emulator.
  - Verify Gradle builds cleanly with Java 25 toolchain.
- [x] **1.2 — Outbox & Messaging Configuration in `application.yml`:**
  - Enable Spring task scheduling: `catalog.outbox.relay.enabled: true`.
  - Configure poller parameters: `catalog.outbox.relay.fixed-delay-ms: 500`, `catalog.outbox.relay.batch-size: 50`, `catalog.outbox.relay.max-retries: 3`.
  - Configure Service Bus topic name: `catalog.messaging.topics.event-lifecycle: event.lifecycle.v1`.
- [x] **1.3 — Scheduling Activation:**
  - Add `@EnableScheduling` to an infrastructure configuration class (`OutboxRelayConfig.java`) isolated from the domain.

### ✅ Milestone 1 Checkpoint

```powershell
# C1: Gradle build compiles with new messaging/scheduling dependencies
.\gradlew.bat clean compileJava
# Expected: BUILD SUCCESSFUL, exit code 0

# C2: Application boots up and Actuator confirms persistence & scheduling
.\gradlew.bat bootRun
# Expected: Application starts on port 8080, log confirms HikariPool and task scheduler active

# C3: Health check remains green
curl http://localhost:8080/actuator/health
# Expected: {"status":"UP","components":{"db":{"status":"UP"},...}}
```

---

## Milestone 2 — Domain Core, CQRS Mediator & Command Use Case Folders

> **Goal:** Pure Java 25 domain write models, state machine transitions, domain events, domain repository ports, and isolated use case packages under `application/command/`. Zero framework dependencies inside `domain/`.

### Tasks

- [x] **2.1 — Domain Entities & Aggregate Roots:**
  - Create rich domain entity `Event.java` in `domain/entity/`:
    - Encapsulates lifecycle state transitions (`DRAFT` $\to$ `PUBLISHED` $\to$ `CANCELLED`).
    - Methods enforcing invariants: `publish()`, `cancel(String reason)`, `updateSchedule(...)`.
  - Create `EventSector.java` in `domain/entity/` enforcing positive capacity and non-negative pricing invariants.
- [x] **2.2 — Domain Events (Immutable Java Records):** Create in `domain/event/`:
  - `EventCreatedEvent.java` (eventId, organizerId, venueId, slug, title, startsAt, sectors, occurredAt).
  - `EventCancelledEvent.java` (eventId, cancellationReason, occurredAt).
  - `EventPublishedEvent.java` (eventId, publishedAt).
- [x] **2.3 — Custom Domain Exceptions:** Create in `domain/exceptions/`:
  - `VenueNotFoundException.java` (status 404 — venue does not exist for new event).
  - `DuplicateEventSlugException.java` (status 409 — slug already registered).
  - `EventAlreadyCancelledException.java` (status 409 — illegal transition on cancelled event).
  - `InvalidEventStateException.java` (status 422 — attempting to publish without sectors).
- [x] **2.4 — Outbound Ports:** Create in `domain/port/`:
  - `EventWriteRepositoryPort.java`:
    ```java
    public interface EventWriteRepositoryPort {
        Event saveWithOutbox(Event event, Object domainEvent);
        Optional<Event> findByIdForUpdate(String id);
        boolean existsBySlug(String slug);
    }
    ```
- [x] **2.5 — Isolated CQRS Command Packages:** Group each use case into its dedicated folder:
  - `application/command/create_event/`:
    - `CreateEventCommand.java` (record implementing `Request<EventCreateResult>`).
    - `CreateEventCommandHandler.java` (implementing `RequestHandler<CreateEventCommand, EventCreateResult>`, `@Transactional`).
  - `application/command/cancel_event/`:
    - `CancelEventCommand.java` (record implementing `Request<Void>`).
    - `CancelEventCommandHandler.java` (implementing `RequestHandler<CancelEventCommand, Void>`, `@Transactional`).
  - `application/command/publish_event/`:
    - `PublishEventCommand.java` (record implementing `Request<Void>`).
    - `PublishEventCommandHandler.java` (implementing `RequestHandler<PublishEventCommand, Void>`, `@Transactional`).

### ✅ Milestone 2 Checkpoint

```powershell
# C4: Project compiles cleanly after domain command models
.\gradlew.bat clean compileJava
# Expected: BUILD SUCCESSFUL, exit code 0

# C5: Domain purity guard — zero framework imports in domain/
grep -r "springframework\|jakarta.persistence" src/main/java/com/aieta/ticketing_app_catalog/domain/
# Expected: NO OUTPUT (zero matches — Cardinal Rule violation if any found)

# C6: Pure domain unit tests for write invariants pass with zero I/O
.\gradlew.bat test --tests "*EventAggregateDomainUnitTest"
# Expected: All state transition & invariant tests GREEN in < 1s
```

---

## Milestone 3 — Persistence Adapter & Transactional Outbox

> **Goal:** Implement write-side JPA adapters, atomic local ACID persistence for entities + outbox events, and an asynchronous outbox relay worker dispatching to Azure Service Bus (Floci-AZ).

### Tasks

- [x] **3.1 — Outbox JPA Entity & Repository:**
  - Create `OutboxEventJpaEntity.java` in `infrastructure/database/entity/`:
    - Fields: `id` (UUID), `aggregateType`, `aggregateId`, `eventType`, `payload` (JSONB), `status` (`PENDING`, `PUBLISHED`, `FAILED`), `retryCount`, `createdAt`, `processedAt`.
  - Create `SpringDataOutboxEventRepository.java` in `infrastructure/database/repository/`:
    - Add `@Query("SELECT o FROM OutboxEventJpaEntity o WHERE o.status = 'PENDING' ORDER BY o.createdAt ASC")` with pessimistic write lock / `SKIP LOCKED` capabilities.
- [x] **3.2 — Write Repository Adapter:**
  - Create `JpaEventWriteRepositoryAdapter.java` in `infrastructure/database/adapter/`:
    - Implements `EventWriteRepositoryPort`.
    - In a **single local ACID transaction**, writes `EventJpaEntity`, child `EventSectorJpaEntity` records, and serializes the domain event into `OutboxEventJpaEntity`.
- [x] **3.3 — Asynchronous Outbox Relay Worker:**
  - Create `OutboxRelayScheduler.java` in `infrastructure/messaging/outbox/`:
    - Runs periodically (`@Scheduled(fixedDelayString = "${catalog.outbox.relay.fixed-delay-ms:500}")`).
    - Reads pending batches (`FOR UPDATE SKIP LOCKED`).
    - Publishes messages to Service Bus topic `event.lifecycle.v1` with message ID `evt_{outboxId}` (ensuring idempotency).
    - Updates outbox status to `PUBLISHED` upon confirmation.

### ✅ Milestone 3 Checkpoint

```powershell
# C7: JPA entities match live PostgreSQL schema (ddl-auto: validate)
.\gradlew.bat test --tests "*TicketingAppCatalogApplicationTests"
# Expected: App starts without HibernateException on outbox_events or events tables

# C8: Write integration test verifies ACID dual-persistence
.\gradlew.bat test --tests "*EventWriteRepositoryIT"
# Expected: Event, sectors, and outbox_events rows inserted in 1 local transaction

# C9: Outbox poller integration test verifies event publication
.\gradlew.bat test --tests "*OutboxRelayIT"
# Expected: Outbox record transitions from 'PENDING' to 'PUBLISHED'
```

---

## Milestone 4 — REST API, Native Versioning & RFC 7807 (Write Operations)

> **Goal:** Expose B2B administrative HTTP endpoints with Spring Boot 4 native versioning, Mediator dispatching, HTTP 202 Accepted semantics for event creation, and RFC 7807 error responses.

### Tasks

- [x] **4.1 — Command Request & Response DTOs:** Create in `infrastructure/api/dto/`:
  - `CreateEventRequest.java` (organizerId, venueId, slug, title, category, genres, doorsOpenAt, startsAt, endsAt, timezone, sectors, media, salesPolicy).
  - `CreateEventSectorRequest.java` (name, type, totalCapacity, basePrice, serviceFee, currency).
  - `EventCreatedResponse.java` (id, slug, status, createdAt, estimatedSeatsToGenerate).
- [x] **4.2 — Admin Controller with Mediator:**
  - Create `EventAdminController.java` in `infrastructure/api/`:
    - Map route `@RequestMapping("/events")`.
    - `@PostMapping(version = "1.0")`: Accepts `CreateEventRequest`, returns `HTTP 202 Accepted` with `EventCreatedResponse`.
    - `@PostMapping(path = "/{id}/publish", version = "1.0")`: Returns `HTTP 200 OK` or `204 No Content`.
    - `@DeleteMapping(path = "/{id}", version = "1.0")`: Cancels event, returns `HTTP 200 OK`.
    - Inject only `Mediator` and delegate to `mediator.dispatch(...)`.
- [x] **4.3 — Granular RFC 7807 Exception Handlers:**
  - Extend `GlobalExceptionHandler.java`:
    - `VenueNotFoundException` $\to$ HTTP 404 (`VENUE_NOT_FOUND`).
    - `DuplicateEventSlugException` $\to$ HTTP 409 (`DUPLICATE_EVENT_SLUG`).
    - `EventAlreadyCancelledException` $\to$ HTTP 409 (`EVENT_ALREADY_CANCELLED`).
    - `InvalidEventStateException` $\to$ HTTP 422 (`INVALID_EVENT_STATE`).

### ✅ Milestone 4 Checkpoint (End-to-End)

```powershell
# C10: Create event returns HTTP 202 Accepted with creation metadata
curl -X POST http://localhost:8080/api/v1/events `
  -H "Content-Type: application/json" `
  -d '{"organizerId":"org_001","venueId":"ven_estadio_nacional","slug":"queen-tribute-2026","title":"Queen Tribute 2026","category":"CONCERT","doorsOpenAt":"2026-12-01T18:00:00Z","startsAt":"2026-12-01T20:00:00Z","endsAt":"2026-12-01T23:00:00Z","sectors":[{"name":"Platea","type":"GENERAL_ADMISSION","totalCapacity":1000,"basePrice":50.00,"serviceFee":5.00,"currency":"USD"}]}'
# Expected: HTTP/1.1 202 Accepted, {"id":"...","slug":"queen-tribute-2026","status":"DRAFT",...}

# C11: Duplicate slug returns RFC 7807 Conflict (409)
curl -X POST http://localhost:8080/api/v1/events -H "Content-Type: application/json" -d '{...duplicate slug...}'
# Expected: HTTP 409 Conflict with errorCode "DUPLICATE_EVENT_SLUG"

# C12: Outbox table contains matching event payload in PostgreSQL
docker exec ticketing-postgres psql -U ticketing_admin -d ticketing_db -c "SELECT aggregate_id, event_type, status FROM outbox_events WHERE aggregate_id = 'queen-tribute-2026' OR payload->>'slug' = 'queen-tribute-2026';"
# Expected: 1 row with event_type 'EventCreatedEvent' and status 'PUBLISHED' (after relay)

# C13: Controller purity guard — zero JpaEntity references in api/
grep -r "JpaEntity" src/main/java/com/aieta/ticketing_app_catalog/infrastructure/api/
# Expected: NO OUTPUT
```

---

## Milestone 5 — Full Testing Coverage & Reliability Verification (Write Side)

> **Goal:** Comprehensive 5-tier test suite verifying event creation business specs, isolated aggregate invariants, ACID database integrity, race-condition immunity, and outbox retry resilience.

### Tasks

- [x] **5.1 — Gherkin Scenarios (Fully Commented):**
  - Create `src/test/resources/features/event_creation_lifecycle.feature` using Cucumber/Gherkin syntax with full header comments:
    - Scenario: Organizer creates a new stadium event with multiple sectors and receives HTTP 202 Accepted.
    - Scenario: System rejects event creation when schedule is inverted (starts after ends).
    - Scenario: Organizer cancels an active event, preventing future sales.
- [x] **5.2 — Unit Tests Without I/O:**
  - Create `EventAggregateDomainUnitTest.java` in `src/test/java/.../domain/`:
    - Pure in-memory unit tests (zero Spring, zero DB) testing aggregate state machine rules and validation exceptions.
- [x] **5.3 — Real Database Integration Tests:**
  - Create `EventWriteRepositoryIT.java` against PostgreSQL 17:
    - Tests atomic rollback: if sector insertion fails, ensure neither event nor outbox event is persisted.
    - Tests optimistic locking (`@Version`) during simultaneous updates.
- [x] **5.4 — Concurrent Tests on Identical Registry:**
  - Create `EventCreationConcurrencyIT.java`:
    - 50 concurrent virtual threads attempting to create an event with the **same unique slug** simultaneously.
    - Assert that exactly 1 succeeds and 49 receive `DuplicateEventSlugException` / HTTP 409 (zero duplicate database records).
- [x] **5.5 — Idempotency & Outbox Retry Resilience Tests:**
  - Create `OutboxRelayIdempotencyIT.java`:
    - Simulate 20 relay retries for the same outbox event.
    - Assert message ID deduplication (`evt_{outboxId}`) on Azure Service Bus / Floci-AZ topic.

### ✅ Milestone 5 Checkpoint

```powershell
# C14: Unit tests run in-memory without I/O
.\gradlew.bat test --tests "*EventAggregateDomainUnitTest"
# Expected: Blazing fast execution (< 1s), zero database or network calls

# C15: Gherkin BDD write scenarios pass
.\gradlew.bat test --tests "*EventCreationCucumberTest"
# Expected: All scenarios and steps GREEN

# C16: Real DB write & outbox integration tests pass
.\gradlew.bat test --tests "*EventWriteRepositoryIT"
# Expected: Dual ACID persistence and rollback semantics verified cleanly

# C17: Concurrency collision test confirms slug uniqueness under race conditions
.\gradlew.bat test --tests "*EventCreationConcurrencyIT"
# Expected: 50 concurrent requests handled with exactly 1 success and 49 safe 409 rejections

# C18: Idempotency test confirms zero duplicate message publishing
.\gradlew.bat test --tests "*OutboxRelayIdempotencyIT"
# Expected: Repeated worker executions produce deterministic, idempotent delivery
```

---

## Definition of Done for `catalog-service` (Part 2 — Write Operations)

All checkpoints must be green before closing this plan:

| # | Checkpoint | Status |
|---|---|---|
| C1 | Gradle build compiles with messaging & scheduling dependencies | ✅ |
| C2 | Application boots up with active HikariCP and Outbox scheduler | ✅ |
| C3 | `actuator/health` → `status: UP` with DB operational | ✅ |
| C4 | Full compile after domain command layer | ✅ |
| C5 | Zero Spring/JPA imports in `domain/` (purity guard) | ✅ |
| C6 | Domain aggregate unit tests GREEN in < 1s | ✅ |
| C7 | JPA validate mode succeeds against live `outbox_events` table | ✅ |
| C8 | Real DB write integration test verifies atomic entity + outbox transaction | ✅ |
| C9 | Outbox poller transitions records from `PENDING` to `PUBLISHED` | ✅ |
| C10 | `POST /api/v1/events` returns HTTP 202 Accepted | ✅ |
| C11 | Duplicate slug returns RFC 7807 Conflict (409) with errorCode | ✅ |
| C12 | Outbox table in PostgreSQL contains verified event payload | ✅ |
| C13 | Zero JpaEntity references in `api/` layer | ✅ |
| C14 | Aggregate unit tests run without I/O | ✅ |
| C15 | Fully commented Gherkin BDD write scenarios GREEN | ✅ |
| C16 | Transactional rollback test verifies zero orphan records | ✅ |
| C17 | Concurrency test (50 threads colliding on same slug) GREEN | ✅ |
| C18 | Outbox relay idempotency test verifies zero duplicate messages | ✅ |

---

## Deferred to Later Milestones

| Item | Reason |
|---|---|
| Bulk seat generation (15,000+ seats) | Handled asynchronously by `ticketing-service` upon consuming `EventCreatedEvent`. |
| Cosmos DB NoSQL read-model synchronization | Deferred until multi-region low-latency caching layer is scheduled. |
| Spring Security / Entra ID JWT verification | Enforced perimetrically at Spring Cloud Gateway (`gateway-service`). |
| Vector embeddings generation pipeline (`text-embedding-3-small`) | Deferred to Recommendation Engine worker phase. |

---

## Notes & Decisions

| Decision | Rationale |
|---|---|
| HTTP 202 Accepted on Event Creation | Creating a stadium event triggers downstream batch generation of thousands of seats in `ticketing-service`. An asynchronous acknowledgment aligns with Section 6.1 of [Ticketing-app.md](../Ticketing-app.md). |
| Transactional Outbox Pattern | Solves the Dual-Write Problem between PostgreSQL and Azure Service Bus without distributed 2PC/XA transactions. |
| Message Deduplication ID (`evt_{outboxId}`) | Guarantees at-least-once message delivery while preventing downstream duplicate seat generation. |
| Isolated Command Packages | `application/command/create_event/` and `application/command/cancel_event/` enforce CQRS vertical slice modularity. |
