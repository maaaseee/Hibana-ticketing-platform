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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Component
public class GetOrderInvoicePdfQueryHandler implements RequestHandler<GetOrderInvoicePdfQuery, byte[]> {

    private static final Logger log = LoggerFactory.getLogger(GetOrderInvoicePdfQueryHandler.class);

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
    public byte[] handle(GetOrderInvoicePdfQuery query) {
        Order order = orderRepository.findById(OrderId.of(query.orderId()))
                .orElseThrow(() -> new OrderNotFoundException(query.orderId()));

        boolean isAdmin = "ADMIN".equalsIgnoreCase(query.userId()) || "ROLE_ADMIN".equalsIgnoreCase(query.userId());
        boolean isAnonymous = query.userId() == null
                || "anonymous-user".equalsIgnoreCase(query.userId())
                || "anonymous".equalsIgnoreCase(query.userId())
                || "anonymous-user".equalsIgnoreCase(order.getUserId());
        if (!isAdmin && !isAnonymous && !order.getUserId().equals(query.userId())) {
            throw new DomainException("Access denied: Order invoice does not belong to user " + query.userId());
        }

        // Fast-path: check Azure Blob Storage / cache first
        if (invoiceStoragePort != null) {
            Optional<byte[]> cachedPdf = invoiceStoragePort.retrieveInvoicePdf(order.getId().toString());
            if (cachedPdf.isPresent()) {
                log.debug("[INVOICE-QUERY] Returning stored PDF from Azure Blob Storage for order: {}", order.getId());
                return cachedPdf.get();
            }
        }

        InvoiceData invoiceData = InvoiceDataMapper.fromOrder(order, null, null, null);
        byte[] generatedPdf = invoicePdfPort.generateInvoicePdf(invoiceData);

        if (invoiceStoragePort != null && generatedPdf != null) {
            invoiceStoragePort.storeInvoicePdf(order.getId().toString(), generatedPdf);
        }

        return generatedPdf;
    }

    @Override
    public Class<GetOrderInvoicePdfQuery> getRequestType() {
        return GetOrderInvoicePdfQuery.class;
    }
}
