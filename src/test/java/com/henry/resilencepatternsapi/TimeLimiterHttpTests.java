package com.henry.resilencepatternsapi;

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
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class TimeLimiterHttpTests {

    @LocalServerPort
    private int port;

    @Autowired
    @Qualifier("timeLimiterExecutor")
    private ThreadPoolExecutor executor;

    @Test
    void returns200ForFastWorkAnd504ForSlowWork() throws Exception {
        try (HttpClient client = HttpClient.newHttpClient()) {
            var success = get(client, "0");
            assertThat(success.statusCode()).isEqualTo(200);
            assertThat(success.body()).isEqualTo(
                    "{\"message\":\"Simulated downstream service responded successfully\"}");
            var timeout = get(client, "2000");
            assertThat(timeout.statusCode()).isEqualTo(504);
            assertThat(timeout.body()).isEqualTo("{\"message\":\"Downstream service timed out\"}");
            assertThat(get(client, "0").statusCode()).isEqualTo(200);
        }
    }

    @Test
    void rejectsInvalidDelay() throws Exception {
        try (HttpClient client = HttpClient.newHttpClient()) {
            for (String value : new String[]{"-1", "3001", "abc"}) {
                assertThat(get(client, value).statusCode()).isEqualTo(400);
            }
        }
    }

    @Test
    void rejectsWhenExecutorAndQueueAreFullThenRecovers() throws Exception {
        var release = new CountDownLatch(1);
        var entered = new CountDownLatch(2);
        var tasks = new ArrayList<Future<?>>();
        try (HttpClient client = HttpClient.newHttpClient()) {
            try {
                // Fill two running slots, then all four queue slots.
                Callable<Void> blockingTask = () -> {
                    entered.countDown();
                    if (!release.await(5, TimeUnit.SECONDS)) {
                        throw new AssertionError("Timed out waiting for executor release");
                    }
                    return null;
                };
                for (int i = 0; i < 2; i++) {
                    tasks.add(executor.submit(blockingTask));
                }
                assertThat(entered.await(5, TimeUnit.SECONDS)).isTrue();
                for (int i = 0; i < 4; i++) {
                    tasks.add(executor.submit(blockingTask));
                }
                var rejected = get(client, "0");
                assertThat(rejected.statusCode()).isEqualTo(503);
                assertThat(rejected.body()).isEqualTo(
                        "{\"message\":\"Downstream executor is busy; try again later\"}");
            } finally {
                release.countDown();
                for (Future<?> task : tasks) {
                    task.get(5, TimeUnit.SECONDS);
                }
            }
            assertThat(get(client, "0").statusCode()).isEqualTo(200);
        }
    }

    private HttpResponse<String> get(HttpClient client, String delayMs) throws Exception {
        var request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/demo/time-limit?delayMs=" + delayMs))
                .timeout(Duration.ofSeconds(5))
                .GET().build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }
}
