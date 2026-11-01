package com.aieta.ticketing_app_orders.domain;

import com.aieta.ticketing_app_orders.domain.model.BankPromotionPolicy;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class BankPromotionPolicyTest {

    @Test
    @DisplayName("Should offer 1 payment without interest across all banks")
    void shouldOfferOnePaymentInterestFree() {
        BankPromotionPolicy.InstallmentPlan plan = BankPromotionPolicy.calculatePlan(new BigDecimal("10000.00"), "GALICIA", 1);
        assertTrue(plan.isInterestFree());
        assertEquals(new BigDecimal("10000.00"), plan.totalAmount());
        assertEquals(new BigDecimal("10000.00"), plan.installmentAmount());
    }

    @Test
    @DisplayName("Banco Galicia should provide 3 and 6 installments interest-free, 12 with interest")
    void shouldCalculateGaliciaPromo() {
        var plan3 = BankPromotionPolicy.calculatePlan(new BigDecimal("60000.00"), "GALICIA", 3);
        assertTrue(plan3.isInterestFree());
        assertEquals(new BigDecimal("60000.00"), plan3.totalAmount());
        assertEquals(new BigDecimal("20000.00"), plan3.installmentAmount());

        var plan6 = BankPromotionPolicy.calculatePlan(new BigDecimal("60000.00"), "GALICIA", 6);
        assertTrue(plan6.isInterestFree());
        assertEquals(new BigDecimal("10000.00"), plan6.installmentAmount());

        var plan12 = BankPromotionPolicy.calculatePlan(new BigDecimal("60000.00"), "GALICIA", 12);
        assertFalse(plan12.isInterestFree());
        assertEquals(new BigDecimal("81000.00"), plan12.totalAmount()); // 35% surcharge
    }

    @Test
    @DisplayName("Banco Nacion should provide up to 12 installments interest-free")
    void shouldCalculateNacionPromo() {
        var plan12 = BankPromotionPolicy.calculatePlan(new BigDecimal("120000.00"), "NACION", 12);
        assertTrue(plan12.isInterestFree());
        assertEquals(new BigDecimal("120000.00"), plan12.totalAmount());
        assertEquals(new BigDecimal("10000.00"), plan12.installmentAmount());
    }

    @Test
    @DisplayName("Banco Santander should provide 3 installments interest-free, 6 and 12 with interest")
    void shouldCalculateSantanderPromo() {
        var plan3 = BankPromotionPolicy.calculatePlan(new BigDecimal("30000.00"), "SANTANDER", 3);
        assertTrue(plan3.isInterestFree());
        assertEquals(new BigDecimal("10000.00"), plan3.installmentAmount());

        var plan6 = BankPromotionPolicy.calculatePlan(new BigDecimal("100000.00"), "SANTANDER", 6);
        assertFalse(plan6.isInterestFree());
        assertEquals(new BigDecimal("118000.00"), plan6.totalAmount()); // 18% surcharge
    }

    @Test
    @DisplayName("Other generic banks should apply standard financial surcharge")
    void shouldCalculateOtherBanksStandard() {
        var plan3 = BankPromotionPolicy.calculatePlan(new BigDecimal("10000.00"), "OTHER", 3);
        assertFalse(plan3.isInterestFree());
        assertEquals(new BigDecimal("11200.00"), plan3.totalAmount()); // 12% surcharge
    }
}
