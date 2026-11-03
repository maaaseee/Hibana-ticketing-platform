package com.aieta.ticketing_app_inventory.infrastructure.security;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Converts Microsoft Entra ID / Floci-AZ JWTs into Spring Security authentication tokens for Inventory.
 */
@Component
public class EntraJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    public static final String SEED_ADMIN_SUB = "1c5ca4ea-7651-3d75-b981-cd6e66a12a68";
    public static final String SEED_OPERATOR_SUB = "usr-operator-01";

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        Set<GrantedAuthority> authorities = new HashSet<>();

        // 1. Roles claim
        Object rawRoles = jwt.getClaims().get("roles");
        if (rawRoles instanceof List<?> list) {
            for (Object item : list) {
                if (item != null) {
                    authorities.add(normalizeRole(item.toString()));
                }
            }
        } else if (rawRoles instanceof String str) {
            Arrays.stream(str.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .forEach(r -> authorities.add(normalizeRole(r)));
        }

        // 2. Groups claim
        Object rawGroups = jwt.getClaims().get("groups");
        if (rawGroups instanceof List<?> list) {
            for (Object item : list) {
                if (item != null) {
                    authorities.add(normalizeRole(item.toString()));
                }
            }
        }

        // 3. Fallback for seeded dev accounts in Floci-AZ emulation
        String sub = jwt.getSubject();
        String preferredUsername = jwt.getClaimAsString("preferred_username");
        String upn = jwt.getClaimAsString("upn");
        String email = jwt.getClaimAsString("email");

        if (SEED_ADMIN_SUB.equals(sub)
                || "alex.thorne@hibana.tk".equalsIgnoreCase(preferredUsername)
                || "alex.thorne@hibana.tk".equalsIgnoreCase(upn)
                || "alex.thorne@hibana.tk".equalsIgnoreCase(email)) {
            authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
            authorities.add(new SimpleGrantedAuthority("ROLE_OPERATOR"));
        } else if (SEED_OPERATOR_SUB.equals(sub)
                || "operator@hibana.tk".equalsIgnoreCase(preferredUsername)
                || "operator@hibana.tk".equalsIgnoreCase(upn)
                || "operator@hibana.tk".equalsIgnoreCase(email)) {
            authorities.add(new SimpleGrantedAuthority("ROLE_OPERATOR"));
        }

        if (authorities.isEmpty()) {
            authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
        }

        String principalName = sub != null ? sub : (preferredUsername != null ? preferredUsername : "anonymous");
        return new JwtAuthenticationToken(jwt, authorities, principalName);
    }

    private GrantedAuthority normalizeRole(String role) {
        String clean = role.trim();
        if (!clean.startsWith("ROLE_")) {
            clean = "ROLE_" + clean.toUpperCase(Locale.ROOT);
        }
        return new SimpleGrantedAuthority(clean);
    }
}
