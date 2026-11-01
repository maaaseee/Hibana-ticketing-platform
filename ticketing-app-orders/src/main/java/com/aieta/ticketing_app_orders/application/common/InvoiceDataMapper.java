package com.aieta.ticketing_app_orders.application.common;

import com.aieta.ticketing_app_orders.application.dto.InvoiceData;
import com.aieta.ticketing_app_orders.domain.model.Order;
import com.aieta.ticketing_app_orders.domain.model.OrderItem;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public final class InvoiceDataMapper {

    private InvoiceDataMapper() {}

    public static InvoiceData fromOrder(Order order, String buyerName, String buyerEmail, String buyerTaxId) {
        String effectiveBuyerName = (buyerName != null && !buyerName.isBlank()) ? buyerName : "Cliente Hibana";
        String effectiveEmail = (buyerEmail != null && !buyerEmail.isBlank())
                ? buyerEmail
                : (order.getUserId().contains("@") ? order.getUserId() : order.getUserId() + "@hibana.tk");
        String effectiveTaxId = (buyerTaxId != null && !buyerTaxId.isBlank())
                ? buyerTaxId
                : "Consumidor Final (DNI: 35.882.109)";

        Instant issuedAt = order.getUpdatedAt() != null ? order.getUpdatedAt() : Instant.now();
        Instant caeExpiresAt = issuedAt.plusSeconds(10L * 86400L); // 10 days validity

        int hash = Math.abs(order.getId().hashCode() % 100_000_000);
        String invoiceNum = String.format("A-0001-%08d", hash);
        String caeNum = "74123890" + String.format("%06d", hash % 1_000_000);

        BigDecimal grandTotal = order.getTotal().amount();
        BigDecimal subtotal = order.getSubtotal() != null ? order.getSubtotal().amount() : grandTotal.multiply(new BigDecimal("0.90")).setScale(2, RoundingMode.HALF_UP);
        BigDecimal serviceFee = order.getServiceFee() != null ? order.getServiceFee().amount() : grandTotal.subtract(subtotal);
        BigDecimal vatTax = grandTotal.subtract(subtotal).subtract(serviceFee);

        List<InvoiceData.InvoiceItem> items = new ArrayList<>();
        if (order.getItems() != null && !order.getItems().isEmpty()) {
            for (OrderItem item : order.getItems()) {
                String seatLabel = item.getSeatId() != null ? "Butaca " + item.getSeatId() : "Sector General";
                items.add(new InvoiceData.InvoiceItem(
                        item.getDescription() != null ? item.getDescription() : "Entrada para evento " + order.getEventId(),
                        seatLabel,
                        item.getQuantity(),
                        item.getUnitPrice(),
                        item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity()))
                ));
            }
        } else {
            items.add(new InvoiceData.InvoiceItem(
                    "Entrada Oficial Hibana - Acceso General",
                    "Butaca " + order.getSeatId(),
                    1,
                    subtotal,
                    subtotal
            ));
        }

        return new InvoiceData(
                invoiceNum,
                order.getId().toString(),
                issuedAt,
                caeNum,
                caeExpiresAt,
                effectiveBuyerName,
                effectiveEmail,
                effectiveTaxId,
                order.getPaymentMethod() != null ? order.getPaymentMethod().name() : "POCKET_WALLET",
                order.getEventId(),
                "Hibana Live Experience - " + order.getEventId(),
                "Sábado 15 de Noviembre, 2026 - 21:00 hs",
                "Estadio Hibana Arena",
                "Av. del Libertador 7401, CABA",
                items,
                subtotal,
                serviceFee,
                vatTax,
                grandTotal,
                "EUR"
        );
    }

    public static InvoiceData createSampleInvoice() {
        Instant now = Instant.now();
        List<InvoiceData.InvoiceItem> sampleItems = List.of(
                new InvoiceData.InvoiceItem(
                        "Entrada Platea Preferencial VIP - Hibana Music Fest",
                        "Sector A, Fila 4, Butaca 12",
                        1,
                        new BigDecimal("85000.00"),
                        new BigDecimal("85000.00")
                ),
                new InvoiceData.InvoiceItem(
                        "Pase Fast-Pass & Merchandising Kit Oficial",
                        "Acceso Preferente al Recinto",
                        1,
                        new BigDecimal("18000.00"),
                        new BigDecimal("18000.00")
                )
        );

        return new InvoiceData(
                "A-0001-00042819",
                "ord-9b1c7420-sample",
                now,
                "74281930491823",
                now.plusSeconds(10L * 86400L),
                "Daniel Casanova",
                "daniel.casanova@hibana.tk",
                "DNI: 35.882.109",
                "TARJETA_CREDITO",
                "event-hibana-music-fest-2026",
                "Hibana Music Fest 2026 - World Tour",
                "Sábado 15 de Noviembre, 2026 - 21:00 hs",
                "Estadio Hibana Arena",
                "Av. del Libertador 7401, CABA",
                sampleItems,
                new BigDecimal("103000.00"),
                new BigDecimal("10300.00"),
                new BigDecimal("23793.00"),
                new BigDecimal("137093.00"),
                "ARS"
        );
    }
}
