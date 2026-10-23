package com.henry.resiliencepatternsapi.exception;

import com.henry.resiliencepatternsapi.controller.CircuitBreakerDemoController;
import com.henry.resiliencepatternsapi.controller.RetryDemoController;
import com.henry.resiliencepatternsapi.controller.RateLimiterDemoController;
import com.henry.resiliencepatternsapi.controller.BulkheadDemoController;
import io.github.resilience4j.bulkhead.BulkheadFullException;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice(assignableTypes = {
        CircuitBreakerDemoController.class, RetryDemoController.class,
        RateLimiterDemoController.class, BulkheadDemoController.class
})
public class DemoExceptionHandler {

    @ExceptionHandler(BulkheadFullException.class)
    public ResponseEntity<Map<String, String>> bulkheadFull() {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of("message", "Bulkhead is full; try again later"));
    }

    @ExceptionHandler(RequestNotPermitted.class)
    public ResponseEntity<Map<String, String>> rateLimitExceeded() {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header("Retry-After", "10")
                .body(Map.of("message", "Rate limit exceeded; try again later"));
    }

    @ExceptionHandler(DownstreamUnavailableException.class)
    public ResponseEntity<Map<String, String>> downstreamUnavailable(DownstreamUnavailableException exception) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of("message", exception.getMessage()));
    }

    @ExceptionHandler(CallNotPermittedException.class)
    public ResponseEntity<Map<String, String>> circuitOpen() {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of("message", "Circuit breaker is open; try again later"));
    }
}
