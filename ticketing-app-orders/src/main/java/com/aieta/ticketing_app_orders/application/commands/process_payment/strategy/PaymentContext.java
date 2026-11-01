package com.aieta.ticketing_app_orders.application.commands.process_payment.strategy;

import com.aieta.ticketing_app_orders.domain.model.Order;

public record PaymentContext(
        Order order,
        String userId,
        String buyerEmail,
        String buyerName,
        String buyerTaxId,
        Integer installments,
        String issuingBank
) {
    public PaymentContext(Order order, String userId, String buyerEmail, String buyerName, String buyerTaxId) {
        this(order, userId, buyerEmail, buyerName, buyerTaxId, 1, "OTHER");
    }
}
