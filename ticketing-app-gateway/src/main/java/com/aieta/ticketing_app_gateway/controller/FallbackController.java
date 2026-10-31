package com.aieta.ticketing_app_gateway.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;

import java.net.URI;
import java.time.Instant;

@RestController
@RequestMapping("/fallback")
public class FallbackController {

    private static final Logger log = LoggerFactory.getLogger(FallbackController.class);

    @RequestMapping(value = "/{service}", produces = MediaType.APPLICATION_PROBLEM_JSON_VALUE)
    public ResponseEntity<ProblemDetail> handleFallback(
            @PathVariable("service") String service,
            ServerWebExchange exchange
    ) {
        String path = exchange.getRequest().getURI().getPath();
        log.warn("[CIRCUIT-BREAKER] Tripped fallback for service: {} on path: {}", service, path);

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.SERVICE_UNAVAILABLE,
                "Downstream service '" + service + "' is currently degraded or unreachable. Please retry shortly."
        );
        problem.setType(URI.create("https://hibana.tk/errors/service-unavailable"));
        problem.setTitle("Service Degraded");
        problem.setInstance(URI.create(path));
        problem.setProperty("service", service);
        problem.setProperty("timestamp", Instant.now().toString());

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(problem);
    }
}
