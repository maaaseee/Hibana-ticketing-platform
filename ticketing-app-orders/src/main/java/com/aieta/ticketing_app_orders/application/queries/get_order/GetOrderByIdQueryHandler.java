package com.aieta.ticketing_app_orders.application.queries.get_order;

import com.aieta.ticketing_app_orders.application.common.mediator.RequestHandler;
import com.aieta.ticketing_app_orders.domain.exceptions.DomainException;
import com.aieta.ticketing_app_orders.domain.exceptions.OrderNotFoundException;
import com.aieta.ticketing_app_orders.domain.model.Order;
import com.aieta.ticketing_app_orders.domain.model.OrderId;
import com.aieta.ticketing_app_orders.domain.ports.OrderRepositoryPort;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class GetOrderByIdQueryHandler implements RequestHandler<GetOrderByIdQuery, OrderDetailDto> {

    private final OrderRepositoryPort orderRepository;

    public GetOrderByIdQueryHandler(OrderRepositoryPort orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Override
    public OrderDetailDto handle(GetOrderByIdQuery request) {
        Order order = orderRepository.findById(OrderId.of(request.orderId()))
                .orElseThrow(() -> new OrderNotFoundException(request.orderId()));

        if (!order.getUserId().equals(request.userId()) && !"ROLE_ADMIN".equals(request.userId())) {
            throw new DomainException("Unauthorized access to order " + request.orderId());
        }

        List<OrderDetailDto.OrderItemDto> items = order.getItems().stream()
                .map(i -> new OrderDetailDto.OrderItemDto(
                        i.getId().toString(),
                        i.getEventId(),
                        i.getSeatId(),
                        i.getSectorId(),
                        i.getUnitPrice(),
                        i.getQuantity(),
                        i.getDescription()))
                .toList();

        return new OrderDetailDto(
                order.getId().toString(),
                order.getUserId(),
                order.getReservationId().value(),
                order.getEventId(),
                order.getSeatId(),
                order.getTotal().amount(),
                order.getTotal().currency().getCurrencyCode(),
                order.getStatus().name(),
                order.getPaymentMethod() != null ? order.getPaymentMethod().name() : null,
                order.getCreatedAt(),
                order.getUpdatedAt(),
                items
        );
    }

    @Override
    public Class<GetOrderByIdQuery> getRequestType() {
        return GetOrderByIdQuery.class;
    }
}
