# Java Spring Boot Resilience Patterns & Performance Testing API

A Java 21 learning project demonstrating Resilience4j patterns, independent downstream
circuit breakers, Actuator monitoring, automated HTTP tests and Grafana k6 checks.
Downstream calls are simulated locally; no database, external service or credentials are required.

Moving computers? Follow [NEW_LAPTOP_SETUP.md](NEW_LAPTOP_SETUP.md) for cloning,
IntelliJ configuration, branch recovery and the separate-backup checklist.

## Technology

| Component | Version/tool |
| --- | --- |
| Java | 21 |
| Spring Boot | 4.1.1 |
| Resilience4j | 2.4.0 |
| Build | Maven Wrapper |
| Tests | JUnit, Spring Boot Test, Mockito, AssertJ |
| Monitoring | Spring Boot Actuator and Micrometer |
| Performance | Grafana k6; validated with portable v2.3.0 |

## Run locally

Open `pom.xml` in IntelliJ IDEA, select JDK 21 and reload the Maven project.
Run `com.henry.resilencepatternsapi.ResilencePatternsApiApplication`.
Alternatively, from the repository root in PowerShell:

```powershell
.\mvnw.cmd clean verify
.\mvnw.cmd spring-boot:run
```

On Linux/macOS, use `sh mvnw clean verify` and `sh mvnw spring-boot:run`.
Maven downloads dependencies on the first run. To run the packaged application:

```powershell
java -jar target/resilence-patterns-api-0.0.1-SNAPSHOT.jar
```

The default URL is `http://localhost:8080`. Check that port 8080 is free before starting.
For another port, use `java -jar target/resilence-patterns-api-0.0.1-SNAPSHOT.jar --server.port=8081`.
Verify startup in a browser or Postman:

```text
GET http://localhost:8080/api/demo/hello
```

```json
{"message":"Resilience API is running successfully"}
```

There is no controller mapping for `/`; a 404 on the root URL is expected.

## Endpoint catalogue

All endpoints below use GET. Paths are relative to `http://localhost:8080`.

| Path | Parameters/defaults | Expected behaviour |
| --- | --- | --- |
| `/api/demo/hello` | None | 200 with the startup message. |
| `/api/demo/circuit-breaker` | `fail=false` | 200 on success; 503 on simulated failure or open circuit. |
| `/api/demo/retry` | `failTimes=0`, range 0–3 | 200 with attempt count for 0–2 failures; 503 after three failures. |
| `/api/demo/rate-limit` | None | 200 when a permit is available; 429 otherwise, with `Retry-After: 10`. |
| `/api/demo/bulkhead` | `delayMs=1000`, range 0–3000 | 200 on completion; 503 when two calls are already running. |
| `/api/demo/time-limit` | `delayMs=2000`, range 0–3000 | 200 for fast work; 504 after the one-second timeout; 503 if executor capacity is full. |
| `/api/demo/fallback` | `fail=false` | 200 with an explicit `fallbackUsed` flag. |
| `/api/demo/inventory` | `fail=false` | Uses the independent `inventoryService` circuit breaker. |
| `/api/demo/payment` | `fail=false` | Uses the independent `paymentService` circuit breaker. |
| `/actuator/health` | None | Application health, normally `{"status":"UP"}`. |
| `/actuator/metrics` | Optional metric path and tags | JVM, HTTP and resilience measurements. |

Invalid parameter types or out-of-range validated inputs return 400.
An exhausted retry returns `{"message":"Simulated downstream service is unavailable"}`.
The fallback endpoint returns this explicit degraded response for expected failures:

```json
{
  "message": "Fallback response: downstream service is temporarily unavailable",
  "fallbackUsed": true
}
```

Fallback catches downstream unavailability and open-circuit exceptions. Unexpected failures
are allowed to propagate. It shares `demoDownstream` with the original circuit-breaker demo.

## Resilience configuration

