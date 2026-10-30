package com.aieta.ticketing_app_orders.application.queries.get_user_orders;

import com.aieta.ticketing_app_orders.application.common.mediator.RequestHandler;
import com.aieta.ticketing_app_orders.application.queries.get_order.OrderDetailDto;
import com.aieta.ticketing_app_orders.domain.model.Order;
import com.aieta.ticketing_app_orders.domain.ports.OrderRepositoryPort;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class GetUserOrdersQueryHandler implements RequestHandler<GetUserOrdersQuery, List<OrderDetailDto>> {

    private final OrderRepositoryPort orderRepository;

    public GetUserOrdersQueryHandler(OrderRepositoryPort orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Override
    public List<OrderDetailDto> handle(GetUserOrdersQuery request) {
        List<Order> orders = orderRepository.findByUserId(request.userId(), request.page(), request.size());

        return orders.stream()
                .map(order -> {
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
                })
                .toList();
    }

    @Override
    public Class<GetUserOrdersQuery> getRequestType() {
        return GetUserOrdersQuery.class;
    }
}
