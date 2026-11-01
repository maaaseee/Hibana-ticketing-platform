package com.aieta.ticketing_app_orders.application.commands.process_payment;

import com.aieta.ticketing_app_orders.application.common.mediator.Request;
import com.aieta.ticketing_app_orders.domain.enums.PaymentMethod;

public record ProcessPaymentCommand(
        String orderId,
        String userId,
        PaymentMethod paymentMethod,
        String buyerEmail,
        String buyerName,
        String buyerTaxId,
        Integer installments,
        String issuingBank
) implements Request<PaymentResult> {

    public ProcessPaymentCommand(String orderId, String userId, PaymentMethod paymentMethod) {
        this(orderId, userId, paymentMethod, null, null, null, 1, "OTHER");
    }

    public ProcessPaymentCommand(String orderId, String userId, PaymentMethod paymentMethod,
                                 String buyerEmail, String buyerName, String buyerTaxId) {
        this(orderId, userId, paymentMethod, buyerEmail, buyerName, buyerTaxId, 1, "OTHER");
    }
}
