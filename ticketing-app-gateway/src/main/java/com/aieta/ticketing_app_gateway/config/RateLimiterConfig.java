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
                        return Mono.just(candidate);
                    }
                }
            }

            // 2. Check X-Real-IP
            String xRealIp = exchange.getRequest().getHeaders().getFirst("X-Real-IP");
            if (xRealIp != null && !xRealIp.isBlank()) {
                return Mono.just(xRealIp.trim());
            }

            // 3. Fallback to TCP remote socket address
            InetSocketAddress remoteAddress = exchange.getRequest().getRemoteAddress();
            if (remoteAddress != null && remoteAddress.getAddress() != null) {
                return Mono.just(remoteAddress.getAddress().getHostAddress());
            }

            // 4. Default fallback
            return Mono.just("anonymous-client");
        };
    }
}
