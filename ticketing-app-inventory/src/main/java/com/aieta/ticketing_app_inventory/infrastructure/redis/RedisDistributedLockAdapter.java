package com.aieta.ticketing_app_inventory.infrastructure.redis;

import com.aieta.ticketing_app_inventory.domain.ports.DistributedLockPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Collections;
import java.util.List;

@Component
public class RedisDistributedLockAdapter implements DistributedLockPort {
    private static final Logger log = LoggerFactory.getLogger(RedisDistributedLockAdapter.class);

    private final StringRedisTemplate redisTemplate;

    private static final String ACQUIRE_LOCK_LUA = """
            if redis.call('EXISTS', KEYS[1]) == 1 then
                return 0
            else
                redis.call('SET', KEYS[1], ARGV[1], 'EX', tonumber(ARGV[2]))
                return 1
            end
            """;

    private static final String RELEASE_LOCK_LUA = """
            if redis.call('GET', KEYS[1]) == ARGV[1] then
                return redis.call('DEL', KEYS[1])
            else
                return 0
            end
            """;

    private final DefaultRedisScript<Long> acquireScript;
    private final DefaultRedisScript<Long> releaseScript;

    public RedisDistributedLockAdapter(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.acquireScript = new DefaultRedisScript<>(ACQUIRE_LOCK_LUA, Long.class);
        this.releaseScript = new DefaultRedisScript<>(RELEASE_LOCK_LUA, Long.class);
    }

    private String buildKey(String eventId, String seatId) {
        return String.format("lock:event:%s:seat:%s", eventId, seatId);
    }

    @Override
    public boolean acquireSeatLock(String eventId, String seatId, String userId, Duration ttl) {
        String key = buildKey(eventId, seatId);
        long ttlSeconds = (ttl != null && !ttl.isNegative() && !ttl.isZero()) ? ttl.toSeconds() : 600L;
        List<String> keys = Collections.singletonList(key);

        try {
            Long result = redisTemplate.execute(acquireScript, keys, userId, String.valueOf(ttlSeconds));
            boolean acquired = result != null && result == 1L;
            if (acquired) {
                log.debug("Acquired distributed lock for key {} by user {} (ttl: {}s)", key, userId, ttlSeconds);
            } else {
                log.debug("Lock contention: key {} already held (requested by {})", key, userId);
            }
            return acquired;
        } catch (Exception e) {
            log.error("Error executing Redis Redlock Lua script for key {}", key, e);
            throw new RuntimeException("Redis lock acquisition failed for key: " + key, e);
        }
    }

    @Override
    public boolean releaseSeatLock(String eventId, String seatId, String userId) {
        String key = buildKey(eventId, seatId);
        List<String> keys = Collections.singletonList(key);

        try {
            Long result = redisTemplate.execute(releaseScript, keys, userId);
            boolean released = result != null && result > 0L;
            log.debug("Release distributed lock for key {} by user {}: {}", key, userId, released);
            return released;
        } catch (Exception e) {
            log.error("Error executing Redis Redlock release script for key {}", key, e);
            return false;
        }
    }

    @Override
    public void forceReleaseSeatLock(String eventId, String seatId) {
        String key = buildKey(eventId, seatId);
        try {
            Boolean deleted = redisTemplate.delete(key);
            log.debug("Force released lock for key {}: {}", key, deleted);
        } catch (Exception e) {
            log.warn("Error force releasing lock key {}", key, e);
        }
    }
}
