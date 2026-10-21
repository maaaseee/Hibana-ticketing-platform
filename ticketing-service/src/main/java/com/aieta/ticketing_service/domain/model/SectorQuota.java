package com.aieta.ticketing_service.domain.model;

import java.util.Objects;

public record SectorQuota(String sectorId, int remainingCapacity) {
    public SectorQuota {
        Objects.requireNonNull(sectorId, "SectorId cannot be null");
        if (sectorId.isBlank()) {
            throw new IllegalArgumentException("SectorId cannot be blank");
        }
        if (remainingCapacity < 0) {
            throw new IllegalArgumentException("Remaining capacity cannot be negative");
        }
    }
}
