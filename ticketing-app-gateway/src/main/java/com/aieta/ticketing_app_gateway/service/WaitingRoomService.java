package com.aieta.ticketing_app_gateway.service;

import com.aieta.ticketing_app_gateway.dto.QueueStatusResponse;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

@Service
public class WaitingRoomService {

    private static final Logger log = LoggerFactory.getLogger(WaitingRoomService.class);

    public static final String KEY_PREFIX_QUEUE = "queue:event:";
    public static final String KEY_PREFIX_ADMITTED = "queue:admitted:event:";
    public static final String KEY_ACTIVE_EVENTS = "queue:active-events";
    private static final Duration ADMISSION_TOKEN_TTL = Duration.ofMinutes(15);

    private final ReactiveStringRedisTemplate redisTemplate;

    @Value("${waiting-room.jwt.secret:${WAITING_ROOM_JWT_SECRET:virtual-waiting-room-hmac-key-2026-minimum-256-bits!}}")
    private String jwtSecret;

    @Value("${waiting-room.jwt.issuer:${WAITING_ROOM_JWT_ISSUER:waiting-room-engine}}")
    private String jwtIssuer;

    public WaitingRoomService(ReactiveStringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    private String queueKey(String eventId) {
        return KEY_PREFIX_QUEUE + eventId;
    }

    private String admittedKey(String eventId, String userId) {
        return KEY_PREFIX_ADMITTED + eventId + ":user:" + userId;
    }

    public Mono<QueueStatusResponse> joinQueue(String eventId, String userId) {
        String admKey = admittedKey(eventId, userId);
        String qKey = queueKey(eventId);

        return redisTemplate.opsForValue().get(admKey)
                .flatMap(existingToken -> redisTemplate.getExpire(admKey)
                        .map(ttl -> {
                            long expiresAt = Instant.now().plus(ttl).toEpochMilli();
                            return QueueStatusResponse.admitted(eventId, userId, existingToken, expiresAt);
                        })
                        .defaultIfEmpty(QueueStatusResponse.admitted(eventId, userId, existingToken,
                                Instant.now().plus(ADMISSION_TOKEN_TTL).toEpochMilli()))
                )
                .switchIfEmpty(
                        redisTemplate.opsForSet().add(KEY_ACTIVE_EVENTS, eventId)
                                .then(redisTemplate.opsForZSet().add(qKey, userId, System.currentTimeMillis()))
                                .then(getWaitingStatus(eventId, userId))
                );
    }

    public Mono<QueueStatusResponse> getQueueStatus(String eventId, String userId) {
        String admKey = admittedKey(eventId, userId);

        return redisTemplate.opsForValue().get(admKey)
                .flatMap(token -> redisTemplate.getExpire(admKey)
                        .map(ttl -> {
                            long expiresAt = Instant.now().plus(ttl).toEpochMilli();
                            return QueueStatusResponse.admitted(eventId, userId, token, expiresAt);
                        })
                        .defaultIfEmpty(QueueStatusResponse.admitted(eventId, userId, token,
                                Instant.now().plus(ADMISSION_TOKEN_TTL).toEpochMilli()))
                )
                .switchIfEmpty(getWaitingStatus(eventId, userId));
    }

    private Mono<QueueStatusResponse> getWaitingStatus(String eventId, String userId) {
        String qKey = queueKey(eventId);

        return redisTemplate.opsForZSet().rank(qKey, userId)
                .flatMap(rank -> redisTemplate.opsForZSet().size(qKey)
                        .map(total -> {
                            long position = rank + 1;
                            long estimatedWaitSeconds = position * 2; // ~2s per position with batch admission
                            return QueueStatusResponse.waiting(eventId, userId, position, total, estimatedWaitSeconds);
                        })
                )
                .defaultIfEmpty(QueueStatusResponse.notInQueue(eventId, userId));
    }

    public Mono<Void> leaveQueue(String eventId, String userId) {
        String qKey = queueKey(eventId);
        String admKey = admittedKey(eventId, userId);

        return redisTemplate.opsForZSet().remove(qKey, userId)
                .then(redisTemplate.delete(admKey))
                .then();
    }

    public Mono<QueueStatusResponse> skipOrAdmit(String eventId, String userId) {
        String qKey = queueKey(eventId);
        String admKey = admittedKey(eventId, userId);

        Instant now = Instant.now();
        Instant exp = now.plus(ADMISSION_TOKEN_TTL);
        String token = mintToken(eventId, userId, now, exp);

        return redisTemplate.opsForValue().set(admKey, token, ADMISSION_TOKEN_TTL)
                .then(redisTemplate.opsForZSet().remove(qKey, userId))
                .map(ok -> QueueStatusResponse.admitted(eventId, userId, token, exp.toEpochMilli()));
    }

    public Flux<String> getActiveEvents() {
        return redisTemplate.opsForSet().members(KEY_ACTIVE_EVENTS);
    }

    public Mono<Integer> admitBatch(String eventId, int batchSize) {
        String qKey = queueKey(eventId);

        return redisTemplate.opsForZSet().popMin(qKey, batchSize)
                .flatMap(tuple -> {
                    String userId = tuple.getValue();
                    if (userId == null || userId.isBlank()) {
                        return Mono.empty();
                    }
                    String admKey = admittedKey(eventId, userId);
                    Instant now = Instant.now();
                    Instant exp = now.plus(ADMISSION_TOKEN_TTL);
                    String token = mintToken(eventId, userId, now, exp);

                    return redisTemplate.opsForValue().set(admKey, token, ADMISSION_TOKEN_TTL)
                            .doOnSuccess(v -> log.info("[WAITING-ROOM] Admitted user {} for event {}", userId, eventId));
                })
                .count()
                .map(count -> count.intValue())
                .flatMap(admittedCount -> {
                    // Check if queue is now empty
                    return redisTemplate.opsForZSet().size(qKey)
                            .flatMap(remaining -> {
                                if (remaining == 0) {
                                    return redisTemplate.opsForSet().remove(KEY_ACTIVE_EVENTS, eventId)
                                            .thenReturn(admittedCount);
                                }
                                return Mono.just(admittedCount);
                            });
                });
    }

    public String mintToken(String eventId, String userId, Instant issuedAt, Instant expiresAt) {
        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        return Jwts.builder()
                .issuer(jwtIssuer)
                .subject(userId != null ? userId : "guest-" + System.currentTimeMillis())
                .claim("eventId", eventId)
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(expiresAt))
                .signWith(key)
                .compact();
    }
}
