package com.aieta.ticketing_app_orders.infrastructure.mercadopago;

public record MercadoPagoPreference(
        String preferenceId,
        String initPoint,
        String sandboxInitPoint,
        String externalReference
) {}
