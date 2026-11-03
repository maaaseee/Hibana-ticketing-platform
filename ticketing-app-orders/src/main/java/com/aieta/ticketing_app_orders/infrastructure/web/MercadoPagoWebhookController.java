package com.aieta.ticketing_app_orders.infrastructure.web;

import com.aieta.ticketing_app_orders.application.common.mediator.Mediator;
import com.aieta.ticketing_app_orders.infrastructure.mercadopago.MercadoPagoClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

@RestController
@RequestMapping("/api/v1/payments/mercadopago")
public class MercadoPagoWebhookController {
    private static final Logger log = LoggerFactory.getLogger(MercadoPagoWebhookController.class);

    public static final long MAX_ALLOWED_DRIFT_MS = 5 * 60 * 1000L; // 5 minutes

    private final MercadoPagoClient mercadoPagoClient;
    private final Mediator mediator;
    private final String webhookSecret;
    private final ObjectMapper objectMapper;

    @Autowired
    public MercadoPagoWebhookController(
            MercadoPagoClient mercadoPagoClient,
            Mediator mediator,
            @Value("${mercadopago.webhook.secret:mercadopago-webhook-secret-key-prod-minimum-256-bits!}") String webhookSecret,
            @Autowired(required = false) ObjectMapper objectMapper) {
        this.mercadoPagoClient = mercadoPagoClient;
        this.mediator = mediator;
        this.webhookSecret = (webhookSecret != null && !webhookSecret.isBlank())
                ? webhookSecret
                : "mercadopago-webhook-secret-key-prod-minimum-256-bits!";
        this.objectMapper = objectMapper != null ? objectMapper : new ObjectMapper();
    }

    public MercadoPagoWebhookController(MercadoPagoClient mercadoPagoClient, Mediator mediator) {
        this(mercadoPagoClient, mediator, "mercadopago-webhook-secret-key-prod-minimum-256-bits!", new ObjectMapper());
    }

    @PostMapping("/webhook")
    public ResponseEntity<Map<String, String>> handleWebhook(
            @RequestHeader(value = "x-signature", required = false) String xSignature,
            @RequestHeader(value = "x-request-id", required = false) String xRequestId,
            @RequestParam(value = "topic", required = false) String topic,
            @RequestParam(value = "id", required = false) String paramId,
            @RequestParam(value = "data.id", required = false) String dataId,
            @RequestParam(value = "type", required = false) String queryType,
            @RequestBody(required = false) String rawBody) {

        // 1. Mandatory x-signature validation
        if (xSignature == null || xSignature.isBlank()) {
            log.warn("[MERCADO-PAGO-WEBHOOK] Missing x-signature header");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Unauthorized", "message", "Missing x-signature header"));
        }

        Map<String, String> signatureParts = parseSignatureHeader(xSignature);
        String tsStr = signatureParts.get("ts");
        String hashV1 = signatureParts.get("v1");

        if (tsStr == null || hashV1 == null || tsStr.isBlank() || hashV1.isBlank()) {
            log.warn("[MERCADO-PAGO-WEBHOOK] Malformed x-signature header: ts and v1 are required");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Unauthorized", "message", "Malformed x-signature header: ts and v1 are required"));
        }

        // 2. Timestamp drift validation (reject if > 5 minutes drift)
        long ts;
        try {
            ts = Long.parseLong(tsStr);
        } catch (NumberFormatException e) {
            log.warn("[MERCADO-PAGO-WEBHOOK] Invalid timestamp format in x-signature: {}", tsStr);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Unauthorized", "message", "Invalid timestamp in signature"));
        }

        long tsMillis = (ts < 100_000_000_000L) ? ts * 1000L : ts;
        long now = System.currentTimeMillis();
        long drift = Math.abs(now - tsMillis);
        if (drift > MAX_ALLOWED_DRIFT_MS) {
            log.warn("[MERCADO-PAGO-WEBHOOK] Timestamp drift exceeded: {} ms (limit: {} ms)", drift, MAX_ALLOWED_DRIFT_MS);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Unauthorized", "message", "Webhook timestamp drift exceeded 5 minutes"));
        }

        // Parse optional JSON body
        JsonNode bodyNode = null;
        if (rawBody != null && !rawBody.isBlank()) {
            try {
                bodyNode = objectMapper.readTree(rawBody);
            } catch (Exception e) {
                log.debug("[MERCADO-PAGO-WEBHOOK] Non-JSON payload received: {}", e.getMessage());
            }
        }

        String paymentId = paramId != null ? paramId : dataId;
        String eventType = topic != null ? topic : queryType;

        if (bodyNode != null) {
            if (paymentId == null && bodyNode.has("data") && bodyNode.get("data").has("id")) {
                paymentId = bodyNode.get("data").get("id").asText();
            }
            if (eventType == null && bodyNode.has("type")) {
                eventType = bodyNode.get("type").asText();
            }
        }

        // 3. Cryptographic HMAC-SHA256 signature verification
        if (!verifySignature(hashV1, tsStr, xRequestId, paymentId, dataId, rawBody)) {
            log.warn("[MERCADO-PAGO-WEBHOOK] Webhook signature verification failed for ts={}", tsStr);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Unauthorized", "message", "Invalid webhook signature"));
        }

        log.info("[MERCADO-PAGO-WEBHOOK] Received authenticated IPN notification: type={}, paymentId={}", eventType, paymentId);

        if (paymentId != null) {
            boolean approved = mercadoPagoClient.isPaymentApproved(paymentId);
            log.info("[MERCADO-PAGO-WEBHOOK] Payment {} verification result: approved={}", paymentId, approved);
        }

        return ResponseEntity.ok(Map.of("status", "PROCESSED", "paymentId", paymentId != null ? paymentId : "N/A"));
    }

