package com.aieta.ticketing_app_gateway.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("WebFlux Gateway Test: Native Documentation Controller")
class DocumentationControllerTest {

    private WebTestClient webTestClient;

    @BeforeEach
    void setUp() {
        Resource openApiResource = new ByteArrayResource(
                "openapi: 3.1.0\ninfo:\n  title: Hibana Ticketing\n".getBytes(StandardCharsets.UTF_8)
        );
        Resource asyncApiResource = new ByteArrayResource(
                "asyncapi: 3.0.0\ninfo:\n  title: Hibana Event Mesh\n".getBytes(StandardCharsets.UTF_8)
        );

        DocumentationController controller = new DocumentationController(openApiResource, asyncApiResource);
        this.webTestClient = WebTestClient.bindToController(controller).build();
    }

    @Test
    @DisplayName("GET /docs returns interactive developer portal HTML")
    void shouldReturnDocumentationPortalHtml() {
        webTestClient.get()
                .uri("/docs")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.TEXT_HTML)
                .expectBody(String.class)
                .value(body -> {
                    assertThat(body).contains("Hibana Ticketing — Microservices Developer Portal");
                    assertThat(body).contains("api-reference");
                    assertThat(body).contains("/docs/openapi.yaml");
                    assertThat(body).contains("Azure Service Bus & Transactional Outbox Event Bus");
                });
    }

    @Test
    @DisplayName("GET /docs/openapi.yaml returns raw OpenAPI 3.1 specification")
    void shouldReturnOpenApiYamlSpec() {
        webTestClient.get()
                .uri("/docs/openapi.yaml")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().value("Content-Type", ct -> assertThat(ct).contains("text/yaml"))
                .expectBody(String.class)
                .value(body -> {
                    assertThat(body).contains("openapi: 3.1.0");
                    assertThat(body).contains("Hibana Ticketing");
                });
    }

    @Test
    @DisplayName("GET /docs/asyncapi.yaml returns raw AsyncAPI 3.0 specification")
    void shouldReturnAsyncApiYamlSpec() {
        webTestClient.get()
                .uri("/docs/asyncapi.yaml")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().value("Content-Type", ct -> assertThat(ct).contains("text/yaml"))
                .expectBody(String.class)
                .value(body -> {
                    assertThat(body).contains("asyncapi: 3.0.0");
                    assertThat(body).contains("Hibana Event Mesh");
                });
    }
}
