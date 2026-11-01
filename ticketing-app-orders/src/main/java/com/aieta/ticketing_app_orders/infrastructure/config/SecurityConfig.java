package com.aieta.ticketing_app_orders.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .headers(headers -> headers
                        .frameOptions(HeadersConfigurer.FrameOptionsConfig::disable)
                )
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/**").permitAll()
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        .requestMatchers("/api/v1/orders/preview/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/orders/**").hasRole("USER")
                        .requestMatchers(HttpMethod.GET, "/api/v1/orders/**").hasRole("USER")
                        .requestMatchers(HttpMethod.POST, "/api/v1/payments/**").hasRole("USER")
                        .requestMatchers(HttpMethod.GET, "/api/v1/wallets/**").hasRole("USER")
                        .requestMatchers(HttpMethod.POST, "/api/v1/wallets/**").hasRole("USER")
                        .anyRequest().authenticated()
                )
                .addFilterBefore(new XUserIdAuthFilter(), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
