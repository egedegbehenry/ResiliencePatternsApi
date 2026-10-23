package com.henry.resiliencepatternsapi.service;

import com.henry.resiliencepatternsapi.exception.DownstreamUnavailableException;
import io.github.resilience4j.timelimiter.TimeLimiter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

@Service
public class TimeLimiterDemoService {

    private final TimeLimiter timeLimiter;
    private final ExecutorService executor;
    private final SimulatedDownstreamService downstreamService;

    public TimeLimiterDemoService(TimeLimiter demoTimeLimiter,
                                  @Qualifier("timeLimiterExecutor") ExecutorService executor,
                                  SimulatedDownstreamService downstreamService) {
        this.timeLimiter = demoTimeLimiter;
        this.executor = executor;
        this.downstreamService = downstreamService;
    }

    public String call(int delayMs) throws Exception {
        if (delayMs < 0 || delayMs > 3000) {
            throw new IllegalArgumentException("delayMs must be between 0 and 3000");
        }
        Future<String> future = executor.submit(() -> downstreamService.call(false, delayMs));
        try {
            // Future.get's timeout includes time spent waiting in the executor queue.
            return timeLimiter.executeFutureSupplier(() -> future);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new DownstreamUnavailableException("Downstream request was interrupted", exception);
        } finally {
            if (!future.isDone()) {
                future.cancel(true);
            }
        }
    }
}
