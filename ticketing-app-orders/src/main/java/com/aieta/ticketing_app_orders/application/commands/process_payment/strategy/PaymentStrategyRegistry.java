package com.aieta.ticketing_app_orders.application.commands.process_payment.strategy;

import com.aieta.ticketing_app_orders.domain.enums.PaymentMethod;
import com.aieta.ticketing_app_orders.domain.exceptions.DomainException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
public class PaymentStrategyRegistry {

    private final Map<PaymentMethod, PaymentStrategy> strategies = new EnumMap<>(PaymentMethod.class);
    private final boolean debitCardEnabled;
    private final boolean creditCardEnabled;
    private final boolean mercadoPagoEnabled;

    @org.springframework.beans.factory.annotation.Autowired
    public PaymentStrategyRegistry(
            List<PaymentStrategy> strategyList,
            @Value("${payment.methods.debit-card.enabled:true}") boolean debitCardEnabled,
            @Value("${payment.methods.credit-card.enabled:true}") boolean creditCardEnabled,
            @Value("${payment.methods.mercado-pago.enabled:false}") boolean mercadoPagoEnabled) {
        this.debitCardEnabled = debitCardEnabled;
        this.creditCardEnabled = creditCardEnabled;
        this.mercadoPagoEnabled = mercadoPagoEnabled;
        for (PaymentStrategy strategy : strategyList) {
            strategies.put(strategy.supportedMethod(), strategy);
        }
    }

    /**
     * Default constructor for test scenarios allowing all payment methods by default.
     */
    public PaymentStrategyRegistry(List<PaymentStrategy> strategyList) {
        this(strategyList, true, true, true);
    }

    public boolean isMethodEnabled(PaymentMethod method) {
        return switch (method) {
            case DEBIT_CARD -> debitCardEnabled;
            case CREDIT_CARD -> creditCardEnabled;
            case MERCADO_PAGO -> mercadoPagoEnabled;
        };
    }

    public PaymentStrategy getStrategy(PaymentMethod method) {
        if (!isMethodEnabled(method)) {
            throw new DomainException("El método de pago " + method + " se encuentra temporalmente deshabilitado.");
        }
        PaymentStrategy strategy = strategies.get(method);
        if (strategy == null) {
            throw new DomainException("No payment strategy configured for payment method: " + method);
        }
        return strategy;
    }

    public List<PaymentMethodStatusDto> getAvailableMethods() {
        return List.of(
                new PaymentMethodStatusDto(PaymentMethod.DEBIT_CARD.name(), "Tarjeta de Débito", debitCardEnabled, null),
                new PaymentMethodStatusDto(PaymentMethod.CREDIT_CARD.name(), "Tarjeta de Crédito", creditCardEnabled, null),
                new PaymentMethodStatusDto(PaymentMethod.MERCADO_PAGO.name(), "Mercado Pago", mercadoPagoEnabled, mercadoPagoEnabled ? "OFICIAL" : "Próximamente")
        );
    }

    public record PaymentMethodStatusDto(String id, String name, boolean enabled, String badge) {}
}
