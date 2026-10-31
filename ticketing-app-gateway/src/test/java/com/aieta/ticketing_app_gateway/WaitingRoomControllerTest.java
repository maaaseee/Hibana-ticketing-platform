package com.aieta.ticketing_app_gateway;

import com.aieta.ticketing_app_gateway.controller.WaitingRoomController;
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

import java.time.Instant;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("WebFlux API Unit Test: Virtual Waiting Room Controller")
class WaitingRoomControllerTest {

    private WebTestClient webTestClient;

    @Mock
    private WaitingRoomService waitingRoomService;

    @BeforeEach
    void setUp() {
        WaitingRoomController controller = new WaitingRoomController(waitingRoomService);
        this.webTestClient = WebTestClient.bindToController(controller).build();
    }

    @Test
    @DisplayName("POST /api/v1/queue/join returns WAITING status with initial rank")
    void shouldJoinQueueSuccessfully() {
        String eventId = "chronos-fest-2026";
        String userId = "usr-test-100";

        when(waitingRoomService.joinQueue(eq(eventId), eq(userId)))
                .thenReturn(Mono.just(QueueStatusResponse.waiting(eventId, userId, 42L, 100L, 84L)));

        webTestClient.post()
                .uri("/api/v1/queue/join")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new QueueJoinRequest(eventId, userId))
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
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
    @DisplayName("GET /api/v1/queue/status returns ADMITTED status when token is issued")
    void shouldReturnAdmittedStatus() {
        String eventId = "chronos-fest-2026";
        String userId = "usr-test-100";
        String mockJwt = "header.payload.signature";
        long exp = Instant.now().plusSeconds(900).toEpochMilli();

        when(waitingRoomService.getQueueStatus(eq(eventId), eq(userId)))
                .thenReturn(Mono.just(QueueStatusResponse.admitted(eventId, userId, mockJwt, exp)));

        webTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/v1/queue/status")
                        .queryParam("eventId", eventId)
                        .queryParam("userId", userId)
                        .build())
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
    @DisplayName("POST /api/v1/queue/skip admits user immediately for demo")
    void shouldSkipQueueForDemo() {
        String eventId = "chronos-fest-2026";
        String userId = "usr-test-100";
        String mockJwt = "fast-path-token";
        long exp = Instant.now().plusSeconds(900).toEpochMilli();

        when(waitingRoomService.skipOrAdmit(eq(eventId), eq(userId)))
                .thenReturn(Mono.just(QueueStatusResponse.admitted(eventId, userId, mockJwt, exp)));

        webTestClient.post()
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
    @DisplayName("POST /api/v1/queue/leave removes user and returns 204 No Content")
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
                .expectStatus().isNoContent();

        verify(waitingRoomService).leaveQueue(eventId, userId);
    }
}
