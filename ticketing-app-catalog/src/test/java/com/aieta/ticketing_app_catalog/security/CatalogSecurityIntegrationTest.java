package com.aieta.ticketing_app_catalog.security;

import com.aieta.ticketing_app_catalog.infrastructure.security.EntraJwtAuthenticationConverter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class CatalogSecurityIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private EntraJwtAuthenticationConverter jwtConverter;

    @Autowired
    private com.aieta.ticketing_app_catalog.infrastructure.security.GatewayStampValidator stampValidator;

    private final RestTemplate restTemplate = new RestTemplate();

    private String getBaseUrl() {
        return "http://localhost:" + port;
    }

    @Test
    @DisplayName("Public read: GET /api/v1/events is accessible anonymously (HTTP 200)")
    void getEvents_anonymously_shouldSucceed() {
        ResponseEntity<String> response = restTemplate.getForEntity(getBaseUrl() + "/api/v1/events", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("Public read with non-public status: GET /api/v1/events?status=ALL returns HTTP 403 Forbidden")
    void getEvents_withNonPublicStatus_shouldReturn403() {
        HttpClientErrorException ex = assertThrows(HttpClientErrorException.class, () ->
                restTemplate.getForEntity(getBaseUrl() + "/api/v1/events?status=ALL", String.class)
        );
        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("Admin endpoint: GET /api/v1/admin/events without authentication returns HTTP 401 Unauthorized")
    void getAdminEvents_unauthenticated_shouldReturn401() {
        HttpClientErrorException ex = assertThrows(HttpClientErrorException.class, () ->
                restTemplate.getForEntity(getBaseUrl() + "/api/v1/admin/events", String.class)
        );
        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("Admin endpoint: GET /api/v1/admin/events with ROLE_USER returns HTTP 403 Forbidden")
    void getAdminEvents_withUserRole_shouldReturn403() {
        String userId = "usr-lucia-regular";
        String roles = "ROLE_USER";
        long now = System.currentTimeMillis();
        String sig = stampValidator.generateSignature(userId, roles, now);

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-User-Id", userId);
        headers.set("X-User-Roles", roles);
        headers.set("X-Gateway-Timestamp", String.valueOf(now));
        headers.set("X-Gateway-Signature", sig);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        HttpClientErrorException ex = assertThrows(HttpClientErrorException.class, () ->
                restTemplate.exchange(getBaseUrl() + "/api/v1/admin/events", HttpMethod.GET, entity, String.class)
        );
        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("Admin endpoint: GET /api/v1/admin/events with ROLE_ADMIN returns HTTP 200 OK with all events")
    void getAdminEvents_withAdminRole_shouldReturn200() {
        String userId = "usr-alex-admin";
        String roles = "ROLE_ADMIN";
        long now = System.currentTimeMillis();
        String sig = stampValidator.generateSignature(userId, roles, now);

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-User-Id", userId);
        headers.set("X-User-Roles", roles);
        headers.set("X-Gateway-Timestamp", String.valueOf(now));
        headers.set("X-Gateway-Signature", sig);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<String> response = restTemplate.exchange(
                getBaseUrl() + "/api/v1/admin/events?status=ALL",
                HttpMethod.GET,
                entity,
                String.class
        );
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("Admin endpoint: POST /api/v1/events without authentication should return HTTP 401 Unauthorized")
    void postEvents_unauthenticated_shouldReturn401() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>("{}", headers);

        HttpClientErrorException ex = assertThrows(HttpClientErrorException.class, () ->
                restTemplate.postForEntity(getBaseUrl() + "/api/v1/events", entity, String.class)
        );

        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("Admin endpoint: POST /api/v1/events with spoofed unverified ROLE_ADMIN header returns HTTP 401 Unauthorized")
    void postEvents_withSpoofedAdminRole_shouldReturn401() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-User-Id", "usr-alex-admin");
        headers.set("X-User-Roles", "ROLE_ADMIN");
        // No Gateway Stamp or invalid signature!
        headers.set("X-Gateway-Signature", "forged_untrusted_signature");
        HttpEntity<String> entity = new HttpEntity<>("{}", headers);

        HttpClientErrorException ex = assertThrows(HttpClientErrorException.class, () ->
                restTemplate.postForEntity(getBaseUrl() + "/api/v1/events", entity, String.class)
        );

        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("Admin endpoint: POST /api/v1/events with keyless hashCode stamp returns HTTP 401 Unauthorized (Candidate 4 rejection)")
    void postEvents_withKeylessHashCodeStamp_shouldReturn401() {
        String hexHash = Integer.toHexString("usr-alex-admin:ROLE_ADMIN".hashCode());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-User-Id", "usr-alex-admin");
        headers.set("X-User-Roles", "ROLE_ADMIN");
        headers.set("X-Gateway-Stamp", hexHash);
        HttpEntity<String> entity = new HttpEntity<>("{}", headers);

        HttpClientErrorException ex = assertThrows(HttpClientErrorException.class, () ->
                restTemplate.postForEntity(getBaseUrl() + "/api/v1/events", entity, String.class)
        );

        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("Admin endpoint: POST /api/v1/events with verified ROLE_USER returns HTTP 403 Forbidden")
    void postEvents_withUserRole_shouldReturn403() {
        String userId = "usr-lucia-regular";
        String roles = "ROLE_USER";
        long now = System.currentTimeMillis();
        String sig = stampValidator.generateSignature(userId, roles, now);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-User-Id", userId);
        headers.set("X-User-Roles", roles);
        headers.set("X-Gateway-Timestamp", String.valueOf(now));
        headers.set("X-Gateway-Signature", sig);
        HttpEntity<String> entity = new HttpEntity<>("{}", headers);

        HttpClientErrorException ex = assertThrows(HttpClientErrorException.class, () ->
                restTemplate.postForEntity(getBaseUrl() + "/api/v1/events", entity, String.class)
        );

        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("Admin endpoint: POST /api/v1/events with verified ROLE_ADMIN passes security (returns 400 Bad Request due to empty payload, not 401/403)")
    void postEvents_withAdminRole_shouldPassSecurity() {
        String userId = "usr-alex-admin";
        String roles = "ROLE_ADMIN";
        long now = System.currentTimeMillis();
        String sig = stampValidator.generateSignature(userId, roles, now);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-User-Id", userId);
        headers.set("X-User-Roles", roles);
        headers.set("X-Gateway-Timestamp", String.valueOf(now));
        headers.set("X-Gateway-Signature", sig);
        HttpEntity<String> entity = new HttpEntity<>("{}", headers);

        // When security allows the request through, Spring MVC validates the @Valid @RequestBody CreateEventRequest
        // and returns 400 BAD REQUEST, confirming security check was PASSED!
        HttpClientErrorException ex = assertThrows(HttpClientErrorException.class, () ->
                restTemplate.postForEntity(getBaseUrl() + "/api/v1/events", entity, String.class)
        );

        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("EntraJwtAuthenticationConverter: extracts ROLE_ADMIN from claims and seeded dev identities")
    void entraJwtConverter_extractsAdminRoles() {
        Jwt jwt = new Jwt(
                "mock-token-value",
                Instant.now(),
                Instant.now().plusSeconds(3600),
                Map.of("alg", "RS256"),
                Map.of(
                        "sub", "1c5ca4ea-7651-3d75-b981-cd6e66a12a68",
                        "preferred_username", "alex.thorne@hibana.tk",
                        "roles", List.of("ROLE_ADMIN")
                )
        );

        AbstractAuthenticationToken auth = jwtConverter.convert(jwt);
        assertThat(auth).isNotNull();
        List<String> authorityNames = auth.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList();
        assertThat(authorityNames).contains("ROLE_ADMIN", "ROLE_OPERATOR");
    }
}
