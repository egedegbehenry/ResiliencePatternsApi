package com.henry.resiliencepatternsapi.service;

import com.henry.resiliencepatternsapi.configuration.BulkheadConfiguration;
import com.henry.resiliencepatternsapi.exception.DownstreamUnavailableException;
import io.github.resilience4j.bulkhead.BulkheadFullException;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class BulkheadDemoServiceTests {

    @Test
    void rejectsThirdConcurrentCallAndReleasesCapacityAfterSuccess() throws Exception {
        var bulkhead = new BulkheadConfiguration().demoBulkhead();
        var downstream = mock(SimulatedDownstreamService.class);
        var entered = new CountDownLatch(2);
        var release = new CountDownLatch(1);
        when(downstream.call(false, 0)).thenAnswer(invocation -> {
            entered.countDown();
            if (!release.await(5, TimeUnit.SECONDS)) {
                throw new AssertionError("Timed out waiting for release");
            }
            return "OK";
        });
        var service = new BulkheadDemoService(bulkhead, downstream);

        try (var executor = Executors.newFixedThreadPool(2)) {
            try {
                var first = executor.submit(() -> service.call(0));
                var second = executor.submit(() -> service.call(0));
                assertThat(entered.await(5, TimeUnit.SECONDS)).isTrue();
                assertThatThrownBy(() -> service.call(0)).isInstanceOf(BulkheadFullException.class);
                verify(downstream, times(2)).call(false, 0);
                release.countDown();
                assertThat(first.get(5, TimeUnit.SECONDS)).isEqualTo("OK");
                assertThat(second.get(5, TimeUnit.SECONDS)).isEqualTo("OK");
                assertThat(service.call(0)).isEqualTo("OK");
                assertThat(bulkhead.getMetrics().getAvailableConcurrentCalls()).isEqualTo(2);
            } finally {
                release.countDown();
            }
        }
    }

    @Test
    void releasesCapacityAfterFailure() {
        var bulkhead = new BulkheadConfiguration().demoBulkhead();
        var downstream = mock(SimulatedDownstreamService.class);
        when(downstream.call(false, 0)).thenThrow(new DownstreamUnavailableException()).thenReturn("OK");
        var service = new BulkheadDemoService(bulkhead, downstream);

        assertThatThrownBy(() -> service.call(0)).isInstanceOf(DownstreamUnavailableException.class);
        assertThat(bulkhead.getMetrics().getAvailableConcurrentCalls()).isEqualTo(2);
        assertThat(service.call(0)).isEqualTo("OK");
    }

    @Test
    void preservesInterruptFlagAndReleasesCapacityWhenDelayIsInterrupted() {
        var bulkhead = new BulkheadConfiguration().demoBulkhead();
        var downstream = mock(SimulatedDownstreamService.class);
        when(downstream.call(false, 1000)).thenAnswer(invocation -> {
            // Interrupt after permission acquisition, immediately before the delay.
            Thread.currentThread().interrupt();
            return new SimulatedDownstreamService().call(false, 1000);
        });
        var service = new BulkheadDemoService(bulkhead, downstream);
        try {
            assertThatThrownBy(() -> service.call(1000))
                    .isInstanceOf(DownstreamUnavailableException.class)
                    .hasCauseInstanceOf(InterruptedException.class);
            assertThat(Thread.currentThread().isInterrupted()).isTrue();
            assertThat(bulkhead.getMetrics().getAvailableConcurrentCalls()).isEqualTo(2);
        } finally {
            Thread.interrupted();
        }
    }
}
