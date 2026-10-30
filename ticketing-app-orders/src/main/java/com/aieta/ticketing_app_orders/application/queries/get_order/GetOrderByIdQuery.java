package com.aieta.ticketing_app_orders.application.queries.get_order;

import com.aieta.ticketing_app_orders.application.common.mediator.Request;

public record GetOrderByIdQuery(
        String orderId,
        String userId
) implements Request<OrderDetailDto> {
}
