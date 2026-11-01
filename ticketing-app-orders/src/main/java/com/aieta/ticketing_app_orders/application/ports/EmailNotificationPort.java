package com.aieta.ticketing_app_orders.application.ports;

import com.aieta.ticketing_app_orders.application.dto.OrderConfirmationEmail;

public interface EmailNotificationPort {
    void sendOrderConfirmation(OrderConfirmationEmail emailData);
}