| Pattern | Configuration |
| --- | --- |
| Circuit Breaker | Five-call count-based window, minimum five calls, 50% failure threshold, ten-second open wait, two half-open recovery calls. |
| Retry | Three total attempts, 200 ms between attempts; retries only `DownstreamUnavailableException`. |
| Rate Limiter | Five permits per ten-second refresh cycle; immediate rejection when exhausted. |
| Bulkhead | Semaphore with two concurrent permits and zero wait. |
| Time Limiter | One-second wait, cancellation requested on timeout; dedicated two-worker executor with four queue slots and immediate rejection when full. |

Settings are defined in `src/main/java/com/henry/resiliencepatternsapi/configuration/`.
Each named circuit breaker has independent state. Rate-limit permits are shared across
callers in one application instance. This is an in-memory demonstration, not a distributed
or per-user quota. Timeouts include executor queue waiting; cancellation interrupts the
simulated delay, while real integrations must provide their own cancellation support.

### Demonstrate failure isolation in Postman

Use a fresh application instance to start with closed circuits.

1. Send `/api/demo/inventory?fail=true` five times. Each returns 503 and inventory's circuit opens.
2. Immediately send `/api/demo/inventory?fail=false`. It still returns 503 because the circuit is open.
3. Send `/api/demo/payment?fail=false`. It returns 200 with `service: "paymentService"`.
4. Wait ten seconds, then send `/api/demo/inventory?fail=false` twice. Successful recovery calls close its circuit.

For Bulkhead, send three overlapping requests to `/api/demo/bulkhead?delayMs=3000` from
separate Postman tabs: two should complete successfully and the excess call returns 503.
For Time Limiter, `/api/demo/time-limit?delayMs=2000` returns 504 after about one second.

## Package organization

```text
src/main/java/com/henry/
  resilencepatternsapi/ResilencePatternsApiApplication.java
  resiliencepatternsapi/
    configuration/   Resilience settings, executor lifecycle and metrics binding
    controller/      HTTP routes and parameter validation
    dto/             Immutable JSON response records
    exception/       Downstream exceptions and HTTP error handling
    service/         Pattern execution and simulated downstream calls
src/test/java/       Service tests and real HTTP integration tests
performance/         k6 scripts, instructions and recorded validation
docs/                Monitoring guide and session handoff notes
```

The existing directory/artifact/entry-point spelling is `resilence`; feature packages use
`resilience`. The application explicitly scans both packages. Controllers must remain
under Maven's `src/main/java` source root and inside a scanned package.

## Automated tests

```powershell
.\mvnw.cmd clean verify
```

Tests cover successes, failures, invalid input, breaker isolation/recovery, retry exhaustion,
concurrent rejection, permit release, timeout cancellation and Actuator metric updates.
HTTP integration tests use random ports. Concurrency tests use synchronization rather than
arbitrary sleeps; real time-limit tests also exercise cancellation with running work.
The monitoring stage passed 41 tests. Tests do not require a separately running application.

If Windows cannot delete or rename a file under `target/`, close the process holding that
file before rebuilding. Earlier validation used isolated temporary source copies when
workspace build outputs were locked.

## Monitoring and performance

- [Monitoring guide](docs/monitoring.md): Actuator URLs, instance tags, counts and state gauges.
- [k6 instructions](performance/README.md): five reproducible scripts, configurable baseline and thresholds.
- [Recorded local measurements](performance/VALIDATION.md): 1047 passing checks, expected rejections and limitations.

Example baseline run with k6 installed and the application running:

```powershell
k6 run -e BASE_URL=http://localhost:8080 performance/baseline.js
```

Expected resilience responses are checked explicitly and separated from unexpected HTTP
failures. The initial paced baseline recorded 475 hello requests with 7.01 ms p95 latency;
this short same-machine run does not establish production capacity. The circuit-breaker
script changes state: restart the application before repeating the complete suite.

Only Actuator health and metrics are exposed by this project. Metric storage, Prometheus
scraping and a Grafana dashboard are not configured. Metrics reset when the application restarts.

## Scope

This project demonstrates resilience mechanisms using bounded simulated work. It does not
implement real inventory/payment operations, persistence, authentication or production deployment.
Retries apply to simulated read-only calls; use idempotency and failure classification before
adapting them to operations with side effects. No secrets are required or included.
