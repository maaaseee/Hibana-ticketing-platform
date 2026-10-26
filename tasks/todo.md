# Tasks Checklist: Spring Cloud Gateway (`ticketing-app-gateway`)

- [ ] Task 1.1: Pre-flight infrastructure & port check
  - Acceptance: Docker compose services verified, port 8080 and Redis 6379 ready.
  - Verify: `docker compose ps` shows Redis running, `netstat -ano | findstr 8080` confirms port free.
  - Files: `docker-compose.yml`

- [ ] Task 2.1: Scaffold `ticketing-app-gateway` project & Gradle setup
  - Acceptance: `ticketing-app-gateway/build.gradle` targeting Java 25 and Spring Boot 4.1.1 + Spring Cloud 2025.1.2 with WebFlux Gateway, Reactive Redis, Resilience4j, and JJWT.
  - Verify: `.\gradlew.bat clean compileJava` compiles with 0 errors.
  - Files: `ticketing-app-gateway/build.gradle`, `ticketing-app-gateway/settings.gradle`, `ticketing-app-gateway/gradle/wrapper/*`

- [ ] Task 2.2: Gateway Application Entry Point & Base Configuration
  - Acceptance: `TicketingAppGatewayApplication.java` boots on port 8080, enables Virtual Threads and Actuator health.
  - Verify: Service starts and responds HTTP 200 on `http://localhost:8080/actuator/health`.
  - Files: `ticketing-app-gateway/src/main/java/com/aieta/ticketing_app_gateway/TicketingAppGatewayApplication.java`, `ticketing-app-gateway/src/main/resources/application.yml`

- [ ] Task 3.1: Implement `TraceIdGlobalFilter` & `GlobalLoggingFilter`
  - Acceptance: Injects `Trace-ID` and `X-Trace-Id` headers into downstream requests and client responses, logs latency and HTTP status.
  - Verify: Unit/WebFlux test confirms headers exist in response.
  - Files: `ticketing-app-gateway/src/main/java/com/aieta/ticketing_app_gateway/filter/TraceIdGlobalFilter.java`, `ticketing-app-gateway/src/main/java/com/aieta/ticketing_app_gateway/filter/GlobalLoggingFilter.java`

- [ ] Task 3.2: Implement `CheckoutTokenFilterFactory` (Waiting Room Enforcement)
  - Acceptance: Intercepts seat reservation paths. Rejects requests lacking valid `X-Checkout-Token` with HTTP 403 Forbidden and RFC 7807 problem detail.
  - Verify: Unit test asserts HTTP 403 when header is missing/invalid and passes when valid JWT is supplied.
  - Files: `ticketing-app-gateway/src/main/java/com/aieta/ticketing_app_gateway/filter/CheckoutTokenFilterFactory.java`

- [ ] Task 3.3: Global CORS & Security Configuration
  - Acceptance: Pre-flight OPTIONS requests allowed, B2C (5173) and B2B (5174) origins allowed, headers exposed.
  - Verify: Test asserts CORS headers on cross-origin requests.
  - Files: `ticketing-app-gateway/src/main/java/com/aieta/ticketing_app_gateway/config/CorsConfig.java`

- [ ] Task 4.1: Redis Token Bucket Rate Limiter & Key Resolver
  - Acceptance: Client IP `KeyResolver` configured with Redis rate limiter (100 req/min, burst 120).
  - Verify: Beans load in context and rate limiter filter is applied to public routes.
  - Files: `ticketing-app-gateway/src/main/java/com/aieta/ticketing_app_gateway/config/RateLimiterConfig.java`

- [ ] Task 4.2: Microservice Routes & Resilience4j Fallback Controller
  - Acceptance: YAML routes for all 6 microservices configured. Fallback controller returns HTTP 503 RFC 7807 problem detail on circuit breaker trip.
  - Verify: WireMock integration test routes `/api/catalog/**` and `/api/tickets/**` correctly and triggers fallback on timeout.
  - Files: `ticketing-app-gateway/src/main/resources/application.yml`, `ticketing-app-gateway/src/main/java/com/aieta/ticketing_app_gateway/controller/FallbackController.java`

- [ ] Task 5.1: Integration Testing Suite
  - Acceptance: Full `@SpringBootTest(webEnvironment = RANDOM_PORT)` suite testing routing, filters, and health.
  - Verify: `.\gradlew.bat test` passes 100%.
  - Files: `ticketing-app-gateway/src/test/java/com/aieta/ticketing_app_gateway/GatewayIntegrationTest.java`

- [ ] Task 5.2: Knowledge Graph Synchronization
  - Acceptance: Codebase knowledge graph updated with new gateway nodes and relationships.
  - Verify: Run `graphify update .` successfully.
  - Files: `graphify-out/graph.json`
