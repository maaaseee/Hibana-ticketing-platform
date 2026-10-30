package com.aieta.ticketing_app_orders.domain;

import com.aieta.ticketing_app_orders.domain.enums.OrderStatus;
import com.aieta.ticketing_app_orders.domain.enums.PaymentMethod;
import com.aieta.ticketing_app_orders.domain.exceptions.InvalidOrderStateTransitionException;
import com.aieta.ticketing_app_orders.domain.model.Money;
import com.aieta.ticketing_app_orders.domain.model.Order;
import com.aieta.ticketing_app_orders.domain.model.ReservationId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class OrderInvariantsTest {

    @Test
    @DisplayName("Should successfully create order in PENDING_PAYMENT status")
    void shouldCreateOrderPending() {
        Order order = Order.create("usr-1", new ReservationId("res-1"), "evt-1", "seat-1", Money.of(new BigDecimal("59.00")));
        assertEquals(OrderStatus.PENDING_PAYMENT, order.getStatus());
        assertEquals("usr-1", order.getUserId());
        assertEquals(new BigDecimal("59.00"), order.getTotal().amount());
        assertFalse(order.getItems().isEmpty());
    }

    @Test
    @DisplayName("Should transition from PENDING_PAYMENT -> PROCESSING -> PAID")
    void shouldFollowHappyPathTransitions() {
        Order order = Order.create("usr-1", new ReservationId("res-1"), "evt-1", "seat-1", Money.of(new BigDecimal("59.00")));

        order.markProcessing(PaymentMethod.POCKET_WALLET);
        assertEquals(OrderStatus.PROCESSING, order.getStatus());
        assertEquals(PaymentMethod.POCKET_WALLET, order.getPaymentMethod());

        order.markPaid();
        assertEquals(OrderStatus.PAID, order.getStatus());
    }

    @Test
    @DisplayName("Should fail when attempting invalid transition from PAID to CANCELLED")
    void shouldPreventCancellingPaidOrder() {
        Order order = Order.create("usr-1", new ReservationId("res-1"), "evt-1", "seat-1", Money.of(new BigDecimal("59.00")));
        order.markPaid();

        assertThrows(InvalidOrderStateTransitionException.class, () -> order.cancel("Customer changed mind"));
    }

    @Test
    @DisplayName("Should allow refund on PAID order")
    void shouldAllowRefundOnPaidOrder() {
        Order order = Order.create("usr-1", new ReservationId("res-1"), "evt-1", "seat-1", Money.of(new BigDecimal("59.00")));
        order.markPaid();
        order.refund();
        assertEquals(OrderStatus.REFUNDED, order.getStatus());
    }
}
