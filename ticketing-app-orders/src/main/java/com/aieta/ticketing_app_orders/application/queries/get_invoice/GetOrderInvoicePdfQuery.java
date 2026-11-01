package com.aieta.ticketing_app_orders.application.queries.get_invoice;

import com.aieta.ticketing_app_orders.application.common.mediator.Request;

public record GetOrderInvoicePdfQuery(
        String orderId,
        String userId
) implements Request<byte[]> {
}
