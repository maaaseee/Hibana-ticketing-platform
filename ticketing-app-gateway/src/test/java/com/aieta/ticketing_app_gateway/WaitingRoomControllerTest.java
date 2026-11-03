package com.aieta.ticketing_app_gateway;

import com.aieta.ticketing_app_gateway.controller.WaitingRoomController;
import com.aieta.ticketing_app_gateway.controller.WaitingRoomDebugController;
import com.aieta.ticketing_app_gateway.dto.QueueJoinRequest;
import com.aieta.ticketing_app_gateway.dto.QueueStatusResponse;
import com.aieta.ticketing_app_gateway.service.WaitingRoomService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("WebFlux API Unit Test: Virtual Waiting Room Controller & State Ownership Gating")
class WaitingRoomControllerTest {

    private WebTestClient webTestClient;
    private WebTestClient debugWebTestClient;

    @Mock
    private WaitingRoomService waitingRoomService;

    @BeforeEach
    void setUp() {
        WaitingRoomController controller = new WaitingRoomController(waitingRoomService);
        this.webTestClient = WebTestClient.bindToController(controller).build();

        WaitingRoomDebugController debugController = new WaitingRoomDebugController(waitingRoomService);
        this.debugWebTestClient = WebTestClient.bindToController(debugController).build();
    }

    @Test
    @DisplayName("POST /api/v1/queue/join returns WAITING status and sets HIBANA_QUEUE_SESSION cookie")
    void shouldJoinQueueSuccessfully() {
        String eventId = "chronos-fest-2026";
        String userId = "usr-test-100";
        String mockSessionToken = "header.payload.signature-session";

        when(waitingRoomService.mintQueueSessionToken(eq(eventId), eq(userId)))
                .thenReturn(mockSessionToken);
        when(waitingRoomService.joinQueue(eq(eventId), eq(userId)))
                .thenReturn(Mono.just(QueueStatusResponse.waiting(eventId, userId, 42L, 100L, 84L)));

        webTestClient.post()
                .uri("/api/v1/queue/join")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new QueueJoinRequest(eventId, userId))
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectHeader().valueEquals("X-Queue-Session", mockSessionToken)
                .expectCookie().valueEquals("HIBANA_QUEUE_SESSION", mockSessionToken)
                .expectBody()
                .jsonPath("$.status").isEqualTo("WAITING")
                .jsonPath("$.position").isEqualTo(42)
                .jsonPath("$.totalInQueue").isEqualTo(100)
                .jsonPath("$.estimatedWaitSeconds").isEqualTo(84)
                .jsonPath("$.eventId").isEqualTo(eventId)
                .jsonPath("$.userId").isEqualTo(userId);

