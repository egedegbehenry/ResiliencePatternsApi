package com.henry.resilencepatternsapi;

import org.junit.jupiter.api.Test;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ResilencePatternsApiApplicationTests {

    @LocalServerPort
    private int port;

    @Autowired
    private CircuitBreaker demoCircuitBreaker;

    @Test
    void contextLoads() {
    }

    @Test
    void helloReturnsSuccessMessage() throws Exception {
        try (HttpClient client = HttpClient.newHttpClient()) {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:" + port + "/api/demo/hello"))
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            assertThat(response.statusCode()).isEqualTo(200);
            assertThat(response.headers().firstValue("Content-Type")).hasValueSatisfying(
                    contentType -> assertThat(contentType).startsWith("application/json"));
            assertThat(response.body()).isEqualTo(
                    "{\"message\":\"Resilience API is running successfully\"}");
        }
    }

    @Test
    void circuitBreakerReturns503ThenRecovers() throws Exception {
        demoCircuitBreaker.reset();
        try (HttpClient client = HttpClient.newHttpClient()) {
            for (int i = 0; i < 5; i++) {
                HttpResponse<String> failure = callDemo(client, true);
                assertThat(failure.statusCode()).isEqualTo(503);
                assertThat(failure.body()).contains("Simulated downstream service is unavailable");
            }
            assertThat(demoCircuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);

            HttpResponse<String> blocked = callDemo(client, false);
            assertThat(blocked.statusCode()).isEqualTo(503);
            assertThat(blocked.body()).contains("Circuit breaker is open; try again later");

            // Exercise recovery without making this HTTP test sleep for ten seconds.
            demoCircuitBreaker.transitionToHalfOpenState();
            for (int i = 0; i < 2; i++) {
                HttpResponse<String> success = callDemo(client, false);
                assertThat(success.statusCode()).isEqualTo(200);
                assertThat(success.body()).isEqualTo(
                        "{\"message\":\"Simulated downstream service responded successfully\"}");
            }
            assertThat(demoCircuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
        } finally {
            demoCircuitBreaker.reset();
        }
    }

    private HttpResponse<String> callDemo(HttpClient client, boolean fail) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/demo/circuit-breaker?fail=" + fail))
                .GET().build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

}
