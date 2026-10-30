package com.aieta.ticketing_app_orders.domain;

import com.aieta.ticketing_app_orders.domain.exceptions.DomainException;
import com.aieta.ticketing_app_orders.domain.model.Money;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Currency;

import static org.junit.jupiter.api.Assertions.*;

class MoneyArithmeticTest {

    @Test
    @DisplayName("Should scale amounts to 2 decimal places")
    void shouldScaleDecimalPlaces() {
        Money money = Money.of(new BigDecimal("19.999"));
        assertEquals(new BigDecimal("20.00"), money.amount());
    }

    @Test
    @DisplayName("Should add amounts with same currency")
    void shouldAddSameCurrency() {
        Money a = Money.of(new BigDecimal("25.50"));
        Money b = Money.of(new BigDecimal("14.50"));
        Money result = a.add(b);
        assertEquals(new BigDecimal("40.00"), result.amount());
        assertEquals(Currency.getInstance("EUR"), result.currency());
    }

    @Test
    @DisplayName("Should reject negative money creation")
    void shouldRejectNegativeMoney() {
        assertThrows(DomainException.class, () -> Money.of(new BigDecimal("-5.00")));
    }

    @Test
    @DisplayName("Should reject subtraction leading to negative balance")
    void shouldRejectNegativeSubtraction() {
        Money a = Money.of(new BigDecimal("10.00"));
        Money b = Money.of(new BigDecimal("20.00"));
        assertThrows(DomainException.class, () -> a.subtract(b));
    }

    @Test
    @DisplayName("Should reject currency mismatch")
    void shouldRejectCurrencyMismatch() {
        Money eur = Money.of(new BigDecimal("10.00"), "EUR");
        Money usd = Money.of(new BigDecimal("10.00"), "USD");
        assertThrows(DomainException.class, () -> eur.add(usd));
    }
}
