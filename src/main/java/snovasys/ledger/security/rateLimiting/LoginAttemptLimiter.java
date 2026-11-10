package snovasys.ledger.security.rateLimiting;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

@Service
public class LoginAttemptLimiter {

    private static final int MAX_FAILURES = 5;
    private static final long WINDOW_MS = 10 * 60 * 1000L;

    private final StringRedisTemplate redis;

    private static final DefaultRedisScript<Long> CHECK_SCRIPT =
        new DefaultRedisScript<>("""
            local a = redis.call('PTTL', KEYS[1])
            local i = redis.call('PTTL', KEYS[2])
            if a > 0 and i > 0 then
                return math.max(a, i)
            elseif a > 0 then
                return a
            elseif i > 0 then
                return i
            end
            return 0
            """, Long.class);

    private static final DefaultRedisScript<Long> FAILURE_SCRIPT =
        new DefaultRedisScript<>("""
            local accountCount = redis.call('INCR', KEYS[1])
            if accountCount == 1 then
                redis.call('PEXPIRE', KEYS[1], ARGV[1])
            end

            local ipCount = redis.call('INCR', KEYS[2])
            if ipCount == 1 then
                redis.call('PEXPIRE', KEYS[2], ARGV[1])
            end

            if accountCount >= tonumber(ARGV[2]) then
                redis.call('SET', KEYS[3], '1', 'PX', ARGV[1])
            end

            if ipCount >= tonumber(ARGV[2]) then
                redis.call('SET', KEYS[4], '1', 'PX', ARGV[1])
            end

            local a = redis.call('PTTL', KEYS[3])
            local i = redis.call('PTTL', KEYS[4])
            return math.max(a, i, 0)
            """, Long.class);

    private static final DefaultRedisScript<Long> CLEAR_SCRIPT =
        new DefaultRedisScript<>("""
            return redis.call('DEL', KEYS[1])
            """, Long.class);

    public LoginAttemptLimiter(StringRedisTemplate redis) {
        this.redis = redis;
    }

    public long blockedForSeconds(String email, String ip) {
        List<String> keys = lockKeys(email, ip);

        Long ttl = redis.execute(CHECK_SCRIPT, keys);

        return ttl == null ? 0 : Math.max(0, (ttl + 999) / 1000);
    }

    public long recordFailure(String email, String ip) {
        List<String> keys = allKeys(email, ip);

        Long ttl = redis.execute(
            FAILURE_SCRIPT,
            keys,
            String.valueOf(WINDOW_MS),
            String.valueOf(MAX_FAILURES)
        );

        return ttl == null ? 0 : Math.max(0, (ttl + 999) / 1000);
    }

    public void recordSuccess(String email) {
        redis.execute(
            CLEAR_SCRIPT,
            List.of(accountCountKey(email))
        );
    }

    private List<String> lockKeys(String email, String ip) {
        return List.of(accountLockKey(email), ipLockKey(ip));
    }

    private List<String> allKeys(String email, String ip) {
        return List.of(
            accountCountKey(email),
            ipCountKey(ip),
            accountLockKey(email),
            ipLockKey(ip)
        );
    }

    private String accountCountKey(String email) {
        return "auth:login:account:count:" + digest(normalizeEmail(email));
    }

    private String accountLockKey(String email) {
        return "auth:login:account:lock:" + digest(normalizeEmail(email));
    }

    private String ipCountKey(String ip) {
        return "auth:login:ip:count:" + digest(ip);
    }

    private String ipLockKey(String ip) {
        return "auth:login:ip:lock:" + digest(ip);
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String digest(String value) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256")
                .digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
