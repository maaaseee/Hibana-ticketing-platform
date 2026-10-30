package com.aieta.ticketing_app_orders.infrastructure.persistence.adapter;

import com.aieta.ticketing_app_orders.domain.model.Order;
import com.aieta.ticketing_app_orders.domain.model.OrderId;
import com.aieta.ticketing_app_orders.domain.ports.OrderRepositoryPort;
import com.aieta.ticketing_app_orders.infrastructure.persistence.entity.OrderJpaEntity;
import com.aieta.ticketing_app_orders.infrastructure.persistence.mapper.OrderMapper;
import com.aieta.ticketing_app_orders.infrastructure.persistence.repository.SpringDataOrderRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class JpaOrderRepositoryAdapter implements OrderRepositoryPort {

    private final SpringDataOrderRepository springDataOrderRepository;

    public JpaOrderRepositoryAdapter(SpringDataOrderRepository springDataOrderRepository) {
        this.springDataOrderRepository = springDataOrderRepository;
    }

    @Override
    public Order save(Order order) {
        OrderJpaEntity entity = OrderMapper.toEntity(order);
        OrderJpaEntity saved = springDataOrderRepository.save(entity);
        return OrderMapper.toDomain(saved);
    }

    @Override
    public Optional<Order> findById(OrderId id) {
        return springDataOrderRepository.findById(id.value())
                .map(OrderMapper::toDomain);
    }

    @Override
    public Optional<Order> findByReservationId(String reservationId) {
        return springDataOrderRepository.findByReservationId(reservationId)
                .map(OrderMapper::toDomain);
    }

    @Override
    public List<Order> findByUserId(String userId, int page, int size) {
        return springDataOrderRepository.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(page, size))
                .stream()
                .map(OrderMapper::toDomain)
                .toList();
    }
}
