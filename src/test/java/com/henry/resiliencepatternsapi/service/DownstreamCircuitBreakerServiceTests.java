package com.henry.resiliencepatternsapi.service;

import com.henry.resiliencepatternsapi.configuration.CircuitBreakerConfiguration;
import com.henry.resiliencepatternsapi.exception.DownstreamUnavailableException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class DownstreamCircuitBreakerServiceTests {

    @Test
    void inventoryFailuresDoNotOpenPaymentCircuit() {
        verifyIsolation(true);
    }

    @Test
    void paymentFailuresDoNotOpenInventoryCircuit() {
        verifyIsolation(false);
    }

    private void verifyIsolation(boolean failInventory) {
        var configuration = new CircuitBreakerConfiguration();
        var inventory = configuration.inventoryCircuitBreaker();
        var payment = configuration.paymentCircuitBreaker();
        var downstream = mock(SimulatedDownstreamService.class);
        when(downstream.call(true)).thenThrow(new DownstreamUnavailableException());
        when(downstream.call(false)).thenReturn("OK");
        var service = new DownstreamCircuitBreakerService(inventory, payment, downstream);
        var failingCircuit = failInventory ? inventory : payment;
        var healthyCircuit = failInventory ? payment : inventory;

        for (int i = 0; i < 5; i++) {
            assertThatThrownBy(() -> {
                if (failInventory) {
                    service.inventory(true);
                } else {
                    service.payment(true);
                }
            }).isInstanceOf(DownstreamUnavailableException.class);
        }
        assertThat(failingCircuit.getState()).isEqualTo(CircuitBreaker.State.OPEN);
        assertThat(healthyCircuit.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
        assertThat(healthyCircuit.getMetrics().getNumberOfFailedCalls()).isZero();

        assertThatThrownBy(() -> {
            if (failInventory) {
                service.inventory(false);
            } else {
                service.payment(false);
            }
        }).isInstanceOf(CallNotPermittedException.class);
        assertThat(failInventory ? service.payment(false) : service.inventory(false)).isEqualTo("OK");
        verify(downstream, times(5)).call(true);
        verify(downstream).call(false);
        verifyNoMoreInteractions(downstream);
    }

    @Test
    void recoveryOfInventoryDoesNotClosePaymentCircuit() {
        var configuration = new CircuitBreakerConfiguration();
        var inventory = configuration.inventoryCircuitBreaker();
        var payment = configuration.paymentCircuitBreaker();
        var service = new DownstreamCircuitBreakerService(inventory, payment, new SimulatedDownstreamService());
        inventory.transitionToOpenState();
        payment.transitionToOpenState();
        inventory.transitionToHalfOpenState();

        service.inventory(false);
        service.inventory(false);

        assertThat(inventory.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
        assertThat(payment.getState()).isEqualTo(CircuitBreaker.State.OPEN);
    }
}
