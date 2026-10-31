package com.aieta.ticketing_app_gateway.dto;

public record QueueJoinRequest(
        String eventId,
        String userId
) {}
