package com.henry.resilencepatternsapi;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ResilienceMonitoringHttpTests {

    @LocalServerPort
    private int port;

    @Autowired
    @Qualifier("inventoryCircuitBreaker")
    private CircuitBreaker inventory;

    @Autowired
    @Qualifier("paymentCircuitBreaker")
    private CircuitBreaker payment;

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void exposesHealthAndMetricsForAllFivePatterns() throws Exception {
        try (HttpClient client = HttpClient.newHttpClient()) {
            var health = get(client, "/actuator/health");
            assertThat(health.statusCode()).isEqualTo(200);
            assertThat(json(health).get("status")).isEqualTo("UP");

            var metrics = get(client, "/actuator/metrics");
            assertThat(metrics.statusCode()).isEqualTo(200);
            List<?> names = (List<?>) json(metrics).get("names");
            for (String name : List.of("resilience4j.circuitbreaker.calls", "resilience4j.circuitbreaker.state",
                    "resilience4j.retry.calls", "resilience4j.ratelimiter.available.permissions",
                    "resilience4j.bulkhead.available.concurrent.calls", "resilience4j.timelimiter.calls",
                    "jvm.memory.used", "http.server.requests")) {
                assertThat(names.contains(name)).as("Metric %s exists", name).isTrue();
            }
        }
    }

    @Test
    void reportsInventoryFailureAndOpenStateWithoutAffectingPayment() throws Exception {
        inventory.reset();
        payment.reset();
        try (HttpClient client = HttpClient.newHttpClient()) {
            String failedCalls = "resilience4j.circuitbreaker.calls?tag=name:inventoryService&tag=kind:failed";
            String blockedCalls = "resilience4j.circuitbreaker.not.permitted.calls?tag=name:inventoryService";
            String paymentSuccess = "resilience4j.circuitbreaker.calls?tag=name:paymentService&tag=kind:successful";
            double failuresBefore = metric(client, failedCalls, "COUNT");
            double blockedBefore = metric(client, blockedCalls, "COUNT");
            double paymentsBefore = metric(client, paymentSuccess, "COUNT");

            for (int i = 0; i < 5; i++) {
                assertThat(get(client, "/api/demo/inventory?fail=true").statusCode()).isEqualTo(503);
            }
            assertThat(get(client, "/api/demo/inventory?fail=false").statusCode()).isEqualTo(503);
            assertThat(get(client, "/api/demo/payment?fail=false").statusCode()).isEqualTo(200);

            assertThat(metric(client, failedCalls, "COUNT")).isEqualTo(failuresBefore + 5);
            assertThat(metric(client, blockedCalls, "COUNT")).isEqualTo(blockedBefore + 1);
            assertThat(metric(client, paymentSuccess, "COUNT")).isEqualTo(paymentsBefore + 1);
            assertThat(metric(client,
                    "resilience4j.circuitbreaker.state?tag=name:inventoryService&tag=state:open", "VALUE"))
                    .isEqualTo(1);
            assertThat(metric(client,
                    "resilience4j.circuitbreaker.state?tag=name:paymentService&tag=state:closed", "VALUE"))
                    .isEqualTo(1);
        } finally {
            inventory.reset();
            payment.reset();
        }
    }

    @Test
    void updatesRetryAndTimeLimiterOutcomesAfterRequests() throws Exception {
        try (HttpClient client = HttpClient.newHttpClient()) {
            String retries = "resilience4j.retry.calls?tag=name:demoRetryDownstream&tag=kind:successful_with_retry";
            String timeouts = "resilience4j.timelimiter.calls?tag=name:demoTimeLimitedDownstream&tag=kind:timeout";
            double retriesBefore = metric(client, retries, "COUNT");
            double timeoutsBefore = metric(client, timeouts, "COUNT");

            assertThat(get(client, "/api/demo/retry?failTimes=1").statusCode()).isEqualTo(200);
            assertThat(get(client, "/api/demo/time-limit?delayMs=2000").statusCode()).isEqualTo(504);

            assertThat(metric(client, retries, "COUNT")).isEqualTo(retriesBefore + 1);
            assertThat(metric(client, timeouts, "COUNT")).isEqualTo(timeoutsBefore + 1);
        }
    }

    private double metric(HttpClient client, String metricAndTags, String statistic) throws Exception {
        var response = get(client, "/actuator/metrics/" + metricAndTags);
        assertThat(response.statusCode()).as("Metric %s", metricAndTags).isEqualTo(200);
        for (Object entry : (List<?>) json(response).get("measurements")) {
            Map<?, ?> measurement = (Map<?, ?>) entry;
            if (statistic.equals(measurement.get("statistic"))) {
                return ((Number) measurement.get("value")).doubleValue();
            }
        }
        throw new AssertionError("Missing statistic " + statistic + " for " + metricAndTags);
    }

    private Map<?, ?> json(HttpResponse<String> response) {
        return mapper.readValue(response.body(), Map.class);
    }

    private HttpResponse<String> get(HttpClient client, String path) throws Exception {
        var request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + path))
                .timeout(Duration.ofSeconds(5)).GET().build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }
}
