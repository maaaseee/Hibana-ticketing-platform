package com.aieta.ticketing_app_orders.application.commands.create_order;

import com.aieta.ticketing_app_orders.application.common.mediator.Request;

import java.math.BigDecimal;

public record CreateOrderCommand(
        String userId,
        String reservationId,
        String eventId,
        String seatId,
        BigDecimal totalAmount
) implements Request<CreateOrderResult> {
}
