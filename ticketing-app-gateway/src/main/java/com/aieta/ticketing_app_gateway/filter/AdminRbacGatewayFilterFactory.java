package com.aieta.ticketing_app_gateway.filter;

import com.aieta.ticketing_app_gateway.auth.FlociEntraJwksProvider;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.PublicKey;
import java.time.Instant;
import java.util.*;

/**
 * Gateway filter that enforces Role-Based Access Control (RBAC) on administrative endpoints.
 * Validates Microsoft Entra ID / Floci-AZ RS256 signed JWTs using public keys from the JWKS endpoint.
 * Requires caller to have at least one role matching requiredRoles (default: ROLE_ADMIN, ROLE_OPERATOR).
 */
@Component
public class AdminRbacGatewayFilterFactory extends AbstractGatewayFilterFactory<AdminRbacGatewayFilterFactory.Config> {

    private static final Logger log = LoggerFactory.getLogger(AdminRbacGatewayFilterFactory.class);

    private final FlociEntraJwksProvider jwksProvider;
    private final ObjectMapper objectMapper;

    public static final String DEFAULT_ISSUER = "http://localhost:4577/00000000-0000-0000-0000-000000000002/v2.0";
    public static final String DEFAULT_AUDIENCE = "api://hibana-api";

    @org.springframework.beans.factory.annotation.Value("${azure.entra.issuer:${AZURE_ENTRA_ISSUER:" + DEFAULT_ISSUER + "}}")
    private String defaultIssuer = DEFAULT_ISSUER;

    @org.springframework.beans.factory.annotation.Value("${azure.entra.audience:${AZURE_ENTRA_AUDIENCE:" + DEFAULT_AUDIENCE + "}}")
    private String defaultAudience = DEFAULT_AUDIENCE;

    public static final String DEFAULT_HMAC_SECRET = "gateway-internal-hmac-secret-2026-minimum-256-bits!";

    @org.springframework.beans.factory.annotation.Value("${gateway.hmac.secret:${GATEWAY_HMAC_SECRET:" + DEFAULT_HMAC_SECRET + "}}")
    private String gatewayHmacSecret = DEFAULT_HMAC_SECRET;

