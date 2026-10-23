# Initial local validation

Validated on 23 October 2026 using official portable Grafana k6 v2.3.0 on Windows,
Java 21.0.12.1 and Spring Boot 4.1.1. The release ZIP's SHA-256 matched its official
GitHub release asset digest. No system installation was changed.

The application was started from the previously validated monitoring build on a free
local port (`49912`), with clean in-memory resilience state. The load generator and
application ran on the same machine. The temporary application was stopped afterward;
the existing IntelliJ application was not used.

| Script | Observed result |
| --- | --- |
| Baseline | Five virtual users for ten seconds, paced by 100 ms sleeps. 475 hello requests, hello p95 **7.01 ms**, maximum **15.14 ms**. Overall request rate **46.46/s**, including the setup health request. |
| Rate Limiter | Initial burst: **5 accepted**, **15 rejected with 429**. Recovery succeeded after the 11-second wait. |
| Bulkhead | Six overlapping calls: **2 accepted**, **4 rejected with 503**. A request succeeded after capacity was released. |
| Time Limiter | All **4 slow requests returned 504**, with measured p95 **1020.42 ms**. A subsequent fast request succeeded. |
| Circuit Breakers | Inventory failure, payment availability, inventory recovery, reverse isolation and the original demo's independence all passed. |

All five scripts exited with code **0**. All **1047 checks passed**; no unexpected HTTP
failures were recorded. Expected 429, 503 and 504 responses were counted separately.
JavaScript syntax checks and `k6 inspect` also passed for every script.

Raw JSON summaries and console output were saved under `performance/results/` locally,
which is ignored by Git. The application code was unchanged in this stage; its preceding
monitoring build passed all 41 Java tests.

These are short local development checks, not a maximum-throughput benchmark or a
production capacity claim. Baseline throughput is intentionally limited by pacing.
The resilience scenarios are controlled behaviour checks with small sample sizes.
