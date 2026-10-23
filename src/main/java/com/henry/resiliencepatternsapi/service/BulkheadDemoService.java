package com.henry.resiliencepatternsapi.service;

import io.github.resilience4j.bulkhead.Bulkhead;
import org.springframework.stereotype.Service;

@Service
public class BulkheadDemoService {

    private final Bulkhead bulkhead;
    private final SimulatedDownstreamService downstreamService;

    public BulkheadDemoService(Bulkhead demoBulkhead, SimulatedDownstreamService downstreamService) {
        this.bulkhead = demoBulkhead;
        this.downstreamService = downstreamService;
    }

    public String call(int delayMs) {
        if (delayMs < 0 || delayMs > 3000) {
            throw new IllegalArgumentException("delayMs must be between 0 and 3000");
        }
        return bulkhead.executeSupplier(() -> downstreamService.call(false, delayMs));
    }
}
