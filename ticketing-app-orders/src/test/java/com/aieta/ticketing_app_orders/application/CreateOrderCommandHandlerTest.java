package com.aieta.ticketing_app_orders.application;

import com.aieta.ticketing_app_orders.application.commands.create_order.CreateOrderCommand;
import com.aieta.ticketing_app_orders.application.commands.create_order.CreateOrderCommandHandler;
import com.aieta.ticketing_app_orders.application.commands.create_order.CreateOrderResult;
import com.aieta.ticketing_app_orders.domain.model.Money;
import com.aieta.ticketing_app_orders.domain.model.Order;
import com.aieta.ticketing_app_orders.domain.model.ReservationId;
import com.aieta.ticketing_app_orders.domain.ports.OrderRepositoryPort;
import com.aieta.ticketing_app_orders.infrastructure.persistence.repository.SpringDataOutboxRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreateOrderCommandHandlerTest {

    @Mock
    private OrderRepositoryPort orderRepository;

    @Mock
    private SpringDataOutboxRepository outboxRepository;

    private ObjectMapper objectMapper;
    private CreateOrderCommandHandler handler;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
        handler = new CreateOrderCommandHandler(orderRepository, outboxRepository, objectMapper);
    }

    @Test
    @DisplayName("Should successfully create order and save outbox event")
    void shouldCreateOrderAndOutboxEvent() {
        CreateOrderCommand cmd = new CreateOrderCommand("user-1", "res-1", "evt-1", "seat-1", new BigDecimal("50.00"));

        when(orderRepository.findByReservationId("res-1")).thenReturn(Optional.empty());
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CreateOrderResult result = handler.handle(cmd);

        assertNotNull(result.orderId());
        assertEquals("user-1", result.userId());
        assertEquals("res-1", result.reservationId());
        assertEquals("PENDING_PAYMENT", result.status());
        assertEquals(new BigDecimal("50.00"), result.totalAmount());

        verify(orderRepository).save(any(Order.class));
        verify(outboxRepository).save(any());
    }

    @Test
    @DisplayName("Should return existing order on duplicate reservationId (idempotent)")
    void shouldReturnExistingOnDuplicate() {
        CreateOrderCommand cmd = new CreateOrderCommand("user-1", "res-1", "evt-1", "seat-1", new BigDecimal("50.00"));
        Order existing = Order.create("user-1", new ReservationId("res-1"), "evt-1", "seat-1", Money.of(new BigDecimal("50.00")));

        when(orderRepository.findByReservationId("res-1")).thenReturn(Optional.of(existing));

        CreateOrderResult result = handler.handle(cmd);

        assertEquals(existing.getId().toString(), result.orderId());
        verify(orderRepository, never()).save(any());
        verify(outboxRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should successfully create order with custom subtotal and service fee")
    void shouldCreateOrderWithCustomSubtotalAndFee() {
        CreateOrderCommand cmd = new CreateOrderCommand("user-1", "res-2", "evt-1", "seat-1", new BigDecimal("110.00"), new BigDecimal("100.00"), new BigDecimal("10.00"));
        when(orderRepository.findByReservationId("res-2")).thenReturn(Optional.empty());
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CreateOrderResult result = handler.handle(cmd);

        assertNotNull(result.orderId());
        assertEquals("user-1", result.userId());
        assertEquals("res-2", result.reservationId());
        assertEquals(new BigDecimal("110.00"), result.totalAmount());
        verify(orderRepository).save(any(Order.class));
    }
}
