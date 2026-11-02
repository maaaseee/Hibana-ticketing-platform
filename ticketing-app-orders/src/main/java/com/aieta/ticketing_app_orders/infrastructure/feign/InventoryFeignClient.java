package com.aieta.ticketing_app_orders.infrastructure.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(
        name = "inventory-service",
        url = "${ticketing.inventory.service-url:http://localhost:8082}",
        fallback = InventoryFeignFallback.class
)
public interface InventoryFeignClient {

    @GetMapping("/api/v1/events/{eventId}/seats/{seatId}")
    SeatInfoDto getSeatInfo(@PathVariable("eventId") String eventId, @PathVariable("seatId") String seatId);

    @PostMapping("/api/v1/reservations/{reservationId}/confirm")
    ConfirmReservationDto confirmReservation(
            @PathVariable("reservationId") String reservationId,
            @RequestBody ConfirmReservationRequestDto request
    );
}