    public static Map<String, String> parseSignatureHeader(String header) {
        Map<String, String> parts = new HashMap<>();
        if (header == null) {
            return parts;
        }
        for (String segment : header.split("[,;]")) {
            String trimmed = segment.trim();
            int eqIdx = trimmed.indexOf('=');
            if (eqIdx > 0 && eqIdx < trimmed.length() - 1) {
                String key = trimmed.substring(0, eqIdx).trim().toLowerCase(Locale.ROOT);
                String value = trimmed.substring(eqIdx + 1).trim();
                parts.put(key, value);
            }
        }
        return parts;
    }

    public boolean verifySignature(String receivedHash, String ts, String xRequestId, String paymentId, String dataId, String rawBody) {
        if (receivedHash == null || receivedHash.isBlank()) {
            return false;
        }

        String effectiveId = paymentId != null ? paymentId : (dataId != null ? dataId : "");
        String reqId = xRequestId != null ? xRequestId : "";
        String trimmedBody = rawBody != null ? rawBody.trim() : "";

        // Standard MercadoPago template: id:[data.id_url];request-id:[x-request-id_header];ts:[ts_header];
        String manifestOfficial = "id:" + effectiveId + ";request-id:" + reqId + ";ts:" + ts + ";";
        String manifestDataId = "id:" + (dataId != null ? dataId : "") + ";request-id:" + reqId + ";ts:" + ts + ";";
        // Remediation plan template: request-id + timestamp + body
        String manifestBodyColons = reqId + ":" + ts + ":" + trimmedBody;
        String manifestBodySemi = "request-id:" + reqId + ";ts:" + ts + ";body:" + trimmedBody + ";";
        String manifestTsDotBody = ts + "." + trimmedBody;
        String manifestTsBody = ts + trimmedBody;

        List<String> candidates = List.of(
                manifestOfficial,
                manifestDataId,
                manifestBodyColons,
                manifestBodySemi,
                manifestTsDotBody,
                manifestTsBody
        );

        byte[] expectedBytes = receivedHash.trim().toLowerCase(Locale.ROOT).getBytes(StandardCharsets.UTF_8);
        for (String candidate : candidates) {
            String calculatedHex = calculateHmacSha256(candidate, this.webhookSecret);
            byte[] calculatedBytes = calculatedHex.toLowerCase(Locale.ROOT).getBytes(StandardCharsets.UTF_8);
            if (MessageDigest.isEqual(calculatedBytes, expectedBytes)) {
                return true;
            }
        }
        return false;
    }

    public static String calculateHmacSha256(String data, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec keySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(keySpec);
            byte[] rawHmac = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(rawHmac);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to calculate HMAC-SHA256 signature", e);
        }
    }

    public String generateSignatureHeader(String manifest, long timestampSecondsOrMillis) {
        String hmac = calculateHmacSha256(manifest, this.webhookSecret);
        return "ts=" + timestampSecondsOrMillis + ",v1=" + hmac;
    }
}
