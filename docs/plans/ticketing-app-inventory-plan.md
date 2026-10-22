# Microservice Implementation Plan: `ticketing-app-inventory`

> **Service:** `com.aieta.ticketing_app_inventory`  
> **Stack:** Java 25 · Spring Boot 4 (`4.1.x`) · Hexagonal Architecture (Ports & Adapters) · CQRS with Mediator  
> **Build Tool:** Gradle (`gradlew.bat`) with Java 25 toolchain  
> **Primary Datastore:** PostgreSQL 17 (`localhost:5432/ticketing_db`) + Azure Cache for Redis (`localhost:6379`)  
> **Messaging Backbone:** Azure Service Bus (local emulation via `floci-az`) on Topics `event.lifecycle.v1` and `ticket.reservation.v1`  
> **Port:** `8082`  
> **Goal:** High-concurrency seat inventory lifecycle management, asynchronous bulk seat provisioning from `EventCreatedEvent`, atomic distributed locking using Redis Redlock (10m TTL), Transactional Outbox event relay, and seat reservation endpoints with a strict 0% double-booking guarantee.

---

## Pre-Flight Checklist (Before Writing Code)

- [ ] **Infrastructure running:** `docker compose ps` shows `ticketing-postgres`, `ticketing-redis`, and `floci-az` as **healthy**.
- [ ] **Database Schema:** Tables `seats`, `ticket_reservations`, and `outbox_events` provisioned in `ticketing_db`.
- [ ] **Java 25 toolchain:** Gradle compiles with `JavaLanguageVersion.of(25)`.
- [ ] **Target Git Worktree:** Isolated worktree `.worktrees/backend-ticketing-service` on branch `feature/backend-ticketing-service`.

---

## Milestone 1 — Scaffold & Runtime Baseline

> **Goal:** A Spring Boot 4 application in `ticketing-app-inventory/` that boots on port `8082`, enables Virtual Threads, connects to PostgreSQL 17 and Redis 7, and responds `UP` on `/actuator/health`.

### Tasks
- [ ] **1.1 — Project Initialization:**
  - Create `ticketing-app-inventory/` directory with Gradle wrapper (`gradlew.bat`).
  - Configure `build.gradle` targeting Java 25 toolchain with Spring Boot 4 (`4.1.x`), Spring Data JPA, Redis (Lettuce/Redisson), Actuator, Validation, and Jackson Databind.
- [ ] **1.2 — Configuration (`application.yml`):**
  - Server port: `8082`.
  - Virtual Threads: `spring.threads.virtual.enabled: true`.
  - Datasource connection pool (HikariCP) pointing to `localhost:5432/ticketing_db`.
  - Redis connection pointing to `localhost:6379`.
  - Service Bus / Floci-AZ AMQP configuration for ports 4577 / 5673.
- [ ] **1.3 — Entry Point & Scheduling:**
  - `TicketingAppInventoryApplication.java` under `com.aieta.ticketing_app_inventory`.
  - Enable scheduling for outbox relay.

### ✅ Milestone 1 Checkpoint
```powershell
# C1: Gradle build compiles cleanly
.\gradlew.bat clean compileJava

# C2: Service boots on port 8082 and health check reports DB + Redis UP
curl http://localhost:8082/actuator/health
```

---

## Milestone 2 — Domain Core & CQRS Handlers (Pure Java 25)

> **Goal:** Pure domain entities and aggregates (`Seat`, `TicketReservation`), value objects, custom domain exceptions, and CQRS commands/queries. Zero Spring or JPA imports in `domain/`.

### Tasks
- [ ] **2.1 — Domain Entities & Aggregates:**
  - `Seat.java`: Aggregate root encapsulating seat coordinates (sector, row, number), price, version (`@Version`), and lifecycle state transitions (`AVAILABLE` $\to$ `HELD` $\to$ `BOOKED` $\to$ `BLOCKED`).
  - `TicketReservation.java`: Aggregate managing temporary hold lifecycle with 10-minute expiration timestamp and buyer reference.
  - Value Objects: `SeatId`, `ReservationId`, `SectorQuota`, `Money`.
- [ ] **2.2 — Domain Events (Java 25 Records):**
  - `SeatHeldEvent.java` (seatId, eventId, buyerId, expiresAt, occurredAt).
  - `ReservationConfirmedEvent.java` (reservationId, seatIds, orderId, confirmedAt).
  - `SeatReleasedEvent.java` (seatId, eventId, reason, occurredAt).
- [ ] **2.3 — Domain Exceptions (RFC 7807 aligned):**
  - `SeatAlreadyHeldException.java` (HTTP 409 Conflict).
  - `SeatNotFoundException.java` (HTTP 404 Not Found).
  - `ReservationExpiredException.java` (HTTP 410 Gone / 422).
  - `InvalidSeatStateTransitionException.java` (HTTP 422 Unprocessable).
