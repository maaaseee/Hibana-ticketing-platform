package com.aieta.ticketing_app_orders.infrastructure.feign;

import com.aieta.ticketing_app_orders.infrastructure.config.GatewayStampValidator;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.stereotype.Component;

/**
 * Interceptor that signs outgoing internal OpenFeign requests from Orders to Inventory
 * with an authenticated ROLE_SERVICE identity and cryptographic Gateway Stamp.
 *
 * Ensures that endpoints like POST /api/v1/reservations/{reservationId}/confirm are
 * successfully authorized by Inventory's GatewayHeaderAuthFilter while preventing
 * untrusted external access.
 */
@Component
public class InventoryFeignAuthRequestInterceptor implements RequestInterceptor {

    private final GatewayStampValidator stampValidator;

    public InventoryFeignAuthRequestInterceptor(GatewayStampValidator stampValidator) {
        this.stampValidator = stampValidator;
    }

    @Override
    public void apply(RequestTemplate template) {
        String serviceUserId = "orders-service";
        String serviceRoles = "ROLE_SERVICE";
        long timestamp = System.currentTimeMillis();
        String signature = stampValidator.generateSignature(serviceUserId, serviceRoles, timestamp);

        template.header("X-User-Id", serviceUserId);
        template.header("X-User-Roles", serviceRoles);
        template.header("X-Gateway-Timestamp", String.valueOf(timestamp));
        template.header("X-Gateway-Signature", signature);
        template.header("X-Gateway-Stamp", signature);
    }
}
