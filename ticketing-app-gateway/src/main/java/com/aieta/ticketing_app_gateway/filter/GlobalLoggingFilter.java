package com.aieta.ticketing_app_gateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class GlobalLoggingFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(GlobalLoggingFilter.class);

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE - 1;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        long startTime = System.currentTimeMillis();
        String method = exchange.getRequest().getMethod().name();
        String path = exchange.getRequest().getURI().getRawPath();
        String query = exchange.getRequest().getURI().getRawQuery();
        String fullPath = query != null ? path + "?" + query : path;

        return chain.filter(exchange)
                .then(Mono.<Void>fromRunnable(() -> {
                    long latency = System.currentTimeMillis() - startTime;
                    HttpStatusCode status = exchange.getResponse().getStatusCode();
                    int statusCode = status != null ? status.value() : 200;
                    String traceId = exchange.getResponse().getHeaders().getFirst(TraceIdGlobalFilter.TRACE_ID_HEADER);

                    log.info("[GATEWAY] [Trace-ID: {}] {} {} -> {} ({}ms)",
                            traceId != null ? traceId : "none", method, fullPath, statusCode, latency);
                }))
                .doOnError(throwable -> {
                    long latency = System.currentTimeMillis() - startTime;
                    String traceId = exchange.getResponse().getHeaders().getFirst(TraceIdGlobalFilter.TRACE_ID_HEADER);
                    log.error("[GATEWAY] [Trace-ID: {}] {} {} -> ERROR: {} ({}ms)",
                            traceId != null ? traceId : "none", method, fullPath, throwable.getMessage(), latency);
                });
    }
}
