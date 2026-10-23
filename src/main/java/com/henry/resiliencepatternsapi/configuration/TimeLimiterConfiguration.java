package com.henry.resiliencepatternsapi.configuration;

import io.github.resilience4j.timelimiter.TimeLimiter;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Configuration
public class TimeLimiterConfiguration {

    @Bean
    public TimeLimiter demoTimeLimiter() {
        TimeLimiterConfig config = TimeLimiterConfig.custom()
                .timeoutDuration(Duration.ofSeconds(1))
                .cancelRunningFuture(true)
                .build();
        return TimeLimiter.of("demoTimeLimitedDownstream", config);
    }

    @Bean(destroyMethod = "shutdownNow")
    public ThreadPoolExecutor timeLimiterExecutor() {
        return new ThreadPoolExecutor(2, 2, 0, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(4),
                Thread.ofPlatform().name("time-limiter-", 0).factory(),
                new ThreadPoolExecutor.AbortPolicy());
    }
}
