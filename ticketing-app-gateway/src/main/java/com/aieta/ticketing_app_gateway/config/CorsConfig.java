package com.aieta.ticketing_app_gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
public class CorsConfig {

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public CorsWebFilter corsWebFilter() {
        CorsConfiguration config = new CorsConfiguration();

        // Allowed Origins: B2C (5173 / www) and B2B (5174 / admin)
        config.setAllowedOriginPatterns(List.of(
                "http://localhost:5173",
                "https://www.hibana.tk",
                "http://localhost:5174",
                "https://admin.hibana.tk",
                "http://127.0.0.1:5173",
                "http://127.0.0.1:5174"
        ));

        // Allowed Methods
        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));

        // Allowed Headers
        config.setAllowedHeaders(Arrays.asList(
                "Origin",
                "Content-Type",
                "Accept",
                "Authorization",
                "X-Checkout-Token",
                "Trace-ID",
                "X-Trace-Id",
                "X-Requested-With"
        ));

        // Exposed Headers
        config.setExposedHeaders(Arrays.asList(
                "Trace-ID",
                "X-Trace-Id",
                "X-Checkout-Token",
                "Location",
                "Content-Disposition"
        ));

        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return new CorsWebFilter(source);
    }
}
