package com.aieta.ticketing_app_orders.infrastructure.feign;

import com.aieta.ticketing_app_orders.application.ports.InventoryReservationPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class InventoryReservationAdapter implements InventoryReservationPort {
    private static final Logger log = LoggerFactory.getLogger(InventoryReservationAdapter.class);

    private final InventoryFeignClient inventoryFeignClient;

    public InventoryReservationAdapter(InventoryFeignClient inventoryFeignClient) {
        this.inventoryFeignClient = inventoryFeignClient;
    }

    @Override
    public void confirmReservation(String reservationId, String orderId) {
        if (reservationId == null || reservationId.isBlank()) {
            log.warn("[SAGA-INVENTORY] Skipped reservation confirmation: reservationId is blank");
            return;
        }
        try {
            log.info("[SAGA-INVENTORY] Confirming reservation {} for order {}", reservationId, orderId);
            ConfirmReservationDto result = inventoryFeignClient.confirmReservation(
                    reservationId,
                    new ConfirmReservationRequestDto(orderId)
            );
            log.info("[SAGA-INVENTORY] Reservation {} confirmed: status={}, orderId={}",
                    reservationId, result != null ? result.status() : "OK", orderId);
        } catch (Exception e) {
            log.error("[SAGA-INVENTORY] Failed to confirm reservation {} for order {}: {}",
                    reservationId, orderId, e.getMessage(), e);
        }
    }
}
