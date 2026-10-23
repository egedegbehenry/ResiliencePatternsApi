package com.henry.resiliencepatternsapi.service;

import com.henry.resiliencepatternsapi.dto.RetryDemoResponse;
import io.github.resilience4j.retry.Retry;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicInteger;

@Service
public class RetryDemoService {

    private final Retry retry;
    private final SimulatedDownstreamService downstreamService;

    public RetryDemoService(Retry demoRetry, SimulatedDownstreamService downstreamService) {
        this.retry = demoRetry;
        this.downstreamService = downstreamService;
    }

    public RetryDemoResponse call(int failTimes) {
        if (failTimes < 0 || failTimes > 3) {
            throw new IllegalArgumentException("failTimes must be between 0 and 3");
        }
        // Each HTTP request has its own counter, even when requests run concurrently.
        AtomicInteger attempts = new AtomicInteger();
        String message = retry.executeSupplier(() ->
                downstreamService.call(attempts.incrementAndGet() <= failTimes));
        return new RetryDemoResponse(message, attempts.get());
    }
}
