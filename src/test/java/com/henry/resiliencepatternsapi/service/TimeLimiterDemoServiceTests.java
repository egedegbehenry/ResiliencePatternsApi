package com.henry.resiliencepatternsapi.service;

import com.henry.resiliencepatternsapi.configuration.TimeLimiterConfiguration;
import com.henry.resiliencepatternsapi.exception.DownstreamUnavailableException;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class TimeLimiterDemoServiceTests {

    @Test
    void cancelsAndInterruptsRunningWorkOnTimeoutThenAllowsAnotherCall() throws Exception {
        var configuration = new TimeLimiterConfiguration();
        var downstream = mock(SimulatedDownstreamService.class);
        var interrupted = new CountDownLatch(1);
        when(downstream.call(false, 2000)).thenAnswer(invocation -> {
            try {
                new CountDownLatch(1).await();
                return "Unexpected completion";
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                interrupted.countDown();
                throw new DownstreamUnavailableException("Interrupted", exception);
            }
        });
        when(downstream.call(false, 0)).thenReturn("OK");

        try (var executor = configuration.timeLimiterExecutor()) {
            var service = new TimeLimiterDemoService(configuration.demoTimeLimiter(), executor, downstream);
            assertThatThrownBy(() -> service.call(2000)).isInstanceOf(TimeoutException.class);
            assertThat(interrupted.await(5, TimeUnit.SECONDS)).isTrue();
            assertThat(service.call(0)).isEqualTo("OK");
        }
    }

    @Test
    void preservesDownstreamFailureWithoutTurningItIntoTimeout() {
        var configuration = new TimeLimiterConfiguration();
        var downstream = mock(SimulatedDownstreamService.class);
        var failure = new DownstreamUnavailableException();
        when(downstream.call(false, 0)).thenThrow(failure);

        try (var executor = configuration.timeLimiterExecutor()) {
            var service = new TimeLimiterDemoService(configuration.demoTimeLimiter(), executor, downstream);
            assertThatThrownBy(() -> service.call(0)).isSameAs(failure);
        }
    }

    @Test
    void interruptedCallerCancelsWorkAndPreservesInterruptFlag() throws Exception {
        var configuration = new TimeLimiterConfiguration();
        var entered = new CountDownLatch(1);
        var workerInterrupted = new CountDownLatch(1);
        var finished = new CountDownLatch(1);
        var failure = new AtomicReference<Throwable>();
        var downstream = mock(SimulatedDownstreamService.class);
        when(downstream.call(false, 2000)).thenAnswer(invocation -> {
            entered.countDown();
            try {
                new CountDownLatch(1).await();
                return "Unexpected completion";
            } catch (InterruptedException exception) {
                workerInterrupted.countDown();
                Thread.currentThread().interrupt();
                throw new DownstreamUnavailableException("Interrupted", exception);
            }
        });
        try (var executor = configuration.timeLimiterExecutor()) {
            var service = new TimeLimiterDemoService(configuration.demoTimeLimiter(), executor, downstream);
            Thread caller = Thread.ofPlatform().start(() -> {
                try {
                    service.call(2000);
                    failure.set(new AssertionError("Expected interruption"));
                } catch (Exception exception) {
                    if (!(exception instanceof DownstreamUnavailableException)
                            || !(exception.getCause() instanceof InterruptedException)
                            || !Thread.currentThread().isInterrupted()) {
                        failure.set(exception);
                    }
                } finally {
                    finished.countDown();
                }
            });
            try {
                assertThat(entered.await(5, TimeUnit.SECONDS)).isTrue();
                caller.interrupt();
                assertThat(finished.await(5, TimeUnit.SECONDS)).isTrue();
                assertThat(workerInterrupted.await(5, TimeUnit.SECONDS)).isTrue();
                assertThat(failure.get()).isNull();
            } finally {
                caller.interrupt();
                caller.join(5000);
                executor.shutdownNow();
            }
        }
    }
}
