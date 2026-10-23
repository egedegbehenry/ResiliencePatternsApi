package com.henry.resiliencepatternsapi.configuration;

import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.micrometer.tagged.TaggedBulkheadMetricsPublisher;
import io.github.resilience4j.micrometer.tagged.TaggedCircuitBreakerMetricsPublisher;
import io.github.resilience4j.micrometer.tagged.TaggedRateLimiterMetricsPublisher;
import io.github.resilience4j.micrometer.tagged.TaggedRetryMetricsPublisher;
import io.github.resilience4j.micrometer.tagged.TaggedTimeLimiterMetricsPublisher;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.timelimiter.TimeLimiter;
import io.micrometer.core.instrument.binder.MeterBinder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration(proxyBeanMethods = false)
public class ResilienceMetricsConfiguration {

    @Bean
    public MeterBinder resilienceMetrics(List<CircuitBreaker> circuitBreakers,
                                         List<Retry> retries,
                                         List<RateLimiter> rateLimiters,
                                         List<Bulkhead> bulkheads,
                                         List<TimeLimiter> timeLimiters) {
        return meterRegistry -> {
            // Bind the same instances injected into the services, rather than creating new ones.
            var circuitBreakerMetrics = new TaggedCircuitBreakerMetricsPublisher(meterRegistry);
            circuitBreakers.forEach(circuitBreakerMetrics::publishMetrics);

            var retryMetrics = new TaggedRetryMetricsPublisher(meterRegistry);
            retries.forEach(retryMetrics::publishMetrics);

            var rateLimiterMetrics = new TaggedRateLimiterMetricsPublisher(meterRegistry);
            rateLimiters.forEach(rateLimiterMetrics::publishMetrics);

            var bulkheadMetrics = new TaggedBulkheadMetricsPublisher(meterRegistry);
            bulkheads.forEach(bulkheadMetrics::publishMetrics);

            var timeLimiterMetrics = new TaggedTimeLimiterMetricsPublisher(meterRegistry);
            timeLimiters.forEach(timeLimiterMetrics::publishMetrics);
        };
    }
}
