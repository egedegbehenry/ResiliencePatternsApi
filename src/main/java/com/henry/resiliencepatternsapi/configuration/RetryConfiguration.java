package com.henry.resiliencepatternsapi.configuration;

import com.henry.resiliencepatternsapi.exception.DownstreamUnavailableException;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class RetryConfiguration {

    private static final Logger log = LoggerFactory.getLogger(RetryConfiguration.class);

    @Bean
    public Retry demoRetry() {
        RetryConfig config = RetryConfig.custom()
                .maxAttempts(3)
                .waitDuration(Duration.ofMillis(200))
                .retryExceptions(DownstreamUnavailableException.class)
                .build();

        Retry retry = Retry.of("demoRetryDownstream", config);
        retry.getEventPublisher().onRetry(event ->
                log.info("Retry {}: retry number {}", event.getName(), event.getNumberOfRetryAttempts()));
        return retry;
    }
}
