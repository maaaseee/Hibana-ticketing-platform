package com.aieta.ticketing_app_orders.application.commands.cancel_order;

import com.aieta.ticketing_app_orders.application.common.mediator.Request;

public record CancelOrderCommand(
        String orderId,
        String userId,
        String reason
) implements Request<Void> {
}
