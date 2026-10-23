package com.henry.resiliencepatternsapi.exception;

import com.henry.resiliencepatternsapi.controller.CircuitBreakerDemoController;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice(assignableTypes = CircuitBreakerDemoController.class)
public class DemoExceptionHandler {

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
