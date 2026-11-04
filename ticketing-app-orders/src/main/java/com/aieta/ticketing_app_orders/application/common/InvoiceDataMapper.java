package com.aieta.ticketing_app_orders.application.common;

import com.aieta.ticketing_app_orders.application.commands.process_payment.ProcessPaymentCommand;
import com.aieta.ticketing_app_orders.application.dto.InvoiceData;
import com.aieta.ticketing_app_orders.domain.model.Order;
import com.aieta.ticketing_app_orders.domain.model.OrderItem;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class InvoiceDataMapper {

    private static final Map<String, InvoiceData> INVOICE_METADATA_CACHE = new ConcurrentHashMap<>();

    private InvoiceDataMapper() {}

    public static InvoiceData fromOrder(Order order, String buyerName, String buyerEmail, String buyerTaxId) {
        InvoiceData cached = INVOICE_METADATA_CACHE.get(order.getId().toString());
        if (cached != null) {
            if (buyerEmail != null && !buyerEmail.isBlank() && !buyerEmail.equals(cached.buyerEmail())) {
                return new InvoiceData(
                        cached.invoiceNumber(), cached.orderId(), cached.issuedAt(), cached.caeNumber(),
                        cached.caeExpiresAt(), cached.buyerName(), buyerEmail, cached.buyerTaxId(),
                        cached.paymentMethod(), cached.eventId(), cached.eventTitle(), cached.eventDate(),
                        cached.venueName(), cached.venueAddress(), cached.items(), cached.subtotal(),
                        cached.serviceFee(), cached.vatTax(), cached.grandTotal(), cached.currency()
                );
            }
            return cached;
        }
        return fromOrder(order, new ProcessPaymentCommand(
                order.getId().toString(),
                order.getUserId(),
                order.getPaymentMethod(),
                buyerEmail,
                buyerName,
                buyerTaxId
        ));
    }

    public static InvoiceData fromOrder(Order order, ProcessPaymentCommand cmd) {
        String buyerName = cmd != null ? cmd.buyerName() : null;
        String buyerEmail = cmd != null ? cmd.buyerEmail() : null;
        String buyerTaxId = cmd != null ? cmd.buyerTaxId() : null;

        String effectiveBuyerName = (buyerName != null && !buyerName.isBlank()) ? buyerName : "Cliente Hibana";
        String effectiveEmail = (buyerEmail != null && !buyerEmail.isBlank())
                ? buyerEmail
                : (order.getUserId().contains("@") ? order.getUserId() : order.getUserId() + "@hibana.tk");
        String effectiveTaxId = (buyerTaxId != null && !buyerTaxId.isBlank())
                ? (buyerTaxId.toUpperCase().startsWith("DNI") || buyerTaxId.toUpperCase().startsWith("CUIT") || buyerTaxId.toUpperCase().startsWith("CUIL")
                    ? buyerTaxId
                    : "DNI / CUIL: " + buyerTaxId)
                : "Consumidor Final";

        Instant issuedAt = order.getUpdatedAt() != null ? order.getUpdatedAt() : Instant.now();
        Instant caeExpiresAt = issuedAt.plusSeconds(10L * 86400L); // 10 days validity

        int hash = Math.abs(order.getId().hashCode() % 100_000_000);
        String invoiceNum = String.format("A-0001-%08d", hash);
        String caeNum = "74123890" + String.format("%06d", hash % 1_000_000);

        BigDecimal grandTotal = order.getTotal().amount();
        BigDecimal subtotal = (cmd != null && cmd.subtotalAmount() != null)
                ? cmd.subtotalAmount()
                : (order.getSubtotal() != null ? order.getSubtotal().amount() : grandTotal.multiply(new BigDecimal("0.90")).setScale(2, RoundingMode.HALF_UP));
        BigDecimal serviceFee = (cmd != null && cmd.serviceFeeAmount() != null)
                ? cmd.serviceFeeAmount()
                : (order.getServiceFee() != null ? order.getServiceFee().amount() : grandTotal.subtract(subtotal));
        BigDecimal vatTax = (cmd != null && cmd.vatTaxAmount() != null)
                ? cmd.vatTaxAmount()
                : grandTotal.subtract(subtotal).subtract(serviceFee).max(BigDecimal.ZERO);

        String eventTitle = (cmd != null && cmd.eventTitle() != null && !cmd.eventTitle().isBlank())
                ? cmd.eventTitle()
                : "Hibana Live Experience - " + order.getEventId();
        String eventDate = (cmd != null && cmd.eventDate() != null && !cmd.eventDate().isBlank())
                ? cmd.eventDate()
                : "Fecha Confirmada en Ticket Oficial";
        String venueName = (cmd != null && cmd.venueName() != null && !cmd.venueName().isBlank())
                ? cmd.venueName()
                : "Estadio Hibana Arena";
        String venueAddress = (cmd != null && cmd.venueAddress() != null && !cmd.venueAddress().isBlank())
                ? cmd.venueAddress()
                : "Acceso Oficial - Validación QR";
        String currency = (cmd != null && cmd.currency() != null && !cmd.currency().isBlank())
                ? cmd.currency()
                : "ARS";

        List<InvoiceData.InvoiceItem> items = new ArrayList<>();
        if (cmd != null && cmd.items() != null && !cmd.items().isEmpty()) {
            items.addAll(cmd.items());
        } else if (order.getItems() != null && !order.getItems().isEmpty()) {
            for (OrderItem item : order.getItems()) {
                String seatLabel = item.getSeatId() != null ? "Ubicación: " + item.getSeatId() : "Sector General";
                items.add(new InvoiceData.InvoiceItem(
                        item.getDescription() != null && !item.getDescription().startsWith("Ticket for event")
                                ? item.getDescription()
                                : "Entrada Oficial - " + eventTitle,
                        seatLabel,
                        item.getQuantity(),
                        item.getUnitPrice(),
                        item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity()))
                ));
            }
        } else {
            items.add(new InvoiceData.InvoiceItem(
                    "Entrada Oficial - " + eventTitle,
                    "Ubicación: " + order.getSeatId(),
                    1,
                    subtotal,
                    subtotal
            ));
        }

        String paymentMethodLabel = order.getPaymentMethod() != null ? order.getPaymentMethod().name() : "TARJETA_CREDITO";
        if (cmd != null && cmd.installments() != null && cmd.installments() > 1) {
            paymentMethodLabel += " (" + cmd.installments() + " cuotas)";
        }

        InvoiceData result = new InvoiceData(
                invoiceNum,
                order.getId().toString(),
                issuedAt,
                caeNum,
                caeExpiresAt,
                effectiveBuyerName,
                effectiveEmail,
                effectiveTaxId,
                paymentMethodLabel,
                order.getEventId(),
                eventTitle,
                eventDate,
                venueName,
                venueAddress,
                items,
                subtotal,
                serviceFee,
                vatTax,
                grandTotal,
                currency
        );
        INVOICE_METADATA_CACHE.put(order.getId().toString(), result);
        return result;
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
