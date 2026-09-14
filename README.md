# Agentic Software Engineering System — URL Shortener

A governed, stateful, agentic software-engineering orchestration platform,
demonstrated through a URL-shortener workload (create / redirect / analytics).

**The URL shortener is not the deliverable.** It exists to give the
orchestrator something real to build, change, and validate. Everything scored
by this assignment — decomposition, non-linear orchestration, governance,
retries/rollback, audit, re-planning — lives in the orchestration layer
(`com.example.agentic.orchestration`, `.agent`, `.policy`, `.approval`,
`.audit`) and is demonstrated *through* the shortener across three scenarios:
Greenfield, Brownfield, Ambiguous. See [`01-architecture.md`](01-architecture.md),
[`02-decisions.md`](02-decisions.md), [`03-scenarios.md`](03-scenarios.md), and
[`04-execution-plan.md`](04-execution-plan.md) for the original design docs
this implementation follows.

---

## Quick start

### Option A — Docker Compose (no local Java/Maven needed)

```bash
docker compose up --build
```

App comes up on `http://localhost:8080`. Flyway migrations run automatically.

### Option B — Local Maven + Postgres

```bash
createdb agentic   # Postgres running locally, user/db "agentic"/"agentic"
mvn spring-boot:run
```

Swagger UI: `http://localhost:8080/swagger-ui.html`

---

## Demo script (~10–15 min)

The commands below assume `BASE=http://localhost:8080`.

**1. URL shortener basics**

```bash
curl -X POST $BASE/api/v1/urls -H 'Content-Type: application/json' \
  -d '{"url":"https://example.com/very/long/path"}'
curl -i $BASE/<shortCode>                      # 302 redirect
curl $BASE/api/v1/urls/<shortCode>/analytics
```

**2. Greenfield — full happy path with a human approval gate**

```bash
curl -X POST $BASE/api/v1/workflows -H 'Content-Type: application/json' -d '{
  "scenario": "GREENFIELD",
  "requirement": "Build a URL shortener that allows users to create short links and track clicks."
}'
```

Watch it run `requirements → (architecture ∥ planning) → implementation →
(testing ∥ security) → documentation → release`, park at
`WAITING_FOR_APPROVAL` on the HIGH-risk `release` node, and show the Base62
decision:

```bash
curl $BASE/api/v1/workflows/{id}/decisions
curl $BASE/api/v1/workflows/{id}/approvals
curl -X POST $BASE/api/v1/workflows/{id}/approvals/{approvalId}/approve \
  -H 'Content-Type: application/json' -d '{"approver":"panel"}'
curl $BASE/api/v1/workflows/{id}          # status: SUCCEEDED
```

**3. Brownfield — codebase-aware impact analysis + schema-change approval**

```bash
curl -X POST $BASE/api/v1/workflows -H 'Content-Type: application/json' -d '{
  "scenario": "BROWNFIELD",
  "requirement": "Add configurable URL expiration without breaking existing clients."
}'
curl $BASE/api/v1/workflows/{id}/artifacts   # IMPACT_ANALYSIS names affected/unaffected components
```

The `schema` node parks at `WAITING_FOR_APPROVAL` (HIGH risk) while
`implementation` and `test_plan` — independent of the schema decision — have
already proceeded in parallel. Approve twice (schema, then release) to
complete.

**4. Ambiguous — structured ambiguity detection, clarification, re-plan**

```bash
curl -X POST $BASE/api/v1/workflows -H 'Content-Type: application/json' -d '{
  "scenario": "AMBIGUOUS",
  "requirement": "Make the URL shortener highly scalable and improve analytics performance."
}'
curl $BASE/api/v1/workflows/{id}   # status: CLARIFICATION_REQUIRED, requirements node BLOCKED with ambiguities[]

curl -X POST $BASE/api/v1/workflows/{id}/clarify -H 'Content-Type: application/json' -d '{
  "clarifiedRequirement": "Support 500 req/s sustained, p99 redirect latency under 100ms, 99.9% availability, analytics under 1s with eventual consistency, 90-day retention."
}'
```

Requirement version bumps to 2, `ImpactAnalyzer`/`Replanner` run (the same
mechanism as Brownfield), and the graph extends past `requirements` into the
full build-out.

**5. Forced failure — retry → fallback → rollback → audit, live**

```bash
curl -X POST $BASE/api/v1/workflows -H 'Content-Type: application/json' -d '{
  "scenario": "GREENFIELD",
  "requirement": "Build a URL shortener MVP.",
  "metadata": {"forceTestFailure": true}
}'
curl $BASE/api/v1/workflows/{id}/audit
```

`testing` retries twice, exhausts its budget, falls back, and
`RollbackManager` invalidates its artifacts and resets downstream successors.
`metadata: {"forceSecurityFailure": true}` demonstrates the non-retryable
path instead — `SECURITY` failures skip straight to fallback/rollback with
zero retries, per the `FailureType` classification.

**6. Metrics summary**

```bash
curl $BASE/api/v1/metrics/summary
```

Success rate, retry/rollback/replan counts, average approval wait time
(MTTR proxy), average end-to-end workflow duration — no Grafana required.

---

## Requirement → implementation traceability

