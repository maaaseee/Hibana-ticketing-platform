package com.aieta.ticketing_app_orders.application.ports;

import com.aieta.ticketing_app_orders.application.dto.InvoiceData;

public interface InvoicePdfPort {
    byte[] generateInvoicePdf(InvoiceData data);
}
