package com.aieta.ticketing_app_orders.infrastructure.web;

import com.aieta.ticketing_app_orders.application.commands.process_payment.PaymentResult;
import com.aieta.ticketing_app_orders.application.commands.process_payment.ProcessPaymentCommand;
import com.aieta.ticketing_app_orders.application.common.mediator.Mediator;
import com.aieta.ticketing_app_orders.infrastructure.web.dto.ProcessPaymentRequest;
import com.aieta.ticketing_app_orders.infrastructure.web.dto.ProcessPaymentResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/orders/{orderId}/pay")
public class PaymentController {

    private final Mediator mediator;

    public PaymentController(Mediator mediator) {
        this.mediator = mediator;
    }

    @PostMapping
    public ProcessPaymentResponse processPayment(
            @RequestHeader(value = "X-User-Id", required = false) String headerUserId,
            @PathVariable("orderId") String orderId,
            @Valid @RequestBody(required = false) ProcessPaymentRequest request) {
        String effectiveUserId = (headerUserId != null && !headerUserId.isBlank()) ? headerUserId : "anonymous-user";
        var paymentMethod = (request != null && request.paymentMethod() != null)
                ? request.paymentMethod()
                : com.aieta.ticketing_app_orders.domain.enums.PaymentMethod.POCKET_WALLET;

        String buyerEmail = request != null ? request.buyerEmail() : null;
        String buyerName = request != null ? request.buyerName() : null;
        String buyerTaxId = request != null ? request.buyerTaxId() : null;

        PaymentResult result = mediator.dispatch(new ProcessPaymentCommand(
                orderId,
                effectiveUserId,
                paymentMethod,
                buyerEmail,
                buyerName,
                buyerTaxId
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
