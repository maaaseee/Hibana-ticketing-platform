package com.aieta.ticketing_app_catalog.infrastructure.security;

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
                        // Public read endpoints (both versioned and direct paths)
                        .requestMatchers(HttpMethod.GET, "/events/**", "/venues/**", "/api/v1/events/**", "/api/v1/venues/**").permitAll()
                        .requestMatchers("/actuator/**", "/error", "/swagger-ui/**", "/v3/api-docs/**").permitAll()

                        // Administrative mutations
                        .requestMatchers(HttpMethod.POST, "/events/**", "/venues/**", "/api/v1/events/**", "/api/v1/venues/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/events/**", "/venues/**", "/api/v1/events/**", "/api/v1/venues/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/events/**", "/venues/**", "/api/v1/events/**", "/api/v1/venues/**").hasRole("ADMIN")

                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter))
                )
                .addFilterBefore(new GatewayHeaderAuthFilter(), BearerTokenAuthenticationFilter.class);

        return http.build();
    }
}
