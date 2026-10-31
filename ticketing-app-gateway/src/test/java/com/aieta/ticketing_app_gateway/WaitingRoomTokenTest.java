package com.aieta.ticketing_app_gateway;

import com.aieta.ticketing_app_gateway.service.WaitingRoomService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

@DisplayName("Unit Test: Waiting Room Token Generation & Gateway Filter Interop")
class WaitingRoomTokenTest {

    private static final String SECRET = "virtual-waiting-room-hmac-key-2026-minimum-256-bits!";
    private static final String ISSUER = "waiting-room-engine";

    @Test
    @DisplayName("mintToken generates HMAC-SHA256 JWT verifiable by CheckoutTokenFilterFactory")
    void shouldGenerateValidToken() {
        ReactiveStringRedisTemplate redisTemplate = mock(ReactiveStringRedisTemplate.class);
        WaitingRoomService service = new WaitingRoomService(redisTemplate);
        ReflectionTestUtils.setField(service, "jwtSecret", SECRET);
        ReflectionTestUtils.setField(service, "jwtIssuer", ISSUER);

        String eventId = "chronos-fest-2026";
        String userId = "usr-vip-999";
        Instant now = Instant.now();
        Instant exp = now.plus(15, ChronoUnit.MINUTES);

        String token = service.mintToken(eventId, userId, now, exp);
        assertThat(token).isNotBlank();

        // Validate using exact same verification logic as CheckoutTokenFilterFactory
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .requireIssuer(ISSUER)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        assertThat(claims.getSubject()).isEqualTo(userId);
        assertThat(claims.get("eventId", String.class)).isEqualTo(eventId);
        assertThat(claims.getIssuer()).isEqualTo(ISSUER);
        assertThat(claims.getExpiration()).isAfter(claims.getIssuedAt());
    }
}
