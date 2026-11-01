package com.aieta.ticketing_app_gateway.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;

@Configuration
public class RateLimiterConfig {

    private static final Logger log = LoggerFactory.getLogger(RateLimiterConfig.class);

    @Bean
    @Primary
    public KeyResolver ipKeyResolver() {
        return exchange -> {
            // 1. Check X-Forwarded-For (take the first non-blank client IP candidate)
            String xForwardedFor = exchange.getRequest().getHeaders().getFirst("X-Forwarded-For");
            if (xForwardedFor != null && !xForwardedFor.isBlank()) {
                String[] parts = xForwardedFor.split(",");
                for (String part : parts) {
                    String candidate = part.trim();
                    if (!candidate.isBlank()) {
                        log.debug("[RATE-LIMIT] Resolved client key from X-Forwarded-For: {}", candidate);
                        return Mono.just(candidate);
                    }
                }
            }

            // 2. Check X-Real-IP
            String xRealIp = exchange.getRequest().getHeaders().getFirst("X-Real-IP");
            if (xRealIp != null && !xRealIp.isBlank()) {
                String candidate = xRealIp.trim();
                log.debug("[RATE-LIMIT] Resolved client key from X-Real-IP: {}", candidate);
                return Mono.just(candidate);
            }

            // 3. Fallback to TCP remote socket address
            InetSocketAddress remoteAddress = exchange.getRequest().getRemoteAddress();
            if (remoteAddress != null && remoteAddress.getAddress() != null) {
                String candidate = remoteAddress.getAddress().getHostAddress();
                log.debug("[RATE-LIMIT] Resolved client key from RemoteAddress: {}", candidate);
                return Mono.just(candidate);
            }

            // 4. Default fallback
            log.warn("[RATE-LIMIT] Falling back to default anonymous client key");
            return Mono.just("anonymous-client");
        };
    }
}
