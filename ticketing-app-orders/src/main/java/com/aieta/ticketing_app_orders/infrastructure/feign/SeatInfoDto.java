package com.aieta.ticketing_app_orders.infrastructure.feign;

import java.math.BigDecimal;

public record SeatInfoDto(
        String seatId,
        String eventId,
        String status,
        BigDecimal price
) {
}
