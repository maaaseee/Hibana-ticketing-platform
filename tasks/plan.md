# Implementation Plan: Spring Cloud Gateway (`ticketing-app-gateway`)

> **Service:** `ticketing-app-gateway` (`com.aieta.ticketing_app_gateway`)  
> **Reference Spec:** [docs/plans/ticketing-app-gateway-plan.md](../docs/plans/ticketing-app-gateway-plan.md)  
> **Status:** APPROVED & READY FOR TEAMWORK EXECUTION  

---

## 1. Architecture Overview & Components

The Gateway acts as the single entry point for all API requests coming from the Front-Office (B2C) and Back-Office (B2B) clients. It is built on **Spring Cloud Gateway (WebFlux / Netty)** running on **Java 25** and **Spring Boot 4.1.1**.

### Key Components:
1. **Entry Point & Configuration (`ticketing-app-gateway`):**
   - Application bootstrap listening on port `8080`.
   - Redis Reactive connectivity for rate limiting.
   - Non-blocking Netty runtime.
2. **Route Configuration (`application.yml`):**
   - Routing table for Catalog (8081), Inventory (8082), Orders (8083), Support (8084), Recommendations (8085), IAM (8086).
   - Resilience4j circuit breaker integration with fallback routes.
3. **Filter Pipeline:**
   - `TraceIdGlobalFilter`: Injects and propagates `Trace-ID` and `X-Trace-Id` headers.
   - `GlobalLoggingFilter`: Observability filter logging request path, method, status code, latency.
   - `CheckoutTokenFilterFactory`: Custom gateway filter validating `X-Checkout-Token` on seat reservation paths. Rejects unvalidated requests with HTTP 403 Forbidden.
   - `RedisRateLimiterConfig`: IP-based Token Bucket rate limiting (100 req/min).
   - `CorsConfiguration`: Global CORS rules for B2C (`http://localhost:5173`) and B2B (`http://localhost:5174`).
4. **Fallback Controller:**
   - WebFlux reactive controller returning RFC 7807 `application/problem+json` when circuit breakers open.

---

## 2. Implementation Order & Teamwork Worktree Strategy

Implementation is partitioned across specialized subagents using isolated Git worktrees:

```
Phase 1: Pre-flight & Infra (Infra & Data Agent: .worktrees/infra-gateway)
   │
Phase 2: Project Scaffold & Dependencies (Backend Agent: .worktrees/backend-gateway)
   │
Phase 3: Core Filters & Security Gate (Backend Agent: .worktrees/backend-gateway)
   │
Phase 4: Routing Table & Resilience Fallbacks (Backend Agent: .worktrees/backend-gateway)
   │
Phase 5: Verification & End-to-End Stress Testing (QA & Integrator: .worktrees/qa-gateway)
```

### Risk & Mitigation Strategies
- **Risk:** Classpath pollution if `spring-boot-starter-web` (blocking Tomcat) is added.  
  *Mitigation:* Explicit MUST NOT DO boundary. The project uses only `spring-cloud-starter-gateway-server-webflux`.
- **Risk:** Virtual Waiting Room bypass if route predicates do not capture all reservation variations.  
  *Mitigation:* Route predicate regex covers both `/api/tickets/reserve/**` and `/api/inventory/reserve/**`.
- **Risk:** Redis rate limiter connection failure stalling the gateway.  
  *Mitigation:* Reactive Redis driver with non-blocking error handling and fallback allowance during Redis downtime.

---

## 3. Milestones & Checkpoints

### Milestone 1: Infra & Pre-flight
- [ ] Confirm Redis is healthy on port 6379 via Docker Compose.
- [ ] Verify port 8080 is available.
- **Checkpoint:** `docker compose ps` shows Redis healthy.

### Milestone 2: Scaffold & Baseline Gateway
- [ ] Create `ticketing-app-gateway/` with Gradle wrapper and `build.gradle` (Spring Boot 4.1.1 + Spring Cloud 2025.1.2 + Java 25 toolchain).
- [ ] Configure `application.yml` with port 8080 and Actuator endpoints.
- **Checkpoint:** `.\gradlew.bat clean compileJava` passes, `/actuator/health` returns `UP`.

### Milestone 3: Observability & Security Filters
- [ ] Implement `TraceIdGlobalFilter`.
- [ ] Implement `CheckoutTokenFilterFactory` with cryptographic verification of `X-Checkout-Token`.
- [ ] Implement `GlobalLoggingFilter`.
- [ ] Configure global reactive CORS filter.
- **Checkpoint:** Slice tests pass verifying missing `X-Checkout-Token` produces HTTP 403.

### Milestone 4: Routes, Redis Rate Limiting & Circuit Breakers
- [ ] Configure YAML route predicates and filters for all 6 microservices.
- [ ] Configure Redis Token Bucket `RequestRateLimiter` with client IP `KeyResolver`.
- [ ] Configure Resilience4j circuit breakers and `/fallback` endpoint.
- **Checkpoint:** Downstream routing through WireMock responds correctly; circuit breaker trips on failure.

### Milestone 5: Verification & Knowledge Graph
- [ ] Full suite test execution (`.\gradlew.bat test`).
- [ ] Run `graphify update .` to synchronize knowledge graph.
