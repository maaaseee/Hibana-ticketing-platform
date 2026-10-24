package com.aieta.ticketing_app_catalog.infrastructure.cache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.function.Supplier;

@Service
public class CatalogCacheService {

    private static final Logger log = LoggerFactory.getLogger(CatalogCacheService.class);

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public CatalogCacheService(@Autowired(required = false) StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    public <T> T getOrFetch(String key, Duration ttl, Class<T> clazz, Supplier<T> fallback) {
        if (redisTemplate != null) {
            try {
                String cachedJson = redisTemplate.opsForValue().get(key);
                if (cachedJson != null && !cachedJson.isBlank()) {
                    return objectMapper.readValue(cachedJson, clazz);
                }
            } catch (Exception e) {
                log.warn("Redis cache read failed for key {}: {}. Falling back to source.", key, e.getMessage());
            }
        }

        T data = fallback.get();

        if (redisTemplate != null && data != null) {
            try {
                String json = objectMapper.writeValueAsString(data);
                redisTemplate.opsForValue().set(key, json, ttl);
            } catch (Exception e) {
                log.warn("Redis cache write failed for key {}: {}", key, e.getMessage());
            }
        }

        return data;
    }

    public void evict(String key) {
        if (redisTemplate != null) {
            try {
                redisTemplate.delete(key);
            } catch (Exception e) {
                log.warn("Redis cache evict failed for key {}: {}", key, e.getMessage());
            }
        }
    }
}
