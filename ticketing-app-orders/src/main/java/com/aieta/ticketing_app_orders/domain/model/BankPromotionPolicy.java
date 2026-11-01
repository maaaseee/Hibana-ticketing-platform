package com.aieta.ticketing_app_orders.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;

public class BankPromotionPolicy {

    public record InstallmentPlan(
            int installments,
            BigDecimal interestRatePercent,
            BigDecimal totalAmount,
            BigDecimal installmentAmount,
            boolean isInterestFree,
            String bankName
    ) {}

    public static InstallmentPlan calculatePlan(BigDecimal baseAmount, String bankName, int installments) {
        if (baseAmount == null || baseAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Base amount must be positive");
        }
        if (installments <= 0) {
            installments = 1;
        }

        String normalizedBank = (bankName != null) ? bankName.trim().toUpperCase(Locale.ROOT) : "OTHER";
        BigDecimal ratePercent = getInterestRate(normalizedBank, installments);

        BigDecimal surcharge = baseAmount.multiply(ratePercent).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        BigDecimal total = baseAmount.add(surcharge).setScale(2, RoundingMode.HALF_UP);
        BigDecimal perInstallment = total.divide(BigDecimal.valueOf(installments), 2, RoundingMode.HALF_UP);
        boolean interestFree = ratePercent.compareTo(BigDecimal.ZERO) == 0;

        return new InstallmentPlan(installments, ratePercent, total, perInstallment, interestFree, normalizedBank);
    }

    public static BigDecimal getInterestRate(String bankName, int installments) {
        if (installments == 1) {
            return BigDecimal.ZERO;
        }

        return switch (bankName) {
            case "GALICIA" -> switch (installments) {
                case 3, 6 -> BigDecimal.ZERO;
                case 12 -> new BigDecimal("35.00");
                default -> new BigDecimal("20.00");
            };
            case "BBVA" -> switch (installments) {
                case 3, 6 -> BigDecimal.ZERO;
                case 12 -> new BigDecimal("36.00");
                default -> new BigDecimal("20.00");
            };
            case "NACION" -> switch (installments) {
                case 3, 6, 12 -> BigDecimal.ZERO; // Promo BNA
                default -> BigDecimal.ZERO;
            };
            case "SANTANDER" -> switch (installments) {
                case 3 -> BigDecimal.ZERO;
                case 6 -> new BigDecimal("18.00");
                case 12 -> new BigDecimal("38.00");
                default -> new BigDecimal("25.00");
            };
            case "MACRO" -> switch (installments) {
                case 3 -> BigDecimal.ZERO;
                case 6 -> new BigDecimal("15.00");
                case 12 -> new BigDecimal("35.00");
                default -> new BigDecimal("22.00");
            };
            default -> switch (installments) {
                case 3 -> new BigDecimal("12.00");
                case 6 -> new BigDecimal("25.00");
                case 12 -> new BigDecimal("48.00");
                default -> new BigDecimal("30.00");
            };
        };
    }
}
