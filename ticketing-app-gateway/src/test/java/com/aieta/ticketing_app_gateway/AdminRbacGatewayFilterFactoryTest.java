package com.aieta.ticketing_app_gateway;

import com.aieta.ticketing_app_gateway.auth.FlociEntraJwksProvider;
import com.aieta.ticketing_app_gateway.filter.AdminRbacGatewayFilterFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Unit Test: AdminRbacGatewayFilterFactory Security & Strict Role Enforcement")
class AdminRbacGatewayFilterFactoryTest {

    private AdminRbacGatewayFilterFactory filterFactory;
    private FlociEntraJwksProvider jwksProvider;
    private RSAPrivateKey privateKey;
    private RSAPublicKey publicKey;
    private static final String TEST_KID = "test-kid-entra-001";

    @BeforeEach
    void setUp() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair pair = generator.generateKeyPair();
        this.privateKey = (RSAPrivateKey) pair.getPrivate();
        this.publicKey = (RSAPublicKey) pair.getPublic();

        ObjectMapper objectMapper = new ObjectMapper();
        this.jwksProvider = new FlociEntraJwksProvider(FlociEntraJwksProvider.DEFAULT_JWKS_URL, objectMapper);
        this.jwksProvider.registerKey(TEST_KID, this.publicKey);

