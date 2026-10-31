package com.aieta.ticketing_app_gateway.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record QueueStatusResponse(
        String eventId,
        String userId,
        String status, // WAITING, ADMITTED, NOT_IN_QUEUE
        Long position,
        Long totalInQueue,
        Long estimatedWaitSeconds,
        String checkoutToken,
        Long tokenExpiresAt
) {
    public static QueueStatusResponse admitted(String eventId, String userId, String token, Long expiresAt) {
        return new QueueStatusResponse(eventId, userId, "ADMITTED", 0L, 0L, 0L, token, expiresAt);
    }

    public static QueueStatusResponse waiting(String eventId, String userId, Long position, Long totalInQueue, Long estimatedWaitSeconds) {
        return new QueueStatusResponse(eventId, userId, "WAITING", position, totalInQueue, estimatedWaitSeconds, null, null);
    }

    public static QueueStatusResponse notInQueue(String eventId, String userId) {
        return new QueueStatusResponse(eventId, userId, "NOT_IN_QUEUE", 0L, 0L, 0L, null, null);
    }
}
