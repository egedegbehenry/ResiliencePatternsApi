package com.henry.resiliencepatternsapi.configuration;

import com.henry.resiliencepatternsapi.exception.DownstreamUnavailableException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class CircuitBreakerConfiguration {

    private static final Logger log = LoggerFactory.getLogger(CircuitBreakerConfiguration.class);

    @Bean
    public CircuitBreaker demoCircuitBreaker() {
        return createCircuitBreaker("demoDownstream");
    }

    @Bean
    public CircuitBreaker inventoryCircuitBreaker() {
        return createCircuitBreaker("inventoryService");
    }

    @Bean
    public CircuitBreaker paymentCircuitBreaker() {
        return createCircuitBreaker("paymentService");
    }

    private CircuitBreaker createCircuitBreaker(String name) {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
                .slidingWindowSize(5)
                .minimumNumberOfCalls(5)
                .failureRateThreshold(50)
                .waitDurationInOpenState(Duration.ofSeconds(10))
                .permittedNumberOfCallsInHalfOpenState(2)
                .recordExceptions(DownstreamUnavailableException.class)
                .build();

        CircuitBreaker breaker = CircuitBreaker.of(name, config);
        breaker.getEventPublisher().onStateTransition(event ->
                log.info("Circuit breaker {}: {}", event.getCircuitBreakerName(), event.getStateTransition()));
        return breaker;
    }
}
