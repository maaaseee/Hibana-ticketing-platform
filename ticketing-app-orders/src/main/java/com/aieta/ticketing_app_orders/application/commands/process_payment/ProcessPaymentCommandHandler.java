package com.aieta.ticketing_app_orders.application.commands.process_payment;

import com.aieta.ticketing_app_orders.application.commands.process_payment.strategy.PaymentContext;
import com.aieta.ticketing_app_orders.application.commands.process_payment.strategy.PaymentExecutionResult;
import com.aieta.ticketing_app_orders.application.commands.process_payment.strategy.PaymentStrategy;
import com.aieta.ticketing_app_orders.application.commands.process_payment.strategy.PaymentStrategyRegistry;
import com.aieta.ticketing_app_orders.application.common.InvoiceDataMapper;
import com.aieta.ticketing_app_orders.application.common.mediator.RequestHandler;
import com.aieta.ticketing_app_orders.application.dto.InvoiceData;
import com.aieta.ticketing_app_orders.application.dto.OrderConfirmationEmail;
import com.aieta.ticketing_app_orders.application.ports.EmailNotificationPort;
import com.aieta.ticketing_app_orders.application.ports.InventoryReservationPort;
import com.aieta.ticketing_app_orders.application.ports.InvoicePdfPort;
import com.aieta.ticketing_app_orders.domain.enums.OrderStatus;
import com.aieta.ticketing_app_orders.domain.enums.PaymentMethod;
import com.aieta.ticketing_app_orders.domain.events.OrderPaidEvent;
import com.aieta.ticketing_app_orders.domain.exceptions.DomainException;
import com.aieta.ticketing_app_orders.domain.exceptions.OrderNotFoundException;
import com.aieta.ticketing_app_orders.domain.model.Order;
import com.aieta.ticketing_app_orders.domain.model.OrderId;
import com.aieta.ticketing_app_orders.domain.model.pocket.DoubleEntryTransaction;
import com.aieta.ticketing_app_orders.domain.model.pocket.LedgerAccount;
import com.aieta.ticketing_app_orders.domain.model.pocket.LedgerEntry;
import com.aieta.ticketing_app_orders.domain.ports.LedgerRepositoryPort;
import com.aieta.ticketing_app_orders.domain.ports.OrderRepositoryPort;
import com.aieta.ticketing_app_orders.infrastructure.persistence.entity.OutboxEventJpaEntity;
import com.aieta.ticketing_app_orders.infrastructure.persistence.repository.SpringDataOutboxRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Currency;
import java.util.List;
import java.util.UUID;

@Component
public class ProcessPaymentCommandHandler implements RequestHandler<ProcessPaymentCommand, PaymentResult> {
    private static final Logger log = LoggerFactory.getLogger(ProcessPaymentCommandHandler.class);

    private final OrderRepositoryPort orderRepository;
    private final PaymentStrategyRegistry paymentStrategyRegistry;
    private final LedgerRepositoryPort ledgerRepository;
    private final SpringDataOutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;
    private final InvoicePdfPort invoicePdfPort;
    private final EmailNotificationPort emailNotificationPort;
    private final InventoryReservationPort inventoryReservationPort;

    public ProcessPaymentCommandHandler(OrderRepositoryPort orderRepository,
                                        PaymentStrategyRegistry paymentStrategyRegistry,
                                        LedgerRepositoryPort ledgerRepository,
                                        SpringDataOutboxRepository outboxRepository,
                                        ObjectMapper objectMapper,
                                        InvoicePdfPort invoicePdfPort,
                                        EmailNotificationPort emailNotificationPort,
                                        @org.springframework.beans.factory.annotation.Autowired(required = false) InventoryReservationPort inventoryReservationPort) {
        this.orderRepository = orderRepository;
        this.paymentStrategyRegistry = paymentStrategyRegistry;
        this.ledgerRepository = ledgerRepository;
        this.outboxRepository = outboxRepository;
        this.objectMapper = objectMapper;
        this.invoicePdfPort = invoicePdfPort;
        this.emailNotificationPort = emailNotificationPort;
        this.inventoryReservationPort = inventoryReservationPort;
    }

