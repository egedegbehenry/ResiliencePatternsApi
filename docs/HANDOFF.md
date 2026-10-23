# Resume notes — 23 October 2026

Work paused at the user's request after completing and validating the k6 stage.

## Project and Git

- Actual project: `C:\Users\egede\IdeaProjects\resilence-patterns-api` (directory spelling is intentional here).
- Java 21, Spring Boot 4.1.1, Maven wrapper.
- Resume branch: `feature/k6-performance-tests`.
- `master` contains the starter/404 fix; subsequent feature branches build on each other.
  Features have not been merged into `master` or pushed to GitHub.
- Git identity is configured locally for Henry Egedegbe with the confirmed GitHub noreply address.
- Application entry point remains `com.henry.resilencepatternsapi.ResilencePatternsApiApplication`.
  Feature packages use `com.henry.resiliencepatternsapi`; component scanning includes both spellings.

## Completed stages

1. Corrected controller package and verified `/api/demo/hello`.
2. Circuit Breaker, Retry, Rate Limiter, Bulkhead and Time Limiter demos.
3. Explicit fallback responses; fallback and original demo share `demoDownstream`.
4. Independent `inventoryService` and `paymentService` circuit breakers.
5. Actuator health and Micrometer resilience metrics.
6. k6 baseline and resilience checks in `performance/`.

## Validation and artifacts

- Latest Java monitoring build: `clean verify`, 41 tests passed, packaging succeeded.
- k6 v2.3.0: all five scripts passed, 1047 checks, no unexpected HTTP failures.
- Full measurements: `performance/VALIDATION.md`; run commands: `performance/README.md`.
- Monitoring examples: `docs/monitoring.md`.
- Raw k6 results are preserved locally in `performance/results/` and ignored by Git.
- `target/manual-circuit-breaker.log` is generated output, ignored by Git, and contains
  the earlier CLOSED → OPEN → HALF_OPEN → CLOSED sequence.
- k6 was run from an official portable release in a temporary directory, not installed
  on PATH. Install k6 or use the portable executable's full path for future runs.
- The temporary application used for k6 was stopped. Do not stop an IntelliJ instance
  or reuse port 8080 without checking ownership.
- Earlier Windows file locks in the workspace's `target/` blocked cleanup/repackaging.
  Clean builds were validated in temporary copies of the current source instead.

## Next session

1. Inspect Git status and confirm the resume branch before editing.
2. Write the main professional README: setup, package layout, endpoint catalogue,
   resilience configuration, tests, monitoring, k6 instructions and measured limitations.
3. Review remaining assignment requirements and Git branch integration with the user.
4. Obtain approval before commits, pushes, major dependencies or substantial architecture changes.

Keep stages small and explanations beginner-friendly. Include exact file paths and relevant
line numbers when explaining important changes. Do not push without explicit approval.
