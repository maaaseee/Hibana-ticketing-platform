package com.aieta.ticketing_app_orders.application.commands.resend_confirmation;

import com.aieta.ticketing_app_orders.application.common.mediator.Request;

public record ResendOrderConfirmationCommand(
        String orderId,
        String userId,
        String customEmail
) implements Request<Void> {
}
