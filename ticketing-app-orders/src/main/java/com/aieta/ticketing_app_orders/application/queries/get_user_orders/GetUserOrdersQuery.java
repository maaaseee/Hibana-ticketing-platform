package com.aieta.ticketing_app_orders.application.queries.get_user_orders;

import com.aieta.ticketing_app_orders.application.common.mediator.Request;
import com.aieta.ticketing_app_orders.application.queries.get_order.OrderDetailDto;

import java.util.List;

public record GetUserOrdersQuery(
        String userId,
        int page,
        int size
) implements Request<List<OrderDetailDto>> {
}
