package com.aieta.ticketing_app_orders.application.commands.create_order;

import com.aieta.ticketing_app_orders.application.common.mediator.RequestHandler;
import com.aieta.ticketing_app_orders.domain.events.OrderCreatedEvent;
import com.aieta.ticketing_app_orders.domain.exceptions.DomainException;
import com.aieta.ticketing_app_orders.domain.model.Money;
import com.aieta.ticketing_app_orders.domain.model.Order;
import com.aieta.ticketing_app_orders.domain.model.ReservationId;
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
import java.util.Optional;
import java.util.UUID;

@Component
public class CreateOrderCommandHandler implements RequestHandler<CreateOrderCommand, CreateOrderResult> {
    private static final Logger log = LoggerFactory.getLogger(CreateOrderCommandHandler.class);

    private final OrderRepositoryPort orderRepository;
    private final SpringDataOutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    public CreateOrderCommandHandler(OrderRepositoryPort orderRepository,
                                     SpringDataOutboxRepository outboxRepository,
                                     ObjectMapper objectMapper) {
        this.orderRepository = orderRepository;
        this.outboxRepository = outboxRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public CreateOrderResult handle(CreateOrderCommand request) {
        // Idempotency check: if order already created for this reservationId, return it
        Optional<Order> existingOrder = orderRepository.findByReservationId(request.reservationId());
        if (existingOrder.isPresent()) {
            Order existing = existingOrder.get();
            log.info("Idempotent hit: Order {} already exists for reservation {}", existing.getId(), request.reservationId());
            return new CreateOrderResult(
                    existing.getId().toString(),
                    existing.getUserId(),
                    existing.getReservationId().value(),
                    existing.getEventId(),
                    existing.getSeatId(),
                    existing.getTotal().amount(),
                    existing.getStatus().name(),
                    existing.getCreatedAt()
            );
        }

        Money total = Money.of(request.totalAmount());
        Order order = Order.create(
                request.userId(),
                new ReservationId(request.reservationId()),
                request.eventId(),
                request.seatId(),
                total
        );

        Order savedOrder = orderRepository.save(order);

        // Transactional Outbox write within same ACID boundary
        OrderCreatedEvent event = new OrderCreatedEvent(
                savedOrder.getId().toString(),
                savedOrder.getUserId(),
                savedOrder.getReservationId().value(),
                savedOrder.getEventId(),
                savedOrder.getSeatId(),
                savedOrder.getTotal().amount(),
                Instant.now()
        );

        try {
            String payload = objectMapper.writeValueAsString(event);
            OutboxEventJpaEntity outboxEntity = new OutboxEventJpaEntity(
                    UUID.randomUUID(),
                    "Order",
                    savedOrder.getId().toString(),
                    "OrderCreatedEvent",
                    payload
            );
            outboxRepository.save(outboxEntity);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize OrderCreatedEvent for order {}", savedOrder.getId(), e);
            throw new DomainException("Failed to serialize order outbox event", e);
        }

        log.info("Order successfully created: id={}, reservationId={}, total={}",
                savedOrder.getId(), savedOrder.getReservationId(), savedOrder.getTotal().amount());

        return new CreateOrderResult(
                savedOrder.getId().toString(),
                savedOrder.getUserId(),
                savedOrder.getReservationId().value(),
                savedOrder.getEventId(),
                savedOrder.getSeatId(),
                savedOrder.getTotal().amount(),
                savedOrder.getStatus().name(),
                savedOrder.getCreatedAt()
        );
    }

    @Override
    public Class<CreateOrderCommand> getRequestType() {
        return CreateOrderCommand.class;
    }
}
