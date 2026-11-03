package com.aieta.ticketing_app_gateway.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

@DisplayName("Unit Test: FallbackController and Perimeter Block Handling")
class FallbackControllerTest {

    private WebTestClient webTestClient;

    @BeforeEach
    void setUp() {
        FallbackController controller = new FallbackController();
        this.webTestClient = WebTestClient.bindToController(controller).build();
    }

    @Test
    @DisplayName("GET /fallback/inventory returns HTTP 503 SERVICE_UNAVAILABLE ProblemDetail")
    void shouldReturnServiceUnavailableForServiceFallback() {
        webTestClient.get()
                .uri("/fallback/inventory")
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.SERVICE_UNAVAILABLE)
                .expectHeader().contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .expectBody()
                .jsonPath("$.title").isEqualTo("Service Degraded")
                .jsonPath("$.status").isEqualTo(503)
                .jsonPath("$.service").isEqualTo("inventory");
    }

    @Test
    @DisplayName("GET /fallback/blocked returns HTTP 404 NOT_FOUND ProblemDetail for perimeter-blocked routes")
    void shouldReturnNotFoundForBlockedRoute() {
        webTestClient.get()
                .uri("/fallback/blocked")
                .exchange()
                .expectStatus().isNotFound()
                .expectHeader().contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .expectBody()
                .jsonPath("$.title").isEqualTo("Endpoint Not Found")
                .jsonPath("$.status").isEqualTo(404)
                .jsonPath("$.detail").isEqualTo("The requested resource is an internal endpoint and cannot be accessed via the public Gateway.");
    }
}
