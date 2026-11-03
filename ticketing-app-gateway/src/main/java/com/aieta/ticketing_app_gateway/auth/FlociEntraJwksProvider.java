package com.aieta.ticketing_app_gateway.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigInteger;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Connects to Microsoft Entra ID (or the local Floci-AZ Entra emulator)
 * to retrieve and cache the JSON Web Key Set (JWKS) public keys for RS256 token verification.
 */
@Component
public class FlociEntraJwksProvider {

    private static final Logger log = LoggerFactory.getLogger(FlociEntraJwksProvider.class);

    private final String jwksUrl;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final Map<String, PublicKey> keyCache = new ConcurrentHashMap<>();

    // Known seeded identities in local dev environment (infra/local-dev/entra-seed.json)
    public static final String SEED_ADMIN_SUB = "1c5ca4ea-7651-3d75-b981-cd6e66a12a68"; // alex.thorne@hibana.tk
    public static final String SEED_OPERATOR_SUB = "usr-operator-01";

    public static final String DEFAULT_TENANT_ID = "00000000-0000-0000-0000-000000000002";
    public static final String DEFAULT_JWKS_URL = "http://localhost:4577/" + DEFAULT_TENANT_ID + "/discovery/v2.0/keys";

    @Value("${spring.profiles.active:}")
    private String activeProfiles;

    public FlociEntraJwksProvider(
            @Value("${azure.entra.jwks-url:${AZURE_ENTRA_JWKS_URL:" + DEFAULT_JWKS_URL + "}}") String jwksUrl,
            ObjectMapper objectMapper) {
        this.jwksUrl = jwksUrl;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .build();
    }

    /**
     * Resolves an RSA PublicKey by its Key ID (kid) from cache, refreshing from the JWKS endpoint if missing.
     * Enforces strict key ID resolution with NO permissive fallback to arbitrary cached keys.
     */
    public PublicKey getPublicKey(String kid) {
        if (kid == null || kid.isBlank()) {
            throw new IllegalArgumentException("Key ID (kid) must not be null or blank");
        }

        if (keyCache.containsKey(kid)) {
            return keyCache.get(kid);
        }

        refreshKeys();

        if (keyCache.containsKey(kid)) {
            return keyCache.get(kid);
        }

        throw new IllegalStateException("Unable to resolve RSA public key for kid: " + kid + " from JWKS: " + jwksUrl);
    }

    /**
     * Manually registers a key for testing or deterministic mocking.
     */
    public void registerKey(String kid, PublicKey publicKey) {
        keyCache.put(kid, publicKey);
    }

    /**
     * Fetches and parses the JWKS JSON document from Floci-AZ / Entra ID.
     */
    public synchronized void refreshKeys() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(jwksUrl))
                    .timeout(Duration.ofSeconds(3))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                parseAndStoreJwks(response.body());
                log.info("Successfully refreshed Entra ID JWKS from {}. Cached {} key(s).", jwksUrl, keyCache.size());
            } else {
                log.warn("Failed to fetch Entra ID JWKS from {}. HTTP status: {}", jwksUrl, response.statusCode());
            }
        } catch (Exception e) {
            log.warn("Could not reach Entra ID JWKS endpoint at {}: {}", jwksUrl, e.getMessage());
        }
    }

    public void parseAndStoreJwks(String json) {
        try {
            JsonNode root = objectMapper.readTree(json);
            JsonNode keys = root.get("keys");
            if (keys != null && keys.isArray()) {
                KeyFactory keyFactory = KeyFactory.getInstance("RSA");
                for (JsonNode keyNode : keys) {
                    String kty = keyNode.path("kty").asText();
                    if ("RSA".equalsIgnoreCase(kty)) {
                        String kid = keyNode.path("kid").asText();
                        String nStr = keyNode.path("n").asText();
                        String eStr = keyNode.path("e").asText();

                        if (!nStr.isEmpty() && !eStr.isEmpty()) {
                            byte[] modulusBytes = Base64.getUrlDecoder().decode(nStr);
                            byte[] exponentBytes = Base64.getUrlDecoder().decode(eStr);

                            BigInteger modulus = new BigInteger(1, modulusBytes);
                            BigInteger exponent = new BigInteger(1, exponentBytes);

                            PublicKey publicKey = keyFactory.generatePublic(new RSAPublicKeySpec(modulus, exponent));
                            keyCache.put(kid, publicKey);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error parsing JWKS JSON: {}", e.getMessage(), e);
        }
    }

    /**
     * Resolves the user roles from token claims.
     * Checks:
     * 1. Standard Entra ID 'roles' claim (App Roles)
     * 2. 'groups' claim (Security Groups)
     * 3. Seeded user identifiers in local dev (alex.thorne -> ROLE_ADMIN, ROLE_OPERATOR)
     */
    public List<String> resolveRoles(Claims claims) {
        Set<String> roles = new LinkedHashSet<>();

        // 1. Direct 'roles' claim
        Object rawRoles = claims.get("roles");
        if (rawRoles instanceof List<?> list) {
            list.forEach(r -> roles.add(r.toString()));
        } else if (rawRoles instanceof String s) {
            Arrays.stream(s.split(",")).map(String::trim).filter(r -> !r.isEmpty()).forEach(roles::add);
        }

        // 2. Direct 'groups' claim
        Object rawGroups = claims.get("groups");
        if (rawGroups instanceof List<?> list) {
            list.forEach(g -> roles.add(g.toString()));
        }

        // 3. Fallback to seeded directory users for local Floci-AZ emulation (disabled in production)
        if (!isProductionProfile()) {
            String sub = claims.getSubject();
            String preferredUsername = claims.get("preferred_username", String.class);
            String upn = claims.get("upn", String.class);

            if (SEED_ADMIN_SUB.equals(sub)
                    || "alex.thorne@hibana.tk".equalsIgnoreCase(preferredUsername)
                    || "alex.thorne@hibana.tk".equalsIgnoreCase(upn)) {
                roles.add("ROLE_ADMIN");
                roles.add("ROLE_OPERATOR");
            } else if (SEED_OPERATOR_SUB.equals(sub)
                    || "operator@hibana.tk".equalsIgnoreCase(preferredUsername)
                    || "operator@hibana.tk".equalsIgnoreCase(upn)) {
                roles.add("ROLE_OPERATOR");
            }
        }

        return new ArrayList<>(roles);
    }

    private boolean isProductionProfile() {
        return activeProfiles != null && (activeProfiles.contains("prod") || activeProfiles.contains("production"));
    }
}