        verify(waitingRoomService).joinQueue(eventId, userId);
    }

    @Test
    @DisplayName("GET /api/v1/queue/status returns ADMITTED status when valid session token cookie is provided")
    void shouldReturnAdmittedStatus() {
        String eventId = "chronos-fest-2026";
        String userId = "usr-test-100";
        String mockSessionToken = "valid-session-jwt";
        String mockJwt = "header.payload.signature";
        long exp = Instant.now().plusSeconds(900).toEpochMilli();

        when(waitingRoomService.validateQueueSessionToken(eq(eventId), eq(userId), eq(mockSessionToken)))
                .thenReturn(true);
        when(waitingRoomService.getQueueStatus(eq(eventId), eq(userId)))
                .thenReturn(Mono.just(QueueStatusResponse.admitted(eventId, userId, mockJwt, exp)));

        webTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/v1/queue/status")
                        .queryParam("eventId", eventId)
                        .queryParam("userId", userId)
                        .build())
                .cookie("HIBANA_QUEUE_SESSION", mockSessionToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo("ADMITTED")
                .jsonPath("$.position").isEqualTo(0)
                .jsonPath("$.checkoutToken").isEqualTo(mockJwt)
                .jsonPath("$.tokenExpiresAt").isEqualTo(exp);

        verify(waitingRoomService).getQueueStatus(eventId, userId);
    }

    @Test
    @DisplayName("GET /api/v1/queue/status rejects with 403 Forbidden when queue session is missing")
    void shouldRejectQueueStatusWhenSessionMissing() {
        String eventId = "chronos-fest-2026";
        String userId = "usr-victim-100";

        webTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/v1/queue/status")
                        .queryParam("eventId", eventId)
                        .queryParam("userId", userId)
                        .build())
                .exchange()
                .expectStatus().isForbidden()
                .expectHeader().contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .expectBody()
                .jsonPath("$.title").isEqualTo("Queue State Access Denied");
    }

    @Test
    @DisplayName("GET /api/v1/queue/status rejects with 403 Forbidden when session token does not match userId")
    void shouldRejectQueueStatusWhenSessionMismatched() {
        String eventId = "chronos-fest-2026";
        String requestedUserId = "usr-victim-100";
        String invalidSessionToken = "attacker-session-token";

        when(waitingRoomService.validateQueueSessionToken(eq(eventId), eq(requestedUserId), eq(invalidSessionToken)))
                .thenReturn(false);

        webTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/v1/queue/status")
                        .queryParam("eventId", eventId)
                        .queryParam("userId", requestedUserId)
                        .build())
                .cookie("HIBANA_QUEUE_SESSION", invalidSessionToken)
                .exchange()
                .expectStatus().isForbidden()
                .expectHeader().contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .expectBody()
                .jsonPath("$.title").isEqualTo("Queue State Access Denied");
    }

    @Test
    @DisplayName("GET /api/v1/queue/stream rejects with 403 Forbidden when session is missing")
    void shouldRejectStreamWhenSessionMissing() {
        String eventId = "chronos-fest-2026";
        String userId = "usr-victim-100";

        webTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/v1/queue/stream")
                        .queryParam("eventId", eventId)
                        .queryParam("userId", userId)
                        .build())
                .accept(MediaType.TEXT_EVENT_STREAM)
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    @DisplayName("POST /api/v1/queue/skip admits user in test/debug profile via WaitingRoomDebugController")
    void shouldSkipQueueInDebugController() {
        String eventId = "chronos-fest-2026";
        String userId = "usr-test-100";
        String mockJwt = "fast-path-token";
        long exp = Instant.now().plusSeconds(900).toEpochMilli();

        when(waitingRoomService.skipOrAdmit(eq(eventId), eq(userId)))
                .thenReturn(Mono.just(QueueStatusResponse.admitted(eventId, userId, mockJwt, exp)));

        debugWebTestClient.post()
                .uri("/api/v1/queue/skip")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new QueueJoinRequest(eventId, userId))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo("ADMITTED")
                .jsonPath("$.checkoutToken").isEqualTo(mockJwt);

        verify(waitingRoomService).skipOrAdmit(eventId, userId);
    }

    @Test
    @DisplayName("POST /api/v1/queue/skip returns 404 Not Found on standard production WaitingRoomController")
    void shouldReturn404ForSkipWhenOnlyProductionControllerRegistered() {
        webTestClient.post()
                .uri("/api/v1/queue/skip")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new QueueJoinRequest("chronos-fest-2026", "usr-123"))
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    @DisplayName("POST /api/v1/queue/leave removes user, returns 204 No Content, and clears cookie")
    void shouldLeaveQueueSuccessfully() {
        String eventId = "chronos-fest-2026";
        String userId = "usr-test-100";

        when(waitingRoomService.leaveQueue(eq(eventId), eq(userId)))
                .thenReturn(Mono.empty());

        webTestClient.post()
                .uri("/api/v1/queue/leave")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new QueueJoinRequest(eventId, userId))
                .exchange()
                .expectStatus().isNoContent()
                .expectCookie().maxAge("HIBANA_QUEUE_SESSION", Duration.ZERO);

        verify(waitingRoomService).leaveQueue(eventId, userId);
    }
}
