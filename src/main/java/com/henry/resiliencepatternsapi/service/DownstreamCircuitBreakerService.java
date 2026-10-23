package com.henry.resiliencepatternsapi.service;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class DownstreamCircuitBreakerService {

    private final CircuitBreaker inventoryCircuitBreaker;
    private final CircuitBreaker paymentCircuitBreaker;
    private final SimulatedDownstreamService downstreamService;

    public DownstreamCircuitBreakerService(
            @Qualifier("inventoryCircuitBreaker") CircuitBreaker inventoryCircuitBreaker,
            @Qualifier("paymentCircuitBreaker") CircuitBreaker paymentCircuitBreaker,
            SimulatedDownstreamService downstreamService) {
        this.inventoryCircuitBreaker = inventoryCircuitBreaker;
        this.paymentCircuitBreaker = paymentCircuitBreaker;
        this.downstreamService = downstreamService;
    }

    public String inventory(boolean fail) {
        return inventoryCircuitBreaker.executeSupplier(() -> downstreamService.call(fail));
    }

    public String payment(boolean fail) {
        return paymentCircuitBreaker.executeSupplier(() -> downstreamService.call(fail));
    }
}
