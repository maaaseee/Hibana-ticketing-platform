package com.aieta.ticketing_app_catalog.infrastructure.api.dto;

import java.math.BigDecimal;

public record SectorResponse(
        String sectorId,
        String name,
        String type,
        int totalCapacity,
        BigDecimal basePrice,
        BigDecimal serviceFee,
        BigDecimal totalPrice,
        String currency
) {}
