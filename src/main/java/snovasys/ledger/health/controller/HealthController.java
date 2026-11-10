package snovasys.ledger.health.controller;

import java.sql.Connection;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.sql.DataSource;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import lombok.RequiredArgsConstructor;

@RestController @RequestMapping("/api/health") @RequiredArgsConstructor
public class HealthController {
    private final DataSource dataSource;
    private final StringRedisTemplate redis;

    @GetMapping("/live")
    public Map<String, Object> live() { return Map.of("status", "UP", "timestamp", Instant.now()); }

    @GetMapping("/ready")
    public ResponseEntity<Map<String, Object>> ready() {
        Map<String, String> checks = new LinkedHashMap<>();
        checks.put("database", databaseReady() ? "UP" : "DOWN");
        checks.put("redis", redisReady() ? "UP" : "DOWN");
        boolean ready = checks.values().stream().allMatch("UP"::equals);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", ready ? "UP" : "DOWN");
        body.put("checks", checks);
        body.put("timestamp", Instant.now());
        return ResponseEntity.status(ready ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE).body(body);
    }

    private boolean databaseReady() {
        try (Connection connection = dataSource.getConnection()) { return connection.isValid(2); }
        catch (Exception exception) { return false; }
    }

    private boolean redisReady() {
        try (RedisConnection connection = redis.getConnectionFactory().getConnection()) {
            return "PONG".equalsIgnoreCase(connection.ping());
        } catch (Exception exception) { return false; }
    }
}
