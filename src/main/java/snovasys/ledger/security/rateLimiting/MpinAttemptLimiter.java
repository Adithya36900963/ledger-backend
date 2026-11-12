package snovasys.ledger.security.rateLimiting;

import java.util.List;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

@Service
public class MpinAttemptLimiter {
    private static final long WINDOW_MS = 10 * 60 * 1000L;
    private static final int MAX_FAILURES = 5;
    private static final DefaultRedisScript<Long> FAILURE_SCRIPT = new DefaultRedisScript<>("""
        if redis.call('PTTL', KEYS[2]) > 0 then return redis.call('PTTL', KEYS[2]) end
        local count = redis.call('INCR', KEYS[1])
        if count == 1 then redis.call('PEXPIRE', KEYS[1], ARGV[1]) end
        if count >= tonumber(ARGV[2]) then
            redis.call('SET', KEYS[2], '1', 'PX', ARGV[1])
            return tonumber(ARGV[1])
        end
        return 0
        """, Long.class);

    private final StringRedisTemplate redis;

    public MpinAttemptLimiter(StringRedisTemplate redis) { this.redis = redis; }

    public long recordFailure(long userId) {
        Long ttl = redis.execute(FAILURE_SCRIPT,
                List.of("security:mpin:count:" + userId, "security:mpin:lock:" + userId),
                String.valueOf(WINDOW_MS), String.valueOf(MAX_FAILURES));
        return ttl == null ? 0 : Math.max(0, (ttl + 999) / 1000);
    }

    public long blockedForSeconds(long userId) {
        Long ttl = redis.getExpire("security:mpin:lock:" + userId, java.util.concurrent.TimeUnit.SECONDS);
        return ttl == null ? 0 : Math.max(0, ttl);
    }

    public void recordSuccess(long userId) {
        redis.delete(List.of("security:mpin:count:" + userId, "security:mpin:lock:" + userId));
    }
}
