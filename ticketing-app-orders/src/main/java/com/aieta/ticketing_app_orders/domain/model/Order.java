package com.aieta.ticketing_app_orders.domain.model;

import com.aieta.ticketing_app_orders.domain.enums.OrderStatus;
import com.aieta.ticketing_app_orders.domain.enums.PaymentMethod;
import com.aieta.ticketing_app_orders.domain.exceptions.DomainException;
import com.aieta.ticketing_app_orders.domain.exceptions.InvalidOrderStateTransitionException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class Order {
    private final OrderId id;
    private final String userId;
    private final ReservationId reservationId;
    private final String eventId;
    private final String seatId;
    private Money total;
    private OrderStatus status;
    private PaymentMethod paymentMethod;
    private final Instant createdAt;
    private Instant updatedAt;
    private final List<OrderItem> items;

    public Order(OrderId id, String userId, ReservationId reservationId, String eventId, String seatId,
                 Money total, OrderStatus status, PaymentMethod paymentMethod, Instant createdAt,
                 Instant updatedAt, List<OrderItem> items) {
        this.id = Objects.requireNonNull(id, "OrderId cannot be null");
        this.userId = Objects.requireNonNull(userId, "userId cannot be null");
        this.reservationId = Objects.requireNonNull(reservationId, "reservationId cannot be null");
        this.eventId = Objects.requireNonNull(eventId, "eventId cannot be null");
        this.seatId = Objects.requireNonNull(seatId, "seatId cannot be null");
        this.total = Objects.requireNonNull(total, "total cannot be null");
        this.status = Objects.requireNonNull(status, "status cannot be null");
        this.paymentMethod = paymentMethod;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
        this.updatedAt = updatedAt != null ? updatedAt : this.createdAt;
        this.items = items != null ? new ArrayList<>(items) : new ArrayList<>();
    }

    public static Order create(String userId, ReservationId reservationId, String eventId, String seatId, Money total) {
        if (total.amount().compareTo(BigDecimal.ZERO) < 0) {
            throw new DomainException("Order total cannot be negative");
        }
        OrderId orderId = OrderId.generate();
        Instant now = Instant.now();
        List<OrderItem> defaultItems = new ArrayList<>();
        defaultItems.add(new OrderItem(UUID.randomUUID(), eventId, seatId, null, total.amount(), 1, "Ticket for event " + eventId));
        return new Order(orderId, userId, reservationId, eventId, seatId, total,
                OrderStatus.PENDING_PAYMENT, null, now, now, defaultItems);
    }

    public void markProcessing(PaymentMethod paymentMethod) {
        assertTransitionAllowed(OrderStatus.PROCESSING);
        this.paymentMethod = Objects.requireNonNull(paymentMethod, "paymentMethod cannot be null");
        this.status = OrderStatus.PROCESSING;
        this.updatedAt = Instant.now();
    }

    public void markPaid() {
        assertTransitionAllowed(OrderStatus.PAID);
        this.status = OrderStatus.PAID;
        this.updatedAt = Instant.now();
    }

    public void cancel(String reason) {
        assertTransitionAllowed(OrderStatus.CANCELLED);
        this.status = OrderStatus.CANCELLED;
        this.updatedAt = Instant.now();
    }

    public void refund() {
        assertTransitionAllowed(OrderStatus.REFUNDED);
        this.status = OrderStatus.REFUNDED;
        this.updatedAt = Instant.now();
    }

    private void assertTransitionAllowed(OrderStatus target) {
        boolean allowed = switch (target) {
            case PROCESSING -> status == OrderStatus.PENDING_PAYMENT;
            case PAID -> status == OrderStatus.PROCESSING || status == OrderStatus.PENDING_PAYMENT;
            case CANCELLED -> status == OrderStatus.PENDING_PAYMENT || status == OrderStatus.PROCESSING;
            case REFUNDED -> status == OrderStatus.PAID;
            default -> false;
        };
        if (!allowed) {
            throw new InvalidOrderStateTransitionException(status, target);
        }
    }

    public OrderId getId() {
        return id;
    }

    public String getUserId() {
        return userId;
    }

    public ReservationId getReservationId() {
        return reservationId;
    }

    public String getEventId() {
        return eventId;
    }

    public String getSeatId() {
        return seatId;
    }

    public Money getTotal() {
        return total;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public List<OrderItem> getItems() {
        return Collections.unmodifiableList(items);
    }
}
