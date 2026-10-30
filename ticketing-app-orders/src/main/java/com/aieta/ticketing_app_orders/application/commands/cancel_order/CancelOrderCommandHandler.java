package com.aieta.ticketing_app_orders.application.commands.cancel_order;

import com.aieta.ticketing_app_orders.application.common.mediator.RequestHandler;
import com.aieta.ticketing_app_orders.domain.enums.OrderStatus;
import com.aieta.ticketing_app_orders.domain.events.PaymentFailedEvent;
import com.aieta.ticketing_app_orders.domain.exceptions.DomainException;
import com.aieta.ticketing_app_orders.domain.exceptions.OrderNotFoundException;
import com.aieta.ticketing_app_orders.domain.model.Order;
import com.aieta.ticketing_app_orders.domain.model.OrderId;
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
import java.util.UUID;

@Component
public class CancelOrderCommandHandler implements RequestHandler<CancelOrderCommand, Void> {
    private static final Logger log = LoggerFactory.getLogger(CancelOrderCommandHandler.class);

    private final OrderRepositoryPort orderRepository;
    private final SpringDataOutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    public CancelOrderCommandHandler(OrderRepositoryPort orderRepository,
                                     SpringDataOutboxRepository outboxRepository,
                                     ObjectMapper objectMapper) {
        this.orderRepository = orderRepository;
        this.outboxRepository = outboxRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public Void handle(CancelOrderCommand request) {
        Order order = orderRepository.findById(OrderId.of(request.orderId()))
                .orElseThrow(() -> new OrderNotFoundException(request.orderId()));

        if (order.getStatus() == OrderStatus.CANCELLED) {
            log.info("Order {} is already CANCELLED", order.getId());
            return null;
        }

        order.cancel(request.reason());
        orderRepository.save(order);

        // Publish PaymentFailedEvent to Outbox so Inventory can compensate (delete Redis Redlock)
        PaymentFailedEvent event = new PaymentFailedEvent(
                order.getId().toString(),
                order.getUserId(),
                order.getReservationId().value(),
                order.getEventId(),
                order.getSeatId(),
                request.reason(),
                Instant.now()
        );

        try {
            String payload = objectMapper.writeValueAsString(event);
            OutboxEventJpaEntity outbox = new OutboxEventJpaEntity(
                    UUID.randomUUID(),
                    "Order",
                    order.getId().toString(),
                    "PaymentFailedEvent",
                    payload
            );
            outboxRepository.save(outbox);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize PaymentFailedEvent for order {}", order.getId(), e);
            throw new DomainException("Failed to serialize payment failure outbox event", e);
        }

        log.info("Order {} cancelled. Compensating PaymentFailedEvent queued to Outbox.", order.getId());
        return null;
    }

    @Override
    public Class<CancelOrderCommand> getRequestType() {
        return CancelOrderCommand.class;
    }
}