        this.filterFactory = new AdminRbacGatewayFilterFactory(jwksProvider, objectMapper);
    }

    private String createToken(String sub, String email, List<String> roles, Instant expiry) {
        return createToken(sub, email, roles, expiry, AdminRbacGatewayFilterFactory.DEFAULT_ISSUER, AdminRbacGatewayFilterFactory.DEFAULT_AUDIENCE);
    }

    private String createToken(String sub, String email, List<String> roles, Instant expiry, String issuer, String audience) {
        var builder = Jwts.builder()
                .header().keyId(TEST_KID).and()
                .subject(sub)
                .claim("preferred_username", email)
                .claim("roles", roles)
                .issuer(issuer)
                .issuedAt(Date.from(Instant.now()))
                .expiration(Date.from(expiry));

        if (audience != null) {
            builder.audience().add(audience).and();
        }

        return builder.signWith(privateKey, Jwts.SIG.RS256)
                .compact();
    }

    @Test
    @DisplayName("Rejects request with 401 when Authorization header is missing")
    void shouldRejectWhenAuthorizationHeaderMissing() {
        AdminRbacGatewayFilterFactory.Config config = new AdminRbacGatewayFilterFactory.Config();
        GatewayFilter filter = filterFactory.apply(config);

        MockServerHttpRequest request = MockServerHttpRequest.post("/api/v1/events").build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        GatewayFilterChain chain = ex -> Mono.empty();

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("Rejects request with 401 when Authorization header is malformed (not Bearer)")
    void shouldRejectWhenAuthorizationHeaderMalformed() {
        AdminRbacGatewayFilterFactory.Config config = new AdminRbacGatewayFilterFactory.Config();
        GatewayFilter filter = filterFactory.apply(config);

        MockServerHttpRequest request = MockServerHttpRequest.post("/api/v1/events")
                .header(HttpHeaders.AUTHORIZATION, "Basic dXNlcjpwYXNz")
                .build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        StepVerifier.create(filter.filter(exchange, ex -> Mono.empty()))
                .verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("Rejects request with 401 when token is expired")
    void shouldRejectWhenTokenIsExpired() {
        AdminRbacGatewayFilterFactory.Config config = new AdminRbacGatewayFilterFactory.Config();
        GatewayFilter filter = filterFactory.apply(config);

        String expiredToken = createToken("usr-admin", "admin@hibana.tk", List.of("ROLE_ADMIN"), Instant.now().minus(1, ChronoUnit.HOURS));

        MockServerHttpRequest request = MockServerHttpRequest.post("/api/v1/events")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + expiredToken)
                .build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        StepVerifier.create(filter.filter(exchange, ex -> Mono.empty()))
                .verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("Rejects request with 401 when token issuer is invalid / untrusted")
    void shouldRejectWhenIssuerDoesNotMatch() {
        AdminRbacGatewayFilterFactory.Config config = new AdminRbacGatewayFilterFactory.Config();
        GatewayFilter filter = filterFactory.apply(config);

        String untrustedIssuerToken = createToken(
                "usr-admin",
                "admin@hibana.tk",
                List.of("ROLE_ADMIN"),
                Instant.now().plus(1, ChronoUnit.HOURS),
                "https://rogue-tenant.microsoftonline.com/v2.0",
                AdminRbacGatewayFilterFactory.DEFAULT_AUDIENCE
        );

        MockServerHttpRequest request = MockServerHttpRequest.post("/api/v1/events")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + untrustedIssuerToken)
                .build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        StepVerifier.create(filter.filter(exchange, ex -> Mono.empty()))
                .verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("Rejects request with 401 when token audience does not match expected client")
    void shouldRejectWhenAudienceDoesNotMatch() {
        AdminRbacGatewayFilterFactory.Config config = new AdminRbacGatewayFilterFactory.Config();
        GatewayFilter filter = filterFactory.apply(config);

        String wrongAudienceToken = createToken(
                "usr-admin",
                "admin@hibana.tk",
                List.of("ROLE_ADMIN"),
                Instant.now().plus(1, ChronoUnit.HOURS),
                AdminRbacGatewayFilterFactory.DEFAULT_ISSUER,
                "api://rogue-application"
        );

        MockServerHttpRequest request = MockServerHttpRequest.post("/api/v1/events")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + wrongAudienceToken)
                .build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        StepVerifier.create(filter.filter(exchange, ex -> Mono.empty()))
                .verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("Rejects request with 401 when token kid is unknown and cannot be resolved")
    void shouldRejectWhenKeyIdIsUnknown() {
        AdminRbacGatewayFilterFactory.Config config = new AdminRbacGatewayFilterFactory.Config();
        GatewayFilter filter = filterFactory.apply(config);

        String unknownKidToken = Jwts.builder()
                .header().keyId("unknown-unregistered-kid").and()
                .subject("usr-admin")
                .claim("preferred_username", "admin@hibana.tk")
                .claim("roles", List.of("ROLE_ADMIN"))
                .issuer(AdminRbacGatewayFilterFactory.DEFAULT_ISSUER)
                .audience().add(AdminRbacGatewayFilterFactory.DEFAULT_AUDIENCE).and()
                .issuedAt(Date.from(Instant.now()))
                .expiration(Date.from(Instant.now().plus(1, ChronoUnit.HOURS)))
                .signWith(privateKey, Jwts.SIG.RS256)
                .compact();

        MockServerHttpRequest request = MockServerHttpRequest.post("/api/v1/events")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + unknownKidToken)
                .build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        StepVerifier.create(filter.filter(exchange, ex -> Mono.empty()))
                .verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("Rejects request with 403 Forbidden when user has valid token but lacks ROLE_ADMIN / ROLE_OPERATOR")
    void shouldRejectWith403WhenUserHasInsufficientRole() {
        AdminRbacGatewayFilterFactory.Config config = new AdminRbacGatewayFilterFactory.Config();
        GatewayFilter filter = filterFactory.apply(config);

        String regularUserToken = createToken("usr-client", "buyer@gmail.com", List.of("ROLE_USER"), Instant.now().plus(1, ChronoUnit.HOURS));

        MockServerHttpRequest request = MockServerHttpRequest.post("/api/v1/events")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + regularUserToken)
                .build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        StepVerifier.create(filter.filter(exchange, ex -> Mono.empty()))
                .verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("Allows request with 200 and injects downstream headers when user has valid token with ROLE_ADMIN")
    void shouldAllowWhenUserHasRoleAdmin() {
        AdminRbacGatewayFilterFactory.Config config = new AdminRbacGatewayFilterFactory.Config();
        GatewayFilter filter = filterFactory.apply(config);

        String adminToken = createToken("usr-alex-001", "alex.thorne@hibana.tk", List.of("ROLE_ADMIN", "ROLE_OPERATOR"), Instant.now().plus(1, ChronoUnit.HOURS));

        MockServerHttpRequest request = MockServerHttpRequest.post("/api/v1/events")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        AtomicBoolean chainCalled = new AtomicBoolean(false);
        GatewayFilterChain chain = ex -> {
            chainCalled.set(true);
            // Verify injected downstream headers
            assertThat(ex.getRequest().getHeaders().getFirst("X-User-Id")).isEqualTo("usr-alex-001");
            assertThat(ex.getRequest().getHeaders().getFirst("X-User-Email")).isEqualTo("alex.thorne@hibana.tk");
            assertThat(ex.getRequest().getHeaders().getFirst("X-User-Roles")).contains("ROLE_ADMIN");
            assertThat(ex.getRequest().getHeaders().getFirst("X-Gateway-Auth-Stamp")).isNotBlank();
            assertThat(ex.getRequest().getHeaders().getFirst("X-Gateway-Timestamp")).isNotBlank();
            assertThat(ex.getRequest().getHeaders().getFirst("X-Gateway-Signature")).isNotBlank();
            assertThat(ex.getRequest().getHeaders().getFirst("X-Gateway-Stamp")).isNotBlank();
            return Mono.empty();
        };

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        assertThat(chainCalled).isTrue();
        assertThat(exchange.getResponse().getStatusCode()).isNull(); // Not rejected
    }

    @Test
    @DisplayName("Allows request when user is seeded Alex Thorne even without explicit roles claim")
    void shouldAllowSeededAdminUser() {
        AdminRbacGatewayFilterFactory.Config config = new AdminRbacGatewayFilterFactory.Config();
        GatewayFilter filter = filterFactory.apply(config);

        // Token with no roles claim, but subject matches seeded Alex Thorne and has valid issuer/audience
        String seededToken = Jwts.builder()
                .header().keyId(TEST_KID).and()
                .subject(FlociEntraJwksProvider.SEED_ADMIN_SUB)
                .issuer(AdminRbacGatewayFilterFactory.DEFAULT_ISSUER)
                .audience().add(AdminRbacGatewayFilterFactory.DEFAULT_AUDIENCE).and()
                .claim("preferred_username", "alex.thorne@hibana.tk")
                .issuedAt(Date.from(Instant.now()))
                .expiration(Date.from(Instant.now().plus(1, ChronoUnit.HOURS)))
                .signWith(privateKey, Jwts.SIG.RS256)
                .compact();

        MockServerHttpRequest request = MockServerHttpRequest.post("/api/v1/events")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + seededToken)
                .build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        AtomicBoolean chainCalled = new AtomicBoolean(false);
        GatewayFilterChain chain = ex -> {
            chainCalled.set(true);
            assertThat(ex.getRequest().getHeaders().getFirst("X-User-Roles")).contains("ROLE_ADMIN");
            return Mono.empty();
        };

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        assertThat(chainCalled).isTrue();
    }
}
