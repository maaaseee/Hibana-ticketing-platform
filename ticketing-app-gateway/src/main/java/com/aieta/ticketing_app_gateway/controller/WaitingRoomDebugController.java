package com.aieta.ticketing_app_gateway.controller;

import com.aieta.ticketing_app_gateway.dto.QueueJoinRequest;
import com.aieta.ticketing_app_gateway.dto.QueueStatusResponse;
import com.aieta.ticketing_app_gateway.service.WaitingRoomService;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Debug controller exposing the virtual waiting room skip endpoint.
 * Strictly gated under 'test' and 'local-debug' Spring profiles to prevent unauthenticated
 * queue bypass in production environments (C2 vulnerability remediation).
 */
@RestController
@RequestMapping("/api/v1/queue")
@Profile({"test", "local-debug"})
public class WaitingRoomDebugController {

    private final WaitingRoomService waitingRoomService;

    public WaitingRoomDebugController(WaitingRoomService waitingRoomService) {
        this.waitingRoomService = waitingRoomService;
    }

    private String resolveUserId(String userId) {
        if (userId == null || userId.isBlank()) {
            return "guest-" + UUID.randomUUID().toString().substring(0, 8);
        }
        return userId;
    }

    @PostMapping("/skip")
    public Mono<ResponseEntity<QueueStatusResponse>> skipQueue(@RequestBody QueueJoinRequest request) {
        String eventId = request.eventId() != null ? request.eventId() : "default-event";
        String userId = resolveUserId(request.userId());

        return waitingRoomService.skipOrAdmit(eventId, userId)
                .map(ResponseEntity::ok);
    }
}