| Assignment requirement | Implementation |
|---|---|
| Requirement understanding | `RequirementsAgent` |
| Ambiguity detection | `RequirementsAgent` → structured `ambiguities[]` in `AgentResult` |
| Task decomposition | `PlanningAgent` → `WorkflowGraph` / `GraphNodeSpec` |
| Dependency graph, seq/parallel, sync | `WorkflowGraph` + `WorkflowEngine.tick()` ready-node evaluation |
| Cross-stage context | `Artifact` store keyed by `ArtifactType`, latest-active-version lookup in `AgentContext` |
| Decision lineage | `Decision` entity, e.g. the recorded Base62 choice |
| Human approval checkpoints | `ApprovalService` + `PolicyEngine` risk mapping |
| Bounded retry / fallback | `RetryPolicy`, `FailureType` |
| Rollback | `RollbackManager` (compensating actions: invalidate artifact, reset downstream node) |
| Safe-stop | `WorkflowEngine.safeStop()` / `resume()` |
| Policy guardrails | `PolicyEngine` (`SecurityPolicy`, `ChangeControlPolicy`, `ReleasePolicy`) |
| Audit-grade observability | `AuditEvent` + `AuditService`, written in the same transaction as the state change it describes |
| Reliability metrics | `MetricsService` (Micrometer) + `/api/v1/metrics/summary` |
| Dynamic re-planning | `ImpactAnalyzer` + `Replanner`, shared by Brownfield impact analysis and the Ambiguous clarify flow |
| Greenfield / Brownfield / Ambiguous | `WorkflowGraphs` + `ScenarioRunner` — see [`03-scenarios.md`](03-scenarios.md) |

---

## Architecture at a glance

```
REST API (WorkflowController, UrlController, ApprovalController-equivalent, AuditController)
        │
        ▼
WorkflowEngine  ← the brain: dependency eval, policy, approval, retry, rollback
   │        │
   ▼        ▼
Agents   Policies   (propose)        (authorize) — an LLM never executes directly (ADR-6)
   │        │
   ▼        ▼
Artifact / Decision / Approval / AuditEvent store  →  PostgreSQL (Flyway V1–V6)
```

- **Orchestrator owns control; agents only propose** (ADR-6). `Agent.execute()`
  returns an `AgentResult`; `WorkflowEngine` is the only thing that persists
  workflow/node/artifact/decision state.
- **Risk → autonomy mapping** (01-architecture.md §4): LOW/MEDIUM execute
  autonomously (MEDIUM audited); HIGH requires human approval; CRITICAL
  requires approval + security review. Enforced independently by three
  `Policy` beans (`ChangeControlPolicy`, `SecurityPolicy`, `ReleasePolicy`),
  combined by `PolicyEngine` with DENY > REQUIRE_APPROVAL > ALLOW precedence.
- **Approval authorizes a proposal, it does not re-trigger one.** A node's
  already-computed `AgentResult` is stashed (in-memory, keyed by node id) when
  a policy requires approval; approving commits that same result rather than
  re-running the agent and re-evaluating policy — otherwise every HIGH-risk
  node would re-request approval forever.
- **Single-writer-per-workflow.** A `ReentrantLock` per workflow id guards
  `WorkflowEngine.runToQuiescence()` so the background `WorkflowScheduler`
  tick and a synchronous API call (approve/resume) can never process the same
  node concurrently.
- **LLM isolated behind `LlmClient`** (ADR-5). `MockLlmClient` is the default
  for deterministic CI/demo; `RealLlmClient` (Anthropic Messages API over
  plain `HttpClient`, no vendor SDK) activates via `agent.llm.mode=real` +
  `ANTHROPIC_API_KEY`. Every agent calls the client (so the seam is
  demonstrably live), but decision logic itself is deterministic Java —
  ambiguity detection, risk classification, and artifact generation do not
  depend on parsing free-form model text, which is what keeps the orchestration
  test suite reproducible.

Full trade-off reasoning, including anticipated interviewer Q&A, is in
[`02-decisions.md`](02-decisions.md).

---

## Testing

```bash
mvn test
```

- **Unit tests** (no infra): short-code generation, URL/scheme validation,
  visitor-hash IP protection, dependency-graph cycle/readiness logic,
  `RetryPolicy` classification, `PolicyEngine` precedence, `ImpactAnalyzer`
  invalidation-set computation, `RequirementsAgent` ambiguity detection.
- **Integration tests** (Testcontainers Postgres, real Spring context):
  full Greenfield/Brownfield/Ambiguous scenario runs, the HIGH-risk approval
  gate, the retry→fallback→rollback path for both `TRANSIENT` (test failure)
  and `SECURITY` (non-retryable) failure types, and requirement clarification
  → selective re-plan.

Requires a Docker daemon on `PATH`/`DOCKER_HOST` for the Testcontainers-based
tests (`WorkflowOrchestrationIntegrationTest`, `UrlShortenerIntegrationTest`).

---

## Limitations

Given the assessment's time window, I prioritized the orchestration engine —
dependency graph, state machine, policy/approval, retry/rollback, audit,
re-planning — over breadth of the URL-shortener feature set and over the
Kafka brownfield extension (Scenario 2b in `03-scenarios.md`), which is
designed for (the `EventPublisher` seam is real and already used by
`ClickTrackingService`) but not implemented as a second `KafkaEventPublisher`.
I intentionally did not implement authentication, custom aliases, or
multi-region deployment, because they don't demonstrate the orchestration
objective this assignment evaluates.

The `WorkflowEngine`'s concurrency guard (a per-workflow `ReentrantLock`) is
correct for this single-process "modular monolith" deployment (ADR-4) but
would need a database-level advisory lock if this were ever split into
multiple orchestrator instances.

`EngineeringTool` implementations (`RunTestTool`, `GitDiffTool`, etc.) are
intentionally simulated rather than shelling out to a real test runner —
the assessment's subject is orchestration governance, not whether the
orchestrator can drive this specific project's own Maven build as a side
effect of a demo API call. `RunTestTool`/`SecurityAgent` do honor an explicit
`forceTestFailure`/`forceSecurityFailure` metadata flag precisely so the
retry/fallback/rollback path can be demonstrated deterministically (see demo
script step 5) rather than only on the happy path.
