package com.aieta.ticketing_app_gateway;

import com.aieta.ticketing_app_gateway.filter.StripInboundSecurityHeadersFilter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.core.Ordered;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Unit Test: StripInboundSecurityHeadersFilter Inbound Header Sanitization")
class StripInboundSecurityHeadersFilterTest {

    private StripInboundSecurityHeadersFilter filter;

    @BeforeEach
    void setUp() {
        filter = new StripInboundSecurityHeadersFilter();
    }

    @Test
    @DisplayName("Filter runs at HIGHEST_PRECEDENCE")
    void shouldHaveHighestPrecedence() {
        assertThat(filter.getOrder()).isEqualTo(Ordered.HIGHEST_PRECEDENCE);
    }

    @Test
    @DisplayName("Strips all forbidden inbound security headers while preserving legitimate headers")
    void shouldStripAllForbiddenHeaders() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/events")
                .header("X-User-Id", "spoofed-user-id")
                .header("X-User-Roles", "ROLE_ADMIN,ROLE_SUPER")
                .header("X-Roles", "ADMIN")
                .header("X-User-Email", "attacker@evil.com")
                .header("X-Gateway-Auth-Stamp", "fake-stamp-123")
                .header("X-Gateway-Signature", "forged-sig-xyz")
                .header("X-Gateway-Stamp", "forged-stamp")
                .header("Authorization", "Bearer valid-token")
                .header("Content-Type", "application/json")
                .header("Custom-Header", "allowed-value")
                .build();

        ServerWebExchange exchange = MockServerWebExchange.from(request);

        AtomicBoolean chainCalled = new AtomicBoolean(false);
        GatewayFilterChain chain = mutatedExchange -> {
            chainCalled.set(true);

            // Assert that none of the forbidden headers exist in the downstream request
            assertThat(mutatedExchange.getRequest().getHeaders().getFirst("X-User-Id")).isNull();
            assertThat(mutatedExchange.getRequest().getHeaders().getFirst("X-User-Roles")).isNull();
            assertThat(mutatedExchange.getRequest().getHeaders().getFirst("X-Roles")).isNull();
            assertThat(mutatedExchange.getRequest().getHeaders().getFirst("X-User-Email")).isNull();
            assertThat(mutatedExchange.getRequest().getHeaders().getFirst("X-Gateway-Auth-Stamp")).isNull();
            assertThat(mutatedExchange.getRequest().getHeaders().getFirst("X-Gateway-Signature")).isNull();
            assertThat(mutatedExchange.getRequest().getHeaders().getFirst("X-Gateway-Stamp")).isNull();

            // Assert that allowed headers are preserved intact
            assertThat(mutatedExchange.getRequest().getHeaders().getFirst("Authorization")).isEqualTo("Bearer valid-token");
            assertThat(mutatedExchange.getRequest().getHeaders().getFirst("Content-Type")).isEqualTo("application/json");
            assertThat(mutatedExchange.getRequest().getHeaders().getFirst("Custom-Header")).isEqualTo("allowed-value");

            return Mono.empty();
        };

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        assertThat(chainCalled).isTrue();
    }

    @Test
    @DisplayName("Case-insensitive header stripping removes lowercase variants")
    void shouldStripCaseInsensitiveVariants() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/events")
                .header("x-user-id", "attacker-lower")
                .header("x-user-roles", "role-lower")
                .header("x-gateway-signature", "sig-lower")
                .build();

        ServerWebExchange exchange = MockServerWebExchange.from(request);

        AtomicBoolean chainCalled = new AtomicBoolean(false);
        GatewayFilterChain chain = mutatedExchange -> {
            chainCalled.set(true);
            assertThat(mutatedExchange.getRequest().getHeaders().getFirst("X-User-Id")).isNull();
            assertThat(mutatedExchange.getRequest().getHeaders().getFirst("x-user-id")).isNull();
            assertThat(mutatedExchange.getRequest().getHeaders().getFirst("X-User-Roles")).isNull();
            assertThat(mutatedExchange.getRequest().getHeaders().getFirst("x-user-roles")).isNull();
            assertThat(mutatedExchange.getRequest().getHeaders().getFirst("X-Gateway-Signature")).isNull();
            return Mono.empty();
        };

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        assertThat(chainCalled).isTrue();
    }
}
