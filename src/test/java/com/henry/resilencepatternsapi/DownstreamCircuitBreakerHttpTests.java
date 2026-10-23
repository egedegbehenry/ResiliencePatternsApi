package com.henry.resilencepatternsapi;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class DownstreamCircuitBreakerHttpTests {

    @LocalServerPort
    private int port;

    @Autowired
    @Qualifier("inventoryCircuitBreaker")
    private CircuitBreaker inventory;

    @Autowired
    @Qualifier("paymentCircuitBreaker")
    private CircuitBreaker payment;

    @Autowired
    @Qualifier("demoCircuitBreaker")
    private CircuitBreaker demo;

    @Test
    void inventoryOutageLeavesPaymentAndOriginalDemoAvailable() throws Exception {
        verifyIsolation("inventory", "payment", inventory, payment);
    }

    @Test
    void paymentOutageLeavesInventoryAndOriginalDemoAvailable() throws Exception {
        verifyIsolation("payment", "inventory", payment, inventory);
    }

    private void verifyIsolation(String failingPath, String healthyPath,
                                 CircuitBreaker failingCircuit, CircuitBreaker healthyCircuit) throws Exception {
        inventory.reset();
        payment.reset();
        demo.reset();
        try (HttpClient client = HttpClient.newHttpClient()) {
            for (int i = 0; i < 5; i++) {
                var failure = get(client, failingPath + "?fail=true");
                assertThat(failure.statusCode()).isEqualTo(503);
                assertThat(failure.body()).contains("Simulated downstream service is unavailable");
            }
            assertThat(failingCircuit.getState()).isEqualTo(CircuitBreaker.State.OPEN);
            var blocked = get(client, failingPath + "?fail=false");
            assertThat(blocked.statusCode()).isEqualTo(503);
            assertThat(blocked.body()).contains("Circuit breaker is open; try again later");

            var healthy = get(client, healthyPath + "?fail=false");
            assertThat(healthy.statusCode()).isEqualTo(200);
            assertThat(healthy.body()).contains("\"service\":\"" + healthyPath + "Service\"");
            assertThat(healthy.body()).contains("Simulated downstream service responded successfully");
            assertThat(healthyCircuit.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
            assertThat(get(client, "circuit-breaker?fail=false").statusCode()).isEqualTo(200);
        } finally {
            inventory.reset();
            payment.reset();
            demo.reset();
        }
    }

    @Test
    void rejectsInvalidFailureFlag() throws Exception {
        try (HttpClient client = HttpClient.newHttpClient()) {
            assertThat(get(client, "inventory?fail=invalid").statusCode()).isEqualTo(400);
            assertThat(get(client, "payment?fail=invalid").statusCode()).isEqualTo(400);
        }
    }

    private HttpResponse<String> get(HttpClient client, String path) throws Exception {
        var request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/demo/" + path))
                .timeout(Duration.ofSeconds(5)).GET().build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }
}