    private String computeHmacSha256(String data, String secret) {
        try {
            String effectiveSecret = (secret != null && !secret.isBlank()) ? secret : DEFAULT_HMAC_SECRET;
            javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
            javax.crypto.spec.SecretKeySpec spec = new javax.crypto.spec.SecretKeySpec(effectiveSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(spec);
            byte[] raw = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(raw);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to calculate HMAC-SHA256 signature", e);
        }
    }

    @org.springframework.beans.factory.annotation.Autowired
    public AdminRbacGatewayFilterFactory(FlociEntraJwksProvider jwksProvider, ObjectMapper objectMapper) {
        this(jwksProvider, objectMapper, DEFAULT_ISSUER, DEFAULT_AUDIENCE, DEFAULT_HMAC_SECRET);
    }

    public AdminRbacGatewayFilterFactory(
            FlociEntraJwksProvider jwksProvider,
            ObjectMapper objectMapper,
            String defaultIssuer,
            String defaultAudience) {
        this(jwksProvider, objectMapper, defaultIssuer, defaultAudience, DEFAULT_HMAC_SECRET);
    }

    public AdminRbacGatewayFilterFactory(
            FlociEntraJwksProvider jwksProvider,
            ObjectMapper objectMapper,
            String defaultIssuer,
            String defaultAudience,
            String gatewayHmacSecret) {
        super(Config.class);
        this.jwksProvider = jwksProvider;
        this.objectMapper = objectMapper;
        this.defaultIssuer = defaultIssuer != null ? defaultIssuer : DEFAULT_ISSUER;
        this.defaultAudience = defaultAudience != null ? defaultAudience : DEFAULT_AUDIENCE;
        this.gatewayHmacSecret = (gatewayHmacSecret != null && !gatewayHmacSecret.isBlank()) ? gatewayHmacSecret : DEFAULT_HMAC_SECRET;
    }

    @Override
    public String name() {
        return "AdminRbac";
    }

    @Override
    public List<String> shortcutFieldOrder() {
        return List.of("requiredRoles", "issuer", "audience");
    }

    @Override
    public GatewayFilter apply(Config config) {
        List<String> effectiveRequiredRoles = (config.getRequiredRoles() != null && !config.getRequiredRoles().isEmpty())
                ? config.getRequiredRoles()
                : List.of("ROLE_ADMIN", "ROLE_OPERATOR");

        String effectiveIssuer = (config.getIssuer() != null && !config.getIssuer().isBlank())
                ? config.getIssuer()
                : defaultIssuer;
        String effectiveAudience = (config.getAudience() != null && !config.getAudience().isBlank())
                ? config.getAudience()
                : defaultAudience;

        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

            if (authHeader == null || !authHeader.regionMatches(true, 0, "Bearer ", 0, 7)) {
                log.warn("Admin RBAC rejection: Missing or invalid Authorization header on {}", request.getPath());
                return rejectWithProblemDetail(
                        exchange,
                        HttpStatus.UNAUTHORIZED,
                        "https://hibana.tk/errors/unauthorized",
                        "Authentication Required",
                        "Missing or malformed Authorization header. Expected 'Bearer <token>'."
                );
            }

            String token = authHeader.substring(7).trim();
            String kid = extractKeyId(token);

            PublicKey publicKey;
            try {
                publicKey = jwksProvider.getPublicKey(kid);
            } catch (Exception e) {
                log.error("Admin RBAC error: Failed to retrieve public key for kid {}: {}", kid, e.getMessage());
                return rejectWithProblemDetail(
                        exchange,
                        HttpStatus.UNAUTHORIZED,
                        "https://hibana.tk/errors/invalid-token",
                        "Invalid Key ID",
                        "Unable to verify token: unknown or unavailable signing key."
                );
            }

            Claims claims;
            try {
                var parser = Jwts.parser()
                        .verifyWith(publicKey);

                if (effectiveIssuer != null && !effectiveIssuer.isBlank()) {
                    parser.requireIssuer(effectiveIssuer);
                }
                if (effectiveAudience != null && !effectiveAudience.isBlank()) {
                    parser.requireAudience(effectiveAudience);
                }

                claims = parser.build()
                        .parseSignedClaims(token)
                        .getPayload();
            } catch (ExpiredJwtException e) {
                log.warn("Admin RBAC rejection: Expired Entra ID token on {}", request.getPath());
                return rejectWithProblemDetail(
                        exchange,
                        HttpStatus.UNAUTHORIZED,
                        "https://hibana.tk/errors/token-expired",
                        "Token Expired",
                        "The supplied Entra ID authorization token has expired."
                );
            } catch (io.jsonwebtoken.IncorrectClaimException e) {
                log.warn("Admin RBAC rejection: Incorrect claim on {}: {}", request.getPath(), e.getMessage());
                return rejectWithProblemDetail(
                        exchange,
                        HttpStatus.UNAUTHORIZED,
                        "https://hibana.tk/errors/invalid-token",
                        "Invalid Token Claim",
                        "Token claim validation failed: " + e.getMessage()
                );
            } catch (io.jsonwebtoken.MissingClaimException e) {
                log.warn("Admin RBAC rejection: Missing claim on {}: {}", request.getPath(), e.getMessage());
                return rejectWithProblemDetail(
                        exchange,
                        HttpStatus.UNAUTHORIZED,
                        "https://hibana.tk/errors/invalid-token",
                        "Missing Required Claim",
                        "Token claim validation failed: " + e.getMessage()
                );
            } catch (JwtException e) {
                log.warn("Admin RBAC rejection: Signature verification failed on {}: {}", request.getPath(), e.getMessage());
                return rejectWithProblemDetail(
                        exchange,
                        HttpStatus.UNAUTHORIZED,
                        "https://hibana.tk/errors/invalid-token",
                        "Invalid Token",
                        "Cryptographic signature verification failed: " + e.getMessage()
                );
            }

            List<String> userRoles = jwksProvider.resolveRoles(claims);
            boolean hasPermission = userRoles.stream().anyMatch(effectiveRequiredRoles::contains);

            if (!hasPermission) {
                log.warn("Admin RBAC rejection: Insufficient permissions on {}. User roles: {}, Required: {}",
                        request.getPath(), userRoles, effectiveRequiredRoles);
                return rejectWithProblemDetail(
                        exchange,
                        HttpStatus.FORBIDDEN,
                        "https://hibana.tk/errors/insufficient-permissions",
                        "Insufficient Permissions",
                        "User does not possess any of the required administrative roles: " + effectiveRequiredRoles
                );
            }

            String userId = claims.getSubject() != null ? claims.getSubject() : (String) claims.get("oid");
            String email = claims.get("preferred_username", String.class);
            if (email == null) {
                email = claims.get("email", String.class);
            }
            if (email == null) {
                email = claims.get("upn", String.class);
            }

            String rolesStr = String.join(",", userRoles);
            long timestamp = System.currentTimeMillis();
            String signature = computeHmacSha256((userId != null ? userId : "") + ":" + rolesStr + ":" + timestamp, gatewayHmacSecret);

            ServerHttpRequest mutatedRequest = request.mutate()
                    .header("X-User-Id", userId != null ? userId : "")
                    .header("X-User-Roles", rolesStr)
                    .header("X-User-Email", email != null ? email : "")
                    .header("X-Gateway-Auth-Stamp", Instant.now().toString())
                    .header("X-Gateway-Timestamp", String.valueOf(timestamp))
                    .header("X-Gateway-Signature", signature)
                    .header("X-Gateway-Stamp", signature)
                    .build();

            return chain.filter(exchange.mutate().request(mutatedRequest).build());
        };
    }

