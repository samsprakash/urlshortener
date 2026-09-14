# Agentic Software Engineering System — Architecture

**Workload:** URL Shortener (create / redirect / analytics)
**Actual subject under test:** a governed, stateful agentic SDLC orchestration layer

The URL shortener is not the deliverable. It exists to give the orchestrator something
real to build, change, and validate. Everything the interview rubric scores —
decomposition, non-linear orchestration, governance, retries/rollback, audit,
re-planning — lives in the orchestration layer, and is demonstrated *through* the
shortener across three scenarios (greenfield, brownfield, ambiguous).

---

## 1. Target architecture

```
                     ┌─────────────────────┐
                     │   REST API / CLI     │
                     └──────────┬──────────┘
                                │
                                ▼
                 ┌──────────────────────────┐
                 │    Workflow Orchestrator │
                 │                          │
                 │  DAG + State + Scheduler │
                 │  Policy · Retry · Replan │
                 └────────────┬─────────────┘
                              │
         ┌────────────────────┼────────────────────┐
         │                    │                    │
         ▼                    ▼                    ▼
  ┌─────────────┐      ┌─────────────┐     ┌─────────────┐
  │    Agents   │      │  Policies   │     │    Tools    │
  │ Requirement │      │  Security   │     │ Read/Write  │
  │ Architecture│      │  Change Ctl │     │ Run Tests   │
  │ Planning    │      │  Approval   │     │ Git Diff    │
  │ Developer   │      │  Release    │     └─────────────┘
  │ Test        │      └─────────────┘
  │ Security    │
  │ Docs        │
  │ Release     │
  └──────┬──────┘
         │
         ▼
  ┌──────────────────────────────────────────┐
  │ Artifact Store · Decision Log · Audit Log │
  │           Workflow State                  │
  └────────────────────┬───────────────────────┘
                        ▼
                   PostgreSQL
```

The **orchestrator is the brain**. Agents propose; the orchestrator authorizes,
sequences, retries, rolls back, and audits. An LLM never directly executes a
mutating action — it returns a structured recommendation that passes through the
policy engine like everything else.

```
        LLM
         │
   proposes action
         │
         ▼
┌────────────────┐
│  ORCHESTRATOR  │
│  state         │
│  dependencies  │
│  policies      │
│  approvals     │
│  retries       │
│  rollback      │
└───────┬────────┘
        │
  authorized action
        │
        ▼
      TOOL / AGENT EXECUTION
```

---

## 2. Technology stack (and why)

| Concern | Choice | Rationale |
|---|---|---|
| Language/runtime | Java 21 + Spring Boot 3.x | Java Developer interview; strong typing, DI, mature testing/observability ecosystem |
| Persistence | PostgreSQL + Flyway | Workflow state, nodes, artifacts, decisions, approvals, audit events are relational with real FK relationships (node depends_on node, artifact derived_from artifact). Consistency > flexibility here. |
| Eventing (v1) | `EventPublisher` interface → `InProcessEventPublisher` | No workload yet justifies Kafka's operational cost. Interface is the seam for a brownfield Kafka enhancement later (see `03-scenarios.md`, Scenario 2b). |
| LLM | `LlmClient` interface → `MockLlmClient` (default) / `RealLlmClient` (flag) | Deterministic, reproducible CI and demo by default; `agent.llm.mode=real` swaps in an actual Claude/OpenAI call with no changes to orchestration code. |
| Testing | JUnit 5, Mockito, Testcontainers (Postgres) | Real integration tests without requiring local infra installs |
| Packaging | Modular monolith, Docker Compose | Matches the actual complexity budget of a 2–3 day assessment; module boundaries are drawn so pieces (orchestrator, url-shortener, agent-runtime) could later be extracted as services if team/scale justified it |

Full trade-off reasoning (including anticipated interviewer questions and answers)
is in `02-decisions.md`.

---

## 3. Package structure

