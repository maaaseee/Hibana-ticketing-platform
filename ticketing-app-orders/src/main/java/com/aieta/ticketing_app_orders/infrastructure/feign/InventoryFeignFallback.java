package com.aieta.ticketing_app_orders.infrastructure.feign;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class InventoryFeignFallback implements InventoryFeignClient {
    private static final Logger log = LoggerFactory.getLogger(InventoryFeignFallback.class);

    @Override
    public SeatInfoDto getSeatInfo(String eventId, String seatId) {
        log.warn("InventoryFeign fallback triggered for eventId={}, seatId={}. Inventory service might be down.", eventId, seatId);
        return new SeatInfoDto(seatId, eventId, "UNKNOWN", BigDecimal.ZERO);
    }
}
