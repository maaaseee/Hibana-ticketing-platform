package com.aieta.ticketing_app_inventory.infrastructure.security;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;

/**
 * Validates cryptographic internal Gateway Stamps and HMAC signatures (X-Gateway-Signature / X-Gateway-Stamp).
 * Ensures that microservices only trust identity headers (X-User-Id, X-User-Roles) when signed
 * by the API Gateway or authorized internal services with the shared secret.
 */
@Component
public class GatewayStampValidator {

    private static final Logger log = LoggerFactory.getLogger(GatewayStampValidator.class);
    private static final String HMAC_SHA256 = "HmacSHA256";
    private static final long MAX_CLOCK_SKEW_MILLIS = 10 * 60 * 1000L; // 10 minutes tolerance

    private final String secretKey;

    public GatewayStampValidator(
            @Value("${gateway.hmac.secret:${GATEWAY_HMAC_SECRET:gateway-internal-hmac-secret-2026-minimum-256-bits!}}") String secretKey) {
        this.secretKey = secretKey != null && !secretKey.isBlank()
                ? secretKey
                : "gateway-internal-hmac-secret-2026-minimum-256-bits!";
    }

    /**
     * Validates whether the incoming request contains authentic, verified gateway headers.
     */
    public boolean validateRequest(HttpServletRequest request) {
        String userId = request.getHeader("X-User-Id");
        if (userId == null || userId.isBlank()) {
            return false;
        }

        String roles = request.getHeader("X-User-Roles");
        if (roles == null || roles.isBlank()) {
            roles = request.getHeader("X-Roles");
        }
        roles = roles != null ? roles.trim() : "";

        String signature = request.getHeader("X-Gateway-Signature");
        String stamp = request.getHeader("X-Gateway-Stamp");
        String timestampHeader = request.getHeader("X-Gateway-Timestamp");
        if (timestampHeader == null || timestampHeader.isBlank()) {
            timestampHeader = request.getHeader("X-Gateway-Auth-Stamp");
        }

        // Check if timestamp is present and within acceptable clock drift
        if (timestampHeader != null && !timestampHeader.isBlank()) {
            Long timestampMillis = parseTimestampToMillis(timestampHeader);
            if (timestampMillis != null) {
                long currentMillis = System.currentTimeMillis();
                if (Math.abs(currentMillis - timestampMillis) > MAX_CLOCK_SKEW_MILLIS) {
                    log.warn("[SECURITY] Rejecting expired Gateway Stamp timestamp: {} (drift: {}ms)",
                            timestampHeader, Math.abs(currentMillis - timestampMillis));
                    return false;
                }
            }
        }

        // Test signature header
        if (signature != null && !signature.isBlank()) {
            if (verifyHmacSignature(signature.trim(), userId, roles, timestampHeader)) {
                return true;
            }
        }

        // Test gateway stamp header
        if (stamp != null && !stamp.isBlank()) {
            if (verifyHmacSignature(stamp.trim(), userId, roles, timestampHeader)) {
                return true;
            }
        }

        log.warn("[SECURITY] Invalid or missing Gateway Stamp for user '{}' on {} {}",
                userId, request.getMethod(), request.getRequestURI());
        return false;
    }

    private boolean verifyHmacSignature(String providedSig, String userId, String roles, String timestamp) {
        // Strip potential "v1=" or "hex=" prefix
        String cleanSig = providedSig;
        if (cleanSig.startsWith("v1=")) {
            cleanSig = cleanSig.substring(3);
        }

        // 1. Candidate: userId:roles:timestamp
        if (timestamp != null && !timestamp.isBlank()) {
            String candidate1 = computeHmacSha256(userId + ":" + roles + ":" + timestamp);
            if (constantTimeEquals(cleanSig, candidate1)) {
                return true;
            }
            String candidate2 = computeHmacSha256(userId + ":" + timestamp);
            if (constantTimeEquals(cleanSig, candidate2)) {
                return true;
            }
        }

        // 2. Candidate: userId:roles
        String candidateWithRoles = computeHmacSha256(userId + ":" + roles);
        if (constantTimeEquals(cleanSig, candidateWithRoles)) {
            return true;
        }

        // 3. Candidate: userId only
        String candidateUserIdOnly = computeHmacSha256(userId);
        if (constantTimeEquals(cleanSig, candidateUserIdOnly)) {
            return true;
        }

        return false;
    }

    public String generateSignature(String userId, String roles, long timestamp) {
        String payload = (userId != null ? userId : "") + ":" + (roles != null ? roles : "") + ":" + timestamp;
        return computeHmacSha256(payload);
    }

    public String generateSignature(String userId, String roles) {
        String payload = (userId != null ? userId : "") + ":" + (roles != null ? roles : "");
        return computeHmacSha256(payload);
    }

    public String computeHmacSha256(String data) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            SecretKeySpec secretKeySpec = new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), HMAC_SHA256);
            mac.init(secretKeySpec);
            byte[] rawHmac = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(rawHmac);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to calculate HMAC-SHA256 signature", e);
        }
    }

    private boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) {
            return false;
        }
        return MessageDigest.isEqual(a.getBytes(StandardCharsets.UTF_8), b.getBytes(StandardCharsets.UTF_8));
    }

    private Long parseTimestampToMillis(String timestampStr) {
        try {
            return Long.parseLong(timestampStr);
        } catch (NumberFormatException ignored) {
            try {
                return Instant.parse(timestampStr).toEpochMilli();
            } catch (Exception e) {
                return null;
            }
        }
    }
}
