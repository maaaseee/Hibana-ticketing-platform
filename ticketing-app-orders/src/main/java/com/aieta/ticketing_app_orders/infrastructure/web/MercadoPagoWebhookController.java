package com.aieta.ticketing_app_orders.infrastructure.web;

import com.aieta.ticketing_app_orders.application.common.mediator.Mediator;
import com.aieta.ticketing_app_orders.infrastructure.mercadopago.MercadoPagoClient;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/payments/mercadopago")
public class MercadoPagoWebhookController {
    private static final Logger log = LoggerFactory.getLogger(MercadoPagoWebhookController.class);

    private final MercadoPagoClient mercadoPagoClient;
    private final Mediator mediator;

    public MercadoPagoWebhookController(MercadoPagoClient mercadoPagoClient, Mediator mediator) {
        this.mercadoPagoClient = mercadoPagoClient;
        this.mediator = mediator;
    }

    @PostMapping("/webhook")
    public ResponseEntity<Map<String, String>> handleWebhook(
            @RequestParam(value = "topic", required = false) String topic,
            @RequestParam(value = "id", required = false) String paramId,
            @RequestBody(required = false) JsonNode body) {

        String paymentId = paramId;
        String eventType = topic;

        if (body != null) {
            if (paymentId == null && body.has("data") && body.get("data").has("id")) {
                paymentId = body.get("data").get("id").asText();
            }
            if (eventType == null && body.has("type")) {
                eventType = body.get("type").asText();
            }
        }

        log.info("[MERCADO-PAGO-WEBHOOK] Received IPN notification: type={}, paymentId={}", eventType, paymentId);

        if (paymentId != null) {
            boolean approved = mercadoPagoClient.isPaymentApproved(paymentId);
            log.info("[MERCADO-PAGO-WEBHOOK] Payment {} verification result: approved={}", paymentId, approved);
        }

        return ResponseEntity.ok(Map.of("status", "PROCESSED", "paymentId", paymentId != null ? paymentId : "N/A"));
    }
}
