package com.henry.resilencepatternsapi;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class FallbackHttpTests {

    @LocalServerPort
    private int port;

    @Autowired
    private CircuitBreaker demoCircuitBreaker;

    @Test
    void returnsFallbackDuringFailureAndOpenCircuitThenRecovers() throws Exception {
        demoCircuitBreaker.reset();
        try (HttpClient client = HttpClient.newHttpClient()) {
            for (int i = 0; i < 5; i++) {
                var failed = get(client, "/api/demo/fallback?fail=true");
                assertThat(failed.statusCode()).isEqualTo(200);
                assertThat(failed.body()).contains("\"fallbackUsed\":true");
            }
            assertThat(demoCircuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);
            var blocked = get(client, "/api/demo/fallback?fail=false");
            assertThat(blocked.statusCode()).isEqualTo(200);
            assertThat(blocked.body()).contains("\"fallbackUsed\":true");
            assertThat(blocked.body()).contains("Fallback response: downstream service is temporarily unavailable");
            // The original endpoint keeps its 503 contract, even when fallback is available elsewhere.
            assertThat(get(client, "/api/demo/circuit-breaker?fail=false").statusCode()).isEqualTo(503);

            demoCircuitBreaker.transitionToHalfOpenState();
            for (int i = 0; i < 2; i++) {
                var recovered = get(client, "/api/demo/fallback?fail=false");
                assertThat(recovered.statusCode()).isEqualTo(200);
                assertThat(recovered.body()).contains("\"fallbackUsed\":false");
                assertThat(recovered.body()).contains("Simulated downstream service responded successfully");
            }
            assertThat(demoCircuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
        } finally {
            demoCircuitBreaker.reset();
        }
    }

    @Test
    void rejectsInvalidBoolean() throws Exception {
        try (HttpClient client = HttpClient.newHttpClient()) {
            assertThat(get(client, "/api/demo/fallback?fail=invalid").statusCode()).isEqualTo(400);
        }
    }

    private HttpResponse<String> get(HttpClient client, String path) throws Exception {
        var request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + path))
                .timeout(Duration.ofSeconds(5)).GET().build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }
}
