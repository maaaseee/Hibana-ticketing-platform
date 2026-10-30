package com.aieta.ticketing_app_orders.domain.ports;

import com.aieta.ticketing_app_orders.domain.model.Order;
import com.aieta.ticketing_app_orders.domain.model.OrderId;

import java.util.List;
import java.util.Optional;

public interface OrderRepositoryPort {
    Order save(Order order);
    Optional<Order> findById(OrderId id);
    Optional<Order> findByReservationId(String reservationId);
    List<Order> findByUserId(String userId, int page, int size);
}
