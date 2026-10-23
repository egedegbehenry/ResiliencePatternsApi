# Monitoring the resilience demos

Start `ResilencePatternsApiApplication` in IntelliJ, then send GET requests in Postman.
The default base URL is `http://localhost:8080`.

| URL | Purpose |
| --- | --- |
| `/actuator/health` | Application health; normally `{"status":"UP"}`. |
| `/actuator/metrics` | Lists available metric names, including JVM and HTTP metrics. |
| `/actuator/metrics/resilience4j.circuitbreaker.state` | Breaker state gauges, filtered using tags below. |
| `/actuator/metrics/resilience4j.circuitbreaker.calls` | Successful, failed and ignored call counts and durations. |
| `/actuator/metrics/resilience4j.circuitbreaker.not.permitted.calls` | Calls rejected by an open circuit. |
| `/actuator/metrics/resilience4j.retry.calls` | Retry outcomes, including success after retry. |
| `/actuator/metrics/resilience4j.bulkhead.available.concurrent.calls` | Available concurrent call slots. |
| `/actuator/metrics/resilience4j.ratelimiter.available.permissions` | Available permits in the current refresh cycle. |
| `/actuator/metrics/resilience4j.timelimiter.calls` | Successful, failed and timed-out calls. |

## Observe failure isolation

1. Restart the app for a fresh demo, then send `/api/demo/inventory?fail=true` five times.
   Each request returns HTTP 503, and the fifth opens the inventory breaker.
2. Send `/api/demo/inventory?fail=false` immediately. It returns HTTP 503 because the circuit is open.
3. Send `/api/demo/payment?fail=false`. It returns HTTP 200.
4. Inspect inventory's open state:
   `/actuator/metrics/resilience4j.circuitbreaker.state?tag=name:inventoryService&tag=state:open`.
   The `VALUE` measurement is `1` while that state is active, and `0` otherwise.
5. Inspect payment's closed state:
   `/actuator/metrics/resilience4j.circuitbreaker.state?tag=name:paymentService&tag=state:closed`.
   Its `VALUE` should remain `1`.
6. Inspect failed inventory calls:
   `/actuator/metrics/resilience4j.circuitbreaker.calls?tag=name:inventoryService&tag=kind:failed`.
   `COUNT` records failed calls; `TOTAL_TIME` and `MAX` describe their duration.

In Postman's Params tab, add two separate `tag` rows when filtering both name and state/kind.
A query without tags aggregates matching measurements across instances and outcomes.

## Observe retries and timeouts

- Call `/api/demo/retry?failTimes=1`, then inspect
  `/actuator/metrics/resilience4j.retry.calls?tag=name:demoRetryDownstream&tag=kind:successful_with_retry`.
  Its `COUNT` increases by one per completed request that succeeds after retrying,
  rather than once per individual attempt.
- Call `/api/demo/time-limit?delayMs=2000`, then inspect
  `/actuator/metrics/resilience4j.timelimiter.calls?tag=name:demoTimeLimitedDownstream&tag=kind:timeout`.
  Its `COUNT` increases by one after the HTTP 504 response.

These metrics bind to the same resilience objects used by the services. They live in memory
and restart with the application. Health describes the application itself; a simulated
downstream outage does not make the application unhealthy. The fallback demo shares the
`demoDownstream` breaker, while inventory and payment use independent breakers.

This stage exposes `health` and `metrics` over HTTP. It does not configure Prometheus
scraping, persistent metric storage or a Grafana dashboard.
