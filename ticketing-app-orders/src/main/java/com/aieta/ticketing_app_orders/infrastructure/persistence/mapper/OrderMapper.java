package com.aieta.ticketing_app_orders.infrastructure.persistence.mapper;

import com.aieta.ticketing_app_orders.domain.enums.OrderStatus;
import com.aieta.ticketing_app_orders.domain.enums.PaymentMethod;
import com.aieta.ticketing_app_orders.domain.model.Money;
import com.aieta.ticketing_app_orders.domain.model.Order;
import com.aieta.ticketing_app_orders.domain.model.OrderId;
import com.aieta.ticketing_app_orders.domain.model.OrderItem;
import com.aieta.ticketing_app_orders.domain.model.ReservationId;
import com.aieta.ticketing_app_orders.infrastructure.persistence.entity.OrderJpaEntity;
import com.aieta.ticketing_app_orders.infrastructure.persistence.entity.OrderItemJpaEntity;

import java.util.Currency;
import java.util.List;

public final class OrderMapper {

    private OrderMapper() {
    }

    public static Order toDomain(OrderJpaEntity entity) {
        if (entity == null) {
            return null;
        }

        List<OrderItem> items = entity.getItems() != null ? entity.getItems().stream()
                .map(item -> toDomainItem(item))
                .toList() : List.of();

        Currency currency = Currency.getInstance(entity.getCurrency());
        Money total = new Money(entity.getTotalAmount(), currency);
        Money subtotal = entity.getSubtotalAmount() != null ? new Money(entity.getSubtotalAmount(), currency) : null;
        Money serviceFee = entity.getServiceFeeAmount() != null ? new Money(entity.getServiceFeeAmount(), currency) : null;
        OrderStatus status = OrderStatus.valueOf(entity.getStatus());
        PaymentMethod paymentMethod = entity.getPaymentMethod() != null ? PaymentMethod.valueOf(entity.getPaymentMethod()) : null;

        return new Order(
                new OrderId(entity.getId()),
                entity.getUserId(),
                new ReservationId(entity.getReservationId()),
                entity.getEventId(),
                entity.getSeatId(),
                subtotal,
                serviceFee,
                total,
                status,
                paymentMethod,
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                items
        );
    }

    public static OrderItem toDomainItem(OrderItemJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        return new OrderItem(
                entity.getId(),
                entity.getEventId(),
                entity.getSeatId(),
                entity.getSectorId(),
                entity.getUnitPrice(),
                entity.getQuantity(),
                entity.getDescription()
        );
    }

    public static OrderJpaEntity toEntity(Order domain) {
        if (domain == null) {
            return null;
        }
        OrderJpaEntity entity = new OrderJpaEntity(
                domain.getId().value(),
                domain.getUserId(),
                domain.getReservationId().value(),
                domain.getEventId(),
                domain.getSeatId(),
                domain.getSubtotal() != null ? domain.getSubtotal().amount() : null,
                domain.getServiceFee() != null ? domain.getServiceFee().amount() : null,
                domain.getTotal().amount(),
                domain.getTotal().currency().getCurrencyCode(),
                domain.getStatus().name(),
                domain.getPaymentMethod() != null ? domain.getPaymentMethod().name() : null,
                domain.getCreatedAt(),
                domain.getUpdatedAt()
        );

        if (domain.getItems() != null) {
            for (OrderItem item : domain.getItems()) {
                entity.addItem(new OrderItemJpaEntity(
                        item.getId(),
                        entity,
                        item.getEventId(),
                        item.getSeatId(),
                        item.getSectorId(),
                        item.getUnitPrice(),
                        item.getQuantity(),
                        item.getDescription()
                ));
            }
        }

        return entity;
    }
}
