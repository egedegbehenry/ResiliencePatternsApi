package com.henry.resiliencepatternsapi.service;

import com.henry.resiliencepatternsapi.configuration.RetryConfiguration;
import com.henry.resiliencepatternsapi.exception.DownstreamUnavailableException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class RetryDemoServiceTests {

    @Test
    void succeedsWithoutRetryWhenDownstreamIsHealthy() {
        SimulatedDownstreamService downstream = mock(SimulatedDownstreamService.class);
        when(downstream.call(false)).thenReturn("OK");
        RetryDemoService service = service(downstream);

        assertThat(service.call(0).attempts()).isEqualTo(1);
        verify(downstream).call(false);
        verifyNoMoreInteractions(downstream);
    }

    @Test
    void recoversOnThirdAttemptAndResetsCounterForNextRequest() {
        SimulatedDownstreamService downstream = mock(SimulatedDownstreamService.class);
        when(downstream.call(true)).thenThrow(new DownstreamUnavailableException());
        when(downstream.call(false)).thenReturn("OK");
        RetryDemoService service = service(downstream);

        assertThat(service.call(2).attempts()).isEqualTo(3);
        assertThat(service.call(0).attempts()).isEqualTo(1);
        verify(downstream, times(2)).call(true);
        verify(downstream, times(2)).call(false);
        verifyNoMoreInteractions(downstream);
    }

    @Test
    void stopsAfterThreeAttemptsWhenDownstreamKeepsFailing() {
        SimulatedDownstreamService downstream = mock(SimulatedDownstreamService.class);
        when(downstream.call(true)).thenThrow(new DownstreamUnavailableException());

        assertThatThrownBy(() -> service(downstream).call(3))
                .isInstanceOf(DownstreamUnavailableException.class);
        verify(downstream, times(3)).call(true);
        verifyNoMoreInteractions(downstream);
    }

    @Test
    void doesNotRetryAnUnexpectedException() {
        SimulatedDownstreamService downstream = mock(SimulatedDownstreamService.class);
        when(downstream.call(false)).thenThrow(new IllegalStateException("Unexpected failure"));

        assertThatThrownBy(() -> service(downstream).call(0)).isInstanceOf(IllegalStateException.class);
        verify(downstream).call(false);
        verifyNoMoreInteractions(downstream);
    }

    private RetryDemoService service(SimulatedDownstreamService downstream) {
        return new RetryDemoService(new RetryConfiguration().demoRetry(), downstream);
    }
}
