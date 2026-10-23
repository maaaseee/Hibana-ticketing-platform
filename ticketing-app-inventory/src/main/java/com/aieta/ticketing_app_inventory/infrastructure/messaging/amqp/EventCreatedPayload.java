package com.aieta.ticketing_app_inventory.infrastructure.messaging.amqp;

import java.math.BigDecimal;
import java.util.List;

public record EventCreatedPayload(
        String eventId,
        String venueId,
        String slug,
        String title,
        List<SectorItemPayload> sectors
) {
    public record SectorItemPayload(
            String sectorId,
            String name,
            String type,
            int totalCapacity,
            BigDecimal basePrice,
            BigDecimal serviceFee,
            String currency,
            Integer seatsPerRow
    ) {
        public SectorItemPayload(String sectorId, String name, String type, int totalCapacity, BigDecimal basePrice, BigDecimal serviceFee, String currency) {
            this(sectorId, name, type, totalCapacity, basePrice, serviceFee, currency, null);
        }
    }
}
