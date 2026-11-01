package com.aieta.ticketing_app_orders.application.commands.process_payment.strategy;

import com.aieta.ticketing_app_orders.domain.enums.PaymentMethod;

public interface PaymentStrategy {
    PaymentMethod supportedMethod();
    PaymentExecutionResult execute(PaymentContext context);
}
