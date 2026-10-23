# k6 performance and resilience checks

Run from the repository root with Java 21 and [Grafana k6](https://grafana.com/docs/k6/latest/set-up/install-k6/).
No Java dependency is added by these scripts.

Start `ResilencePatternsApiApplication` in IntelliJ. Use a fresh application instance and
avoid other requests during resilience checks, because rate-limit permits and circuit-breaker
state are shared within that instance. The scripts default to `http://localhost:8080`.

## Run the scripts

```powershell
New-Item -ItemType Directory -Force performance/results | Out-Null
k6 run --summary-export performance/results/baseline.json performance/baseline.js
k6 run --summary-export performance/results/rate-limiter.json performance/rate-limiter.js
k6 run --summary-export performance/results/bulkhead.json performance/bulkhead.js
k6 run --summary-export performance/results/time-limiter.json performance/time-limiter.js
k6 run --summary-export performance/results/circuit-breakers.json performance/circuit-breakers.js
```

If using a portable release, replace `k6` with its executable path, e.g.
`& 'C:\tools\k6\k6.exe' run performance/baseline.js`.

| Script | Workload and pass criteria |
| --- | --- |
| `baseline.js` | Five virtual users for ten seconds, with a 100 ms pause after each request. Every hello response must be correct; hello p95 latency must be below 500 ms. |
| `rate-limiter.js` | Twenty rapid sequential requests. Requires accepted requests and explicit 429 rejections, with rejection latency below one second. After an 11-second wait, a request must succeed. |
| `bulkhead.js` | Six parallel calls with a three-second delay. Requires exactly two successes, explicit bulkhead rejections for the rest, and successful recovery after those calls complete. |
| `time-limiter.js` | Four parallel calls with a two-second delay. All must return the intended 504 timeout; p95 latency must be below two seconds. A subsequent fast request must succeed. |
| `circuit-breakers.js` | Opens inventory's breaker while payment remains available, waits for inventory recovery, then verifies the reverse isolation. The original demo must remain available. |

Every script first checks `/actuator/health`. All checks must pass and unexpected HTTP
failures must be zero. Nonzero k6 exit codes indicate a failed run.

## Configure the baseline

```powershell
k6 run -e BASE_URL=http://localhost:8080 -e VUS=10 -e DURATION=30s -e BASELINE_P95_MS=500 performance/baseline.js
```

`BASE_URL` works with every script. `VUS`, `DURATION` and `BASELINE_P95_MS` configure only
the baseline script. Resilience scripts use one orchestrating virtual user and explicit
parallel request batches so they can check known capacity limits.

## Interpret results

- `http_req_duration` reports request latency in milliseconds. p95 is the latency at or
  below which 95% of sampled requests completed.
- `http_reqs` reports request count and rate. The baseline rate includes the health check;
  use its named `hello` timing metric for endpoint latency.
- `rate_limit_rejections`, `bulkhead_rejections` and `expected_timeouts` count intentional
  resilience responses. Per-request `expectedStatuses` marks these as expected for their
  specific requests. Response-body checks distinguish intended rejection from other 503s.
- Expected rejection responses are not counted as `http_req_failed`. Unexpected statuses,
  transport errors and failed body checks still fail the run.
- Latency thresholds are starter targets for local development, rather than established
  production service objectives. Passing these short checks does not establish maximum
  capacity or replace a longer soak/stress test.

The bulkhead check assumes all six requests overlap within its three-second delay. On a
heavily stalled load generator, inspect the request timings before interpreting a failure.
Restart the application before repeating the full suite: the circuit-breaker script leaves
the payment breaker open. Generated JSON summaries and logs under `performance/results/`
are ignored by Git. Monitoring URLs and tag examples are in [the monitoring guide](../docs/monitoring.md).
Measured results from the initial local run are recorded in [VALIDATION.md](VALIDATION.md).

The [official k6 thresholds documentation](https://grafana.com/docs/k6/latest/using-k6/thresholds/)
explains pass/fail thresholds; [response callbacks](https://grafana.com/docs/k6/latest/javascript-api/k6-http/set-response-callback/)
explain how expected statuses affect HTTP failure metrics.
