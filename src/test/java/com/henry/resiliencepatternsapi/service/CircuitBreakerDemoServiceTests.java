package com.henry.resiliencepatternsapi.service;

import com.henry.resiliencepatternsapi.configuration.CircuitBreakerConfiguration;
import com.henry.resiliencepatternsapi.exception.DownstreamUnavailableException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class CircuitBreakerDemoServiceTests {

    @Test
    void opensAtFailureThresholdAndDoesNotCallDownstreamWhileOpen() {
        CircuitBreaker breaker = new CircuitBreakerConfiguration().demoCircuitBreaker();
        SimulatedDownstreamService downstream = mock(SimulatedDownstreamService.class);
        when(downstream.call(false)).thenReturn("OK");
        when(downstream.call(true)).thenThrow(new DownstreamUnavailableException());
        CircuitBreakerDemoService service = new CircuitBreakerDemoService(breaker, downstream);

        service.call(false);
        service.call(false);
        for (int i = 0; i < 3; i++) {
            assertThatThrownBy(() -> service.call(true)).isInstanceOf(DownstreamUnavailableException.class);
            if (i < 2) {
                assertThat(breaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
            }
        }
        assertThat(breaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);
        assertThat(breaker.getCircuitBreakerConfig().getWaitIntervalFunctionInOpenState().apply(1))
                .isEqualTo(TimeUnit.SECONDS.toMillis(10));
        assertThatThrownBy(() -> service.call(false)).isInstanceOf(CallNotPermittedException.class);
        verify(downstream, times(2)).call(false);
        verify(downstream, times(3)).call(true);
        verifyNoMoreInteractions(downstream);

        breaker.transitionToHalfOpenState();
        service.call(false);
        assertThat(breaker.getState()).isEqualTo(CircuitBreaker.State.HALF_OPEN);
        service.call(false);
        assertThat(breaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
    }

    @Test
    void failedRecoveryReopensCircuit() {
        CircuitBreaker breaker = new CircuitBreakerConfiguration().demoCircuitBreaker();
        CircuitBreakerDemoService service = new CircuitBreakerDemoService(breaker, new SimulatedDownstreamService());
        breaker.transitionToOpenState();
        breaker.transitionToHalfOpenState();

        assertThatThrownBy(() -> service.call(true)).isInstanceOf(DownstreamUnavailableException.class);
        service.call(false);

        assertThat(breaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);
    }
}