```
com.example.agentic
├── AgenticApplication.java
│
├── url/                      # the workload
│   ├── domain/  Url, UrlStatus, ClickEvent
│   ├── api/     UrlController, AnalyticsController, DTOs
│   ├── service/ UrlService, RedirectService, ClickTrackingService
│   └── repo/    UrlRepository, ClickEventRepository
│
├── orchestration/            # the actual subject of the assessment
│   ├── graph/     WorkflowGraph, WorkflowNode, DependencyResolver
│   ├── state/     Workflow, WorkflowState, NodeStatus, WorkflowStateMachine
│   ├── engine/    WorkflowEngine, WorkflowScheduler
│   ├── retry/     RetryPolicy, FailureType
│   ├── rollback/  RollbackManager, RollbackAction (compensating actions)
│   ├── replan/    ImpactAnalyzer, Replanner
│   └── repo/      WorkflowRepository, WorkflowNodeRepository
│
├── agent/
│   ├── Agent.java, AgentType, AgentContext, AgentResult
│   ├── RequirementsAgent, ArchitectureAgent, PlanningAgent
│   ├── DeveloperAgent, TestAgent, SecurityAgent
│   ├── DocumentationAgent, ReleaseAgent
│   └── llm/  LlmClient, MockLlmClient, RealLlmClient, PromptTemplates
│
├── artifact/
│   ├── Artifact.java, ArtifactType
│   ├── Decision.java              # decision lineage
│   └── repo/ ArtifactRepository, DecisionRepository
│
├── policy/
│   ├── Policy.java, PolicyDecision, RiskLevel
│   ├── SecurityPolicy, ChangeControlPolicy, ReleasePolicy
│   └── PolicyEngine.java
│
├── approval/
│   ├── Approval.java, ApprovalStatus
│   ├── ApprovalService.java
│   └── api/ ApprovalController
│
├── tool/
│   ├── EngineeringTool.java
│   └── ReadFileTool, WriteFileTool, RunTestTool, GitDiffTool
│
├── events/
│   └── EventPublisher.java, InProcessEventPublisher   # Kafka-ready seam
│
├── audit/
│   ├── AuditEvent.java, AuditService.java
│   └── api/ AuditController
│
└── observability/
    └── MetricsService.java        # Micrometer counters/timers
```

---

## 4. Core domain model

### Workflow / orchestration tables

```sql
workflows(id, scenario, status, requirement_version, correlation_id, created_at, completed_at)
workflow_nodes(id, workflow_id, node_key, agent_type, status, risk_level,
               retry_count, max_retries, depends_on jsonb, started_at, completed_at)
artifacts(id, workflow_id, node_id, type, version, content jsonb,
          source_artifact_ids jsonb, status, created_by, created_at)
decisions(id, workflow_id, artifact_id, decision, rationale,
          alternatives jsonb, selected, created_at)
approvals(id, workflow_id, node_id, action, risk_level, status,
          approver, comment, requested_at, decided_at)
audit_events(id, workflow_id, node_id, event_type, actor, outcome,
             reason, correlation_id, artifact_versions jsonb, created_at)
```

### URL shortener tables

```sql
urls(id, short_code UNIQUE, original_url, status, created_at, expires_at)
click_events(id, url_id FK, occurred_at, visitor_hash, user_agent, referrer)
```

Six Flyway migrations (`V1`..`V6`) create these incrementally — itself a small
demonstration of controlled schema evolution.

### Key abstractions

```java
public interface Agent {
    AgentType type();
    AgentResult execute(AgentContext context);
}

public record AgentContext(UUID workflowId, Map<ArtifactType, Artifact> artifacts,
                            Map<String, Object> metadata) {}

public record AgentResult(boolean success, List<Artifact> artifacts,
                           List<Decision> decisions, String error) {}

public interface Policy {
    PolicyDecision evaluate(AgentAction action, AgentContext context);
}

public record PolicyDecision(DecisionType type /* ALLOW/DENY/REQUIRE_APPROVAL */,
                              String reason, boolean requiresApproval) {}

public enum NodeStatus {
    PENDING, READY, RUNNING, WAITING_FOR_APPROVAL,
    RETRYING, SUCCEEDED, FAILED, BLOCKED, ROLLED_BACK, SAFE_STOPPED
}

public enum RiskLevel { LOW, MEDIUM, HIGH, CRITICAL }
```

Risk → autonomy mapping (the "controlled autonomy" the rubric asks for explicitly):

| Action | Risk | Autonomy |
|---|---|---|
| Analyze requirement, generate docs | LOW | Agent executes autonomously |
| Generate code, run tests | MEDIUM | Agent executes, audited |
| Database schema change | HIGH | Human approval required |
| Production deployment / destructive action | CRITICAL | Human approval + security review |

---

## 5. Workflow graph (base shape, all scenarios)

```
requirements
   │
   ├──────────────┐
   ▼               ▼
architecture   planning          ← parallel, synchronize below
   │               │
   └──────┬────────┘
          ▼
   implementation
          │
   ┌──────┴──────┐
   ▼             ▼
testing       security           ← parallel
   │             │
   └──────┬──────┘
          ▼
   documentation
          │
          ▼
   release review
          │
          ▼
   human approval  (HIGH/CRITICAL only)
          │
          ▼
         done
```

Back-edges exist for failure handling and are the actual differentiator versus a
plain DAG runner:

```
security FAIL → rollback implementation → replan implementation+testing
tests exhausted retries → fallback (alternate agent strategy) → replan
requirement changed → impact analysis → selective invalidation → replan
```

`WorkflowEngine` evaluates, for every tick: dependencies satisfied? policy allows?
approval pending? not safe-stopped? Only then does a node move `READY → RUNNING`.