    private String extractKeyId(String token) {
        try {
            int firstDot = token.indexOf('.');
            if (firstDot > 0) {
                String headerJson = new String(Base64.getUrlDecoder().decode(token.substring(0, firstDot)), StandardCharsets.UTF_8);
                JsonNode node = objectMapper.readTree(headerJson);
                return node.path("kid").asText(null);
            }
        } catch (Exception e) {
            log.debug("Could not parse kid from JWT header: {}", e.getMessage());
        }
        return null;
    }

    private Mono<Void> rejectWithProblemDetail(
            ServerWebExchange exchange,
            HttpStatus status,
            String typeUri,
            String title,
            String detail) {

        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_PROBLEM_JSON);

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setType(URI.create(typeUri));
        problem.setTitle(title);
        problem.setInstance(URI.create(exchange.getRequest().getURI().getPath()));
        problem.setProperty("timestamp", Instant.now().toString());

        byte[] bytes;
        try {
            bytes = objectMapper.writeValueAsBytes(problem);
        } catch (JsonProcessingException e) {
            bytes = ("{\"type\":\"" + typeUri + "\",\"status\":" + status.value() + ",\"title\":\"" + title + "\",\"detail\":\"" + detail + "\"}").getBytes(StandardCharsets.UTF_8);
        }

        DataBuffer buffer = response.bufferFactory().wrap(bytes);
        return response.writeWith(Mono.just(buffer));
    }

    public static class Config {
        private List<String> requiredRoles = new ArrayList<>(List.of("ROLE_ADMIN", "ROLE_OPERATOR"));
        private String issuer;
        private String audience;

        public List<String> getRequiredRoles() {
            return requiredRoles;
        }

        public void setRequiredRoles(List<String> requiredRoles) {
            this.requiredRoles = requiredRoles;
        }

        public String getIssuer() {
            return issuer;
        }

        public void setIssuer(String issuer) {
            this.issuer = issuer;
        }

        public String getAudience() {
            return audience;
        }

        public void setAudience(String audience) {
            this.audience = audience;
        }
    }
}
