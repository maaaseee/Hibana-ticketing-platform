package com.aieta.ticketing_app_catalog.infrastructure.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.*;

/**
 * Filter that authenticates requests arriving from Spring Cloud Gateway.
 * Enforces cryptographic Gateway Stamp validation so that headers X-User-Id / X-User-Roles
 * are only trusted if genuinely signed by the Gateway with the shared secret.
 */
public class GatewayHeaderAuthFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(GatewayHeaderAuthFilter.class);

    private final GatewayStampValidator stampValidator;

    public GatewayHeaderAuthFilter() {
        this(new GatewayStampValidator("gateway-internal-hmac-secret-2026-minimum-256-bits!"));
    }

    public GatewayHeaderAuthFilter(GatewayStampValidator stampValidator) {
        this.stampValidator = stampValidator != null
                ? stampValidator
                : new GatewayStampValidator("gateway-internal-hmac-secret-2026-minimum-256-bits!");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            String userId = request.getHeader("X-User-Id");

            if (userId != null && !userId.isBlank()) {
                if (stampValidator.validateRequest(request)) {
                    Set<GrantedAuthority> authorities = new HashSet<>();
                    authorities.add(new SimpleGrantedAuthority("ROLE_USER"));

                    String rolesHeader = request.getHeader("X-User-Roles");
                    if (rolesHeader == null || rolesHeader.isBlank()) {
                        rolesHeader = request.getHeader("X-Roles");
                    }

                    if (rolesHeader != null && !rolesHeader.isBlank()) {
                        for (String role : rolesHeader.split(",")) {
                            String clean = role.trim();
                            if (!clean.isEmpty()) {
                                if (!clean.startsWith("ROLE_")) {
                                    clean = "ROLE_" + clean.toUpperCase(Locale.ROOT);
                                }
                                authorities.add(new SimpleGrantedAuthority(clean));
                            }
                        }
                    }

                    UsernamePasswordAuthenticationToken auth =
                            new UsernamePasswordAuthenticationToken(userId, null, authorities);
                    SecurityContextHolder.getContext().setAuthentication(auth);
                    log.debug("[SECURITY] Authenticated user '{}' with roles {} via verified Gateway Stamp", userId, authorities);
                } else {
                    log.warn("[SECURITY] Blocked untrusted identity header X-User-Id='{}' without valid Gateway Stamp on {} {}",
                            userId, request.getMethod(), request.getRequestURI());
                }
            }
        }

        filterChain.doFilter(request, response);
    }
}
