package com.henry.resiliencepatternsapi.service;

import com.henry.resiliencepatternsapi.dto.FallbackDemoResponse;
import com.henry.resiliencepatternsapi.exception.DownstreamUnavailableException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class FallbackDemoService {

    private static final Logger log = LoggerFactory.getLogger(FallbackDemoService.class);

    private final CircuitBreakerDemoService circuitBreakerService;

    public FallbackDemoService(CircuitBreakerDemoService circuitBreakerService) {
        this.circuitBreakerService = circuitBreakerService;
    }

    public FallbackDemoResponse call(boolean fail) {
        try {
            return new FallbackDemoResponse(circuitBreakerService.call(fail), false);
        } catch (DownstreamUnavailableException | CallNotPermittedException exception) {
            log.warn("Returning demo fallback after {}", exception.getClass().getSimpleName());
            return new FallbackDemoResponse(
                    "Fallback response: downstream service is temporarily unavailable", true);
        }
    }
}
