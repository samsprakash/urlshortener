# Runbook — Local Setup & Commands

Everything needed to build, run, test, and inspect the system from a terminal
on this machine. `.zshrc` already exports the toolchain paths (JAVA_HOME,
Postgres, Colima/Docker, Maven) — open a **new terminal tab** (or run `source
~/.zshrc`) after setup so they take effect.

---

## 0. One-time setup (already done on this machine)

```bash
# JDK 21, Maven, Postgres 16, Colima (Docker runtime for Testcontainers)
brew install openjdk@21 maven postgresql@16 colima docker

# Postgres role + database
brew services start postgresql@16
createuser -s agentic
psql -d postgres -c "ALTER USER agentic WITH PASSWORD 'agentic';"
createdb -O agentic agentic
```

`.zshrc` additions (already applied):

```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home
export PATH="$JAVA_HOME/bin:/opt/homebrew/opt/postgresql@16/bin:/opt/homebrew/opt/colima/bin:/opt/homebrew/opt/docker/bin:/opt/homebrew/opt/maven/bin:$PATH"
```

Open a new terminal (or `source ~/.zshrc`) to pick these up.

---

## 1. Start/stop Postgres

Postgres runs as a persistent background service — you generally don't need
to start it manually, but here's how:

```bash
brew services start postgresql@16     # start now + at login
brew services stop postgresql@16      # stop
brew services list                    # check status
pg_isready                            # quick health check
```

## 2. Start/stop Colima (only needed for `mvn test`'s Testcontainers tests)

Colima is **not** a brew service on this machine — start it manually when you
need Docker (e.g. before running the full test suite):

```bash
colima start                # first start of the day; takes ~15-30s
colima status                # check it's up
docker ps                    # confirms the Docker CLI can reach it
colima stop                  # stop when done (frees RAM/CPU)
```

---

## 3. Build & run the app

```bash
cd /Users/prakashselvam/Documents/projects/code/urlshortner

# Compile only
mvn -q compile

# Run in foreground (Ctrl+C to stop)
mvn spring-boot:run

# Run in background, logging to a file
mvn -q spring-boot:run > /tmp/agentic-app.log 2>&1 &
disown
tail -f /tmp/agentic-app.log      # watch logs
```

Health check once it's up:

```bash
curl -s http://localhost:8080/actuator/health
```

Stop a backgrounded instance:

```bash
lsof -ti:8080 | xargs kill      # find & kill whatever owns port 8080
# or, if you launched it via `mvn spring-boot:run`:
pkill -f "spring-boot:run"
```

Swagger UI (interactive API explorer): **http://localhost:8080/swagger-ui.html**

---

## 4. Run tests

```bash
# Everything (needs Colima/Docker running — see §2 — for the 2 Testcontainers tests)
colima start
mvn test

# Unit tests only, skip the 2 Testcontainers-based integration tests
mvn -Dtest='!WorkflowOrchestrationIntegrationTest,!UrlShortenerIntegrationTest' test

# A single test class
mvn -Dtest=PolicyEngineTest test

# A single test method
mvn -Dtest=PolicyEngineTest#denyTakesPrecedenceOverAllowAndApproval test
```

Test reports land in `target/surefire-reports/`.

---

## 5. Docker Compose (alternative to local Maven/Postgres)

Runs the whole thing — app + its own Postgres — in containers, no local JDK
needed on the host:

```bash
docker compose up --build       # foreground, rebuilds the app image
docker compose up -d            # background
docker compose logs -f app      # tail app logs
docker compose down             # stop and remove containers
docker compose down -v          # also wipe the Postgres volume (fresh DB next time)
```

---

## 6. Database access

Connect with `psql` directly:

```bash
psql -U agentic -d agentic -h localhost
```

Common inspection queries once connected (or via `psql -c "..."` one-liners):

```sql
-- List all tables
\dt

-- URL shortener data
SELECT short_code, original_url, status, click_count FROM urls ORDER BY created_at DESC LIMIT 10;
SELECT * FROM click_events ORDER BY occurred_at DESC LIMIT 10;

-- Orchestration state
SELECT id, scenario, status, requirement_version, created_at FROM workflows ORDER BY created_at DESC LIMIT 10;
SELECT node_key, agent_type, status, risk_level, retry_count FROM workflow_nodes WHERE workflow_id = '<id>' ORDER BY created_at;
SELECT type, version, status, created_by FROM artifacts WHERE workflow_id = '<id>' ORDER BY created_at;
SELECT decision, rationale, selected FROM decisions WHERE workflow_id = '<id>';
SELECT node_id, status, risk_level, approver, requested_at, decided_at FROM approvals WHERE workflow_id = '<id>';
SELECT event_type, outcome, reason, created_at FROM audit_events WHERE workflow_id = '<id>' ORDER BY created_at;
```

One-liners from the shell without opening a `psql` session:

```bash
psql -U agentic -d agentic -h localhost -c "SELECT id, scenario, status FROM workflows ORDER BY created_at DESC LIMIT 5;"
```

**Reset all data** (careful — wipes everything, useful between demo runs):

```bash
psql -U agentic -d agentic -h localhost -c \
  "TRUNCATE audit_events, approvals, decisions, artifacts, workflow_nodes, workflows, click_events, urls CASCADE;"
```

**Full reset including schema** (Flyway will re-run all migrations on next app start):

```bash
dropdb -U agentic agentic 2>/dev/null; dropdb agentic 2>/dev/null
createdb -O agentic agentic
```

---

## 7. Quick API smoke test (copy-paste block)

Run the app first (§3), then:

```bash
BASE=http://localhost:8080

# URL shortener
curl -s -X POST $BASE/api/v1/urls -H 'Content-Type: application/json' \
  -d '{"url":"https://example.com/very/long/path"}' | python3 -m json.tool

# Greenfield workflow
curl -s -X POST $BASE/api/v1/workflows -H 'Content-Type: application/json' -d \
  '{"scenario":"GREENFIELD","requirement":"Build a URL shortener that allows users to create short links and track clicks."}' \
  | python3 -m json.tool

# Metrics
curl -s $BASE/api/v1/metrics/summary | python3 -m json.tool
```

See [`docs/scenarios.md`](docs/scenarios.md) for the full scenario-by-scenario
design and the demo script intended for the interview panel.

---

## 8. Troubleshooting

| Symptom | Likely cause / fix |
|---|---|
| `java: command not found` in a new terminal | `.zshrc` not sourced yet — open a new tab or run `source ~/.zshrc` |
| App fails to start: `Connection refused` to Postgres | `brew services start postgresql@16`, then `pg_isready` |
| `mvn test` fails with "Could not find a valid Docker environment" | Run `colima start` first; Testcontainers needs a live Docker daemon |
| Port 8080 already in use | `lsof -ti:8080 \| xargs kill`, then restart |
| Flyway migration checksum error | Someone edited an already-applied `V*.sql` file — never edit a shipped migration, add a new `V*` file instead |
