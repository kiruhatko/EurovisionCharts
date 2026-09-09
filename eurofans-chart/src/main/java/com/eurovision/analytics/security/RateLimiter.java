package com.eurovision.analytics.security;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Redis-backed rolling-window rate limiter (spec 11) for user-facing commands
 * and provider sync jobs. A fixed-window counter is a deliberate
 * simplification over a true sliding window: it is enough to bound abuse
 * without adding another moving part, and it fails open (allows the request)
 * if Redis itself is unreachable, since a rate limiter must never become a
 * single point of total outage for the bot.
 */
@Component
public class RateLimiter {

    private final StringRedisTemplate redisTemplate;

    public RateLimiter(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public boolean tryAcquire(String key, int maxRequests, Duration window) {
        try {
            String redisKey = "ratelimit:" + key;
            Long count = redisTemplate.opsForValue().increment(redisKey);
            if (count == null) {
                return true;
            }
            if (count == 1L) {
                redisTemplate.expire(redisKey, window);
            }
            return count <= maxRequests;
        } catch (Exception e) {
            return true;
        }
    }
}