---

## 6. Retry / fallback / rollback / safe-stop

- **Retry**: bounded per node (`maxRetries`, default 2), classified by `FailureType`
  (`TRANSIENT`, `VALIDATION`, `SECURITY`, `BUSINESS`, `INFRASTRUCTURE`). Only
  `TRANSIENT`/`INFRASTRUCTURE` auto-retry; `SECURITY`/`VALIDATION` go straight to
  fallback or safe-stop.
- **Fallback**: after retry exhaustion, orchestrator can invoke an alternate agent
  strategy, request another agent's analysis, or mark the node `BLOCKED` pending a
  human.
- **Rollback**: compensating actions, not transactional rollback (agent actions can
  span external systems). `RollbackAction.compensate()` — e.g. mark artifact
  `INVALIDATED`, reverse a generated patch, run a down-migration.
- **Safe-stop**: on critical policy violation or inconsistent state, the scheduler
  stops issuing new node executions, persists state, records an audit event, and
  waits for human `resume`/`approve`/`reject`. Already-running non-destructive work
  is allowed to finish.

## 7. Human approval

```
GET  /api/v1/workflows/{id}
GET  /api/v1/workflows/{id}/approvals
POST /api/v1/workflows/{id}/approvals/{approvalId}/approve
POST /api/v1/workflows/{id}/approvals/{approvalId}/reject
POST /api/v1/workflows/{id}/stop
POST /api/v1/workflows/{id}/resume
```

Swagger/OpenAPI is sufficient for the demo — no frontend needed. A `HIGH`/`CRITICAL`
node parks the workflow in `WAITING_FOR_APPROVAL` and the demo script pauses there
live.

## 8. Dynamic re-planning

Artifacts are versioned (`RequirementSpec v1 → v2`, etc.) with an explicit
dependency map (`ArtifactType → Set<ArtifactType>`). When a requirement changes:

1. Version the new requirement artifact.
2. `ImpactAnalyzer` walks the dependency map to find affected downstream artifacts.
3. Affected artifacts are marked `INVALIDATED`; unaffected artifacts and their nodes
   are preserved (not re-run).
4. `Replanner` regenerates only the affected subgraph.
5. Decision lineage and audit history from before the change are preserved.

This is demonstrated concretely in the Ambiguous scenario (`03-scenarios.md`).

## 9. Observability & metrics

Micrometer counters/timers: `workflow.success`, `workflow.failure`,
`workflow.retry`, `workflow.rollback`, `workflow.replan`, `workflow.duration`,
`node.duration`, `approval.wait.time`. `AuditEvent` rows give per-node,
per-decision traceability keyed by `workflowId` + `correlationId`. Together these
satisfy the rubric's "success rate, retry/rollback frequency, MTTR, end-to-end
latency" requirement without needing Grafana — a CLI/JSON summary endpoint is
enough for the demo.

## 10. Security

- URL ingestion: scheme allowlist (`http`/`https` only), length limits, no
  `javascript:`/`data:` schemes, basic SSRF-awareness note in docs.
- Analytics: `visitor_hash = SHA-256(ip + salt)`, never store raw IP.
- Agent/tool boundary: LLM output never executes directly — it's a structured,
  validated recommendation (Jakarta Validation on the parsed JSON) that goes
  through `PolicyEngine` before any `EngineeringTool` runs. No unrestricted shell
  access for agents.
- Prompt-injection posture: agent tool calls are allowlisted and parameterized, not
  a free-form shell string built from LLM output.

## 11. Requirement → implementation traceability

| Assignment requirement | Implementation |
|---|---|
| Requirement understanding | `RequirementsAgent` |
| Ambiguity detection | `RequirementsAgent` → `ambiguities[]` in structured output |
| Task decomposition | `PlanningAgent` → `WorkflowGraph` |
| Dependency graph, seq/parallel, sync | `WorkflowGraph` + `WorkflowScheduler` ready-node evaluation |
| Cross-stage context | `Artifact` store keyed by `ArtifactType` |
| Decision lineage | `Decision` entity |
| Human approval checkpoints | `ApprovalService` + `PolicyEngine` risk mapping |
| Bounded retry / fallback | `RetryPolicy`, `FailureType` |
| Rollback | `RollbackManager` (compensating actions) |
| Safe-stop | `WorkflowEngine.safeStop()` |
| Policy guardrails | `PolicyEngine` (Security/ChangeControl/Release policies) |
| Audit-grade observability | `AuditEvent` + `AuditService` |
| Reliability metrics | `MetricsService` (Micrometer) |
| Dynamic re-planning | `ImpactAnalyzer` + `Replanner` |
| Greenfield / Brownfield / Ambiguous | Scenarios 1/2/3 — see `03-scenarios.md` |

This table is meant to go straight into the submission README.
