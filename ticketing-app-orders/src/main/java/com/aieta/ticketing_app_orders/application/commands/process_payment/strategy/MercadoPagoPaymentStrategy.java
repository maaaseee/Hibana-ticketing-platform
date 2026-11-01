package com.aieta.ticketing_app_orders.application.commands.process_payment.strategy;

import com.aieta.ticketing_app_orders.domain.enums.PaymentMethod;
import com.aieta.ticketing_app_orders.infrastructure.mercadopago.MercadoPagoClient;
import com.aieta.ticketing_app_orders.infrastructure.mercadopago.MercadoPagoPreference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class MercadoPagoPaymentStrategy implements PaymentStrategy {
    private static final Logger log = LoggerFactory.getLogger(MercadoPagoPaymentStrategy.class);

    private final MercadoPagoClient mercadoPagoClient;

    public MercadoPagoPaymentStrategy(MercadoPagoClient mercadoPagoClient) {
        this.mercadoPagoClient = mercadoPagoClient;
    }

    @Override
    public PaymentMethod supportedMethod() {
        return PaymentMethod.MERCADO_PAGO;
    }

    @Override
    public PaymentExecutionResult execute(PaymentContext context) {
        log.info("[STRATEGY-MERCADO-PAGO] Orchestrating Mercado Pago checkout preference for order {} (amount: {}, user: {}, buyer: {})",
                context.order().getId(), context.order().getTotal().amount(), context.userId(), context.buyerEmail());

        MercadoPagoPreference preference = mercadoPagoClient.createPreference(
                context.order(),
                context.buyerEmail(),
                context.buyerName()
        );

        return PaymentExecutionResult.ofSuccess(preference.preferenceId(), preference.sandboxInitPoint());
    }
}
