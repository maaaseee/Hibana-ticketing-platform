package com.aieta.ticketing_app_orders.application.commands.process_payment.strategy;

import com.aieta.ticketing_app_orders.domain.enums.PaymentMethod;
import com.aieta.ticketing_app_orders.domain.model.BankPromotionPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class CreditCardPaymentStrategy implements PaymentStrategy {
    private static final Logger log = LoggerFactory.getLogger(CreditCardPaymentStrategy.class);

    @Override
    public PaymentMethod supportedMethod() {
        return PaymentMethod.CREDIT_CARD;
    }

    @Override
    public PaymentExecutionResult execute(PaymentContext context) {
        int installments = (context.installments() != null && context.installments() > 0)
                ? context.installments()
                : 1;
        String bank = (context.issuingBank() != null && !context.issuingBank().isBlank())
                ? context.issuingBank()
                : "OTHER";

        BankPromotionPolicy.InstallmentPlan plan = BankPromotionPolicy.calculatePlan(
                context.order().getTotal().amount(),
                bank,
                installments
        );

        log.info("[STRATEGY-CREDIT-CARD] Authorizing tokenized credit card payment for order {} (base: {}, bank: {}, installments: {}, rate: {}%, perMonth: {}, total: {}, free: {})",
                context.order().getId(),
                context.order().getTotal().amount(),
                plan.bankName(),
                plan.installments(),
                plan.interestRatePercent(),
                plan.installmentAmount(),
                plan.totalAmount(),
                plan.isInterestFree());

        String txId = "cc_tx_" + UUID.randomUUID();
        String authCode = "AUTH_" + (int)(Math.random() * 900000 + 100000);
        return PaymentExecutionResult.ofSuccess(txId, authCode);
    }
}
