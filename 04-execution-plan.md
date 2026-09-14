# 2–3 Day Execution Plan

## Priority order if time runs out

Build top-down; everything below the line is genuinely optional.

**Must have**
- URL shortener (create, redirect, analytics) with tests
- `WorkflowGraph` + `WorkflowStateMachine` (real dependency graph, not a linear
  chain)
- At least 3 agents wired through the DAG (Requirements, Implementation, Test is
  enough to prove the pattern — the rest can be thin)
- `Artifact` store with versioning (cross-stage context + lineage)
- `PolicyEngine` + risk-based human approval on at least one HIGH-risk node
- Bounded retry with a demonstrated failure → retry → outcome
- `AuditEvent` trail
- All three scenarios runnable, even if some agents are intentionally thin/mocked
- Unit + a handful of orchestration tests (failure/retry/approval paths)

**Should have**
- Rollback demonstration (Scenario 2b or a forced security-fail in Scenario 1)
- Re-planning demonstration (Scenario 3)
- Metrics summary endpoint
- Real `LlmClient` wired for at least one agent, demoed live

**Nice to have (cut first under time pressure)**
- Kafka brownfield enhancement (Scenario 2b) fully implemented
- Feature-flag rollback (`analytics.processing.mode`)
- Prometheus/Grafana wiring beyond raw Micrometer counters
- A frontend/dashboard (Swagger is enough)

---

## Day 1 — Foundation

**Morning**
- Spring Boot project skeleton, Docker Compose (app + Postgres)
- Flyway migrations V1–V2: `urls`, `click_events`
- `Url`, `ClickEvent` entities; `UrlService`, `RedirectService`
- `POST /api/v1/urls`, `GET /{shortCode}`, `GET /api/v1/urls/{shortCode}/analytics`
- Unit tests for short-code generation, validation

**Afternoon**
- Flyway V3–V6: `workflows`, `workflow_nodes`, `artifacts`, `decisions`,
  `approvals`, `audit_events`
- `WorkflowGraph`, `WorkflowNode`, `DependencyResolver`
- `WorkflowStateMachine` (`NodeStatus` enum + valid transitions)
- `WorkflowEngine` ready-node scheduling loop (sequential first, prove it works)

**Evening**
- `Agent` interface, `AgentContext`, `AgentResult`
- `MockLlmClient` returning deterministic structured JSON
- `RequirementsAgent`, `ArchitectureAgent`, `DeveloperAgent`, `TestAgent` (thin is
  fine — the point is the contract, not eloquence)

*End of Day 1 checkpoint: URL shortener works standalone; a hard-coded workflow
graph executes sequentially end-to-end with mock agents.*

---

## Day 2 — The actual differentiator

- Parallel execution + synchronization in the scheduler (architecture ∥ planning;
  testing ∥ security)
- `Artifact` store with `source_artifact_ids` lineage
- `Decision` entity + at least one real recorded decision (Base62 choice)
- `PolicyEngine` + `RiskLevel` mapping; `ApprovalService` + approval REST API
- `RetryPolicy` + `FailureType` classification; one deliberately-failing node to
  prove retry → fallback
- `RollbackManager` with at least one compensating action
- `WorkflowEngine.safeStop()` / `resume()`
- `AuditService` wired into every state transition
- Wire the three scenario payloads (`greenfield.json`, `brownfield.json`,
  `ambiguous.json`) and confirm each produces the graph shape described in
  `03-scenarios.md`

*End of Day 2 checkpoint: all three scenarios runnable via REST; approval gate,
retry, and audit trail all visibly working.*

---

## Day 3 — Polish, validate, rehearse

- `ImpactAnalyzer` + `Replanner` for Scenario 3 (and 2b if time allows)
- `RealLlmClient` behind `agent.llm.mode=real`, demoed for at least one agent
- Metrics summary endpoint (success rate, retry/rollback frequency, MTTR, e2e
  latency)
- Security pass: URL scheme allowlist, IP hashing, tool allowlisting review
- Orchestration tests: parallel sync, retry→fallback, HIGH-risk approval,
  requirement-change→replan
- Testcontainers integration tests (Postgres, and Kafka if 2b is in scope)
- Write `README.md` with the traceability matrix from `01-architecture.md` §11
- Record/rehearse the demo script in `03-scenarios.md` §"Demo script" — time it,
  keep it under 15 minutes, make sure steps 3–5 (brownfield, ambiguous, failure
  handling) get real airtime, not just the greenfield happy path

---

## What to say if you run out of time before Day 3 finishes

Be upfront rather than silent about scope. In the README's "Limitations" section:

> "Given the 2–3 day window, I prioritized the orchestration engine — dependency
> graph, state machine, policy/approval, retry/rollback, audit — over breadth of
> the URL-shortener feature set and over the Kafka brownfield extension. I
> intentionally did not implement authentication, custom aliases, or multi-region
> deployment, because they don't demonstrate the orchestration objective this
> assignment is evaluating. \[List anything else cut, and why.\]"

That's engineering judgment, not a gap — say it that way in the doc and out loud.
