package com.aieta.ticketing_app_orders.infrastructure.persistence;

import com.aieta.ticketing_app_orders.domain.enums.OrderStatus;
import com.aieta.ticketing_app_orders.domain.model.Money;
import com.aieta.ticketing_app_orders.domain.model.Order;
import com.aieta.ticketing_app_orders.domain.model.ReservationId;
import com.aieta.ticketing_app_orders.domain.ports.OrderRepositoryPort;
import com.aieta.ticketing_app_orders.infrastructure.persistence.entity.OutboxEventJpaEntity;
import com.aieta.ticketing_app_orders.infrastructure.persistence.repository.SpringDataOutboxRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class OutboxAtomicityIntegrationTest {

    @Autowired
    private OrderRepositoryPort orderRepository;

    @Autowired
    private SpringDataOutboxRepository outboxRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Test
    @DisplayName("Should commit Order and OutboxEvent atomically within same ACID transaction")
    void shouldCommitOrderAndOutboxAtomically() {
        TransactionTemplate template = new TransactionTemplate(transactionManager);

        Order order = Order.create("user-atom-1", new ReservationId("res-atom-1"), "evt-1", "seat-1", Money.of(new BigDecimal("75.00")));
        UUID outboxId = UUID.randomUUID();

        template.executeWithoutResult(status -> {
            orderRepository.save(order);
            OutboxEventJpaEntity outbox = new OutboxEventJpaEntity(outboxId, "Order", order.getId().toString(), "OrderCreatedEvent", "{}");
            outboxRepository.save(outbox);
        });

        assertTrue(orderRepository.findById(order.getId()).isPresent());
        assertTrue(outboxRepository.findById(outboxId).isPresent());
    }

    @Test
    @DisplayName("Should rollback both Order and OutboxEvent when transaction rolls back")
    void shouldRollbackBothOnFailure() {
        TransactionTemplate template = new TransactionTemplate(transactionManager);

        Order order = Order.create("user-atom-2", new ReservationId("res-atom-2"), "evt-1", "seat-2", Money.of(new BigDecimal("75.00")));
        UUID outboxId = UUID.randomUUID();

        assertThrows(RuntimeException.class, () -> {
            template.executeWithoutResult(status -> {
                orderRepository.save(order);
                OutboxEventJpaEntity outbox = new OutboxEventJpaEntity(outboxId, "Order", order.getId().toString(), "OrderCreatedEvent", "{}");
                outboxRepository.save(outbox);
                throw new RuntimeException("Simulated transaction failure");
            });
        });

        assertFalse(orderRepository.findById(order.getId()).isPresent());
        assertFalse(outboxRepository.findById(outboxId).isPresent());
    }
}
