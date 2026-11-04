package com.aieta.ticketing_app_gateway.filter;

import com.aieta.ticketing_app_gateway.service.AzureKeyVaultSecretService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

/**
 * Gateway filter for the Orders & Financial Engine routes (/api/v1/orders/**, /api/v1/payments/**, /api/v1/vouchers/**).
 * Runs after StripInboundSecurityHeadersFilter to extract the verified buyer identity from X-Checkout-Token
 * (if present and valid) or X-Client-User-Id, and injects a cryptographic Gateway HMAC stamp so downstream
 * order creation, payment processing, and PDF invoice downloads are authenticated at the perimeter.
 */
@Component
public class OrdersAuthStampFilterFactory extends AbstractGatewayFilterFactory<OrdersAuthStampFilterFactory.Config>
        implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(OrdersAuthStampFilterFactory.class);
    public static final String HEADER_CLIENT_USER_ID = "X-Client-User-Id";

    private final AzureKeyVaultSecretService keyVaultSecretService;

    @Value("${waiting-room.jwt.secret:${WAITING_ROOM_JWT_SECRET:virtual-waiting-room-hmac-key-2026-minimum-256-bits!}}")
    private String defaultJwtSecret;

    @Value("${waiting-room.jwt.issuer:${WAITING_ROOM_JWT_ISSUER:waiting-room-engine}}")
    private String defaultJwtIssuer;

    @Value("${gateway.hmac.secret:${GATEWAY_HMAC_SECRET:" + CheckoutTokenFilterFactory.DEFAULT_GATEWAY_HMAC_SECRET + "}}")
    private String gatewayHmacSecret = CheckoutTokenFilterFactory.DEFAULT_GATEWAY_HMAC_SECRET;

    public OrdersAuthStampFilterFactory() {
        this(null);
    }

    @Autowired
    public OrdersAuthStampFilterFactory(@Autowired(required = false) AzureKeyVaultSecretService keyVaultSecretService) {
        super(Config.class);
        this.keyVaultSecretService = keyVaultSecretService;
    }

    @Override
    public String name() {
        return "OrdersAuthStamp";
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 10;
    }

    private String computeHmacSha256(String data, String secret) {
        try {
            String effectiveSecret = (secret != null && !secret.isBlank())
                    ? secret
                    : CheckoutTokenFilterFactory.DEFAULT_GATEWAY_HMAC_SECRET;
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec spec = new SecretKeySpec(effectiveSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(spec);
            byte[] raw = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(raw);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to calculate HMAC-SHA256 signature", e);
        }
    }

    private String resolveUserId(ServerHttpRequest request) {
        String checkoutToken = request.getHeaders().getFirst(CheckoutTokenFilterFactory.HEADER_CHECKOUT_TOKEN);
        if (checkoutToken != null && !checkoutToken.isBlank() && checkoutToken.contains(".")) {
            try {
                String secret = (keyVaultSecretService != null)
                        ? keyVaultSecretService.getSecret("waiting-room-jwt-secret", defaultJwtSecret)
                        : defaultJwtSecret;
                SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
                Claims claims = Jwts.parser()
                        .verifyWith(key)
                        .requireIssuer(defaultJwtIssuer)
                        .build()
                        .parseSignedClaims(checkoutToken)
                        .getPayload();
                if (claims.getSubject() != null && !claims.getSubject().isBlank()) {
                    return claims.getSubject();
                }
            } catch (Exception e) {
                log.debug("[ORDERS-AUTH-STAMP] Non-fatal: could not extract subject from X-Checkout-Token: {}", e.getMessage());
            }
        }

        String clientUserId = request.getHeaders().getFirst(HEADER_CLIENT_USER_ID);
        if (clientUserId != null && !clientUserId.isBlank()) {
            return clientUserId.trim();
        }

        return "anonymous-user";
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        if (path == null || !(path.startsWith("/api/v1/orders")
                || path.startsWith("/api/v1/payments")
                || path.startsWith("/api/v1/vouchers"))) {
            return chain.filter(exchange);
        }
        return stampAndForward(exchange, chain);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return this::stampAndForward;
    }

    private Mono<Void> stampAndForward(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String userId = resolveUserId(request);
        String roles = "ROLE_USER";
        long timestamp = System.currentTimeMillis();
        String signature = computeHmacSha256(userId + ":" + roles + ":" + timestamp, gatewayHmacSecret);

        ServerHttpRequest mutatedRequest = request.mutate()
                .headers(h -> {
                    h.set(CheckoutTokenFilterFactory.HEADER_USER_ID, userId);
                    h.set("X-User-Roles", roles);
                    h.set("X-Gateway-Timestamp", String.valueOf(timestamp));
                    h.set("X-Gateway-Signature", signature);
                    h.set(CheckoutTokenFilterFactory.HEADER_GATEWAY_STAMP, signature);
                })
                .build();

        return chain.filter(exchange.mutate().request(mutatedRequest).build());
    }

    public static class Config {
    }
}
