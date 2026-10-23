package com.henry.resilencepatternsapi;

import com.henry.resiliencepatternsapi.exception.DownstreamUnavailableException;
import com.henry.resiliencepatternsapi.service.SimulatedDownstreamService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class BulkheadHttpTests {

    @LocalServerPort
    private int port;

    @MockitoBean
    private SimulatedDownstreamService downstream;

    @Test
    void returns503WhileTwoCallsAreInFlightThenRecovers() throws Exception {
        var entered = new CountDownLatch(2);
        var release = new CountDownLatch(1);
        when(downstream.call(eq(false), anyInt())).thenAnswer(invocation -> {
            entered.countDown();
            if (!release.await(5, TimeUnit.SECONDS)) {
                throw new AssertionError("Timed out waiting for HTTP calls to be released");
            }
            return "Simulated downstream service responded successfully";
        });

        try (HttpClient client = HttpClient.newHttpClient()) {
            var first = client.sendAsync(request("0"), HttpResponse.BodyHandlers.ofString());
            var second = client.sendAsync(request("0"), HttpResponse.BodyHandlers.ofString());
            try {
                assertThat(entered.await(5, TimeUnit.SECONDS)).isTrue();
                var rejected = client.send(request("0"), HttpResponse.BodyHandlers.ofString());
                assertThat(rejected.statusCode()).isEqualTo(503);
                assertThat(rejected.body()).isEqualTo("{\"message\":\"Bulkhead is full; try again later\"}");
                verify(downstream, times(2)).call(false, 0);
            } finally {
                release.countDown();
            }
            assertThat(first.get(5, TimeUnit.SECONDS).statusCode()).isEqualTo(200);
            assertThat(second.get(5, TimeUnit.SECONDS).statusCode()).isEqualTo(200);
            var recovered = client.send(request("0"), HttpResponse.BodyHandlers.ofString());
            assertThat(recovered.statusCode()).isEqualTo(200);
            assertThat(recovered.body()).isEqualTo(
                    "{\"message\":\"Simulated downstream service responded successfully\"}");
        }
    }

    @Test
    void returns503OnDownstreamFailureAndAllowsNextCall() throws Exception {
        when(downstream.call(false, 0)).thenThrow(new DownstreamUnavailableException()).thenReturn("OK");
        try (HttpClient client = HttpClient.newHttpClient()) {
            assertThat(client.send(request("0"), HttpResponse.BodyHandlers.ofString()).statusCode()).isEqualTo(503);
            assertThat(client.send(request("0"), HttpResponse.BodyHandlers.ofString()).statusCode()).isEqualTo(200);
        }
    }

    @Test
    void rejectsInvalidDelayBeforeCallingDownstream() throws Exception {
        try (HttpClient client = HttpClient.newHttpClient()) {
            for (String value : new String[]{"-1", "3001", "abc"}) {
                assertThat(client.send(request(value), HttpResponse.BodyHandlers.ofString()).statusCode()).isEqualTo(400);
            }
        }
        verifyNoInteractions(downstream);
    }

    private HttpRequest request(String delayMs) {
        return HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/demo/bulkhead?delayMs=" + delayMs))
                .timeout(Duration.ofSeconds(5))
                .GET().build();
    }
}
