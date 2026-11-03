package com.aieta.ticketing_app_orders.application.queries.get_invoice;

import com.aieta.ticketing_app_orders.application.common.InvoiceDataMapper;
import com.aieta.ticketing_app_orders.application.common.mediator.RequestHandler;
import com.aieta.ticketing_app_orders.application.dto.InvoiceData;
import com.aieta.ticketing_app_orders.application.ports.InvoicePdfPort;
import com.aieta.ticketing_app_orders.application.ports.InvoiceStoragePort;
import com.aieta.ticketing_app_orders.domain.exceptions.DomainException;
import com.aieta.ticketing_app_orders.domain.exceptions.OrderNotFoundException;
import com.aieta.ticketing_app_orders.domain.model.Order;
import com.aieta.ticketing_app_orders.domain.model.OrderId;
import com.aieta.ticketing_app_orders.domain.ports.OrderRepositoryPort;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Component
public class GetOrderInvoicePdfQueryHandler implements RequestHandler<GetOrderInvoicePdfQuery, byte[]> {

    private final OrderRepositoryPort orderRepository;
    private final InvoicePdfPort invoicePdfPort;
    private final InvoiceStoragePort invoiceStoragePort;

    public GetOrderInvoicePdfQueryHandler(OrderRepositoryPort orderRepository, InvoicePdfPort invoicePdfPort) {
        this(orderRepository, invoicePdfPort, null);
    }

    @Autowired
    public GetOrderInvoicePdfQueryHandler(
            OrderRepositoryPort orderRepository,
            InvoicePdfPort invoicePdfPort,
            @Autowired(required = false) InvoiceStoragePort invoiceStoragePort) {
        this.orderRepository = orderRepository;
        this.invoicePdfPort = invoicePdfPort;
        this.invoiceStoragePort = invoiceStoragePort;
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] handle(GetOrderInvoicePdfQuery request) {
        Order order = orderRepository.findById(OrderId.of(request.orderId()))
                .orElseThrow(() -> new OrderNotFoundException(request.orderId()));

        if (!order.getUserId().equals(request.userId()) && !"anonymous-user".equals(request.userId()) && !"admin".equals(request.userId())) {
            throw new DomainException("Unauthorized access to invoice for order " + request.orderId());
        }

        // 1. Retrieve pre-rendered invoice from Azure Blob Storage if available
        if (invoiceStoragePort != null) {
            Optional<byte[]> storedPdf = invoiceStoragePort.retrieveInvoicePdf(request.orderId());
            if (storedPdf.isPresent()) {
                return storedPdf.get();
            }
        }

        // 2. Generate on-demand if missing in blob storage, persist for future queries, and return
        InvoiceData invoiceData = InvoiceDataMapper.fromOrder(order, null, null, null);
        byte[] pdfBytes = invoicePdfPort.generateInvoicePdf(invoiceData);
        if (invoiceStoragePort != null && pdfBytes != null) {
            invoiceStoragePort.storeInvoicePdf(request.orderId(), pdfBytes);
        }
        return pdfBytes;
    }

    @Override
    public Class<GetOrderInvoicePdfQuery> getRequestType() {
        return GetOrderInvoicePdfQuery.class;
    }
}
