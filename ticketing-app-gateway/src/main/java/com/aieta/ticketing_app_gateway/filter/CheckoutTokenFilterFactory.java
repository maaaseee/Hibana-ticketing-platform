package com.aieta.ticketing_app_gateway.filter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;

@Component
public class CheckoutTokenFilterFactory extends AbstractGatewayFilterFactory<CheckoutTokenFilterFactory.Config> {

    private static final Logger log = LoggerFactory.getLogger(CheckoutTokenFilterFactory.class);
    public static final String HEADER_CHECKOUT_TOKEN = "X-Checkout-Token";
    public static final String HEADER_USER_ID = "X-User-Id";
    public static final String HEADER_WAITING_ROOM_VALIDATED = "X-Waiting-Room-Validated";

    private final ObjectMapper objectMapper;

    @Value("${waiting-room.jwt.secret:amaryllis-super-secret-waiting-room-hmac-key-2026-minimum-256-bits!}")
    private String defaultSecret;

    @Value("${waiting-room.jwt.issuer:amaryllis-waiting-room}")
    private String defaultIssuer;

    @Value("${waiting-room.url:https://www.amaryllis-tickets.com/waiting-room}")
    private String defaultWaitingRoomUrl;

    public CheckoutTokenFilterFactory(ObjectMapper objectMapper) {
        super(Config.class);
        this.objectMapper = objectMapper;
    }

    @Override
    public String name() {
        return "CheckoutToken";
    }

    @Override
    public List<String> shortcutFieldOrder() {
        return List.of("secretKey", "issuer", "waitingRoomUrl");
    }

    @Override
    public GatewayFilter apply(Config config) {
        String secret = (config.getSecretKey() != null && !config.getSecretKey().isBlank())
                ? config.getSecretKey()
                : defaultSecret;
        String issuer = (config.getIssuer() != null && !config.getIssuer().isBlank())
                ? config.getIssuer()
                : defaultIssuer;
        String waitingRoomUrl = (config.getWaitingRoomUrl() != null && !config.getWaitingRoomUrl().isBlank())
                ? config.getWaitingRoomUrl()
                : defaultWaitingRoomUrl;

        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));

        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            String token = request.getHeaders().getFirst(HEADER_CHECKOUT_TOKEN);

            if (token == null || token.isBlank()) {
                log.warn("[WAITING-ROOM] Missing {} header for URI: {}", HEADER_CHECKOUT_TOKEN, request.getURI().getPath());
                return rejectWithProblemDetail(exchange, "Missing waiting room checkout token", waitingRoomUrl);
            }

            try {
                Claims claims = Jwts.parser()
                        .verifyWith(key)
                        .requireIssuer(issuer)
                        .build()
                        .parseSignedClaims(token)
                        .getPayload();

                String userId = claims.getSubject();
                log.debug("[WAITING-ROOM] Successfully validated checkout token for user: {}", userId);

                ServerHttpRequest mutatedRequest = request.mutate()
                        .headers(h -> {
                            h.set(HEADER_USER_ID, userId != null ? userId : "anonymous");
                            h.set(HEADER_WAITING_ROOM_VALIDATED, "true");
                        })
                        .build();

                return chain.filter(exchange.mutate().request(mutatedRequest).build());

            } catch (ExpiredJwtException e) {
                log.warn("[WAITING-ROOM] Expired checkout token: {}", e.getMessage());
                return rejectWithProblemDetail(exchange, "Checkout token has expired. Please re-enter the queue.", waitingRoomUrl);
            } catch (JwtException | IllegalArgumentException e) {
                log.warn("[WAITING-ROOM] Invalid checkout token: {}", e.getMessage());
                return rejectWithProblemDetail(exchange, "Invalid checkout token signature or format.", waitingRoomUrl);
            }
        };
    }

    private Mono<Void> rejectWithProblemDetail(ServerWebExchange exchange, String detail, String waitingRoomUrl) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.FORBIDDEN);
        response.getHeaders().setContentType(MediaType.APPLICATION_PROBLEM_JSON);

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, detail);
        problem.setType(URI.create("https://amaryllis-tickets.com/errors/waiting-room-required"));
        problem.setTitle("Virtual Waiting Room Gating");
        problem.setInstance(URI.create(exchange.getRequest().getURI().getPath()));
        problem.setProperty("waitingRoomUrl", waitingRoomUrl);
        problem.setProperty("timestamp", Instant.now().toString());

        byte[] bytes;
        try {
            bytes = objectMapper.writeValueAsBytes(problem);
        } catch (JsonProcessingException e) {
            bytes = ("{\"type\":\"https://amaryllis-tickets.com/errors/waiting-room-required\","
                    + "\"status\":403,\"title\":\"Virtual Waiting Room Gating\",\"detail\":\"" + detail + "\"}").getBytes(StandardCharsets.UTF_8);
        }

        DataBuffer buffer = response.bufferFactory().wrap(bytes);
        return response.writeWith(Mono.just(buffer));
    }

    public static class Config {
        private String secretKey;
        private String issuer = "amaryllis-waiting-room";
        private String waitingRoomUrl;

        public String getSecretKey() {
            return secretKey;
        }

        public void setSecretKey(String secretKey) {
            this.secretKey = secretKey;
        }

        public String getIssuer() {
            return issuer;
        }

        public void setIssuer(String issuer) {
            this.issuer = issuer;
        }

        public String getWaitingRoomUrl() {
            return waitingRoomUrl;
        }

        public void setWaitingRoomUrl(String waitingRoomUrl) {
            this.waitingRoomUrl = waitingRoomUrl;
        }
    }
}
