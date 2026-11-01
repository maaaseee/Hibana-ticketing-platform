package com.aieta.ticketing_app_orders.application.commands.resend_confirmation;

import com.aieta.ticketing_app_orders.application.common.InvoiceDataMapper;
import com.aieta.ticketing_app_orders.application.common.mediator.RequestHandler;
import com.aieta.ticketing_app_orders.application.dto.InvoiceData;
import com.aieta.ticketing_app_orders.application.dto.OrderConfirmationEmail;
import com.aieta.ticketing_app_orders.application.ports.EmailNotificationPort;
import com.aieta.ticketing_app_orders.application.ports.InvoicePdfPort;
import com.aieta.ticketing_app_orders.domain.exceptions.DomainException;
import com.aieta.ticketing_app_orders.domain.exceptions.OrderNotFoundException;
import com.aieta.ticketing_app_orders.domain.model.Order;
import com.aieta.ticketing_app_orders.domain.model.OrderId;
import com.aieta.ticketing_app_orders.domain.ports.OrderRepositoryPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
public class ResendOrderConfirmationCommandHandler implements RequestHandler<ResendOrderConfirmationCommand, Void> {

    private static final Logger log = LoggerFactory.getLogger(ResendOrderConfirmationCommandHandler.class);

    private final OrderRepositoryPort orderRepository;
    private final InvoicePdfPort invoicePdfPort;
    private final EmailNotificationPort emailNotificationPort;

    public ResendOrderConfirmationCommandHandler(OrderRepositoryPort orderRepository,
                                                InvoicePdfPort invoicePdfPort,
                                                EmailNotificationPort emailNotificationPort) {
        this.orderRepository = orderRepository;
        this.invoicePdfPort = invoicePdfPort;
        this.emailNotificationPort = emailNotificationPort;
    }

    @Override
    @Transactional(readOnly = true)
    public Void handle(ResendOrderConfirmationCommand request) {
        Order order = orderRepository.findById(OrderId.of(request.orderId()))
                .orElseThrow(() -> new OrderNotFoundException(request.orderId()));

        if (!order.getUserId().equals(request.userId()) && !"anonymous-user".equals(request.userId()) && !"admin".equals(request.userId())) {
            throw new DomainException("Unauthorized access to order " + request.orderId());
        }

        InvoiceData invoiceData = InvoiceDataMapper.fromOrder(order, null, request.customEmail(), null);
        byte[] pdfBytes = invoicePdfPort.generateInvoicePdf(invoiceData);

        List<String> seats = List.of(order.getSeatId());
        OrderConfirmationEmail email = new OrderConfirmationEmail(
                order.getId().toString(),
                invoiceData.buyerEmail(),
                invoiceData.buyerName(),
                invoiceData.eventTitle(),
                invoiceData.eventDate(),
                invoiceData.venueName(),
                seats,
                order.getTotal().amount(),
                pdfBytes
        );

        emailNotificationPort.sendOrderConfirmation(email);
        log.info("[CHECKOUT-RESEND] Resent confirmation email and invoice for order {} to {}", order.getId(), invoiceData.buyerEmail());
        return null;
    }

    @Override
    public Class<ResendOrderConfirmationCommand> getRequestType() {
        return ResendOrderConfirmationCommand.class;
    }
}
