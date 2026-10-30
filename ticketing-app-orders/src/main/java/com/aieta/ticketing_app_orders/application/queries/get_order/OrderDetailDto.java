package com.aieta.ticketing_app_orders.application.queries.get_order;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record OrderDetailDto(
        String orderId,
        String userId,
        String reservationId,
        String eventId,
        String seatId,
        BigDecimal totalAmount,
        String currency,
        String status,
        String paymentMethod,
        Instant createdAt,
        Instant updatedAt,
        List<OrderItemDto> items
) {
    public record OrderItemDto(
            String itemId,
            String eventId,
            String seatId,
            String sectorId,
            BigDecimal unitPrice,
            int quantity,
            String description
    ) {}
}
