package com.aieta.ticketing_app_orders.application.dto;

import java.math.BigDecimal;
import java.util.List;

public record OrderConfirmationEmail(
        String orderId,
        String recipientEmail,
        String recipientName,
        String eventTitle,
        String eventDate,
        String venueName,
        List<String> seats,
        BigDecimal totalAmount,
        byte[] pdfInvoiceBytes
) {}
