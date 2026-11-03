package com.aieta.ticketing_app_gateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * High-precedence security filter that strips untrusted identity and internal security headers
 * sent by external clients before any route processing or downstream forwarding occurs.
 *
 * Enforces SEC-01 perimeter hygiene by ensuring that external callers cannot spoof user identities,
 * roles, or internal gateway authentication stamps.
 */
@Component
public class StripInboundSecurityHeadersFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(StripInboundSecurityHeadersFilter.class);

    public static final List<String> FORBIDDEN_INBOUND_HEADERS = List.of(
            "X-User-Id",
            "X-User-Roles",
            "X-Roles",
            "X-User-Email",
            "X-Gateway-Auth-Stamp",
            "X-Gateway-Signature",
            "X-Gateway-Stamp"
    );

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();

        boolean strippedAny = false;
        for (String headerName : FORBIDDEN_INBOUND_HEADERS) {
            String value = request.getHeaders().getFirst(headerName);
            if (value != null) {
                strippedAny = true;
                log.warn("[SECURITY] Blocked inbound untrusted header '{}' with value '{}' from {} {}",
                        headerName, value,
                        request.getMethod(), request.getURI().getPath());
            }
        }

        ServerHttpRequest mutatedRequest = request.mutate()
                .headers(httpHeaders -> {
                    for (String headerName : FORBIDDEN_INBOUND_HEADERS) {
                        httpHeaders.remove(headerName);
                    }
                })
                .build();

        return chain.filter(exchange.mutate().request(mutatedRequest).build());
    }
}
