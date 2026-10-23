package com.henry.resiliencepatternsapi.service;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class CircuitBreakerDemoService {

    private final CircuitBreaker circuitBreaker;
    private final SimulatedDownstreamService downstreamService;

    public CircuitBreakerDemoService(@Qualifier("demoCircuitBreaker") CircuitBreaker demoCircuitBreaker,
                                     SimulatedDownstreamService downstreamService) {
        this.circuitBreaker = demoCircuitBreaker;
        this.downstreamService = downstreamService;
    }

    public String call(boolean fail) {
        return circuitBreaker.executeSupplier(() -> downstreamService.call(fail));
    }
}
