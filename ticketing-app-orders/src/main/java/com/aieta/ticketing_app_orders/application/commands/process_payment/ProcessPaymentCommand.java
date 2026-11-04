package com.aieta.ticketing_app_orders.application.commands.process_payment;

import com.aieta.ticketing_app_orders.application.common.mediator.Request;
import com.aieta.ticketing_app_orders.application.dto.InvoiceData;
import com.aieta.ticketing_app_orders.domain.enums.PaymentMethod;

import java.math.BigDecimal;
import java.util.List;

public record ProcessPaymentCommand(
        String orderId,
        String userId,
        PaymentMethod paymentMethod,
        String buyerEmail,
        String buyerName,
        String buyerTaxId,
        Integer installments,
        String issuingBank,
        String eventTitle,
        String eventDate,
        String venueName,
        String venueAddress,
        String currency,
        BigDecimal subtotalAmount,
        BigDecimal serviceFeeAmount,
        BigDecimal vatTaxAmount,
        List<InvoiceData.InvoiceItem> items
) implements Request<PaymentResult> {

    public ProcessPaymentCommand(String orderId, String userId, PaymentMethod paymentMethod) {
        this(orderId, userId, paymentMethod, null, null, null, 1, "OTHER", null, null, null, null, null, null, null, null, null);
    }

    public ProcessPaymentCommand(String orderId, String userId, PaymentMethod paymentMethod,
                                 String buyerEmail, String buyerName, String buyerTaxId) {
        this(orderId, userId, paymentMethod, buyerEmail, buyerName, buyerTaxId, 1, "OTHER", null, null, null, null, null, null, null, null, null);
    }

    public ProcessPaymentCommand(String orderId, String userId, PaymentMethod paymentMethod,
                                 String buyerEmail, String buyerName, String buyerTaxId,
                                 Integer installments, String issuingBank) {
        this(orderId, userId, paymentMethod, buyerEmail, buyerName, buyerTaxId, installments, issuingBank, null, null, null, null, null, null, null, null, null);
    }
}
