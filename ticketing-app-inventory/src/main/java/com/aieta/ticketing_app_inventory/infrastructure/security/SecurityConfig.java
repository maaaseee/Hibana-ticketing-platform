package com.aieta.ticketing_app_inventory.infrastructure.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final EntraJwtAuthenticationConverter jwtAuthenticationConverter;

    public SecurityConfig(@Autowired(required = false) EntraJwtAuthenticationConverter jwtAuthenticationConverter) {
        this.jwtAuthenticationConverter = jwtAuthenticationConverter != null
                ? jwtAuthenticationConverter
                : new EntraJwtAuthenticationConverter();
    }

    @Bean
    @ConditionalOnMissingBean
    public JwtDecoder jwtDecoder(
            @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri:http://localhost:4577/00000000-0000-0000-0000-000000000002/discovery/v2.0/keys}") String jwkSetUri) {
        return NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Public seat browsing
                        .requestMatchers(HttpMethod.GET, "/api/v1/events/*/seats/**", "/api/v1/events/**").permitAll()
                        .requestMatchers("/actuator/**", "/error", "/swagger-ui/**", "/v3/api-docs/**").permitAll()

                        // Seat reservations (guarded by Gateway Checkout Token Filter)
                        .requestMatchers(HttpMethod.POST, "/api/v1/reservations/**").permitAll()
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/reservations/**").permitAll()

                        // Administrative seat generation / management
                        .requestMatchers(HttpMethod.POST, "/api/v1/admin/**", "/api/v1/seats/bulk/**").hasRole("ADMIN")

                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter))
                )
                .addFilterBefore(new GatewayHeaderAuthFilter(), BearerTokenAuthenticationFilter.class);

        return http.build();
    }
}
