package com.henry.resiliencepatternsapi.service;

import io.github.resilience4j.ratelimiter.RateLimiter;
import org.springframework.stereotype.Service;

@Service
public class RateLimiterDemoService {

    private final RateLimiter rateLimiter;
    private final SimulatedDownstreamService downstreamService;

    public RateLimiterDemoService(RateLimiter demoRateLimiter, SimulatedDownstreamService downstreamService) {
        this.rateLimiter = demoRateLimiter;
        this.downstreamService = downstreamService;
    }

    public String call() {
        return rateLimiter.executeSupplier(() -> downstreamService.call(false));
    }
}
