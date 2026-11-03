package com.aieta.ticketing_app_orders.infrastructure.web;

import com.aieta.ticketing_app_orders.application.commands.process_payment.strategy.PaymentStrategyRegistry;
import com.aieta.ticketing_app_orders.application.commands.cancel_order.CancelOrderCommand;
import com.aieta.ticketing_app_orders.application.commands.create_order.CreateOrderCommand;
import com.aieta.ticketing_app_orders.application.commands.create_order.CreateOrderResult;
import com.aieta.ticketing_app_orders.application.commands.resend_confirmation.ResendOrderConfirmationCommand;
import com.aieta.ticketing_app_orders.application.common.InvoiceDataMapper;
import com.aieta.ticketing_app_orders.application.common.mediator.Mediator;
import com.aieta.ticketing_app_orders.application.dto.InvoiceData;
import com.aieta.ticketing_app_orders.application.dto.OrderConfirmationEmail;
import com.aieta.ticketing_app_orders.application.ports.InvoicePdfPort;
import com.aieta.ticketing_app_orders.application.queries.get_invoice.GetOrderInvoicePdfQuery;
import com.aieta.ticketing_app_orders.application.queries.get_order.GetOrderByIdQuery;
import com.aieta.ticketing_app_orders.application.queries.get_order.OrderDetailDto;
import com.aieta.ticketing_app_orders.application.queries.get_user_orders.GetUserOrdersQuery;
import com.aieta.ticketing_app_orders.infrastructure.mail.SpringMailNotificationAdapter;
import com.aieta.ticketing_app_orders.infrastructure.web.dto.CreateOrderRequest;
import com.aieta.ticketing_app_orders.infrastructure.web.dto.CreateOrderResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.Authentication;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    private final Mediator mediator;
    private final InvoicePdfPort invoicePdfPort;
    private final SpringMailNotificationAdapter mailAdapter;
    private final PaymentStrategyRegistry paymentStrategyRegistry;


    private String resolveUserId(String headerUserId, Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getName())) {
            return authentication.getName();
        }
        return "anonymous-user";
    }

    public OrderController(Mediator mediator,
                           InvoicePdfPort invoicePdfPort,
                           SpringMailNotificationAdapter mailAdapter,
                           PaymentStrategyRegistry paymentStrategyRegistry) {
        this.mediator = mediator;
        this.invoicePdfPort = invoicePdfPort;
        this.mailAdapter = mailAdapter;
        this.paymentStrategyRegistry = paymentStrategyRegistry;
    }

    @GetMapping("/payment-methods")
    public List<PaymentStrategyRegistry.PaymentMethodStatusDto> getPaymentMethods() {
        return paymentStrategyRegistry.getAvailableMethods();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CreateOrderResponse createOrder(
            @RequestHeader(value = "X-User-Id", required = false) String headerUserId,
            Authentication authentication,
            @Valid @RequestBody CreateOrderRequest request) {
        String effectiveUserId = resolveUserId(headerUserId, authentication);

        CreateOrderResult result = mediator.dispatch(new CreateOrderCommand(
                effectiveUserId,
                request.reservationId(),
                request.eventId(),
                request.seatId(),
                request.totalAmount(),
                request.subtotalAmount(),
                request.serviceFeeAmount()
        ));

        return new CreateOrderResponse(
                result.orderId(),
                result.userId(),
                result.reservationId(),
                result.eventId(),
                result.seatId(),
                result.totalAmount(),
                result.status(),
                result.createdAt()
        );
    }

    @GetMapping("/{orderId}")
    public OrderDetailDto getOrder(
            @RequestHeader(value = "X-User-Id", required = false) String headerUserId,
            Authentication authentication,
            @PathVariable("orderId") String orderId) {
        String effectiveUserId = resolveUserId(headerUserId, authentication);
        return mediator.dispatch(new GetOrderByIdQuery(orderId, effectiveUserId));
    }

    @GetMapping("/me")
    public List<OrderDetailDto> getUserOrders(
            @RequestHeader(value = "X-User-Id", required = false) String headerUserId,
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        String effectiveUserId = resolveUserId(headerUserId, authentication);
        return mediator.dispatch(new GetUserOrdersQuery(effectiveUserId, page, size));
    }

    @PostMapping("/{orderId}/cancel")
    public Map<String, Object> cancelOrder(
            @RequestHeader(value = "X-User-Id", required = false) String headerUserId,
            Authentication authentication,
            @PathVariable("orderId") String orderId,
            @RequestBody(required = false) Map<String, String> body) {
        String effectiveUserId = resolveUserId(headerUserId, authentication);
        String reason = (body != null && body.containsKey("reason")) ? body.get("reason") : "User requested cancellation";

        mediator.dispatch(new CancelOrderCommand(orderId, effectiveUserId, reason));
        return Map.of("orderId", orderId, "status", "CANCELLED", "message", "Order cancelled and compensation initiated");
    }

    @GetMapping(value = "/{orderId}/invoice", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> getOrderInvoice(
            @RequestHeader(value = "X-User-Id", required = false) String headerUserId,
            Authentication authentication,
            @PathVariable("orderId") String orderId) {
        String effectiveUserId = resolveUserId(headerUserId, authentication);
        byte[] pdf = mediator.dispatch(new GetOrderInvoicePdfQuery(orderId, effectiveUserId));

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"Factura_Hibana_" + orderId + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @GetMapping(value = "/preview/invoice.pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> previewInvoicePdf() {
        InvoiceData sampleData = InvoiceDataMapper.createSampleInvoice();
        byte[] pdf = invoicePdfPort.generateInvoicePdf(sampleData);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"Factura_Hibana_Preview.pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @GetMapping(value = "/preview/email.html", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> previewEmailHtml() {
        InvoiceData sampleData = InvoiceDataMapper.createSampleInvoice();
        OrderConfirmationEmail sampleEmail = new OrderConfirmationEmail(
                sampleData.orderId(),
                sampleData.buyerEmail(),
                sampleData.buyerName(),
                sampleData.eventTitle(),
                sampleData.eventDate(),
                sampleData.venueName(),
                List.of("Sector Platea VIP - Fila 4 - Butaca 12"),
                sampleData.grandTotal(),
                null
        );

        String html = mailAdapter.buildHtmlEmailTemplate(sampleEmail);
        return ResponseEntity.ok()
                .contentType(MediaType.valueOf("text/html;charset=UTF-8"))
                .body(html);
    }

    @PostMapping("/{orderId}/resend-confirmation")
    public Map<String, Object> resendConfirmation(
            @RequestHeader(value = "X-User-Id", required = false) String headerUserId,
            Authentication authentication,
            @PathVariable("orderId") String orderId,
            @RequestBody(required = false) Map<String, String> body) {
        String effectiveUserId = resolveUserId(headerUserId, authentication);
        String customEmail = (body != null) ? body.get("email") : null;

        mediator.dispatch(new ResendOrderConfirmationCommand(orderId, effectiveUserId, customEmail));

        return Map.of(
                "orderId", orderId,
                "status", "DISPATCHED",
                "message", "Confirmation email and PDF invoice successfully scheduled for delivery"
        );
    }
}
