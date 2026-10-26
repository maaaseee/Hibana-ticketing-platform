# Spec-Driven Development: Spring Cloud Gateway (`ticketing-app-gateway`)

> **Document Type:** System Specification & Teamwork Execution Plan  
> **Service:** `ticketing-app-gateway` (`com.aieta.ticketing_app_gateway`)  
> **Bounded Context:** Edge API Gateway & Traffic Ingress Management  
> **Architecture Pattern:** Reactive Gateway (Project Reactor / WebFlux Netty)  
> **Tech Stack:** Java 25 · Spring Boot 4 (`4.1.1`) · Spring Cloud Gateway (`2025.1.x`) · Redis Token Bucket  
> **Default Port:** `8080`  
> **Lead Orchestrator:** Planner & System Architect (Teamwork Mode)  

---

## 1. Context & Business Intent

The Amaryllis Tickets platform serves two distinct client populations with extreme concurrency requirements:
1. **Front-Office (B2C - `www.amaryllis-tickets.com`):** High-concurrency ticket buyers requiring sub-second response times, protected by a Virtual Waiting Room during high-demand on-sale bursts.
2. **Back-Office (B2B - `admin.amaryllis-tickets.com`):** Venue operators and event administrators using an interactive HTML5 Canvas seat map, requiring dedicated operational availability even during B2C flash surges.

The **Spring Cloud Gateway (`ticketing-app-gateway`)** acts as the perimeter ingress controller for all client traffic. It guarantees:
- **Unified Routing:** Intelligently directing API traffic across the 6 bounded contexts (`Catalog`, `Inventory`, `Orders`, `Support`, `Recommendations`, `IAM`).
- **Traffic Protection & Rate Limiting:** Enforcing 100 req/min per IP rate limits using Redis Token Bucket to neutralize DDoS and scraping.
- **Virtual Waiting Room Enforcement:** Intercepting seat reservation endpoints (`/api/tickets/reserve/**`, `/api/inventory/reserve/**`) with a custom `CheckoutTokenFilterFactory` that mandates a valid, signed `X-Checkout-Token` before letting traffic touch transactional pods.
- **Distributed Observability:** Generating and propagating `Trace-ID` across downstream services and logs.
- **Resilience:** Circuit breaking with Resilience4j and fallback responses for degraded downstream services.

---

## 2. Locked Dependencies Specification

> **CRITICAL RULE:** No agent is permitted to introduce dependencies outside this specification.

### 2.1 Backend (`build.gradle`)

```groovy
plugins {
    id 'java'
    id 'org.springframework.boot' version '4.1.1'
    id 'io.spring.dependency-management' version '1.1.7'
}

group = 'com.aieta.ticketing_app_gateway'
version = '0.0.1-SNAPSHOT'

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

repositories {
    mavenCentral()
}

ext {
    set('springCloudVersion', "2025.1.2")
    set('jjwtVersion', "0.12.6")
}

dependencies {
    // 1. Reactive API Gateway Core
    implementation 'org.springframework.cloud:spring-cloud-starter-gateway-server-webflux'
    
    // 2. Distributed Rate Limiting via Redis Reactive Token Bucket
    implementation 'org.springframework.boot:spring-boot-starter-data-redis-reactive'
    
    // 3. Actuator & Observability
    implementation 'org.springframework.boot:spring-boot-starter-actuator'
    
    // 4. Circuit Breaker (Resilience4j Reactor)
    implementation 'org.springframework.cloud:spring-cloud-starter-circuitbreaker-reactor-resilience4j'
    
    // 5. JWT Validation for X-Checkout-Token (Virtual Waiting Room pass)
    implementation "io.jsonwebtoken:jjwt-api:${jjwtVersion}"
    runtimeOnly "io.jsonwebtoken:jjwt-impl:${jjwtVersion}"
    runtimeOnly "io.jsonwebtoken:jjwt-jackson:${jjwtVersion}"

    developmentOnly 'org.springframework.boot:spring-boot-devtools'

    // Testing Suite
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
    testImplementation 'io.projectreactor:reactor-test'
    testImplementation 'org.wiremock:wiremock-standalone:3.10.0'
    testRuntimeOnly 'org.junit.platform:junit-platform-launcher'
}

dependencyManagement {
    imports {
        mavenBom "org.springframework.cloud:spring-cloud-dependencies:${springCloudVersion}"
    }
}

tasks.withType(JavaCompile).configureEach {
    options.compilerArgs.add("-parameters")
}

tasks.named('test') {
    useJUnitPlatform()
}
```

### 2.2 Infrastructure (`docker-compose.yml`)

The Gateway relies on existing infrastructure services defined in `docker-compose.yml`:
- `ticketing-redis`: Redis 7 (`localhost:6379`) for `RequestRateLimiter` key tracking.
- Network: `ticketing-net`.

---

## 3. Explicit MUST DO (Requirements & Invariants)

- [ ] **Reactive Stack:** Built exclusively on Spring Cloud Gateway Reactive WebFlux (Netty). Never include `spring-boot-starter-web` or `spring-boot-starter-webmvc`.
- [ ] **Port Allocation:** Service listens on port `8080`.
- [ ] **Route Mapping:**
  - `catalog-service`: `/api/catalog/**`, `/api/events/**`, `/api/venues/**` $\to$ `http://localhost:8081`
  - `inventory-service`: `/api/tickets/**`, `/api/inventory/**`, `/api/seats/**` $\to$ `http://localhost:8082`
  - `inventory-checkout-gated`: `/api/tickets/reserve/**`, `/api/inventory/reserve/**` $\to$ `http://localhost:8082` (Gated by `CheckoutTokenFilterFactory`)
  - `order-service`: `/api/orders/**`, `/api/payments/**`, `/api/wallets/**` $\to$ `http://localhost:8083`
  - `support-service`: `/api/support/**`, `/ws/chat/**` $\to$ `http://localhost:8084`
  - `recommendation-service`: `/api/recommendations/**` $\to$ `http://localhost:8085`
  - `iam-service`: `/api/auth/**`, `/api/users/**` $\to$ `http://localhost:8086`
