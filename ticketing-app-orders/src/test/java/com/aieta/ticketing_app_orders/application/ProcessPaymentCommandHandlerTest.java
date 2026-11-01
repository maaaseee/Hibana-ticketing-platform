package com.aieta.ticketing_app_orders.application;

import com.aieta.ticketing_app_orders.application.commands.process_payment.PaymentResult;
import com.aieta.ticketing_app_orders.application.commands.process_payment.ProcessPaymentCommand;
import com.aieta.ticketing_app_orders.application.commands.process_payment.ProcessPaymentCommandHandler;
import com.aieta.ticketing_app_orders.application.ports.EmailNotificationPort;
import com.aieta.ticketing_app_orders.application.ports.InvoicePdfPort;
import com.aieta.ticketing_app_orders.domain.enums.PaymentMethod;
import com.aieta.ticketing_app_orders.domain.exceptions.InsufficientFundsException;
import com.aieta.ticketing_app_orders.domain.model.Money;
import com.aieta.ticketing_app_orders.domain.model.Order;
import com.aieta.ticketing_app_orders.domain.model.ReservationId;
import com.aieta.ticketing_app_orders.domain.model.VirtualWallet;
import com.aieta.ticketing_app_orders.domain.model.pocket.LedgerAccount;
import com.aieta.ticketing_app_orders.domain.ports.LedgerRepositoryPort;
import com.aieta.ticketing_app_orders.domain.ports.OrderRepositoryPort;
import com.aieta.ticketing_app_orders.domain.ports.WalletRepositoryPort;
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
    private WalletRepositoryPort walletRepository;

    @Mock
    private LedgerRepositoryPort ledgerRepository;

    @Mock
    private SpringDataOutboxRepository outboxRepository;

    @Mock
    private InvoicePdfPort invoicePdfPort;

    @Mock
    private EmailNotificationPort emailNotificationPort;

    private ObjectMapper objectMapper;
    private ProcessPaymentCommandHandler handler;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
        handler = new ProcessPaymentCommandHandler(
                orderRepository,
                walletRepository,
                ledgerRepository,
                outboxRepository,
                objectMapper,
                invoicePdfPort,
                emailNotificationPort
        );
    }

    @Test
    @DisplayName("Should successfully pay order with pocket wallet and trigger invoice & email")
    void shouldProcessPocketWalletPayment() {
        Order order = Order.create("user-1", new ReservationId("res-1"), "evt-1", "seat-1", Money.of(new BigDecimal("50.00")));
        VirtualWallet wallet = VirtualWallet.create("user-1", Currency.getInstance("EUR"));
        wallet.credit(new BigDecimal("100.00"));

        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));
        when(walletRepository.findByUserIdForUpdate("user-1")).thenReturn(Optional.of(wallet));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LedgerAccount mockAccount = new LedgerAccount(UUID.randomUUID(), "PLATFORM", LedgerAccount.AccountType.REVENUE, BigDecimal.ZERO, Currency.getInstance("EUR"));
        when(ledgerRepository.findAccountByOwnerIdAndType(anyString(), any())).thenReturn(Optional.of(mockAccount));
        when(invoicePdfPort.generateInvoicePdf(any())).thenReturn(new byte[]{1, 2, 3});

        ProcessPaymentCommand cmd = new ProcessPaymentCommand(order.getId().toString(), "user-1", PaymentMethod.POCKET_WALLET);
        PaymentResult result = handler.handle(cmd);

        assertEquals("PAID", result.status());
        assertEquals("POCKET_WALLET", result.paymentMethod());
        assertEquals(new BigDecimal("50.00"), wallet.getBalance());

        verify(walletRepository).save(wallet);
        verify(ledgerRepository).recordEntries(anyList());
        verify(outboxRepository).save(any());
        verify(invoicePdfPort).generateInvoicePdf(any());
        verify(emailNotificationPort).sendOrderConfirmation(any());
    }

    @Test
    @DisplayName("Should reject payment when wallet has insufficient funds")
    void shouldRejectInsufficientFunds() {
        Order order = Order.create("user-1", new ReservationId("res-1"), "evt-1", "seat-1", Money.of(new BigDecimal("100.00")));
        VirtualWallet wallet = VirtualWallet.create("user-1", Currency.getInstance("EUR"));
        wallet.credit(new BigDecimal("20.00")); // insufficient!

        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));
        when(walletRepository.findByUserIdForUpdate("user-1")).thenReturn(Optional.of(wallet));

        ProcessPaymentCommand cmd = new ProcessPaymentCommand(order.getId().toString(), "user-1", PaymentMethod.POCKET_WALLET);

        assertThrows(InsufficientFundsException.class, () -> handler.handle(cmd));
        verify(orderRepository, never()).save(any());
        verify(outboxRepository, never()).save(any());
        verify(invoicePdfPort, never()).generateInvoicePdf(any());
        verify(emailNotificationPort, never()).sendOrderConfirmation(any());
    }
}
