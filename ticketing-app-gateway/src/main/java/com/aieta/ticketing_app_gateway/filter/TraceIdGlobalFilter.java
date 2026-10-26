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
import reactor.util.context.Context;

import java.util.UUID;

@Component
public class TraceIdGlobalFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(TraceIdGlobalFilter.class);
    public static final String TRACE_ID_HEADER = "Trace-ID";
    public static final String X_TRACE_ID_HEADER = "X-Trace-Id";
    public static final String CONTEXT_TRACE_ID = "traceId";

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 1;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();

        String traceId = request.getHeaders().getFirst(TRACE_ID_HEADER);
        if (traceId == null || traceId.isBlank()) {
            traceId = request.getHeaders().getFirst(X_TRACE_ID_HEADER);
        }
        if (traceId == null || traceId.isBlank()) {
            traceId = UUID.randomUUID().toString();
        }

        final String finalTraceId = traceId;

        ServerHttpRequest mutatedRequest = request.mutate()
                .headers(h -> {
                    h.set(TRACE_ID_HEADER, finalTraceId);
                    h.set(X_TRACE_ID_HEADER, finalTraceId);
                })
                .build();

        exchange.getResponse().beforeCommit(() -> {
            exchange.getResponse().getHeaders().set(TRACE_ID_HEADER, finalTraceId);
            exchange.getResponse().getHeaders().set(X_TRACE_ID_HEADER, finalTraceId);
            return Mono.empty();
        });

        return chain.filter(exchange.mutate().request(mutatedRequest).build())
                .contextWrite(Context.of(CONTEXT_TRACE_ID, finalTraceId));
    }
}
