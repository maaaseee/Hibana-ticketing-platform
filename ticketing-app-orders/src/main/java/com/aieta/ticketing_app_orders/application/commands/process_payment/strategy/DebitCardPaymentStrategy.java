package com.aieta.ticketing_app_orders.application.commands.process_payment.strategy;

import com.aieta.ticketing_app_orders.domain.enums.PaymentMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class DebitCardPaymentStrategy implements PaymentStrategy {
    private static final Logger log = LoggerFactory.getLogger(DebitCardPaymentStrategy.class);

    @Override
    public PaymentMethod supportedMethod() {
        return PaymentMethod.DEBIT_CARD;
    }

    @Override
    public PaymentExecutionResult execute(PaymentContext context) {
        log.info("[STRATEGY-DEBIT-CARD] Executing immediate single-payment debit for order {} (amount: {}, user: {})",
                context.order().getId(), context.order().getTotal().amount(), context.userId());

        String txId = "deb_tx_" + UUID.randomUUID();
        String authCode = "DEB_AUTH_" + (int)(Math.random() * 900000 + 100000);
        return PaymentExecutionResult.ofSuccess(txId, authCode);
    }
}