- [ ] **`CheckoutTokenFilterFactory` Invariants:**
  - Intercepts requests on checkout/reservation endpoints.
  - Verifies presence of HTTP header `X-Checkout-Token`.
  - Parses and cryptographically validates the token (HMAC-SHA256 signature, expiry, issuer `amaryllis-waiting-room`).
  - If missing, invalid, or expired: Blocks request immediately with **HTTP 403 Forbidden**, returns RFC 7807 `application/problem+json` with waiting room redirection link, and aborts downstream call.
- [ ] **Redis Token Bucket Rate Limiting:**
  - Implements `KeyResolver` extracting client IP (supporting `X-Forwarded-For`).
  - Limits public endpoints to `replenishRate: 100`, `burstCapacity: 120` (tokens per minute).
  - Emits HTTP 429 (*Too Many Requests*) when budget exceeded.
- [ ] **Trace-ID Observability:**
  - `TraceIdGlobalFilter` extracts incoming `Trace-ID` or generates a new UUID.
  - Injects `Trace-ID` and `X-Trace-Id` into downstream request headers and client response headers.
- [ ] **CORS Configuration:**
  - Supports B2C (`http://localhost:5173`, `https://www.amaryllis-tickets.com`) and B2B (`http://localhost:5174`, `https://admin.amaryllis-tickets.com`).
  - Exposes headers: `Trace-ID`, `X-Trace-Id`, `X-Checkout-Token`.
- [ ] **Circuit Breaker Fallbacks:**
  - Uses Resilience4j with fallback route `/fallback/{service}` responding with HTTP 503 and RFC 7807 payload.

---

## 4. Explicit MUST NOT DO (Strict Anti-Patterns)

- ❌ **MUST NOT** include `spring-boot-starter-web` or `spring-boot-starter-tomcat` (would break WebFlux Netty event loop).
- ❌ **MUST NOT** perform blocking I/O calls (`Thread.sleep()`, JDBC, synchronous blocking Feign clients) within Gateway filters.
- ❌ **MUST NOT** allow reservation endpoints (`/api/tickets/reserve/**`, `/api/inventory/reserve/**`) to be routed without passing through `CheckoutTokenFilterFactory`.
- ❌ **MUST NOT** hardcode secrets or JWT signing keys in source code (must load via environment/config).
- ❌ **MUST NOT** add unbudgeted dependencies to `build.gradle`.

---

## 5. Teamwork Mode: Subagent Roles & Worktree Allocations

```
┌────────────────────────────────────────────────────────┐
│            Planner & Architect (Orchestrator)          │
│   Specification, Contracts, Worktrees, Dependency Lock │
└──────────────┬──────────────────┬──────────────────────┘
               │                  │
               ▼                  ▼
┌─────────────────────────┐  ┌─────────────────────────┐
│ Infra & Data Engineer   │  │ Backend Engineer        │
│ .worktrees/infra-gateway│  │ .worktrees/backend-gate.│
│ Local Redis & Health    │  │ Spring Gateway & Filters│
└──────────────┬──────────┘  └───────────┬─────────────┘
               │                         │
               └───────────┬─────────────┘
                           ▼
             ┌─────────────────────────┐
             │ QA & Integrator Engineer│
             │ .worktrees/qa-gateway   │
             │ Integration & k6 Tests  │
             └─────────────────────────┘
```

| Agent Role | Worktree Path | Primary Responsibility |
| :--- | :--- | :--- |
| **Planner & Architect** | Main repo / docs | Specifications, routing architecture, dependency lock, milestone gating. |
| **Infra & Data Engineer** | `.worktrees/infra-gateway` | Verify Redis availability, validate port allocations (8080-8086), verify container network connectivity. |
| **Backend Engineer** | `.worktrees/backend-gateway` | Implement `ticketing-app-gateway/` module, filters (`CheckoutTokenFilterFactory`, `TraceIdGlobalFilter`), CORS, and YAML routes. |
| **QA & Integrator Engineer**| `.worktrees/qa-gateway` | Develop `WebTestClient` integration test suite, WireMock downstream stubs, and k6 Scenario 3 rate limiting verification. |

---

## 6. Verification & Acceptance Gates

```powershell
# C1: Gradle build compiles cleanly with Java 25
.\gradlew.bat clean compileJava

# C2: Unit and slice tests pass
.\gradlew.bat test

# C3: Health check returns UP with Redis Reactive status
curl -i http://localhost:8080/actuator/health

# C4: Missing X-Checkout-Token on reservation endpoint returns HTTP 403
curl -i -X POST http://localhost:8080/api/tickets/reserve/event-101/seat-104
# Expected: HTTP/1.1 403 Forbidden with problem+json

# C5: Valid X-Checkout-Token passes through to downstream service
curl -i -X POST http://localhost:8080/api/tickets/reserve/event-101/seat-104 -H "X-Checkout-Token: <valid_token>"
```