    @Override
    @Transactional
    public PaymentResult handle(ProcessPaymentCommand request) {
        Order order = orderRepository.findById(OrderId.of(request.orderId()))
                .orElseThrow(() -> new OrderNotFoundException(request.orderId()));

        if (!order.getUserId().equals(request.userId())) {
            throw new DomainException("Unauthorized access to order " + request.orderId());
        }

        // Idempotency: if already paid, return existing result
        if (order.getStatus() == OrderStatus.PAID) {
            log.info("Order {} is already PAID", order.getId());
            return new PaymentResult(
                    order.getId().toString(),
                    order.getStatus().name(),
                    order.getPaymentMethod() != null ? order.getPaymentMethod().name() : PaymentMethod.CREDIT_CARD.name(),
                    order.getTotal().amount(),
                    order.getUpdatedAt()
            );
        }

        PaymentMethod method = request.paymentMethod() != null ? request.paymentMethod() : PaymentMethod.CREDIT_CARD;

        // 1. Strategy Pattern: execute payment according to the selected gateway
        PaymentStrategy strategy = paymentStrategyRegistry.getStrategy(method);
        PaymentContext context = new PaymentContext(
                order,
                request.userId(),
                request.buyerEmail(),
                request.buyerName(),
                request.buyerTaxId(),
                request.installments(),
                request.issuingBank()
        );
        PaymentExecutionResult executionResult = strategy.execute(context);
        if (!executionResult.success()) {
            throw new DomainException("Payment processing failed: " + executionResult.message());
        }

        // 2. Double-entry bookkeeping ledger (com.aieta.pocket) for platform settlement
        UUID txRef = UUID.randomUUID();
        LedgerAccount revenueAccount = ledgerRepository.findAccountByOwnerIdAndType("PLATFORM", LedgerAccount.AccountType.REVENUE)
                .orElseGet(() -> ledgerRepository.saveAccount(new LedgerAccount(
                        UUID.randomUUID(), "PLATFORM", LedgerAccount.AccountType.REVENUE, order.getTotal().amount(), Currency.getInstance("EUR"))));

        LedgerAccount assetAccount = ledgerRepository.findAccountByOwnerIdAndType("PLATFORM", LedgerAccount.AccountType.ASSET)
                .orElseGet(() -> ledgerRepository.saveAccount(new LedgerAccount(
                        UUID.randomUUID(), "PLATFORM", LedgerAccount.AccountType.ASSET, order.getTotal().amount(), Currency.getInstance("EUR"))));

        DoubleEntryTransaction doubleEntryTx = new DoubleEntryTransaction(txRef)
                .addDebit(assetAccount.id(), order.getTotal().amount(), "Settlement via " + method.name() + " for order " + order.getId())
                .addCredit(revenueAccount.id(), order.getTotal().amount(), "Ticket revenue for order " + order.getId());

        List<LedgerEntry> entries = doubleEntryTx.buildAndValidate();
        ledgerRepository.recordEntries(entries);

        // Transition order status
        order.markProcessing(method);
        order.markPaid();
        Order savedOrder = orderRepository.save(order);

        // Transactional Outbox event write
        OrderPaidEvent event = new OrderPaidEvent(
                savedOrder.getId().toString(),
                savedOrder.getUserId(),
                savedOrder.getReservationId().value(),
                savedOrder.getEventId(),
                savedOrder.getSeatId(),
                savedOrder.getTotal().amount(),
                method.name(),
                Instant.now()
        );

        try {
            String payload = objectMapper.writeValueAsString(event);
            OutboxEventJpaEntity outbox = new OutboxEventJpaEntity(
                    UUID.randomUUID(),
                    "Order",
                    savedOrder.getId().toString(),
                    "OrderPaidEvent",
                    payload
            );
            outboxRepository.save(outbox);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize OrderPaidEvent for order {}", savedOrder.getId(), e);
            throw new DomainException("Failed to serialize payment outbox event", e);
        }

        log.info("Order {} successfully PAID via {}", savedOrder.getId(), method);

        // Saga Step: Confirm seat hold in inventory microservice
        if (inventoryReservationPort != null) {
            inventoryReservationPort.confirmReservation(
                    savedOrder.getReservationId().value(),
                    savedOrder.getId().toString()
            );
        }

        // Post-checkout: Generate Official PDF Invoice & Trigger Transactional Email
        dispatchInvoiceAndEmail(savedOrder, request);

        return new PaymentResult(
                savedOrder.getId().toString(),
                savedOrder.getStatus().name(),
                method.name(),
                savedOrder.getTotal().amount(),
                savedOrder.getUpdatedAt()
        );
    }

    private void dispatchInvoiceAndEmail(Order order, ProcessPaymentCommand request) {
        try {
            InvoiceData invoiceData = InvoiceDataMapper.fromOrder(
                    order,
                    request.buyerName(),
                    request.buyerEmail(),
                    request.buyerTaxId()
            );

            byte[] pdfBytes = null;
            if (invoicePdfPort != null) {
                pdfBytes = invoicePdfPort.generateInvoicePdf(invoiceData);
            }

            if (emailNotificationPort != null) {
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
            }
        } catch (Exception e) {
            log.error("[CHECKOUT-NOTIFICATION] Failed to generate invoice/send confirmation email for order {}: {}",
                    order.getId(), e.getMessage(), e);
            // Non-blocking: order payment remains successfully committed
        }
    }

    @Override
    public Class<ProcessPaymentCommand> getRequestType() {
        return ProcessPaymentCommand.class;
    }
}
