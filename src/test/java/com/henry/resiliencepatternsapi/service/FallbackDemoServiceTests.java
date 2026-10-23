package com.henry.resiliencepatternsapi.service;

import com.henry.resiliencepatternsapi.exception.DownstreamUnavailableException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class FallbackDemoServiceTests {

    @Test
    void returnsDownstreamResponseWhenHealthy() {
        var downstream = mock(CircuitBreakerDemoService.class);
        when(downstream.call(false)).thenReturn("OK");

        var response = new FallbackDemoService(downstream).call(false);

        assertThat(response.message()).isEqualTo("OK");
        assertThat(response.fallbackUsed()).isFalse();
        verify(downstream).call(false);
    }

    @Test
    void returnsExplicitFallbackForDownstreamFailure() {
        var downstream = mock(CircuitBreakerDemoService.class);
        when(downstream.call(true)).thenThrow(new DownstreamUnavailableException());

        var response = new FallbackDemoService(downstream).call(true);

        assertThat(response.fallbackUsed()).isTrue();
        assertThat(response.message()).isEqualTo(
                "Fallback response: downstream service is temporarily unavailable");
    }

    @Test
    void returnsFallbackForOpenCircuitWithoutInvokingDownstream() {
        var breaker = CircuitBreaker.ofDefaults("fallbackTest");
        breaker.transitionToOpenState();
        var downstream = mock(SimulatedDownstreamService.class);
        var protectedService = new CircuitBreakerDemoService(breaker, downstream);

        var response = new FallbackDemoService(protectedService).call(false);

        assertThat(response.fallbackUsed()).isTrue();
        verifyNoInteractions(downstream);
    }

    @Test
    void propagatesUnexpectedFailures() {
        var downstream = mock(CircuitBreakerDemoService.class);
        var failure = new IllegalStateException("Unexpected failure");
        when(downstream.call(false)).thenThrow(failure);

        assertThatThrownBy(() -> new FallbackDemoService(downstream).call(false)).isSameAs(failure);
    }
}
