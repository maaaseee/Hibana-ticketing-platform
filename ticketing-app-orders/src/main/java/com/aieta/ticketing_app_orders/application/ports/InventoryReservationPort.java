package com.aieta.ticketing_app_orders.application.ports;

public interface InventoryReservationPort {
    void confirmReservation(String reservationId, String orderId);
}
