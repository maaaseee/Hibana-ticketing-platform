package com.aieta.ticketing_app_orders.application;

import com.aieta.ticketing_app_orders.application.commands.process_payment.PaymentResult;
import com.aieta.ticketing_app_orders.application.commands.process_payment.ProcessPaymentCommand;
import com.aieta.ticketing_app_orders.application.commands.process_payment.ProcessPaymentCommandHandler;
import com.aieta.ticketing_app_orders.application.commands.process_payment.strategy.CreditCardPaymentStrategy;
import com.aieta.ticketing_app_orders.application.commands.process_payment.strategy.DebitCardPaymentStrategy;
import com.aieta.ticketing_app_orders.application.commands.process_payment.strategy.MercadoPagoPaymentStrategy;
import com.aieta.ticketing_app_orders.application.commands.process_payment.strategy.PaymentStrategyRegistry;
import com.aieta.ticketing_app_orders.application.ports.EmailNotificationPort;
import com.aieta.ticketing_app_orders.application.ports.InvoicePdfPort;
import com.aieta.ticketing_app_orders.domain.enums.PaymentMethod;
import com.aieta.ticketing_app_orders.domain.exceptions.DomainException;
import com.aieta.ticketing_app_orders.domain.model.Money;
import com.aieta.ticketing_app_orders.domain.model.Order;
import com.aieta.ticketing_app_orders.domain.model.ReservationId;
import com.aieta.ticketing_app_orders.domain.model.pocket.LedgerAccount;
import com.aieta.ticketing_app_orders.domain.ports.LedgerRepositoryPort;
import com.aieta.ticketing_app_orders.domain.ports.OrderRepositoryPort;
import com.aieta.ticketing_app_orders.infrastructure.mercadopago.MercadoPagoClient;
import com.aieta.ticketing_app_orders.infrastructure.mercadopago.MercadoPagoPreference;
import com.aieta.ticketing_app_orders.infrastructure.persistence.repository.SpringDataOutboxRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProcessPaymentCommandHandlerTest {

    @Mock
    private OrderRepositoryPort orderRepository;

    @Mock
    private LedgerRepositoryPort ledgerRepository;

    @Mock
    private SpringDataOutboxRepository outboxRepository;

    @Mock
    private InvoicePdfPort invoicePdfPort;

    @Mock
    private EmailNotificationPort emailNotificationPort;

    @Mock
    private MercadoPagoClient mercadoPagoClient;

    private PaymentStrategyRegistry strategyRegistry;
    private ObjectMapper objectMapper;
    private ProcessPaymentCommandHandler handler;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());

        strategyRegistry = new PaymentStrategyRegistry(List.of(
                new DebitCardPaymentStrategy(),
                new CreditCardPaymentStrategy(),
                new MercadoPagoPaymentStrategy(mercadoPagoClient)
        ));

        handler = new ProcessPaymentCommandHandler(
                orderRepository,
                strategyRegistry,
                ledgerRepository,
                outboxRepository,
                objectMapper,
                invoicePdfPort,
                emailNotificationPort
        );
    }

    @Test
    @DisplayName("Should successfully pay order with Debit Card in 1 payment")
    void shouldProcessDebitCardPayment() {
        Order order = Order.create("user-1", new ReservationId("res-1"), "evt-1", "seat-1", Money.of(new BigDecimal("45000.00")));

        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LedgerAccount mockAccount = new LedgerAccount(UUID.randomUUID(), "PLATFORM", LedgerAccount.AccountType.REVENUE, BigDecimal.ZERO, Currency.getInstance("EUR"));
        when(ledgerRepository.findAccountByOwnerIdAndType(anyString(), any())).thenReturn(Optional.of(mockAccount));
        when(invoicePdfPort.generateInvoicePdf(any())).thenReturn(new byte[]{1, 2, 3});

        ProcessPaymentCommand cmd = new ProcessPaymentCommand(
                order.getId().toString(),
                "user-1",
                PaymentMethod.DEBIT_CARD,
                "buyer@gmail.com",
                "Daniel Casanova",
                "20-38491024-9"
        );
        PaymentResult result = handler.handle(cmd);

        assertEquals("PAID", result.status());
        assertEquals("DEBIT_CARD", result.paymentMethod());
        assertEquals(new BigDecimal("45000.00"), result.totalAmount());

        verify(ledgerRepository).recordEntries(anyList());
        verify(outboxRepository).save(any());
        verify(invoicePdfPort).generateInvoicePdf(any());
        verify(emailNotificationPort).sendOrderConfirmation(any());
    }

    @Test
    @DisplayName("Should successfully pay order with Credit Card with 6 installments (Banco Galicia promo)")
    void shouldProcessCreditCardPaymentWithBankPromo() {
        Order order = Order.create("user-1", new ReservationId("res-1"), "evt-1", "seat-1", Money.of(new BigDecimal("50000.00")));

        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LedgerAccount mockAccount = new LedgerAccount(UUID.randomUUID(), "PLATFORM", LedgerAccount.AccountType.REVENUE, BigDecimal.ZERO, Currency.getInstance("EUR"));
        when(ledgerRepository.findAccountByOwnerIdAndType(anyString(), any())).thenReturn(Optional.of(mockAccount));
        when(invoicePdfPort.generateInvoicePdf(any())).thenReturn(new byte[]{1, 2, 3});

        ProcessPaymentCommand cmd = new ProcessPaymentCommand(
                order.getId().toString(),
                "user-1",
                PaymentMethod.CREDIT_CARD,
                "buyer@gmail.com",
                "Daniel Casanova",
                "20-38491024-9",
                6,
                "GALICIA"
        );
        PaymentResult result = handler.handle(cmd);

        assertEquals("PAID", result.status());
        assertEquals("CREDIT_CARD", result.paymentMethod());
        assertEquals(new BigDecimal("50000.00"), result.totalAmount());
    }

    @Test
    @DisplayName("Should successfully pay order with Mercado Pago strategy and trigger invoice & email")
    void shouldProcessMercadoPagoPayment() {
        Order order = Order.create("user-1", new ReservationId("res-1"), "evt-1", "seat-1", Money.of(new BigDecimal("85000.00")));

        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        when(mercadoPagoClient.createPreference(any(), any(), any()))
                .thenReturn(new MercadoPagoPreference("pref-123", "https://mp.com/init", "https://sandbox.mp.com/init", order.getId().toString()));

        LedgerAccount mockAccount = new LedgerAccount(UUID.randomUUID(), "PLATFORM", LedgerAccount.AccountType.REVENUE, BigDecimal.ZERO, Currency.getInstance("EUR"));
        when(ledgerRepository.findAccountByOwnerIdAndType(anyString(), any())).thenReturn(Optional.of(mockAccount));
        when(invoicePdfPort.generateInvoicePdf(any())).thenReturn(new byte[]{1, 2, 3});

        ProcessPaymentCommand cmd = new ProcessPaymentCommand(
                order.getId().toString(),
                "user-1",
                PaymentMethod.MERCADO_PAGO,
                "buyer@gmail.com",
                "Daniel Casanova",
                "20-38491024-9"
        );
        PaymentResult result = handler.handle(cmd);

        assertEquals("PAID", result.status());
        assertEquals("MERCADO_PAGO", result.paymentMethod());
        assertEquals(new BigDecimal("85000.00"), result.totalAmount());

        verify(ledgerRepository).recordEntries(anyList());
        verify(outboxRepository).save(any());
        verify(invoicePdfPort).generateInvoicePdf(any());
        verify(emailNotificationPort).sendOrderConfirmation(any());
    }

    @Test
    @DisplayName("Should reject payment when unauthorized user attempts access")
    void shouldRejectUnauthorizedUser() {
        Order order = Order.create("user-1", new ReservationId("res-1"), "evt-1", "seat-1", Money.of(new BigDecimal("100.00")));
        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));

        ProcessPaymentCommand cmd = new ProcessPaymentCommand(
                order.getId().toString(),
                "malicious-user",
                PaymentMethod.MERCADO_PAGO
        );

        assertThrows(DomainException.class, () -> handler.handle(cmd));
        verify(orderRepository, never()).save(any());
        verify(outboxRepository, never()).save(any());
        verify(invoicePdfPort, never()).generateInvoicePdf(any());
        verify(emailNotificationPort, never()).sendOrderConfirmation(any());
    }

    @Test
    @DisplayName("Should reject payment when payment method is disabled via feature flag")
    void shouldRejectPaymentWhenMethodIsDisabled() {
        PaymentStrategyRegistry disabledRegistry = new PaymentStrategyRegistry(
                List.of(new MercadoPagoPaymentStrategy(mercadoPagoClient)),
                true,
                true,
                false
        );
        ProcessPaymentCommandHandler handlerWithDisabledMP = new ProcessPaymentCommandHandler(
                orderRepository,
                disabledRegistry,
                ledgerRepository,
                outboxRepository,
                objectMapper,
                invoicePdfPort,
                emailNotificationPort
        );

        Order order = Order.create("user-1", new ReservationId("res-1"), "evt-1", "seat-1", Money.of(new BigDecimal("100.00")));
        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));

        ProcessPaymentCommand cmd = new ProcessPaymentCommand(
                order.getId().toString(),
                "user-1",
                PaymentMethod.MERCADO_PAGO
        );

        DomainException ex = assertThrows(DomainException.class, () -> handlerWithDisabledMP.handle(cmd));
        assertTrue(ex.getMessage().contains("temporalmente deshabilitado"));
    }
}
