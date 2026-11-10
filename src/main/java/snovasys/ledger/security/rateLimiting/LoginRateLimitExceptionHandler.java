package snovasys.ledger.security.rateLimiting;



import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class LoginRateLimitExceptionHandler {

    @ExceptionHandler(LoginRateLimitException.class)
    public ResponseEntity<Map<String, Object>> handle(
            LoginRateLimitException ex) {

        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
            .header(
                HttpHeaders.RETRY_AFTER,
                String.valueOf(ex.getRetryAfterSeconds())
            )
            .body(Map.of(
                "status", 429,
                "message", "Too many login attempts. Try again later.",
                "retryAfterSeconds", ex.getRetryAfterSeconds()
            ));
    }
}
