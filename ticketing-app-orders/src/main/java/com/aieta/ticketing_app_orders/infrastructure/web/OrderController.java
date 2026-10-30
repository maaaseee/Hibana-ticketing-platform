package com.aieta.ticketing_app_orders.infrastructure.web;

import com.aieta.ticketing_app_orders.application.commands.cancel_order.CancelOrderCommand;
import com.aieta.ticketing_app_orders.application.commands.create_order.CreateOrderCommand;
import com.aieta.ticketing_app_orders.application.commands.create_order.CreateOrderResult;
import com.aieta.ticketing_app_orders.application.common.mediator.Mediator;
import com.aieta.ticketing_app_orders.application.queries.get_order.GetOrderByIdQuery;
import com.aieta.ticketing_app_orders.application.queries.get_order.OrderDetailDto;
import com.aieta.ticketing_app_orders.application.queries.get_user_orders.GetUserOrdersQuery;
import com.aieta.ticketing_app_orders.infrastructure.web.dto.CreateOrderRequest;
import com.aieta.ticketing_app_orders.infrastructure.web.dto.CreateOrderResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    private final Mediator mediator;

    public OrderController(Mediator mediator) {
        this.mediator = mediator;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CreateOrderResponse createOrder(
            @RequestHeader(value = "X-User-Id", required = false) String headerUserId,
            @Valid @RequestBody CreateOrderRequest request) {
        String effectiveUserId = (headerUserId != null && !headerUserId.isBlank()) ? headerUserId : "anonymous-user";

        CreateOrderResult result = mediator.dispatch(new CreateOrderCommand(
                effectiveUserId,
                request.reservationId(),
                request.eventId(),
                request.seatId(),
                request.totalAmount()
        ));

        return new CreateOrderResponse(
                result.orderId(),
                result.userId(),
                result.reservationId(),
                result.eventId(),
                result.seatId(),
                result.totalAmount(),
                result.status(),
                result.createdAt()
        );
    }

    @GetMapping("/{orderId}")
    public OrderDetailDto getOrder(
            @RequestHeader(value = "X-User-Id", required = false) String headerUserId,
            @PathVariable("orderId") String orderId) {
        String effectiveUserId = (headerUserId != null && !headerUserId.isBlank()) ? headerUserId : "anonymous-user";
        return mediator.dispatch(new GetOrderByIdQuery(orderId, effectiveUserId));
    }

    @GetMapping("/me")
    public List<OrderDetailDto> getUserOrders(
            @RequestHeader(value = "X-User-Id", required = false) String headerUserId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        String effectiveUserId = (headerUserId != null && !headerUserId.isBlank()) ? headerUserId : "anonymous-user";
        return mediator.dispatch(new GetUserOrdersQuery(effectiveUserId, page, size));
    }

    @PostMapping("/{orderId}/cancel")
    public Map<String, Object> cancelOrder(
            @RequestHeader(value = "X-User-Id", required = false) String headerUserId,
            @PathVariable("orderId") String orderId,
            @RequestBody(required = false) Map<String, String> body) {
        String effectiveUserId = (headerUserId != null && !headerUserId.isBlank()) ? headerUserId : "anonymous-user";
        String reason = (body != null && body.containsKey("reason")) ? body.get("reason") : "User requested cancellation";

        mediator.dispatch(new CancelOrderCommand(orderId, effectiveUserId, reason));
        return Map.of("orderId", orderId, "status", "CANCELLED", "message", "Order cancelled and compensation initiated");
    }
}
