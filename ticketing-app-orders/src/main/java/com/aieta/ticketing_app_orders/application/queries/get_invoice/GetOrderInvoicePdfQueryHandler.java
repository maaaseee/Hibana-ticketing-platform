package com.aieta.ticketing_app_orders.application.queries.get_invoice;

import com.aieta.ticketing_app_orders.application.common.InvoiceDataMapper;
import com.aieta.ticketing_app_orders.application.common.mediator.RequestHandler;
import com.aieta.ticketing_app_orders.application.dto.InvoiceData;
import com.aieta.ticketing_app_orders.application.ports.InvoicePdfPort;
import com.aieta.ticketing_app_orders.domain.exceptions.DomainException;
import com.aieta.ticketing_app_orders.domain.exceptions.OrderNotFoundException;
import com.aieta.ticketing_app_orders.domain.model.Order;
import com.aieta.ticketing_app_orders.domain.model.OrderId;
import com.aieta.ticketing_app_orders.domain.ports.OrderRepositoryPort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class GetOrderInvoicePdfQueryHandler implements RequestHandler<GetOrderInvoicePdfQuery, byte[]> {

    private final OrderRepositoryPort orderRepository;
    private final InvoicePdfPort invoicePdfPort;

    public GetOrderInvoicePdfQueryHandler(OrderRepositoryPort orderRepository, InvoicePdfPort invoicePdfPort) {
        this.orderRepository = orderRepository;
        this.invoicePdfPort = invoicePdfPort;
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] handle(GetOrderInvoicePdfQuery request) {
        Order order = orderRepository.findById(OrderId.of(request.orderId()))
                .orElseThrow(() -> new OrderNotFoundException(request.orderId()));

        if (!order.getUserId().equals(request.userId()) && !"anonymous-user".equals(request.userId()) && !"admin".equals(request.userId())) {
            throw new DomainException("Unauthorized access to invoice for order " + request.orderId());
        }

        InvoiceData invoiceData = InvoiceDataMapper.fromOrder(order, null, null, null);
        return invoicePdfPort.generateInvoicePdf(invoiceData);
    }

    @Override
    public Class<GetOrderInvoicePdfQuery> getRequestType() {
        return GetOrderInvoicePdfQuery.class;
    }
}
