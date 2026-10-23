package com.henry.resiliencepatternsapi.service;

import com.henry.resiliencepatternsapi.configuration.RateLimiterConfiguration;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import io.github.resilience4j.ratelimiter.internal.AtomicRateLimiter;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class RateLimiterDemoServiceTests {

    @Test
    void rejectsExcessCallsWithoutInvokingDownstreamAndRefreshesPermissions() {
        AtomicLong clock = new AtomicLong();
        SimulatedDownstreamService downstream = mock(SimulatedDownstreamService.class);
        when(downstream.call(false)).thenReturn("OK");
        RateLimiterDemoService service = service(clock, downstream);

        for (int i = 0; i < 5; i++) {
            assertThat(service.call()).isEqualTo("OK");
        }
        assertThatThrownBy(service::call).isInstanceOf(RequestNotPermitted.class);
        verify(downstream, times(5)).call(false);

        clock.set(Duration.ofSeconds(10).toNanos() - 1);
        assertThatThrownBy(service::call).isInstanceOf(RequestNotPermitted.class);
        verifyNoMoreInteractions(downstream);

        clock.incrementAndGet();
        for (int i = 0; i < 5; i++) {
            assertThat(service.call()).isEqualTo("OK");
        }
        assertThatThrownBy(service::call).isInstanceOf(RequestNotPermitted.class);
        verify(downstream, times(10)).call(false);
        verifyNoMoreInteractions(downstream);
    }

    @Test
    void sharesFivePermissionsAcrossConcurrentCallers() throws Exception {
        SimulatedDownstreamService downstream = mock(SimulatedDownstreamService.class);
        when(downstream.call(false)).thenReturn("OK");
        RateLimiterDemoService service = service(new AtomicLong(), downstream);

        try (var executor = Executors.newFixedThreadPool(10)) {
            List<Future<Boolean>> results = new ArrayList<>();
            for (int i = 0; i < 20; i++) {
                results.add(executor.submit(() -> {
                    try {
                        service.call();
                        return true;
                    } catch (RequestNotPermitted exception) {
                        return false;
                    }
                }));
            }
            int accepted = 0;
            for (Future<Boolean> result : results) {
                if (result.get()) {
                    accepted++;
                }
            }
            assertThat(accepted).isEqualTo(5);
        }
        verify(downstream, times(5)).call(false);
        verifyNoMoreInteractions(downstream);
    }

    private RateLimiterDemoService service(AtomicLong clock, SimulatedDownstreamService downstream) {
        var config = new RateLimiterConfiguration().demoRateLimiter().getRateLimiterConfig();
        assertThat(config.getTimeoutDuration()).isEqualTo(Duration.ZERO);
        return new RateLimiterDemoService(new AtomicRateLimiter("test", config, clock::get), downstream);
    }
}
