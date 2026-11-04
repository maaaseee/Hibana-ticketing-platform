package com.aieta.ticketing_app_orders.infrastructure.web;

import com.aieta.ticketing_app_orders.application.commands.process_payment.PaymentResult;
import com.aieta.ticketing_app_orders.application.commands.process_payment.ProcessPaymentCommand;
import com.aieta.ticketing_app_orders.application.common.mediator.Mediator;
import com.aieta.ticketing_app_orders.application.dto.InvoiceData;
import com.aieta.ticketing_app_orders.infrastructure.web.dto.ProcessPaymentRequest;
import com.aieta.ticketing_app_orders.infrastructure.web.dto.ProcessPaymentResponse;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/orders/{orderId}/pay")
public class PaymentController {

    private final Mediator mediator;

    private String resolveUserId(String headerUserId, Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getName())) {
            return authentication.getName();
        }
        return "anonymous-user";
    }

    public PaymentController(Mediator mediator) {
        this.mediator = mediator;
    }

    @PostMapping
    public ProcessPaymentResponse processPayment(
            @RequestHeader(value = "X-User-Id", required = false) String headerUserId,
            Authentication authentication,
            @PathVariable("orderId") String orderId,
            @Valid @RequestBody(required = false) ProcessPaymentRequest request) {
        String effectiveUserId = resolveUserId(headerUserId, authentication);
        var paymentMethod = (request != null && request.paymentMethod() != null)
                ? request.paymentMethod()
                : com.aieta.ticketing_app_orders.domain.enums.PaymentMethod.CREDIT_CARD;

        String buyerEmail = request != null ? request.buyerEmail() : null;
        String buyerName = request != null ? request.buyerName() : null;
        String buyerTaxId = request != null ? request.buyerTaxId() : null;
        Integer installments = (request != null && request.installments() != null) ? request.installments() : 1;
        String issuingBank = (request != null && request.issuingBank() != null && !request.issuingBank().isBlank()) ? request.issuingBank() : "OTHER";

        List<InvoiceData.InvoiceItem> invoiceItems = (request != null && request.items() != null)
                ? request.items().stream()
                .map(i -> new InvoiceData.InvoiceItem(
                        i.description(),
                        i.sectorAndSeat(),
                        i.quantity() > 0 ? i.quantity() : 1,
                        i.unitPrice(),
                        i.totalPrice() != null ? i.totalPrice() : i.unitPrice()
                ))
                .toList()
                : null;

        PaymentResult result = mediator.dispatch(new ProcessPaymentCommand(
                orderId,
                effectiveUserId,
                paymentMethod,
                buyerEmail,
                buyerName,
                buyerTaxId,
                installments,
                issuingBank,
                request != null ? request.eventTitle() : null,
                request != null ? request.eventDate() : null,
                request != null ? request.venueName() : null,
                request != null ? request.venueAddress() : null,
                request != null ? request.currency() : null,
                request != null ? request.subtotalAmount() : null,
                request != null ? request.serviceFeeAmount() : null,
                request != null ? request.vatTaxAmount() : null,
                invoiceItems
        ));

        return new ProcessPaymentResponse(
                result.orderId(),
                result.status(),
                result.paymentMethod(),
                result.totalAmount(),
                result.paidAt()
        );
    }
}