- [ ] **2.4 — Outbound Ports:**
  - `SeatRepositoryPort.java`: Finding seats by event, batch saving seats.
  - `ReservationRepositoryPort.java`: Save reservation with transactional outbox (`saveWithOutbox`).
  - `DistributedLockPort.java`: Atomic lock acquisition (`acquireSeatLock`, `releaseSeatLock`).
- [ ] **2.5 — CQRS Commands & Queries:**
  - `application/command/hold_seat/HoldSeatCommand.java` + `Handler`.
  - `application/command/confirm_reservation/ConfirmReservationCommand.java` + `Handler`.
  - `application/command/release_seat/ReleaseSeatCommand.java` + `Handler`.
  - `application/command/generate_seats/BatchGenerateSeatsCommand.java` + `Handler`.
  - `application/query/get_event_seats/GetEventSeatsQuery.java` + `Handler`.

### ✅ Milestone 2 Checkpoint
```powershell
# C3: Domain unit tests run in-memory with zero I/O in < 1 second
.\gradlew.bat test --tests "*DomainUnitTest"
```

---

## Milestone 3 — Database Adapters & Transactional Outbox Pattern

> **Goal:** Relational persistence adapters for PostgreSQL, optimistic concurrency control, and transactional outbox persistence in a single local ACID transaction.

### Tasks
- [ ] **3.1 — Relational Entities & Mappers:**
  - `SeatJpaEntity.java` with `@Version` for optimistic locking.
  - `TicketReservationJpaEntity.java`.
  - `OutboxEventJpaEntity.java` with status `PENDING`, `PUBLISHED`, `FAILED`.
  - Mapping layers converting between JPA entities and rich domain models.
- [ ] **3.2 — Repository Adapters:**
  - `JpaSeatRepositoryAdapter.java` implementing `SeatRepositoryPort`.
  - `JpaReservationRepositoryAdapter.java` implementing `ReservationRepositoryPort` with atomic `saveWithOutbox`.
- [ ] **3.3 — Outbox Relay Scheduler:**
  - `OutboxRelayScheduler.java` polling `outbox_events` with `SELECT ... FOR UPDATE SKIP LOCKED`.
  - Dispatches events to Service Bus emulator and updates status to `PUBLISHED`.

### ✅ Milestone 3 Checkpoint
```powershell
# C4: Slice integration test verifies atomic entity + outbox persistence
.\gradlew.bat test --tests "*ReservationRepositoryIT"
```

---

## Milestone 4 — Distributed Locking (Redlock) & AMQP Bulk Provisioning

> **Goal:** Atomic concurrency control on Redis (`lock:event:{id}:seat:{id}`) with 10-minute TTL and AMQP consumer for `EventCreatedEvent`.

### Tasks
- [ ] **4.1 — Redis Redlock Engine:**
  - Implement atomic reservation Lua script or Redisson Redlock adapter:
    `lock:event:{eventId}:seat:{seatId}` with 600s TTL.
  - Release mechanism: passive TTL expiration and active compensation upon cancellation.
- [ ] **4.2 — AMQP Service Bus Listener:**
  - Consume `EventCreatedEvent` from topic `event.lifecycle.v1` (subscription `ticketing-seat-generator`).
  - Trigger `BatchGenerateSeatsCommand` executing high-throughput batch insert of thousands of seats.

### ✅ Milestone 4 Checkpoint
```powershell
# C5: Redlock atomic hold test verifies 600s TTL and lock collision rejection
.\gradlew.bat test --tests "*DistributedLockTest"
```

---

## Milestone 5 — REST API, RFC 7807 Handlers & Concurrency Stress Suite

> **Goal:** Inbound REST controllers, versioning, OpenAPI documentation, and rigorous 500-thread collision tests verifying zero double-booking.

### Tasks
- [ ] **5.1 — Controllers:**
  - `ReservationController.java` (`POST /api/v1/reservations`, `POST /api/v1/reservations/{id}/confirm`).
  - `SeatQueryController.java` (`GET /api/v1/events/{id}/seats`).
- [ ] **5.2 — Global Exception Handler:**
  - Translates domain exceptions into RFC 7807 `ProblemDetail` with error codes.
- [ ] **5.3 — Concurrency & Collision Test Suite:**
  - 500 virtual threads attempting to reserve the exact same seat (`seatId: 104`) at the exact same millisecond.
  - Assert strictly 1 success (HTTP 201) and 499 conflicts (HTTP 409). Zero overbooking.

### ✅ Milestone 5 Checkpoint
```powershell
# C6: Concurrency race condition test passes with zero double-booking
.\gradlew.bat test --tests "*SeatReservationConcurrencyIT"

# C7: Full test suite passes cleanly
.\gradlew.bat test
```
