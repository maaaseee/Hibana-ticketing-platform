package com.aieta.ticketing_app_gateway;

import com.aieta.ticketing_app_gateway.auth.FlociEntraJwksProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PublicKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Unit Test: FlociEntraJwksProvider Key Parsing & Strict Role Resolution")
class FlociEntraJwksProviderTest {

    private FlociEntraJwksProvider provider;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        provider = new FlociEntraJwksProvider(FlociEntraJwksProvider.DEFAULT_JWKS_URL, objectMapper);
    }

    @Test
    @DisplayName("parseAndStoreJwks parses valid JWKS and populates public key cache")
    void shouldParseAndStoreJwks() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair pair = generator.generateKeyPair();
        RSAPublicKey pub = (RSAPublicKey) pair.getPublic();

        byte[] modBytes = pub.getModulus().toByteArray();
        if (modBytes.length > 0 && modBytes[0] == 0) {
            modBytes = Arrays.copyOfRange(modBytes, 1, modBytes.length);
        }
        String n = Base64.getUrlEncoder().withoutPadding().encodeToString(modBytes);
        String e = Base64.getUrlEncoder().withoutPadding().encodeToString(pub.getPublicExponent().toByteArray());

        String jwksJson = """
                {
                  "keys": [
                    {
                      "kty": "RSA",
                      "use": "sig",
                      "alg": "RS256",
                      "kid": "test-key-id-1",
                      "n": "%s",
                      "e": "%s"
                    }
                  ]
                }
                """.formatted(n, e);

        provider.parseAndStoreJwks(jwksJson);

        PublicKey resolvedKey = provider.getPublicKey("test-key-id-1");
        assertThat(resolvedKey).isNotNull();
        assertThat(resolvedKey.getAlgorithm()).isEqualTo("RSA");
    }

    @Test
    @DisplayName("Strict JWKS: getPublicKey throws IllegalStateException when kid is missing and eliminates permissive fallback")
    void shouldThrowWhenKidNotFoundAndEliminateFallback() {
        KeyPairGenerator generator;
        try {
            generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            KeyPair pair = generator.generateKeyPair();
            provider.registerKey("known-key-id", pair.getPublic());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        // Even though cache contains exactly one key ("known-key-id"), querying for an unknown kid
        // MUST NOT fall back to that key (C4 vulnerability fix)
        assertThatThrownBy(() -> provider.getPublicKey("unknown-key-id"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("unknown-key-id");

        assertThatThrownBy(() -> provider.getPublicKey(null))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> provider.getPublicKey("  "))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("resolveRoles correctly extracts roles claim, groups claim, and seeded identities")
    void shouldResolveRolesFromClaims() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair pair = generator.generateKeyPair();
        RSAPrivateKey priv = (RSAPrivateKey) pair.getPrivate();

        // 1. Direct 'roles' claim
        String tokenWithRoles = Jwts.builder()
                .subject("usr-123")
                .claim("roles", List.of("ROLE_ADMIN", "ROLE_OPERATOR"))
                .signWith(priv, Jwts.SIG.RS256)
                .compact();

        Claims claims1 = Jwts.parser().verifyWith(pair.getPublic()).build().parseSignedClaims(tokenWithRoles).getPayload();
        List<String> roles1 = provider.resolveRoles(claims1);
        assertThat(roles1).containsExactlyInAnyOrder("ROLE_ADMIN", "ROLE_OPERATOR");

        // 2. Direct 'groups' claim
        String tokenWithGroups = Jwts.builder()
                .subject("usr-456")
                .claim("groups", List.of("ROLE_OPERATOR"))
                .signWith(priv, Jwts.SIG.RS256)
                .compact();

        Claims claims2 = Jwts.parser().verifyWith(pair.getPublic()).build().parseSignedClaims(tokenWithGroups).getPayload();
        List<String> roles2 = provider.resolveRoles(claims2);
        assertThat(roles2).contains("ROLE_OPERATOR");

        // 3. Known seeded admin user
        String tokenSeededAdmin = Jwts.builder()
                .subject(FlociEntraJwksProvider.SEED_ADMIN_SUB)
                .claim("preferred_username", "alex.thorne@hibana.tk")
                .signWith(priv, Jwts.SIG.RS256)
                .compact();

        Claims claims3 = Jwts.parser().verifyWith(pair.getPublic()).build().parseSignedClaims(tokenSeededAdmin).getPayload();
        List<String> roles3 = provider.resolveRoles(claims3);
        assertThat(roles3).contains("ROLE_ADMIN", "ROLE_OPERATOR");

        // 4. Regular user with no administrative roles
        String tokenRegularUser = Jwts.builder()
                .subject("usr-random-buyer")
                .claim("preferred_username", "buyer@example.com")
                .signWith(priv, Jwts.SIG.RS256)
                .compact();

        Claims claims4 = Jwts.parser().verifyWith(pair.getPublic()).build().parseSignedClaims(tokenRegularUser).getPayload();
        List<String> roles4 = provider.resolveRoles(claims4);
        assertThat(roles4).isEmpty();
    }
}
