package com.henry.resilencepatternsapi;

import com.henry.resiliencepatternsapi.configuration.RateLimiterConfiguration;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.internal.AtomicRateLimiter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(RateLimiterHttpTests.ControlledTimeConfiguration.class)
class RateLimiterHttpTests {

    @LocalServerPort
    private int port;

    @Autowired
    private AtomicLong rateLimiterClock;

    @Test
    void returns429WhenExhaustedAnd200AfterRefresh() throws Exception {
        try (HttpClient client = HttpClient.newHttpClient()) {
            for (int i = 0; i < 5; i++) {
                HttpResponse<String> response = get(client, "/api/demo/rate-limit");
                assertThat(response.statusCode()).isEqualTo(200);
                assertThat(response.body()).isEqualTo(
                        "{\"message\":\"Simulated downstream service responded successfully\"}");
            }

            HttpResponse<String> rejected = get(client, "/api/demo/rate-limit");
            assertThat(rejected.statusCode()).isEqualTo(429);
            assertThat(rejected.headers().firstValue("Content-Type")).hasValueSatisfying(
                    contentType -> assertThat(contentType).startsWith("application/json"));
            assertThat(rejected.headers().firstValue("Retry-After")).contains("10");
            assertThat(rejected.body()).isEqualTo(
                    "{\"message\":\"Rate limit exceeded; try again later\"}");
            assertThat(get(client, "/api/demo/hello").statusCode()).isEqualTo(200);

            // Advance only the limiter's clock; no real-time sleep or boundary race.
            rateLimiterClock.addAndGet(Duration.ofSeconds(10).toNanos());
            assertThat(get(client, "/api/demo/rate-limit").statusCode()).isEqualTo(200);
        }
    }

    private HttpResponse<String> get(HttpClient client, String path) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + path))
                .GET().build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class ControlledTimeConfiguration {

        @Bean
        AtomicLong rateLimiterClock() {
            return new AtomicLong();
        }

        @Bean
        @Primary
        RateLimiter controlledRateLimiter(RateLimiterConfiguration configuration, AtomicLong rateLimiterClock) {
            return new AtomicRateLimiter("httpTest", configuration.demoRateLimiter().getRateLimiterConfig(),
                    rateLimiterClock::get);
        }
    }
}
