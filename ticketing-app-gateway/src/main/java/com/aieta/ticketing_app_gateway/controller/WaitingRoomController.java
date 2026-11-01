package com.aieta.ticketing_app_gateway.controller;

import com.aieta.ticketing_app_gateway.dto.QueueJoinRequest;
import com.aieta.ticketing_app_gateway.dto.QueueStatusResponse;
import com.aieta.ticketing_app_gateway.service.WaitingRoomService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/queue")
public class WaitingRoomController {

    private final WaitingRoomService waitingRoomService;

    public WaitingRoomController(WaitingRoomService waitingRoomService) {
        this.waitingRoomService = waitingRoomService;
    }

    private String resolveUserId(String userId) {
        if (userId == null || userId.isBlank()) {
            return "guest-" + UUID.randomUUID().toString().substring(0, 8);
        }
        return userId;
    }

    @PostMapping("/join")
    public Mono<ResponseEntity<QueueStatusResponse>> joinQueue(@RequestBody QueueJoinRequest request) {
        String eventId = request.eventId() != null ? request.eventId() : "default-event";
        String userId = resolveUserId(request.userId());

        return waitingRoomService.joinQueue(eventId, userId)
                .map(res -> ResponseEntity.ok(res));
    }

    @GetMapping("/status")
    public Mono<ResponseEntity<QueueStatusResponse>> getQueueStatus(
            @RequestParam("eventId") String eventId,
            @RequestParam("userId") String userId
    ) {
        return waitingRoomService.getQueueStatus(eventId, userId)
                .map(res -> ResponseEntity.ok(res));
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<QueueStatusResponse>> streamQueueStatus(
            @RequestParam("eventId") String eventId,
            @RequestParam("userId") String userId
    ) {
        return Flux.interval(Duration.ZERO, Duration.ofSeconds(1))
                .concatMap(tick -> waitingRoomService.getQueueStatus(eventId, userId))
                .map(status -> ServerSentEvent.<QueueStatusResponse>builder()
                        .event("queue-status")
                        .data(status)
                        .build()
                )
                .takeUntil(sse -> sse.data() != null && "ADMITTED".equalsIgnoreCase(sse.data().status()));
    }

    @PostMapping("/leave")
    public Mono<ResponseEntity<Void>> leaveQueue(@RequestBody QueueJoinRequest request) {
        String eventId = request.eventId() != null ? request.eventId() : "default-event";
        String userId = resolveUserId(request.userId());

        return waitingRoomService.leaveQueue(eventId, userId)
                .thenReturn(ResponseEntity.noContent().build());
    }

    @PostMapping("/skip")
    public Mono<ResponseEntity<QueueStatusResponse>> skipQueue(@RequestBody QueueJoinRequest request) {
        String eventId = request.eventId() != null ? request.eventId() : "default-event";
        String userId = resolveUserId(request.userId());

        return waitingRoomService.skipOrAdmit(eventId, userId)
                .map(res -> ResponseEntity.ok(res));
    }
}
