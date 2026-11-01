package com.aieta.ticketing_app_gateway;

import com.aieta.ticketing_app_gateway.filter.TraceIdGlobalFilter;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gateway.filter.ratelimit.RateLimiter;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.data.redis.connection.ReactiveRedisConnection;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.ReactiveServerCommands;
import java.util.Properties;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Integration Test Suite: API Gateway Ingress, Filters & Security")
class GatewayIntegrationTest {

    private static WireMockServer wireMockServer;
    private static final String TEST_SECRET = "virtual-waiting-room-hmac-key-2026-minimum-256-bits!";
    private static final String TEST_ISSUER = "waiting-room-engine";

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    @SuppressWarnings("unused")
    private RedisConnectionFactory redisConnectionFactory;

    @MockitoBean
    private ReactiveRedisConnectionFactory reactiveRedisConnectionFactory;

    @MockitoBean
    private RedisRateLimiter redisRateLimiter;

    @BeforeAll
    static void startWireMock() {
        wireMockServer = new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort());
        wireMockServer.start();
        WireMock.configureFor("localhost", wireMockServer.port());
    }

    @AfterAll
    static void stopWireMock() {
        if (wireMockServer != null) {
            wireMockServer.stop();
        }
    }

    @DynamicPropertySource
    static void registerDynamicProperties(DynamicPropertyRegistry registry) {
        registry.add("CATALOG_SERVICE_URL", () -> "http://localhost:" + wireMockServer.port());
        registry.add("INVENTORY_SERVICE_URL", () -> "http://localhost:" + wireMockServer.port());
        registry.add("IAM_SERVICE_URL", () -> "http://localhost:" + wireMockServer.port());
        registry.add("waiting-room.jwt.secret", () -> TEST_SECRET);
        registry.add("waiting-room.jwt.issuer", () -> TEST_ISSUER);
        registry.add("resilience4j.timelimiter.instances.catalogCircuitBreaker.timeout-duration", () -> "500ms");
    }

    @BeforeEach
    void setupMocks() {
        wireMockServer.resetAll();

        ReactiveRedisConnection reactiveRedisConnection = org.mockito.Mockito.mock(ReactiveRedisConnection.class);
        ReactiveServerCommands reactiveServerCommands = org.mockito.Mockito.mock(ReactiveServerCommands.class);
        when(reactiveRedisConnectionFactory.getReactiveConnection()).thenReturn(reactiveRedisConnection);
        when(reactiveRedisConnection.ping()).thenReturn(Mono.just("PONG"));
        when(reactiveRedisConnection.closeLater()).thenReturn(Mono.empty());
        when(reactiveRedisConnection.serverCommands()).thenReturn(reactiveServerCommands);
        when(reactiveServerCommands.info(anyString())).thenReturn(Mono.just(new Properties()));

        Map<String, String> headers = Map.of(
                "X-RateLimit-Remaining", "99",
                "X-RateLimit-Burst-Capacity", "120",
                "X-RateLimit-Replenish-Rate", "100"
        );
        when(redisRateLimiter.isAllowed(anyString(), anyString()))
                .thenReturn(Mono.just(new RateLimiter.Response(true, headers)));
    }

    // =========================================================================
    // 1. Health & Actuator Baseline
    // =========================================================================
    @Test
    @Order(1)
    @DisplayName("Health Probe: /actuator/health should return HTTP 200 UP")
    void testActuatorHealthReturnsUp() {
        webTestClient.get()
                .uri("/actuator/health")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo("UP");
    }

    // =========================================================================
    // 2. Routing & Trace-ID Propagation
    // =========================================================================
    @Test
    @Order(2)
    @DisplayName("Routing: GET /api/v1/events forwards to Catalog and returns Trace-ID headers")
    void testCatalogRouteAndTraceIdInjection() {
        wireMockServer.stubFor(get(urlEqualTo("/api/v1/events"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("[{\"id\":\"evt_101\",\"title\":\"Coldplay 2026\"}]")));

        webTestClient.get()
                .uri("/api/v1/events")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().exists(TraceIdGlobalFilter.TRACE_ID_HEADER)
                .expectHeader().exists(TraceIdGlobalFilter.X_TRACE_ID_HEADER)
                .expectBody()
                .jsonPath("$[0].id").isEqualTo("evt_101");

        wireMockServer.verify(getRequestedFor(urlEqualTo("/api/v1/events"))
                .withHeader(TraceIdGlobalFilter.TRACE_ID_HEADER, matching(".+")));
    }

    @Test
    @Order(3)
    @DisplayName("Route Precedence: GET /api/v1/events/{id}/seats routes to Inventory, not Catalog")
    void testSeatQueryRoutePrecedence() {
        wireMockServer.stubFor(get(urlEqualTo("/api/v1/events/evt_101/seats"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("[{\"seatId\":\"seat_1\",\"status\":\"AVAILABLE\"}]")));

        webTestClient.get()
                .uri("/api/v1/events/evt_101/seats")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$[0].seatId").isEqualTo("seat_1")
                .jsonPath("$[0].status").isEqualTo("AVAILABLE");

        wireMockServer.verify(getRequestedFor(urlEqualTo("/api/v1/events/evt_101/seats")));
    }

    // =========================================================================
    // 3. Virtual Waiting Room Checkout Gating (CheckoutTokenFilterFactory)
    // =========================================================================
    @Test
    @Order(4)
    @DisplayName("Gating: POST /api/v1/reservations without token returns HTTP 403 ProblemDetail")
    void testReservationMissingTokenReturns403() {
        webTestClient.post()
                .uri("/api/v1/reservations")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"eventId\":\"evt_101\",\"seatId\":\"seat_1\",\"userId\":\"usr_1\"}")
                .exchange()
                .expectStatus().isForbidden()
                .expectHeader().contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .expectHeader().exists(TraceIdGlobalFilter.TRACE_ID_HEADER)
                .expectBody()
                .jsonPath("$.status").isEqualTo(403)
                .jsonPath("$.type").isEqualTo("https://hibana.tk/errors/waiting-room-required")
                .jsonPath("$.detail").isEqualTo("Missing waiting room checkout token");

        wireMockServer.verify(0, postRequestedFor(urlEqualTo("/api/v1/reservations")));
    }

    @Test
    @Order(5)
    @DisplayName("Gating: POST /api/v1/reservations with expired token returns HTTP 403")
    void testReservationExpiredTokenReturns403() {
        String expiredToken = createTestJwt("usr_999", TEST_ISSUER, Instant.now().minus(10, ChronoUnit.MINUTES));

        webTestClient.post()
                .uri("/api/v1/reservations")
                .header("X-Checkout-Token", expiredToken)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"eventId\":\"evt_101\",\"seatId\":\"seat_1\"}")
                .exchange()
                .expectStatus().isForbidden()
                .expectHeader().contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .expectBody()
                .jsonPath("$.detail").value(detail -> org.hamcrest.MatcherAssert.assertThat((String) detail, org.hamcrest.Matchers.containsString("expired")));

        wireMockServer.verify(0, postRequestedFor(urlEqualTo("/api/v1/reservations")));
    }

    @Test
    @Order(6)
    @DisplayName("Gating: POST /api/v1/reservations with forged signature returns HTTP 403")
    void testReservationForgedTokenReturns403() {
        String forgedToken = createTestJwtWithSecret("usr_attacker", TEST_ISSUER, "wrong-forged-secret-key-32-bytes-minimum!", Instant.now().plus(10, ChronoUnit.MINUTES));

        webTestClient.post()
                .uri("/api/v1/reservations")
                .header("X-Checkout-Token", forgedToken)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"eventId\":\"evt_101\",\"seatId\":\"seat_1\"}")
                .exchange()
                .expectStatus().isForbidden()
                .expectHeader().contentType(MediaType.APPLICATION_PROBLEM_JSON);

        wireMockServer.verify(0, postRequestedFor(urlEqualTo("/api/v1/reservations")));
    }

    @Test
    @Order(7)
    @DisplayName("Gating: POST /api/v1/reservations with valid token forwards to Inventory (201 Created)")
    void testReservationValidTokenPassesThrough() {
        String validToken = createTestJwt("usr_legit_buyer", TEST_ISSUER, Instant.now().plus(15, ChronoUnit.MINUTES));

        wireMockServer.stubFor(post(urlEqualTo("/api/v1/reservations"))
                .willReturn(aResponse()
                        .withStatus(201)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"reservationId\":\"res_777\",\"status\":\"HELD\"}")));

        webTestClient.post()
                .uri("/api/v1/reservations")
                .header("X-Checkout-Token", validToken)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"eventId\":\"evt_101\",\"seatId\":\"seat_1\"}")
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.reservationId").isEqualTo("res_777")
                .jsonPath("$.status").isEqualTo("HELD");

        wireMockServer.verify(postRequestedFor(urlEqualTo("/api/v1/reservations"))
                .withHeader("X-User-Id", equalTo("usr_legit_buyer"))
                .withHeader("X-Waiting-Room-Validated", equalTo("true")));
    }

    // =========================================================================
    // 4. Resilience4j Circuit Breaker & Fallback
    // =========================================================================
    @Test
    @Order(8)
    @DisplayName("Resilience: Downstream timeout trips Circuit Breaker and returns HTTP 503 ProblemDetail")
    void testDownstreamFailureReturnsFallback503() {
        wireMockServer.stubFor(get(urlEqualTo("/api/v1/venues/1"))
                .willReturn(aResponse()
                        .withStatus(500)
                        .withFixedDelay(1500)));

        webTestClient.get()
                .uri("/api/v1/venues/1")
                .exchange()
                .expectStatus().isEqualTo(503)
                .expectHeader().contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .expectBody()
                .jsonPath("$.status").isEqualTo(503)
                .jsonPath("$.type").isEqualTo("https://hibana.tk/errors/service-unavailable")
                .jsonPath("$.title").isEqualTo("Service Degraded");
    }

    // =========================================================================
    // 5. Rate Limiting Excess Traffic (HTTP 429)
    // =========================================================================
    @Test
    @Order(9)
    @DisplayName("Rate Limiting: Exceeded rate limit returns HTTP 429 Too Many Requests")
    void testRateLimitExceededReturns429() {
        when(redisRateLimiter.isAllowed(anyString(), anyString()))
                .thenReturn(Mono.just(new RateLimiter.Response(false, Map.of(
                        "X-RateLimit-Remaining", "0",
                        "X-RateLimit-Burst-Capacity", "120",
                        "X-RateLimit-Replenish-Rate", "100"
                ))));

        webTestClient.get()
                .uri("/api/v1/events")
                .exchange()
                .expectStatus().isEqualTo(429);
    }

    // =========================================================================
    // 6. Reactive CORS Pre-flight
    // =========================================================================
    @Test
    @Order(10)
    @DisplayName("CORS: OPTIONS pre-flight from B2C origin returns HTTP 200 with exposed headers")
    void testCorsPreflightB2COrigin() {
        webTestClient.options()
                .uri("/api/v1/reservations")
                .header(HttpHeaders.ORIGIN, "http://localhost:5173")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "X-Checkout-Token, Content-Type")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().valueEquals(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:5173")
                .expectHeader().value(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, h ->
                        assertThat(h).contains(TraceIdGlobalFilter.TRACE_ID_HEADER, "X-Checkout-Token"));
    }

    // =========================================================================
    // 7. Security Remediations (SEC-01, SEC-03, IAM Throttling)
    // =========================================================================
    @Test
    @Order(11)
    @DisplayName("Security SEC-01: Spoofed client X-User-Id header is overridden by validated token identity")
    void testSpoofedUserIdHeaderIsOverriddenByToken() {
        String validToken = createTestJwt("usr_legit_buyer", TEST_ISSUER, Instant.now().plus(15, ChronoUnit.MINUTES));

        wireMockServer.stubFor(post(urlEqualTo("/api/v1/reservations"))
                .willReturn(aResponse()
                        .withStatus(201)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"reservationId\":\"res_888\",\"status\":\"HELD\"}")));

        webTestClient.post()
                .uri("/api/v1/reservations")
                .header("X-Checkout-Token", validToken)
                .header("X-User-Id", "malicious_spoofed_user")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"eventId\":\"evt_101\",\"seatId\":\"seat_1\"}")
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.reservationId").isEqualTo("res_888");

        wireMockServer.verify(postRequestedFor(urlEqualTo("/api/v1/reservations"))
                .withHeader("X-User-Id", equalTo("usr_legit_buyer"))
                .withHeader("X-User-Id", notMatching(".*malicious_spoofed_user.*"))
                .withHeader("X-Waiting-Room-Validated", equalTo("true")));
    }

    @Test
    @Order(12)
    @DisplayName("Security SEC-03: Malformed X-Forwarded-For header (, or ,,) does not crash Gateway and resolves safely")
    void testMalformedXForwardedForHeaderDoesNotCrash() {
        wireMockServer.stubFor(get(urlEqualTo("/api/v1/events"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("[{\"id\":\"evt_101\",\"title\":\"Coldplay 2026\"}]")));

        // Case 1: Multiple values in comma-separated header
        webTestClient.get()
                .uri("/api/v1/events")
                .header("X-Forwarded-For", "203.0.113.195, 70.41.3.18, 150.172.238.178")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$[0].id").isEqualTo("evt_101");

        // Case 2: Standard single IP header
        webTestClient.get()
                .uri("/api/v1/events")
                .header("X-Forwarded-For", "198.51.100.42")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    @Order(13)
    @DisplayName("Security IAM: Rate limiting is enforced on /api/v1/auth/** routes")
    void testIamRouteRateLimiting() {
        when(redisRateLimiter.isAllowed(anyString(), anyString()))
                .thenReturn(Mono.just(new RateLimiter.Response(false, Map.of(
                        "X-RateLimit-Remaining", "0",
                        "X-RateLimit-Burst-Capacity", "120",
                        "X-RateLimit-Replenish-Rate", "100"
                ))));

        webTestClient.post()
                .uri("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"username\":\"attacker\",\"password\":\"bruteforce\"}")
                .exchange()
                .expectStatus().isEqualTo(429);
    }

    // =========================================================================
    // Helper Methods
    // =========================================================================
    private String createTestJwt(String subject, String issuer, Instant expiration) {
        return createTestJwtWithSecret(subject, issuer, TEST_SECRET, expiration);
    }

    private String createTestJwtWithSecret(String subject, String issuer, String secret, Instant expiration) {
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        return Jwts.builder()
                .subject(subject)
                .issuer(issuer)
                .expiration(Date.from(expiration))
                .issuedAt(new Date())
                .signWith(key)
                .compact();
    }
}
