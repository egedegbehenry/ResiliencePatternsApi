# New laptop setup

## Repository and prerequisites

Repository: https://github.com/egedegbehenry/ResiliencePatternsApi

Install Git and a full Java **JDK 21**, set `JAVA_HOME` to its installation
directory and add its `bin` directory to `PATH`. Confirm `java -version` and
`javac -version` report 21. The old laptop uses Java 21.0.12.1.
The IDE's bundled runtime does not replace the project's JDK.

Install IntelliJ IDEA from https://www.jetbrains.com/idea/download/.
The current unified installer includes the free Java/Maven functionality formerly
provided by Community Edition; no paid subscription is required for this project.
See the [official installation guide](https://www.jetbrains.com/help/idea/installation-guide.html)
and [distribution explanation](https://www.jetbrains.com/help/idea/intellij-idea-single-distribution.html).

## Clone and open

In PowerShell:

```powershell
git clone https://github.com/egedegbehenry/ResiliencePatternsApi.git
cd ResiliencePatternsApi
git switch master
git status -sb
```

In IntelliJ choose **Open**, select this directory or `pom.xml`, and import it as
a Maven project. Under Project Structure select JDK 21; under Maven settings use
the Maven Wrapper and JDK 21 for importing/running. Reload all Maven projects and
allow dependency downloads. The wrapper pins Maven 3.10.0; no separate Maven
installation is needed. Internet access to Maven Central is required initially.

## Build, test and run

```powershell
.\mvnw.cmd verify
.\mvnw.cmd spring-boot:run
```

Alternatively, after building:

```powershell
java -jar target/resilence-patterns-api-0.0.1-SNAPSHOT.jar
```

On Linux/macOS use `sh mvnw verify` and `sh mvnw spring-boot:run`.
In IntelliJ run `com.henry.resilencepatternsapi.ResilencePatternsApiApplication`.
The entry-point spelling `resilence` is intentional; feature packages use
`resilience`, and component scanning includes both.

Check http://localhost:8080/api/demo/hello and http://localhost:8080/actuator/health.
The `/` path returns 404 by design. Stop your terminal application with Ctrl+C.
For a busy port use the JAR argument `--server.port=8081`.

## Configuration and optional tools

Spring Boot 4.1.1 and Resilience4j 2.4.0 are pinned in `pom.xml`.
`src/main/resources/application.properties` is tracked. The default profile needs
no application environment variables, secrets, database, migrations, Docker or
external downstream services. Inventory and payment calls are local simulations.
`JAVA_HOME`/`PATH` configure the build; optional `SERVER_PORT` changes the HTTP port.
No additional application profiles or Docker configuration currently exist.

Optional k6 checks and installation instructions are in `performance/README.md`.
Postman is optional for manual HTTP checks. Monitoring details are in
`docs/monitoring.md`. Historical raw performance output is preserved under
`docs/migration-backup/performance-results/`; future generated output remains
ignored under `performance/results/`.

Never commit credentials. Future local overrides may use ignored
`application-local.properties` with `SPRING_PROFILES_ACTIVE=local`, or environment
variables. Spring Boot does not load `.env` automatically. Store any future
secrets in a password manager and document variable names with safe examples.

## Resume development and recover checkpoints

`master` contains all completed features. Read `README.md`, `docs/HANDOFF.md`,
`performance/VALIDATION.md` and `docs/MIGRATION_AUDIT.md` before continuing.

```powershell
git fetch origin
git branch -a
git switch --track origin/feature/k6-performance-tests
```

Use that last command only when you want a historical feature checkpoint rather
than current `master`. The other preserved feature branches cover circuit breaker,
retry, rate limiter, bulkhead, time limiter, fallback, downstream circuit breakers
and monitoring. Git clone creates remote-tracking branches; local feature branches
are created on demand. Use `git switch master` to return to the latest work.

## Files and services to transfer separately

Git does not migrate IDE preferences, Local History, run configurations in
`.idea/`, GitHub login credentials/SSH keys, global Git configuration, Maven user
settings or caches, installed JDKs, or Postman collections stored outside the repo.
Export any desired IDE settings/Local History and Postman work separately before
retiring the old laptop. Copy credentials securely, never through this repository.
These machine-level locations and external applications have not been audited.
No project database or external service data needs migration.

Ignored `target/` output is reproducible using the build. Existing ignored raw
performance results have tracked archive copies; `.idea/` remains machine-local.
`HELP.md` is preserved as reference documentation. No stashes were present at
inspection. Keep the old laptop until you have cloned, built and tested on the new one.
